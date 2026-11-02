"""Create a pending Traditional Chinese UI catalogue with OpenCC; no approvals."""
import json
from pathlib import Path
import localise

def main():
    from opencc import OpenCC
    source=json.loads((localise.DIR/'source.en-GB.json').read_text(encoding='utf-8'))
    simplified=json.loads((localise.DIR/'zh-Hans.pending.json').read_text(encoding='utf-8'))
    errors=localise.validate_catalog(source,simplified)
    if errors:raise ValueError('Incomplete Simplified Chinese catalogue: '+', '.join(errors))
    converter=OpenCC('s2t')
    target={key:converter.convert(value) for key,value in simplified.items()}
    errors=localise.validate_catalog(source,target)
    if errors:raise ValueError('Invalid converted catalogue: '+', '.join(errors))
    (localise.DIR/'zh-Hant.pending.json').write_text(json.dumps(target,ensure_ascii=False,indent=2)+'\n',encoding='utf-8',newline='\n')
    provenance={'locale':'zh-Hant','status':'Machine translation and script conversion; no human language approval',
        'sourceSha256':localise.digest(source),'targetSha256':localise.digest(target),
        'simplifiedCatalogueSha256':localise.digest(simplified),'engine':'OpenCC s2t (opencc-python-reimplemented 0.1.7)',
        'source':'https://github.com/BYVoid/OpenCC','licence':'Apache-2.0; development tool only',
        'educationalPacksTranslated':False,'limitation':'Generic Traditional Chinese; not a Taiwan/Hong Kong terminology certification.'}
    (localise.DIR/'zh-Hant.machine.json').write_text(json.dumps(provenance,ensure_ascii=False,indent=2)+'\n',encoding='utf-8',newline='\n')
    print('zh-Hant: '+str(len(target))+' converted UI entries; placeholders valid; no approval recorded.')

if __name__=='__main__':main()
