"""Additional authored development exercises; every item still needs teacher review."""
from fractions import Fraction

PREREQUISITES = {
    'addition':['counting'], 'subtraction':['addition'], 'place-value':['counting'],
    'multiplication-facts':['counting'], 'division-facts':['multiplication-facts'],
    'division':['multiplication-facts'], 'fractions':['division-facts'],
    'measurement':['counting'], 'decimals':['place-value'], 'percentages':['fractions'],
    'ratio':['fractions'], 'algebra':['addition'], 'area':['multiplication-facts'],
    'volume':['area'], 'angles':['shape-properties'], 'statistics':['addition'],
    'negative-numbers':['subtraction'], 'word-problems':['addition'],
    'reading-inference':['reading-retrieval'], 'reading-character':['reading-retrieval'],
    'reading-evidence':['reading-inference'], 'reading-summary':['reading-retrieval'],
}

# Each row is year, skill, prompt, correct answer, distractors, explanation, hint.
EXERCISES = [
 (1,'place-value','What is 14 made of?','1 ten and 4 ones',['4 tens and 1 one','1 ten and 1 one'],'Ten and four more make fourteen.','Make a group of ten first.'),
 (1,'shape-properties','Which shape has 3 straight sides?','Triangle',['Circle','Square'],'A triangle has three straight sides and three corners.','Count the sides of each shape.'),
 (1,'measurement','Which is longer: a 9 cm ribbon or a 6 cm ribbon?','9 cm ribbon',['6 cm ribbon','They are equal'],'Nine centimetres is longer than six centimetres.','Compare 9 and 6 on a number line.'),
 (1,'time','The clock shows 3 o’clock. What time is one hour later?','4 o’clock',['2 o’clock','5 o’clock'],'One hour after three is four.','Count forward one hour.'),
 (1,'money','Pip has a 2p coin and a 5p coin. How much altogether?','7p',['3p','10p'],'Two pence and five pence make seven pence.','Add 2 and 5.'),
 (1,'fractions','A sandwich is cut into 2 equal pieces. Each piece is a ___.','half',['quarter','whole'],'Two equal halves make one whole.','How many equal parts make the sandwich?'),
 (2,'place-value','Which number has 4 tens and 7 ones?','47',['74','407'],'Four tens make forty; seven ones make forty-seven.','Write the tens digit before the ones digit.'),
 (2,'shape-properties','How many faces does a cube have?','6',['4','8'],'A cube has six square faces.','Think of the surfaces of a box with all edges equal.'),
 (2,'time','Half past 8 is written as ___.','8:30',['8:15','8:45'],'Half an hour is thirty minutes.','An hour has sixty minutes.'),
 (2,'money','A snack costs 35p. You pay 50p. What change do you get?','15p',['25p','85p'],'Count from 35 to 50: five to 40, then ten to 50.','Subtract the cost from the amount paid.'),
 (2,'fractions','What is one quarter of 20?','5',['4','10'],'Divide twenty into four equal groups: five in each.','A quarter is one of four equal parts.'),
 (2,'statistics','A chart shows 4 apples and 7 pears. How many pieces of fruit?','11',['3','10'],'Four plus seven is eleven.','Add both groups; do not subtract them.'),
 (3,'place-value','What is the value of the 6 in 362?','60',['6','600'],'The six is in the tens column, so it means sixty.','Label hundreds, tens and ones.'),
 (3,'fractions','Which fraction is equivalent to 1/2?','2/4',['1/4','3/4'],'Two of four equal parts cover half of a whole.','Draw two equal rectangles and split them differently.'),
 (3,'measurement','How many millimetres are in 4 cm?','40',['400','14'],'Each centimetre contains ten millimetres; four tens make forty.','Multiply the number of centimetres by ten.'),
 (3,'time','A walk starts at 10:45 and lasts 30 minutes. When does it end?','11:15',['10:75','11:45'],'Fifteen minutes reach eleven; fifteen more reach 11:15.','Cross the next hour carefully.'),
 (3,'perimeter','A rectangle is 6 cm long and 3 cm wide. What is its perimeter?','18 cm',['9 cm','18 cm²'],'Add all four sides: 6 + 3 + 6 + 3 = 18 cm.','Perimeter is the distance around the edge.'),
 (3,'word-problems','There are 4 bags with 6 seeds each. Pip plants 5 seeds. How many remain?','19',['29','24'],'Four times six is twenty-four; twenty-four minus five is nineteen.','Find the total before subtracting.'),
 (4,'decimals','Which decimal is the same as 3/10?','0.3',['0.03','3.0'],'Three tenths occupy the first place after the decimal point.','Identify the tenths column.'),
 (4,'area','A rectangle is 7 cm by 4 cm. What is its area?','28 cm²',['22 cm²','11 cm²'],'Seven rows of four unit squares cover twenty-eight square centimetres.','Area counts squares inside a shape.'),
 (4,'angles','An angle of 120° is ___.','obtuse',['acute','a right angle'],'An obtuse angle is greater than 90° and less than 180°.','Compare the angle with a right angle.'),
 (4,'negative-numbers','It is −2°C. The temperature rises by 5°C. What is it now?','3°C',['−7°C','7°C'],'Move five steps up from minus two: minus one, zero, one, two, three.','Count upwards through zero.'),
 (4,'fractions','What is 2/7 + 3/7?','5/7',['5/14','6/7'],'The parts are all sevenths. Add their counts: two plus three is five.','Keep the size of the equal parts unchanged.'),
 (4,'statistics','A table shows 18 bus trips on Monday and 27 on Tuesday. How many more on Tuesday?','9',['45','11'],'Twenty-seven minus eighteen is nine.','Find the difference between the two totals.'),
 (5,'decimals','What is 3.45 + 1.8?','5.25',['3.63','4.25'],'Write 1.8 as 1.80 and line up decimal places: 3.45 + 1.80 = 5.25.','Align ones, tenths and hundredths.'),
 (5,'percentages','What is 25% of 80?','20',['25','40'],'Twenty-five percent is one quarter; eighty divided by four is twenty.','Find a quarter of eighty.'),
 (5,'fractions','What is 1/2 + 1/4?','3/4',['2/6','2/4'],'One half equals two quarters; add another quarter to make three quarters.','Express both fractions in quarters.'),
 (5,'volume','A cuboid is 3 cm long, 2 cm wide and 4 cm high. What is its volume?','24 cm³',['9 cm³','12 cm³'],'There are six cubes in one layer and four layers: twenty-four cubic centimetres.','Multiply length, width and height.'),
 (5,'angles','Angles around a point add to 360°. Two are 140° and 90°. Find the remaining angle.','130°',['230°','40°'],'The known angles total 230°. Subtract from 360° to leave 130°.','Add the known angles before subtracting.'),
 (5,'place-value','Round 36,749 to the nearest thousand.','37,000',['36,000','36,700'],'The hundreds digit is seven, so round up to thirty-seven thousand.','Look at the digit immediately after the thousands.'),
 (6,'ratio','Red and blue beads are in the ratio 2:3. There are 20 beads. How many are blue?','12',['8','15'],'Five equal parts make twenty, so each part is four. Three blue parts make twelve.','Add the ratio parts first.'),
 (6,'algebra','If 3n + 4 = 19, what is n?','5',['7','15'],'Subtract four to get 3n = 15, then divide by three.','Undo addition before undoing multiplication.'),
 (6,'fractions','What is 2/3 × 3/4?','1/2',['5/7','2/7'],'Multiply numerators and denominators to get 6/12, which simplifies to 1/2.','Multiply the tops and multiply the bottoms.'),
 (6,'percentages','A £60 coat is reduced by 15%. What is the sale price?','£51',['£45','£69'],'Ten percent is £6 and five percent is £3. Subtract the £9 reduction from £60.','Find the reduction, then subtract it.'),
 (6,'statistics','What is the mean of 4, 7, 9 and 12?','8',['7','9'],'Their total is thirty-two. Divide by the four values to get eight.','Add all values and divide by how many there are.'),
 (6,'negative-numbers','What is the difference between −6 and 4?','10',['2','−2'],'There are six steps from minus six to zero and four more to four.','Count the distance across zero.'),
]

