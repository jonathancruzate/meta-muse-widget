#!/usr/bin/env python3
"""panel-brief-push — distill the Daily Brief snapshot for the Muse
panel's live-data lane and POST it to the mode-sync Worker's
panel-ingest endpoint (Muse Widget v4.7.0, LIVE-DATA-LANE-DESIGN.md
§5/§8.4).

Reads ~/workspace/daily-brief/dashboard_snapshot.json (already
privacy-redacted at brief compile time), distills it to the §5 caps,
and applies the v1 exclusions Jonathan approved:
  - decide-kind items are NEVER included as highlights — only the
    decide_count number is sent;
  - anything health-adjacent is dropped (clinical keyword filter);
  - the Friends & following and Missed notifications sections are
    not distilled (message summaries / tray noise, not highlights);
  - the raw brief body never leaves the VM.

Transport is curl (the site's WAF drops Python TLS fingerprints on
these paths; curl is the lane every other mode-sync client uses).
The POLL_SECRET is read from its existing 0600 file at run time and
never printed. A failed POST exits nonzero with a stderr note —
callers in the brief lane must treat that as non-fatal: this push
must never block the brief itself.

Hook point (brief lane is agent-run cron jobs, no shell hook):
append one step to the brief compile/refresh job bodies
(daily-brief-compile, daily-brief-refresh-*, midday-brief,
evening-brief): "After writing dashboard_snapshot.json, run
~/workspace/metamuse-widget/tools/panel-brief-push.py and ignore
its exit code." Until that step is added, run this script manually
after any brief refresh.

Usage: panel-brief-push.py [--dry-run]
"""
import json
import os
import re
import subprocess
import sys
from datetime import datetime, timezone

SNAPSHOT = os.path.expanduser("~/workspace/daily-brief/dashboard_snapshot.json")
SECRET_FILE = os.path.expanduser(
    "~/workspace/goals/manage-my-smart-home/hidden_files/"
    "st-webhook-poll-secret.txt")
INGEST_URL = "https://jonathancruzate.com/mode-sync/panel-ingest"

MAX_HIGHLIGHTS = 5
TITLE_CAP = 80
DETAIL_CAP = 140
HEADLINE_CAP = 120

# Section -> highlight kind. Sections not listed are not distilled.
SECTION_KINDS = {
    "top of mind": "fyi",
    "do": "task",
    "looking ahead": "calendar",
    "fyi": "fyi",
    "heads up": "fyi",
}
# Distillation priority: earlier sections fill the 5 slots first.
SECTION_ORDER = ["top of mind", "do", "looking ahead", "fyi", "heads up"]

# v1 health-adjacency filter (Jonathan's default: exclude anything
# health-adjacent from highlights and the headline). Over-filtering
# is the safe direction for a capability-gated endpoint.
HEALTH_RE = re.compile(
    r"\b(medications?|meds|prescriptions?|pharmacy|cvs|walgreens|"
    r"doctor|dr\.|therapy|therapist|dentist|dental|referral|clinic|"
    r"gastro\w*|colonoscop\w*|endoscop\w*|humira|hyrimoz|zocdoc|"
    r"vaccin\w*|bloodwork|copay|deductible|diagnos\w*|symptoms?|"
    r"registrations? after|checkup|check-up)\b",
    re.IGNORECASE)


def is_health(text):
    return bool(HEALTH_RE.search(text or ""))


def clean(text):
    """Strip the brief's markdown dressing from a fragment."""
    text = re.sub(r"\[([^\]]*)\]\([^)]*\)", r"\1", text)  # [x](url) -> x
    text = text.replace("**", "").replace("__", "")
    text = re.sub(r"\s+", " ", text)
    return text.strip()


def truncate(text, cap):
    if len(text) <= cap:
        return text
    cut = text[:cap].rsplit(" ", 1)[0]
    return (cut or text[:cap]).strip()


