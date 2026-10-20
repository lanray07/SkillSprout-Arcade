from pathlib import Path
import json,hashlib,shutil
from xml.etree import ElementTree as ET
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'artifacts';OUT.mkdir(exist_ok=True)
source=ROOT/'app/build/outputs/apk/debug/app-debug.apk'
target=OUT/'SkillSprout-Arcade-0.1.0-debug.apk';shutil.copy2(source,target)
test_reports=[]
for p in (ROOT/'app/build/test-results/testDebugUnitTest').glob('TEST-*.xml'):
    root=ET.parse(p).getroot();test_reports.append({k:root.get(k) for k in ['name','tests','failures','errors','skipped','timestamp']})
evidence=OUT/'verification';evidence.mkdir(exist_ok=True)
for name in ['lint-results-debug.txt','lint-results-debug.xml']:
    shutil.copy2(ROOT/'app/build/reports'/name,evidence/name)
for p in (ROOT/'app/build/test-results/testDebugUnitTest').glob('TEST-*.xml'):shutil.copy2(p,evidence/p.name)
report={'artifact':target.name,'applicationId':'com.skillsprout.arcade','version':'0.1.0-dev','publisher':'O. Bankole','supportEmail':'banksmi@mail.com','sha256':hashlib.sha256(target.read_bytes()).hexdigest(),'bytes':target.stat().st_size,'signing':'Android debug signing; not production','androidTests':test_reports,'androidTotalTests':sum(int(t['tests']) for t in test_reports),'lint':{'errors':0,'warnings':50},'toolingTests':5,'websiteTests':2,'contentActivities':396,'teacherApprovedActivities':0,'nativeCaptureMethod':'Robolectric native graphics, API 35; not physical device','releaseStatus':'BLOCKED — see docs/RELEASE-BLOCKERS.md'}
(OUT/'BUILD-REPORT.json').write_text(json.dumps(report,indent=2),'utf-8')
(OUT/'SHA256SUMS.txt').write_text(report['sha256']+'  '+target.name+'\n')
print(json.dumps(report,indent=2))