STORIES = [
 (1,'Ada put a red cup on the mat. A bee landed on the cup. Ada waited. The bee flew away. Then Ada picked up the cup.',
  [('Where was the cup?','On the mat',['In a bag','On a shelf'],'The first sentence says the cup was on the mat.','retrieval'),('Why did Ada wait?','To let the bee leave',['To make the cup red','To find a mat'],'She picked up the cup only after the bee flew away.','inference')]),
 (2,'Pip and Ada planned a picnic. Dark clouds covered the sun. They packed their lunch in a basket and took it to the greenhouse. Rain tapped on the glass while they ate beside the plants.',
  [('Where did they eat?','In the greenhouse',['On the beach','In a boat'],'The story says they took their lunch to the greenhouse.','retrieval'),('Why was the greenhouse useful?','It kept the rain off',['It made the lunch bigger','It had no plants'],'Rain tapped on the glass while they stayed inside.','inference')]),
 (3,'The library bell had stopped ringing. Pip thought it was broken. Ada climbed the steps and found a twig wedged beside the bell. She removed it gently. At noon, the bell rang clearly across the square.',
  [('What stopped the bell?','A trapped twig',['A missing step','The time of day'],'Ada found a twig wedged beside the bell.','retrieval'),('Which evidence shows the problem was solved?','The bell rang at noon',['Pip thought it was broken','Ada climbed the steps'],'The clear ringing after the twig was removed shows the repair worked.','evidence')]),
 (4,'A new footpath crossed the meadow. Some neighbours wanted to widen it. Ada counted the flowers beside the path and drew a map of nesting places. At the meeting, she suggested a route around the busiest patches. The neighbours agreed to try her plan for one season.',
  [('Why did Ada draw a map?','To show places needing protection',['To record the weather','To close every path'],'Her map showed nesting places so the new route could avoid them.','inference'),('Which description best fits Ada?','She uses evidence to suggest a solution',['She refuses to listen','She ignores the meadow'],'She gathered information and proposed a route using it.','character')]),
 (5,'The harbour committee proposed replacing an old lamp with a brighter one. Pip welcomed the idea until a fisher explained that its glare might hide the channel markers. Ada suggested testing a shield that directed the light downwards. During the trial, walkers could see the steps and fishers could still distinguish the markers. The committee asked for a second trial in fog before deciding.',
  [('Why did the committee ask for another trial?','The first trial did not test fog',['The shield was missing','No one could see the steps'],'The first trial worked, but conditions in fog remained untested.','inference'),('Which detail supports using a shield?','Walkers saw steps and fishers saw markers',['The lamp was old','Pip first welcomed the idea'],'The shield met both groups’ needs during the first trial.','evidence')]),
 (6,'A notice claimed that the community orchard used less water after new pipes were installed. Pip celebrated the saving. Ada asked to see the measurements: the totals were lower, but that summer had also been much wetter. They compared dry weeks from both years and found a smaller saving. Their report praised the pipes while explaining the limits of the comparison. The orchard team decided to keep measuring before expanding the system.',
  [('Why did Ada compare dry weeks?','Rainfall could also explain lower water use',['Dry weeks have no trees','The pipes were removed'],'A wetter summer could reduce watering even without better pipes.','inference'),('Which summary is most balanced?','The pipes may help, but more comparable measurements are needed',['The pipes certainly saved all the water','The measurements prove nothing can help'],'The report found a smaller saving and explained the comparison’s limits.','summary')]),
]

