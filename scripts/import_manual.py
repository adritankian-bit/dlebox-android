"""Fusiona un JSON de entradas revisadas y valida antes de escribir."""
import json,sys,subprocess
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
    manual_path=target.parent/'sources/manual-games.json'
    manual_path.parent.mkdir(parents=True,exist_ok=True)
    manual={g['id']:g for g in json.loads(manual_path.read_text(encoding='utf-8'))} if manual_path.exists() else {}
    manual.update({g['id']:g for g in incoming})
    manual_temp=manual_path.with_suffix('.tmp')
    manual_temp.write_text(json.dumps(list(manual.values()),ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    manual_temp.replace(manual_path)
    temp=target.with_suffix('.tmp')
    temp.write_text(json.dumps(c,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    temp.replace(target)
    if (manual_path.parent/'manifest.json').exists():
        subprocess.run([sys.executable,str(Path(__file__).with_name('import_cc0.py'))],check=True)
    print(f"Catálogo v{c['version']}: {len(c['games'])} juegos")
