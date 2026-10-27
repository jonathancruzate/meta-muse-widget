package com.samsung.oneui9.metamusewidget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;

/**
 * The 4x1 search-bar widget, modeled on the One UI 9 Finder bar:
 * a matte, borderless pill; a plain glyph on the left; hint text;
 * one plain action glyph on the right. The pill itself is rendered
 * as a bitmap by BarStyle, so the Customize settings (theme presets,
 * Custom hue/saturation, transparency) apply exactly. Tapping the
 * bar opens a new Muse thread, the compose surface, so the keyboard
 * comes up ready to type. The right glyph is the customizable
 * shortcut (its icon mutates to match the destination) and can be
 * hidden entirely. Tapping the Muse logo on the left just opens
 * the Muse app.
 */
public class SearchBarWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            // Per-widget settings (theme, transparency, shortcut),
            // each falling back to the global value when this
            // widget was never individually customized.
            int accent = BarStyle.accent(context, id);
            String action = WidgetPrefs.shortcutAction(context, id);
            UriCatalog.Entry shortcut = null;
            if (WidgetPrefs.SHORTCUT_ASSIST.equals(action)) {
                shortcut = new UriCatalog.Entry("Widget", "Assistant", Router.ASSIST_URI,
                        null, null, R.drawable.ic_assistant, false);
            } else if (WidgetPrefs.SHORTCUT_LAUNCH.equals(action)) {
                shortcut = new UriCatalog.Entry("Widget", "Open app", null,
                        null, null, R.drawable.ic_open, false);
            }
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_search_bar);
            Bundle opts = manager.getAppWidgetOptions(id);
            int wDp = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0);
            if (wDp <= 0) wDp = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 300);
            // The pill is Samsung's bar: a fixed 52dp tall, centered in
            // whatever row the launcher gives the widget. A launcher
            // search bar is not a widget — it lives in the launcher's
            // own slimmer container — so filling the whole widget row
            // (what this used to do) reads far too thick next to it.
            // The root FrameLayout pads 2dp on every side.
            views.setImageViewBitmap(R.id.iv_pill_bg,
                    BarStyle.pillBitmap(context, wDp - 4, 52, id));
            // In the Pixel bar style the logo sits untinted, the
            // way Pixel's multicolor G does; the One UI style keeps
            // the theme tint. (Fresh RemoteViews: skipping the call
            // leaves the glyph in its native colors.)
            if (!BarStyle.isPixel(context, id)) {
                views.setInt(R.id.iv_muse_logo, "setColorFilter", accent);
            }
            views.setTextColor(R.id.tv_search_query, BarStyle.hintColor(context, id));
            // Tapping the bar opens this app's ask page (type with
            // suggestions; Enter lands the text in Muse's composer).
            android.content.Intent compose = new android.content.Intent(
                    context, ComposeActivity.class);
            compose.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                    | android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP);
            views.setOnClickPendingIntent(R.id.container_search_bar,
                    android.app.PendingIntent.getActivity(context, 1000 + id, compose,
                            android.app.PendingIntent.FLAG_UPDATE_CURRENT
                                    | android.app.PendingIntent.FLAG_IMMUTABLE));
            // The logo is its own target (a transparent hit view over
            // the glyph): tapping it just opens the Muse app — a
            // plain launch, no ask page.
            UriCatalog.Entry openApp = new UriCatalog.Entry("Widget", "Open Muse",
                    null, null, null, R.drawable.ic_open, false);
            views.setContentDescription(R.id.iv_logo_hit, openApp.label);
            views.setOnClickPendingIntent(R.id.iv_logo_hit,
                    Router.routerPendingIntent(context, openApp, 3000 + id));
            if (shortcut != null) {
                views.setViewVisibility(R.id.iv_shortcut, View.VISIBLE);
                if (BarStyle.isPixel(context, id)) {
                    // Pixel presents the end action as a filled
                    // circle; the glyph is baked into the bitmap,
                    // accent tint included.
                    views.setImageViewBitmap(R.id.iv_shortcut,
                            BarStyle.shortcutCircleBitmap(context, id, shortcut.icon));
                } else {
                    views.setImageViewResource(R.id.iv_shortcut, shortcut.icon);
                    views.setInt(R.id.iv_shortcut, "setColorFilter", accent);
                }
                views.setContentDescription(R.id.iv_shortcut, shortcut.label);
                views.setOnClickPendingIntent(R.id.iv_shortcut,
                        Router.routerPendingIntent(context, shortcut, 2000 + id));
            } else {
                views.setViewVisibility(R.id.iv_shortcut, View.GONE);
            }
            manager.updateAppWidget(id, views);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        for (int id : appWidgetIds) WidgetPrefs.clearWidget(context, id);
        super.onDeleted(context, appWidgetIds);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager manager,
                                          int appWidgetId, Bundle newOptions) {
        // A resize changes the pill's real size; re-render at it.
        onUpdate(context, manager, new int[]{appWidgetId});
    }
}
