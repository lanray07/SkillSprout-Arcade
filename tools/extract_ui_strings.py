"""Move fixed native UI literals to Android resources. Dynamic text is reported separately."""
import hashlib,re
from pathlib import Path
from xml.etree import ElementTree as ET
ROOT=Path(__file__).resolve().parents[1]
resources=ET.Element('resources')
for file in (ROOT/'app/src/main/java/com/skillsprout/arcade').glob('*.kt'):
    source=file.read_text('utf-8')
    def replace(m):
        text=m.group(2)
        if not text.strip() or '\\' in text:return m.group(0)
        key='ui_'+hashlib.sha256(text.encode()).hexdigest()[:10]
        if not any(e.attrib.get('name')==key for e in resources):ET.SubElement(resources,'string',name=key).text=text.replace("'","\\'")
        return m.group(1)+'(stringResource(R.string.'+key+')'
    updated=re.sub(r'\b(Text|Heading|Action|SoftButton)\("([^"$\n]*)"',replace,source)
    if updated!=source:
        if 'import androidx.compose.ui.res.stringResource' not in updated:updated=updated.replace('package com.skillsprout.arcade','package com.skillsprout.arcade\nimport androidx.compose.ui.res.stringResource',1)
        file.write_text(updated,'utf-8')
destination=ROOT/'app/src/main/res/values/ui.xml'
if destination.exists():
    old=ET.parse(destination).getroot()
    for e in old:
        if not any(x.attrib.get('name')==e.attrib.get('name') for x in resources):resources.append(e)
ET.indent(resources);ET.ElementTree(resources).write(destination,encoding='utf-8',xml_declaration=True)
print('Resource strings:',len(resources))
