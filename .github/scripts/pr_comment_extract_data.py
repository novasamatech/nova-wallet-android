import os
import sys
import re
from datetime import datetime, timezone
import requests


ALLOWED_SEVERITIES = {"Major", "Critical", "Normal"}

# Matches: "Release severity: <value>" (case-insensitive, flexible spaces)
SEVERITY_LINE_RE = re.compile(r"^release\s+severity\s*:\s*(.+)$", re.IGNORECASE)


def parse_base_params(comment_link: str) -> None:
    if not comment_link:
        print("COMMENT_LINK is not set. Provide a valid PR comment API URL in env var COMMENT_LINK.")
        sys.exit(1)

    env_file = os.getenv("GITHUB_ENV")
    if not env_file:
        print("GITHUB_ENV is not set. This script expects GitHub Actions environment.")
        sys.exit(1)

    try:
        resp = requests.get(comment_link, timeout=10)
        resp.raise_for_status()
        payload = resp.json()
    except requests.RequestException as e:
        print(f"Failed to fetch PR comment: {e}")
        sys.exit(1)
    except ValueError:
        print("Response is not valid JSON.")
        sys.exit(1)

    body = payload.get("body")
    if not isinstance(body, str) or not body.strip():
        print("PR comment body is empty. Add 'Release severity: Major | Critical | Normal'.")
        sys.exit(1)

    lines = [line.strip() for line in body.splitlines()]

    severity_raw = ""

    for line in lines:
        m = SEVERITY_LINE_RE.match(line)
        if m:
            severity_raw = m.group(1).strip()
            break

    if not severity_raw:
        print("Release severity is missing. Add a line 'Release severity: Major | Critical | Normal'.")
        sys.exit(1)

    if severity_raw not in ALLOWED_SEVERITIES:
        print(f"Invalid severity '{severity_raw}'. Allowed values: Major, Critical, Normal.")
        sys.exit(1)

    severity = severity_raw

    time_iso = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")

    try:
        with open(env_file, "a", encoding="utf-8") as f:
            f.write(f"TIME={time_iso}\n")
            f.write(f"SEVERITY={severity}\n")
    except OSError as e:
        print(f"Failed to write to GITHUB_ENV: {e}")
        sys.exit(1)


if __name__ == "__main__":
    parse_base_params(os.getenv("COMMENT_LINK"))
