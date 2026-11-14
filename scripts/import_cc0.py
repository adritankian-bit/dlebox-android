"""Importación reproducible CC0. No copia imágenes ni rastrea Listdle.

python scripts/import_cc0.py --fetch-source --verify
python scripts/import_cc0.py  # reconstruye sin red desde instantánea e informe
"""
import argparse, concurrent.futures, datetime, hashlib, json, re, threading, time
from pathlib import Path
from urllib.parse import urlsplit, urlunsplit
from urllib.request import Request, urlopen
from urllib.error import HTTPError
from validate_catalog import validate, url

ROOT=Path(__file__).resolve().parents[1]
SOURCES=ROOT/'catalog/sources'
REPO='guilyx/awesome-daily-minigames'
CATEGORIES={'word-guessing':'Palabras','word-connections':'Conexiones','crosswords':'Crucigramas','word-grids':'Letras','numbers-math':'Matemáticas','logic':'Lógica','geography':'Geografía','history':'Historia','music':'Música','movies-tv':'Cine y series','sports':'Deportes','video-games':'Videojuegos','trivia':'Cultura general','visual':'Visual y estimación','science-nature':'Ciencia y naturaleza','arcade':'Habilidad','board-card':'Mesa y cartas','hubs':'Colecciones'}

def canonical(value):
    p=urlsplit(value)
    return urlunsplit(('https',p.netloc.lower().removesuffix(':443'),p.path.rstrip('/'),p.query,''))

def fetch_source():
    with urlopen(f'https://api.github.com/repos/{REPO}/commits/main',timeout=30) as r: revision=json.load(r)['sha']
    base=f'https://raw.githubusercontent.com/{REPO}/{revision}'
    for remote,local in [('docs/games.json','awesome-daily-minigames.json'),('LICENSE','CC0-1.0.txt')]:
        with urlopen(f'{base}/{remote}',timeout=30) as r: raw=r.read()
        (SOURCES/local).write_bytes(raw)
    license_text=(SOURCES/'CC0-1.0.txt').read_text(encoding='utf-8')
    if 'CC0 1.0 Universal' not in license_text: raise ValueError('Licencia inesperada')
    previous=json.loads((ROOT/'catalog/games.json').read_text(encoding='utf-8'))
    (SOURCES/'manifest.json').write_text(json.dumps({'repository':f'https://github.com/{REPO}','revision':revision,'license':'CC0-1.0','catalogVersion':previous['version']+1,'importedAt':datetime.datetime.now(datetime.timezone.utc).date().isoformat(),'addedDates':{g['id']:g['addedAt'] for g in previous['games']},'snapshotSha256':hashlib.sha256((SOURCES/'awesome-daily-minigames.json').read_bytes()).hexdigest()},indent=2)+'\n',encoding='utf-8')

