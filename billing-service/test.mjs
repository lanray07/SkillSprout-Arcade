import test from 'node:test';
import assert from 'node:assert/strict';
import {generateKeyPairSync,verify,createHash} from 'node:crypto';
import http from 'node:http';
import {createVerifier,VerificationError} from './service.mjs';
import {createHandler} from './server.mjs';
import {loadConfiguration} from './config.mjs';
import {provisionKeys} from './setup-keys.mjs';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import {createPublicKey} from 'node:crypto';
import {createWorker} from './worker.mjs';

const {privateKey,publicKey}=generateKeyPairSync('ec',{namedCurve:'prime256v1'});
const privatePem=privateKey.export({type:'pkcs8',format:'pem'});
const now=1700000000000;
const request={userId:'test-user',receiptId:'test-receipt',sku:'test.monthly'};
const base={receiptId:request.receiptId,productId:request.sku,productType:'SUBSCRIPTION',purchaseDate:now-10000,cancelDate:null,renewalDate:now+86400000,gracePeriodEndDate:null,testTransaction:false,betaProduct:false};
const payload=token=>JSON.parse(Buffer.from(token.payloadBase64,'base64url'));
function verifier(receipt=base,options={}){
  return createVerifier({sharedSecret:'test-only-secret',products:{'test.monthly':'SUBSCRIPTION'},privateKey:privatePem,now:()=>now,fetchImpl:async()=>new Response(JSON.stringify(receipt)),...options});
}
test('verified response signs exact account, SKU, receipt, environment and bounded lease',async()=>{
  const token=await verifier()(request);const data=payload(token);
  assert.equal(data.accountHash,createHash('sha256').update(request.userId).digest('hex'));
  assert.equal(data.sku,request.sku);assert.equal(data.receiptId,request.receiptId);assert.equal(data.environment,'production');
  assert.equal(data.refreshBy,now+6*3600000);assert.equal(data.expiresAt,data.refreshBy);
  assert.ok(verify('sha256',Buffer.from(token.payloadBase64,'base64url'),publicKey,Buffer.from(token.signatureBase64,'base64url')));
});
for(const [name,change]of Object.entries({expired:{cancelDate:now-1},wrongSku:{productId:'other'},wrongReceipt:{receiptId:'other'},consumable:{productType:'CONSUMABLE'},test:{testTransaction:true},beta:{betaProduct:true},futurePurchase:{purchaseDate:now+3600000},invalidDate:{cancelDate:'tomorrow'}})){
  test(`rejects ${name} receipts`,async()=>assert.rejects(verifier({...base,...change})(request),error=>error.status===403));
}
test('cancellation boundary overrides cache duration',async()=>assert.equal(payload(await verifier({...base,cancelDate:now+60000})(request)).expiresAt,now+60000));
test('a due renewal without cancellation receives only a one-minute recheck lease',async()=>{
  for(const sandbox of [false,true]){
    const data=payload(await verifier({...base,renewalDate:now-1000},{sandbox})(request));
    assert.equal(data.expiresAt,now+60000);assert.equal(data.refreshBy,data.expiresAt);
  }
});
test('a due renewal never extends cancellation or exhausted grace',async()=>{
  for(const change of [{cancelDate:now},{gracePeriodEndDate:now-1}]){
    await assert.rejects(verifier({...base,renewalDate:now-1000,...change})(request),error=>error.status===403);
  }
  assert.equal(payload(await verifier({...base,renewalDate:now-1000,cancelDate:now+10000})(request)).expiresAt,now+10000);
});
test('active grace period can extend renewal boundary',async()=>assert.equal(payload(await verifier({...base,renewalDate:now-1,gracePeriodEndDate:now+60000})(request)).expiresAt,now+60000));
test('sandbox is explicit and encoded into the signed access',async()=>{
  let called='';const token=await verifier({...base,testTransaction:true},{sandbox:true,fetchImpl:async url=>{called=url;return new Response(JSON.stringify({...base,testTransaction:true}))}})(request);
  assert.equal(payload(token).environment,'sandbox');assert.ok(called.startsWith('https://appstore-sdk.amazon.com/sandbox/'));
});
test('input cannot choose an RVS host or an unconfigured product',async()=>{
  let called=false;const run=verifier(base,{fetchImpl:async()=>{called=true}});
  await assert.rejects(run({...request,sku:'other'}),error=>error.status===400);
  await assert.rejects(run({...request,userId:'a\nheader'}),error=>error.status===400);assert.equal(called,false);
  await assert.rejects(run({...request,childName:'unnecessary personal data'}),error=>error.status===400);
});

