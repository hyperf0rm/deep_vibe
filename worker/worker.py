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

                y, sr = load_audiofile(url)
                bpm = get_bpm(y, sr)
                rms = get_rms(y)
                track = {
                    "id": track_id,
                    "bpm": bpm,
                    "rms": rms
                }
                result = json.dumps(track)
                logging.info(f"Pushing to redis queue: {result}")
                r.lpush(RESULTS_QUEUE_NAME, result)
                logging.info(f"Completed track {track_id} with BPM: {bpm} and energy score: {rms}")
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
    y, sr = librosa.load(output_stream)
    return y, sr


def get_bpm(y, sr):
    tempo, _ = librosa.beat.beat_track(y=y, sr=sr)
    bpm = round(tempo[0])
    return bpm


def get_rms(y):
    rms = ndarray.flatten(librosa.feature.rms(y=y))
    print(f"RMS: {rms}")
    print(f"len(RMS): {len(rms)}")
    mean_rms = np.mean(rms)
    mean_rms_dbfs = librosa.amplitude_to_db(mean_rms)
    normalized_rms = (mean_rms_dbfs - MIN_DBFS) / (MAX_DBFS - MIN_DBFS) * 100
    clipped_rms = np.clip(normalized_rms, 1, 100)
    converted_rms = float(clipped_rms)
    energy_score = round(converted_rms, 2)
    return energy_score


main()
