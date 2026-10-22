"""Run the physical VoiceView node test and restore the original service settings."""
import argparse
import datetime
import hashlib
import json
from pathlib import Path
import subprocess
import time

ROOT = Path(__file__).resolve().parents[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--serial', required=True)
    parser.add_argument('--adb', default=str(Path.home() / 'AppData/Local/Android/Sdk/platform-tools/adb.exe'))
    args = parser.parse_args()
    def run(*commands, timeout=60):
        return subprocess.check_output([args.adb, '-s', args.serial, *commands], timeout=timeout).decode(errors='replace')
    assert 'DEBUGGABLE' in run('shell', 'dumpsys', 'package', 'com.skillsprout.arcade'), 'Use the dedicated debug QA app'
    apk = ROOT / 'app/build/outputs/apk/amazonPreview/app-amazonPreview.apk'
    test = ROOT / 'app/build/outputs/apk/androidTest/amazonPreview/app-amazonPreview-androidTest.apk'
    remote = run('shell', 'pm', 'path', 'com.skillsprout.arcade').strip().removeprefix('package:')
    apk_hash = hashlib.sha256(apk.read_bytes()).hexdigest()
    assert run('shell', 'sha256sum', remote).split()[0] == apk_hash, 'Installed app differs from the local preview APK'
    original = {key: run('shell', 'settings', 'get', 'secure', key).strip()
                for key in ['enabled_accessibility_services', 'accessibility_enabled']}
    sandbox = run('shell', 'getprop', 'debug.amazon.sandboxmode').strip()
    out = ROOT / 'release/fire-voiceview-2026-10-22.json'
    previous = json.loads(out.read_text()) if out.exists() else None
    report = {'checkedAt': datetime.datetime.now(datetime.timezone.utc).isoformat(),
              'device': {'model': run('shell', 'getprop', 'ro.product.model').strip(),
                         'androidApi': int(run('shell', 'getprop', 'ro.build.version.sdk').strip())},
              'variant': 'amazonPreview', 'apkSha256': apk_hash,
              'testApkSha256': hashlib.sha256(test.read_bytes()).hexdigest(),
              'productionApproval': False, 'spokenOutputVerified': False,
              'limitations': ['Accessibility focus/node semantics only; human listening and gesture review remain pending.']}
    if previous:
        report['earlierAttempts'] = previous.get('earlierAttempts', []) + [
            {key: previous[key] for key in ['checkedAt', 'passed', 'output', 'cleanup'] if key in previous}]
    try:
        run('install', '-r', str(test))
        # A sleeping/locked tablet has a system root instead of the app root.
        run('shell', 'input', 'keyevent', '224')
        run('shell', 'wm', 'dismiss-keyguard')
        services = [] if original['enabled_accessibility_services'] in ['', 'null'] else original['enabled_accessibility_services'].split(':')
        voice = 'com.amazon.logan/.LoganAccessibilityService'
        if voice not in services:
            services.append(voice)
        run('shell', 'settings', 'put', 'secure', 'enabled_accessibility_services', ':'.join(services))
        run('shell', 'settings', 'put', 'secure', 'accessibility_enabled', '1')
        run('shell', 'setprop', 'debug.amazon.sandboxmode', 'debug')
        time.sleep(2)
        output = run('shell', 'am', 'instrument', '-w', '-e', 'class',
                     'com.skillsprout.arcade.FireAccessibilityTest#voiceViewFocusAndProtectedPinNodes',
                     '-e', 'voiceViewExpected', 'true',
                     'com.skillsprout.arcade.test/androidx.test.runner.AndroidJUnitRunner', timeout=100)
        report.update(passed='OK (1 test)' in output, output=output[-3500:], wakeAndDismissRequested=True)
        if report['passed']:
            report['deviceReport'] = json.loads(run('shell', 'cat', '/sdcard/Android/data/com.skillsprout.arcade/files/fire-qa/voiceview-nodes.json'))
    finally:
        for key, value in original.items():
            run('shell', 'settings', 'delete', 'secure', key) if value == 'null' else run('shell', 'settings', 'put', 'secure', key, value)
        run('shell', "setprop debug.amazon.sandboxmode '" + sandbox + "'")
        report['cleanup'] = {key + 'Restored': run('shell', 'settings', 'get', 'secure', key).strip() == value
                             for key, value in original.items()}
        report['cleanup']['sandboxPropertyRestored'] = run('shell', 'getprop', 'debug.amazon.sandboxmode').strip() == sandbox
        out.write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(report, indent=2))
    if not report.get('passed') or not all(report['cleanup'].values()):
        raise SystemExit(1)


if __name__ == '__main__':
    main()
