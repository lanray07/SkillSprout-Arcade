"""Local-only review desk. Serves public review/marketing artifacts, never private evidence or keys."""
from functools import partial
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
ALLOWED=[ROOT/'release/review-kit',ROOT/'marketing/localized']

class ReviewHandler(SimpleHTTPRequestHandler):
    def translate_path(self,path):
        target=Path(super().translate_path(path)).resolve()
        if any(target==folder or folder in target.parents for folder in ALLOWED):return str(target)
        return str(ROOT/'.review-not-served')
    def list_directory(self,path):
        self.send_error(403,'Directory listing disabled');return None

if __name__=='__main__':
    print('Review desk: http://127.0.0.1:4180/release/review-kit/index.html',flush=True)
    ThreadingHTTPServer(('127.0.0.1',4180),partial(ReviewHandler,directory=str(ROOT))).serve_forever()
