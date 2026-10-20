"""Development-time localisation; never ships credentials or translates educational packs.

Commands: extract, report, translate --locale fr, export --locale fr.
Google Cloud Translation is only called by the explicit translate command.
All machine output stays pending until a human review record is supplied.
"""
import argparse, hashlib, json, os, re, urllib.request
from datetime import date
from pathlib import Path
from xml.etree import ElementTree as ET
from extract_ui_strings import android_escape
ROOT=Path(__file__).resolve().parents[1]
LOCALES=['en-GB','en-US','fr','es','de','it','pt-BR','pt-PT','nl','pl','ar','hi']
GLOSSARY=['SkillSprout Arcade','Sproutland','Pip','Maths Dash','Spelling Sprint','Story Detectives','Times Table Heroes','Logic Lab','Word Builder','Number Valley','Wordwood Forest','Story Harbour','Puzzle Peaks']
DIR=ROOT/'localisation';DIR.mkdir(exist_ok=True)
def load(path,default):return json.loads(path.read_text('utf-8')) if path.exists() else default
FORMAT = re.compile(r"%%|%(?:[1-9][0-9]*\$)?[-#+ 0,(<]*[0-9]*(?:\.[0-9]+)?(?:[tT][a-zA-Z]|[bBhHsScCdoxXeEfgGaAn])|\{[a-zA-Z_][\w]*\}")
def placeholders(text):return sorted(m.group() for m in FORMAT.finditer(text))
def digest(catalog):return hashlib.sha256(json.dumps(catalog,sort_keys=True,ensure_ascii=False).encode()).hexdigest()
REQUIRED_PLURALS={'ar':{'zero','one','two','few','many','other'},'pl':{'one','few','many','other'}}
def review_errors(source,target,review,locale):
    errors=validate_catalog(source,target)
    if review.get('status')!='Approved' or review.get('locale')!=locale:errors.append('review-status-or-locale')
    if not all(isinstance(review.get(k),str) and review[k].strip() for k in ['reviewer','reviewedAt','evidence']):errors.append('review-evidence')
    try:
        if date.fromisoformat(review.get('reviewedAt',''))>date.today():errors.append('future-review')
    except (ValueError,TypeError):errors.append('review-date')
    if review.get('sha256')!=digest(target) or review.get('sourceSha256')!=digest(source):errors.append('stale-review')
    names={key.split('[')[0] for key in source if '[' in key}
    for name in names:
        for quantity in REQUIRED_PLURALS.get(locale,{'other'}):
            if f'{name}[{quantity}]' not in target:errors.append(f'plural:{name}[{quantity}]')
    return errors

def android_text(text):
    return re.sub(r'''\\([\\'"ntr])''',lambda m:{'n':'\n','t':'\t','r':'\r'}.get(m.group(1),m.group(1)),text)

def validate_catalog(source,target):
    errors=['missing:'+key for key in source if key not in target]
    for key,text in target.items():
        reference=source.get(key)
        if reference is None:
            match=re.fullmatch(r'(.+)\[(zero|one|two|few|many|other)\]',key)
            reference=source.get(match[1]+'[other]') if match else None
        if reference is None:errors.append('unexpected:'+key)
        elif not isinstance(text,str) or (reference.strip() and not text.strip()) or placeholders(reference)!=placeholders(text):errors.append('placeholders:'+key)
    return errors
def extract():
    catalog={}
    for path in (ROOT/'app/src/main/res/values').glob('*.xml'):
        for element in ET.parse(path).getroot():
            if element.tag=='string':catalog[element.attrib['name']]=android_text(''.join(element.itertext()))
            elif element.tag=='plurals':
                for item in element:catalog[f"{element.attrib['name']}[{item.attrib['quantity']}]"]=android_text(''.join(item.itertext()))
    (DIR/'source.en-GB.json').write_text(json.dumps(catalog,ensure_ascii=False,indent=2),'utf-8')
    # Unwired literal detection is intentionally visible; it is a release blocker.
    remaining=[]
    for path in (ROOT/'app/src/main/java').rglob('*.kt'):
        for line_no,line in enumerate(path.read_text('utf-8').splitlines(),1):
            if re.search(r'(?:Text|Heading|Action|SoftButton|Choice|Toggle)\(\s*(?:"|if\b)|(?:message|feedback|error)\s*=\s*(?:"|if\b)',line):remaining.append(f'{path.relative_to(ROOT).as_posix()}:{line_no}')
    report={'sourceStrings':len(catalog),'remainingLiteralLocations':remaining,'manualReviewRequired':['conditional labels','enum and district titles','constructed selectors','accessibility descriptions','callback errors'],'locales':LOCALES,'educationalTranslation':'disabled; separate pack review required'}
    (DIR/'extraction-report.json').write_text(json.dumps(report,indent=2),'utf-8');return catalog
def translate(locale):
    source=extract();target=load(DIR/f'{locale}.pending.json',{});cache=load(DIR/'translation-cache.json',{})
    if locale=='en-US':
        # Same-language baseline, deliberately pending human regional review; not an API translation.
        (DIR/'en-US.pending.json').write_text(json.dumps(source,ensure_ascii=False,indent=2),'utf-8')
        return
    token=os.environ.get('GOOGLE_TRANSLATION_ACCESS_TOKEN')
    if not token:raise SystemExit('Set GOOGLE_TRANSLATION_ACCESS_TOKEN locally. No credentials are stored in the APK or repository.')
    for key,text in source.items():
        cache_key=hashlib.sha256((locale+'|v2|'+text).encode()).hexdigest()
        if cache_key not in cache:
            protected=text
            tokens={}
            for i,word in enumerate(sorted(set(GLOSSARY+placeholders(text)),key=len,reverse=True)):
                if word in protected:
                    marker=f'SPROUTKEEP{i}TOKEN';tokens[marker]=word;protected=protected.replace(word,marker)
            req=urllib.request.Request('https://translation.googleapis.com/language/translate/v2',data=json.dumps({'q':protected,'source':'en','target':locale,'format':'text'}).encode(),headers={'Authorization':'Bearer '+token,'Content-Type':'application/json'})
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
    errors=review_errors(source,target,review,locale)
    if errors:raise SystemExit('Language export blocked: '+', '.join(errors))
    folder=ROOT/'app/src/main/res'/('values-b+'+locale.replace('-','+'));folder.mkdir(exist_ok=True)
    resources=ET.Element('resources');plurals={}
    for key,text in target.items():
        if '[' in key:
            name,quantity=key[:-1].split('[')
            if name not in plurals:plurals[name]=ET.SubElement(resources,'plurals',name=name)
            el=ET.SubElement(plurals[name],'item',quantity=quantity)
        else:el=ET.SubElement(resources,'string',name=key)
        el.text=android_escape(text)
    ET.indent(resources);ET.ElementTree(resources).write(folder/'strings.xml',encoding='utf-8',xml_declaration=True)
if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('command',choices=['extract','report','translate','export']);parser.add_argument('--locale',choices=LOCALES)
    args=parser.parse_args()
    if args.command in ['translate','export'] and not args.locale:parser.error('--locale is required')
    {'extract':extract,'report':report,'translate':lambda:translate(args.locale),'export':lambda:export(args.locale)}[args.command]()
