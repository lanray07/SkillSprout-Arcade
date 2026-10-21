"""Password-encrypted portable signing backup. Run locally; never send passwords to chat."""
import argparse, base64, getpass, hashlib, json, os
from pathlib import Path
from cryptography.hazmat.primitives.ciphers.aead import AESGCM
from cryptography.hazmat.primitives.kdf.scrypt import Scrypt

ROOT = Path(__file__).resolve().parents[1]
REQUIRED = {'signing.properties','skillsprout-production.jks','publisher-certificate.pem','content-private.pem','content-password.txt'}
AAD = b'SkillSprout signing backup v1'
MAX_BYTES = 16*1024*1024

def key(password, salt):
    return Scrypt(salt=salt,length=32,n=2**17,r=8,p=1).derive(password.encode('utf-8'))

def encrypt(files, password):
    if len(password)<16: raise ValueError('Use a unique passphrase of at least 16 characters; store it separately.')
    if set(files)!=REQUIRED: raise ValueError('The complete five-file signing folder is required.')
    if not all(isinstance(v,bytes) and v for v in files.values()) or sum(map(len,files.values()))>MAX_BYTES:
        raise ValueError('Empty or oversized signing files')
    payload=json.dumps({name:base64.b64encode(data).decode('ascii') for name,data in files.items()}).encode()
    salt=os.urandom(16);nonce=os.urandom(12)
    ciphertext=AESGCM(key(password,salt)).encrypt(nonce,payload,AAD)
    return json.dumps({'format':'skillsprout-signing-backup-v1','salt':base64.b64encode(salt).decode(),
        'nonce':base64.b64encode(nonce).decode(),'ciphertext':base64.b64encode(ciphertext).decode()}).encode()

def decrypt(data, password):
    if len(data)>MAX_BYTES*3: raise ValueError('Backup too large')
    envelope=json.loads(data)
    if set(envelope)!={'format','salt','nonce','ciphertext'} or envelope['format']!='skillsprout-signing-backup-v1':
        raise ValueError('Unknown backup format')
    salt,nonce,ciphertext=(base64.b64decode(envelope[k],validate=True) for k in ['salt','nonce','ciphertext'])
    if len(salt)!=16 or len(nonce)!=12: raise ValueError('Invalid backup parameters')
    decoded=json.loads(AESGCM(key(password,salt)).decrypt(nonce,ciphertext,AAD))
    if set(decoded)!=REQUIRED: raise ValueError('Incomplete signing backup')
    files={name:base64.b64decode(value,validate=True) for name,value in decoded.items()}
    if not all(files.values()) or sum(map(len,files.values()))>MAX_BYTES: raise ValueError('Invalid signing files')
    return files

def private_destination(path):
    path=path.expanduser().resolve()
    if path==ROOT or ROOT in path.parents or any((p/'.git').exists() for p in [path,*path.parents]):
        raise ValueError('Choose a private destination outside all Git checkouts.')
    if not path.parent.is_dir(): raise ValueError('Destination parent directory must already exist.')
    if path.exists(): raise ValueError('Refusing to overwrite an existing destination.')
    return path

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('command',choices=['create','verify','restore'])
    parser.add_argument('--source',type=Path,default=Path.home()/'.skillsprout/release-signing')
    parser.add_argument('--backup',type=Path,required=True)
    parser.add_argument('--destination',type=Path,help='New private directory for a recovery test; outside Git')
    args=parser.parse_args()
    password=getpass.getpass('Private backup passphrase (never sent to Codex): ')
    if args.command=='create':
        if password!=getpass.getpass('Repeat passphrase: '): raise ValueError('Passphrases differ')
        destination=private_destination(args.backup)
        source=args.source.expanduser().resolve()
        if any((source/name).is_symlink() for name in REQUIRED): raise ValueError('Signing files must not be symbolic links')
        files={name:(source/name).read_bytes() for name in REQUIRED}
        encrypted=encrypt(files,password)
        # Verify before writing; never place plaintext keys in an intermediate archive.
        if decrypt(encrypted,password)!=files: raise ValueError('Backup verification failed')
        fd=os.open(destination,os.O_WRONLY|os.O_CREAT|os.O_EXCL,0o600)
        with os.fdopen(fd,'wb') as output: output.write(encrypted)
        print('Encrypted backup created and decrypted successfully in memory. SHA-256:',hashlib.sha256(encrypted).hexdigest())
        print('Store it off this computer and keep the passphrase separately. This alone does not approve signing.')
    else:
        files=decrypt(args.backup.expanduser().read_bytes(),password)
        if args.command=='verify':
            if args.source.is_dir() and any(files[name]!=(args.source/name).read_bytes() for name in REQUIRED):
                raise ValueError('Backup differs from the selected signing folder')
            print('Backup authenticates and contains all five files; no private contents displayed.')
        else:
            if not args.destination: parser.error('restore requires --destination')
            destination=private_destination(args.destination)
            destination.mkdir(mode=0o700)
            if os.name=='nt':
                import subprocess
                user=subprocess.check_output(['whoami'],text=True).strip()
                subprocess.run(['icacls',str(destination),'/inheritance:r','/grant:r',user+':(OI)(CI)F','SYSTEM:(OI)(CI)F'],check=True,stdout=subprocess.DEVNULL)
            for name,data in files.items():
                fd=os.open(destination/name,os.O_WRONLY|os.O_CREAT|os.O_EXCL,0o600)
                with os.fdopen(fd,'wb') as output: output.write(data)
            print('Restored to the new private directory. signing.properties retains its original absolute keystore path; update that path if recovering on a different computer.')

if __name__=='__main__': main()
