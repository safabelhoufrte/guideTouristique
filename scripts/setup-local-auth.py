"""Generate local-only Keycloak credentials. Never commit .env or .local/."""
from pathlib import Path
import secrets

root = Path(__file__).resolve().parent.parent
path = root / '.env'
if not path.exists():
    raise SystemExit('Create .env from .env.example and set the business database passwords first.')
lines = path.read_text(encoding='utf-8').splitlines()
values = dict(line.split('=', 1) for line in lines if line and not line.startswith('#') and '=' in line)
for key in ('KEYCLOAK_DB_PASSWORD', 'KEYCLOAK_ADMIN_PASSWORD', 'KEYCLOAK_DEMO_PASSWORD'):
    if not values.get(key) or values[key].startswith('replace-'):
        value = secrets.token_urlsafe(24)
        lines = [line for line in lines if not line.startswith(key + '=')]
        lines.append(key + '=' + value)
        values[key] = value
path.write_text('\n'.join(lines) + '\n', encoding='utf-8')
local = root / '.local'
local.mkdir(exist_ok=True)
(local / 'demo-accounts.txt').write_text(
    'LOCAL DEVELOPMENT ONLY\nLogin: http://localhost:4200/connexion\n'
    + '\n'.join('demo-' + user + ' : ' + values['KEYCLOAK_DEMO_PASSWORD'] for user in ('touriste','guide','admin','autre'))
    + '\n\nKeycloak console: http://localhost:8180/admin\nlocal-admin : ' + values['KEYCLOAK_ADMIN_PASSWORD'] + '\n',
    encoding='utf-8')
print('Local credentials ready in .local/demo-accounts.txt (ignored by Git).')
