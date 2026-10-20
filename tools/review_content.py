"""Validate packs and track explicit human reviews. No synthetic teacher approvals."""
import argparse,json,hashlib
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
        if a['type']=='choice':assert a['answer'] in a['choices'] and len(set(a['choices']))==len(a['choices'])
        if a['type']=='letters':assert sorted(a['choices'])==sorted(a['answer'])
        if a['type']=='sequence':assert sorted(a['choices'])==sorted(a['answer'].split())
    return len(ids)
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('command',choices=['validate','release-check','record-review']);p.add_argument('--pack',default=str(ROOT/'app/src/main/assets/packs/starter.json'));p.add_argument('--review-file')
    args=p.parse_args();file=Path(args.pack);pack=json.loads(file.read_text('utf-8'));print('Validated',validate(pack),'activities')
    if args.command=='record-review':
        if not args.review_file:p.error('--review-file is required')
        review=json.loads(Path(args.review_file).read_text('utf-8'))
        assert review.get('reviewer') and review.get('qualifications') and review.get('reviewedAt') and review.get('evidence')
        assert review.get('packSha256')==hashlib.sha256(file.read_bytes()).hexdigest()
        assert review.get('status') in ['Approved','Rejected']
        # Records evidence separately. Updating a signed approved bundle is a publisher-controlled release step.
        out=ROOT/'content-reviews';out.mkdir(exist_ok=True)
        (out/(review['packSha256']+'.json')).write_text(json.dumps(review,indent=2),'utf-8');print('Human-supplied review recorded; pack unchanged.')
    if args.command=='release-check':
        not_approved=[a['id'] for a in pack['activities'] if a['reviewStatus']!='Approved']
        if not_approved:raise SystemExit(f'RELEASE BLOCKED: {len(not_approved)} activities await teacher approval.')
