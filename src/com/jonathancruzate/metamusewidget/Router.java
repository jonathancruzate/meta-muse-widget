package com.jonathancruzate.metamusewidget;

import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.widget.Toast;

/**
 * How this app talks to Muse from Meta — rebuilt on REA evidence
 * (Muse APK 13.0.0.4.159 decompiled 2026-10-26; full record in
 * BUILD-REPORT.md).
 *
 * Why every URI failed: BOTH deeplink entry points re-run Meta's
 * TrustedCaller check (X.CRT) on the sending app. The deeplink
 * handler validates and logs "untrusted caller, rejecting
 * deeplink"; the main activity re-checks any intent carrying data
 * and silently clears the data on failure. TrustedCaller verifies
 * the caller's package AND its signing certificate against Meta's
 * own code-signing root CA — a third-party app can never pass, so
 * hatch:// navigation from outside Meta's family is a dead end by
 * cryptographic design. The handler's public_relay parameter is
 * the designed degradation: it opens the app WITHOUT the payload.
 *
 * What does work (no data URI = no caller gate):
 *   - ACTION_ASSIST into AuraMainActivity: Muse's
 *     HatchIntentViewModel routes it to the main chat with the
 *     composer primed; the extra
 *     com.facebook.aura.extra.ASSIST_QUERY prefills the composer.
 *     This is the surface built for third-party assistants.
 *   - A plain app launch.
 * The catalog (UriCatalog) holds only destinations that work
 * through those surfaces; the gated URI destinations whose only
 * behavior was copying the URI were removed in v4.12.1.
 */
public final class Router {

    public static final String MUSE_PACKAGE = "com.facebook.aura";
    public static final String MUSE_MAIN_ACTIVITY = "com.facebook.aura.main.AuraMainActivity";
    public static final String EXTRA_ASSIST_QUERY = "com.facebook.aura.extra.ASSIST_QUERY";
    private static final String ACTION_AURA_ASSIST = "com.facebook.aura.action.ASSIST";

    /** Pseudo-URI for the Muse assistant surface (ACTION_ASSIST on the main activity). */
    public static final String ASSIST_URI = "muse://assist";

    /** The Muse Voice Mode Shortcut app and its no-UI toggle activity. */
    public static final String VOICE_MODE_PACKAGE = "com.jonathancruzate.metamusevoicemodeshortcut";
    public static final String VOICE_MODE_ACTIVITY =
            "com.jonathancruzate.metamusevoicemodeshortcut.ShortcutActivity";

    /** Fallback packages, inherited from the original widget (v3.0.0). */
    static final String[] TARGET_PACKAGES = {
            "com.facebook.aura",
            "com.facebook.stella",
            "com.meta.ai",
            "com.meta.muse",
    };

    public static final String EXTRA_URI = "extra_uri";
    public static final String EXTRA_MODE = "extra_mode";
    public static final String EXTRA_QUERY = "extra_query";

    private Router() {}

    public static Intent routerIntent(Context ctx, UriCatalog.Entry e) {
        Intent i = new Intent(ctx, LaunchMuseActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        // A unique data URI per destination: PendingIntents are matched
        // ignoring extras, so without this, widget buttons that share a
        // request code would silently merge into one another.
        String key = e == null ? "app"
                : Integer.toHexString((e.label + "|" + e.mode + "|" + e.uri).hashCode());
        i.setData(Uri.parse("widget://route/" + key));
        if (e != null) {
            i.putExtra(EXTRA_MODE, e.mode);
            if (e.uri != null) i.putExtra(EXTRA_URI, e.uri);
        }
        return i;
    }

    public static PendingIntent routerPendingIntent(Context ctx, UriCatalog.Entry e, int requestCode) {
        return PendingIntent.getActivity(ctx, requestCode, routerIntent(ctx, e),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** The trampoline's dispatch, by catalog mode. */
    public static void dispatch(Context ctx, int mode, String uri, String query) {
        switch (mode) {
            case UriCatalog.MODE_ASSIST:
                if (assist(ctx, query)) return;
                break;
            case UriCatalog.MODE_VOICE_MODE:
                if (voiceMode(ctx)) return;
                Toast.makeText(ctx, R.string.voice_shortcut_missing, Toast.LENGTH_LONG).show();
                if (assist(ctx, null)) return;
                break;
            case UriCatalog.MODE_HUB:
                ctx.startActivity(new Intent(ctx, WidgetCustomizeActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                return;
            case UriCatalog.MODE_CUSTOMIZE:
                ctx.startActivity(new Intent(ctx, WidgetCustomizeActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                return;
            case UriCatalog.MODE_COMPOSE:
                ctx.startActivity(new Intent(ctx, ComposeActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                return;
            default:
                break;
        }
        if (launchApp(ctx)) return;
        if (tryView(ctx, "market://details?id=" + MUSE_PACKAGE)) return;
        Toast.makeText(ctx, R.string.not_found, Toast.LENGTH_LONG).show();
    }

    /**
     * Voice mode via the Muse Voice Mode Shortcut app (v4.12.0):
     * Muse's hatch://voice deeplink is TrustedCaller-gated, so the
     * widget hands off to the companion app whose accessibility
     * service taps Muse's own voice button. Its ShortcutActivity
     * is a no-UI toggle — each fire flips voice mode on or off.
     * False when that app is not installed.
     */
    public static boolean voiceMode(Context ctx) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setComponent(new ComponentName(VOICE_MODE_PACKAGE, VOICE_MODE_ACTIVITY));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
            return true;
        } catch (ActivityNotFoundException | SecurityException e) {
            return false;
        }
    }

    /**
     * The ungated surface: ACTION_ASSIST into Muse's main activity.
     * A non-empty query lands in the composer via ASSIST_QUERY.
     */
    public static boolean assist(Context ctx, String query) {
        if (tryAssistAction(ctx, Intent.ACTION_ASSIST, query)) return true;
        return tryAssistAction(ctx, ACTION_AURA_ASSIST, query);
    }

    private static boolean tryAssistAction(Context ctx, String action, String query) {
        try {
            Intent i = new Intent(action);
            i.setComponent(new ComponentName(MUSE_PACKAGE, MUSE_MAIN_ACTIVITY));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            if (query != null && !query.isEmpty()) i.putExtra(EXTRA_ASSIST_QUERY, query);
            ctx.startActivity(i);
            return true;
        } catch (ActivityNotFoundException | SecurityException e) {
            return false;
        }
    }

    private static boolean tryView(Context ctx, String uri) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
            return true;
        } catch (ActivityNotFoundException | SecurityException e) {
            return false;
        }
    }

    public static boolean launchApp(Context ctx) {
        PackageManager pm = ctx.getPackageManager();
        for (String pkg : TARGET_PACKAGES) {
            try {
                Intent i = pm.getLaunchIntentForPackage(pkg);
                if (i != null) {
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    ctx.startActivity(i);
                    return true;
                }
            } catch (Exception ignored) {
            }
        }
        return false;
    }
}
