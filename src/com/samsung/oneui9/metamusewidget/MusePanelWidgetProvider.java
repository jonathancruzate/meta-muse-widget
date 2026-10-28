package com.samsung.oneui9.metamusewidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.SizeF;
import android.view.View;
import android.widget.RemoteViews;

import java.util.HashMap;
import java.util.Map;

/**
 * The Muse panel as a home-screen widget (v4.8.0) — Jonathan's
 * ask: the panel on the home screen itself, expanding dynamically
 * up to full screen, styled One UI 9 blended with Material You.
 *
 * A NEW provider beside the frozen search-bar widget (that
 * provider, its layout, and its widget-info stay byte-identical;
 * nothing here touches them). This widget renders the SAME live
 * data as MusePanelActivity — brief headline + highlights with
 * the v1 exclusions already applied upstream, the decide count
 * as a number only, the house mode with its report age — through
 * {@link PanelDataClient}'s shared fetch+cache path:
 *
 * <ul>
 *   <li>Every update renders the last-good cache IMMEDIATELY,
 *       then refreshes in the background via {@code goAsync} and
 *       re-renders. The widget never blocks on the network.</li>
 *   <li>Refresh lanes: the 30-minute {@code updatePeriodMillis}
 *       floor, the in-widget Refresh affordance (a broadcast back
 *       to this provider), and {@link #poke} from
 *       MusePanelActivity after its own fetches.</li>
 *   <li>Size-adaptive: on API 31+ the three density layouts ship
 *       as one responsive RemoteViews (SizeF breakpoints); below
 *       31 the density is picked from the widget options, and
 *       {@code onAppWidgetOptionsChanged} re-picks on resize.</li>
 *   <li>Styling: One UI 9 focus-block cards (18dp) with the app
 *       palette by default; on API 31+ the card fill comes from
 *       the Material You system neutral tones (drawable-v31
 *       variants) and the accent tint resolves from the framework
 *       dynamic accent — the device palette, the way Material You
 *       widgets follow it. Below 31 the app palette is the
 *       fallback.</li>
 * </ul>
 *
 * Layout law (the render-guard rule): these layouts contain ONLY
 * RemoteViews-supported view classes — no plain View, ever.
 * Tapping anywhere opens MusePanelActivity; Up next appears
 * only when the calendar permission is already granted (a widget
 * cannot ask, so it degrades silently without it).
 */
public class MusePanelWidgetProvider extends AppWidgetProvider {

    /** The Refresh affordance's broadcast action. */
    public static final String ACTION_REFRESH =
            "com.samsung.oneui9.metamusewidget.action.PANEL_WIDGET_REFRESH";

    private static final int DENSITY_COMPACT = 0;
    private static final int DENSITY_MEDIUM = 1;
    private static final int DENSITY_FULL = 2;

    // SizeF breakpoints for the responsive (API 31+) RemoteViews:
    // the host picks the largest breakpoint that fits the widget.
    private static final SizeF BP_MEDIUM = new SizeF(170, 120);
    private static final SizeF BP_FULL = new SizeF(250, 240);

    private static final int[] ROW_IDS = {
            R.id.row_hl1, R.id.row_hl2, R.id.row_hl3, R.id.row_hl4, R.id.row_hl5};
    private static final int[] TITLE_IDS = {
            R.id.tv_hl1_title, R.id.tv_hl2_title, R.id.tv_hl3_title,
            R.id.tv_hl4_title, R.id.tv_hl5_title};
    private static final int[] DETAIL_IDS = {
            R.id.tv_hl1_detail, R.id.tv_hl2_detail, R.id.tv_hl3_detail,
            R.id.tv_hl4_detail, R.id.tv_hl5_detail};

