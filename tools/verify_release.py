"""Conservative release gate; requires real publisher-supplied evidence, never invents it."""
import json,sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
evidence_path=ROOT/'release/evidence.json'
required=['teacherApprovedContent','physicalFireQa','accessibilityQa','amazonBillingQa','legalPrivacyReview','brandClearance','productionSigning','localisationReview','hostedPrivacyAndSupport']
evidence=json.loads(evidence_path.read_text('utf-8')) if evidence_path.exists() else {}
blocked=[]
for key in required:
    record=evidence.get(key,{})
    if record.get('status')!='Approved' or not record.get('reviewer') or not record.get('artifact'):blocked.append(key)
pack=json.loads((ROOT/'app/src/main/assets/packs/starter.json').read_text('utf-8'))
if any(a['reviewStatus']!='Approved' for a in pack['activities']):blocked.append('unapproved bundled activities')
if blocked:
    print('RELEASE BLOCKED:\n- '+'\n- '.join(blocked));sys.exit(1)
print('Evidence gate passed. Publisher must still validate the exact signed candidate and store declarations.')
