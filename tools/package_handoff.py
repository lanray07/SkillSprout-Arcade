"""Package a tested development APK using actual metadata and check reports."""
from pathlib import Path
import json,hashlib,shutil
from xml.etree import ElementTree as ET
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'artifacts';OUT.mkdir(exist_ok=True)
source=ROOT/'app/build/outputs/apk/debug/app-debug.apk'
metadata=json.loads(source.with_name('output-metadata.json').read_text('utf-8'))
version=metadata['elements'][0]['versionName']
native_inputs=list((ROOT/'app/src').rglob('*.kt'))+list((ROOT/'app/src/main/res').rglob('*.xml'))+[ROOT/'app/build.gradle.kts']
newest_input=max(p.stat().st_mtime for p in native_inputs)
test_reports=[]
for module in ['app','billing-amazon']:
    files=list((ROOT/module/'build/test-results/testDebugUnitTest').glob('TEST-*.xml'))
    if not files:raise SystemExit(f'No actual test reports for {module}')
    for p in files:
        if module=='app' and p.stat().st_mtime<newest_input:raise SystemExit('Android reports predate source changes; rerun checks before packaging')
        root=ET.parse(p).getroot();record={k:root.get(k) for k in ['name','tests','failures','errors','skipped','timestamp']};record['module']=module
        if int(record['failures'])+int(record['errors']):raise SystemExit('Failing Android checks; no handoff created')
        test_reports.append(record)
checks=json.loads((OUT/'verification/software-checks.json').read_text('utf-8'))
if any(checks[k]['failures']+checks[k]['errors'] for k in ['tooling','website','receiptService']):raise SystemExit('Non-Android checks failed')
lint=ET.parse(ROOT/'app/build/reports/lint-results-debug.xml').getroot()
lint_totals={'errors':sum(i.get('severity') in ['Error','Fatal'] for i in lint),'warnings':sum(i.get('severity')=='Warning' for i in lint)}
if lint_totals['errors']:raise SystemExit('Lint errors; no handoff created')
target=OUT/f'SkillSprout-Arcade-{version.removesuffix("-dev")}-debug.apk';shutil.copy2(source,target)
pack=json.loads((ROOT/'app/src/main/assets/packs/starter.json').read_text('utf-8'))
publisher=json.loads((ROOT/'website/site.config.json').read_text('utf-8'))
evidence=OUT/'verification';evidence.mkdir(exist_ok=True)
for name in ['lint-results-debug.txt','lint-results-debug.xml']:shutil.copy2(ROOT/'app/build/reports'/name,evidence/name)
for module in ['app','billing-amazon']:
    for p in (ROOT/module/'build/test-results/testDebugUnitTest').glob('TEST-*.xml'):shutil.copy2(p,evidence/f'{module}-{p.name}')
report={'artifact':target.name,'applicationId':metadata['applicationId'],'version':version,'versionCode':metadata['elements'][0]['versionCode'],'publisher':publisher['publisher'],'supportEmail':publisher['supportEmail'],'sha256':hashlib.sha256(target.read_bytes()).hexdigest(),'bytes':target.stat().st_size,'signing':'Android debug signing; not production','androidTests':test_reports,'androidTotalTests':sum(int(t['tests']) for t in test_reports),'lint':lint_totals,'nonAndroidChecks':checks,'contentActivities':len(pack['activities']),'teacherApprovedActivities':sum(a['reviewStatus']=='Approved' for a in pack['activities']),'nativeCaptureMethod':'Robolectric native graphics, API 35; not physical device','releaseStatus':'BLOCKED — see docs/RELEASE-BLOCKERS.md'}
(OUT/'BUILD-REPORT.json').write_text(json.dumps(report,indent=2)+'\n','utf-8')
(OUT/'SHA256SUMS.txt').write_text(report['sha256']+'  '+target.name+'\n','utf-8')
print(json.dumps(report,indent=2))
