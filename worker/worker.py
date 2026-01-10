import librosa
import requests
import io
import redis
import os
import logging
import sys
from pydub import AudioSegment
import json

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

def main():
    try:
        redis_host = os.getenv('REDIS_HOST', 'localhost')
        print(f"Connecting to Redis at: {redis_host}")
        r = redis.Redis(host=redis_host, port=6379, decode_responses=True)
        r.ping()
        print("Connected to Redis successfully")
    except Exception as e:
        print(f"Failed to connect to Redis: {e}")
    while True:
        try:
            task = r.brpop(keys=[TASK_QUEUE_NAME], timeout=0)
            if task:
                queue, item = task
                print(f"queue: {queue}, task: {item}")
                task_data = json.loads(item)

                track_id = task_data["id"]
                url = task_data["preview_url"]

                print(f"Processing track {track_id}")

                response = requests.get(url)
                response.raise_for_status()
                audio_file = io.BytesIO(response.content)
                m4a = AudioSegment.from_file(audio_file, format="m4a")
                output_stream = io.BytesIO()
                m4a.export(output_stream, format="wav")
                output_stream.seek(0)
                y, sr = librosa.load(output_stream)
                tempo, _ = librosa.beat.beat_track(y=y, sr=sr)
                bpm = round(tempo[0])
                track = {
                    "id": track_id,
                    "bpm": bpm
                }
                result = json.dumps(track)
                print(f"Pushing to redis queue: {result}")
                r.lpush(RESULTS_QUEUE_NAME, result)
                print(f"Completed track {track_id} with BPM: {bpm}")
        except json.JSONDecodeError as e:
            print(f"JSON decode error: {e}")
            print(f"Item was: {repr(item)}")
        except Exception as e:
            print(f"Worker error during track analysis: {e}")
            import traceback
            traceback.print_exc()

main()
