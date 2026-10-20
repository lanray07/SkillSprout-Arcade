"""Run non-Android checks and retain actual results for the handoff report."""
from pathlib import Path
from xml.etree import ElementTree as ET
import io,json,subprocess,unittest
from datetime import datetime,timezone

ROOT=Path(__file__).resolve().parents[1]
output=ROOT/'artifacts/verification';output.mkdir(parents=True,exist_ok=True)
suite=unittest.defaultTestLoader.discover(str(ROOT/'tools'))
stream=io.StringIO();result=unittest.TextTestRunner(stream=stream,verbosity=1).run(suite)
print(stream.getvalue())
report={'at':datetime.now(timezone.utc).isoformat(),'tooling':{'tests':result.testsRun,'failures':len(result.failures),'errors':len(result.errors),'skipped':len(result.skipped)}}
success=result.wasSuccessful()
for key,directory in [('website','website'),('receiptService','billing-service')]:
    check=subprocess.run(['node','--test','--test-reporter=junit','test.mjs'],cwd=ROOT/directory,capture_output=True)
    (output/f'{key}-tests.xml').write_bytes(check.stdout)
    root=ET.fromstring(check.stdout)
    cases=list(root.iter('testcase'))
    if not cases:raise RuntimeError(f'No test cases reported by {key}')
    report[key]={'tests':len(cases),**{field:sum(case.find(tag) is not None for case in cases) for field,tag in [('failures','failure'),('errors','error'),('skipped','skipped')]}}
    success=success and check.returncode==0
    print(f'{key}: {report[key]}')
(output/'software-checks.json').write_text(json.dumps(report,indent=2)+'\n','utf-8')
raise SystemExit(0 if success else 1)
