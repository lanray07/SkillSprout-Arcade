import unittest,json,hashlib,subprocess,sys
from pathlib import Path
import localise
import review_content
import extract_ui_strings
import content_delivery
from cryptography.hazmat.primitives.asymmetric import ec
from cryptography.hazmat.primitives import hashes
import base64
ROOT=Path(__file__).resolve().parents[1]
class ToolTests(unittest.TestCase):
    def test_content_hash_and_answers(self):
        path=ROOT/'app/src/main/assets/packs/starter.json';data=path.read_bytes();manifest=json.loads(path.with_name('manifest.json').read_text());pack=json.loads(data)
        self.assertEqual(hashlib.sha256(data).hexdigest(),manifest['sha256']);self.assertEqual(review_content.validate(pack),444)
    def test_no_fabricated_approval(self):
        pack=json.loads((ROOT/'app/src/main/assets/packs/starter.json').read_text('utf-8'))
        self.assertTrue(all(a['reviewStatus']=='Awaiting Teacher Review' for a in pack['activities']))
    def test_agreed_year1_corrections_are_scoped_and_versioned(self):
        from content_expansion import YEAR1_LOGIC_HINTS,YEAR1_MATHS_CORRECTIONS,YEAR1_REVIEW25_36,YEAR1_REVIEW37_48,YEAR1_REVIEW49_60,YEAR1_REVIEW61_72,YEAR1_REVIEW73_74,YEAR2_REVIEW1_12,YEAR2_REVIEW13_24,YEAR2_REVIEW25_36,YEAR2_REVIEW37_48,YEAR2_REVIEW49_60
        pack=json.loads((ROOT/'app/src/main/assets/packs/starter.json').read_text('utf-8'))
        changed=[a for a in pack['activities'] if a['id'] in YEAR1_LOGIC_HINTS]
        self.assertEqual(12,len(changed));self.assertGreaterEqual(pack['version'],14)
        for a in changed:
            self.assertEqual(('LOGIC',1,3),(a['game'],a['year'],a['version']))
            self.assertEqual(['Find the smallest number first.',YEAR1_LOGIC_HINTS[a['id']]],a['hints'])
        maths=[a for a in pack['activities'] if a['id'] in YEAR1_MATHS_CORRECTIONS]
        self.assertEqual(12,len(maths))
        for a in maths:
            self.assertEqual(('MATHS',1,3),(a['game'],a['year'],a['version']))
            for field,value in YEAR1_MATHS_CORRECTIONS[a['id']].items():self.assertEqual(value,a[field])
        next_batch=[a for a in pack['activities'] if a['id'] in YEAR1_REVIEW25_36]
        self.assertEqual(12,len(next_batch))
        for a in next_batch:
            self.assertEqual(('MATHS',1,3),(a['game'],a['year'],a['version']))
            for field,value in YEAR1_REVIEW25_36[a['id']].items():self.assertEqual(value,a[field])
        english=[a for a in pack['activities'] if a['id'] in YEAR1_REVIEW37_48]
        self.assertEqual(12,len(english))
        self.assertEqual({'SPELLING','STORY'},{a['game'] for a in english})
        for a in english:
            self.assertEqual((1,3),(a['year'],a['version']))
            for field,value in YEAR1_REVIEW37_48[a['id']].items():self.assertEqual(value,a[field])
        grouping=[a for a in pack['activities'] if a['id'] in YEAR1_REVIEW49_60]
        self.assertEqual(12,len(grouping))
        self.assertEqual({'STORY','TABLES'},{a['game'] for a in grouping})
        for a in grouping:
            self.assertEqual((1,3),(a['year'],a['version']))
            for field,value in YEAR1_REVIEW49_60[a['id']].items():self.assertEqual(value,a[field])
        final_batch=[a for a in pack['activities'] if a['id'] in YEAR1_REVIEW61_72]
        self.assertEqual(12,len(final_batch))
        self.assertEqual({'TABLES','WORDS'},{a['game'] for a in final_batch})
        for a in final_batch:
            self.assertEqual((1,3),(a['year'],a['version']))
            for field,value in YEAR1_REVIEW61_72[a['id']].items():self.assertEqual(value,a[field])
        last_two=[a for a in pack['activities'] if a['id'] in YEAR1_REVIEW73_74]
        self.assertEqual(2,len(last_two))
        for a in last_two:
            self.assertEqual(('WORDS',1,3),(a['game'],a['year'],a['version']))
            for field,value in YEAR1_REVIEW73_74[a['id']].items():self.assertEqual(value,a[field])
        year2=[a for a in pack['activities'] if a['id'] in YEAR2_REVIEW1_12]
        self.assertEqual(12,len(year2))
        for a in year2:
            self.assertEqual(('LOGIC',2,3),(a['game'],a['year'],a['version']))
            for field,value in YEAR2_REVIEW1_12[a['id']].items():self.assertEqual(value,a[field])
        year2_maths=[a for a in pack['activities'] if a['id'] in YEAR2_REVIEW13_24]
        self.assertEqual(12,len(year2_maths))
        for a in year2_maths:
            self.assertEqual(('MATHS',2,3),(a['game'],a['year'],a['version']))
            for field,value in YEAR2_REVIEW13_24[a['id']].items():self.assertEqual(value,a[field])
        year2_next=[a for a in pack['activities'] if a['id'] in YEAR2_REVIEW25_36]
        self.assertEqual(12,len(year2_next))
        for a in year2_next:
            self.assertEqual(('MATHS',2,3),(a['game'],a['year'],a['version']))
            for field,value in YEAR2_REVIEW25_36[a['id']].items():self.assertEqual(value,a[field])
        year2_english=[a for a in pack['activities'] if a['id'] in YEAR2_REVIEW37_48]
        self.assertEqual(12,len(year2_english))
        self.assertEqual({'SPELLING','STORY'},{a['game'] for a in year2_english})
        for a in year2_english:
            self.assertEqual((2,3),(a['year'],a['version']))
            for field,value in YEAR2_REVIEW37_48[a['id']].items():self.assertEqual(value,a[field])
        year2_groups=[a for a in pack['activities'] if a['id'] in YEAR2_REVIEW49_60]
        self.assertEqual(12,len(year2_groups))
        self.assertEqual({'STORY','TABLES'},{a['game'] for a in year2_groups})
        for a in year2_groups:
            self.assertEqual((2,3),(a['year'],a['version']))
            for field,value in YEAR2_REVIEW49_60[a['id']].items():self.assertEqual(value,a[field])
        revised=set(YEAR1_LOGIC_HINTS)|set(YEAR1_MATHS_CORRECTIONS)|set(YEAR1_REVIEW25_36)|set(YEAR1_REVIEW37_48)|set(YEAR1_REVIEW49_60)|set(YEAR1_REVIEW61_72)|set(YEAR1_REVIEW73_74)|set(YEAR2_REVIEW1_12)|set(YEAR2_REVIEW13_24)|set(YEAR2_REVIEW25_36)|set(YEAR2_REVIEW37_48)|set(YEAR2_REVIEW49_60)
        from batch_content_review import in_scope
        self.assertTrue(all(a['version']==2 for a in pack['activities'] if a['id'] not in revised and not in_scope(a)))
        self.assertTrue(all(a['version']==3 for a in pack['activities'] if in_scope(a)))
    def test_visual_metadata_and_revised_subtraction_are_valid(self):
        pack=json.loads((ROOT/'app/src/main/assets/packs/starter.json').read_text('utf-8'))
        items={a['id']:a for a in pack['activities']}
        revised=items['dev-maths-y1-0013']
        self.assertEqual(('20 − 7 = ?','13'),(revised['prompt'],revised['answer']))
        self.assertEqual(['counting'],items['dev-maths-y1-0401']['prerequisites'])
        for visual in ['clock:13:00','clock:3:60','clock:3:0','fruit:0:7','fruit:4:11','fruit:4:7:1','bus-trips:-1:27','bus-trips:18:1000','bus-trips:18:27:1']:
            bad=json.loads(json.dumps(pack));bad['activities'][0]['visual']=visual
            with self.assertRaises(AssertionError):review_content.validate(bad)
    def test_release_fails_closed(self):
        result=subprocess.run([sys.executable,str(ROOT/'tools/verify_release.py')],capture_output=True,text=True)
        self.assertEqual(result.returncode,1);self.assertIn('RELEASE BLOCKED',result.stdout)
    def test_placeholders_preserve_order_independently(self):
        self.assertEqual(localise.placeholders('%1$s earned %2$d stars, {nickname}'),localise.placeholders('{nickname} : %2$d étoiles pour %1$s'))
        self.assertNotEqual(localise.placeholders('%1$s'),localise.placeholders('%1$d'))
    def test_listing_limits(self):
        listing=json.loads((ROOT/'marketing/listing.en-GB.json').read_text('utf-8'))
        self.assertLessEqual(len(listing['shortDescription'].encode()),2000);self.assertLessEqual(len(listing['longDescription']),4000);self.assertTrue(3<=len(listing['featureBullets'])<=5)
    def test_template_extraction_preserves_expressions_and_percentages(self):
        source='Text("${child.nickname}: ${items.count { it.correct }} of $total (100%)")'
        resources={};converted=extract_ui_strings.migrate(source,resources)
        self.assertIn('child.nickname,items.count { it.correct },total',converted)
        self.assertEqual(next(iter(resources.values()))[0],'%1$s: %2$s of %3$s (100%%)')
        self.assertEqual(extract_ui_strings.migrate(converted,resources),converted)
    def test_resource_escaping_round_trips_quotes_and_newlines(self):
        text='Pip\'s "map"\nA new path \\ one'
        self.assertEqual(localise.android_text(extract_ui_strings.android_escape(text)),text)
    def test_reviewed_languages_can_add_their_own_plural_quantities(self):
        source={'count[one]':'%1$d star','count[other]':'%1$d stars'}
        target={**source,'count[two]':'%1$d stars','count[few]':'%1$d stars','count[many]':'%1$d stars','count[zero]':'%1$d stars'}
        self.assertEqual(localise.validate_catalog(source,target),[])
        self.assertTrue(localise.validate_catalog(source,{**target,'count[invalid]':'%1$d stars'}))
        self.assertTrue(localise.validate_catalog(source,{**target,'count[two]':'%1$s stars'}))
    def test_curriculum_extension_has_support_and_an_acyclic_skill_graph(self):
        pack=json.loads((ROOT/'app/src/main/assets/packs/starter.json').read_text('utf-8'))
        items=pack['activities'];skills={a['skill'] for a in items}
        self.assertTrue(all(p in skills for a in items for p in a['prerequisites']))
        self.assertTrue({'ratio','algebra','percentages','volume','area','decimals'}.issubset(skills))
        self.assertGreaterEqual(len({a['story'] for a in items if a['story']}),8)
        self.assertFalse(any(a['prompt'].startswith('Build this word:') for a in items))
        cycle=json.loads(json.dumps(pack));cycle['activities'][0]['prerequisites']=[cycle['activities'][0]['skill']]
        with self.assertRaises(AssertionError):review_content.validate(cycle)
    def test_locale_review_is_bound_to_source_and_requires_arabic_plurals(self):
        source={'stars[one]':'%1$d star','stars[other]':'%1$d stars'}
        target={f'stars[{q}]':'%1$d نجمة' for q in ['zero','one','two','few','many','other']}
        review={'locale':'ar','reviewer':'Test reviewer','reviewedAt':'2026-01-01','evidence':'Test fixture only','status':'Approved','sourceSha256':localise.digest(source),'sha256':localise.digest(target)}
        self.assertEqual([],localise.review_errors(source,target,review,'ar'))
        self.assertIn('stale-review',localise.review_errors({**source,'new':'New'},target,review,'ar'))
        del target['stars[two]']
        self.assertIn('plural:stars[two]',localise.review_errors(source,target,review,'ar'))
        self.assertTrue(localise.validate_catalog({'a':'Text'},{'a':' '}))
    def test_all_formatter_tokens_and_literal_percent_survive_translation(self):
        text='%1$,.2f / %2$04d / %3$tY %%'
        self.assertEqual(['%%','%1$,.2f','%2$04d','%3$tY'],localise.placeholders(text))
        self.assertNotEqual(localise.placeholders(text),localise.placeholders(text.replace('%%','%')))
    def test_signed_pack_binds_manifest_and_payload_without_approving_content(self):
        key=ec.generate_private_key(ec.SECP256R1())
        pack=json.loads((ROOT/'app/src/main/assets/packs/starter.json').read_text('utf-8'))
        pack['activities']=pack['activities'][:1];pack['activities'][0]['id']='test-pack:one'
        payload=json.dumps(pack).encode()
        with self.assertRaises(ValueError):content_delivery.envelope(payload,'test-pack',1,key)
        result=content_delivery.envelope(payload,'test-pack',1,key,development=True)
        key.public_key().verify(base64.b64decode(result['signature']),content_delivery.message(result['manifest']),ec.ECDSA(hashes.SHA256()))
        self.assertEqual(result['manifest']['sha256'],hashlib.sha256(payload).hexdigest())
        self.assertEqual(pack['activities'][0]['reviewStatus'],'Awaiting Teacher Review')
if __name__=='__main__':unittest.main()
