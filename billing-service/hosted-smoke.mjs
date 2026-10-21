// Manual post-deployment probe: fabricated receipt only; never initiates a purchase.
import assert from 'node:assert/strict';
import fs from 'node:fs';

const environment=process.argv[2];
if(!['sandbox','production'].includes(environment))throw new Error('Choose sandbox or production explicitly');
const base=`https://skillsprout-receipts-${environment}.lanraybanks.workers.dev`;
const probes=[['/healthz',null,200,'ok'],['/verify',{},400,'INVALID_REQUEST'],['/verify',{userId:'skillsprout-deployment-probe',receiptId:'invalid-deployment-probe',sku:'com.skillsprout.arcade.family'},403,'RECEIPT_INACTIVE']];
const checks=[];
for(const [route,body,expected,code]of probes){
  const response=await fetch(base+route,{...(body===null?{}:{method:'POST',headers:{'content-type':'application/json'},body:JSON.stringify(body)}),signal:AbortSignal.timeout(20000)});
  const value=await response.json();assert.equal(response.status,expected);assert.equal(value.error||value.status,code);
  assert.equal(response.headers.get('cache-control'),'no-store');if(body===null)assert.equal(value.ready,true);
  checks.push({route,probe:body===null?'readiness':Object.keys(body).length?'fabricated invalid receipt through Amazon RVS':'invalid input',status:response.status,result:code});
}
const report={checkedAt:new Date().toISOString(),environment,base,checks,validPurchaseTested:false};
if(process.argv[3])fs.writeFileSync(process.argv[3],JSON.stringify(report,null,2)+'\n');
console.log(JSON.stringify(report,null,2));
