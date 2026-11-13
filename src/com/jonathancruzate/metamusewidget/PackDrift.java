package com.jonathancruzate.metamusewidget;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;

import java.util.HashSet;
import java.util.Set;

/**
 * Theme-spec drift check (design Phase 3): compares the placed
 * Muse widgets against the Muse Theme layout specification's
 * verifiable minimum (widget placement is all a widget app can
 * observe; page assignment lives in One UI Home and cannot be
 * read). Counts only, no layout contents ever leave the device.
 */
final class PackDrift {

    private PackDrift() {}

    static int[] placedCounts(Context ctx) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
        return new int[]{
                mgr.getAppWidgetIds(new ComponentName(ctx,
                        MuseOmnibarWidgetProvider.class)).length,
                mgr.getAppWidgetIds(new ComponentName(ctx,
                        MuseTileWidgetProvider.class)).length,
                mgr.getAppWidgetIds(new ComponentName(ctx,
                        MuseStackWidgetProvider.class)).length,
                mgr.getAppWidgetIds(new ComponentName(ctx,
                        MuseGridWidgetProvider.class)).length,
                mgr.getAppWidgetIds(new ComponentName(ctx,
                        MuseHouseWidgetProvider.class)).length,
        };
    }

    static Set<String> placedTileKinds(Context ctx) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
        int[] ids = mgr.getAppWidgetIds(
                new ComponentName(ctx, MuseTileWidgetProvider.class));
        Set<String> kinds = new HashSet<>();
        for (int id : ids) {
            kinds.add(PackPrefs.kindFor(ctx, id, "brief"));
        }
        return kinds;
    }

    /** The spec minimum: one omnibar, the four core tile kinds, one stack card set, one grid. */
    static boolean layoutMatchesSpec(Context ctx) {
        int[] counts = placedCounts(ctx);
        if (counts[0] < 1 || counts[2] < 1 || counts[3] < 1
                || counts[4] < 1) return false;
        Set<String> kinds = placedTileKinds(ctx);
        return kinds.contains("brief") && kinds.contains("up_next")
                && kinds.contains("house") && kinds.contains("clock");
    }

    static String summary(Context ctx) {
        int[] c = placedCounts(ctx);
        return "Omnibar " + c[0] + " · Tiles " + c[1]
                + " · Stack " + c[2] + " · Grid " + c[3]
                + " · House " + c[4]
                + " · kinds " + placedTileKinds(ctx);
    }
}
