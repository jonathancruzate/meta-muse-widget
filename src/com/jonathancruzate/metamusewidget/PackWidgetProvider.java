package com.jonathancruzate.metamusewidget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

/**
 * Shared base for the Muse Launcher Pack providers (v5.0.0),
 * following the Muse panel provider's discipline: render the
 * last-good cache immediately on every update, then refresh in
 * the background (throttled to one fetch per 10 minutes across
 * the pack) and re-render. The pack runs no foreground service;
 * freshness comes from the system widget cycle, the in-tile
 * Refresh affordance, Device Assistance context pokes, and this
 * fetch chain.
 */
public abstract class PackWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_REFRESH =
            "com.jonathancruzate.metamusewidget.action.PACK_REFRESH";

    private static volatile long lastFetchMs = 0L;
    private static final long FETCH_THROTTLE_MS = 10L * 60L * 1000L;

    /** Render one instance from cached state. Never blocks. */
    protected abstract RemoteViews render(Context ctx, int widgetId);

    /** This provider's class, for pokes and refresh intents. */
    protected abstract Class<?> providerClass();

    @Override
    public void onUpdate(Context context, AppWidgetManager mgr,
                         int[] ids) {
        for (int id : ids) {
            mgr.updateAppWidget(id, render(context, id));
        }
        maybeFetch(context, ids);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_REFRESH.equals(intent.getAction())) {
            AppWidgetManager mgr = AppWidgetManager.getInstance(context);
            int[] ids = mgr.getAppWidgetIds(
                    new ComponentName(context, providerClass()));
            if (ids.length > 0) {
                lastFetchMs = 0L; // explicit refresh bypasses throttle
                onUpdate(context, mgr, ids);
            }
        }
    }

    private void maybeFetch(final Context context, final int[] ids) {
        long now = System.currentTimeMillis();
        if (now - lastFetchMs < FETCH_THROTTLE_MS) return;
        lastFetchMs = now;
        final PendingResult pending = goAsync();
        final Context app = context.getApplicationContext();
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    PanelDataClient.fetch(app);
                    LiveUpdateFeeder.syncFromFeed(app);
                    if (SleepReader.refresh(app)) {
                        AppWidgetManager mgr2 =
                                AppWidgetManager.getInstance(app);
                        int[] tileIds = mgr2.getAppWidgetIds(
                                new android.content.ComponentName(app,
                                        MuseTileWidgetProvider.class));
                        for (int id : tileIds) {
                            mgr2.updateAppWidget(id, render(app, id));
                        }
                    }
                    AppWidgetManager mgr =
                            AppWidgetManager.getInstance(app);
                    for (int id : ids) {
                        mgr.updateAppWidget(id, render(app, id));
                    }
                } finally {
                    pending.finish();
                }
            }
        });
        t.start();
    }

    /** Re-render every placed instance of one pack provider. */
    public static void poke(Context ctx, Class<?> provider) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
        int[] ids = mgr.getAppWidgetIds(new ComponentName(ctx, provider));
        if (ids.length == 0) return;
        Intent intent = new Intent(ctx, provider);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        ctx.sendBroadcast(intent);
    }

    /** Re-render the whole pack (context changes, pairing, setup). */
    public static void pokeAll(Context ctx) {
        poke(ctx, MuseOmnibarWidgetProvider.class);
        poke(ctx, MuseTileWidgetProvider.class);
        poke(ctx, MuseStackWidgetProvider.class);
        poke(ctx, MuseGridWidgetProvider.class);
        poke(ctx, MuseHouseWidgetProvider.class);
    }
}
