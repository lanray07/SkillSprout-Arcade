"""Publisher-authorised editorial pass; this never supplies teacher approval.

Only the remaining Year 2 items and Years 3–6 are in scope. Previously
individually reviewed items are left untouched. Stable IDs and answers stay put.
"""
import re


def in_scope(a):
    return a['year'] >= 3 or a['id'] in {
        *(f'dev-tables-y2-{n:04}' for n in range(94, 102)),
        *(f'dev-words-y2-{n:04}' for n in range(126, 132)),
    }


SPELLING = {
    'disappear': (3, 'Start with dis. Add appear, remembering the two p letters in appear.'),
    'believe': (2, 'Start with be, then add lieve. Put i before e in this word.'),
    'bicycle': (3, 'Start with bi, then add cycle. Remember the y after the first c.'),
    'teacher': (2, 'Start with teach and add er.'),
    'careful': (2, 'Start with care and add ful. The ending has one l.'),
    'unhappy': (2, 'Start with un and add happy. Remember the two p letters.'),
    'rebuild': (2, 'Start with re and add build. Remember the u after b.'),
    'garden': (2, 'Start with gard, then add e and n.'),
    'answer': (2, 'Start with ans, then add w, e and r. Remember the w.'),
    'medicine': (3, 'Start with med, then add icine. Remember the c between the two i letters.'),
    'exercise': (3, 'Start with exer, then add cise.'),
    'important': (3, 'Start with import, then add a, n and t.'),
    'natural': (2, 'Start with natur, then add a and l.'),
    'possible': (2, 'Start with poss, then add ible. Remember the two s letters.'),
    'different': (3, 'Start with diff, then add erent. Remember the two f letters.'),
    'famous': (2, 'Start with fam, then add o, u and s.'),
    'usually': (3, 'Start with usual and add ly. There are two l letters where the parts join.'),
    'enough': (2, 'Start with e and n, then add o, u, g and h.'),
    'ancient': (2, 'Start with an, then add cient.'),
    'curiosity': (3, 'Start with curi, then add osity.'),
    'excellent': (3, 'Start with ex, then add cellent. Remember the two l letters.'),
    'physical': (3, 'Start with phys, then add ical. The first two letters are p and h.'),
    'language': (2, 'Start with lang, then add uage.'),
    'conscience': (3, 'Start with con, then add science.'),
    'equipment': (2, 'Start with equip and add ment.'),
    'rhythm': (3, 'Start with r and h, then add y, t, h and m.'),
    'variety': (2, 'Start with vari, then add e, t and y.'),
    'accommodate': (3, 'Start with accom, then add modate. Remember two c letters and two m letters.'),
    'conscientious': (3, 'Start with con, then add scientious. Check the s, c, i sequence after con.'),
    'environment': (3, 'Start with environ and add ment. Remember the n before ment.'),
    'exaggerate': (3, 'Start with ex, then add aggerate. Remember the two g letters.'),
    'pronunciation': (3, 'Start with pronun, then add ciation.'),
    'privilege': (3, 'Start with privi, then add lege. Check the e letters after l and g.'),
    'sufficient': (3, 'Start with suff, then add icient. Remember the two f letters.'),
    'necessary': (3, 'Start with nece, then add ssary. There is one c and two s letters.'),
    'opportunity': (3, 'Start with oppor, then add tunity. Remember the two p letters.'),
}

VOCABULARY_HINTS = {
    'plants': 'Which word tells what Pip does with the seed? Try it in the sentence.',
    'across': 'The boat travels on the water from one side of the bay to the other. Which word fits?',
    'kind': 'Ada shares with others. Which word describes someone who is caring and helpful?',
    'light': 'Imagine turning on a lamp in a dark room. Which word describes the room now?',
    'tiny': 'Imagine a little seed beside a big tree. Which word describes something very small?',
    'mended': 'The sail had a hole. They fixed it so it could be used again. Which word means fixed?',
}

