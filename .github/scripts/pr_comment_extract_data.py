import requests
import sys
import os
from datetime import datetime


def parse_base_params(comment_link):
    response = requests.get(comment_link).json()
    try:
        lines = response["body"].split("\n")
    except:
        print("Please, create a comment with 'Release severity: Major | Critical | Normal' and 'Release notes:'")
        exit(1)
    
    time = datetime.utcnow().strftime('%Y-%m-%dT%H:%M:%SZ')
    severity = next((s.split(': ')[1] for s in lines if s.startswith('Release severity: ')), '').strip()

    if not time or not severity:
        print(f"Current values: Time - '{time}', Severity - '{severity}'")
        print("Time or severity cannot be empty, please set it in the PR comment with lines 'Release time:' and 'Release severity:'")
        sys.exit(1)

    with open(os.getenv('GITHUB_ENV'), 'a') as f:
        f.write(f'TIME={time}\n')
        f.write(f'SEVERITY={severity}\n')
parse_base_params(os.getenv("COMMENT_LINK"))
