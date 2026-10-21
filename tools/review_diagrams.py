"""Vector previews for the three reviewed Android learning-diagram formats."""
import html,math,re

def svg(body,label,view='0 0 200 200'):
    return f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="{view}" width="200" height="200" role="img" aria-label="{html.escape(label,quote=True)}">{body}</svg>'

def markup(activity):
    visual=activity.get('visual','')
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