test('startup loads file secrets, validates binding and hides secret failures',()=>{
  const env={AMAZON_RVS_SECRET_FILE:'secret',ENTITLEMENT_PRIVATE_KEY_FILE:'key',AMAZON_PRODUCTS_JSON:JSON.stringify({'test.parent':'SUBSCRIPTION'})};
  const read=name=>name==='secret'?'test-only-secret\n':privatePem;
  const config=loadConfiguration(env,read);assert.equal(config.host,'127.0.0.1');assert.equal(config.port,8787);
  assert.equal(loadConfiguration({...env,HOST:'0.0.0.0',RVS_ENVIRONMENT:'sandbox'},read).host,'0.0.0.0');
  for(const change of [{RVS_ENVIRONMENT:'typo'},{PORT:'0'},{PORT:'1.5'},{HOST:'public.example'},{AMAZON_PRODUCTS_JSON:'[]'},{AMAZON_PRODUCTS_JSON:'null'},{AMAZON_PRODUCTS_JSON:'{}'},{AMAZON_RVS_SECRET:'conflicting-secret'}])assert.throws(()=>loadConfiguration({...env,...change},read),/configuration invalid/);
  assert.throws(()=>loadConfiguration(env,()=>{throw new Error('private-value-and-file-path')}),error=>!error.message.includes('private-value'));
});

test('key provisioning separates environments and refuses replacement or repo paths',()=>{
  const temporary=fs.mkdtempSync(path.join(os.tmpdir(),'skillsprout-key-test-'));
  const directory=path.join(temporary,'keys');
  try{
    const hashes=provisionKeys(directory);assert.notEqual(hashes.sandbox,hashes.production);
    for(const environment of ['sandbox','production']){
      const privateData=fs.readFileSync(path.join(directory,`${environment}-private.pem`));
      const publicData=fs.readFileSync(path.join(directory,`${environment}-public.der`));
      assert.deepEqual(createPublicKey(privateData).export({type:'spki',format:'der'}),publicData);
      assert.equal(createHash('sha256').update(publicData).digest('hex'),hashes[environment]);
    }
    assert.throws(()=>provisionKeys(directory));
    assert.throws(()=>provisionKeys(path.resolve('forbidden-test-keys')),/outside the repository/);
  }finally{fs.rmSync(temporary,{recursive:true,force:true})}
});

test('HTTP health stays available under verification load and concurrency recovers',async()=>{
  let release;const gate=new Promise(resolve=>{release=resolve});
  const server=http.createServer(createHandler(async()=>{await gate;return {ok:true}},{maxConcurrent:1}));
  await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
  const url=`http://127.0.0.1:${server.address().port}`;
  const post=()=>fetch(url+'/verify',{method:'POST',headers:{'content-type':'application/json'},body:'{}'});
  try{
    const first=post();
    // A socket reaching the handler signals deterministic occupancy without a sleep.
    await new Promise(resolve=>server.once('request',resolve));
    const busy=await post();assert.equal(busy.status,503);assert.equal((await busy.json()).error,'VERIFICATION_UNAVAILABLE');
    const health=await fetch(url+'/healthz');assert.equal(health.status,200);assert.deepEqual(await health.json(),{status:'ok'});assert.equal(health.headers.get('cache-control'),'no-store');
    release();assert.equal((await first).status,200);assert.equal((await post()).status,200);
  }finally{release();server.closeAllConnections();await new Promise(resolve=>server.close(resolve))}
});

test('HTTP verification issues a signature for the receipt parent and rejects child fields',async()=>{
  const parent='com.skillsprout.arcade.family';
  let calls=0;
  const verifyReceipt=verifier({...base,productId:parent},{products:{[parent]:'SUBSCRIPTION'},fetchImpl:async()=>{calls++;return new Response(JSON.stringify({...base,productId:parent}))}});
  const server=http.createServer(createHandler(verifyReceipt));await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
  const url=`http://127.0.0.1:${server.address().port}/verify`;
  const post=body=>fetch(url,{method:'POST',headers:{'content-type':'application/json'},body:JSON.stringify(body)});
  try{
    const response=await post({...request,sku:parent});assert.equal(response.status,200);
    const token=await response.json();assert.equal(payload(token).sku,parent);
    assert.ok(verify('sha256',Buffer.from(token.payloadBase64,'base64url'),publicKey,Buffer.from(token.signatureBase64,'base64url')));
    assert.equal((await post({...request,sku:parent,childName:'not accepted'})).status,400);
    assert.equal((await post({...request,sku:parent+'.monthly'})).status,400);assert.equal(calls,1);
  }finally{server.closeAllConnections();await new Promise(resolve=>server.close(resolve))}
});

