"""Development fixture in a different language. Not the teammate's real Guide service."""
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import argparse
import json
import re

class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        match = re.fullmatch(r'/api/guides/(\d+)', self.path)
        guide_id = int(match.group(1)) if match else None
        if guide_id in (1, 2):
            status = 200
            body = {'id': guide_id, 'disponible': guide_id == 1,
                    'nom': 'Guide de démonstration', 'ville': 'Agadir', 'langues': ['Français']}
        else:
            status, body = 404, {'detail': 'Guide introuvable'}
        payload = json.dumps(body, ensure_ascii=False).encode('utf-8')
        self.send_response(status)
        self.send_header('Content-Type', 'application/json; charset=utf-8')
        self.send_header('Content-Length', str(len(payload)))
        self.end_headers()
        self.wfile.write(payload)

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--host', default='127.0.0.1')
    parser.add_argument('--port', type=int, default=8081)
    args = parser.parse_args()
    ThreadingHTTPServer((args.host, args.port), Handler).serve_forever()
