"""Python caller -> Java Circuit -> Python Guide, in Docker demo mode."""
import argparse
import json
import os
from urllib.request import Request, urlopen

parser = argparse.ArgumentParser()
parser.add_argument('--url', default='http://localhost:8082')
args = parser.parse_args()
token = os.environ.get('DEMO_ADMIN_TOKEN')
if not token:
    parser.error('Set DEMO_ADMIN_TOKEN to a current ADMIN access token (never commit it).')
base = args.url.rstrip('/')
body = json.dumps({'titre': 'Tour Agadir', 'description': 'Visite de la ville', 'ville': 'Agadir', 'dureeHeures': 4, 'prix': 300.50, 'guideId': 1}).encode()
request = Request(base + '/api/circuits', data=body,
                  headers={'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token}, method='POST')
with urlopen(request, timeout=10) as response:
    circuit = json.load(response)
    assert response.status == 201
print('Created circuit:', circuit['id'])
with urlopen(base + '/api/circuits/' + str(circuit['id']), timeout=10) as response:
    details = json.load(response)
assert details['guideId'] == 1
print(json.dumps(details, ensure_ascii=False, indent=2))
print('Success: Python client -> Spring Boot Circuit -> Python Guide fixture')
