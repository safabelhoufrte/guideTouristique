"""Python caller -> Java Circuit -> Python Guide, in Docker demo mode."""
import argparse
import json
from urllib.request import Request, urlopen

parser = argparse.ArgumentParser()
parser.add_argument('--url', default='http://localhost:18092')
args = parser.parse_args()
base = args.url.rstrip('/')
body = json.dumps({'titre': 'Tour Agadir', 'prix': 300.50, 'guideId': 1, 'disponible': True}).encode()
request = Request(base + '/circuits', data=body, headers={'Content-Type': 'application/json'}, method='POST')
with urlopen(request, timeout=10) as response:
    circuit = json.load(response)
    assert response.status == 201
print('Created circuit:', circuit['id'])
with urlopen(base + '/circuits/' + str(circuit['id']) + '/reservation-details', timeout=10) as response:
    details = json.load(response)
assert details['disponible'] is True and details['guideId'] == 1
print(json.dumps(details, ensure_ascii=False, indent=2))
print('Success: Python client -> Spring Boot Circuit -> Python Guide fixture')
