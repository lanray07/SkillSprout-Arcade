"""Open exact preview-only diagram screens with reversible VoiceView settings."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]
CASES = {'shapes': 'dev-maths-y1-0397', 'clock-3': 'dev-maths-y1-0399',
         'halves': 'dev-maths-y1-0401', 'clock-830': 'dev-maths-y2-0404',
         'fruit': 'dev-maths-y2-0407', 'bus': 'dev-maths-y4-0419'}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--serial', required=True)
    action = parser.add_mutually_exclusive_group(required=True)
    action.add_argument('--case', choices=CASES)
    action.add_argument('--app', action='store_true', help='Open the normal QA app for state checks')
    action.add_argument('--restore', action='store_true')
    parser.add_argument('--large-text', action='store_true', help='Temporarily use font scale 1.5')
    args = parser.parse_args()
    adb = Path.home() / 'AppData/Local/Android/Sdk/platform-tools/adb.exe'
    def run(*commands):
        return subprocess.check_output([str(adb), '-s', args.serial, *commands], timeout=30).decode(errors='replace').strip()
    state_file = ROOT / 'artifacts/amazon-submission/fire-qa/diagram-review-settings.json'
    state = json.loads(state_file.read_text()) if state_file.exists() else None
    if args.restore:
        assert state and not state['restored'], 'No active review session'
        assert state['serial'] == args.serial, 'Restore on the original device'
        for key, value in state['original'].items():
            run('shell', 'settings', 'delete', 'secure', key) if value == 'null' else run('shell', 'settings', 'put', 'secure', key, value)
        checks = {key: run('shell', 'settings', 'get', 'secure', key) == value
                  for key, value in state['original'].items()}
        for key, value in state.get('originalSystem', {}).items():
            run('shell', 'settings', 'delete', 'system', key) if value == 'null' else run('shell', 'settings', 'put', 'system', key, value)
            checks[key] = run('shell', 'settings', 'get', 'system', key) == value
        assert all(checks.values()), 'Settings restoration incomplete'
        run('shell', 'am', 'start', '-f', '0x10008000', '-n', 'com.skillsprout.arcade/.MainActivity')
        state['restored'] = True
        state_file.write_text(json.dumps(state, indent=2) + '\n', newline='\n')
        print('Original VoiceView settings restored and verified; normal app opened.')
        return
    assert 'DEBUGGABLE' in run('shell', 'dumpsys', 'package', 'com.skillsprout.arcade'), 'Use the dedicated QA app'
    apk = ROOT / 'app/build/outputs/apk/amazonPreview/app-amazonPreview.apk'
    apk_hash = hashlib.sha256(apk.read_bytes()).hexdigest()
    remote = run('shell', 'pm', 'path', 'com.skillsprout.arcade').removeprefix('package:')
    assert run('shell', 'sha256sum', remote).split()[0] == apk_hash, 'Install the current preview APK'
    if not state or state['restored']:
        state = {'serial': args.serial, 'restored': False, 'apkSha256': apk_hash,
                 'original': {key: run('shell', 'settings', 'get', 'secure', key)
                              for key in ['enabled_accessibility_services', 'accessibility_enabled']}}
        state_file.parent.mkdir(parents=True, exist_ok=True)
        state_file.write_text(json.dumps(state, indent=2) + '\n', newline='\n')
    assert state['serial'] == args.serial and state['apkSha256'] == apk_hash
    if args.large_text:
        state.setdefault('originalSystem', {}).setdefault('font_scale', run('shell', 'settings', 'get', 'system', 'font_scale'))
        state_file.write_text(json.dumps(state, indent=2) + '\n', newline='\n')
        run('shell', 'settings', 'put', 'system', 'font_scale', '1.5')
    original = state['original']['enabled_accessibility_services']
    services = [] if original in ['', 'null'] else original.split(':')
    voice = 'com.amazon.logan/.LoganAccessibilityService'
    if voice not in services:
        services.append(voice)
    run('shell', 'input', 'keyevent', '224')
    run('shell', 'wm', 'dismiss-keyguard')
    run('shell', 'settings', 'put', 'secure', 'enabled_accessibility_services', ':'.join(services))
    run('shell', 'settings', 'put', 'secure', 'accessibility_enabled', '1')
    target = 'com.skillsprout.arcade/.MainActivity' if args.app else 'com.skillsprout.arcade/.AccessibilityReviewActivity'
    command = ['shell', 'am', 'start', '-f', '0x10008000', '-n', target]
    if args.case:
        command += ['--es', 'activityId', CASES[args.case]]
    result = run(*command)
    assert 'Error' not in result, result
    print(f'Opened {CASES[args.case] if args.case else "normal QA app"}; VoiceView enabled. Run --restore after review.')


if __name__ == '__main__':
    main()
