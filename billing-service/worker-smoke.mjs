// Actual workerd runtime with synthetic credentials and an isolated Amazon mock.
import {Miniflare,convertV4MiniflareOptions} from 'miniflare';
import {execFileSync} from 'node:child_process';
import {generateKeyPairSync,verify} from 'node:crypto';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import assert from 'node:assert/strict';

const cwd=path.dirname(fileURLToPath(import.meta.url));
const directory=fs.mkdtempSync(path.join(os.tmpdir(),'skillsprout-worker-'));
let runtime;
try{
  execFileSync(process.execPath,[path.join(cwd,'node_modules/wrangler/bin/wrangler.js'),'deploy','--env','','--dry-run','--outdir',directory],{cwd,stdio:'pipe',env:{...process.env,WRANGLER_SEND_METRICS:'false'}});
  const {privateKey,publicKey}=generateKeyPairSync('ec',{namedCurve:'prime256v1'});
  const parent='com.skillsprout.arcade.family';let upstreamCalls=0;let redirect=false;
  const config=JSON.parse(fs.readFileSync(path.join(cwd,'wrangler.jsonc'),'utf8'));
  runtime=new Miniflare(convertV4MiniflareOptions({modules:true,scriptPath:path.join(directory,'worker.js'),compatibilityDate:config.compatibility_date,compatibilityFlags:config.compatibility_flags,
    bindings:{...config.vars,AMAZON_RVS_SECRET:'synthetic-runtime-only',ENTITLEMENT_PRIVATE_KEY_PEM:privateKey.export({type:'pkcs8',format:'pem'})},
    ratelimits:Object.fromEntries(config.ratelimits.map(({name,...options})=>[name,options])),
    outboundService:async request=>{
      const url=new URL(request.url);assert.equal(url.host,'appstore-sdk.amazon.com');assert.ok(url.pathname.startsWith('/sandbox/'));
      upstreamCalls++;
      if(redirect)return new Response(null,{status:302,headers:{location:'https://must-not-follow.invalid/'}});
      return new Response(JSON.stringify({receiptId:'runtime-receipt',productId:parent,productType:'SUBSCRIPTION',purchaseDate:Date.now()-10000,renewalDate:Date.now()+600000,testTransaction:true,betaProduct:false}));
    }}));
  assert.equal((await runtime.dispatchFetch('https://test.invalid/healthz')).status,200);
  const post=body=>runtime.dispatchFetch('https://test.invalid/verify',{method:'POST',headers:{'content-type':'application/json','cf-connecting-ip':'192.0.2.5'},body:JSON.stringify(body)});
  const result=await post({userId:'runtime-account',receiptId:'runtime-receipt',sku:parent});assert.equal(result.status,200,await result.clone().text());
  const token=await result.json();
  assert.ok(verify('sha256',Buffer.from(token.payloadBase64,'base64url'),publicKey,Buffer.from(token.signatureBase64,'base64url')));
  const payload=JSON.parse(Buffer.from(token.payloadBase64,'base64url'));assert.equal(payload.environment,'sandbox');assert.equal(payload.sku,parent);
  assert.equal((await post({})).status,400);assert.equal(upstreamCalls,1);
  redirect=true;assert.equal((await post({userId:'runtime-account',receiptId:'runtime-receipt',sku:parent})).status,503);assert.equal(upstreamCalls,2);
  console.log('Worker runtime passed: secret bindings, rate-limit bindings, mocked RVS call, signed lease, invalid receipt denial. No live Amazon request.');
}finally{
  if(runtime)await runtime.dispose();
  fs.rmSync(directory,{recursive:true,force:true});
}