CLUES = {'because': 'A word meaning for the reason that.', 'people': 'More than one person.', 'children': 'Young people.', 'door': 'You open this to enter a room.', 'pretty': 'An adjective meaning attractive.', 'every': 'Each one without exception.', 'plant': 'A living thing with roots and leaves.', 'would': 'Complete this: I ___ like a drink.', 'again': 'One more time.', 'disappear': 'To stop being visible.', 'believe': 'To think something is true.', 'bicycle': 'A vehicle with two wheels and pedals.', 'teacher': 'A person who helps a class learn.', 'careful': 'Taking care to avoid mistakes.', 'unhappy': 'The opposite of happy.', 'rebuild': 'To build something again.', 'garden': 'A place where flowers and vegetables grow.', 'answer': 'A reply to a question.', 'medicine': 'Something used to treat an illness.', 'exercise': 'Activity that helps keep your body fit.', 'important': 'Something that matters a great deal is ___.', 'natural': 'Made by nature rather than people.', 'possible': 'Able to happen or be done.', 'different': 'Not the same.', 'famous': 'Known by many people.', 'usually': 'Most of the time.', 'enough': 'As much as is needed.', 'ancient': 'Very old; from long ago.', 'curiosity': 'A strong wish to learn or find things out.', 'excellent': 'An adjective meaning extremely good.', 'physical': 'Relating to the body.', 'language': 'A system of words used to communicate.', 'conscience': 'The sense that helps you judge right and wrong.', 'equipment': 'Tools needed for an activity.', 'rhythm': 'A repeated pattern of beats.', 'variety': 'A range of different kinds.', 'accommodate': 'To provide room or space for someone.', 'conscientious': 'Careful and responsible about doing work well.', 'environment': 'The surroundings in which living things exist.', 'exaggerate': 'To describe something as bigger or greater than it is.', 'pronunciation': 'The way a word is spoken.', 'privilege': 'A special right or advantage.', 'sufficient': 'Enough for what is needed.', 'necessary': 'Required; something you must have or do.', 'opportunity': 'A chance to do something.'}

YEAR1_LOGIC_HINTS = {
    'dev-logic-y1-0045': 'Which number comes next when you count?',
    'dev-logic-y1-0046': 'Count forwards in twos to find the next number.',
    'dev-logic-y1-0047': 'Choose the smallest number left each time.',
    'dev-logic-y1-0048': 'Which number comes next when you count?',
    'dev-logic-y1-0049': 'Choose the smallest number left each time.',
    'dev-logic-y1-0050': 'Choose the smallest number left each time.',
    'dev-logic-y1-0051': 'Which number comes next when you count?',
    'dev-logic-y1-0052': 'Count forwards in twos to find the next number.',
    'dev-logic-y1-0053': 'Choose the smallest number left each time.',
    'dev-logic-y1-0054': 'Which number comes next when you count?',
    'dev-logic-y1-0055': 'Choose the smallest number left each time.',
    'dev-logic-y1-0056': 'Choose the smallest number left each time.',
}

