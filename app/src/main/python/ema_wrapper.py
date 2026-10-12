
import os
import threading
_tts = None
_lock = threading.Lock()
def get_tts():
    global _tts
    with _lock:
        if _tts is None:
            from ema_lightning import EMA
            _tts = EMA()
            print("EMA modeli yüklendi:", type(_tts))
            import inspect
            print("stream() imzası:", inspect.signature(_tts.stream))
            print("say() imzası:", inspect.signature(_tts.say))
        return _tts
def init_model():
    get_tts()
    return True
def get_sample_rate():
    return 48000
def synthesize_to_file(text: str, out_path: str):
    tts = get_tts()
    speech = tts.say(text, path=out_path)
    return speech.sample_rate, speech.duration
