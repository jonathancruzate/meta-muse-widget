package com.jonathancruzate.metamusewidget;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

/**
 * Client for the Device Assistance local channel (see
 * {@link DaContract}). Every method is a synchronous
 * ContentProvider call and BLOCKS: call off the UI thread (widget
 * providers call from their fetch thread; activities from a
 * worker thread). Failures return a Bundle with ok=false and a
 * plain-language error; nothing here throws.
 */
final class DaClient {

    private DaClient() {}

    static boolean daInstalled(Context ctx) {
        try {
            ctx.getPackageManager().getPackageInfo(DaContract.DA_PACKAGE, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private static Bundle call(Context ctx, String method, Bundle in) {
        try {
            Bundle out = ctx.getContentResolver().call(
                    Uri.parse(DaContract.CHANNEL_URI), method, null, in);
            return out == null ? new Bundle() : out;
        } catch (Exception e) {
            Bundle out = new Bundle();
            out.putBoolean(DaContract.KEY_OK, false);
            out.putString(DaContract.KEY_ERROR,
                    e.getClass().getSimpleName());
            return out;
        }
    }

    /**
     * Claim a pairing token inside the window Device Assistance
     * opened. On success the token is stored in PackPrefs and
     * true is returned.
     */
    static boolean claimPairing(Context ctx) {
        Bundle out = call(ctx, DaContract.METHOD_CLAIM_PAIRING,
                new Bundle());
        String token = out.getString(DaContract.KEY_TOKEN, "");
        if (token != null && !token.isEmpty()) {
            PackPrefs.setDaToken(ctx, token);
            return true;
        }
        return false;
    }

    /** Ask Device Assistance whether our stored token still pairs. */
    static boolean pairStatus(Context ctx) {
        if (!PackPrefs.daPaired(ctx)) return false;
        Bundle in = new Bundle();
        in.putString(DaContract.KEY_TOKEN, PackPrefs.daToken(ctx));
        Bundle out = call(ctx, DaContract.METHOD_PAIR_STATUS, in);
        boolean paired = out.getBoolean(DaContract.KEY_PAIRED, false);
        if (!paired) PackPrefs.clearDaToken(ctx);
        return paired;
    }

    /** Revoke from this side: Device Assistance drops the token. */
    static void revoke(Context ctx) {
        Bundle in = new Bundle();
        in.putString(DaContract.KEY_TOKEN, PackPrefs.daToken(ctx));
        call(ctx, DaContract.METHOD_REVOKE, in);
        PackPrefs.clearDaToken(ctx);
    }

    /**
     * Execute one allowlisted Device Assistance template and
     * return its acknowledgement Bundle (ok / output / error /
     * ms). Unpaired calls fail locally with a plain reason.
     */
    static Bundle execute(Context ctx, String template, String... args) {
        if (!PackPrefs.daPaired(ctx)) {
            Bundle out = new Bundle();
            out.putBoolean(DaContract.KEY_OK, false);
            out.putString(DaContract.KEY_ERROR, "not paired");
            return out;
        }
        Bundle in = new Bundle();
        in.putString(DaContract.KEY_TOKEN, PackPrefs.daToken(ctx));
        in.putString(DaContract.KEY_TEMPLATE, template);
        in.putStringArray(DaContract.KEY_ARGS, args);
        return call(ctx, DaContract.METHOD_EXECUTE, in);
    }
}