YEAR1_MATHS_CORRECTIONS = {'dev-maths-y1-0000': {'hints': ['Start at 1.', 'Count on one more. What number comes next?']},
 'dev-maths-y1-0001': {'hints': ['Start with the total and count back.',
                                 'Count back two steps from 4, one step at a time.']},
 'dev-maths-y1-0002': {'difficulty': 1,
                       'hints': ['Try using counters or a number line.',
                                 'Start counting at 3. Which number comes next?']},
 'dev-maths-y1-0003': {'hints': ['Start at 4.', 'Count on four more, one step at a time.']},
 'dev-maths-y1-0004': {'hints': ['Start with the total and count back.',
                                 'Count back five steps from 10, one step at a time.']},
 'dev-maths-y1-0005': {'difficulty': 1,
                       'hints': ['Try using counters or a number line.',
                                 'Start counting at 6. Which number comes next?']},
 'dev-maths-y1-0006': {'difficulty': 2, 'hints': ['Start at 7.', 'Count on seven more, one step at a time.']},
 'dev-maths-y1-0007': {'difficulty': 1,
                       'hints': ['Start with the total and count back.',
                                 'Start at 9 and count back one step.']},
 'dev-maths-y1-0008': {'difficulty': 1,
                       'hints': ['Try using counters or a number line.',
                                 'Start counting at 9. Which number comes next?']},
 'dev-maths-y1-0009': {'hints': ['Start with the larger number and count on.',
                                 'Start at 10 and count on three steps.']},
 'dev-maths-y1-0010': {'hints': ['Start with the total and count back.',
                                 'Count back four steps from 15, one step at a time.']},
 'dev-maths-y1-0011': {'difficulty': 1,
                       'hints': ['Try using counters or a number line.',
                                 'Start counting at 12. Which number comes next?']}}

YEAR1_REVIEW25_36 = {'dev-maths-y1-0012': {'difficulty': 2,
                       'hints': ['Start with the larger number and count on.',
                                 'Start at 13 and count on six steps, one step at a time.']},
 'dev-maths-y1-0013': {'prompt': '20 − 7 = ?',
                       'choices': ['14', '16', '12', '13'],
                       'answer': '13',
                       'explanation': 'Count back 7 from 20. You reach 13.',
                       'hints': ['Start with the total and count back.',
                                 'Count back seven steps from 20, one step at a time.'],
                       'visual': 'number:13'},
 'dev-maths-y1-0014': {'difficulty': 1,
                       'hints': ['Try using counters or a number line.',
                                 'Start counting at 15. Which number comes next?']},
 'dev-maths-y1-0015': {'hints': ['Start with the larger number and count on.',
                                 'Start at 16 and count on two steps.']},
 'dev-maths-y1-0016': {'hints': ['Start with the total and count back.',
                                 'Count back three steps from 20, one step at a time.']},
 'dev-maths-y1-0017': {'difficulty': 1,
                       'hints': ['Try using counters or a number line.',
                                 'Start counting at 18. Which number comes next?']},
 'dev-maths-y1-0396': {'prompt': 'How many tens and ones make 14?',
                       'hints': ['Make a group of ten first.',
                                 'After making a group of ten, how many ones are left?']},
 'dev-maths-y1-0397': {'hints': ['Count the sides of each shape.', 'Look for a shape with three corners.'],
                       'visual': 'choice-shapes'},
 'dev-maths-y1-0398': {'hints': ['Both ribbons are measured in centimetres.',
                                 'Compare 9 and 6. Which number is larger?']},
 'dev-maths-y1-0399': {'hints': ['Count forward one hour.', 'Which number comes just after 3 on the clock?'],
                       'visual': 'clock:3:00'},
 'dev-maths-y1-0400': {'hints': ['Add 2 and 5.', 'Start with 5p and count on two more pence.']},
 'dev-maths-y1-0401': {'prerequisites': ['counting'], 'visual': 'sandwich:equal-halves'}}

