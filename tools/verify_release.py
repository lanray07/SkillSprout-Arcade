"""Separate reproducible candidate prerequisites from exact-APK publication acceptance."""
import argparse
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PACK = 'app/src/main/assets/packs/starter.json'
WAIVER = 'release/bundled-content-publication-policy.json'
ACCEPTANCE = 'release/candidate-acceptance.json'


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def candidate_checks(root=ROOT):
    blocked = []
    pack = root / PACK
    manifest = json.loads((pack.parent / 'manifest.json').read_text('utf-8'))
    policy = json.loads((root / WAIVER).read_text('utf-8'))
    data = json.loads(pack.read_text('utf-8'))
    digest = sha(pack)
    if not (digest == manifest['sha256'] == policy.get('packSha256')):
        blocked.append('Bundled pack/manifest/publisher waiver hashes differ')
    if policy.get('status') != 'Publisher waiver' or policy.get('teacherApprovalClaimed') is not False:
        blocked.append('Missing truthful publisher bundled-content waiver')
    if not policy.get('publisher') or not policy.get('instruction'):
        blocked.append('Publisher waiver attribution missing')
    source = (root / 'app/src/main/java/com/skillsprout/arcade/ContentPacks.kt').read_text('utf-8')
    if f'BUNDLED_PUBLISHER_WAIVER_SHA256 = "{digest}"' not in source:
        blocked.append('Runtime bundled exception does not match reviewed bytes')
    if len(data['activities']) != manifest['count'] or any(a['reviewStatus'] in ['Rejected', 'Retired', 'Draft'] for a in data['activities']):
        blocked.append('Invalid bundled activity count/status')
    backup = json.loads((root / 'release/signing-backup-verification.json').read_text('utf-8'))
    if not all(backup['checks'].values()) or backup.get('publisherRetentionConfirmation', {}).get('status', '').startswith('Publisher confirmed') is False:
        blocked.append('Signing backup verification missing')
    return blocked


def publication_checks(root=ROOT, apk=None):
    blocked = candidate_checks(root)
    record_path = root / ACCEPTANCE
    if not record_path.is_file():
        return blocked + ['Exact-candidate acceptance has not been recorded']
    record = json.loads(record_path.read_text('utf-8'))
    if record.get('knownBlockingIssues'):
        blocked.append('Known release-blocking defects remain; exact-candidate acceptance is withdrawn')
    if apk is None or not apk.is_file() or record.get('apkSha256') != sha(apk):
        blocked.append('Publication acceptance must match the exact supplied APK')
    for key in ['signatureAndManifest', 'releaseStartupAndGames', 'sandboxCheckoutAndRestore',
                'localisedLayoutsAndAssets', 'publicPrivacyAndStoreDeclarations']:
        item = record.get('checks', {}).get(key, {})
        path = (root / item.get('artifact', '')).resolve()
        if not (item.get('status') == 'Passed' and path.is_relative_to(root.resolve()) and path.is_file()
                and sha(path) == item.get('artifactSha256')):
            blocked.append('Unaccepted candidate check: ' + key)
    return blocked


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--phase', choices=['candidate', 'publish'], default='publish')
    parser.add_argument('--apk', type=Path)
    args = parser.parse_args()
    try:
        blocked = candidate_checks() if args.phase == 'candidate' else publication_checks(apk=args.apk)
    except (ValueError, KeyError, OSError) as error:
        blocked = ['Missing or invalid release input: ' + type(error).__name__]
    if blocked:
        print(args.phase.upper() + ' BLOCKED:\n- ' + '\n- '.join(blocked))
        raise SystemExit(1)
    print('Candidate prerequisites passed; this does not authorise publication.' if args.phase == 'candidate'
          else 'Exact-APK publication acceptance passed; verify portal declarations before submitting.')


if __name__ == '__main__':
    main()
