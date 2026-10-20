"""Move direct fixed/template Compose labels into resources without changing routing keys.

Only known label arguments are rewritten. Conditional branches and non-UI literals are
reported by the localisation audit for deliberate review. Re-running is idempotent.
"""
import hashlib,re
from pathlib import Path
from xml.etree import ElementTree as ET
ROOT=Path(__file__).resolve().parents[1]
CALL=re.compile(r'\b(Text|Heading|Action|SoftButton|Choice|Toggle|Metric)\(\s*(?=")')

def literal(source,start):
    pieces=[];args=[];i=start+1
    while i<len(source):
        c=source[i]
        if c=='"':
            text=''.join(pieces)
            if args:
                text=text.replace('%','%%')
                for n in range(len(args)):text=text.replace(f'\u0001{n}\u0001',f'%{n+1}$s')
            return text,args,i+1
        if c=='\\':
            i+=1
            if i>=len(source):raise ValueError('Incomplete string')
            pieces.append({'n':'\n','t':'\t','r':'\r'}.get(source[i],source[i]));i+=1;continue
        if source.startswith('${',i):
            j=i+2;depth=1;begin=j
            while depth:
                if source[j]=='"':_,_,j=literal(source,j);continue
                if source[j]=="'":
                    j+=1
                    while source[j]!="'":j+=2 if source[j]=='\\' else 1
                elif source[j]=='{':depth+=1
                elif source[j]=='}':depth-=1
                j+=1
            args.append(source[begin:j-1]);pieces.append(f'\u0001{len(args)-1}\u0001');i=j;continue
        if c=='$' and (m:=re.match(r'[A-Za-z_][A-Za-z_0-9]*',source[i+1:])):
            args.append(m.group());pieces.append(f'\u0001{len(args)-1}\u0001');i+=len(m.group())+1;continue
        pieces.append(c);i+=1
    raise ValueError('Unclosed string')

def android_escape(text):
    return text.replace('\\','\\\\').replace('\n','\\n').replace('\t','\\t').replace("'","\\'").replace('"','\\"')

def migrate(source,resources):
    edits=[]
    for match in CALL.finditer(source):
        start=match.end();text,args,end=literal(source,start)
        if not text.strip():continue
        key='ui_'+hashlib.sha256((text+'|'+str(len(args))).encode()).hexdigest()[:10]
        if key not in resources:resources[key]=(text,bool(args))
        edits.append((start,end,'stringResource(R.string.'+key+(' ,'+','.join(args) if args else '')+')'))
    for start,end,replacement in reversed(edits):source=source[:start]+replacement+source[end:]
    return source

def main():
    destination=ROOT/'app/src/main/res/values/ui.xml'
    root=ET.parse(destination).getroot() if destination.exists() else ET.Element('resources')
    resources={e.attrib['name']:(e.text or '',None) for e in root}
    for file in (ROOT/'app/src/main/java/com/skillsprout/arcade').glob('*.kt'):
        source=file.read_text('utf-8');updated=migrate(source,resources)
        if updated!=source:
            if 'import androidx.compose.ui.res.stringResource' not in updated:updated=updated.replace('package com.skillsprout.arcade','package com.skillsprout.arcade\nimport androidx.compose.ui.res.stringResource',1)
            file.write_text(updated,encoding='utf-8')
    known={e.attrib['name'] for e in root}
    for key,(text,formatted) in resources.items():
        if key not in known:
            element=ET.SubElement(root,'string',name=key)
            if not formatted and '%' in text:element.set('formatted','false')
            element.text=android_escape(text)
    ET.indent(root);ET.ElementTree(root).write(destination,encoding='utf-8',xml_declaration=True)
    print('Resource strings:',len(root))
if __name__=='__main__':main()
