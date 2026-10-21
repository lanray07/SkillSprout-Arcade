import http from 'node:http';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {VerificationError} from './service.mjs';
import {loadConfiguration} from './config.mjs';

export function createHandler(verify,{now=Date.now,maxConcurrent=16}={}){
  if(!Number.isInteger(maxConcurrent)||maxConcurrent<1)throw new Error('Invalid concurrency limit');
  let active=0;
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
    if(req.url==='/healthz'&&req.method==='GET'){send(200,{status:'ok'});return}
    if(req.url!=='/verify'||req.method!=='POST'){send(404,{error:'NOT_FOUND'});req.resume();return}
    // Use the socket peer, never client-supplied forwarded headers. Configure edge limits too.
    if(!allowed(req.socket.remoteAddress||'unknown')){send(429,{error:'RATE_LIMITED'});req.resume();return}
    if(!/^application\/json(?:\s*;|$)/i.test(req.headers['content-type']||'')){send(415,{error:'JSON_REQUIRED'});req.resume();return}
    if(active>=maxConcurrent){send(503,{error:'VERIFICATION_UNAVAILABLE'});req.resume();return}
    active++;
    try{
      let size=0;const chunks=[];
      for await(const chunk of req){size+=chunk.length;if(size>8192){send(413,{error:'REQUEST_TOO_LARGE'});req.resume();return}chunks.push(chunk)}
      let request;try{request=JSON.parse(Buffer.concat(chunks).toString('utf8'))}catch{throw new VerificationError('INVALID_REQUEST',400)}
      send(200,await verify(request));
    }catch(error){if(!res.destroyed)send(error instanceof VerificationError?error.status:503,{error:error instanceof VerificationError?error.code:'VERIFICATION_UNAVAILABLE'})}
    finally{active--}
  };
}

if(process.argv[1]&&path.resolve(process.argv[1])===fileURLToPath(import.meta.url)){
  const {verify,host,port}=loadConfiguration();
  const server=http.createServer({requestTimeout:10000,headersTimeout:10000,maxHeaderSize:8192},createHandler(verify));
  server.maxConnections=128;
  server.setTimeout(10000,socket=>socket.destroy());
  server.listen(port,host,()=>process.stdout.write('Receipt service ready; HTTPS ingress is required.\n'));
  for(const signal of ['SIGTERM','SIGINT'])process.once(signal,()=>{
    server.close(()=>process.exit(0));
    setTimeout(()=>{server.closeAllConnections();process.exit(1)},12000).unref();
  });
}
