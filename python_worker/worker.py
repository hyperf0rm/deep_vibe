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
        url = "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview115/v4/30/f6/a2/30f6a256-5e70-b374-81a0-74d8d5061dcf/mzaf_14751213315257693749.plus.aac.p.m4a"
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
        print(f"BPM: {bpm}")

        '''    r = redis.Redis(host="localhost", port=6379, decode_responses=True)

        while True:
        task = r.brpop(keys=[QUEUE_NAME], timeout=0)
        if task:
        queue, item = task
        print(f"queue: {queue}, task: {task}")'''


main()

