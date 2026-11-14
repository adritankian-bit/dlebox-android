"""Valida el catálogo sin dependencias externas. No hace solicitudes de red."""
import datetime
import json
import re
import sys
from pathlib import Path
from urllib.parse import urlsplit

def url(value):
    if not isinstance(value, str): return False
    try:
        p = urlsplit(value)
        return p.scheme == 'https' and bool(p.hostname) and p.username is None and p.password is None and p.port in (None,443) and not any(c.isspace() for c in value)
    except ValueError: return False

def validate(c):
    assert c['schemaVersion'] == 1 and type(c['version']) is int and c['version'] > 0
    assert 0 < len(c['games']) <= 5000
    ids, urls = set(), set()
    for g in c['games']:
        assert re.fullmatch(r'[a-z0-9][a-z0-9-]{0,79}',g['id']) and g['id'] not in ids, 'ID duplicado o inválido'
        ids.add(g['id'])
        assert all(isinstance(g[k], str) and g[k].strip() for k in ('name','description','category','thumbnailSource'))
        assert url(g['gameUrl']), 'URL inválida'
        u = urlsplit(g['gameUrl'])
        canonical = (u.hostname.lower(),u.path.rstrip('/'),u.query)
        assert canonical not in urls, 'URL duplicada'
        urls.add(canonical)
        assert g.get('sourceUrl') is None or url(g['sourceUrl'])
        assert g['thumbnailType'] in ('generated','official','authorized')
        assert g.get('thumbnailUrl') is None or (url(g['thumbnailUrl']) and g.get('thumbnailPermission') and g['thumbnailType'] != 'generated')
        assert g['linkStatus'] in ('verified','unverified','broken')
        datetime.date.fromisoformat(g['addedAt'])
        if g.get('verifiedAt'): datetime.date.fromisoformat(g['verifiedAt'])
        assert g['linkStatus'] != 'verified' or g.get('verifiedAt')
    return len(ids),len({g['category'] for g in c['games']})

if __name__ == '__main__':
    path = Path(sys.argv[1]) if len(sys.argv)>1 else Path(__file__).resolve().parents[1]/'catalog/games.json'
    n,k=validate(json.loads(path.read_text(encoding='utf-8')))
    print(f'OK: {n} juegos, {k} categorías')
