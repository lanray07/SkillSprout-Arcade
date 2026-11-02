"""Verify the interface languages actually packaged in Android resources. Read-only."""
import hashlib
import json
from pathlib import Path
from xml.etree import ElementTree as ET
import localise
from package_interface_languages import catalogue,quality_errors

def check(root):
    directory=root/'localisation'
    manifest=json.loads((directory/'packaged-interface-locales.json').read_text(encoding='utf-8'))
    source=catalogue(root/'app/src/main/res/values')
    if source!=json.loads((directory/'source.en-GB.json').read_text(encoding='utf-8')):raise ValueError('UI source catalogue is stale')
    if manifest['sourceSha256']!=localise.digest(source):raise ValueError('Packaged UI source hash is stale')
    rows=manifest['locales'];expected=localise.LOCALES[1:]
    if len(rows)!=len(expected) or {row['locale'] for row in rows}!=set(expected):raise ValueError('Packaged locale set differs from the requested set')
    array=ET.parse(root/'app/src/main/res/values/languages.xml').getroot().find("string-array[@name='interface_locales']")
    if array is None or [item.text for item in array]!=localise.LOCALES:raise ValueError('Language selector differs from packaged locales')
    for row in rows:
        locale=row['locale'];path=Path('app/src/main/res')/localise.resource_folder(locale)/'strings.xml'
        if row['path']!=path.as_posix():raise ValueError('Unexpected locale resource path')
        file=root/path
        if hashlib.sha256(file.read_bytes()).hexdigest()!=row['sha256']:raise ValueError('Locale resource bytes changed: '+locale)
        target=catalogue(file.parent)
        errors=localise.validate_catalog(source,target)+quality_errors(source,target)
        for name in {key.split('[')[0] for key in source if '[' in key}:
            for quantity in localise.REQUIRED_PLURALS.get(locale,{'other'}):
                if name+'['+quantity+']' not in target:errors.append('plural:'+name+'['+quantity+']')
        if errors:raise ValueError(locale+': '+', '.join(errors))
        if localise.digest(target)!=row['targetSha256']:raise ValueError('Locale text hash differs from packaged manifest: '+locale)
    return {'interfaceLocaleVariants':len(rows)+1,'sourceKeys':len(source),'resourceHashesVerified':len(rows),
        'missingKeys':0,'placeholderErrors':0,'pathologicalOutputs':0,'humanLanguageApproval':manifest['humanLanguageApproval'],
        'independentReviewWaived':manifest['publisherWaivedIndependentLanguageReview'],
        'educationalPacksTranslated':False,'productionApproval':False}

if __name__=='__main__':print(json.dumps(check(localise.ROOT),indent=2))
