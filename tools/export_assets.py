"""Export production source assets and editable marketing templates; never fabricate app screens."""
import json,hashlib
from pathlib import Path
from PIL import Image
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'assets/exports';OUT.mkdir(parents=True,exist_ok=True)
pip=Image.open(ROOT/'assets/originals/pip.png').convert('RGBA')
world=Image.open(ROOT/'assets/originals/sproutland.png').convert('RGB')
for size in [1024,512,114]:
    icon=Image.new('RGBA',(size,size),(217,234,189,255));sprite=pip.copy();sprite.thumbnail((int(size*.92),int(size*.92)),Image.Resampling.LANCZOS);icon.alpha_composite(sprite,((size-sprite.width)//2,(size-sprite.height)//2));icon.save(OUT/f'icon-{size}.png')
pip.copy().save(OUT/'pip-master.png')
for i,name in enumerate(['number-valley','wordwood-forest','story-harbour','puzzle-peaks']):
    # Editable environment studies extracted from the original world source; not new scenes.
    world.crop((world.width*i//4,world.height//5,world.width*(i+1)//4,world.height)).save(OUT/f'{name}-study.png')
symbols={
 'maths':'<path d="M20 32h24M32 20v24M62 25h18M62 38h18"/>',
 'spelling':'<path d="M18 68 34 25 50 68M24 52h20M61 68V28h12q20 0 0 18H61m12 0q23 0 0 22H61"/>',
 'story':'<path d="M14 24q20-8 34 5 14-13 34-5v46q-20-8-34 5-14-13-34-5ZM48 29v46M23 37h15M58 37h15M23 48h15M58 48h15"/>',
 'tables':'<path d="M28 18h40v24q0 23-20 23T28 42ZM28 24H15v12q0 16 18 16M68 24h13v12q0 16-18 16M48 65v14M34 80h28"/>',
 'logic':'<path d="m48 15 30 31-30 33-30-33ZM31 46h34M48 30v33"/>',
 'words':'<path d="M15 27h22v41H15ZM37 20h22v48H37ZM59 30h22v38H59M22 43l5 12 5-12M44 35h8M48 35v21M66 43h9M66 52h9"/>'}
colors=['#d9eabd','#ffd786','#bee4f1','#f9c6b6','#e0cef9','#ffe5a8']
for (name,body),color in zip(symbols.items(),colors):
    (OUT/f'game-{name}.svg').write_text(f'<svg xmlns="http://www.w3.org/2000/svg" width="96" height="96" viewBox="0 0 96 96"><rect x="2" y="2" width="92" height="92" rx="22" fill="{color}"/><g fill="none" stroke="#173f32" stroke-width="5" stroke-linecap="round" stroke-linejoin="round">{body}</g></svg>')
    import re
    paths=''.join(f'<path android:pathData="{p}" android:fillColor="#00000000" android:strokeColor="#173f32" android:strokeWidth="5" android:strokeLineCap="round" android:strokeLineJoin="round"/>' for p in re.findall('d="([^"]+)"',body))
    (ROOT/f'app/src/main/res/drawable/game_{name}.xml').write_text(f'<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="56dp" android:height="56dp" android:viewportWidth="96" android:viewportHeight="96">{paths}</vector>')
for n,name in [(1,'first-discovery'),(5,'curious-explorer'),(15,'woodland-helper'),(30,'sproutland-adventurer')]:
    (OUT/f'badge-{name}.svg').write_text(f'<svg xmlns="http://www.w3.org/2000/svg" width="160" height="180" viewBox="0 0 160 180"><path d="m45 105-8 64 43-20 43 20-8-64" fill="#427c39"/><circle cx="80" cy="72" r="62" fill="#f4c567" stroke="#173f32" stroke-width="4"/><path d="m80 29 12 28 30 3-23 20 7 30-26-16-26 16 7-30-23-20 30-3Z" fill="#fff9ee"/><text x="80" y="80" text-anchor="middle" fill="#173f32" font-size="24" font-family="sans-serif" font-weight="bold">{n}</text></svg>')
manifest=[]
for path in sorted((ROOT/'assets').rglob('*')):
    if path.is_file() and path.name!='manifest.json':manifest.append({'path':str(path.relative_to(ROOT)).replace('\\','/'),'bytes':path.stat().st_size,'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'provenance':'Built-in imagegen original' if path.parent.name in ['originals','concepts'] else 'Original project source or derived asset'})
(ROOT/'assets/manifest.json').write_text(json.dumps(manifest,indent=2))
print('Exported',len(list(OUT.iterdir())),'assets; manifest entries',len(manifest))
