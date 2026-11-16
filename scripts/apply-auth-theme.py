"""Apply the login theme to an existing local realm without importing users or credentials."""
import json
from pathlib import Path
from http.client import HTTPException
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import Request, urlopen

root = Path(__file__).resolve().parent.parent
values = dict(
    line.split('=', 1) for line in (root / '.env').read_text(encoding='utf-8').splitlines()
    if line and not line.startswith('#') and '=' in line
)
password = values.get('KEYCLOAK_ADMIN_PASSWORD')
if not password:
    raise SystemExit('Run scripts/setup-local-auth.py first.')
server = 'http://localhost:8180'

def request(path, data=None, token=None, method=None):
    headers = {}
    if token:
        headers['Authorization'] = 'Bearer ' + token
        headers['Content-Type'] = 'application/json'
    else:
        headers['Content-Type'] = 'application/x-www-form-urlencoded'
    with urlopen(Request(server + path, data=data, headers=headers, method=method), timeout=15) as response:
        content = response.read()
        return json.loads(content) if content else None

try:
    token = request('/realms/master/protocol/openid-connect/token', urlencode({
        'grant_type': 'password', 'client_id': 'admin-cli',
        'username': 'local-admin', 'password': password,
    }).encode())['access_token']
    realm_path = '/admin/realms/guide-touristique'
    request(realm_path, json.dumps({
        'loginTheme': 'guide-touristique', 'internationalizationEnabled': True,
        'supportedLocales': ['fr', 'en'], 'defaultLocale': 'fr',
    }).encode(), token, 'PUT')
    clients = request(realm_path + '/clients?clientId=guide-touristique-web', token=token)
    if len(clients) != 1:
        raise SystemExit('Expected the existing guide-touristique-web client.')
    request(realm_path + '/clients/' + clients[0]['id'],
            json.dumps({'baseUrl': 'http://localhost:4200/'}).encode(), token, 'PUT')
    realm = request(realm_path, token=token)
    if realm.get('loginTheme') != 'guide-touristique':
        raise SystemExit('The theme setting was not saved.')
except (HTTPError, URLError, HTTPException, TimeoutError):
    raise SystemExit('Could not apply the local theme. Check that Keycloak is running and the local admin credentials are current.') from None
print('Guide Touristique login theme enabled. Accounts, roles and SMTP settings preserved.')
