import {createHash,createPrivateKey,sign} from 'node:crypto';

export class VerificationError extends Error {
  constructor(code,status=503){super(code);this.code=code;this.status=status;}
}
const deny=()=>{throw new VerificationError('RECEIPT_INACTIVE',403)};
const validId=value=>typeof value==='string'&&value.length>0&&value.length<=2048&&!/[\x00-\x20\x7f]/.test(value);
async function limitedJson(response){
  const reader=response.body?.getReader();if(!reader)throw new VerificationError('RVS_UNAVAILABLE');
  const chunks=[];let size=0;
  try{while(true){const {done,value}=await reader.read();if(done)break;size+=value.length;if(size>65536)throw new VerificationError('RVS_UNAVAILABLE');chunks.push(Buffer.from(value))}
    return JSON.parse(Buffer.concat(chunks).toString('utf8'));
  }finally{await reader.cancel().catch(()=>{});reader.releaseLock()}
}

export function createVerifier({sharedSecret,products,privateKey,sandbox=false,fetchImpl=fetch,now=Date.now,refreshMillis=6*60*60*1000}){
  if(!sharedSecret||!products||Object.keys(products).length===0||Object.values(products).some(type=>!['ENTITLED','SUBSCRIPTION'].includes(type)))throw new Error('Explicit Amazon products and shared secret are required');
  const key=createPrivateKey(privateKey);
  if(key.asymmetricKeyType!=='ec'||key.asymmetricKeyDetails.namedCurve!=='prime256v1')throw new Error('Use a dedicated P-256 entitlement signing key');
  if(!Number.isSafeInteger(refreshMillis)||refreshMillis<60000||refreshMillis>7*86400000)throw new Error('Invalid refresh interval');
  const environment=sandbox?'sandbox':'production';
  return async function verify(request){
    if(!request||Array.isArray(request)||Object.keys(request).length!==3||!['userId','receiptId','sku'].every(k=>validId(request[k]))||!Object.hasOwn(products,request.sku))throw new VerificationError('INVALID_REQUEST',400);
    const {userId,receiptId,sku}=request;
    const url='https://appstore-sdk.amazon.com'+(sandbox?'/sandbox':'')+'/version/1.0/verifyReceiptId/developer/'+encodeURIComponent(sharedSecret)+'/user/'+encodeURIComponent(userId)+'/receiptId/'+encodeURIComponent(receiptId);
    let response;
    try{response=await fetchImpl(url,{redirect:'error',signal:AbortSignal.timeout(7000),headers:{accept:'application/json'}})}catch{throw new VerificationError('RVS_UNAVAILABLE')}
    if([400,410,497].includes(response.status))deny();
    if(response.status!==200)throw new VerificationError('RVS_UNAVAILABLE');
    let receipt;try{receipt=await limitedJson(response)}catch{throw new VerificationError('RVS_UNAVAILABLE')}
    if(receipt.receiptId!==receiptId||receipt.productId!==sku||receipt.productType!==products[sku])deny();
    if(!sandbox&&(receipt.testTransaction!==false||receipt.betaProduct!==false))deny();
    const issuedAt=now();
    if(!Number.isSafeInteger(receipt.purchaseDate)||receipt.purchaseDate<0||receipt.purchaseDate>issuedAt+300000)deny();
    const limits=[issuedAt+refreshMillis];
    for(const field of ['cancelDate','renewalDate','gracePeriodEndDate']){
      const value=receipt[field];if(value==null)continue;
      if(!Number.isSafeInteger(value)||value<0)deny();
      // An active grace period can extend a renewal boundary, but never a cancellation.
      if(field==='renewalDate'&&Number.isSafeInteger(receipt.gracePeriodEndDate)&&receipt.gracePeriodEndDate>value)continue;
      limits.push(value);
    }
    const expiresAt=Math.min(...limits);if(expiresAt<=issuedAt)deny();
    const payload={version:1,appId:'com.skillsprout.arcade',environment,accountHash:createHash('sha256').update(userId).digest('hex'),sku,receiptId,issuedAt,expiresAt,refreshBy:expiresAt};
    const bytes=Buffer.from(JSON.stringify(payload));
    return {payloadBase64:bytes.toString('base64url'),signatureBase64:sign('sha256',bytes,key).toString('base64url')};
  };
}
