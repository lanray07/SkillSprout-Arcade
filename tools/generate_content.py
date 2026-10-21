"""Deterministic development content. Never assigns teacher approval."""
import json, hashlib
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'app/src/main/assets/packs'
MATH = 'https://www.gov.uk/government/publications/national-curriculum-in-england-mathematics-programmes-of-study/national-curriculum-in-england-mathematics-programmes-of-study'
ENGLISH = 'https://www.gov.uk/government/publications/national-curriculum-in-england-english-programmes-of-study/national-curriculum-in-england-english-programmes-of-study'
activities=[]
def add(game,y,skill,d,prompt,answer,choices,explanation,hints,objective,kind='choice',story='',visual=''):
    activities.append(dict(id=f'dev-{game.lower()}-y{y}-{len(activities):04}',game=game,subject='Mathematics' if game in ['MATHS','TABLES','LOGIC'] else 'English',skill=skill,year=y,objective=objective,reference=(MATH if game in ['MATHS','TABLES','LOGIC'] else ENGLISH)+f'#year-{y}',difficulty=d,prerequisites=[],type=kind,prompt=prompt,answer=str(answer),choices=list(map(str,choices)),explanation=explanation,hints=hints,story=story,visual=visual,reviewStatus='Awaiting Teacher Review',version=2))