    /**
     * Ask for a widget refresh after the panel activity fetched
     * fresh data (the fetch already refreshed the shared cache).
     * Rides the standard APPWIDGET_UPDATE broadcast so the render
     * + background-fetch path stays the single one.
     */
    public static void poke(Context ctx) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
        int[] ids = mgr.getAppWidgetIds(
                new ComponentName(ctx, MusePanelWidgetProvider.class));
        if (ids.length == 0) return;
        Intent intent = new Intent(ctx, MusePanelWidgetProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        ctx.sendBroadcast(intent);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_REFRESH.equals(intent.getAction())) {
            AppWidgetManager mgr = AppWidgetManager.getInstance(context);
            int[] ids = mgr.getAppWidgetIds(
                    new ComponentName(context, MusePanelWidgetProvider.class));
            if (ids.length > 0) onUpdate(context, mgr, ids);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            render(context, manager, id, cachedResult(context));
        }
        refreshAsync(context, manager, appWidgetIds);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager manager,
                                          int appWidgetId, Bundle newOptions) {
        // A resize may cross a density breakpoint: re-render this
        // widget from cache at its new size right away (on API 31+
        // the responsive map re-picks by itself; below 31 this is
        // the density switch), then refresh in the background.
        render(context, manager, appWidgetId, cachedResult(context));
        refreshAsync(context, manager, new int[]{appWidgetId});
    }

    /** The last-good cache as a render input, or the bare state lines. */
    private PanelDataClient.Result cachedResult(Context ctx) {
        PanelDataClient.Result cached = PanelDataClient.loadCache(ctx);
        if (cached != null) return cached;
        PanelDataClient.Result r = new PanelDataClient.Result();
        r.status = PanelDataClient.isConfigured()
                ? PanelDataClient.FAILED : PanelDataClient.NOT_CONFIGURED;
        return r;
    }

    /**
     * Background refresh via goAsync: fetch through the shared
     * client (which also updates the cache), then re-render each
     * widget with the attempt's own result — so a rotated link or
     * a failure shows its state instead of silently aging.
     */
    private void refreshAsync(final Context context, final AppWidgetManager manager,
                              final int[] appWidgetIds) {
        if (!PanelDataClient.isConfigured() || appWidgetIds.length == 0) return;
        final PendingResult pending = goAsync();
        final Context appCtx = context.getApplicationContext();
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    PanelDataClient.Result r = PanelDataClient.fetch(appCtx);
                    for (int id : appWidgetIds) {
                        render(appCtx, manager, id, r);
                    }
                } finally {
                    pending.finish();
                }
            }
        }).start();
    }

    // ---- Rendering ----

    private void render(Context ctx, AppWidgetManager manager, int id,
                        PanelDataClient.Result result) {
        PanelWidgetData.Model model = PanelWidgetData.fromResult(ctx, result);
        RemoteViews views;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Map<SizeF, RemoteViews> bySize = new HashMap<>();
            bySize.put(new SizeF(0, 0), buildViews(ctx, DENSITY_COMPACT, model, id));
            bySize.put(BP_MEDIUM, buildViews(ctx, DENSITY_MEDIUM, model, id));
            bySize.put(BP_FULL, buildViews(ctx, DENSITY_FULL, model, id));
            views = new RemoteViews(bySize);
        } else {
            views = buildViews(ctx, densityForOptions(manager, id), model, id);
        }
        manager.updateAppWidget(id, views);
    }

    /** Pre-31 density pick from the widget's current options. */
    private int densityForOptions(AppWidgetManager manager, int id) {
        Bundle opts = manager.getAppWidgetOptions(id);
        int w = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0);
        if (w <= 0) w = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0);
        int h = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0);
        if (h <= 0) h = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0);
        if (w >= BP_FULL.getWidth() && h >= BP_FULL.getHeight()) return DENSITY_FULL;
        if (h >= BP_MEDIUM.getHeight()) return DENSITY_MEDIUM;
        return DENSITY_COMPACT;
    }

    private RemoteViews buildViews(Context ctx, int density,
                                   PanelWidgetData.Model model, int id) {
        int layout = density == DENSITY_FULL ? R.layout.widget_panel_full
                : density == DENSITY_MEDIUM ? R.layout.widget_panel_medium
                : R.layout.widget_panel_compact;
        RemoteViews views = new RemoteViews(ctx.getPackageName(), layout);
        int accent = accentColor(ctx);

        // The whole widget opens the panel.
        Intent open = new Intent(ctx, MusePanelActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        views.setOnClickPendingIntent(R.id.panel_root,
                PendingIntent.getActivity(ctx, 4000 + id, open,
                        PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE));

        views.setTextViewText(R.id.tv_headline, model.headline);

        if (density == DENSITY_COMPACT) {
            views.setTextViewText(R.id.tv_house, houseOneLine(model));
            return views;
        }

        // Medium + full share the brief block.
        setTextOrGone(views, R.id.tv_status, model.statusLine);
        int cap = density == DENSITY_FULL ? 5 : 3;
        for (int i = 0; i < cap; i++) {
            if (i < model.highlights.size()) {
                String[] h = model.highlights.get(i);
                views.setViewVisibility(ROW_IDS[i], View.VISIBLE);
                views.setTextViewText(TITLE_IDS[i], h[0]);
                setTextOrGone(views, DETAIL_IDS[i], h[1]);
            } else {
                views.setViewVisibility(ROW_IDS[i], View.GONE);
            }
        }
        setTextOrGone(views, R.id.tv_decide, model.decideText);

        // The Refresh affordance: a broadcast back to this provider.
        Intent refresh = new Intent(ctx, MusePanelWidgetProvider.class);
        refresh.setAction(ACTION_REFRESH);
        views.setOnClickPendingIntent(R.id.tv_refresh,
                PendingIntent.getBroadcast(ctx, 5000 + id, refresh,
                        PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE));
        views.setTextColor(R.id.tv_refresh, accent);

        if (density == DENSITY_MEDIUM) {
            views.setTextViewText(R.id.tv_house, houseOneLine(model));
            return views;
        }

        // Full: greeting + date header, house block, Up next.
        views.setTextViewText(R.id.tv_greeting, PanelWidgetData.greetingText(ctx));
        views.setTextViewText(R.id.tv_date, PanelWidgetData.dateText());
        views.setInt(R.id.iv_logo, "setColorFilter", accent);
        views.setTextViewText(R.id.tv_house_title, model.houseTitle);
        setTextOrGone(views, R.id.tv_house_sub, model.houseSub);
        if (!PanelWidgetData.hasCalendarPermission(ctx)) {
            // A widget cannot ask for the permission; the section
            // degrades silently (the panel owns the ask).
            views.setViewVisibility(R.id.block_upnext, View.GONE);
        } else {
            views.setViewVisibility(R.id.block_upnext, View.VISIBLE);
            String[] event = PanelWidgetData.queryNextEvent(ctx);
            if (event == null) {
                views.setTextViewText(R.id.tv_upnext_title,
                        ctx.getString(R.string.nothing_upcoming));
                views.setViewVisibility(R.id.tv_upnext_sub, View.GONE);
            } else {
                views.setTextViewText(R.id.tv_upnext_title, event[0]);
                String sub = event[1];
                if (event[2] != null && !event[2].isEmpty()) {
                    sub = sub + " · " + event[2];
                }
                setTextOrGone(views, R.id.tv_upnext_sub, sub);
            }
        }
        return views;
    }

    /** Compact/medium house slot: title + age on one ellipsized line. */
    private String houseOneLine(PanelWidgetData.Model model) {
        if (model.houseSub == null || model.houseSub.isEmpty()) return model.houseTitle;
        return model.houseTitle + " · " + model.houseSub;
    }

    private void setTextOrGone(RemoteViews views, int viewId, String text) {
        if (text == null || text.isEmpty()) {
            views.setViewVisibility(viewId, View.GONE);
        } else {
            views.setViewVisibility(viewId, View.VISIBLE);
            views.setTextViewText(viewId, text);
        }
    }

    /**
     * The accent for the refresh affordance and the logo tint:
     * the framework's Material You dynamic accent on API 31+
     * (resolved by name so the lookup itself is API-guarded),
     * the app's spec primary below 31.
     */
    private int accentColor(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                int resId = ctx.getResources().getIdentifier(
                        "system_accent1_600", "color", "android");
                if (resId != 0) return ctx.getColor(resId);
            } catch (Exception ignored) {
                // fall through to the app palette
            }
        }
        return ctx.getColor(R.color.primary);
    }
}
