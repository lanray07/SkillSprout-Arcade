import fs from 'node:fs';
import {createVerifier} from './service.mjs';

/** Read credentials only at startup; never include values or paths in errors. */
export function loadConfiguration(env=process.env,readFile=file=>fs.readFileSync(file,'utf8')){
  try{
    const environment=env.RVS_ENVIRONMENT||'production';
    if(!['production','sandbox'].includes(environment))throw new Error();
    const port=Number(env.PORT||8787);
    if(!Number.isInteger(port)||port<1||port>65535)throw new Error();
    const host=env.HOST||'127.0.0.1';
    if(!['127.0.0.1','0.0.0.0','::1'].includes(host))throw new Error();
    if(env.AMAZON_RVS_SECRET&&env.AMAZON_RVS_SECRET_FILE)throw new Error();
    const sharedSecret=env.AMAZON_RVS_SECRET_FILE?readFile(env.AMAZON_RVS_SECRET_FILE).trim():env.AMAZON_RVS_SECRET;
    if(typeof sharedSecret!=='string'||!sharedSecret.trim()||/[\x00-\x20\x7f]/.test(sharedSecret))throw new Error();
    const products=JSON.parse(env.AMAZON_PRODUCTS_JSON||'{}');
    if(!products||Array.isArray(products)||typeof products!=='object'||Object.keys(products).some(sku=>!sku||sku.length>200||/[\x00-\x20\x7f]/.test(sku)))throw new Error();
    const verify=createVerifier({sharedSecret,products,privateKey:readFile(env.ENTITLEMENT_PRIVATE_KEY_FILE),sandbox:environment==='sandbox'});
    return {host,port,verify};
  }catch{throw new Error('Receipt service configuration invalid; check environment, products and mounted secret files.');}
}
