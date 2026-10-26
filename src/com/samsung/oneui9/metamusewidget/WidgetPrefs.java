package com.samsung.oneui9.metamusewidget;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/**
 * Widget settings, edited from the hub's Widget settings card:
 * the color theme (One UI spec palette vs the system palette from
 * Material You / Samsung color palette) and the search-bar widget's
 * right-hand shortcut (any catalog URI, a custom URI, or hidden).
 */
public final class WidgetPrefs {

    private static final String FILE = "widget_prefs";
    private static final String KEY_THEME = "theme_mode";
    private static final String KEY_SHORTCUT_URI = "shortcut_uri";
    private static final String KEY_SHORTCUT_LABEL = "shortcut_label";
    private static final String KEY_CUSTOM_HUE = "custom_hue";
    private static final String KEY_CUSTOM_SAT = "custom_sat";
    private static final String KEY_TRANSPARENCY = "transparency";

    public static final String THEME_ONEUI = "oneui";
    public static final String THEME_SYSTEM = "system";
    public static final String THEME_STEPHANY = "stephany";
    public static final String THEME_CUSTOM = "custom";
    public static final String DEFAULT_SHORTCUT_URI = "hatch://search";
    public static final String DEFAULT_SHORTCUT_LABEL = "Search";

    private WidgetPrefs() {}

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static String themeMode(Context ctx) {
        return prefs(ctx).getString(KEY_THEME, THEME_ONEUI);
    }

    public static boolean systemTheme(Context ctx) {
        return THEME_SYSTEM.equals(themeMode(ctx));
    }

    public static void setTheme(Context ctx, String mode) {
        prefs(ctx).edit().putString(KEY_THEME, mode).apply();
    }

    /** Custom-theme hue, 0-359 (default: One UI blue family). */
    public static int customHue(Context ctx) {
        return prefs(ctx).getInt(KEY_CUSTOM_HUE, 205);
    }

    public static void setCustomHue(Context ctx, int hue) {
        prefs(ctx).edit().putInt(KEY_CUSTOM_HUE, hue).apply();
    }

    /** Custom-theme saturation, 0-100. */
    public static int customSat(Context ctx) {
        return prefs(ctx).getInt(KEY_CUSTOM_SAT, 70);
    }

    public static void setCustomSat(Context ctx, int sat) {
        prefs(ctx).edit().putInt(KEY_CUSTOM_SAT, sat).apply();
    }

    /** Bar transparency, 0 (opaque) to 90, Google-widget style. */
    public static int transparency(Context ctx) {
        return prefs(ctx).getInt(KEY_TRANSPARENCY, 0);
    }

    public static void setTransparency(Context ctx, int value) {
        prefs(ctx).edit().putInt(KEY_TRANSPARENCY, value).apply();
    }

    // The bar's right-hand shortcut is an ACTION now, not a URI:
    // Muse gates every payload URI by caller signature (see
    // Router), so the working choices are the assist surface and a
    // plain launch. Stored URIs from earlier versions migrate:
    // assist-family URIs -> ASSIST, anything else -> LAUNCH.
    public static final String SHORTCUT_ASSIST = "assist";
    public static final String SHORTCUT_LAUNCH = "launch";
    public static final String SHORTCUT_NONE = "none";

    public static String shortcutAction(Context ctx) {
        if (shortcutHidden(ctx)) return SHORTCUT_NONE;
        String uri = prefs(ctx).getString(KEY_SHORTCUT_URI, DEFAULT_SHORTCUT_URI);
        if (Router.ASSIST_URI.equals(uri) || "widget://assist".equals(uri)
                || "hatch://thread/new".equals(uri) || "hatch://chat".equals(uri)
                || (uri != null && uri.startsWith("hatch://search"))) {
            return SHORTCUT_ASSIST;
        }
        return SHORTCUT_LAUNCH;
    }

    public static void setShortcutAction(Context ctx, String action) {
        if (SHORTCUT_NONE.equals(action)) {
            setShortcut(ctx, null, null);
        } else if (SHORTCUT_ASSIST.equals(action)) {
            setShortcut(ctx, "widget://assist", "Assistant");
        } else {
            setShortcut(ctx, "widget://launch", "Open app");
        }
    }

    public static String shortcutActionLabel(Context ctx) {
        String a = shortcutAction(ctx);
        if (SHORTCUT_ASSIST.equals(a)) return "Assistant";
        if (SHORTCUT_LAUNCH.equals(a)) return "Open app";
        return "Hidden";
    }

    /** The shortcut target URI, or null when the button is hidden. */
    public static String shortcutUri(Context ctx) {
        if (shortcutHidden(ctx)) return null;
        return prefs(ctx).getString(KEY_SHORTCUT_URI, DEFAULT_SHORTCUT_URI);
    }

    public static String shortcutLabel(Context ctx) {
        return prefs(ctx).getString(KEY_SHORTCUT_LABEL, DEFAULT_SHORTCUT_LABEL);
    }

    /** uri == null hides the button. */
    public static void setShortcut(Context ctx, String uri, String label) {
        SharedPreferences.Editor ed = prefs(ctx).edit();
        if (uri == null) {
            ed.remove(KEY_SHORTCUT_URI).remove(KEY_SHORTCUT_LABEL);
            ed.putString(KEY_SHORTCUT_URI + "_hidden", "1");
        } else {
            ed.putString(KEY_SHORTCUT_URI, uri);
            ed.putString(KEY_SHORTCUT_LABEL, label != null ? label : uri);
            ed.remove(KEY_SHORTCUT_URI + "_hidden");
        }
        ed.apply();
    }

    public static boolean shortcutHidden(Context ctx) {
        return prefs(ctx).contains(KEY_SHORTCUT_URI + "_hidden");
    }

    /** Re-render every placed widget after a settings change. */
    public static void refreshWidgets(Context ctx) {
        AppWidgetManager manager = AppWidgetManager.getInstance(ctx);
        Class<?>[] providers = {SearchBarWidgetProvider.class, GridWidgetProvider.class};
        for (Class<?> provider : providers) {
            int[] ids = manager.getAppWidgetIds(new ComponentName(ctx, provider));
            if (ids == null || ids.length == 0) continue;
            Intent update = new Intent(ctx, provider);
            update.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            update.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
            ctx.sendBroadcast(update);
        }
    }
}
