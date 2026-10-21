// CI-only container check. Uses fabricated credentials and ephemeral keys, never Amazon.
import {spawnSync} from 'node:child_process';
import {generateKeyPairSync} from 'node:crypto';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import assert from 'node:assert/strict';

const directory=fs.mkdtempSync(path.join(os.tmpdir(),'skillsprout-container-'));
const name=`skillsprout-receipts-test-${process.pid}`;
const cwd=path.dirname(fileURLToPath(import.meta.url));
function docker(args){
  const result=spawnSync('docker',args,{cwd,encoding:'utf8',timeout:240000});
  if(result.status!==0)throw new Error(`Docker check failed: ${args[0]}\n${result.stderr||result.error?.message||''}`);
  return result.stdout.trim();
}
try{
  const {privateKey}=generateKeyPairSync('ec',{namedCurve:'prime256v1'});
  // World-readable only for these fabricated CI fixtures, mounted read-only into non-root container.
  fs.writeFileSync(path.join(directory,'key.pem'),privateKey.export({type:'pkcs8',format:'pem'}),{mode:0o444});
  fs.writeFileSync(path.join(directory,'rvs.txt'),'fabricated-ci-secret',{mode:0o444});
  fs.chmodSync(directory,0o755);
  docker(['build','-t','skillsprout-receipts-check','.']);
  docker(['run','-d','--name',name,'--read-only','--cap-drop=ALL','--security-opt=no-new-privileges','--memory=256m','--pids-limit=64',
    '-p','127.0.0.1::8787','--mount',`type=bind,src=${directory},dst=/run/secrets,readonly`,
    '-e','RVS_ENVIRONMENT=sandbox','-e','AMAZON_PRODUCTS_JSON={"test.parent":"SUBSCRIPTION"}',
    '-e','AMAZON_RVS_SECRET_FILE=/run/secrets/rvs.txt','-e','ENTITLEMENT_PRIVATE_KEY_FILE=/run/secrets/key.pem','skillsprout-receipts-check']);
  const port=docker(['port',name,'8787/tcp']).split(':').at(-1);
  const url=`http://127.0.0.1:${port}`;
  let ready=false;
  for(let attempt=0;attempt<40;attempt++){
    try{ready=(await fetch(url+'/healthz',{signal:AbortSignal.timeout(1000)})).ok}catch{}
    if(ready)break;
    await new Promise(resolve=>setTimeout(resolve,250));
  }
  assert.ok(ready,'Container never became ready');
  assert.equal(docker(['exec',name,'id','-u']),'1000');
  const rejected=await fetch(url+'/verify',{method:'POST',headers:{'content-type':'application/json'},body:'{}'});
  assert.equal(rejected.status,400);assert.deepEqual(await rejected.json(),{error:'INVALID_REQUEST'});
  assert.equal(rejected.headers.get('cache-control'),'no-store');
  docker(['stop','--time','15',name]);
  assert.equal(docker(['inspect','--format','{{.State.ExitCode}}',name]),'0');
  console.log('Container passed: non-root startup, mounted secrets, health, denied invalid receipt and graceful shutdown.');
}finally{
  spawnSync('docker',['rm','-f',name],{stdio:'ignore'});
  fs.rmSync(directory,{recursive:true,force:true});
}
