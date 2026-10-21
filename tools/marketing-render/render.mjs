// Deterministic layouts: original English captures remain intact; only surrounding copy is localized.
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import {fileURLToPath} from 'node:url';
import {Resvg} from '@resvg/resvg-js';

const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'../..');
const base=path.join(root,'marketing/localized');
const copy=JSON.parse(fs.readFileSync(path.join(base,'copy.json'),'utf8'));
const fontDir=process.env.SKILLSPROUT_MARKETING_FONTS || 'C:/Windows/Fonts';
const fontFiles=['trebuc.ttf','trebucbd.ttf','arial.ttf','arialbd.ttf','Nirmala.ttc'].map(f=>path.join(fontDir,f));
for(const file of fontFiles)if(!fs.existsSync(file))throw new Error('Required shaping font missing: '+file);
const options={font:{loadSystemFonts:false,fontFiles,defaultFontFamily:'Trebuchet MS'},textRendering:2,imageRendering:0};
const esc=s=>String(s).replaceAll('&','&amp;').replaceAll('<','&lt;').replaceAll('>','&gt;').replaceAll('"','&quot;');
const hash=b=>crypto.createHash('sha256').update(b).digest('hex');
const sizes={tablet:[1920,1200],phone:[1080,1920]};
// Description and image locale selectors differ in the actual Amazon draft console.
const mediaLocales=new Set(['en-US','de','es','fr','it','pt-BR']);
const measurement=new Map();
function textNode(text,size,family,bold,direction){
  // Explicit embedding preserves the RTL paragraph order around Latin tokens such as PIN/JVM.
  const shaped=direction==='rtl'?'\u202B'+text+'\u202C':text;
  return `<text x="0" y="0" direction="${direction}" font-family="${family}" font-size="${size}" font-weight="${bold?700:400}">${esc(shaped)}</text>`;
}
function bounds(text,size,family,bold,direction){
  const key=JSON.stringify([text,size,family,bold,direction]);
  if(!measurement.has(key)){
    const sample=textNode(text,size,family,bold,direction).replace('x="0" y="0"','x="4096" y="256"');
    const svg=`<svg xmlns="http://www.w3.org/2000/svg" width="16384" height="512">${sample}</svg>`;
    const b=new Resvg(svg,options).innerBBox();
    if(!b || b.width<=0 || b.height<=0)throw new Error('Text shaping failed: '+text);
    measurement.set(key,{x:b.x-4096,y:b.y-256,width:b.width,height:b.height});
  }
  return measurement.get(key);
}
function lines(text,size,family,bold,direction,width){
  const output=[];let line='';
  for(const word of text.split(/\s+/)){
    if(bounds(word,size,family,bold,direction).width>width)throw new Error('Word exceeds safe width: '+word);
    const candidate=line?line+' '+word:word;
    if(line && bounds(candidate,size,family,bold,direction).width>width){output.push(line);line=word;}else line=candidate;
  }
  if(line)output.push(line);
  return output;
}
const manifest={generatedOn:'2026-10-21',status:copy.status,renderer:'@resvg/resvg-js 2.6.2; shaped text and measured ink bounds',appLanguages:['en-GB'],translationReview:'Pending native-language review',profiles:[],assets:[]};
for(const [locale,c]of Object.entries(copy.locales)){
  if(c.screens.length!==8 || c.featureBullets.length<3 || c.featureBullets.length>5 || c.displayTitle.length>200 || Buffer.byteLength(c.shortDescription)>2000 || c.longDescription.length>4000)throw new Error('Invalid listing: '+locale);
  const family=locale==='ar'?'Arial':locale==='hi'?'Nirmala UI':'Trebuchet MS';
  const listing={locale,amazonLocale:c.amazonLocale,amazonLabel:c.amazonLabel,status:'Draft translation - native review pending',appInterfaceAndContent:'English only',displayTitle:c.displayTitle,shortDescription:c.shortDescription,longDescription:c.longDescription,featureBullets:c.featureBullets,keywords:c.keywords};
  const directory=path.join(base,locale);fs.mkdirSync(directory,{recursive:true});
  fs.writeFileSync(path.join(directory,'listing.json'),JSON.stringify(listing,null,2)+'\n');
  manifest.profiles.push({locale,amazonLocale:c.amazonLocale,amazonLabel:c.amazonLabel,amazonDescriptionSupported:c.amazonLocale!==null,amazonMediaSupported:mediaLocales.has(locale),listing:`marketing/localized/${locale}/listing.json`});
  for(const [variant,[w,h]]of Object.entries(sizes)){
    const out=path.join(directory,variant);fs.mkdirSync(out,{recursive:true});
    for(let i=0;i<8;i++){
      const source=path.join(root,`artifacts/screenshots/${variant}/${copy.sourceScreens[i]}.png`);
      const original=fs.readFileSync(source);
      if(original.subarray(0,8).toString('hex')!=='89504e470d0a1a0a')throw new Error('Capture is not PNG');
      const sw=original.readUInt32BE(16),sh=original.readUInt32BE(20);
      const margin=variant==='phone'?70:100,width=w-2*margin;
      const parts=[],layout=[];let y=68;
      function paragraph(text,size,bold=false,color='#173f32',at=null){
        if(at!==null)y=at;
        for(const line of lines(text,size,family,bold,c.direction,width)){
          const b=bounds(line,size,family,bold,c.direction);
          const left=c.direction==='rtl'?w-margin-b.width:margin;
          if(left<margin-1 || left+b.width>w-margin+1 || y+b.height>h-18)throw new Error(`Text outside safe area: ${locale}/${variant}/${i+1}`);
          parts.push(`<g fill="${color}" transform="translate(${(left-b.x).toFixed(3)} ${(y-b.y).toFixed(3)})">${textNode(line,size,family,bold,c.direction)}</g>`);
          layout.push({text:line,x:left,y,width:b.width,height:b.height});
          y+=Math.max(size*1.33,b.height+12);
        }
      }
      paragraph(c.screens[i][0],variant==='phone'?59:65,true);
      y+=12;paragraph(c.screens[i][1],variant==='phone'?32:34);
      y+=12;paragraph(c.disclosure,variant==='phone'?25:26,false,'#52664c');
      y+=28;
      const footerSize=variant==='phone'?20:22;
      const footerLines=lines(c.footer,footerSize,family,false,c.direction,width).length;
      const footerTop=h-36-footerLines*footerSize*1.33;
      const scale=Math.min(width/sw,(footerTop-y-30)/sh);
      if(scale<=0)throw new Error('No capture area');
      const iw=sw*scale,ih=sh*scale,x=(w-iw)/2;
      parts.push(`<image x="${x}" y="${y}" width="${iw}" height="${ih}" href="data:image/png;base64,${original.toString('base64')}"/>`);
      const capture={x,y,width:iw,height:ih};
      paragraph(c.footer,footerSize,false,'#52664c',footerTop);
      const svg=`<svg xmlns="http://www.w3.org/2000/svg" width="${w}" height="${h}" viewBox="0 0 ${w} ${h}" xml:lang="${locale}"><title>${esc(c.screens[i][0])}</title><desc>${esc(c.disclosure)}. ${esc(c.footer)}</desc><rect width="100%" height="100%" fill="#fff9ee"/>${parts.join('')}</svg>`;
      const stem=String(i+1).padStart(2,'0');
      fs.writeFileSync(path.join(out,stem+'.svg'),svg);
      const png=new Resvg(svg,{...options,languages:[locale]}).render().asPng();
      fs.writeFileSync(path.join(out,stem+'.png'),png);
      manifest.assets.push({locale,variant,id:i+1,width:w,height:h,png:`marketing/localized/${locale}/${variant}/${stem}.png`,editable:`marketing/localized/${locale}/${variant}/${stem}.svg`,sha256:hash(png),source:path.relative(root,source).replaceAll('\\','/'),sourceSha256:hash(original),capture,captionBounds:layout,englishUiPreserved:true});
    }
  }
  console.log(locale+': 16 PNGs, 16 editable SVGs and listing JSON');
}
fs.writeFileSync(path.join(base,'manifest.json'),JSON.stringify(manifest,null,2)+'\n');
console.log('Complete: '+manifest.assets.length+' measured screenshot exports.');
