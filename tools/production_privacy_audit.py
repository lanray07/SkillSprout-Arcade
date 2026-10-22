"""Record reproducible engineering observations, not legal or brand approval."""
import datetime
import hashlib
import json
from pathlib import Path
import re
import subprocess
import zipfile
from xml.etree import ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def audit():
    android = '{http://schemas.android.com/apk/res/android}'
    main = ET.parse(ROOT / 'app/src/main/AndroidManifest.xml').getroot()
    app = main.find('application')
    report = {'checkedAt': datetime.datetime.now(datetime.timezone.utc).isoformat(),
              'status': 'Engineering observations only; publisher/legal and brand decisions pending',
              'productionApproval': False,
              'manifest': {'androidBackupDisabled': app.get(android+'allowBackup') == 'false',
                           'cleartextTrafficDisabled': app.get(android+'usesCleartextTraffic') == 'false'},
              'apkObservations': [], 'assetManifest': [], 'fontsInPublicSourceRoots': []}
    sdk = Path.home() / 'AppData/Local/Android/Sdk/build-tools/35.0.0/aapt.exe'
    forbidden = {'android.permission.RECORD_AUDIO', 'android.permission.ACCESS_FINE_LOCATION',
                 'android.permission.ACCESS_COARSE_LOCATION', 'android.permission.READ_CONTACTS',
                 'com.google.android.gms.permission.AD_ID'}
    pack = ROOT / 'app/src/main/assets/packs/starter.json'
    for variant in ['debug', 'amazonPreview', 'amazonReview']:
        apk = ROOT / f'app/build/outputs/apk/{variant}/app-{variant}.apk'
        if not apk.exists():
            continue
        permissions = re.findall(r"uses-permission: name='([^']+)'", subprocess.check_output(
            [str(sdk), 'dump', 'permissions', str(apk)], text=True))
        with zipfile.ZipFile(apk) as archive:
            private = [name for name in archive.namelist() if name.endswith(
                ('.jks', '.keystore', 'signing.properties', 'content-private.pem', 'content-password.txt', '.skillsprout-backup'))]
            report['apkObservations'].append({'variant': variant, 'sha256': sha(apk),
                'permissions': permissions, 'forbiddenPermissionsFound': sorted(forbidden.intersection(permissions)),
                'privateSigningFilesFound': private,
                'bundledPackMatchesCurrent': hashlib.sha256(archive.read('assets/packs/starter.json')).hexdigest() == sha(pack)})
    for entry in json.loads((ROOT / 'assets/manifest.json').read_text(encoding='utf-8')):
        path = ROOT / entry['path']
        report['assetManifest'].append({'path': entry['path'], 'matchesRecordedHash': path.is_file() and sha(path) == entry['sha256'],
                                        'recordedProvenance': entry['provenance']})
    for folder in ['assets', 'app/src/main/res', 'website/public', 'marketing']:
        report['fontsInPublicSourceRoots'].extend(str(p.relative_to(ROOT)).replace('\\', '/')
            for p in (ROOT / folder).rglob('*') if p.is_file() and p.suffix.lower() in {'.ttf', '.otf', '.woff', '.woff2'})
    sources = ['app/src/main/AndroidManifest.xml', 'app/src/amazon/AndroidManifest.xml',
        'billing-amazon/src/main/java/com/skillsprout/billing/HttpsReceiptVerifier.kt',
        'billing-amazon/src/main/java/com/skillsprout/billing/AmazonBilling.kt',
        'billing-service/service.mjs', 'billing-service/worker.mjs', 'billing-service/wrangler.jsonc',
        'website/privacy.json', 'assets/DESIGN.md', 'assets/manifest.json']
    report['sourceHashes'] = {name: sha(ROOT / name) for name in sources}
    report['limitations'] = [
        'Permission and filename checks do not prove every SDK data practice or absence of data collection.',
        'APK samples identify the exact files checked; older debug/review samples may contain earlier packs.',
        'No bundled font files found does not certify system-font, dependency or rasterisation rights.',
        'Recorded generated-art provenance is not independent ownership, originality or trademark clearance.',
        'Privacy legal bases, territories, retention, provider terms and final notices/declarations remain unapproved.']
    out = ROOT / 'release/privacy-asset-engineering-audit.json'
    out.write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
    assert all(report['manifest'].values())
    assert all(not a['forbiddenPermissionsFound'] and not a['privateSigningFilesFound'] for a in report['apkObservations'])
    print(json.dumps({'artifact': str(out.relative_to(ROOT)), 'apkSamples': len(report['apkObservations']),
                      'assetHashMismatches': sum(not a['matchesRecordedHash'] for a in report['assetManifest']),
                      'bundledFontFiles': len(report['fontsInPublicSourceRoots']), 'productionApproval': False}))


if __name__ == '__main__':
    audit()
