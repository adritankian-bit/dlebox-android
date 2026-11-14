import copy,json,unittest
from pathlib import Path
from validate_catalog import validate

class CatalogTests(unittest.TestCase):
    def setUp(self): self.c=json.loads((Path(__file__).resolve().parents[1]/'catalog/games.json').read_text(encoding='utf-8'))
    def test_catalog(self):
        count,categories=validate(self.c)
        self.assertEqual(count,len(self.c['games']))
        self.assertGreater(categories,0)
    def test_duplicate_id(self):
        self.c['games'].append(copy.deepcopy(self.c['games'][0]))
        with self.assertRaises(AssertionError): validate(self.c)
    def test_duplicate_url(self):
        self.c['games'][1]['gameUrl']=self.c['games'][0]['gameUrl']+'/'
        with self.assertRaises(AssertionError): validate(self.c)
    def test_bad_urls(self):
        for u in ['javascript:alert(1)','http://example.com','https://user:pass@example.com','https://','https://a.com:99999']:
            with self.subTest(url=u):
                c=copy.deepcopy(self.c);c['games'][0]['gameUrl']=u
                with self.assertRaises(AssertionError): validate(c)
    def test_image_permission(self):
        self.c['games'][0]['thumbnailUrl']='https://example.com/image.jpg'
        with self.assertRaises(AssertionError):validate(self.c)
    def test_verification_date(self):
        self.c['games'][0]['verifiedAt']=None
        with self.assertRaises(AssertionError):validate(self.c)
    def test_legacy_ids_preserved(self):
        ids={g['id'] for g in self.c['games']}
        self.assertTrue({'wordle','waffle','globle','murdle','timeguessr','filmlink'} <= ids)
    def test_no_duplicate_names(self):
        names=[g['name'].strip().casefold() for g in self.c['games']]
        self.assertEqual(len(names),len(set(names)))
    def test_import_is_reproducible_offline(self):
        import subprocess,sys
        path=Path(__file__).resolve().parents[1]/'catalog/games.json'
        before=path.read_bytes()
        subprocess.run([sys.executable,str(Path(__file__).with_name('import_cc0.py'))],check=True,capture_output=True)
        self.assertEqual(before,path.read_bytes())
    def test_provenance_is_explicit(self):
        for g in self.c['games']:
            self.assertTrue(g.get('sourceUrl'))
            if g.get('catalogSourceUrl'):
                self.assertEqual('CC0-1.0',g['sourceLicense'])
                self.assertEqual(40,len(g['sourceRevision']))

if __name__=='__main__': unittest.main()
