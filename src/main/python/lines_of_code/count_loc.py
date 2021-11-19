import subprocess
import os
import re

directory = '.\\code_for_parsing'


lines_of_code = ['folder,loc\n']
for x in list(os.walk(directory))[1:]:
	folder = x[0]
	print('Analysing files in {}'.format(folder))
	result = subprocess.run(['cloc', folder],
           stdout=subprocess.PIPE,
           stderr=subprocess.STDOUT,
		   shell=True)
	
	javaMatch = re.search(r'Java\s+\d+\s+\d+\s+\d+', str(result.stdout))[0]
	loc = int(re.findall(r'\d+', javaMatch)[-1])
	lines_of_code.append('{},{}\n'.format(folder, loc))

with open('output.csv', 'w') as out:
	out.writelines(lines_of_code)