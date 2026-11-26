# Meta Muse Widget v5.3.3 (versionCode 48), 2026-11-25

Pairing error surfacing for the Device Assistance local channel.
Built with `build.sh`; render guard and no-secrets guard passed;
signer is the ONEMUSE cert, SHA-256
`2746d2a2d6dbe0fda4271acd14f4e5ab990da4e9155e7f9e75ae07fca4b814da`
(unchanged, and matching Device Assistance's `PACK_CERT_SHA256`).

APK: `Meta-Muse-Widget-5.3.3.apk` at the project root, 552,494
bytes, SHA-256
`7b803d3ad872b95fe0061e7dc6a68c1878ecfaffef73c69dd474df9c38b738ce`.
Not installed in this handling (the ADB lane was down); install is
handled separately.

## What changed

- `DaClient`: new `claimPairingWithReason` returns null on success
  (token stored in PackPrefs, exactly as before) and the failure
  reason string on failure: the Device Assistance error string
  from the result Bundle, or the caught exception's simple name
  (which the internal call wrapper records in the Bundle).
  `claimPairing` keeps its boolean contract on top of it.
- `SetupWizardActivity`: the pairing failure toast now shows the
  specific reason, in the form
  "Pairing did not complete: no pairing window open".
- `res/values/strings.xml`: the unpaired instruction string
  `pack_wizard_da_unpaired` now names Device Assistance's real UI:
  'Installed, not paired. In Device Assistance, scroll to "Muse
  Launcher Pack — local channel" and tap "Open pairing window
  (5 min)", then tap Complete pairing here within 5 minutes.'

Companion change: Muse Device Assistance v1.37.0 adds the
relay-driven `local_pair_open` verb plus pairing refusal
bookkeeping in its report snapshot (see that project's
`docs/v1.37-local-pairing-fix-2026-11-25.md`).
