"""Vector previews for reviewed Android learning-diagram formats."""
import html,math,re

def svg(body,label,view='0 0 200 200'):
    return f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="{view}" width="200" height="200" role="img" aria-label="{html.escape(label,quote=True)}">{body}</svg>'

def markup(activity):
    visual=activity.get('visual','')
    trips=re.fullmatch(r'bus-trips:([0-9]{1,3}):([0-9]{1,3})',visual)
    if trips:
        monday,tuesday=map(int,trips.groups())
        return f'<table style="border-collapse:collapse;min-width:240px"><caption>Bus trips</caption><thead><tr><th scope="col">Day</th><th scope="col">Bus trips</th></tr></thead><tbody><tr><th scope="row">Monday</th><td>{monday}</td></tr><tr><th scope="row">Tuesday</th><td>{tuesday}</td></tr></tbody></table>'
    fruit=re.fullmatch(r'fruit:([0-9]{1,2}):([0-9]{1,2})',visual)
    if fruit:
        apples,pears=map(int,fruit.groups())
        if not all(1<=n<=10 for n in (apples,pears)):return ''
        drawings={
            'Apples':'<path d="M16 10 C5 3 0 17 9 28 C12 31 14 28 16 28 C19 28 21 31 24 27 C32 16 27 3 16 10 Z" fill="#cc4939"/>',
            'Pears':'<path d="M16 7 C10 7 13 14 7 19 C-1 30 31 33 25 20 C19 14 22 7 16 7 Z" fill="#b9d56c"/>'}
        rows=[]
        for label,count in [('Apples',apples),('Pears',pears)]:
            symbols=''.join('<svg data-fruit="'+label.lower()+'" viewBox="0 0 32 32" width="30" height="30" aria-hidden="true"><g stroke="#193f32" stroke-width="1.3">'+drawings[label]+'<path d="M16 9 L18 2" fill="none" stroke-width="2"/><ellipse cx="22.5" cy="4.5" rx="3.5" ry="1.5" fill="#193f32"/></g></svg>' for _ in range(count))
            rows.append(f'<div style="margin:12px 0"><strong>{label}</strong><div style="display:flex;flex-wrap:wrap;gap:6px">{symbols}</div></div>')
        description=f'Fruit pictogram. Apples: {apples} symbols. Pears: {pears} symbols. Each symbol represents one piece of fruit.'
        return '<div role="img" aria-label="'+description+'">'+''.join(rows)+'<p>Key: each symbol represents one piece of fruit.</p></div>'
    if visual=='choice-shapes':
        drawings={'Triangle':'<path d="M32 10 L54 54 L10 54 Z"/>','Circle':'<circle cx="32" cy="32" r="22"/>','Square':'<rect x="10" y="10" width="44" height="44"/>'}
        choices=[]
        for label in activity['choices']:
            if label not in drawings:return ''
            choices.append('<figure style="margin:12px;display:inline-block;text-align:center"><svg viewBox="0 0 64 64" width="80" height="80" aria-hidden="true"><g fill="none" stroke="#193f32" stroke-width="3">'+drawings[label]+'</g></svg><figcaption>'+html.escape(label)+'</figcaption></figure>')
        return '<div>'+''.join(choices)+'</div>'
    if visual=='sandwich:equal-halves':
        return svg('<g fill="#ffe6ad" stroke="#80501f" stroke-width="4" stroke-linejoin="round"><path d="M25 25 L165 25 L25 165 Z"/><path d="M173 33 L173 173 L33 173 Z"/></g>','A square sandwich cut diagonally into two equal pieces.')
    match=re.fullmatch(r'clock:([0-9]{1,2}):([0-9]{2})',visual)
    if match:
        hour,minute=map(int,match.groups())
        if not (1<=hour<=12 and 0<=minute<=59):return ''
        body='<circle cx="100" cy="100" r="90" fill="white" stroke="#193f32" stroke-width="3"/>'
        for number in range(1,13):
            angle=number*math.pi/6-math.pi/2;x=100+math.cos(angle)*72;y=100+math.sin(angle)*72
            body+=f'<text x="{x:.2f}" y="{y:.2f}" dy=".35em" text-anchor="middle" font-family="sans-serif" font-size="18" fill="#193f32">{number}</text>'
        for angle,length,width in [((hour%12+minute/60)*math.pi/6-math.pi/2,43.2,7),(minute*math.pi/30-math.pi/2,60.3,4)]:
            body+=f'<line x1="100" y1="100" x2="{100+math.cos(angle)*length:.2f}" y2="{100+math.sin(angle)*length:.2f}" stroke="#193f32" stroke-width="{width}" stroke-linecap="round"/>'
        body+='<circle cx="100" cy="100" r="5" fill="#193f32"/>'
        return svg(body,f'Analogue clock showing {hour}:{minute:02d}.')
    return ''
