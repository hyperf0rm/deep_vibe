import librosa
import requests
import io
import redis
import os
import logging
import sys
from pydub import AudioSegment
import json
import numpy as np
from numpy import ndarray

LOG_FILE = "worker_crash.log"

logger = logging.getLogger()
logger.setLevel(logging.INFO)

file_handler = logging.FileHandler(LOG_FILE, mode='a', encoding='utf-8')
file_handler.setFormatter(logging.Formatter('%(asctime)s - %(levelname)s - %(message)s'))
logger.addHandler(file_handler)

console_handler = logging.StreamHandler(sys.stdout)
console_handler.setFormatter(logging.Formatter('[WORKER] %(message)s'))
logger.addHandler(console_handler)

TASK_QUEUE_NAME = "analyze_audio_task_queue"
RESULTS_QUEUE_NAME = "results_queue"

MIN_DBFS = -80
MAX_DBFS = 0

MIN_CENTROID = 500
MAX_CENTROID = 3500

SR_HIGH = 22050
SR_LOW = 11025

def main():
    try:
        redis_host = os.getenv('REDIS_HOST', 'localhost')
        logging.info(f"Connecting to Redis at: {redis_host}")
        r = redis.Redis(host=redis_host, port=6379, decode_responses=True)
        r.ping()
        logging.info("Connected to Redis successfully")
    except Exception as e:
        logging.error(f"Failed to connect to Redis: {e}")
    while True:
        try:
            task = r.brpop(keys=[TASK_QUEUE_NAME], timeout=0)
            if task:
                queue, item = task
                logging.info(f"queue: {queue}, task: {item}")
                task_data = json.loads(item)

                track_id = task_data["id"]
                url = task_data["preview_url"]

                logging.info(f"Processing track {track_id}")

                yt_high, yt_low, S = load_audiofile(url)
                bpm = get_bpm(yt_low, SR_LOW)
                rms = get_rms(S)
                spectral_centroid = get_spectral_centroid(S, SR_HIGH)
                track = {
                    "id": track_id,
                    "bpm": bpm,
                    "rms": rms,
                    "centroid": spectral_centroid
                }
                result = json.dumps(track)
                logging.info(f"Pushing to redis queue: {result}")
                r.lpush(RESULTS_QUEUE_NAME, result)
                logging.info(f"Completed track {track_id} "
                             f"with BPM: {bpm}, "
                             f"energy score: {rms} "
                             f"and centroid: {spectral_centroid}")
        except json.JSONDecodeError as e:
            logging.error(f"JSON decode error: {e}")
            logging.error(f"Item was: {repr(item)}")
        except Exception as e:
            logging.error(f"Worker error during track analysis: {e}")
            import traceback
            traceback.print_exc()

def load_audiofile(url):
    response = requests.get(url)
    response.raise_for_status()
    audio_file = io.BytesIO(response.content)
    m4a = AudioSegment.from_file(audio_file, format="m4a")
    output_stream = io.BytesIO()
    m4a.export(output_stream, format="wav")
    output_stream.seek(0)
    y_high, _ = librosa.load(output_stream, sr=SR_HIGH)
    yt_high, _ = librosa.effects.trim(y_high)
    S, _ = librosa.magphase(librosa.stft(y=yt_high))
    yt_low = yt_high[::2]
    return yt_high, yt_low, S


def normalize_and_convert(original_value, min_value, max_value):
    normalized = (original_value - min_value) / (max_value - min_value) * 100
    clipped = np.clip(normalized, 1, 100)
    converted = float(clipped)
    return round(converted, 2)


def get_bpm(y, sr):
    tempo, _ = librosa.beat.beat_track(y=y, sr=sr)
    bpm = round(tempo[0])
    return bpm


def get_rms(S):
    rms = ndarray.flatten(librosa.feature.rms(S=S))
    mean_rms = np.mean(rms)
    mean_rms_dbfs = librosa.amplitude_to_db(mean_rms)
    return normalize_and_convert(mean_rms_dbfs, MIN_DBFS, MAX_DBFS)


def get_spectral_centroid(S, sr):
    centroid = librosa.feature.spectral_centroid(S=S, sr=sr)
    mean_centroid = np.mean(centroid)
    return normalize_and_convert(mean_centroid, MIN_CENTROID, MAX_CENTROID)


if __name__ == "__main__":
    main()