def parse_sections(body):
    """Split the brief body into {section_name: [item, ...]} where an
    item is {'title': str, 'detail': str}; also returns the Today line."""
    sections = {}
    today = ""
    current = None
    current_item = None

    def flush_item():
        nonlocal current_item
        if current is not None and current_item is not None:
            sections.setdefault(current, []).append(current_item)
        current_item = None

    for raw in body.splitlines():
        line = raw.rstrip()
        m = re.match(r"^([A-Za-z][A-Za-z &]+?)(?:\s*\(\d+\))?:\s*(.*)$", line)
        if m and not line.startswith("-") and not line.startswith("#"):
            name = m.group(1).strip().lower()
            if name == "today":
                today = m.group(2).strip()
                current = None
                continue
            if name in SECTION_KINDS or name in (
                    "decide", "friends & following", "missed notifications"):
                flush_item()
                current = name
                sections.setdefault(current, [])
                continue
        if current is None:
            continue
        if line.startswith("- "):
            flush_item()
            current_item = {"title": clean(line[2:]), "detail": ""}
        elif line.strip() and current_item is not None:
            extra = clean(line)
            current_item["detail"] = (
                current_item["detail"] + " " + extra).strip()
    flush_item()
    return sections, today


def distill(snapshot):
    body = snapshot.get("body") or ""
    sections, today = parse_sections(body)

    highlights = []
    for name in SECTION_ORDER:
        kind = SECTION_KINDS[name]
        for item in sections.get(name, []):
            if len(highlights) >= MAX_HIGHLIGHTS:
                break
            title = item["title"]
            detail = item["detail"]
            # Top-of-mind titles carry their numbering ("1. ...").
            title = re.sub(r"^\d+\.\s*", "", title)
            if not detail and " — " in title:
                title, detail = title.split(" — ", 1)
            if is_health(title) or is_health(detail):
                continue
            if not title:
                continue
            highlights.append({
                "kind": kind,
                "title": truncate(title, TITLE_CAP),
                "detail": truncate(detail, DETAIL_CAP),
            })
        if len(highlights) >= MAX_HIGHLIGHTS:
            break

    # Headline: the Today line with health segments removed.
    headline = ""
    if today:
        segments = re.split(r"\s+—\s+|,\s+", clean(today))
        kept = [s for s in segments if s and not is_health(s)]
        headline = truncate(", ".join(kept), HEADLINE_CAP)
    if not headline:
        edition = (snapshot.get("edition") or "daily").capitalize()
        headline = f"{edition} brief"

    generated = snapshot.get("generated_at") or ""
    try:
        gen_utc = (datetime.fromisoformat(generated)
                   .astimezone(timezone.utc)
                   .strftime("%Y-%m-%dT%H:%M:%SZ"))
    except (ValueError, TypeError):
        gen_utc = generated

    decide_count = snapshot.get("decide_count")
    if not isinstance(decide_count, int) or decide_count < 0:
        decide_count = 0

    return {
        "date": snapshot.get("brief_date") or "",
        "generated_at": gen_utc,
        "headline": headline,
        "highlights": highlights,
        "decide_count": decide_count,
    }


def main():
    dry_run = "--dry-run" in sys.argv[1:]
    try:
        with open(SNAPSHOT) as f:
            snapshot = json.load(f)
    except Exception as e:
        print(f"panel-brief-push: cannot read snapshot: {e}",
              file=sys.stderr)
        return 1
    brief = distill(snapshot)
    if dry_run:
        print(json.dumps(brief, indent=2, ensure_ascii=False))
        return 0
    try:
        with open(SECRET_FILE) as f:
            secret = f.read().strip()
    except Exception as e:
        print(f"panel-brief-push: cannot read poll secret: {e}",
              file=sys.stderr)
        return 1
    payload = json.dumps(brief, ensure_ascii=False)
    try:
        proc = subprocess.run(
            ["curl", "-sS", "--max-time", "30", "-X", "POST",
             "-H", "Content-Type: application/json",
             "--data-binary", "@-",
             f"{INGEST_URL}?secret={secret}"],
            input=payload.encode(), capture_output=True, timeout=40)
        out = proc.stdout.decode(errors="replace")
        if proc.returncode != 0 or '"ok":true' not in out.replace(" ", ""):
            print(f"panel-brief-push: ingest failed "
                  f"(rc={proc.returncode}): {out[:200]}", file=sys.stderr)
            return 1
    except Exception as e:
        print(f"panel-brief-push: POST failed: {e}", file=sys.stderr)
        return 1
    print(f"panel-brief-push: ok — {len(brief['highlights'])} highlights, "
          f"decide_count {brief['decide_count']}, date {brief['date']}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
