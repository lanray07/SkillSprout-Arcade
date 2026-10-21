"""Physical Fire sandbox UI evidence helper. Never taps the live Amazon store."""
from pathlib import Path
import argparse,json,os,re,subprocess,time
from xml.etree import ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
ALLOWED={'com.skillsprout.arcade','com.amazon.sdktestclient'}
def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--serial',required=True)
    parser.add_argument('--adb',default=str(Path(os.getenv('ANDROID_HOME',str(Path.home()/'AppData/Local/Android/Sdk')))/'platform-tools'/('adb.exe' if os.name=='nt' else 'adb')))
    parser.add_argument('--label',default='current')
    parser.add_argument('--tap-text')
    parser.add_argument('--tap-point',help='x,y from a freshly inspected device screenshot')
    parser.add_argument('--screenshot-only',action='store_true')
    args=parser.parse_args()
    if not re.fullmatch(r'[A-Za-z0-9_-]+',args.label):parser.error('Use a simple capture label')
    def adb(*commands):return subprocess.check_output([args.adb,'-s',args.serial,*commands])
    def hierarchy():
        adb('shell','rm','-f','/sdcard/skillsprout-qa-window.xml')
        for attempt in range(3):
            result=adb('shell','uiautomator','dump','/sdcard/skillsprout-qa-window.xml')
            if b'UI hierchary dumped' in result or b'UI hierarchy dumped' in result:break
            time.sleep(1)
        else:raise RuntimeError('Fresh hierarchy unavailable; inspect a screenshot instead')
        data=adb('shell','cat','/sdcard/skillsprout-qa-window.xml')
        return data,ET.fromstring(data)
    if args.tap_text:
        _,tree=hierarchy()
        nodes=[n for n in tree.iter('node') if n.get('package') in ALLOWED and n.get('enabled')=='true' and args.tap_text in (n.get('text'),n.get('content-desc'))]
        if len(nodes)!=1:raise RuntimeError(f'Expected one sandbox/app control; found {len(nodes)}')
        bounds=list(map(int,re.findall(r'\d+',nodes[0].get('bounds',''))))
        if len(bounds)!=4:raise RuntimeError('No visible bounds')
        adb('shell','input','tap',str((bounds[0]+bounds[2])//2),str((bounds[1]+bounds[3])//2))
    if args.tap_point:
        focus=adb('shell','dumpsys','window').decode(errors='replace')
        current=next((line for line in reversed(focus.splitlines()) if 'mCurrentFocus=' in line),'')
        focused=next((line for line in reversed(focus.splitlines()) if 'mFocusedApp=' in line),'')
        allowed=any(package+'/' in current for package in ALLOWED)
        if 'PopupWindow' in current:allowed=any(package+'/' in focused for package in ALLOWED)
        if not allowed:raise RuntimeError('Refusing to tap outside this app or Amazon App Tester')
        point=args.tap_point.split(',')
        if len(point)!=2 or not all(re.fullmatch(r'\d+',p) for p in point):parser.error('Use x,y')
        adb('shell','input','tap',*point)
    output=ROOT/'artifacts/amazon-submission/fire-qa';output.mkdir(parents=True,exist_ok=True)
    def screenshot():
        pixels=adb('exec-out','screencap','-p')
        if not pixels.startswith(b'\x89PNG\r\n\x1a\n'):
            print('Screenshot unavailable or protected; no image evidence recorded');return
        destination=output/(args.label+'.png');destination.write_bytes(pixels)
        print('Captured device screenshot:',destination)
    if args.screenshot_only:
        screenshot();return
    data,tree=hierarchy()
    (output/(args.label+'.xml')).write_bytes(data)
    screenshot()
    print(json.dumps([{'text':'[private identifier]' if n.get('resource-id','').endswith(('/textview_user_id','/receipt_id')) else n.get('text') or n.get('content-desc'),'id':n.get('resource-id'),'bounds':n.get('bounds'),'enabled':n.get('enabled'),'checked':n.get('checked') if n.get('checkable')=='true' else None} for n in tree.iter('node') if n.get('text') or n.get('content-desc')],ensure_ascii=False))
if __name__=='__main__':main()
