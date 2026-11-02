import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import {fileURLToPath} from 'node:url';
import {Resvg} from '@resvg/resvg-js';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'../..');
const base=path.join(root,'marketing/candidate');
const copy=JSON.parse(fs.readFileSync(path.join(base,'copy.json'),'utf8'));
const fontDir=process.env.SKILLSPROUT_MARKETING_FONTS||'C:/Windows/Fonts';
const names=['trebuc.ttf','trebucbd.ttf','arial.ttf','arialbd.ttf','Nirmala.ttc','YuGothM.ttc','malgun.ttf','msyh.ttc','msjh.ttc'];
const fontFiles=names.map(n=>path.join(fontDir,n));
for(const f of fontFiles)if(!fs.existsSync(f))throw Error('Missing authorised local font: '+path.basename(f));
const options={font:{loadSystemFonts:false,fontFiles,defaultFontFamily:'Trebuchet MS'}};
const esc=s=>String(s).replaceAll('&','&amp;').replaceAll('<','&lt;').replaceAll('>','&gt;').replaceAll('"','&quot;');
const sha=b=>crypto.createHash('sha256').update(b).digest('hex');
const manifest={generatedOn:'2026-11-02',status:'Actual localised Compose UI with synthetic explorer; independent review waived',
 captureRuntime:'Robolectric API 35 native renderer, shared GameScreen implementation; not physical Fire captures',
 educationalLanguage:'en-GB',humanLanguageApproval:false,sourceCopySha256:sha(fs.readFileSync(path.join(base,'copy.json'))),assets:[]};
const metricsCache=new Map();
function metrics(t,size,family,bold,rtl){
 const key=JSON.stringify([t,size,family,bold,rtl]);
 if(!metricsCache.has(key)){
 const result=new Resvg(`<svg xmlns="http://www.w3.org/2000/svg" width="16384" height="512"><text x="4096" y="256" direction="${rtl?'rtl':'ltr'}" font-family="${family}" font-size="${size}" font-weight="${bold?700:400}">${esc(t)}</text></svg>`,options).innerBBox();
 if(!result)throw Error('Cannot shape text');metricsCache.set(key,{x:result.x-4096,y:result.y-256,width:result.width,height:result.height});
 }return metricsCache.get(key);
}
function wrap(t,size,family,bold,rtl,width,locale){
 const tokens=/^(ja|ko|zh)/.test(locale)?[...t]:t.split(/(\s+)/);
 let line='';const output=[];
 for(const word of tokens){const next=line+word;if(line.trim()&&metrics(next.trim(),size,family,bold,rtl).width>width){output.push(line.trim());line=word.trimStart();}else line=next;}
 if(line.trim())output.push(line.trim());return output;
}
for(const [locale,c] of Object.entries(copy.locales)){
 const family=locale==='ar'?'Arial':locale==='hi'?'Nirmala UI':locale==='ja'?'Yu Gothic':locale==='ko'?'Malgun Gothic':locale==='zh-Hans'?'Microsoft YaHei':locale==='zh-Hant'?'Microsoft JhengHei':'Trebuchet MS';
 for(const [variant,[w,h]]of Object.entries({tablet:[1920,1200],phone:[1080,1920]}))for(const [game,title]of Object.entries(c.screens)){
 const source=`marketing/candidate/captures/${variant}/${locale}/${game}.png`;
 const png=fs.readFileSync(path.join(root,source));if(png.subarray(0,8).toString('hex')!=='89504e470d0a1a0a')throw Error('Not PNG');
 const sw=png.readUInt32BE(16),sh=png.readUInt32BE(20),margin=variant==='tablet'?88:64,available=w-2*margin;
 const parts=[],bounds=[];let y=58;const rtl=c.direction==='rtl';
 function paragraph(t,size,bold){for(const line of wrap(t,size,family,bold,rtl,available,locale)){
  const b=metrics(line,size,family,bold,rtl),x=rtl?w-margin-b.width:margin;
  if(x<margin-1||x+b.width>w-margin+1)throw Error('Caption overflows: '+locale);
  parts.push(`<text x="${x-b.x}" y="${y-b.y}" direction="${rtl?'rtl':'ltr'}" font-family="${family}" font-size="${size}" font-weight="${bold?700:400}" fill="#173f32">${esc(line)}</text>`);
  bounds.push({x,y,width:b.width,height:b.height});y+=Math.max(size*1.4,b.height+12);
 }}
 paragraph(title,variant==='tablet'?64:54,true);y+=12;paragraph(c.disclosure,variant==='tablet'?29:27,false);y+=24;
 const scale=Math.min(available/sw,(h-y-74)/sh),iw=sw*scale,ih=sh*scale,x=(w-iw)/2;
 if(scale<=0)throw Error('No image space');
 parts.push(`<image x="${x}" y="${y}" width="${iw}" height="${ih}" href="data:image/png;base64,${png.toString('base64')}"/>`);
 parts.push(`<text x="${margin}" y="${h-26}" font-size="21" font-family="Trebuchet MS" fill="#52664c">SkillSprout Arcade</text>`);
 const svg=`<svg xmlns="http://www.w3.org/2000/svg" width="${w}" height="${h}" xml:lang="${locale}"><title>${esc(title)}</title><desc>${esc(c.disclosure)}</desc><rect width="100%" height="100%" fill="#fff9ee"/>${parts.join('')}</svg>`;
 const output=`marketing/candidate/${locale}/${variant}/${game}`,dest=path.join(root,output);fs.mkdirSync(path.dirname(dest),{recursive:true});
 fs.writeFileSync(dest+'.svg',svg);const image=new Resvg(svg,{...options,languages:[locale]}).render().asPng();fs.writeFileSync(dest+'.png',image);
 manifest.assets.push({locale,variant,game,width:w,height:h,source,sourceSha256:sha(png),png:output+'.png',editable:output+'.svg',sha256:sha(image),captionBounds:bounds,capture:{x,y,width:iw,height:ih},localisedUiPreserved:true});
 }
 const listing={...c,status:copy.status};delete listing.screens;delete listing.direction;delete listing.headline;delete listing.disclosure;
 fs.writeFileSync(path.join(base,locale,'listing.json'),JSON.stringify(listing,null,2)+'\n');
 console.log(locale+': localised interface captures exported');
}
fs.writeFileSync(path.join(base,'manifest.json'),JSON.stringify(manifest,null,2)+'\n');
console.log('Exported '+manifest.assets.length+' screenshots');
