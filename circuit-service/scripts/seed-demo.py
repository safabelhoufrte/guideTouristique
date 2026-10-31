"""Opt-in local demo data through the Circuit API. Existing records are never changed."""
import argparse
import json
from urllib.request import Request, urlopen

parser = argparse.ArgumentParser()
parser.add_argument('--url', default='http://localhost:8082')
args = parser.parse_args()
endpoint = args.url.rstrip('/') + '/api/circuits'
with urlopen(endpoint, timeout=10) as response:
    existing = {circuit['titre'] for circuit in json.load(response)}

circuits = [
    dict(titre='Médina et Kasbah', ville='Agadir', dureeHeures=4, prix=250, guideId=1,
         description='Découvrez les ruelles de la médina, les artisans et la Kasbah avec votre guide local. Une promenade entre histoire, rencontres et vues sur la baie.'),
    dict(titre='Vallée du Paradis', ville='Agadir', dureeHeures=6, prix=400, guideId=1,
         description='Partez à la découverte de la vallée, de ses palmiers et des paysages de l’Atlas. Une journée au rythme de la nature, accompagnée par votre guide.'),
    dict(titre='Les jardins de Marrakech', ville='Marrakech', dureeHeures=3, prix=200, guideId=2,
         description='Une parenthèse dans les jardins de Marrakech, à la découverte de leur architecture et de leurs allées ombragées.'),
]
for circuit in circuits:
    if circuit['titre'] in existing:
        continue
    request = Request(endpoint, data=json.dumps(circuit).encode('utf-8'),
                      headers={'Content-Type': 'application/json'}, method='POST')
    with urlopen(request, timeout=10) as response:
        print('Created', json.load(response)['id'], circuit['titre'])