words={1:['the','said','ship','rain','shop','feet','tree','with','chair'],2:['because','people','children','door','pretty','every','plant','would','again'],3:['disappear','believe','bicycle','teacher','careful','unhappy','rebuild','garden','answer'],4:['medicine','exercise','important','natural','possible','different','famous','usually','enough'],5:['ancient','curiosity','excellent','physical','language','conscience','equipment','rhythm','variety'],6:['accommodate','conscientious','environment','exaggerate','pronunciation','privilege','sufficient','necessary','opportunity']}
clues={'the':'A word that comes before a noun: ___ moon.','said':'Told someone something: Pip ___ hello.','ship':'A large boat.','rain':'Water falling from clouds.','shop':'A place where things are sold.','feet':'The plural of foot.','tree':'A tall plant with a trunk.','with':'Together: come ___ me.','chair':'A seat with a back.'}
for y in range(1,7):
    for i in range(18):
        d=i%3+1; a=(i+1)*(1 if y<3 else y); b=i%7+1
        op=i%6 if y>=3 else i%3
        if op==0: p=f'{a} + {b} = ?'; ans=a+b; exp=f'Start at {a} and count on {b}. You reach {ans}.'; skill='addition'; obj='Add numbers using mental methods appropriate to the selected year.'
        elif op==1: p=f'{a+b} − {b} = ?'; ans=a; exp=f'Count back {b} from {a+b}. You reach {ans}.'; skill='subtraction'; obj='Subtract numbers using mental methods appropriate to the selected year.'
        elif op==2: p=f'What comes after {a}?'; ans=a+1; exp=f'On the number line, one step after {a} is {ans}.'; skill='counting'; obj='Count forwards and recognise place value.'
        elif op==3: p=f'Half of {a*2} = ?'; ans=a; exp=f'Split {a*2} into two equal groups. Each group has {a}.'; skill='fractions'; obj='Recognise and calculate simple fractions of quantities.'
        elif op==4: p=f'{a} metres = ? centimetres'; ans=a*100; exp=f'One metre is 100 centimetres. {a} groups of 100 make {ans}.'; skill='measurement'; obj='Convert between common units of length.'
        else: p=f'{a*b} ÷ {b} = ?'; ans=a; exp=f'Make {b} equal groups from {a*b}. There are {a} in each group.'; skill='division'; obj='Use multiplication and division facts to solve problems.'
        options=[ans,ans+1,ans+3,max(0,ans-1)]; options=list(dict.fromkeys(options))
        while len(options)<4: options.append(max(options)+1)
        options=options[i%4:]+options[:i%4]
        add('MATHS',y,skill,d,p,ans,options,exp,['Try using counters or a number line.','Work through a smaller example first: 4 + 2 makes 6.'],obj,visual=f'number:{min(a,20)}')
    tables=([2,5,10] if y<3 else [3,4,8] if y==3 else [6,7,9,11,12])
    for i in range(18):
        t=tables[i%len(tables)]; n=i%9+2; division=i%3==2; ans=n if division else t*n
        add('TABLES',y,'division-facts' if division else 'multiplication-facts',i%3+1,f'{t*n} ÷ {t} = ?' if division else f'{n} groups of {t}. How many altogether?',ans,[ans+1,ans,ans+2,ans+4],f'{n} groups with {t} in each make {t*n}. Count in steps of {t}.',['Tap Add a group to build equal groups.','For a different example, 3 groups of 2 make 6.'],'Recall and use multiplication and related division facts.',visual=f'array:{n}:{t}')
    for i,w in enumerate(words[y]):
        add('SPELLING',y,'spelling',i%3+1,clues.get(w,f'Build this word: {w}'),w,list(w),f'The spelling is {w}. Read the whole word, then say it in a sentence.',['Say the word slowly and listen for its sounds.','Try saying the word in chunks.'], 'Apply spelling patterns and common exception or statutory word spellings.','letters')
    for i in range(12):
        step=(i%3+1)*(1 if y<=2 else y); start=i+1; seq=[start+j*step for j in range(4)]
        add('LOGIC',y,'number-patterns',i%3+1,'Put the stepping stones in order, smallest first.', ' '.join(map(str,seq)),list(map(str,seq)),f'The pattern adds {step} each time. The order is '+', '.join(map(str,seq))+'.',['Find the smallest number first.','Look at how much changes between two stones.'],'Describe number patterns. Reasoning enrichment; mapping needs teacher confirmation.','sequence')
    story=("Pip found a tiny boat beside the harbour. The sail had a hole. Pip asked Ada for a scrap of cloth. Together they mended the sail. When the wind returned, the boat carried seeds across the bay. Pip kept one seed to plant at home." if y<3 else "A storm had scattered the harbour's seed packets. Pip wanted to plant them immediately, but Ada noticed that their labels were missing. They compared the seeds with drawings in an old gardening book. The broad, striped seeds belonged in sunny soil; the smaller round seeds needed shade. Pip made new labels before planting. A month later, the harbour garden was full of shoots. Pip was glad they had taken time to investigate.")
    questions=([('What did Pip find?','A tiny boat',['A tiny boat','A red kite','A gold key'],'The first sentence tells us Pip found a boat.','retrieval'),('Why did Pip need cloth?','To mend the sail',['To mend the sail','To feed the birds','To paint a house'],'The sail had a hole, so cloth helped repair it.','inference'),('What happened last?','Pip kept a seed',['Pip found a boat','They mended the sail','Pip kept a seed'],'Pip kept a seed after the boat carried the others away.','sequencing')] if y<3 else [('Why did Ada suggest investigating?','The labels were missing',['The labels were missing','There were no seeds','The book was wet'],'Without labels they did not know where each kind of seed would grow.','inference'),('What does investigate mean here?','Find out carefully',['Run quickly','Find out carefully','Make a loud sound'],'They compared seeds and drawings to find information.','vocabulary'),('How did Pip feel about waiting?','Glad they took time',['Glad they took time','Sorry they planted','Angry with Ada'],'The last sentence says Pip was glad they had investigated.','character')])
    for p,ans,options,exp,skill in questions:
        add('STORY',y,'reading-'+skill,1,p,ans,options,exp,['Read the story again and look for a clue.','Think about what the characters did and why.'],'Read and discuss stories; retrieve information and make inferences.',story=story)
    sentences=[('Pip ___ a seed.','plants',['plants','cloud','quietly']),('The boat sailed ___ the bay.','across',['across','underneath','sleep']),('Ada is ___ because she shares.','kind',['kind','cold','triangle']),('The opposite of dark is ___.','light',['light','night','deep']),('A word meaning very small is ___.','tiny',['tiny','huge','rough']),('They repaired the sail. Repaired means ___.','mended',['mended','lost','folded'])]
    for i,(p,ans,options) in enumerate(sentences):
        add('WORDS',y,'vocabulary',i%3+1,p,ans,options,f'“{ans}” fits the meaning of this sentence. Read it again with the word in place.',['Read the sentence aloud.','Try each word and think about its meaning.'],'Discuss word meanings and apply vocabulary in context.','word')
from content_expansion import extend
extend(add, activities)
OUT.mkdir(parents=True,exist_ok=True)
pack=dict(schemaVersion=1,id='starter-en-gb',locale='en-GB',version=3,development=True,activities=activities)
data=(json.dumps(pack,ensure_ascii=False,indent=2)+'\n').encode()
(OUT/'starter.json').write_bytes(data)
(OUT/'manifest.json').write_text(json.dumps(dict(id=pack['id'],version=pack['version'],locale='en-GB',sha256=hashlib.sha256(data).hexdigest(),count=len(activities),reviewStatus='Awaiting Teacher Review'),indent=2))
print(f'Generated {len(activities)} development activities. No teacher approvals.')
