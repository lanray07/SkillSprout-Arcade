"""Temporarily remove TTS availability for the isolated Voice QA installation."""
import argparse
import datetime
import hashlib
import json
from pathlib import Path
import re
import subprocess

ROOT = Path(__file__).resolve().parents[1]
PACKAGE = 'com.skillsprout.arcade.voiceqa'
ENGINES = ('com.ivona.tts.oem', 'com.svox.pico')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--serial', required=True)
    action = parser.add_mutually_exclusive_group(required=True)
    action.add_argument('--prepare', action='store_true')
    action.add_argument('--restore', action='store_true')
    args = parser.parse_args()
    adb = Path.home() / 'AppData/Local/Android/Sdk/platform-tools/adb.exe'

    def run(*commands):
        return subprocess.check_output([str(adb), '-s', args.serial, *commands], timeout=40).decode(errors='replace').strip()

    def engine_state(engine):
        line = next(line for line in run('shell', 'dumpsys', 'package', engine).splitlines()
                    if 'User 0:' in line and 'installed=' in line)
        assert 'installed=true' in line and 'hidden=false' in line
        return int(re.search(r'\benabled=(\d+)', line).group(1))

    journal = ROOT / 'artifacts/amazon-submission/fire-qa/voice-qa-missing-voice-settings.json'

    def save(state):
        journal.parent.mkdir(parents=True, exist_ok=True)
        journal.write_text(json.dumps(state, indent=2) + '\n', encoding='utf-8', newline='\n')

    def restore(state):
        assert state['serial'] == args.serial, 'Restore on the original device'
        run('shell', 'am', 'force-stop', PACKAGE)
        for engine, original in state['originalEngineStates'].items():
            assert engine in ENGINES and original == 0
            run('shell', 'pm', 'default-state', '--user', '0', engine)
        run('shell', 'svc', 'wifi', 'enable' if state['wifiOriginal'] == '1' else 'disable')
        checks = {
            'engineStatesRestored': all(engine_state(engine) == original
                for engine, original in state['originalEngineStates'].items()),
            'wifiSettingRestored': run('shell', 'settings', 'get', 'global', 'wifi_on') == state['wifiOriginal'],
            'accessibilitySettingUnchanged': run('shell', 'settings', 'get', 'secure', 'accessibility_enabled') == state['accessibilityOriginal'],
            'defaultEngineSettingUnchanged': run('shell', 'settings', 'get', 'secure', 'tts_default_synth') == state['defaultEngineOriginal'],
        }
        state.update(restored=all(checks.values()), cleanup=checks)
        save(state)
        assert state['restored'], 'Restoration incomplete; inspect the private journal'
        print('Original voice engines and Wi-Fi restored and verified; Voice QA closed.')

    if args.restore:
        state = json.loads(journal.read_text(encoding='utf-8'))
        assert not state['restored'], 'No active missing-voice session'
        restore(state)
        return

    if journal.exists():
        assert json.loads(journal.read_text(encoding='utf-8'))['restored'], 'Restore the earlier session first'
    assert run('shell', 'getprop', 'ro.product.model') == 'KFTRWI', 'This helper is scoped to the reviewed Fire model'
    assert run('shell', 'am', 'get-current-user') == '0'
    assert not re.search(r'(?m)^\s*inputRestricted=true\s*$', run('shell', 'dumpsys', 'window', 'policy')), 'Unlock the tablet privately before testing'
    assert run('shell', 'settings', 'get', 'secure', 'accessibility_enabled') == '0', 'Use ordinary touch; do not remove voices while accessibility is enabled'
    assert 'DEBUGGABLE' in run('shell', 'dumpsys', 'package', PACKAGE), 'Install the isolated debug Voice QA app'
    apk = ROOT / 'app/build/outputs/apk/voiceQa/app-voiceQa.apk'
    digest = hashlib.sha256(apk.read_bytes()).hexdigest()
    remote = run('shell', 'pm', 'path', PACKAGE).removeprefix('package:')
    assert run('shell', 'sha256sum', remote).split()[0] == digest, 'Installed Voice QA APK differs'
    services = run('shell', 'cmd', 'package', 'query-services', '--brief', '-a', 'android.intent.action.TTS_SERVICE')
    assert set(re.findall(r'^\s*([\w.]+)/\S+', services, re.M)) == set(ENGINES), 'Unexpected voice-engine inventory'
    state = {
        'serial': args.serial, 'apkSha256': digest, 'restored': False,
        'startedAt': datetime.datetime.now(datetime.timezone.utc).isoformat(),
        'originalEngineStates': {engine: engine_state(engine) for engine in ENGINES},
        'wifiOriginal': run('shell', 'settings', 'get', 'global', 'wifi_on'),
        'accessibilityOriginal': run('shell', 'settings', 'get', 'secure', 'accessibility_enabled'),
        'defaultEngineOriginal': run('shell', 'settings', 'get', 'secure', 'tts_default_synth'),
    }
    assert all(value == 0 for value in state['originalEngineStates'].values())
    assert state['wifiOriginal'] in ('0', '1')
    save(state)
    try:
        run('shell', 'am', 'force-stop', PACKAGE)
        for engine in ENGINES:
            run('shell', 'pm', 'disable-user', '--user', '0', engine)
            assert engine_state(engine) == 3
        assert 'No services found' in run('shell', 'cmd', 'package', 'query-services', '--brief', '-a', 'android.intent.action.TTS_SERVICE')
        run('shell', 'svc', 'wifi', 'disable')
        assert run('shell', 'settings', 'get', 'global', 'wifi_on') == '0'
        result = run('shell', 'am', 'start', '-n', PACKAGE + '/com.skillsprout.arcade.MainActivity')
        assert 'Error' not in result
        state['noTtsServicesVerified'] = True
        save(state)
        print('Voice QA opened with zero TTS services and Wi-Fi off. Run --restore immediately after the check.')
    except BaseException:
        restore(state)
        raise


if __name__ == '__main__':
    main()
