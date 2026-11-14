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

if __name__=='__main__': unittest.main()
