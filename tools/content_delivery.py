"""Sign reviewed content packs for offline parent import; private material never enters Git.

Requires cryptography. Run keygen once, then sign --pack ... --pack-id ... --version ... --out ...
Signing never approves content. Production signing refuses items without real review records.
"""
import argparse,base64,hashlib,json,os,secrets
from pathlib import Path
from cryptography.hazmat.primitives import hashes,serialization
from cryptography.hazmat.primitives.asymmetric import ec
import review_content

ROOT=Path(__file__).resolve().parents[1]
PRIVATE=Path.home()/'.skillsprout/release-signing/content-private.pem'
PASSWORD=PRIVATE.with_name('content-password.txt')
PUBLIC=ROOT/'app/src/main/assets/content-publisher.der'

def keygen():
    if PRIVATE.exists() or PASSWORD.exists() or PUBLIC.exists():
        raise ValueError('Content identity already exists; do not overwrite or rotate it silently.')
    if not PRIVATE.parent.is_dir():raise ValueError('Run tools/setup-signing.ps1 first to create the restricted private directory.')
    password=secrets.token_urlsafe(48).encode()
    key=ec.generate_private_key(ec.SECP256R1())
    # Exclusive creation prevents accidental replacement of an existing key.
    with PRIVATE.open('xb') as out:out.write(key.private_bytes(serialization.Encoding.PEM,serialization.PrivateFormat.PKCS8,serialization.BestAvailableEncryption(password)))
    with PASSWORD.open('xb') as out:out.write(password)
    PUBLIC.write_bytes(key.public_key().public_bytes(serialization.Encoding.DER,serialization.PublicFormat.SubjectPublicKeyInfo))
    print('Created encrypted private content identity outside Git; only the public verification key is bundled.')

def message(manifest):
    return ('SkillSprout-Pack-v1\n'+ '\n'.join(str(manifest[k]) for k in ['packId','version','locale','sha256'])).encode()

def envelope(payload,pack_id,version,key,development=False):
    import re
    if not re.fullmatch(r'[a-z0-9][a-z0-9-]{0,63}',pack_id) or version<1:raise ValueError('Invalid pack ID or version')
    if len(payload)>5_000_000:raise ValueError('Pack exceeds 5 MB')
    pack=json.loads(payload);review_content.validate(pack)
    if pack.get('schemaVersion')!=1 or pack.get('locale')!='en-GB':raise ValueError('Unsupported schema or educational locale')
    if any(not a['id'].startswith(pack_id+':') for a in pack['activities']):raise ValueError('Activity IDs must start with pack-id:')
    if not development and any(a['reviewStatus']!='Approved' for a in pack['activities']):raise ValueError('Unapproved content cannot be signed for release')
    manifest=dict(packId=pack_id,version=version,locale='en-GB',sha256=hashlib.sha256(payload).hexdigest())
    return dict(format=1,manifest=manifest,payload=base64.b64encode(payload).decode(),signature=base64.b64encode(key.sign(message(manifest),ec.ECDSA(hashes.SHA256()))).decode())

def sign(args):
    payload=Path(args.pack).read_bytes()
    if not args.development:
        digest=hashlib.sha256(payload).hexdigest()
        record=ROOT/'content-reviews'/f'{digest}.json'
        if not record.exists():raise ValueError('A recorded review of the exact pack digest is required')
        review=json.loads(record.read_text('utf-8'))
        if review.get('status')!='Approved' or not all(review.get(k) for k in ['reviewer','qualifications','reviewedAt','evidence']) or review.get('packSha256')!=digest:
            raise ValueError('The exact content review has not been approved')
    key=serialization.load_pem_private_key(PRIVATE.read_bytes(),PASSWORD.read_bytes())
    result=envelope(payload,args.pack_id,args.version,key,args.development)
    with Path(args.out).open('x',encoding='utf-8') as out:json.dump(result,out,separators=(',',':'))
    print('Signed pack saved. The app still checks content approval in release builds.')

if __name__=='__main__':
    parser=argparse.ArgumentParser();sub=parser.add_subparsers(dest='command',required=True)
    sub.add_parser('keygen');p=sub.add_parser('sign')
    p.add_argument('--pack',required=True);p.add_argument('--pack-id',required=True);p.add_argument('--version',required=True,type=int);p.add_argument('--out',required=True);p.add_argument('--development',action='store_true')
    args=parser.parse_args()
    try:keygen() if args.command=='keygen' else sign(args)
    except (ValueError,OSError) as error:parser.exit(1,str(error)+'\n')
