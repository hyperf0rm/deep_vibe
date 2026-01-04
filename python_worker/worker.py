import librosa
import requests
import io
import redis
import os
from pydub import AudioSegment

QUEUE_NAME = "analyze_audio_task_queue"

def main():
    redis_host = os.getenv('REDIS_HOST', 'localhost')

    print(f"Connecting to Redis at: {redis_host}")

    try:
        r = redis.Redis(host=redis_host, port=6379, decode_responses=True)
        r.ping()
        print("Connected to Redis successfully")
    except Exception as e:
        print(f"Failed to connect to Redis: {e}")



main()