host_locks={}
def check(g):
    value=g['gameUrl']; host=urlsplit(value).hostname
    with host_locks[host]:
        time.sleep(1) # como máximo una petición por segundo por host
        result={'url':value,'checkedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'method':'HEAD'}
        try:
            try:
                response=urlopen(Request(value,method='HEAD',headers={'User-Agent':'DleBoxCatalog/2.0 (+https://github.com/adritankian-bit/dlebox-android)'}),timeout=18)
            except HTTPError as e:
                if e.code not in (405,501):raise
                result['method']='GET-range'
                response=urlopen(Request(value,headers={'User-Agent':'DleBoxCatalog/2.0','Range':'bytes=0-2047'}),timeout=18)
            with response as r:
                result.update(httpStatus=r.status,finalUrl=r.url)
                result['state']='verified' if 200 <= r.status < 300 else 'unverified'
                if urlsplit(r.url).hostname in {'sedo.com','www.sedo.com','www.afternic.com','dan.com'}:result['state']='excluded'
        except HTTPError as e: result.update(httpStatus=e.code,state='excluded' if e.code in (404,410) or e.code>=500 else 'unverified',reason=f'HTTP {e.code}')
        except Exception as e:result.update(state='excluded',reason=type(e).__name__+': '+str(e))
        return g['id'],result

def candidates():
    manifest=json.loads((SOURCES/'manifest.json').read_text(encoding='utf-8'))
    raw=(SOURCES/'awesome-daily-minigames.json').read_bytes()
    if hashlib.sha256(raw).hexdigest()!=manifest['snapshotSha256']:raise ValueError('La instantánea no coincide con su SHA-256')
    if 'CC0 1.0 Universal' not in (SOURCES/'CC0-1.0.txt').read_text(encoding='utf-8'):raise ValueError('Licencia inesperada')
    source=json.loads(raw)
    legacy=json.loads((SOURCES/'legacy-v1.json').read_text(encoding='utf-8'))['games']
    by_url={canonical(g['gameUrl']):dict(g) for g in legacy}
    ids={g['id'] for g in legacy}
    source_url=f"https://github.com/{REPO}/blob/{manifest['revision']}/data/games.yml"
    for row in source['games']:
        if row.get('platform')!='web' or not url(row['url']):continue
        dest=canonical(row['url'])
        if dest in by_url:continue
        ident=row['id']
        if ident in ids:ident+='-'+hashlib.sha256(dest.encode()).hexdigest()[:8]
        ids.add(ident)
        by_url[dest]=dict(id=ident,name=re.sub(r'\s+',' ',row['name']).strip(),description=row['description'].strip(),category=CATEGORIES[row['category']],gameUrl=row['url'],sourceUrl=source_url,catalogSourceUrl=source_url,sourceRecordId=row['id'],sourceLicense='CC0-1.0',sourceRevision=manifest['revision'],thumbnailUrl=None,thumbnailType='generated',thumbnailSource='DleBox: composición geométrica original, no es una captura',thumbnailPermission=None,language=None,addedAt='2026-11-14',linkStatus='unverified',verifiedAt=None)
    by_id={g['id']:g for g in by_url.values()}
    manual_file=SOURCES/'manual-games.json'
    if manual_file.exists():
        for g in json.loads(manual_file.read_text(encoding='utf-8')):by_id[g['id']]=g
    return list(by_id.values())

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--fetch-source',action='store_true');parser.add_argument('--verify',action='store_true');args=parser.parse_args()
    SOURCES.mkdir(parents=True,exist_ok=True)
    if args.fetch_source:fetch_source()
    games=candidates()
    report_file=SOURCES/'link-checks.json'
    if args.verify:
        for g in games:host_locks.setdefault(urlsplit(g['gameUrl']).hostname,threading.Lock())
        with concurrent.futures.ThreadPoolExecutor(max_workers=4) as executor:checks=dict(executor.map(check,games))
        report_file.write_text(json.dumps(checks,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    checks=json.loads(report_file.read_text(encoding='utf-8'))
    manifest=json.loads((SOURCES/'manifest.json').read_text(encoding='utf-8'))
    selected=[]
    manual_file=SOURCES/'manual-games.json'
    manual_ids={g['id'] for g in json.loads(manual_file.read_text(encoding='utf-8'))} if manual_file.exists() else set()
    destinations={}
    duplicates=[]
    for g in games:
        check_result=checks.get(g['id'],{})
        if check_result.get('url')!=g['gameUrl']:check_result={}
        if check_result.get('state')=='excluded':continue
        if g['id'] not in manual_ids:g['addedAt']=manifest.get('addedDates',{}).get(g['id'],manifest.get('importedAt',g['addedAt']))
        destination=canonical(check_result.get('finalUrl',g['gameUrl']))
        if destination in destinations:
            duplicates.append({'removedId':g['id'],'keptId':destinations[destination],'finalUrl':destination})
            continue
        destinations[destination]=g['id']
        if check_result:
            g['linkStatus']=check_result.get('state','unverified')
            g['verifiedAt']=check_result['checkedAt'][:10] if g['linkStatus']=='verified' else None
            g['verificationMethod']=check_result.get('method')
            g['httpStatus']=check_result.get('httpStatus')
        selected.append(g)
    previous_version=json.loads((ROOT/'catalog/games.json').read_text(encoding='utf-8'))['version']
    catalog={'schemaVersion':1,'version':max(previous_version,manifest.get('catalogVersion',2)),'games':selected}
    validate(catalog)
    temporary=ROOT/'catalog/games.tmp'
    temporary.write_text(json.dumps(catalog,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    temporary.replace(ROOT/'catalog/games.json')
    summary={'candidates':len(games),'included':len(selected),'categories':len({g['category'] for g in selected}),'httpVerified':sum(g['linkStatus']=='verified' for g in selected),'pending':sum(g['linkStatus']!='verified' for g in selected),'excluded':len(games)-len(selected),'duplicates':duplicates}
    (SOURCES/'import-summary.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(summary,ensure_ascii=False))

if __name__=='__main__':main()