const workerEnv=()=>({RVS_ENVIRONMENT:'sandbox',AMAZON_RVS_SECRET:'fabricated-worker-secret',ENTITLEMENT_PRIVATE_KEY_PEM:privatePem.toString(),AMAZON_PRODUCTS_JSON:JSON.stringify({'test.monthly':'SUBSCRIPTION'}),RECEIPT_RATE_LIMITER:{limit:async()=>({success:true})},LOCATION_RATE_LIMITER:{limit:async()=>({success:true})}});
const workerRequest=(body=request,options={})=>new Request('https://example.invalid/verify',{method:'POST',headers:{'content-type':'application/json','cf-connecting-ip':'192.0.2.1'},body:JSON.stringify(body),...options});
test('Worker verifies and signs the same sandbox protocol without logging receipt data',async()=>{
  const env=workerEnv();const rateKeys=[];env.RECEIPT_RATE_LIMITER.limit=async({key})=>{rateKeys.push(key);return {success:true}};
  const worker=createWorker({now:()=>now,fetchImpl:async()=>new Response(JSON.stringify({...base,testTransaction:true}))});
  const response=await worker.fetch(workerRequest(),env);assert.equal(response.status,200);
  const token=await response.json();assert.equal(payload(token).environment,'sandbox');
  assert.ok(verify('sha256',Buffer.from(token.payloadBase64,'base64url'),publicKey,Buffer.from(token.signatureBase64,'base64url')));
  assert.equal(rateKeys.length,1);assert.match(rateKeys[0],/^[0-9a-f]{64}$/);assert.ok(!rateKeys[0].includes('192.0.2.1'));
  assert.equal(response.headers.get('cache-control'),'no-store');
});
test('Worker readiness and verification fail closed without keys, rate bindings or explicit environment',async()=>{
  const worker=createWorker();
  for(const change of [{ENTITLEMENT_PRIVATE_KEY_PEM:''},{RVS_ENVIRONMENT:''},{RECEIPT_RATE_LIMITER:null},{AMAZON_PRODUCTS_JSON:'[]'}]){
    const env={...workerEnv(),...change};
    const health=await worker.fetch(new Request('https://example.invalid/healthz'),env);assert.equal(health.status,503);assert.deepEqual(await health.json(),{status:'unconfigured',ready:false});
    const response=await worker.fetch(workerRequest(),env);assert.equal(response.status,503);assert.deepEqual(await response.json(),{error:'VERIFICATION_UNAVAILABLE'});
  }
});
test('Worker refuses rate-limited or missing ingress context before calling Amazon',async()=>{
  let calls=0;const worker=createWorker({fetchImpl:async()=>{calls++;throw new Error('should not be reached')}});
  const env=workerEnv();env.RECEIPT_RATE_LIMITER.limit=async()=>({success:false});
  assert.equal((await worker.fetch(workerRequest(),env)).status,429);
  assert.equal((await worker.fetch(workerRequest(request,{headers:{'content-type':'application/json','x-forwarded-for':'192.0.2.1'}}),workerEnv())).status,503);
  assert.equal(calls,0);
});
test('Worker bounds streamed bodies and rejects malformed or unnecessary receipt fields',async()=>{
  let calls=0;const worker=createWorker({fetchImpl:async()=>{calls++;throw new Error('should not be reached')}});const env=workerEnv();
  assert.equal((await worker.fetch(workerRequest(request,{body:'{'}),env)).status,400);
  assert.equal((await worker.fetch(workerRequest(request,{body:' '.repeat(8193)}),env)).status,413);
  assert.equal((await worker.fetch(workerRequest({...request,childName:'not accepted'}),env)).status,400);
  assert.equal(calls,0);
});
test('Worker production rejects Amazon sandbox receipts and handles upstream failure safely',async()=>{
  const env={...workerEnv(),RVS_ENVIRONMENT:'production'};
  assert.equal((await createWorker({fetchImpl:async()=>new Response(JSON.stringify({...base,testTransaction:true})),now:()=>now}).fetch(workerRequest(),env)).status,403);
  const unavailable=await createWorker({fetchImpl:async()=>{throw new Error('secret upstream URL')}}).fetch(workerRequest(),env);
  assert.equal(unavailable.status,503);assert.deepEqual(await unavailable.json(),{error:'RVS_UNAVAILABLE'});
});
test('upstream failures never return secrets or signed access',async()=>{
  await assert.rejects(verifier(base,{fetchImpl:async()=>{throw new Error('secret-url')}})(request),error=>error.code==='RVS_UNAVAILABLE'&&!error.message.includes('secret-url'));
  await assert.rejects(verifier(base,{fetchImpl:async()=>new Response('',{status:410})})(request),error=>error.status===403);
  await assert.rejects(verifier(base,{fetchImpl:async(_url,options)=>{assert.equal(options.redirect,'manual');return new Response(null,{status:302,headers:{location:'https://must-not-follow.invalid'}})}})(request),error=>error.code==='RVS_UNAVAILABLE');
});
test('HTTP boundary validates JSON, body size, rate limit and no-store',async()=>{
  const server=http.createServer(createHandler(async()=>({ok:true})));await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
  const url=`http://127.0.0.1:${server.address().port}/verify`;
  try{
    const get=await fetch(url);assert.equal(get.status,404);
    const bad=await fetch(url,{method:'POST',body:'{}'});assert.equal(bad.status,415);
    const malformed=await fetch(url,{method:'POST',headers:{'content-type':'application/json'},body:'{'});assert.equal(malformed.status,400);
    const large=await fetch(url,{method:'POST',headers:{'content-type':'application/json'},body:' '.repeat(9000)});assert.equal(large.status,413);
    const good=await fetch(url,{method:'POST',headers:{'content-type':'application/json'},body:'{}'});assert.equal(good.status,200);assert.equal(good.headers.get('cache-control'),'no-store');
    let limited;for(let i=0;i<20;i++)limited=await fetch(url,{method:'POST',headers:{'content-type':'application/json'},body:'{}'});
    assert.equal(limited.status,429);
  }finally{server.closeAllConnections();await new Promise(resolve=>server.close(resolve))}
});
