import unittest
from offline_translate import protected_parts,translate_catalog

class OfflineTranslationTests(unittest.TestCase):
    def test_repeated_and_numbered_placeholders_and_names_are_preserved(self):
        source={'message':'Pip has %1$d stars in Sproutland. %1$d!','other':'%1$s %2$d'}
        result=translate_catalog(source,lambda texts:[s.upper() for s in texts])
        self.assertEqual('Pip HAS %1$d STARS IN Sproutland. %1$d!',result['message'])
        self.assertEqual(source['other'],result['other'])
    def test_whitespace_punctuation_and_duplicate_fragments_survive(self):
        result=translate_catalog({'one':'  Next\n','two':'Next','three':'50% →'},lambda texts:['Suivant' for _ in texts])
        self.assertEqual('  Suivant\n',result['one']);self.assertEqual('Suivant',result['two'])
        self.assertEqual('50% →',result['three'])
    def test_blank_or_missing_model_results_are_rejected(self):
        for output in [[],['']]:
            with self.assertRaises(ValueError):translate_catalog({'next':'Next'},lambda _:output)
    def test_model_cannot_introduce_format_placeholders(self):
        with self.assertRaises(ValueError):translate_catalog({'next':'Next'},lambda _:['%1$s'])
    def test_all_source_bytes_can_be_reassembled_before_translation(self):
        text='Pip: %1$d / %2$s\nSkillSprout Arcade — 100%%'
        self.assertEqual(text,''.join(s for s,_ in protected_parts(text)))
    def test_decorative_symbols_do_not_reach_the_language_model(self):
        seen=[]
        def model(texts):seen.extend(texts);return ['미션' for _ in texts]
        self.assertEqual('⚑  미션',translate_catalog({'nav':'⚑  Missions'},model)['nav'])
        self.assertEqual(['Missions'],seen)
    def test_support_addresses_and_policy_urls_survive_translation(self):
        source='Visit https://example.org/privacy/ or email support@example.org.'
        result=translate_catalog({'support':source},lambda texts:[s.upper() for s in texts])['support']
        self.assertIn('https://example.org/privacy/',result)
        self.assertIn('support@example.org',result)
