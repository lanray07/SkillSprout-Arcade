"""Independent numerical checks for the editorial pass, not teacher approvals."""
import json
import re
import unittest
from fractions import Fraction
from pathlib import Path
import batch_content_review

ROOT = Path(__file__).resolve().parents[1]


class BatchReviewTests(unittest.TestCase):
    def setUp(self):
        self.items = json.loads((ROOT/'app/src/main/assets/packs/starter.json').read_text('utf-8'))['activities']

    def test_generated_arithmetic_and_groups_have_correct_answers(self):
        checked = 0
        for a in self.items:
            prompt = a['prompt']
            if a['game'] == 'TABLES':
                _, groups, each = a['visual'].split(':')
                expected = int(groups) if a['skill']=='division-facts' else int(groups)*int(each)
            elif a['game'] == 'MATHS' and (match := re.fullmatch(r'(\d+) ([+−÷]) (\d+) = \?', prompt)):
                left, op, right = match.groups(); left, right = int(left), int(right)
                expected = {'+':lambda:left+right, '−':lambda:left-right, '÷':lambda:Fraction(left,right)}[op]()
            elif a['game'] == 'MATHS' and (match := re.fullmatch(r'Half of (\d+) = \?', prompt)):
                expected = Fraction(int(match[1]),2)
            elif a['game'] == 'MATHS' and (match := re.fullmatch(r'(\d+) metres = \? centimetres', prompt)):
                expected = int(match[1])*100
            elif a['game'] == 'MATHS' and (match := re.fullmatch(r'What comes after (\d+)\?', prompt)):
                expected = int(match[1])+1
            else:
                continue
            self.assertEqual(Fraction(a['answer']),expected,a['id']);checked+=1
        self.assertEqual(216,checked)

    def test_every_number_sequence_has_positive_constant_steps(self):
        sequences = [a for a in self.items if a['game']=='LOGIC']
        self.assertEqual(72,len(sequences))
        for a in sequences:
            numbers = [int(n) for n in a['answer'].split()]
            self.assertEqual(numbers,sorted(numbers),a['id'])
            self.assertGreater(numbers[1]-numbers[0],0,a['id'])
            self.assertEqual(1,len({y-x for x,y in zip(numbers,numbers[1:])}),a['id'])

    def test_supplementary_maths_answers_and_units(self):
        expected = {
            '0408':str(6*10), '0409':str(Fraction(1,2)),
            '0410':str(4*10), '0411':'11:15', '0412':f'{2*(6+3)} cm', '0413':str(4*6-5),
            '0414':str(float(Fraction(3,10))), '0415':f'{7*4} cm²', '0416':'obtuse',
            '0417':f'{-2+5}°C', '0418':str(Fraction(2,7)+Fraction(3,7)), '0419':str(27-18),
            '0420':str(float(Fraction(345,100)+Fraction(18,10))), '0421':str(80//4),
            '0422':str(Fraction(1,2)+Fraction(1,4)), '0423':f'{3*2*4} cm³',
            '0424':f'{360-140-90}°', '0425':f'{(36749+500)//1000*1000:,}',
            '0426':str(20//(2+3)*3), '0427':str((19-4)//3), '0428':str(Fraction(2,3)*Fraction(3,4)),
            '0429':f'£{60-60*15//100}', '0430':str(sum([4,7,9,12])//4), '0431':str(abs(4-(-6))),
        }
        items={a['id'].split('-')[-1]:a for a in self.items if a['game']=='MATHS' and a['year']>=3}
        for suffix,answer in expected.items():
            actual=items[suffix]['answer']
            if suffix=='0409':self.assertEqual(Fraction(actual),Fraction(1,2))
            else:self.assertEqual(answer,actual,suffix)

    def test_batch_scope_and_support_exclude_earlier_human_decisions(self):
        scoped=[a for a in self.items if batch_content_review.in_scope(a)]
        self.assertEqual(310,len(scoped))
        self.assertEqual(14,sum(a['year']==2 for a in scoped))
        self.assertTrue(all(a['version']==3 and a['reviewStatus']=='Awaiting Teacher Review' for a in scoped))
        for a in scoped:
            self.assertNotIn('4 + 2 makes 6',a['hints'][1],a['id'])
            self.assertNotIn('For a different example',a['hints'][1],a['id'])
            self.assertNotIn('Work through the quantities',a['hints'][1],a['id'])
        table=next(a for a in scoped if a['id']=='dev-maths-y4-0419')
        self.assertEqual('bus-trips:18:27',table['visual'])


if __name__=='__main__':unittest.main()
