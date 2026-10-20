import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {createVerifier,VerificationError} from './service.mjs';

export function createHandler(verify,{now=Date.now}={}){
  const buckets=new Map();let global={start:now(),count:0};
  function allowed(ip){
    const time=now();if(time-global.start>=60000)global={start:time,count:0};
    for(const [key,value]of buckets)if(time-value.start>=60000)buckets.delete(key);
    let bucket=buckets.get(ip);
    if(!bucket){if(buckets.size>=10000)return false;bucket={start:time,count:0};buckets.set(ip,bucket)}
    return ++bucket.count<=20&&++global.count<=120;
  }
  return async(req,res)=>{
    const send=(status,value)=>{res.writeHead(status,{'content-type':'application/json','cache-control':'no-store','x-content-type-options':'nosniff'});res.end(JSON.stringify(value))};
    if(req.url!=='/verify'||req.method!=='POST'){send(404,{error:'NOT_FOUND'});req.resume();return}
    // Use the socket peer, never client-supplied forwarded headers. Configure edge limits too.
    if(!allowed(req.socket.remoteAddress||'unknown')){send(429,{error:'RATE_LIMITED'});req.resume();return}
    if(!/^application\/json(?:\s*;|$)/i.test(req.headers['content-type']||'')){send(415,{error:'JSON_REQUIRED'});req.resume();return}
    try{
      let size=0;const chunks=[];
      for await(const chunk of req){size+=chunk.length;if(size>8192){send(413,{error:'REQUEST_TOO_LARGE'});req.resume();return}chunks.push(chunk)}
      let request;try{request=JSON.parse(Buffer.concat(chunks).toString('utf8'))}catch{throw new VerificationError('INVALID_REQUEST',400)}
      send(200,await verify(request));
    }catch(error){send(error instanceof VerificationError?error.status:503,{error:error instanceof VerificationError?error.code:'VERIFICATION_UNAVAILABLE'})}
  };
}

if(process.argv[1]&&path.resolve(process.argv[1])===fileURLToPath(import.meta.url)){
  const {AMAZON_RVS_SECRET,AMAZON_PRODUCTS_JSON,ENTITLEMENT_PRIVATE_KEY_FILE,RVS_ENVIRONMENT='production'}=process.env;
  if(!['production','sandbox'].includes(RVS_ENVIRONMENT))throw new Error('Invalid RVS_ENVIRONMENT');
  const verifier=createVerifier({sharedSecret:AMAZON_RVS_SECRET,products:JSON.parse(AMAZON_PRODUCTS_JSON||'{}'),privateKey:fs.readFileSync(ENTITLEMENT_PRIVATE_KEY_FILE),sandbox:RVS_ENVIRONMENT==='sandbox'});
  const server=http.createServer({requestTimeout:10000,headersTimeout:10000},createHandler(verifier));
  server.listen(Number(process.env.PORT||8787),'127.0.0.1',()=>process.stdout.write('Receipt service listening on loopback; HTTPS ingress is required for deployment.\n'));
}
