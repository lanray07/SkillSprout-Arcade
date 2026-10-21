import {createHmac} from 'node:crypto';
import {createVerifier,VerificationError} from './service.mjs';

const json=(status,value)=>new Response(JSON.stringify(value),{status,headers:{'content-type':'application/json','cache-control':'no-store','x-content-type-options':'nosniff'}});
async function readRequest(request){
  if(Number(request.headers.get('content-length'))>8192)throw new VerificationError('REQUEST_TOO_LARGE',413);
  const reader=request.body?.getReader();if(!reader)throw new VerificationError('INVALID_REQUEST',400);
  let timedOut=false;const timer=setTimeout(()=>{timedOut=true;reader.cancel().catch(()=>{})},7000);
  try{
    let size=0;const chunks=[];
    while(true){const {done,value}=await reader.read();if(timedOut)throw new VerificationError('REQUEST_TIMEOUT',408);if(done)break;
      size+=value.length;if(size>8192)throw new VerificationError('REQUEST_TOO_LARGE',413);chunks.push(Buffer.from(value));}
    try{return JSON.parse(Buffer.concat(chunks).toString('utf8'))}catch{throw new VerificationError('INVALID_REQUEST',400)}
  }finally{clearTimeout(timer);await reader.cancel().catch(()=>{});reader.releaseLock()}
}

/** Fetch adapter: same receipt validation and DER signatures as the Docker service. */
export function createWorker({fetchImpl=fetch,now=Date.now,maxConcurrent=16}={}){
  const cache=new WeakMap();let active=0;
  function verifier(env){
    if(cache.has(env))return cache.get(env);
    if(!['sandbox','production'].includes(env.RVS_ENVIRONMENT)||!env.AMAZON_RVS_SECRET||!env.ENTITLEMENT_PRIVATE_KEY_PEM||
      typeof env.RECEIPT_RATE_LIMITER?.limit!=='function'||typeof env.LOCATION_RATE_LIMITER?.limit!=='function')throw new Error('Configuration incomplete');
    const products=JSON.parse(env.AMAZON_PRODUCTS_JSON||'{}');
    if(!products||Array.isArray(products)||typeof products!=='object')throw new Error('Configuration invalid');
    const verify=createVerifier({sharedSecret:env.AMAZON_RVS_SECRET,privateKey:env.ENTITLEMENT_PRIVATE_KEY_PEM,products,sandbox:env.RVS_ENVIRONMENT==='sandbox',fetchImpl,now});
    cache.set(env,verify);return verify;
  }
  return {async fetch(request,env){
    const url=new URL(request.url);
    if(url.pathname==='/healthz'&&url.search===''&&request.method==='GET'){
      try{verifier(env);return json(200,{status:'ok',ready:true})}catch{return json(503,{status:'unconfigured',ready:false})}
    }
    if(url.pathname!=='/verify'||url.search!==''||request.method!=='POST')return json(404,{error:'NOT_FOUND'});
    if(!/^application\/json(?:\s*;|$)/i.test(request.headers.get('content-type')||''))return json(415,{error:'JSON_REQUIRED'});
    if(active>=maxConcurrent)return json(503,{error:'VERIFICATION_UNAVAILABLE'});
    active++;
    try{
      const verify=verifier(env);
      // Cloudflare supplies this header at ingress; never use client X-Forwarded-For.
      const ip=request.headers.get('cf-connecting-ip');if(!ip)throw new Error('Ingress context missing');
      const key=createHmac('sha256',env.AMAZON_RVS_SECRET).update(ip).digest('hex');
      if(!(await env.LOCATION_RATE_LIMITER.limit({key:'receipts'})).success||!(await env.RECEIPT_RATE_LIMITER.limit({key})).success)return json(429,{error:'RATE_LIMITED'});
      return json(200,await verify(await readRequest(request)));
    }catch(error){return json(error instanceof VerificationError?error.status:503,{error:error instanceof VerificationError?error.code:'VERIFICATION_UNAVAILABLE'})}
    finally{active--}
  }};
}

export default createWorker();
