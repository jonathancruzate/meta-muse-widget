package com.jonathancruzate.metamusewidget;

/**
 * The local channel contract between the Muse Launcher Pack and
 * Muse Device Assistance (design Section 7.2, as amended at
 * execution 2026-11-12). Transport is a ContentProvider call() on
 * Device Assistance's side: synchronous (the return Bundle is the
 * acknowledgement tiles re-render from), and authenticated there
 * by calling package plus signing-certificate fingerprint, with
 * the pairing token as a second, revocable factor. No secret
 * lives in this file: the token is generated on the phone at
 * pairing time and stored only in private preferences.
 */
final class DaContract {

    static final String DA_PACKAGE =
            "com.jonathancruzate.metamusedeviceassistance";
    static final String AUTHORITY = DA_PACKAGE + ".localchannel";
    static final String CHANNEL_URI = "content://" + AUTHORITY + "/channel";

    static final String METHOD_EXECUTE = "execute";
    static final String METHOD_CLAIM_PAIRING = "claim_pairing";
    static final String METHOD_PAIR_STATUS = "pair_status";
    static final String METHOD_REVOKE = "revoke";

    static final String KEY_TEMPLATE = "template";
    static final String KEY_ARGS = "args";
    static final String KEY_TOKEN = "token";
    static final String KEY_OK = "ok";
    static final String KEY_OUTPUT = "output";
    static final String KEY_ERROR = "error";
    static final String KEY_MS = "ms";
    static final String KEY_PAIRED = "paired";

    // Context broadcasts, Device Assistance -> pack (Section 7.3).
    // A spoofed poke can only trigger a re-render from cached data,
    // never an action; actions ride the authenticated call() lane.
    static final String ACTION_CONTEXT_MODE =
            DA_PACKAGE + ".action.CONTEXT_MODE";
    static final String ACTION_CONTEXT_MEDIA =
            DA_PACKAGE + ".action.CONTEXT_MEDIA";
    static final String EXTRA_MODE = "mode";
    static final String EXTRA_TEXT = "text";

    private DaContract() {}
}
