# Live-Data Lane for the Muse Panel — Design (Phase 2)

Status: DESIGN ONLY — no code written. Target release: **v4.7.0**.
Scope: `MusePanelActivity` gains live Muse data — today's Daily Brief
highlights and current house mode/state. The search-bar widget, QS
tiles, and shortcuts are out of scope, and the frozen widget surfaces
(`SearchBarWidgetProvider.java`, `res/layout/widget_search_bar.xml`,
`res/xml/widget_info_search.xml`) stay untouched — the render-guard
lesson of `REA-RENDER-DIFF-431.md` applies to anything this phase adds:
nothing in this design inflates through RemoteViews.

## 1. Constraints (absolute)

1. **No secrets in the APK, ever.** No MacroDroid webhook URLs, no API
   keys, no Worker secrets (`POLL_SECRET`, `PHONE_PUSH_SECRET`), no
   tokens — embedded, obfuscated, or split. Anything compiled into the
   APK is treated as extractable from the APK.
2. **Pure Java, no Gradle, no third-party SDKs.** Build stays
   aapt2 → javac → d8 via `build.sh`. Platform APIs only
   (`HttpURLConnection`, `org.json` are both platform — allowed).
3. **No `hatch://` data deeplinks.** The Muse app
   (`com.facebook.aura`) gates its deeplinks by TrustedCaller
   (REA-verified, `REA-DEEPLINK-FINDINGS.md`); the panel keeps using
   only the existing ungated Router surfaces for *actions*. This lane
   is for *reading data*, and it does not go through the Muse app.
4. **Stale data must show its age, never fake-fresh.** Every rendered
   value carries the timestamp it was true at, and the UI labels age
   explicitly (see §6).
5. **Reuse existing infrastructure.** New surface area needs a reason;
   new URL prefixes need the enumeration in §2.3 first.

## 2. What already exists (researched 2026-10-28)

### 2.1 The phone bridge (MacroDroid + Workers)

From `~/workspace/custom-skills/local-phone-automations/SKILL.md` and
the Worker sources in `~/workspace/cloudflare-workers/`:

- **Universal `Muse` MacroDroid macro** — one webhook URL, branched by
  `?do=…`. The URL is a capability secret held in a 0600 file on the
  VM and in the macro on the phone. It is a *control* lane (fire →
  phone acts). It cannot be in the APK (constraint 1) and the panel
  does not need it: the panel is read-only.
