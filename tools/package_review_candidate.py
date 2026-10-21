"""Validate/package the publisher-signed REVIEW target; never declare production approval."""
from pathlib import Path
import hashlib,json,os,re,shutil,subprocess,zipfile
from xml.etree import ElementTree as ET
from cryptography import x509
from cryptography.hazmat.primitives import hashes,serialization
ROOT=Path(__file__).resolve().parents[1]
SOURCE=ROOT/'app/build/outputs/apk/amazonReview/app-amazonReview.apk'
metadata=json.loads(SOURCE.with_name('output-metadata.json').read_text('utf-8'))
element=metadata['elements'][0]
assert metadata['applicationId']=='com.skillsprout.arcade'
assert element['versionName'].endswith('-review'), 'Not an explicitly labelled review build'
config=(ROOT/'app/build/generated/source/buildConfig/amazonReview/com/skillsprout/arcade/BuildConfig.java').read_text('utf-8')
for name,value in {'DEBUG':'false','DEVELOPMENT_CONTENT':'true','DEVELOPMENT_ACCESS':'false','PURCHASING_ALLOWED':'false'}.items():
    assert re.search(r'boolean '+name+r' = '+value+r';',config), 'Unexpected review flag: '+name
sdk=Path(os.getenv('ANDROID_HOME') or os.getenv('ANDROID_SDK_ROOT') or Path.home()/'AppData/Local/Android/Sdk')
build_tools=sdk/'build-tools/35.0.0'
def run(tool,*args):
    executable=build_tools/(tool+('.bat' if tool=='apksigner' and os.name=='nt' else '.exe' if os.name=='nt' else ''))
    return subprocess.check_output([str(executable),*map(str,args)],text=True,stderr=subprocess.STDOUT)
signature=run('apksigner','verify','--verbose','--print-certs',SOURCE)
match=re.search(r'Signer #1 certificate SHA-256 digest: ([a-fA-F0-9]+)',signature)
assert match, 'APK signer was not identified'
expected=x509.load_pem_x509_certificate((Path.home()/'.skillsprout/release-signing/publisher-certificate.pem').read_bytes())
certificate_sha=expected.fingerprint(hashes.SHA256()).hex()
assert match[1].lower()==certificate_sha, 'Review APK is not signed with the provisioned publisher certificate'
badging=run('aapt','dump','badging',SOURCE)
assert 'application-debuggable' not in badging, 'Review APK is debuggable'
assert f"versionCode='{element['versionCode']}'" in badging
run('zipalign','-c','-P','16','4',SOURCE)
with zipfile.ZipFile(SOURCE) as archive:
    names=archive.namelist()
    assert 'assets/AppstoreAuthenticationKey.pem' in names
    key=serialization.load_pem_public_key(archive.read('assets/AppstoreAuthenticationKey.pem'))
    assert key.key_size==2048
    assert not any(name.endswith(('.jks','.keystore','signing.properties','amazon.properties','content-private.pem')) for name in names)
    pack=json.loads(archive.read('assets/packs/starter.json'))
    activity_count=len(pack['activities'])
    unapproved=sum(a['reviewStatus']!='Approved' for a in pack['activities'])
issues=ET.parse(ROOT/'app/build/reports/lint-results-amazonReview.xml').getroot()
lint={'errors':sum(i.get('severity') in ['Error','Fatal'] for i in issues),'warnings':sum(i.get('severity')=='Warning' for i in issues)}
assert lint['errors']==0, 'Review lint failed'
OUT=ROOT/'artifacts';OUT.mkdir(exist_ok=True)
target=OUT/f"SkillSprout-Arcade-{element['versionName']}-signed.apk"
shutil.copy2(SOURCE,target)
report={'artifact':target.name,'applicationId':metadata['applicationId'],'version':element['versionName'],'versionCode':element['versionCode'],'bytes':target.stat().st_size,'sha256':hashlib.sha256(target.read_bytes()).hexdigest(),'publisherCertificateSha256':certificate_sha,'debuggable':False,'purchasingAllowed':False,'starterAccessOnly':True,'developmentContentAllowed':True,'contentActivities':activity_count,'unapprovedActivities':unapproved,'lint':lint,'status':'REVIEW ONLY — NOT PRODUCTION APPROVED','amazonUpload':'Pending; draft/testing only','productionEvidence':'Unchanged and pending; see release/evidence.json'}
(OUT/'AMAZON-REVIEW-REPORT.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
print(json.dumps(report,indent=2))