YEAR1_REVIEW37_48 = {'dev-spelling-y1-0036': {'prompt': 'Put the letters in order to complete the sentence: Look at '
                                    '___ moon.',
                          'hints': ['Think of ‘the moon’ and ‘the sun’. Which word appears in '
                                    'both?',
                                    'Start with th, then add the final letter.']},
 'dev-spelling-y1-0037': {'prompt': 'Put the letters in order to complete the sentence: Pip ___ '
                                    'hello.',
                          'hints': ['Say the sentence aloud. Which word is missing?',
                                    'Start with s and finish with d. Put a then i between them.']},
 'dev-spelling-y1-0038': {'hints': ['Say the word slowly and listen for its sounds.',
                                    'Start with sh. These two letters make one sound. Then add i '
                                    'and p.'],
                          'difficulty': 2},
 'dev-spelling-y1-0039': {'hints': ['Say the word slowly and listen for its sounds.',
                                    'Start with r and finish with n. Put a then i in the middle to '
                                    'make the ‘ay’ sound.'],
                          'difficulty': 2},
 'dev-spelling-y1-0040': {'hints': ['Say the word slowly and listen for its sounds.',
                                    'Start with sh. These two letters make one sound. Then add o '
                                    'and p.']},
 'dev-spelling-y1-0041': {'prompt': 'Complete the sentence: One foot, two ___.',
                          'hints': ['Say the word slowly and listen for its sounds.',
                                    'Start with f and finish with t. Put two e letters in the '
                                    'middle to make the ‘ee’ sound.'],
                          'difficulty': 2},
 'dev-spelling-y1-0042': {'hints': ['Say the word slowly and listen for its sounds.',
                                    'Start with t, then r. Finish with two e letters to make the '
                                    '‘ee’ sound.'],
                          'difficulty': 2},
 'dev-spelling-y1-0043': {'prompt': 'Put the letters in order to complete the sentence: Come ___ '
                                    'me.',
                          'hints': ['Say the word slowly and listen for its sounds.',
                                    'Start with w, then i. Finish with t and h together.']},
 'dev-spelling-y1-0044': {'hints': ['Say the word slowly and listen for its sounds.',
                                    'Start with ch. Then add a, i and r to spell the word’s '
                                    'ending, air.']},
 'dev-story-y1-0057': {'hints': ['Read the story again and look for a clue.',
                                 'Look at the first sentence. What did Pip find beside the '
                                 'harbour?']},
 'dev-story-y1-0058': {'hints': ['Read the story again and look for a clue.',
                                 'The sail had a hole. How could a scrap of cloth help?'],
                       'difficulty': 2},
 'dev-story-y1-0059': {'hints': ['Read the story again and look for a clue.',
                                 'Look at the final sentence. What did Pip keep to plant at '
                                 'home?']}}

YEAR1_REVIEW49_60 = {'dev-story-y1-0432': {'hints': ['Read the passage again.',
                                 'Look at the first sentence. Where did Ada put the red cup?'],
                       'difficulty': 1},
 'dev-story-y1-0433': {'hints': ['Read the passage again.',
                                 'What was on the cup? What happened before Ada picked it up?']},
 'dev-tables-y1-0018': {'hints': ['Tap Add a group to build equal groups.',
                                  'Build two groups. Check that each group has two dots, then '
                                  'count all the dots.']},
 'dev-tables-y1-0019': {'hints': ['Tap Add a group to build equal groups.',
                                  'Build three groups with five dots in each. Count in fives, '
                                  'saying one number for each group.']},
 'dev-tables-y1-0020': {'prompt': 'How many groups of 10 make 40?',
                        'hints': ['Tap Add a group to build equal groups.',
                                  'Build groups of ten until you have forty dots altogether. Count '
                                  'how many groups you made.'],
                        'difficulty': 2},
 'dev-tables-y1-0021': {'hints': ['Tap Add a group to build equal groups.',
                                  'Build five groups with two dots in each. Count in twos, saying '
                                  'one number for each group.']},
 'dev-tables-y1-0022': {'hints': ['Tap Add a group to build equal groups.',
                                  'Build six groups with five dots in each. Count in fives, saying '
                                  'one number for each group.']},
 'dev-tables-y1-0023': {'prompt': 'How many groups of 10 make 70?',
                        'hints': ['Tap Add a group to build equal groups.',
                                  'Build groups of ten until you have seventy dots altogether. '
                                  'Count how many groups you made.'],
                        'difficulty': 2},
 'dev-tables-y1-0024': {'hints': ['Tap Add a group to build equal groups.',
                                  'Build eight groups with two dots in each. Count in twos, saying '
                                  'one number for each group.']},
 'dev-tables-y1-0025': {'hints': ['Tap Add a group to build equal groups.',
                                  'Build nine groups with five dots in each. Count in fives, '
                                  'saying one number for each group.']},
 'dev-tables-y1-0026': {'prompt': 'How many groups of 10 make 100?',
                        'hints': ['Tap Add a group to build equal groups.',
                                  'Build groups of ten until you have one hundred dots altogether. '
                                  'Count how many groups you made.'],
                        'difficulty': 2},
 'dev-tables-y1-0027': {'hints': ['Tap Add a group to build equal groups.',
                                  'Build two groups. Check that each group has two dots, then '
                                  'count all the dots.']}}