READING_HINTS = {
    'Why did Ada suggest investigating?': 'Find what Ada noticed about the labels. Why would that matter when choosing where to plant?',
    'What does investigate mean here?': 'They compared seeds with drawings in a book. What were they trying to find out?',
    'How did Pip feel about waiting?': 'Read the final sentence. How did Pip feel about taking time to investigate?',
    'What stopped the bell?': 'Find the sentence about what Ada discovered beside the bell.',
    'Which evidence shows the problem was solved?': 'Read the final sentence. What could people hear after Ada removed the twig?',
    'Why did Ada draw a map?': 'Look at what Ada marked on the map. How could it help people choose a route?',
    'Which description best fits Ada?': 'Think about the information Ada collected and how she used it at the meeting.',
    'Why did the committee ask for another trial?': 'Read the final sentence. Which weather condition still needed to be tested?',
    'Which detail supports using a shield?': 'Look at the trial results for both walkers and fishers. Which detail shows that both groups benefited?',
    'Why did Ada compare dry weeks?': 'What else had changed between the two summers? Think about how rain could affect watering.',
    'Which summary is most balanced?': 'Choose a statement that includes both the smaller saving and the need to keep measuring.',
}

# Every supplementary question was inspected, including its correct answer and
# distractors. These hints support the question's own quantities and units.
SUPPLEMENTARY = {
    'dev-maths-y3-0408': 'In 362, the columns are hundreds, tens and ones. Multiply the tens digit by ten.',
    'dev-maths-y3-0409': 'Split a whole into four equal parts. How many of those parts cover one half?',
    'dev-maths-y3-0410': 'Each centimetre has ten millimetres. Count four groups of ten.',
    'dev-maths-y3-0411': 'Count fifteen minutes from 10:45 to the next hour, then add the remaining fifteen minutes.',
    'dev-maths-y3-0412': 'A rectangle has two lengths and two widths. Add 6 + 3 + 6 + 3 and use centimetres.',
    'dev-maths-y3-0413': 'Multiply four by six to find all the seeds. Then take away the five planted seeds.',
    'dev-maths-y4-0414': 'The first place after the decimal point is tenths. Put three in that place.',
    'dev-maths-y4-0415': 'Multiply seven by four. Use square centimetres because you are measuring area.',
    'dev-maths-y4-0416': 'Compare 120 degrees with 90 degrees and 180 degrees. Which angle name fits between them?',
    'dev-maths-y4-0417': 'Count up two degrees to reach zero, then add the remaining three degrees.',
    'dev-maths-y4-0418': 'Both fractions use sevenths. Add the numerators and keep the denominator seven.',
    'dev-maths-y4-0419': 'Count on from 18 to 20, then to 27. Add the two jumps to find how many more.',
    'dev-maths-y5-0420': 'Write 1.8 as 1.80. Line up ones, tenths and hundredths before adding.',
    'dev-maths-y5-0421': 'Twenty-five percent means one quarter. Share eighty into four equal groups.',
    'dev-maths-y5-0422': 'Rewrite one half as two quarters, then add one more quarter.',
    'dev-maths-y5-0423': 'Find the number of unit cubes in a 3 by 2 layer. Then multiply by four layers.',
    'dev-maths-y5-0424': 'Add 140 degrees and 90 degrees, then subtract their total from 360 degrees.',
    'dev-maths-y5-0425': 'Find the two neighbouring thousands. Use the hundreds digit to decide which is nearer.',
    'dev-maths-y6-0426': 'Add two and three to find the number of equal parts. Divide twenty by that total, then take three parts.',
    'dev-maths-y6-0427': 'Subtract four from both sides. Then divide both sides by three.',
    'dev-maths-y6-0428': 'Multiply two by three and three by four, then simplify the resulting fraction.',
    'dev-maths-y6-0429': 'Find ten percent and five percent of sixty pounds. Add those reductions, then subtract from sixty pounds.',
    'dev-maths-y6-0430': 'Add all four numbers. Share the total equally between the four values.',
    'dev-maths-y6-0431': 'Count six steps from minus six to zero, then four more steps to four. Add the distances.',
}


