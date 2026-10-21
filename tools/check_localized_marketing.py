"""Verify localized export counts, source preservation, dimensions, hashes and safe text bounds."""
from pathlib import Path
import base64,hashlib,json,struct
from xml.etree import ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]

def validate(root=ROOT):
    directory=root/'marketing/localized'
    copy=json.loads((directory/'copy.json').read_text('utf-8'))
    manifest=json.loads((directory/'manifest.json').read_text('utf-8'))
    assert len(manifest['assets'])==len(copy['locales'])*16
    keys=set()
    for asset in manifest['assets']:
        key=asset['locale'],asset['variant'],asset['id']
        assert key not in keys;keys.add(key)
        png=(root/asset['png']).read_bytes()
        assert png[:8]==b'\x89PNG\r\n\x1a\n'
        assert struct.unpack('>II',png[16:24])==(asset['width'],asset['height'])
        assert hashlib.sha256(png).hexdigest()==asset['sha256']
        assert (asset['width'],asset['height'])==({'tablet':(1920,1200),'phone':(1080,1920)}[asset['variant']])
        original=(root/asset['source']).read_bytes()
        assert hashlib.sha256(original).hexdigest()==asset['sourceSha256']
        svg=ET.parse(root/asset['editable']).getroot()
        capture=svg.find('{http://www.w3.org/2000/svg}image')
        assert base64.b64decode(capture.attrib['href'].split(',',1)[1],validate=True)==original
        assert asset['englishUiPreserved'] is True
        box=asset['capture']
        for text in asset['captionBounds']:
            assert 0<=text['x'] and text['x']+text['width']<=asset['width']
            assert 0<=text['y'] and text['y']+text['height']<=asset['height']
            assert text['y']+text['height']<=box['y'] or text['y']>=box['y']+box['height']
        caption=' '.join(n.text or '' for n in svg.iter('{http://www.w3.org/2000/svg}text'))
        assert copy['locales'][asset['locale']]['disclosure'] in caption
    for locale,entry in copy['locales'].items():
        assert len(entry['screens'])==8
        listing=json.loads((directory/locale/'listing.json').read_text('utf-8'))
        for field in ['displayTitle','shortDescription','longDescription','featureBullets','keywords']:
            assert listing[field]==entry[field]
        assert 3<=len(entry['featureBullets'])<=5
        assert len(entry['displayTitle'])<=200 and len(entry['shortDescription'].encode())<=2000 and len(entry['longDescription'])<=4000
        assert 'banksmi@mail.com' in entry['longDescription']
        assert listing['status']=='Draft translation - native review pending'
        assert listing['appInterfaceAndContent']=='English only'
    return {'locales':len(copy['locales']),'screenshots':len(keys),'sourceCapturesUnchanged':True,'textBoundsVerified':True,'nativeLanguageReview':'Pending'}

if __name__=='__main__':print(json.dumps(validate(),indent=2))
