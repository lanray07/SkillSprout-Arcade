"""Check real localised UI capture exports and store-copy coverage without approving translations."""
import base64
import hashlib
import json
import struct
from pathlib import Path
from xml.etree import ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]


def check(root=ROOT):
    base=root/'marketing/candidate'
    copy=json.loads((base/'copy.json').read_text('utf-8'))
    manifest=json.loads((base/'manifest.json').read_text('utf-8'))
    expected=set(copy['locales'])
    assert len(expected)==19
    assert manifest['sourceCopySha256']==hashlib.sha256((base/'copy.json').read_bytes()).hexdigest()
    assert len(manifest['assets'])==len(expected)*6
    ns='{http://www.w3.org/2000/svg}'
    seen=set()
    for asset in manifest['assets']:
        key=asset['locale'],asset['variant'],asset['game']
        assert key not in seen;seen.add(key)
        assert asset['locale'] in expected and asset['game'] in ['maths','spelling','story']
        png=(root/asset['png']).read_bytes()
        assert png[:8]==b'\x89PNG\r\n\x1a\n'
        assert struct.unpack('>II',png[16:24])==(asset['width'],asset['height'])
        assert (asset['width'],asset['height'])==({'tablet':(1920,1200),'phone':(1080,1920)}[asset['variant']])
        assert hashlib.sha256(png).hexdigest()==asset['sha256']
        original=(root/asset['source']).read_bytes()
        assert hashlib.sha256(original).hexdigest()==asset['sourceSha256']
        svg=ET.parse(root/asset['editable']).getroot()
        image=svg.find(ns+'image')
        assert base64.b64decode(image.get('href').split(',',1)[1],validate=True)==original
        assert asset['localisedUiPreserved'] is True
        box=asset['capture']
        for text in asset['captionBounds']:
            assert 0<=text['x'] and text['x']+text['width']<=asset['width']
            assert 0<=text['y'] and text['y']+text['height']<=box['y']
    for variant in ['tablet','phone']:
        captures=json.loads((base/'captures'/variant/'capture-records.json').read_text('utf-8'))
        assert len(captures)==len(expected)*3
        assert {row['locale'] for row in captures}==expected
        assert all(row['interfaceLocaleVerified'] and row['textLineBoundOverflowCount']==0 for row in captures)
    for locale,source in copy['locales'].items():
        listing=json.loads((base/locale/'listing.json').read_text('utf-8'))
        for field in ['displayTitle','shortDescription','longDescription','featureBullets','keywords']:
            assert listing[field]==source[field]
        assert 3<=len(listing['featureBullets'])<=5
        assert len(listing['displayTitle'])<=200
        assert len(listing['shortDescription'].encode('utf-8'))<=1200
        assert len(listing['longDescription'])<=4000
        assert 'banksmi@mail.com' in listing['longDescription']
    return {'interfaceVariants':len(expected),'screenshots':len(seen),'realLocalisedUi':True,
            'sourceHashesAndCaptionBoundsChecked':True,'learningLanguage':'en-GB',
            'independentLanguageApproval':False,'physicalFireCaptureClaimed':False}


if __name__=='__main__':print(json.dumps(check(),indent=2))