YEAR1_REVIEW61_72 = {'dev-tables-y1-0028': {'hints': ['Tap Add a group to build equal groups.',
                                  'Build three groups with five dots in each. Count in fives, '
                                  'saying one number for each group.']},
 'dev-tables-y1-0029': {'prompt': 'How many groups of 10 make 40?',
                        'hints': ['Tap Add a group to build equal groups.',
                                  'Build groups of ten until you have forty dots altogether. Count '
                                  'how many groups you made.'],
                        'difficulty': 2},
 'dev-tables-y1-0030': {'hints': ['Tap Add a group to build equal groups.',
                                  'Build five groups with two dots in each. Count in twos, saying '
                                  'one number for each group.']},
 'dev-tables-y1-0031': {'hints': ['Tap Add a group to build equal groups.',
                                  'Build six groups with five dots in each. Count in fives, saying '
                                  'one number for each group.']},
 'dev-tables-y1-0032': {'prompt': 'How many groups of 10 make 70?',
                        'hints': ['Tap Add a group to build equal groups.',
                                  'Build groups of ten until you have seventy dots altogether. '
                                  'Count how many groups you made.'],
                        'difficulty': 2},
 'dev-tables-y1-0033': {'hints': ['Tap Add a group to build equal groups.',
                                  'Build eight groups with two dots in each. Count in twos, saying '
                                  'one number for each group.']},
 'dev-tables-y1-0034': {'hints': ['Tap Add a group to build equal groups.',
                                  'Build nine groups with five dots in each. Count in fives, '
                                  'saying one number for each group.']},
 'dev-tables-y1-0035': {'prompt': 'How many groups of 10 make 100?',
                        'hints': ['Tap Add a group to build equal groups.',
                                  'Build groups of ten until you have one hundred dots altogether. '
                                  'Count how many groups you made.'],
                        'difficulty': 2},
 'dev-words-y1-0060': {'hints': ['Read the sentence aloud.',
                                 'Which word tells what Pip does with the seed? Try it in the '
                                 'sentence.']},
 'dev-words-y1-0061': {'hints': ['Read the sentence aloud.',
                                 'The boat travels on the water from one side of the bay to the '
                                 'other. Which word fits?']},
 'dev-words-y1-0062': {'hints': ['Read the sentence aloud.',
                                 'Ada shares with others. Which word describes someone who is '
                                 'caring and helpful?'],
                       'difficulty': 1},
 'dev-words-y1-0063': {'hints': ['Read the sentence aloud.',
                                 'Imagine turning on a lamp in a dark room. Which word describes '
                                 'the room now?']}}

YEAR1_REVIEW73_74 = {'dev-words-y1-0064': {'hints': ['Read the sentence aloud.',
                                 'Imagine a little seed beside a big tree. Which word describes '
                                 'something very small?']},
 'dev-words-y1-0065': {'hints': ['Read the sentence aloud.',
                                 'The sail had a hole. They fixed it so it could be used again. '
                                 'Which word means fixed?']}}

YEAR2_REVIEW1_12 = {'dev-logic-y2-0111': {'hints': ['Find the smallest number first.',
                                 'Count on in ones. Choose the next number each time.']},
 'dev-logic-y2-0112': {'hints': ['Find the smallest number first.',
                                 'Count on in twos. Choose the next number each time.']},
 'dev-logic-y2-0113': {'hints': ['Find the smallest number first.',
                                 'Count on in threes. Choose the next number each time.']},
 'dev-logic-y2-0114': {'hints': ['Find the smallest number first.',
                                 'Count on in ones. Choose the next number each time.']},
 'dev-logic-y2-0115': {'hints': ['Find the smallest number first.',
                                 'Start with the smallest number, then count on in twos. Choose '
                                 'the next number each time.']},
 'dev-logic-y2-0116': {'hints': ['Find the smallest number first.',
                                 'Start with the smallest number, then count on in threes. Choose '
                                 'the next number each time.']},
 'dev-logic-y2-0117': {'hints': ['Find the smallest number first.',
                                 'Count on in ones. Choose the next number each time.']},
 'dev-logic-y2-0118': {'hints': ['Find the smallest number first.',
                                 'Start with the smallest number, then count on in twos. Choose '
                                 'the next number each time.']},
 'dev-logic-y2-0119': {'hints': ['Find the smallest number first.',
                                 'Start with the smallest number, then count on in threes. Choose '
                                 'the next number each time.']},
 'dev-logic-y2-0120': {'hints': ['Find the smallest number first.',
                                 'Count on in ones. Choose the next number each time.']},
 'dev-logic-y2-0121': {'hints': ['Find the smallest number first.',
                                 'Start with the smallest number, then count on in twos. Choose '
                                 'the next number each time.']},
 'dev-logic-y2-0122': {'hints': ['Find the smallest number first.',
                                 'Start with the smallest number, then count on in threes. Choose '
                                 'the next number each time.']}}

