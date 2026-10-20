"""Development-time localisation; never ships credentials or translates educational packs.

Commands: extract, report, translate --locale fr, export --locale fr.
Google Cloud Translation is only called by the explicit translate command.
All machine output stays pending until a human review record is supplied.
"""
import argparse, hashlib, json, os, re, urllib.request
from pathlib import Path
from xml.etree import ElementTree as ET
ROOT=Path(__file__).resolve().parents[1]
LOCALES=['en-GB','en-US','fr','es','de','it','pt-BR','pt-PT','nl','pl','ar','hi']
GLOSSARY=['SkillSprout Arcade','Sproutland','Pip','Maths Dash','Spelling Sprint','Story Detectives','Times Table Heroes','Logic Lab','Word Builder']
DIR=ROOT/'localisation';DIR.mkdir(exist_ok=True)
def load(path,default):return json.loads(path.read_text('utf-8')) if path.exists() else default
def placeholders(text):return sorted(re.findall(r'%(?:\d+\$)?[dsf]|\{[a-zA-Z_][\w]*\}',text))
def extract():
    catalog={}
    for path in (ROOT/'app/src/main/res/values').glob('*.xml'):
        for element in ET.parse(path).getroot():
            if element.tag=='string':catalog[element.attrib['name']]=''.join(element.itertext())
            elif element.tag=='plurals':
                for item in element:catalog[f"{element.attrib['name']}[{item.attrib['quantity']}]"]=''.join(item.itertext())
    (DIR/'source.en-GB.json').write_text(json.dumps(catalog,ensure_ascii=False,indent=2),'utf-8')
    # Unwired literal detection is intentionally visible; it is a release blocker.
    remaining=[]
    for path in (ROOT/'app/src/main/java').rglob('*.kt'):
        for line_no,line in enumerate(path.read_text('utf-8').splitlines(),1):
            if re.search(r'(?:Text|Heading|Action|SoftButton)\("',line):remaining.append(f'{path.relative_to(ROOT)}:{line_no}')
    report={'sourceStrings':len(catalog),'remainingLiteralLocations':remaining,'locales':LOCALES,'educationalTranslation':'disabled; separate pack review required'}
    (DIR/'extraction-report.json').write_text(json.dumps(report,indent=2),'utf-8');return catalog
def translate(locale):
    source=extract();target=load(DIR/f'{locale}.pending.json',{});cache=load(DIR/'translation-cache.json',{})
    token=os.environ.get('GOOGLE_TRANSLATION_ACCESS_TOKEN')
    if not token:raise SystemExit('Set GOOGLE_TRANSLATION_ACCESS_TOKEN locally. No credentials are stored in the APK or repository.')
    for key,text in source.items():
        cache_key=hashlib.sha256((locale+'|v1|'+text).encode()).hexdigest()
        if cache_key not in cache:
            protected=text
            tokens={}
            for i,word in enumerate(GLOSSARY+placeholders(text)):
                if word in protected:
                    marker=f'SPROUTKEEP{i}TOKEN';tokens[marker]=word;protected=protected.replace(word,marker)
            req=urllib.request.Request('https://translation.googleapis.com/language/translate/v2',data=json.dumps({'q':protected,'source':'en','target':locale.split('-')[0],'format':'text'}).encode(),headers={'Authorization':'Bearer '+token,'Content-Type':'application/json'})
            with urllib.request.urlopen(req,timeout=30) as response:translated=json.load(response)['data']['translations'][0]['translatedText']
            import html
            translated=html.unescape(translated)
            for marker,word in tokens.items():
                if marker not in translated:raise ValueError('Glossary or placeholder token was modified by the provider')
                translated=translated.replace(marker,word)
            if placeholders(text)!=placeholders(translated):raise ValueError(f'Placeholder mismatch: {key}')
            cache[cache_key]=translated
            (DIR/'translation-cache.json').write_text(json.dumps(cache,ensure_ascii=False,indent=2),'utf-8')
        target[key]=cache[cache_key]
    (DIR/f'{locale}.pending.json').write_text(json.dumps(target,ensure_ascii=False,indent=2),'utf-8')
def report():
    source=extract();result={}
    for locale in LOCALES:
        if locale=='en-GB':continue
        target=load(DIR/f'{locale}.pending.json',{})
        result[locale]={'missing':[key for key in source if key not in target],'invalidPlaceholders':[key for key in source if key in target and placeholders(source[key])!=placeholders(target[key])],'reviewStatus':'Awaiting human language review'}
    (DIR/'missing-translations.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),'utf-8')
def export(locale):
    source=extract();target=load(DIR/f'{locale}.pending.json',{});review=load(DIR/f'{locale}.review.json',{})
    digest=hashlib.sha256(json.dumps(target,sort_keys=True,ensure_ascii=False).encode()).hexdigest()
    if not review.get('reviewer') or review.get('sha256')!=digest or review.get('status')!='Approved':raise SystemExit('A human review with reviewer, Approved status and exact translation digest is required.')
    if source.keys()!=target.keys() or any(placeholders(v)!=placeholders(target[k]) for k,v in source.items()):raise SystemExit('Missing strings or placeholder mismatch')
    folder=ROOT/'app/src/main/res'/('values-b+'+locale.replace('-','+'));folder.mkdir(exist_ok=True)
    resources=ET.Element('resources');plurals={}
    for key,text in target.items():
        if '[' in key:
            name,quantity=key[:-1].split('[')
            if name not in plurals:plurals[name]=ET.SubElement(resources,'plurals',name=name)
            el=ET.SubElement(plurals[name],'item',quantity=quantity)
        else:el=ET.SubElement(resources,'string',name=key)
        el.text=text.replace("'","\\'")
    ET.indent(resources);ET.ElementTree(resources).write(folder/'strings.xml',encoding='utf-8',xml_declaration=True)
if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('command',choices=['extract','report','translate','export']);parser.add_argument('--locale',choices=LOCALES)
    args=parser.parse_args()
    if args.command in ['translate','export'] and not args.locale:parser.error('--locale is required')
    {'extract':extract,'report':report,'translate':lambda:translate(args.locale),'export':lambda:export(args.locale)}[args.command]()
