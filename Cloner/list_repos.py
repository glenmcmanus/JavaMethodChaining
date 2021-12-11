import requests

f = open("git_list.txt", "w")

print("Start Queries")
for i in range(1, 11):
    results = requests.get(
        'https://api.github.com/search/repositories?q=language:java&sort=stars&order=desc&per_page=100&page='+str(i)).json()

    print("Request {} returned with {} results".format(i, len(results['items'])))
    for x in results['items']:
        f.write(x["clone_url"])
        f.write('\n')

f.close()