def reviewed_fields(a):
    """Return editorial changes, or fail on an unexamined activity shape."""
    game = a['game']
    fields = {}
    if game == 'LOGIC':
        numbers = [int(n) for n in a['answer'].split()]
        step = numbers[1] - numbers[0]
        assert all(y - x == step for x, y in zip(numbers, numbers[1:]))
        fields['hints'] = [a['hints'][0], f'Start with the smallest number, then add {step} each time. Choose the next number.']
    elif game == 'TABLES':
        _, groups, each = a['visual'].split(':')
        groups, each = int(groups), int(each)
        if a['skill'] == 'division-facts':
            fields['prompt'] = f'How many groups of {each} make {groups * each}?'
            second = f'Build groups of {each} until you have {groups * each} dots altogether. Count how many groups you made.'
            fields['difficulty'] = 2 if a['year'] == 2 or each <= 5 or groups <= 5 else 3
        else:
            second = f'Build {groups} groups with {each} dots in each. Count in steps of {each}, saying one number for each group.'
            fields['difficulty'] = a['difficulty'] if a['year'] == 2 else (2 if each <= 5 or groups <= 5 else 3)
        # Match the already agreed wording and values for the pending Year 2 batch.
        if a['year'] == 2:
            words = {2:'two', 3:'three', 5:'five', 6:'six', 8:'eight', 9:'nine'}
            if a['skill'] == 'division-facts':
                totals = {40:'forty', 70:'seventy', 100:'one hundred'}
                second = f'Build groups of ten until you have {totals[groups * each]} dots altogether. Count how many groups you made.'
            else:
                second = f'Build {words[groups]} groups with {words[each]} dots in each. Count in {"twos" if each == 2 else "fives"}, saying one number for each group.'
        fields['hints'] = [a['hints'][0], second]
    elif game == 'WORDS':
        fields['hints'] = [a['hints'][0], VOCABULARY_HINTS[a['answer']]]
        if a['answer'] == 'kind': fields['difficulty'] = 1
    elif game == 'SPELLING':
        difficulty, second = SPELLING[a['answer']]
        fields.update(hints=[a['hints'][0], second], difficulty=difficulty)
        if a['answer'] == 'excellent': fields['prompt'] = 'A word meaning extremely good.'
    elif game == 'STORY':
        fields['hints'] = [a['hints'][0], READING_HINTS[a['prompt']]]
        if a['prompt'] in ['Why did Ada suggest investigating?', 'What does investigate mean here?']:
            fields['difficulty'] = 2
    elif game == 'MATHS':
        skill, prompt = a['skill'], a['prompt']
        hints = a['hints'].copy()
        if a['id'] in SUPPLEMENTARY:
            hints[1] = SUPPLEMENTARY[a['id']]
            if a['id'] == 'dev-maths-y4-0419': fields['visual'] = 'bus-trips:18:27'
        elif skill == 'addition':
            left, right = map(int, re.fullmatch(r'(\d+) \+ (\d+) = \?', prompt).groups())
            hints = [f'Start at {left}.', f'Count on {right} steps, one step at a time.']
            fields['difficulty'] = 1 if left + right <= 10 or right == 1 else 2
        elif skill == 'subtraction':
            total, taken = map(int, re.fullmatch(r'(\d+) − (\d+) = \?', prompt).groups())
            hints[1] = f'Start at {total} and count back {taken} steps, one step at a time.'
            fields['difficulty'] = 1 if taken <= 2 else 2
        elif skill == 'counting':
            start = int(re.fullmatch(r'What comes after (\d+)\?', prompt)[1])
            hints[1] = f'Start counting at {start}. Which number comes next?'
            fields['difficulty'] = 1
        elif skill == 'fractions':
            total = int(re.fullmatch(r'Half of (\d+) = \?', prompt)[1])
            first = total // 20 * 20
            hints[0] = 'Half means one of two equal groups.'
            hints[1] = (f'Split {total} into {first} and {total - first}. Halve each part, then add the results.'
                        if total > 40 and total != first else f'Share {total} equally between two groups. How many are in one group?')
            fields['difficulty'] = 2 if total > 40 else 1
        elif skill == 'measurement':
            metres = int(re.fullmatch(r'(\d+) metres = \? centimetres', prompt)[1])
            hints = ['One metre is one hundred centimetres.', f'Multiply {metres} by 100 to convert metres to centimetres.']
        elif skill == 'division':
            total, divisor = map(int, re.fullmatch(r'(\d+) ÷ (\d+) = \?', prompt).groups())
            part = (total // divisor // 10) * 10 * divisor
            hints = ['Use multiplication facts to help you divide.',
                     f'Split {total} into {part} and {total - part}. Divide each part by {divisor}, then add the results.'
                     if total != part else f'Find how many groups of {divisor} make {total}. You can use a known multiple of ten.']
        else:
            raise ValueError('Unexamined maths activity: ' + a['id'])
        fields['hints'] = hints
    else:
        raise ValueError('Unexamined activity: ' + a['id'])
    return fields


def apply(activities):
    for a in activities:
        if in_scope(a):
            a.update(reviewed_fields(a))
            a['version'] = 3
