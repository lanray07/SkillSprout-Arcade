"""Run explicit isolated Fire fixtures, preserve observed device settings, never open original app data."""
import argparse
from datetime import datetime, timezone
import hashlib
import json
import re
from pathlib import Path
import subprocess
import uuid

ROOT = Path(__file__).resolve().parents[1]
OWNED_PRODUCTS = {'com.skillsprout.arcade.family.monthly', 'com.skillsprout.arcade.family.annual'}


def restored_catalogue(current_bytes, original_bytes, merged_bytes):
    if current_bytes in [original_bytes, merged_bytes]:
        return original_bytes, True
    current, original = json.loads(current_bytes), json.loads(original_bytes)
    for sku in OWNED_PRODUCTS:
        current.pop(sku, None)
        if sku in original: current[sku] = original[sku]
    return (json.dumps(current, ensure_ascii=False, indent=2) + '\n').encode('utf-8'), False


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--adb', required=True)
    parser.add_argument('--serial', required=True)
    parser.add_argument('--fixture', choices=['voice', 'billing'], required=True)
    parser.add_argument('--billing-ui', action='store_true', help='Use the UI check instead of headless SDK checks; requires an unlocked tablet')
    parser.add_argument('--billing-stage', choices=['purchase', 'restore'], help='Explicit sandbox transaction stage; purchase waits for separately inspected App Tester confirmation')
    args = parser.parse_args()
    if args.billing_ui and args.billing_stage: parser.error('Choose one billing check')
    if args.fixture == 'voice' and (args.billing_ui or args.billing_stage): parser.error('Billing options require the billing fixture')

    def adb(*commands, timeout=90):
        return subprocess.check_output([args.adb, '-s', args.serial, *commands], timeout=timeout).decode('utf-8', errors='replace').strip()

    def settings():
        return {key: adb('shell', 'settings', 'get', group, key) for group, key in [
            ('global', 'wifi_on'), ('system', 'font_scale'), ('secure', 'accessibility_enabled'),
            ('secure', 'enabled_accessibility_services'), ('secure', 'tts_default_synth'),
            ('global', 'stay_on_while_plugged_in')]}

    def original_apk():
        paths = adb('shell', 'pm', 'path', 'com.skillsprout.arcade').splitlines()
        base = next(line.removeprefix('package:') for line in paths if line.endswith('/base.apk'))
        return adb('shell', 'sha256sum', base).split()[0]

    variant, package, classes = (
        ('voiceQa', 'com.skillsprout.arcade.voiceqa', ['InterfaceLanguagesDeviceTest']) if args.fixture == 'voice' else
        ('amazonBillingQa', 'com.skillsprout.arcade.billingqa', ['IsolatedBillingDeviceTest' if args.billing_ui else 'HeadlessBillingDeviceTest']))
    if args.billing_stage: classes = ['FirePurchaseTest#physicalSandboxPurchaseAndRestore']
    apk = ROOT / f'app/build/outputs/apk/{variant}/app-{variant}.apk'
    test_apk = ROOT / f'app/build/outputs/apk/androidTest/{variant}/app-{variant}-androidTest.apk'
    if not apk.is_file() or not test_apk.is_file():
        raise FileNotFoundError('Build the selected QA application and instrumentation APK first')
    aapt = Path(args.adb).resolve().parents[1] / 'build-tools/35.0.0' / ('aapt.exe' if Path(args.adb).suffix == '.exe' else 'aapt')
    for item, expected in [(apk, package), (test_apk, package + '.test')]:
        badging = subprocess.check_output([str(aapt), 'dump', 'badging', str(item)], timeout=30).decode('utf-8', errors='replace')
        match = re.search(r"^package: name='([^']+)'", badging, re.MULTILINE)
        if match is None or match.group(1) != expected:
            raise RuntimeError('Refusing to install an APK outside the selected isolated package')
    before, original_before = settings(), original_apk()
    sandbox_before = adb('shell', 'getprop', 'debug.amazon.sandboxmode')
    output = ''
    catalogue_original = None
    catalogue_merged = None
    catalogue_restored = True
    catalogue_backup = None
    def raw_catalogue():
        return subprocess.check_output([args.adb, '-s', args.serial, 'exec-out', 'cat', '/sdcard/amazon.sdktester.json'], timeout=30)
    try:
        if args.fixture == 'billing':
            catalogue_original = raw_catalogue()
            current = json.loads(catalogue_original)
            fixture = json.loads((ROOT / 'billing-amazon/test-fixtures/skillsprout-catalogue.json').read_text(encoding='utf-8'))
            if set(fixture) != OWNED_PRODUCTS:
                raise RuntimeError('Fixture may contain only the two owned SkillSprout test products')
            folder = ROOT / 'artifacts/verification/app-tester-backups'
            folder.mkdir(parents=True, exist_ok=True)
            token = uuid.uuid4().hex
            catalogue_backup = folder / (token + '-original.json')
            catalogue_backup.write_bytes(catalogue_original)
            current.update(fixture)
            catalogue_merged = (json.dumps(current, ensure_ascii=False, indent=2) + '\n').encode('utf-8')
            merged_file = folder / (token + '-merged.json')
            merged_file.write_bytes(catalogue_merged)
            catalogue_restored = False
            adb('push', str(merged_file), '/sdcard/amazon.sdktester.json')
            if raw_catalogue() != catalogue_merged:
                raise RuntimeError('Temporary App Tester catalogue did not match the intended fixture')
            adb('shell', 'setprop', 'debug.amazon.sandboxmode', 'debug')
            if adb('shell', 'getprop', 'debug.amazon.sandboxmode') != 'debug':
                raise RuntimeError('App Tester sandbox could not be enabled')
        for item in [apk, test_apk]:
            result = adb('install', '-r', str(item), timeout=120)
            if 'Success' not in result:
                raise RuntimeError('QA APK install failed: ' + result)
        if args.fixture == 'billing':
            # Fire OS can reject App Tester's command service after a long background idle.
            # Launch the installed tester, without changing its catalogue, account or responses.
            adb('shell', 'am', 'force-stop', 'com.amazon.sdktestclient')
            adb('shell', 'am', 'start', '-W', '-n', 'com.amazon.sdktestclient/.MainActivity')
        command = ['shell', 'am', 'instrument', '-w', '-r', '-e', 'class',
                   ','.join('com.skillsprout.arcade.' + name for name in classes)]
        if args.billing_stage: command += ['-e', 'stage', args.billing_stage]
        output = adb(*command, package + '.test/androidx.test.runner.AndroidJUnitRunner', timeout=240)
    finally:
        adb('shell', 'am', 'force-stop', package)
        if args.fixture == 'billing':
            adb('shell', 'setprop', 'debug.amazon.sandboxmode', sandbox_before or 'none')
            if catalogue_original is not None and catalogue_backup is not None:
                current_bytes = raw_catalogue()
                restored, exact = restored_catalogue(current_bytes, catalogue_original, catalogue_merged)
                reconciled = catalogue_backup.with_name(catalogue_backup.stem + '-reconciled.json')
                reconciled.write_bytes(restored)
                adb('push', str(reconciled), '/sdcard/amazon.sdktester.json')
                catalogue_restored = exact and raw_catalogue() == catalogue_original
                adb('shell', 'am', 'force-stop', 'com.amazon.sdktestclient')
        after, original_after = settings(), original_apk()
        sandbox_after = adb('shell', 'getprop', 'debug.amazon.sandboxmode')
        report = {
            'checkedAt': datetime.now(timezone.utc).isoformat(), 'fixture': variant,
            'apkSha256': hashlib.sha256(apk.read_bytes()).hexdigest(),
            'testApkSha256': hashlib.sha256(test_apk.read_bytes()).hexdigest(),
            'instrumentationOutput': output, 'testsPassed': 'OK (1 test)' in output,
            'originalInstalledApkBeforeSha256': original_before,
            'originalInstalledApkAfterSha256': original_after,
            'originalInstalledApkUnchanged': original_before == original_after,
            'observedSettingsUnchanged': before == after,
            'sandboxSettingRestored': sandbox_before == sandbox_after,
            'appTesterOpenedForRun': args.fixture == 'billing',
            'testCatalogueRestoredExactly': catalogue_restored,
            'personalPinOrProfilesRead': False, 'productionApproval': False,
        }
        if report['testsPassed']:
            metric = 'interface-languages.json' if args.fixture == 'voice' else ((args.billing_stage + '.json') if args.billing_stage else ('isolated-billing.json' if args.billing_ui else 'headless-billing.json'))
            report['testMetrics'] = json.loads(adb('shell', 'cat', '/sdcard/Android/data/' + package + '/files/fire-qa/' + metric))
        suffix = ('-' + args.billing_stage) if args.billing_stage else ('-ui' if args.billing_ui else '')
        destination = ROOT / 'release' / f'fire-{args.fixture}-isolated-2026-11-02{suffix}.json'
        destination.write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
        print(json.dumps(report, indent=2))
    if not report['testsPassed'] or not all(report[key] for key in
            ['originalInstalledApkUnchanged', 'observedSettingsUnchanged', 'sandboxSettingRestored', 'testCatalogueRestoredExactly']):
        raise RuntimeError('Isolated fixture failed or observed settings changed; inspect the report')


if __name__ == '__main__':
    main()