- **Worker `mode-sync`** (KV binding `MODE_SYNC`) — owns house-mode
  truth. Canonical paths, **every one gated by `?secret=`**
  (`POLL_SECRET` or `PHONE_PUSH_SECRET`, constant-time compared):
  - `GET /mode-sync/mode-desired` → `{mode, seq, updated_at}`
  - `POST /mode-sync/mode-set` (VM writes desired)
  - `POST /mode-sync/mode-ack` (phone's Mode Poll macro reports actual)
  - `GET /mode-sync/mode-state` → `{desired, actual, in_sync}`, where
    `actual = {current, matches_desired, desired_seq, note,
    reported_at}` and `in_sync` requires seq match.
  - Legacy `/st-webhook/mode-*` paths route here (phone macros
    unchanged). The phone reconciles drift every ~5 min via Mode Poll.
  - Deploy is bindings-preserving via
    `~/workspace/cloudflare-workers/mode-sync/deploy.py` — any new
    handler/secret added to this Worker MUST go through that pattern
    (a naive redeploy strips `secret_text` bindings; learned
    2026-10-14).
- **Worker `phone-ingress`** — notification/status intake
  (`/phone-ingress/ingress`, `/ingress-poll`), also secret-gated.
  `ingress-poll` is a **drain**: consumers read-and-clear. The panel
  must never poll it — it would steal notifications from StephAI's
  own consumers.
- **Worker `st-webhook`** — SmartThings only since the 2026-10-26
  split. It holds one relevant read: `GET /st-webhook/mode-latest`
  (secret-authed) — ST-pushed mode/location events captured to KV
  (`mode:latest_push`) as a shadow of the phone-reported state.
- **Known failure mode that shapes this design:** the phone's
  *reporting* leg can die while everything else works (2026-10-16/17:
  `mode:actual` froze ~30 h, `reported_at` stale, mode-sync failed
  closed). House mode served to the panel can therefore be hours or
  days old in the worst case. The contract and UI must carry
  `reported_at` end-to-end — this is why constraint 4 exists.

### 2.2 The Daily Brief (VM-side)

- The brief compiles on the VM (06:30 ET base + in-place refreshes
  through the day) and lands as a snapshot:
  `~/workspace/daily-brief/dashboard_snapshot.json` with keys
  `brief_date`, `edition`, `generated_at`, `timezone`, `body`,
  `podcast_url`, `spark_supplement`, `decide_count`.
- The compile pipeline already applies **privacy redaction** before
  the snapshot is written. The panel consumes a *distillate* of this
  snapshot, produced VM-side — the raw `body` never leaves the VM.
- Nothing about the brief is currently reachable from the phone.

### 2.3 `jonathancruzate.com` path inventory (route-specificity rule)

Before proposing any new prefix, the live Worker-owned space is:

| Prefix | Owner | Notes |
|---|---|---|
| `/phone-ingress*` | phone-ingress | canonical ingress |
| `/mode-sync*` | mode-sync | canonical mode endpoints (§2.1) |
| `/st-webhook/ingress*` | phone-ingress | legacy route, phone macros |
| `/st-webhook/mode-desired\|set\|ack\|state` | mode-sync | legacy route, phone macros |
| `/st-webhook/mode-latest*` | st-webhook | **ST-owned** — the 2026-10-26 shadowing incident: a new `mode-*` wildcard silently captured this path. Any new wildcard gets every existing path under its prefix enumerated first. |
| `/st-webhook/*` (rest) | st-webhook | ST lifecycle, poll, oauth-callback |
| everything else | static site | incl. `/privacy/` |

**No `/panel-*` path exists anywhere in this space.** The one new
prefix this design proposes (§4) is `/panel-data*`, chosen because it
collides with nothing above and shadows nothing.

### 2.4 The app today (v4.6.2)

- `MusePanelActivity` is programmatic UI: greeting + date (viewing
  area), then `bg_card` focus blocks (18dp radius, 24dp screen
  padding) — Up next (CalendarContract), Ask Muse (Router), Home
  controls (package launches), footer.
- The manifest **already declares `INTERNET`** — no permission change
  needed for an HTTPS read lane. No content providers, no
  `FileProvider`, no external-storage use in the panel today.

## 3. Candidate architectures

### Candidate A — Read-only status endpoint on the existing mode-sync Worker (RECOMMENDED)

A VM-side producer distills the brief snapshot and POSTs it to a new
secret-authed ingest handler on the `mode-sync` Worker, which stores
it in the KV it already owns (`panel:brief`). A new GET handler on
the same Worker merges that distillate with the mode KV it already
holds (`mode:desired`, `mode:actual`) and serves one small JSON at an
unguessable path. The app GETs it on panel open, caches the last good
response in private storage, and renders with ages.

- **Writes** use the existing secret lane (VM already holds
  `POLL_SECRET` for this Worker). The app never writes.
- **Reads** are gated by capability, not credential: a 128-bit random
  slug as the final path segment. The slug is **not in source**: at
  build time `build.sh` reads it from a local 0600 file (same pattern
  as `keystore/keystore-password.txt`) and generates a one-field
  constants class; if the file is absent the build compiles a
  placeholder and the panel shows its "not configured" state. The
  GitHub-exported source therefore never contains it. Server-side the
  slug is a Worker secret (`PANEL_SLUG`), constant-time compared,
  rotatable in one deploy + one rebuild.
- Honest residual risk: the slug *is* extractable from a distributed
  APK binary. Mitigations are data-side, not cryptographic: the
  payload is read-only, pre-redacted, and deliberately low-sensitivity
  (§5), and rotation is cheap. This is a capability URL, not a
  credential to any other system — it opens nothing else.

### Candidate B — App polls the existing `/mode-sync/mode-state` directly

Zero new server code. **Disqualified**: every mode-sync endpoint
requires `?secret=` with `POLL_SECRET` or `PHONE_PUSH_SECRET`.
Embedding either in the APK violates constraint 1 outright (and a
secret in a URL query also leaks into any log that records URLs).
It also answers only half the goal — mode-state carries no brief
data at all. Recorded here because "just poll what exists" will keep
sounding attractive; the auth model is why it can't happen.

### Candidate C — Phone-side MacroDroid file drop

A new MacroDroid macro (shipped by Muse, imported by Jonathan, per
the standing macro rule) wakes on a schedule and on mode change,
GETs what it needs — it may use the phone-held secrets, which already
live on the phone legitimately — and writes a JSON file to a shared
folder (e.g. `Download/Muse/panel.json`). The app reads that file
(SAF folder grant, or the legacy external-storage permission path)
and renders it. A content-provider variant was considered and
dropped: there is no second app to host the provider, and
MacroDroid cannot serve one.

- Best APK secret posture of the three (the app holds literally
  nothing and needs no network), and it works fully offline.
- Worst freshness and reliability: MacroDroid timers are subject to
  Doze/battery optimisation, a missed run silently ages the file
  (the age label still protects honesty), and the brief half still
  needs a server endpoint for the macro to fetch — Candidate A's
  ingest work mostly reappears, plus a phone-side hop.
- Highest setup friction: new macro import by Jonathan, a storage
  grant flow in the app, scoped-storage edge cases on Android 15.

### Scoring (1 = worst, 5 = best)

| Criterion | A: Worker endpoint | B: poll mode-state | C: MacroDroid file |
|---|---|---|---|
| Secret exposure | **4** — no credential in APK; slug is build-injected, absent from source, rotatable; payload pre-redacted | **0 — disqualified** (Worker secret in APK) | **5** — nothing in APK at all |
| Freshness | **5** — mode read live from KV at GET time; brief as fresh as last producer push (≤ the brief's own refresh cadence) | 4 — mode live, brief absent | **2** — macro cadence + Doze; mode known to the phone first but file writes lag |
| Battery / cost | **5** — one ~1–2 KB GET per panel open; no background work; KV reads negligible | 5 (moot) | **3** — recurring phone wakeups + file writes, all day, for data the app may never show |
| Build complexity | **4** — one Worker handler pair + one VM producer + one app client; all patterns already exist in-tree | 5 (moot) | **2** — macro authoring + import, SAF grant UX, storage edge cases |

## 4. Recommendation — Candidate A, concretely

### 4.1 New surface (complete list — nothing else is added)

On Worker `mode-sync`, deployed via its existing bindings-preserving
`deploy.py` (extended with the new secret source):

| Endpoint | Auth | Behaviour |
|---|---|---|
| `POST /mode-sync/panel-ingest` | `?secret=` = `POLL_SECRET` (existing) | VM producer only. Validates + caps the brief object (§5), stores it at KV key `panel:brief` with the Worker's receive time. |
| `GET /panel-data/v1/<slug>` | slug == `PANEL_SLUG` (new Worker secret, constant-time) | Merges `panel:brief` + `mode:desired` + `mode:actual` into the §5 contract. `Cache-Control: no-store` (personal JSON must never sit in CDN cache). Wrong slug → plain 404, indistinguishable from an unknown path. |

One new Cloudflare route: `jonathancruzate.com/panel-data*` →
`mode-sync`. Per §2.3 it shadows nothing. The `/mode-sync/` prefix
strip logic in `worker.js` gains a sibling branch for `/panel-data/`;
no existing handler changes behaviour.

### 4.2 Data flow

```
Daily Brief snapshot (VM, redacted at compile)
        │  distill: top ≤5 highlights, caps, no raw body
        ▼
panel-brief-push (VM, on each brief refresh)
        │  POST /mode-sync/panel-ingest?secret=…
        ▼
mode-sync KV: panel:brief ──┐
mode-sync KV: mode:desired ─┼─► GET /panel-data/v1/<slug> ─► MusePanelActivity
mode-sync KV: mode:actual ──┘        (fetch on open, cache last-good)
```

House mode needs **no producer**: the Worker reads its own mode KV
at request time, so mode freshness is exactly the phone's ack
freshness — no better source exists (the ST shadow at
`/st-webhook/mode-latest` is a future corroboration input, out of
scope for v4.7.0).

## 5. Data contract (v1)

```json
{
  "v": 1,
  "generated_at": "2026-10-28T12:05:11Z",
  "brief": {
    "date": "2026-10-28",
    "generated_at": "2026-10-28T11:45:02Z",
    "headline": "Two decisions waiting, dentist at 3",
    "highlights": [
      { "kind": "decide",   "title": "…", "detail": "…" },
      { "kind": "calendar", "title": "…", "detail": "…" }
    ],
    "decide_count": 2
  },
  "house": {
    "mode": "home",
    "desired_mode": "home",
    "in_sync": true,
    "reported_at": "2026-10-28T12:01:40Z",
    "source": "phone-ack"
  }
}
```

Rules:

- `generated_at` (top level) = when the Worker assembled this
  response. `brief.generated_at` = when the brief distillate was
  produced VM-side. `house.reported_at` = the phone's last ack time,
  passed through verbatim from `mode:actual` — **never** replaced by
  the Worker's receive time.
- `brief` is `null` until the first successful ingest; `house.mode`
  is `null` until the first ack. Nulls render as explicit empty
  states, not blanks.
- `highlights`: max 5; `kind` ∈ `decide | calendar | task | home |
  fyi`; `title` ≤ 80 chars, `detail` ≤ 140 chars, enforced
  server-side at ingest (truncate, don't reject). Content comes only
  from the already-redacted snapshot — the distiller adds no new
  data sources. No amounts, no medication names, no message bodies:
  the spoken-announcement sanitisation rules are the model.
- `house.mode` ∈ `home | away | night | morning` (the Worker's
  existing `MODE_VALUES`); anything else is dropped to `null` at
  serve time.
- The app ignores unknown fields; new fields are additive. Breaking
  changes bump `v` and the path (`/panel-data/v2/…`), never mutate
  v1 semantics.

## 6. Refresh model and failure behaviour (app side)

- **Fetch triggers:** panel `onResume`, throttled to one fetch per
  5 min of panel time, plus an explicit **Refresh** row (flat text
  button — the panel mixes no button styles, per spec). **No
  background polling in v4.7.0**: no WorkManager/AlarmManager lane
  exists in this no-SDK build, and panel-open freshness is the
  actual use case.
- **Transport:** `HttpURLConnection`, 8 s timeouts, background
  thread, `org.json` parse. Response > 32 KB or unparseable →
  treated as a failed fetch.
- **Cache:** last good response in app-private storage
  (`panel-cache.json`) with a client-side `fetched_at`. The cache is
  a failure fallback, never a freshness claim.
- **Rendering ages (the honesty rules):**
  - Brief header shows its date if it isn't today ("Tuesday's
    brief"), and its generated time as subtext ("Brief from 7:45 AM").
  - House line always carries its report age: "Home · reported
    4 min ago". Past 30 min it switches to the explicit stale form
    ("House mode last reported 2 h ago") in secondary text **with**
    the words — colour is never the sole cue (spec accessibility).
  - Fetch failure with cache present: data renders, footer line
    reads "Couldn't refresh · showing 9:12 AM" with the cached
    fetch time. Fetch failure with no cache: the card shows
    "Live data unavailable" and the rest of the panel is unaffected.
  - 404/403 (slug rotated or revoked): "Live data link expired —
    ask Muse for a fresh build." No retry loop, no silent fallback
    to any other endpoint.
  - Partial data is rendered partially: a null `brief` empties only
    the brief card; a null `house.mode` shows "House mode not
    reported yet" in the home card only.

## 7. Panel UI notes (One UI spec governs)

- **New "Muse today" focus block** directly under the date (the
  viewing area stays greeting + date; live content is the first
  interaction block): headline as the card's first body line,
  highlights as title + subtext rows in the panel's existing row
  pattern (title ≤ 1 line, ellipsised), `decide_count` as a bottom
  counter line when > 0 ("2 waiting on you").
- **House status joins the existing Home controls card** as its
  first row (icon + "Home" + age subtext), above the SmartThings /
  Google Home launch rows; the card's note line updates to reflect
  that the mode shown is the phone-reported one. No new card, no
  new screen.
- Existing tokens only: `bg_card` (18dp), 24dp margins, Roboto
  (system default), sentence case everywhere, primary `#0381FE`
  reserved for the Refresh affordance. Loading is an in-place
  progress indicator inside the card being refreshed — never a
  full-screen spinner (spec). All states reachable by sequential
  focus; every age/staleness conveyed in text, not colour alone.

## 8. Build task list for v4.7.0

### Server-side (VM + Worker)

1. **Worker handlers** (`cloudflare-workers/mode-sync/worker.js`):
   `panel-ingest` POST (secret-authed, validation + caps per §5) and
   the `/panel-data/v1/<slug>` GET (merge, `no-store`, slug 404
   behaviour). Extend `deploy.py`'s secret sources with `PANEL_SLUG`;
   deploy bindings-preserving and verify all pre-existing bindings
   survive (read settings back, per the deploy script's own rule).
2. **Cloudflare route**: add `jonathancruzate.com/panel-data*` →
   `mode-sync`; before adding, re-enumerate live routes against §2.3
   and probe each neighbour path after (the specificity lesson).
3. **Slug generation**: 128-bit random, stored as the Worker secret
   and in a VM 0600 file; rotation = replace both + rebuild the app.
4. **Brief producer**: `panel-brief-push` step in the Daily Brief
   refresh lane — read `dashboard_snapshot.json`, distill per §5
   (reuse the compile pipeline's redaction; add the panel caps),
   POST to `panel-ingest`. Runs on every brief refresh; a failed
   POST is logged and never blocks the brief itself.

### App-side (`~/workspace/metamuse-widget/`)

5. **`PanelDataClient`** (new class): fetch, timeout, parse,
   cache read/write; no state held beyond the cache file.
6. **`MusePanelActivity`**: "Muse today" card + house row +
   Refresh row + all §6 render states. No other activity changes.
7. **`build.sh`**: optional slug injection (local 0600 file →
   generated constants class; placeholder when absent) **and a
   no-secrets guard** beside the render guard: fail the build if
   source or compiled dex contains the MacroDroid webhook host,
   either Worker secret's value, or the string patterns of the
   phone-held secret files. The guard is the enforcement of
   constraint 1, not a comment.
8. **Verification**: badging/permissions unchanged (INTERNET already
   present; no new permissions, providers, or receivers); render
   guard still passes; provider/layout/widget-info hashes
   unchanged; slug absent from the GitHub-export tree (grep) and
   present only in the built APK's generated class.

### Explicitly not in v4.7.0

Background refresh, push of any kind, mode *control* from the panel
(the panel stays read-only; modes still run through Muse), ST
`mode-latest` corroboration, widget-surface changes of any kind.

## 9. Open questions for Jonathan (3, all concrete)

1. **Brief content on a capability-gated endpoint:** the distillate
   is pre-redacted and slug-gated, but it is fetchable by anyone
   holding the slug. Are headline-level brief items (decisions
   waiting, next commitments, house mode) acceptable at that
   exposure, or should `decide`/health-adjacent kinds be excluded
   from highlights entirely in v1?
2. **Distribution:** slug-bearing builds must not ship as public
   GitHub release assets (the slug is extractable from the binary).
   Sideload-only for v4.7.0+, with GitHub keeping source + notes
   only — acceptable?
3. **House line content:** show desired-vs-actual when they differ
   ("Away set · phone still reports Home"), or actual only with the
   age label? (Design currently shows both only when out of sync.)

## Appendix — rejected without a candidate slot

- **Polling `phone-ingress`** — secret-gated *and* a drain; the
  panel would consume StephAI's own notification stream.
- **Data via the Muse app** — TrustedCaller gate (§1.3); also the
  wrong direction (the app is an action surface, not a data API).
- **FCM / push** — requires the Firebase SDK; barred by constraint 2.
- **A second companion app as content provider** — a whole new APK
  to install and keep in sync, to solve a problem Candidate A solves
  with one GET.
