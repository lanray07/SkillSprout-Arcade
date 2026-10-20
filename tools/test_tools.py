import unittest,json,hashlib,subprocess,sys
from pathlib import Path
import localise
import review_content
ROOT=Path(__file__).resolve().parents[1]
class ToolTests(unittest.TestCase):
    def test_content_hash_and_answers(self):
        path=ROOT/'app/src/main/assets/packs/starter.json';data=path.read_bytes();manifest=json.loads(path.with_name('manifest.json').read_text());pack=json.loads(data)
        self.assertEqual(hashlib.sha256(data).hexdigest(),manifest['sha256']);self.assertEqual(review_content.validate(pack),396)
    def test_no_fabricated_approval(self):
        pack=json.loads((ROOT/'app/src/main/assets/packs/starter.json').read_text('utf-8'))
        self.assertTrue(all(a['reviewStatus']=='Awaiting Teacher Review' for a in pack['activities']))
    def test_release_fails_closed(self):
        result=subprocess.run([sys.executable,str(ROOT/'tools/verify_release.py')],capture_output=True,text=True)
        self.assertEqual(result.returncode,1);self.assertIn('RELEASE BLOCKED',result.stdout)
    def test_placeholders_preserve_order_independently(self):
        self.assertEqual(localise.placeholders('%1$s earned %2$d stars, {nickname}'),localise.placeholders('{nickname} : %2$d étoiles pour %1$s'))
        self.assertNotEqual(localise.placeholders('%1$s'),localise.placeholders('%1$d'))
    def test_listing_limits(self):
        listing=json.loads((ROOT/'marketing/listing.en-GB.json').read_text('utf-8'))
        self.assertLessEqual(len(listing['shortDescription'].encode()),2000);self.assertLessEqual(len(listing['longDescription']),4000);self.assertTrue(3<=len(listing['featureBullets'])<=5)
if __name__=='__main__':unittest.main()
