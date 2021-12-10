# Make sure that cloc is installed on your computer before running this code.
# Change the directory variable to be the directory containing all the subdirectories of repos.
# This code runs cloc on all directories, and compiles the resulting Java lines of code into output.csv
# Written for Windows 10. Adjustments may be needed for Linux.
# Created by Alexander Aldridge as a tool for Fall 2021 SENG 480B at UVic

import subprocess
import os
import re

directory = '.\\code_for_parsing'


lines_of_code = ['folder,loc\n']
directories = list(os.walk(directory))

first_directory = 900

for x in directories[first_directory:-1]:
	if x is None or x[0] is None:
		print('Error with directory ' + str(x))
		continue
	folder = x[0]
	print('Analysing files in {}'.format(folder))
	result = subprocess.run(['cloc', folder],
           stdout=subprocess.PIPE,
           stderr=subprocess.STDOUT,
		   shell=True)
	match_object = re.search(r'Java\s+\d+\s+\d+\s+\d+', str(result.stdout))
	
	if match_object is None:
		print('Unable to find match for directory ' + str(directory))
		continue

	loc = int(re.findall(r'\d+', match_object[0])[-1])
	lines_of_code.append('{},{}\n'.format(folder, loc))

with open('output{}-{}.csv'.format(first_directory, 'end'), 'w') as out:
	out.writelines(lines_of_code)