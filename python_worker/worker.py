import librosa
import requests
import io
import redis
import os
from pydub import AudioSegment
import time
import json

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
                track_id = item["id"]
                url = item["preview_url"]
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
                r.lpush(RESULTS_QUEUE_NAME, result)
                time.sleep(5)
        except Exception as e:
            print(f"Worker error during track analysis: {e}")

main()
