import json
import unittest
from fire_isolated_checks import restored_catalogue


class FireCatalogueTests(unittest.TestCase):
    def test_original_bytes_are_restored_without_reformatting(self):
        original=b'{ "other.product": {"price": 1} }\r\n'
        merged=json.dumps({'other.product':{'price':1},'com.skillsprout.arcade.family.monthly':{'price':4.99}}).encode()
        restored,exact=restored_catalogue(merged,original,merged)
        self.assertEqual(original,restored)
        self.assertTrue(exact)

    def test_concurrent_unrelated_changes_survive_and_owned_entries_revert(self):
        monthly='com.skillsprout.arcade.family.monthly'
        annual='com.skillsprout.arcade.family.annual'
        original=json.dumps({'other.product':{'price':1},monthly:{'price':2}}).encode()
        merged=json.dumps({'other.product':{'price':1},monthly:{'price':4.99},annual:{'price':29.99}}).encode()
        concurrent=json.dumps({'other.product':{'price':3},'new.other.product':{},monthly:{'price':4.99},annual:{'price':29.99}}).encode()
        restored,exact=restored_catalogue(concurrent,original,merged)
        self.assertEqual({'other.product':{'price':3},'new.other.product':{},monthly:{'price':2}},json.loads(restored))
        self.assertFalse(exact)

    def test_invalid_concurrent_data_is_not_silently_replaced(self):
        with self.assertRaises(json.JSONDecodeError):restored_catalogue(b'partial concurrent write',b'{}',b'{"test":1}')
