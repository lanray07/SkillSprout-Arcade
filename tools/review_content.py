"""Validate packs and track explicit human reviews. No synthetic teacher approvals."""
import argparse,json,hashlib,re
from datetime import date
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
STATUSES=['Draft','Automated Validation Passed','Awaiting Teacher Review','Approved','Rejected','Retired']
def validate(pack):
    ids=set()
    for a in pack['activities']:
        assert a['id'] not in ids;ids.add(a['id'])
        assert all(a[k] for k in ['id','subject','skill','objective','reference','answer','explanation','hints'])
        assert a['year'] in range(1,7) and a['difficulty'] in range(1,4)
        assert a['reviewStatus'] in STATUSES and a['version']>=1
        assert a['type'] in ['choice','word','letters','sequence']
        assert 1 <= len(a['choices']) <= 40 and all(isinstance(c,str) and c.strip() for c in a['choices'])
        if a['type'] in ['choice','word']:assert a['answer'] in a['choices'] and len(set(a['choices']))==len(a['choices'])
        if a['type']=='letters':assert sorted(a['choices'])==sorted(a['answer'])
        if a['type']=='sequence':assert sorted(a['choices'])==sorted(a['answer'].split())
        visual=a.get('visual','')
        if visual.startswith('clock:'):
            match=re.fullmatch(r'clock:([0-9]{1,2}):([0-9]{2})',visual)
            assert match and 1<=int(match[1])<=12 and 0<=int(match[2])<=59, 'Invalid clock visual'
        if visual=='choice-shapes':assert a['type']=='choice' and all(c in ['Triangle','Circle','Square'] for c in a['choices'])
        if visual.startswith('fruit:'):
            match=re.fullmatch(r'fruit:([0-9]{1,2}):([0-9]{1,2})',visual)
            assert match and all(1<=int(n)<=10 for n in match.groups()), 'Invalid fruit visual'
    graph={}
    for a in pack['activities']:
        assert isinstance(a['prerequisites'],list) and all(isinstance(p,str) and p for p in a['prerequisites'])
        graph.setdefault(a['skill'],set()).update(a['prerequisites'])
    # Optional packs may refer to a skill in another pack; cycles in the local graph are still invalid.
    visiting=set();done=set()
    def visit(skill):
        assert skill not in visiting, 'Prerequisite cycle: '+skill
        if skill in done:return
        visiting.add(skill)
        for prerequisite in graph.get(skill,[]):visit(prerequisite)
        visiting.remove(skill);done.add(skill)
    for skill in graph:visit(skill)
    return len(ids)

def validate_review(pack, pack_bytes, review):
    """Validate supplied evidence, including explicit per-item decisions from the review desk."""
    for field in ['reviewer','qualifications','reviewedAt','evidence']:
        if not isinstance(review.get(field),str) or not review[field].strip():
            raise ValueError('Missing review field: '+field)
    date.fromisoformat(review['reviewedAt'])
    if review.get('packSha256')!=hashlib.sha256(pack_bytes).hexdigest():
        raise ValueError('Review refers to a different content pack')
    if review.get('status') not in ['Approved','Rejected']:
        raise ValueError('Only a completed Approved/Rejected review can be recorded')
    if 'activityDecisions' in review:
        decisions=review['activityDecisions'];ids={a['id'] for a in pack['activities']}
        if not isinstance(decisions,list) or any(not isinstance(d,dict) for d in decisions):
            raise ValueError('Invalid activity decisions')
        if len(decisions)!=len(ids) or {d.get('id') for d in decisions}!=ids:
            raise ValueError('Every activity must have exactly one decision')
        if any(d.get('status') not in ['Pending','Approved','Rejected'] for d in decisions):
            raise ValueError('Invalid activity decision status')
        if review['status']=='Approved' and any(d['status']!='Approved' for d in decisions):
            raise ValueError('Unreviewed or rejected activities cannot receive pack approval')
        if review['status']=='Rejected' and not any(d['status']=='Rejected' for d in decisions):
            raise ValueError('A rejected review must identify a rejected activity')
    return review
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('command',choices=['validate','release-check','record-review']);p.add_argument('--pack',default=str(ROOT/'app/src/main/assets/packs/starter.json'));p.add_argument('--review-file')
    args=p.parse_args();file=Path(args.pack);pack=json.loads(file.read_text('utf-8'));print('Validated',validate(pack),'activities')
    if args.command=='record-review':
        if not args.review_file:p.error('--review-file is required')
        review=json.loads(Path(args.review_file).read_text('utf-8'))
        validate_review(pack,file.read_bytes(),review)
        # Records evidence separately. Updating a signed approved bundle is a publisher-controlled release step.
        # Completed reviewer identities/evidence are private by default, outside the public checkout.
        out=Path.home()/'.skillsprout/content-reviews';out.mkdir(parents=True,exist_ok=True)
        (out/(review['packSha256']+'.json')).write_text(json.dumps(review,indent=2),'utf-8');print('Human-supplied review recorded; pack unchanged.')
    if args.command=='release-check':
        not_approved=[a['id'] for a in pack['activities'] if a['reviewStatus']!='Approved']
        if not_approved:raise SystemExit(f'RELEASE BLOCKED: {len(not_approved)} activities await teacher approval.')
