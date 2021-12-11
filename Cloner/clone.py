from git import Repo
import os
from shutil import copy, rmtree


f = open("git_list_norep.txt", "r")
for i in range(1000):
    repo = f.readline()
    name = repo.replace('.git', '').replace('https://github.com/', '').replace("/", "--").strip()
    print(name)
    Repo.clone_from(repo.strip(), 'repos/' + name)

    os.mkdir('code_for_parsing/' + name)
    os.mkdir('code_for_parsing/' + name + "/test")
    os.mkdir('code_for_parsing/' + name + "/non_test")
    
    for subdir, dirs, files in os.walk('repos/' + name):
        for filename in files:
            filepath = subdir + os.sep + filename

            if filepath.endswith(".java") and "test" in filepath.lower():
                copy(filepath, 'code_for_parsing/' + name + "/test")
                print(filepath)
            elif filepath.endswith(".java"):
                copy(filepath, 'code_for_parsing/' + name + "/non_test")
                print(filepath)
    rmtree('repos/' + name)