YEAR2_REVIEW13_24 = {'dev-maths-y2-0066': {'hints': ['Start at 1.', 'Count on one step. Which number comes next?']},
 'dev-maths-y2-0067': {'hints': ['Start with the total and count back.',
                                 'Start at 4 and count back two steps, one step at a time.'],
                       'difficulty': 1},
 'dev-maths-y2-0068': {'hints': ['Try using counters or a number line.',
                                 'Start counting at 3. Which number comes next?'],
                       'difficulty': 1},
 'dev-maths-y2-0069': {'hints': ['Start at 4.', 'Count on four steps, one step at a time.']},
 'dev-maths-y2-0070': {'hints': ['Start with the total and count back.',
                                 'Start at 10 and count back five steps, one step at a time.'],
                       'difficulty': 1},
 'dev-maths-y2-0071': {'hints': ['Try using counters or a number line.',
                                 'Start counting at 6. Which number comes next?'],
                       'difficulty': 1},
 'dev-maths-y2-0072': {'hints': ['Start at 7.', 'Count on seven steps, one step at a time.'],
                       'difficulty': 2},
 'dev-maths-y2-0073': {'hints': ['Start with the total and count back.',
                                 'Start at 9 and count back one step. Which number comes just '
                                 'before 9?'],
                       'difficulty': 1},
 'dev-maths-y2-0074': {'hints': ['Try using counters or a number line.',
                                 'Start counting at 9. Which number comes next?'],
                       'difficulty': 1},
 'dev-maths-y2-0075': {'hints': ['Start with the larger number and count on.',
                                 'Start at 10 and count on three steps, one step at a time.']},
 'dev-maths-y2-0076': {'hints': ['Start with the total and count back.',
                                 'Start at 15 and count back four steps, one step at a time.']},
 'dev-maths-y2-0077': {'hints': ['Try using counters or a number line.',
                                 'Start counting at 12. Which number comes next?'],
                       'difficulty': 1}}

YEAR2_REVIEW25_36 = {'dev-maths-y2-0078': {'hints': ['Start with the larger number and count on.',
                                 'Start at 13 and count on six steps, one step at a time.'],
                       'difficulty': 2},
 'dev-maths-y2-0079': {'hints': ['Start with the total and count back.',
                                 'Start at 21. Count back one step to 20, then six more steps.']},
 'dev-maths-y2-0080': {'hints': ['Try using counters or a number line.',
                                 'Start counting at 15. Which number comes next?'],
                       'difficulty': 1},
 'dev-maths-y2-0081': {'hints': ['Start with the larger number and count on.',
                                 'Start at 16 and count on two steps.']},
 'dev-maths-y2-0082': {'hints': ['Start with the total and count back.',
                                 'Start at 20 and count back three steps, one step at a time.']},
 'dev-maths-y2-0083': {'hints': ['Try using counters or a number line.',
                                 'Start counting at 18. Which number comes next?'],
                       'difficulty': 1},
 'dev-maths-y2-0402': {'explanation': 'Four tens make 40. Add seven ones to make 47.',
                       'hints': ['Write the tens digit before the ones digit.',
                                 'Four tens make 40. What number is seven more than 40?']},
 'dev-maths-y2-0403': {'hints': ['A face is a flat surface. Imagine a cube-shaped box.',
                                 'Count the top and bottom, then count the four faces around the '
                                 'sides.']},
 'dev-maths-y2-0404': {'visual': 'clock:8:30',
                       'hints': ['An hour has sixty minutes.',
                                 'Half past means thirty minutes after the hour. Keep 8 before the '
                                 'colon and choose the matching minutes.']},
 'dev-maths-y2-0405': {'hints': ['Subtract the cost from the amount paid.',
                                 'Count on from 35p to 40p, then to 50p. Add the two jumps to find '
                                 'the change.']},
 'dev-maths-y2-0406': {'hints': ['A quarter is one of four equal parts.',
                                 'Share 20 counters equally into four groups. How many counters '
                                 'are in one group?']},
 'dev-maths-y2-0407': {'visual': 'fruit:4:7',
                       'hints': ['Add both groups; do not subtract them.',
                                 'Start with the seven pears and count on four more for the '
                                 'apples.']}}

