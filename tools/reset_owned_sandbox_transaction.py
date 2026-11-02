"""Reset only the isolated Billing QA App Tester subscription; never clear all transactions."""
import argparse
import re
import subprocess
import time
import xml.etree.ElementTree as ET

PACKAGE='com.skillsprout.arcade.billingqa'
TESTER='com.amazon.sdktestclient'


def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--adb',required=True);p.add_argument('--serial',required=True)
    args=p.parse_args()
    def adb(*cmd):return subprocess.check_output([args.adb,'-s',args.serial,*cmd],timeout=40).decode('utf-8',errors='replace')
    def focused():
        if not re.search(r'mCurrentFocus=.*com.amazon.sdktestclient/',adb('shell','dumpsys','window')):
            raise RuntimeError('Refusing input outside installed Amazon App Tester')
    def dump():
        for _ in range(8):
            result=adb('shell','uiautomator','dump','/sdcard/skillsprout-owned-transaction.xml')
            if 'dumped' in result:
                return ET.fromstring(adb('exec-out','cat','/sdcard/skillsprout-owned-transaction.xml'))
            time.sleep(.5)
        raise RuntimeError('No fresh App Tester hierarchy; no reset performed')
    def tap(node):
        focused();x1,y1,x2,y2=map(int,re.findall(r'\d+',node.get('bounds')))
        if x2<=x1 or y2<=y1:raise RuntimeError('Control has no visible bounds')
        adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2));time.sleep(.5)
    adb('shell','input','keyevent','KEYCODE_WAKEUP')
    adb('shell','am','start','-W','-n',TESTER+'/.MainActivity')
    ui=dump()
    module=next((n for n in ui.iter('node') if n.get('text')=="Appstore SDK API's"),None)
    if module is not None:tap(module);ui=dump()
    manage=next((n for n in ui.iter('node') if n.get('text')=='8. Manage Transactions'),None)
    if manage is None:raise RuntimeError('Expected App Tester transaction section is absent')
    tap(manage)
    ui=dump()
    # Only scroll the observed transaction section; never choose Delete All.
    focused();adb('shell','input','swipe','1750','1030','1750','350','500');time.sleep(.5)
    ui=dump();parents={c:p for p in ui.iter() for c in p}
    owned=[]
    for n in ui.iter('node'):
        if n.get('resource-id')==TESTER+':id/package_name' and n.get('text')==PACKAGE:
            parent=parents[n]
            while parent is not ui:
                children=list(parent.iter('node'))
                buttons=[e for e in children if e.get('resource-id')==TESTER+':id/delete_button']
                skus=[e.get('text') for e in children if e.get('resource-id')==TESTER+':id/sku']
                if buttons:
                    if len(buttons)!=1 or skus!=['com.skillsprout.arcade.family']:
                        raise RuntimeError('Ambiguous transaction ownership; no reset performed')
                    owned.append(buttons[0]);break
                parent=parents[parent]
    if len(owned)!=1:raise RuntimeError('Expected exactly one visible owned subscription; no reset performed')
    tap(owned[0])
    after=dump()
    if any(n.get('resource-id')==TESTER+':id/package_name' and n.get('text')==PACKAGE for n in after.iter('node')):
        raise RuntimeError('Owned transaction is still visible; reset not verified')
    print('Removed the single SkillSprout Billing QA subscription from App Tester. No global reset or unrelated transaction action.')


if __name__=='__main__':main()
