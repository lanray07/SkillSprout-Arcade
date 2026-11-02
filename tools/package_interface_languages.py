"""Package validated machine/assistant UI translations only after an explicit publisher waiver.

No human language approval or educational-content approval is created. The older
localise.py export command continues to require its normal human review record.
"""
import argparse
import hashlib
import json
import re
from collections import Counter
from pathlib import Path
from xml.etree import ElementTree as ET
import localise
from extract_ui_strings import android_escape

def catalogue(folder):
    result={}
    for path in sorted(folder.glob('*.xml')):
        for element in ET.parse(path).getroot():
            if element.tag=='string':result[element.attrib['name']]=localise.android_text(''.join(element.itertext()))
            elif element.tag=='plurals':
                for item in element:result[element.attrib['name']+'['+item.attrib['quantity']+']']=localise.android_text(''.join(item.itertext()))
    return result

def quality_errors(source,target):
    errors=[]
    for key,value in target.items():
        original=source.get(key,source.get(key.split('[')[0]+'[other]',''))
        if len(original)>15 and len(value)>max(120,4*len(original)):errors.append('excessive-length:'+key)
        if any(match.group(1).isalpha() for match in re.finditer(r'(.)\1{8,}',value)):errors.append('repeated-character:'+key)
        words=value.split();grams=Counter(tuple(words[i:i+4]) for i in range(len(words)-3))
        if len(words)>20 and grams and max(grams.values())>=4:errors.append('repeated-phrase:'+key)
    return errors

def corrected(source,target,locale,editorial,plurals):
    for document in [editorial,plurals]:
        for key,expected in document['sourceTexts'].items():
            if source.get(key)!=expected:raise ValueError('Stale editorial source: '+key)
        for key,value in document['locales'].get(locale,{}).items():target[key]=value
    # Reuse corrected navigation words where the UI adds only a decorative icon.
    words={source[key]:value for key,value in editorial['locales'].get(locale,{}).items() if key in source}
    for key,original in source.items():
        for english,translated in words.items():
            if original==english:target[key]=translated;break
            if original.endswith(english) and original[:-len(english)] and all(not c.isalnum() for c in original[:-len(english)]):
                target[key]=original[:-len(english)]+translated;break
            if original.startswith(english) and original[len(english):] and all(not c.isalnum() for c in original[len(english):]):
                target[key]=translated+original[len(english):];break
    if locale=='ar':target={key:value.translate(str.maketrans({'←':'→','→':'←'})) for key,value in target.items()}
    if locale in ['fr','es','it','pt-BR','pt-PT']:
        for key in list(target):
            if key.endswith('[other]'):target.setdefault(key[:-7]+'[many]',target[key])
    errors=localise.validate_catalog(source,target)
    errors.extend(quality_errors(source,target))
    for name in {key.split('[')[0] for key in source if '[' in key}:
        for quantity in localise.REQUIRED_PLURALS.get(locale,{'other'}):
            if name+'['+quantity+']' not in target:errors.append('plural:'+name+'['+quantity+']')
    if errors:raise ValueError(locale+': '+', '.join(errors))
    return target

def resource_bytes(target):
    root=ET.Element('resources');plurals={}
    for key,value in target.items():
        if '[' in key:
            name,quantity=key[:-1].split('[')
            if name not in plurals:plurals[name]=ET.SubElement(root,'plurals',name=name)
            item=ET.SubElement(plurals[name],'item',quantity=quantity)
        else:
            item=ET.SubElement(root,'string',name=key)
            if '%' in value and not localise.placeholders(value):item.set('formatted','false')
        item.text=android_escape(value)
    ET.indent(root)
    return ET.tostring(root,encoding='utf-8',xml_declaration=True)+b'\n'

def package(root,waived):
    if not waived:raise ValueError('Publisher language-review waiver must be explicit; no output written')
    directory=root/'localisation';source=catalogue(root/'app/src/main/res/values')
    recorded=json.loads((directory/'source.en-GB.json').read_text(encoding='utf-8'))
    if source!=recorded:raise ValueError('Refresh the UI source catalogue before packaging')
    editorial=json.loads((directory/'editorial-ui-overrides.json').read_text(encoding='utf-8'))
    plurals=json.loads((directory/'plural-ui-overrides.json').read_text(encoding='utf-8'))
    pending={};rows=[]
    for locale in localise.LOCALES[1:]:
        target=json.loads((directory/(locale+'.pending.json')).read_text(encoding='utf-8'))
        metadata=json.loads((directory/(locale+'.machine.json')).read_text(encoding='utf-8'))
        if metadata['sourceSha256']!=localise.digest(source) or metadata['targetSha256']!=localise.digest(target):
            raise ValueError('Stale machine provenance: '+locale)
        target=corrected(source,target,locale,editorial,plurals)
        path=Path('app/src/main/res')/localise.resource_folder(locale)/'strings.xml'
        data=resource_bytes(target);pending[path]=data
        rows.append({'locale':locale,'path':path.as_posix(),'sha256':hashlib.sha256(data).hexdigest(),
            'keys':len(target),'missingKeys':[],'targetSha256':localise.digest(target),'humanLanguageApproval':False})
    # Validate the complete set before changing any packaged resource.
    for path,data in pending.items():
        file=root/path;file.parent.mkdir(parents=True,exist_ok=True)
        temporary=file.with_suffix('.xml.tmp');temporary.write_bytes(data);temporary.replace(file)
    path=root/'app/src/main/res/values/languages.xml';language_root=ET.parse(path).getroot()
    choices=language_root.find("string-array[@name='interface_locales']")
    if choices is None:raise ValueError('Missing interface locale array')
    choices.clear();choices.set('name','interface_locales');choices.set('translatable','false')
    for tag in localise.LOCALES:ET.SubElement(choices,'item').text=tag
    ET.indent(language_root);path.write_bytes(ET.tostring(language_root,encoding='utf-8',xml_declaration=True)+b'\n')
    result={'sourceSha256':localise.digest(source),'sourceKeys':len(source),'defaultLocale':'en-GB',
        'publisherWaivedIndependentLanguageReview':True,'humanLanguageApproval':False,'productionApproval':False,
        'educationalPacksTranslated':False,'locales':rows,
        'editorialInputs':{p:hashlib.sha256((directory/p).read_bytes()).hexdigest() for p in ['editorial-ui-overrides.json','plural-ui-overrides.json']},
        'limitations':['Machine translation with assistant corrections; no native-speaker certification.','Learning activities and narration remain English.','Full bundled privacy policy is English, with a translated summary.','Compilation, rendering and runtime checks are separate from catalogue completeness.']}
    (directory/'packaged-interface-locales.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8',newline='\n')
    return result

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--publisher-waived-language-review',action='store_true')
    args=parser.parse_args();result=package(localise.ROOT,args.publisher_waived_language_review)
    print('Packaged '+str(len(result['locales'])+1)+' interface locale variants; no human approval created.')
