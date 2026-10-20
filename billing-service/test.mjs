import test from 'node:test';
import assert from 'node:assert/strict';
import {generateKeyPairSync,verify,createHash} from 'node:crypto';
import http from 'node:http';
import {createVerifier,VerificationError} from './service.mjs';
import {createHandler} from './server.mjs';

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
test('active grace period can extend renewal boundary',async()=>assert.equal(payload(await verifier({...base,renewalDate:now-1,gracePeriodEndDate:now+60000})(request)).expiresAt,now+60000));
test('sandbox is explicit and encoded into the signed access',async()=>{
  let called='';const token=await verifier({...base,testTransaction:true},{sandbox:true,fetchImpl:async url=>{called=url;return new Response(JSON.stringify({...base,testTransaction:true}))}})(request);
  assert.equal(payload(token).environment,'sandbox');assert.ok(called.startsWith('https://appstore-sdk.amazon.com/sandbox/'));
});
test('input cannot choose an RVS host or an unconfigured product',async()=>{
  let called=false;const run=verifier(base,{fetchImpl:async()=>{called=true}});
  await assert.rejects(run({...request,sku:'other'}),error=>error.status===400);
  await assert.rejects(run({...request,userId:'a\nheader'}),error=>error.status===400);assert.equal(called,false);
});
test('upstream failures never return secrets or signed access',async()=>{
  await assert.rejects(verifier(base,{fetchImpl:async()=>{throw new Error('secret-url')}})(request),error=>error.code==='RVS_UNAVAILABLE'&&!error.message.includes('secret-url'));
  await assert.rejects(verifier(base,{fetchImpl:async()=>new Response('',{status:410})})(request),error=>error.status===403);
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
