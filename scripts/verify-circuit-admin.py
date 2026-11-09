"""Local acceptance check against real Keycloak, Guide, Circuit and the web proxy.

Run only on a development installation. Creates its own guide/circuit and archives
them afterwards. Tokens and credentials stay in memory and are never printed.
"""
import argparse
import base64
import hashlib
from http.client import HTTPException
from html.parser import HTMLParser
from http.cookiejar import CookieJar, DefaultCookiePolicy
import json
from pathlib import Path
import secrets
from urllib.error import HTTPError
from urllib.parse import parse_qs, urlencode, urlparse
from urllib.request import (
    HTTPRedirectHandler, HTTPCookieProcessor, Request, build_opener, urlopen,
)

WEB = 'http://localhost:4200'
OIDC = 'http://localhost:8180/realms/guide-touristique/protocol/openid-connect'


class LoginForm(HTMLParser):
    action = None

    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        if tag == 'form' and attrs.get('id') == 'kc-form-login':
            self.action = attrs.get('action')


class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


def login(username, password):
    # urllib does not share browsers' localhost exception for Secure cookies.
    cookies = CookieJar(DefaultCookiePolicy(secure_protocols=('https', 'http')))
    opener = build_opener(HTTPCookieProcessor(cookies), NoRedirect())
    verifier = secrets.token_urlsafe(48)
    challenge = base64.urlsafe_b64encode(hashlib.sha256(verifier.encode()).digest()).decode().rstrip('=')
    state = secrets.token_urlsafe(24)
    redirect = WEB + '/administration/circuits'
    auth = OIDC + '/auth?' + urlencode({
        'client_id': 'guide-touristique-web', 'redirect_uri': redirect,
        'response_type': 'code', 'scope': 'openid', 'state': state,
        'code_challenge': challenge, 'code_challenge_method': 'S256',
    })
    with opener.open(auth, timeout=20) as response:
        form = LoginForm()
        form.feed(response.read().decode())
    if not form.action or urlparse(form.action).netloc != 'localhost:8180':
        raise RuntimeError('Expected the local Keycloak login form.')
    payload = urlencode({'username': username, 'password': password, 'credentialId': ''}).encode()
    try:
        with opener.open(Request(form.action, data=payload), timeout=20):
            raise RuntimeError('Keycloak did not return an authorization code.')
    except HTTPError as error:
        if error.code != 302:
            raise RuntimeError(f'Keycloak login returned HTTP {error.code}.') from None
        callback = urlparse(error.headers['Location'])
    query = parse_qs(callback.query)
    if (callback.scheme + '://' + callback.netloc + callback.path != redirect
            or query.get('state') != [state] or not query.get('code')):
        raise RuntimeError('Invalid OIDC callback or state.')
    payload = urlencode({
        'grant_type': 'authorization_code', 'client_id': 'guide-touristique-web',
        'code': query['code'][0], 'redirect_uri': redirect, 'code_verifier': verifier,
    }).encode()
    with urlopen(Request(OIDC + '/token', data=payload), timeout=20) as response:
        return json.load(response)['access_token']


def api(method, path, expected, token=None, body=None):
    headers = {'Accept': 'application/json'}
    if token:
        headers['Authorization'] = 'Bearer ' + token
    if body is not None:
        headers['Content-Type'] = 'application/json'
    req = Request(WEB + '/api' + path, method=method, headers=headers,
                  data=None if body is None else json.dumps(body).encode())
    try:
        with urlopen(req, timeout=15) as response:
            status, raw = response.status, response.read()
    except HTTPError as error:
        status, raw = error.code, error.read()
    if status != expected:
        raise RuntimeError(f'{method} {path}: expected {expected}, got {status}.')
    return json.loads(raw) if raw else None


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--env-file', type=Path, default=Path(__file__).resolve().parent.parent / '.env')
    args = parser.parse_args()
    env = dict(line.split('=', 1) for line in args.env_file.read_text(encoding='utf-8').splitlines()
               if line.strip() and not line.lstrip().startswith('#') and '=' in line)
    password = env.get('KEYCLOAK_DEMO_PASSWORD', '')
    if not password or password.startswith('replace-'):
        raise SystemExit('Prepare local credentials with scripts/setup-local-auth.py first.')
    admin = login('demo-admin', password)
    tourist = login('demo-touriste', password)
    print('PASS real Keycloak sign-in with authorization code and PKCE (ADMIN and USER).')
    guide_id = circuit_id = None
    try:
        guide = api('POST', '/guides', 201, admin, {
            'nom': 'Guide validation Circuit ' + secrets.token_hex(3),
            'ville': 'Agadir', 'langues': ['fr'], 'tarifParJour': 250,
            'disponible': True,
        })
        guide_id = guide['id']
        body = {'titre': 'Circuit validation ' + secrets.token_hex(3),
                'description': 'Validation locale du parcours administrateur.',
                'ville': 'Agadir', 'dureeHeures': 3, 'prix': 0, 'guideId': guide_id}
        api('POST', '/circuits', 401, body=body)
        api('POST', '/circuits', 403, tourist, body)
        api('POST', '/circuits', 400, admin, {**body, 'dureeHeures': None})
        api('POST', '/circuits', 400, admin, {**body, 'guideId': 9223372036854775807})
        created = api('POST', '/circuits', 201, admin, body)
        circuit_id = created['id']
        if created != {**body, 'id': circuit_id}:
            raise RuntimeError('Created circuit differs from submitted data.')
        path = f'/circuits/{circuit_id}'
        if api('GET', path, 200) != created:
            raise RuntimeError('Public detail does not show the created circuit.')
        if circuit_id not in [c['id'] for c in api('GET', '/circuits', 200)]:
            raise RuntimeError('Created circuit is absent from the public catalogue.')
        print('PASS real Guide lookup, ADMIN creation, public detail/catalogue, zero price and 400/401/403 refusals.')
        body = {**body, 'titre': body['titre'] + ' modifié', 'prix': 275.50}
        api('PUT', path, 403, tourist, body)
        changed = api('PUT', path, 200, admin, body)
        if changed != {**body, 'id': circuit_id} or api('GET', path, 200) != changed:
            raise RuntimeError('Updated circuit does not retain its ID and submitted data.')
        api('DELETE', path, 403, tourist)
        api('DELETE', path, 204, admin)
        api('GET', path, 404)
        if circuit_id in [c['id'] for c in api('GET', '/circuits', 200)]:
            raise RuntimeError('Archived circuit is still public.')
        print(f'PASS ADMIN update and archive; circuit {circuit_id} is absent from public reads.')
    finally:
        # Only archive records created by this check, never pre-existing records.
        if circuit_id is not None:
            try:
                api('GET', f'/circuits/{circuit_id}', 404)
            except RuntimeError:
                api('DELETE', f'/circuits/{circuit_id}', 204, admin)
        if guide_id is not None:
            api('DELETE', f'/guides/{guide_id}', 204, admin)


if __name__ == '__main__':
    try:
        main()
    except (OSError, HTTPException, RuntimeError) as error:
        raise SystemExit(f'FAIL: {error} Check that the local services are ready.') from None
