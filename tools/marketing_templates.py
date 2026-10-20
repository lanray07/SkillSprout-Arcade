"""Create editable screenshot compositions from actual JVM Android UI renders.

These are preview assets, not evidence of physical device compatibility.
"""
from pathlib import Path
import base64,html,json,textwrap
from PIL import Image,ImageDraw,ImageFont
ROOT=Path(__file__).resolve().parents[1]
spec=json.loads((ROOT/'marketing/screenshots.json').read_text('utf-8'))
font_dir=Path('C:/Windows/Fonts')
def font(size,bold=False):
    path=font_dir/('trebucbd.ttf' if bold else 'trebuc.ttf')
    return ImageFont.truetype(str(path),size) if path.exists() else ImageFont.load_default(size=size)
created=[]
for variant,size in [('tablet',spec['tabletSize']),('phone',spec['phoneSize'])]:
    out=ROOT/'marketing/screenshots'/variant;out.mkdir(parents=True,exist_ok=True)
    for screen in spec['screens']:
        source=ROOT/f'artifacts/screenshots/{variant}/{screen["source"]}.png'
        if not source.exists():continue
        w,h=size;phone=variant=='phone';margin=70 if phone else 100
        title=screen['title'];caption=screen['caption'] if screen['id']!=8 else 'Play installed learning activities without internet.'
        title_lines=textwrap.wrap(title,width=26 if phone else 60)
        caption_lines=textwrap.wrap(caption,width=40 if phone else 90)
        top=80;title_size=60 if phone else 66;caption_size=34 if phone else 34
        y=top;svg=[];canvas=Image.new('RGB',(w,h),'#fff9ee');draw=ImageDraw.Draw(canvas)
        for line in title_lines:
            draw.text((margin,y),line,fill='#173f32',font=font(title_size,True));svg.append(f'<text x="{margin}" y="{y+title_size}" font-size="{title_size}" font-weight="bold">{html.escape(line)}</text>');y+=title_size+10
        y+=12
        for line in caption_lines:
            draw.text((margin,y),line,fill='#173f32',font=font(caption_size));svg.append(f'<text x="{margin}" y="{y+caption_size}" font-size="{caption_size}">{html.escape(line)}</text>');y+=caption_size+10
        y+=35
        screenshot=Image.open(source).convert('RGB');screenshot.thumbnail((w-2*margin,h-y-95),Image.Resampling.LANCZOS)
        x=(w-screenshot.width)//2;canvas.paste(screenshot,(x,y))
        embedded=base64.b64encode(source.read_bytes()).decode()
        svg.append(f'<image x="{x}" y="{y}" width="{screenshot.width}" height="{screenshot.height}" href="data:image/png;base64,{embedded}"/>')
        notice='Development preview · JVM Android render · Physical Fire testing pending'
        draw.text((margin,h-55),notice,fill='#52664c',font=font(19 if phone else 23))
        svg.append(f'<text x="{margin}" y="{h-30}" font-size="{19 if phone else 23}">{notice}</text>')
        stem=f'{screen["id"]:02}-{screen["source"]}'
        canvas.save(out/f'{stem}.png')
        (out/f'{stem}.svg').write_text(f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" viewBox="0 0 {w} {h}"><rect width="100%" height="100%" fill="#fff9ee"/><g font-family="Trebuchet MS, sans-serif" fill="#173f32">'+''.join(svg)+'</g></svg>','utf-8')
        created.append({'id':screen['id'],'variant':variant,'source':str(source.relative_to(ROOT)),'preview':str((out/f'{stem}.png').relative_to(ROOT)),'editable':str((out/f'{stem}.svg').relative_to(ROOT)),'width':w,'height':h,'status':'JVM preview — not physical device capture'})
(ROOT/'marketing/asset-manifest.json').write_text(json.dumps(created,indent=2))
print('Created',len(created),'editable compositions and matching PNG previews.')
