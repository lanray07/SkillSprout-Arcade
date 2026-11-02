"""Generate pending interface catalogues locally with an Argos CTranslate2 model.

Install translation-requirements.txt in a separate virtual environment. Download
models from the official Argos package index; never bundle them in the APK.
This translates UI resources only, not learning packs, and creates no approvals.
"""
import argparse
import hashlib
import json
from pathlib import Path
import re
import unicodedata
import zipfile
import localise

def protected_parts(text):
    # One pass avoids corrupting a placeholder while preserving another token.
    glossary='|'.join(re.escape(s) for s in sorted(localise.GLOSSARY,key=len,reverse=True))
    symbols='|'.join(re.escape(c) for c in sorted(set(text)) if unicodedata.category(c).startswith('S'))
    identifiers=r'https?://[^\s<>]+|[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}'
    pattern=re.compile('(?:'+identifiers+')|(?:'+localise.FORMAT.pattern+')|(?:'+glossary+')'+('|(?:'+symbols+')' if symbols else ''))
    result=[]
    # Long mixed paragraphs can lose clauses in the small offline models. Keep
    # sentence/paragraph boundaries, and never split an ordinary term like PIN.
    for sentence in re.split(r'(\n|(?<=[.!?])[ \t]+)',text):
        start=0
        for match in pattern.finditer(sentence):
            if match.start()>start:result.append((sentence[start:match.start()],False))
            result.append((match.group(),True));start=match.end()
        if start<len(sentence):result.append((sentence[start:],False))
    return result

def translate_catalog(source,translate_many):
    parts={key:protected_parts(value) for key,value in source.items()}
    pending=list(dict.fromkeys(part.strip() for row in parts.values() for part,protected in row
                               if not protected and any(c.isalpha() for c in part)))
    translated=translate_many(pending)
    if len(translated)!=len(pending):raise ValueError('Incomplete model output count')
    missing=[pending[i] for i,s in enumerate(translated) if not isinstance(s,str) or not s.strip()]
    if missing:raise ValueError('Blank model output for UI fragments: '+repr(missing))
    cache=dict(zip(pending,translated));target={}
    for key,row in parts.items():
        values=[]
        for part,protected in row:
            if protected or part.strip() not in cache:values.append(part);continue
            left=part[:len(part)-len(part.lstrip())];right=part[len(part.rstrip()):]
            values.append(left+cache[part.strip()].strip()+right)
        target[key]=''.join(values)
    errors=localise.validate_catalog(source,target)
    if errors:raise ValueError('Invalid translated catalogue: '+', '.join(errors))
    return target

def unpack(model,cache):
    cache=cache.resolve();cache.mkdir(parents=True,exist_ok=True)
    with zipfile.ZipFile(model) as archive:
        metadata=[n for n in archive.namelist() if n.endswith('/metadata.json')]
        if len(metadata)!=1:raise ValueError('Expected one model metadata file')
        for name in archive.namelist():
            if not (cache/name).resolve().is_relative_to(cache):raise ValueError('Unsafe model archive path')
        archive.extractall(cache)
    return cache/Path(metadata[0]).parent

def model_translator(folder):
    import ctranslate2
    import sentencepiece
    metadata=json.loads((folder/'metadata.json').read_text(encoding='utf-8'))
    if metadata.get('from_code')!='en':raise ValueError('UI source must be English')
    if (folder/'sentencepiece.model').exists():
        processor=sentencepiece.SentencePieceProcessor(model_file=str(folder/'sentencepiece.model'))
        encode=lambda text:processor.encode(text,out_type=str)
        decode=lambda tokens:processor.decode(tokens).replace('\u2581',' ').strip()
    else:
        from sacremoses import MosesTokenizer,MosesDetokenizer
        from subword_nmt.apply_bpe import BPE
        tokenizer=MosesTokenizer(lang='en');detokenizer=MosesDetokenizer(lang=metadata['to_code'])
        with (folder/'bpe.model').open(encoding='utf-8') as codes:bpe=BPE(codes)
        encode=lambda text:bpe.process_line(tokenizer.tokenize(text,return_str=True)).split()
        decode=lambda tokens:detokenizer.detokenize(' '.join(tokens).replace('@@ ','').split())
    engine=ctranslate2.Translator(str(folder/'model'),device='cpu',compute_type='int8',intra_threads=2)
    prefix=metadata.get('target_prefix','')
    def translate(texts):
        encoded=[encode(text) for text in texts]
        results=engine.translate_batch(encoded,beam_size=4,replace_unknowns=True,
            max_batch_size=1024,batch_type='tokens',length_penalty=0.2,max_decoding_length=512,
            target_prefix=[[prefix]]*len(texts) if prefix else None)
        output=[]
        for result in results:
            value=decode(result.hypotheses[0])
            if prefix and value.startswith(prefix):value=value[len(prefix):].lstrip()
            output.append(value)
        return output
    return translate,metadata

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--locale',required=True,choices=localise.LOCALES[1:])
    parser.add_argument('--model',type=Path,required=True)
    parser.add_argument('--cache',type=Path,default=localise.ROOT/'artifacts/verification/translation-models/unpacked')
    parser.add_argument('--missing-only',action='store_true',help='Extend a matching generated catalogue; reject changed source strings')
    args=parser.parse_args()
    folder=unpack(args.model,args.cache)
    translator,metadata=model_translator(folder)
    expected={'pt-BR':'pb','pt-PT':'pt','zh-Hans':'zh','zh-Hant':'zt'}.get(args.locale,args.locale)
    if metadata['to_code']!=expected:raise ValueError('Model language does not match target locale')
    source=json.loads((localise.DIR/'source.en-GB.json').read_text(encoding='utf-8'))
    path=localise.DIR/(args.locale+'.pending.json')
    existing={}
    if args.missing_only:
        existing=json.loads(path.read_text(encoding='utf-8'))
        prior=json.loads((localise.DIR/(args.locale+'.machine.json')).read_text(encoding='utf-8'))
        if (not set(existing).issubset(source) or localise.digest(existing)!=prior['targetSha256']
            or localise.digest({k:source[k] for k in existing})!=prior['sourceSha256']):
            raise ValueError('Existing catalogue is stale or edited; regenerate or reconcile explicitly')
    target=existing|translate_catalog({k:v for k,v in source.items() if k not in existing},translator)
    errors=localise.validate_catalog(source,target)
    if errors:raise ValueError('Invalid merged catalogue: '+', '.join(errors))
    path.write_text(json.dumps(target,ensure_ascii=False,indent=2)+'\n',encoding='utf-8',newline='\n')
    provenance={'locale':args.locale,'status':'Machine translation; not human approved','sourceSha256':localise.digest(source),
        'targetSha256':localise.digest(target),'modelSha256':hashlib.sha256(args.model.read_bytes()).hexdigest(),
        'package':metadata,'engine':'CTranslate2 CPU int8','reusedKeys':len(existing),'educationalPacksTranslated':False,
        'source':'https://github.com/argosopentech/argospm-index','licenceAttribution':(folder/'README.md').read_text(encoding='utf-8')}
    (localise.DIR/(args.locale+'.machine.json')).write_text(json.dumps(provenance,ensure_ascii=False,indent=2)+'\n',encoding='utf-8',newline='\n')
    print(args.locale+': '+str(len(target))+' UI entries generated; placeholders valid; no approval recorded.',flush=True)

if __name__=='__main__':main()
