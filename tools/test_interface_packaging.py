import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
from xml.etree import ElementTree as ET
import localise
from package_interface_languages import catalogue,package,corrected
from check_interface_languages import check

class InterfacePackagingTests(unittest.TestCase):
    def test_indonesian_uses_legacy_android_resource_folder(self):
        self.assertEqual('values-in',localise.resource_folder('id'))
        self.assertEqual('values-b+zh+Hant',localise.resource_folder('zh-Hant'))
    def test_repetitive_model_output_is_rejected_before_packaging(self):
        empty={'sourceTexts':{},'locales':{}}
        for text in ['Zaman '*50,'一'*30]:
            with self.assertRaises(ValueError):corrected({'label':'Time'},{'label':text},'tr',empty,empty)
    def fixture(self,root):
        values=root/'app/src/main/res/values';values.mkdir(parents=True)
        (values/'strings.xml').write_text('<resources><string name="hello">Hello</string><string name="percent" formatted="false">100%</string><plurals name="count"><item quantity="one">%1$d discovery</item><item quantity="other">%1$d discoveries</item></plurals></resources>',encoding='utf-8')
        (values/'languages.xml').write_text('<resources><string-array name="interface_locales" translatable="false"><item>en-GB</item></string-array></resources>',encoding='utf-8')
        source=catalogue(values);directory=root/'localisation';directory.mkdir()
        target={'hello':'Bonjour','percent':'100%','count[one]':'%1$d découverte','count[other]':'%1$d découvertes'}
        records={'source.en-GB.json':source,'fr.pending.json':target,
            'fr.machine.json':{'sourceSha256':localise.digest(source),'targetSha256':localise.digest(target)},
            'editorial-ui-overrides.json':{'sourceTexts':{'hello':'Hello'},'locales':{'fr':{'hello':'Salut'}}},
            'plural-ui-overrides.json':{'sourceTexts':{},'locales':{}}}
        for name,value in records.items():(directory/name).write_text(json.dumps(value,ensure_ascii=False),encoding='utf-8')
        return records
    def test_explicit_waiver_is_required_before_any_output(self):
        with tempfile.TemporaryDirectory() as folder:
            with self.assertRaises(ValueError):package(Path(folder),False)
            self.assertEqual([],list(Path(folder).iterdir()))
    def test_complete_resources_apply_source_bound_corrections_and_record_scope(self):
        with tempfile.TemporaryDirectory() as folder,patch.object(localise,'LOCALES',['en-GB','fr']):
            root=Path(folder);self.fixture(root);result=package(root,True)
            resources=root/'app/src/main/res/values-b+fr/strings.xml'
            self.assertEqual('Salut',catalogue(resources.parent)['hello'])
            self.assertEqual('false',ET.parse(resources).getroot().find("string[@name='percent']").attrib['formatted'])
            tags=[e.text for e in ET.parse(root/'app/src/main/res/values/languages.xml').getroot().find('string-array')]
            self.assertEqual(['en-GB','fr'],tags)
            self.assertFalse(result['humanLanguageApproval']);self.assertFalse(result['productionApproval'])
            self.assertEqual(2,check(root)['interfaceLocaleVariants'])
            resources.write_text('<resources/>',encoding='utf-8')
            with self.assertRaises(ValueError):check(root)
    def test_stale_provenance_does_not_write_a_language_folder(self):
        with tempfile.TemporaryDirectory() as folder,patch.object(localise,'LOCALES',['en-GB','fr']):
            root=Path(folder);self.fixture(root)
            (root/'localisation/fr.pending.json').write_text('{}',encoding='utf-8')
            with self.assertRaises(ValueError):package(root,True)
            self.assertFalse((root/'app/src/main/res/values-b+fr').exists())
    def test_changed_english_copy_invalidates_an_editorial_override(self):
        with tempfile.TemporaryDirectory() as folder,patch.object(localise,'LOCALES',['en-GB','fr']):
            root=Path(folder);records=self.fixture(root)
            records['editorial-ui-overrides.json']['sourceTexts']['hello']='An older source'
            (root/'localisation/editorial-ui-overrides.json').write_text(json.dumps(records['editorial-ui-overrides.json']),encoding='utf-8')
            with self.assertRaises(ValueError):package(root,True)
            self.assertFalse((root/'app/src/main/res/values-b+fr').exists())
