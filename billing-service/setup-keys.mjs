import {generateKeyPairSync,createHash} from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

export function provisionKeys(directory){
  if(!path.isAbsolute(directory))throw new Error('Use an absolute key directory');
  const target=path.resolve(directory);
  const repository=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
  const relative=path.relative(repository,target);
  if(!relative||(!relative.startsWith('..'+path.sep)&&relative!=='..'&&!path.isAbsolute(relative)))throw new Error('Choose a key directory outside the repository');
  // Refuse existing directories, including symlinks. Never rotate keys implicitly.
  fs.mkdirSync(target,{mode:0o700});
  const fingerprints={};
  for(const environment of ['sandbox','production']){
    const {privateKey,publicKey}=generateKeyPairSync('ec',{namedCurve:'prime256v1'});
    fs.writeFileSync(path.join(target,`${environment}-private.pem`),privateKey.export({type:'pkcs8',format:'pem'}),{flag:'wx',mode:0o600});
    const der=publicKey.export({type:'spki',format:'der'});
    fs.writeFileSync(path.join(target,`${environment}-public.der`),der,{flag:'wx',mode:0o644});
    fingerprints[environment]=createHash('sha256').update(der).digest('hex');
  }
  return fingerprints;
}
if(process.argv[1]&&path.resolve(process.argv[1])===fileURLToPath(import.meta.url)){
  if(process.argv.length!==3)throw new Error('Usage: node setup-keys.mjs <new-absolute-directory-outside-repository>');
  console.log(JSON.stringify({publicKeySha256:provisionKeys(process.argv[2])},null,2));
}
