"""Fusiona un JSON de entradas revisadas y valida antes de escribir."""
import json,sys
from pathlib import Path
from validate_catalog import validate

if __name__ == '__main__':
    if len(sys.argv) != 2: raise SystemExit('Uso: python scripts/import_manual.py entradas.json')
    target=Path(__file__).resolve().parents[1]/'catalog/games.json'
    c=json.loads(target.read_text(encoding='utf-8'))
    incoming=json.loads(Path(sys.argv[1]).read_text(encoding='utf-8'))
    assert isinstance(incoming,list)
    assert len({g['id'] for g in incoming})==len(incoming), 'IDs repetidos en la importación'
    merged={g['id']:g for g in c['games']}
    merged.update({g['id']:g for g in incoming})
    c['games']=list(merged.values());c['version']+=1
    validate(c)
    temp=target.with_suffix('.tmp')
    temp.write_text(json.dumps(c,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    temp.replace(target)
    print(f"Catálogo v{c['version']}: {len(c['games'])} juegos")
