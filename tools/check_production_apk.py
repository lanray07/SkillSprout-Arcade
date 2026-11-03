"""Inspect the exact signed production APK, including its actual DEX constants."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
import zipfile

ROOT=Path(__file__).resolve().parents[1]
CERT='4a83f019dac2ecff23c32cfe1297db9584f8fd89d414ee8216fc2305e057e689'
PUBLIC_KEY='7f5008e432dabde4954755d6e05defb6c30d6211854a35fca026809646fedfc0'
EXPECTED={'APPLICATION_ID':'com.skillsprout.arcade','BUILD_TYPE':'amazonRelease','DEBUG':False,
          'DEVELOPMENT_CONTENT':False,'DEVELOPMENT_ACCESS':False,'PURCHASING_ALLOWED':True,
          'RVS_ENVIRONMENT':'production','VERIFIER_URL':'https://skillsprout-receipts-production.lanraybanks.workers.dev/verify',
          'VERSION_CODE':6,'VERSION_NAME':'1.0.1',
          'AMAZON_PRODUCT_MAPPINGS':'com.skillsprout.arcade.family.monthly=com.skillsprout.arcade.family,com.skillsprout.arcade.family.annual=com.skillsprout.arcade.family'}


def inspect(apk,sdk):
    def run(name,*args):
        tool=sdk/(name+'.bat' if name=='apksigner' else name+'.exe')
        return subprocess.check_output([str(tool),*map(str,args)],text=True,errors='replace')
    certificate=run('apksigner','verify','--print-certs',apk)
    assert re.search(r'Signer #1 certificate SHA-256 digest: '+CERT+r'\b',certificate)
    run('zipalign','-c','-P','16','4',apk)
    badging=run('aapt','dump','badging',apk)
    assert "package: name='com.skillsprout.arcade' versionCode='6' versionName='1.0.1'" in badging
    assert "sdkVersion:'26'" in badging and "targetSdkVersion:'35'" in badging
    assert 'application-debuggable' not in badging
    permissions=run('aapt','dump','permissions',apk)
    actual_permissions=re.findall(r"uses-permission: name='([^']+)'",permissions)
    forbidden={'android.permission.RECORD_AUDIO','android.permission.ACCESS_FINE_LOCATION','android.permission.ACCESS_COARSE_LOCATION','android.permission.READ_CONTACTS','com.google.android.gms.permission.AD_ID'}
    assert not forbidden.intersection(actual_permissions)
    manifest=run('aapt','dump','xmltree',apk,'AndroidManifest.xml')
    for flag in ['allowBackup','usesCleartextTraffic']:
        assert re.search(r'android:'+flag+r'\([^\n]+\)=\(type 0x12\)0x0',manifest)
    assert 'AccessibilityReviewActivity' not in manifest
    scratch=ROOT/'artifacts/verification/apk-inspection';scratch.mkdir(parents=True,exist_ok=True)
    configs=[]
    with zipfile.ZipFile(apk) as archive:
        assert not any(n.endswith(('.jks','.keystore','signing.properties','content-private.pem')) for n in archive.namelist())
        assert hashlib.sha256(archive.read('assets/entitlement-public.der')).hexdigest()==PUBLIC_KEY
        assert archive.read('assets/packs/starter.json')==(ROOT/'app/src/main/assets/packs/starter.json').read_bytes()
        assert archive.read('assets/privacy/notice.json')==(ROOT/'app/src/main/assets/privacy/notice.json').read_bytes()
        assert b'PRIVATE KEY' not in archive.read('assets/AppstoreAuthenticationKey.pem')
        for name in archive.namelist():
            if not re.fullmatch(r'classes\d*\.dex',name):continue
            dex=scratch/name;dex.write_bytes(archive.read(name))
            dump=run('dexdump',dex)
            for block in dump.split('Class #'):
                if "Class descriptor  : 'Lcom/skillsprout/arcade/BuildConfig;'" not in block.split('Interfaces',1)[0]:continue
                static=block.split('Static fields',1)[1].split('Instance fields',1)[0]
                fields={k:json.loads(v) for k,v in re.findall(r"name\s+: '([^']+)'[^#]*?value\s+: ([^\n]+)",static)}
                configs.append(fields)
    assert configs==[EXPECTED], 'Actual APK build constants differ from production configuration'
    return {'apkSha256':hashlib.sha256(apk.read_bytes()).hexdigest(),'certificateSha256':CERT,
            'productionEntitlementKeySha256':PUBLIC_KEY,'actualDexConstantsVerified':True,
            'versionName':'1.0.1','versionCode':6,'releaseFlags':configs[0],
            'signatureVerified':True,'zipAlignmentVerified':True,'backupAndCleartextDisabled':True,
            'noReviewActivity':True,'permissions':actual_permissions,'forbiddenPermissionsFound':[],
            'bundledPackAndPrivacyMatchCurrentSources':True,'status':'Passed',
            'scope':'Static exact-APK inspection; device, store and billing acceptance are separate.'}


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--apk',type=Path,required=True)
    parser.add_argument('--build-tools',type=Path,default=Path.home()/'AppData/Local/Android/Sdk/build-tools/35.0.0')
    parser.add_argument('--output',type=Path,default=ROOT/'release/production-apk-static-2026-11-03.json')
    args=parser.parse_args();result=inspect(args.apk,args.build_tools)
    target=args.output
    target.write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8',newline='\n')
    print(json.dumps(result,indent=2))