YEAR2_REVIEW37_48 = {'dev-spelling-y2-0102': {'prompt': 'Put the letters in order to complete the sentence: Pip wore a '
                                    'coat ___ it was cold.',
                          'hints': ['Say the word in syllables.',
                                    'Start with be. Then add c, a, u, s and e.'],
                          'difficulty': 3},
 'dev-spelling-y2-0103': {'hints': ['Say the word in syllables.',
                                    'Start with p, e and o. Finish with p, l and e.']},
 'dev-spelling-y2-0104': {'hints': ['Say the word in syllables.',
                                    'Start with child. Then add r, e and n to make the word for '
                                    'more than one child.']},
 'dev-spelling-y2-0105': {'hints': ['Say the word aloud. Listen for the first and last sounds.',
                                    'Start with d and finish with r. Put two o letters in the '
                                    'middle.'],
                          'difficulty': 2},
 'dev-spelling-y2-0106': {'prompt': 'A word meaning lovely to look at.',
                          'hints': ['Say the word in syllables.',
                                    'Start with p, r and e. Add two t letters and finish with y.']},
 'dev-spelling-y2-0107': {'prompt': 'Put the letters in order to complete the sentence: Pip waters '
                                    'the plant ___ day.',
                          'hints': ['Say the word in syllables.',
                                    'Start with e and v. Add another e, then finish with r and y.'],
                          'difficulty': 2},
 'dev-spelling-y2-0108': {'hints': ['Say the word slowly and listen for each sound.',
                                    'Start with p and l. Then add a, n and t.']},
 'dev-spelling-y2-0109': {'hints': ['Say the sentence aloud. Which word is missing?',
                                    'Start with w, then o and u. Remember the l before the final '
                                    'd.']},
 'dev-spelling-y2-0110': {'hints': ['Say the word in syllables.',
                                    'Start with a and g. Add another a, then finish with i and n.'],
                          'difficulty': 2},
 'dev-story-y2-0123': {'hints': ['Read the story again and look for a clue.',
                                 'Look at the first sentence. What did Pip find beside the '
                                 'harbour?']},
 'dev-story-y2-0124': {'hints': ['Read the story again and look for a clue.',
                                 'The sail had a hole. How could a scrap of cloth help?'],
                       'difficulty': 2},
 'dev-story-y2-0125': {'hints': ['Read the story again and look for a clue.',
                                 'Look at the final sentence. What did Pip keep to plant at '
                                 'home?']}}

def extend(add, activities):
    for y,skill,prompt,answer,distractors,explanation,hint in EXERCISES:
        add('MATHS',y,skill,2,prompt,answer,[answer,*distractors],explanation,[hint,'Work through the quantities one step at a time.'],f'Practise Year {y} {skill.replace("-"," ")}; exact curriculum mapping awaits teacher review.')
    for y,story,questions in STORIES:
        for prompt,answer,distractors,explanation,skill in questions:
            add('STORY',y,'reading-'+skill,2,prompt,answer,[answer,*distractors],explanation,['Read the passage again.','Choose the answer supported by details in this passage.'],'Retrieve information, infer meaning and justify answers using textual evidence.',story=story)
    # Append-only authoring preserves IDs of the original exercises and their history.
    for a in activities:
        a['prerequisites']=PREREQUISITES.get(a['skill'],[])
        if a['id'] in YEAR1_LOGIC_HINTS:
            # Publisher accepted these twelve wording corrections individually; no release approval is implied.
            a['hints'][1]=YEAR1_LOGIC_HINTS[a['id']]
            a['version']=3
        if a['game']=='SPELLING' and a['prompt'].startswith('Build this word:'):
            a['prompt']=CLUES[a['answer']]
            a['hints']=['Say the word in syllables.','Look for a familiar root, prefix or suffix.']
        if a['skill']=='addition':a['hints']=['Start with the larger number and count on.','Break the smaller number into easy parts.']
        elif a['skill']=='subtraction':a['hints']=['Start with the total and count back.','Check by adding your answer to the number taken away.']
        elif a['skill']=='measurement':a['hints']=['Identify the unit you are starting with.','Use the conversion described in this question.']
        elif a['skill']=='fractions':a['hints']=[a['hints'][0],'Think about equal parts of the same whole.']
        if a['id'] in YEAR1_MATHS_CORRECTIONS:
            # Apply publisher-agreed item corrections after the generic skill hints.
            a.update(YEAR1_MATHS_CORRECTIONS[a['id']])
            a['version']=3

        if a['id'] in YEAR1_REVIEW25_36:
            a.update(YEAR1_REVIEW25_36[a['id']])
            a['version']=3

        if a['id'] in YEAR1_REVIEW37_48:
            a.update(YEAR1_REVIEW37_48[a['id']])
            a['version']=3

        if a['id'] in YEAR1_REVIEW49_60:
            a.update(YEAR1_REVIEW49_60[a['id']])
            a['version']=3

        if a['id'] in YEAR1_REVIEW61_72:
            a.update(YEAR1_REVIEW61_72[a['id']])
            a['version']=3

        if a['id'] in YEAR1_REVIEW73_74:
            a.update(YEAR1_REVIEW73_74[a['id']])
            a['version']=3

        if a['id'] in YEAR2_REVIEW1_12:
            a.update(YEAR2_REVIEW1_12[a['id']])
            a['version']=3

        if a['id'] in YEAR2_REVIEW13_24:
            a.update(YEAR2_REVIEW13_24[a['id']])
            a['version']=3

        if a['id'] in YEAR2_REVIEW25_36:
            a.update(YEAR2_REVIEW25_36[a['id']])
            a['version']=3

        if a['id'] in YEAR2_REVIEW37_48:
            a.update(YEAR2_REVIEW37_48[a['id']])
            a['version']=3
