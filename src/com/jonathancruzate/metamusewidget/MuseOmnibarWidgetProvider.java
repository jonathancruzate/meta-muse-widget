package com.jonathancruzate.metamusewidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import org.json.JSONObject;

/**
 * The Muse Omnibar (Pack v5.0.0, design W1): the pack's front
 * door on Muse pages. Body tap opens Muse Search, the Muse
 * mark opens Muse Today, the mic fires voice mode. The idle
 * hint line rotates real state (next event, leave-by, house
 * mode) from the cached feed, it never shows filler.
 */
public class MuseOmnibarWidgetProvider extends PackWidgetProvider {

    @Override
    protected Class<?> providerClass() {
        return MuseOmnibarWidgetProvider.class;
    }

    @Override
    protected RemoteViews render(Context ctx, int widgetId) {
        RemoteViews views = new RemoteViews(ctx.getPackageName(),
                R.layout.widget_omnibar);
        views.setTextViewText(R.id.tv_hint, hintText(ctx));

        Intent search = new Intent(ctx, MuseSearchActivity.class);
        search.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        views.setOnClickPendingIntent(R.id.tv_hint,
                PendingIntent.getActivity(ctx, widgetId * 10 + 1,
                        search, PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE));
        views.setOnClickPendingIntent(R.id.omnibar_root,
                PendingIntent.getActivity(ctx, widgetId * 10 + 2,
                        search, PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE));

        Intent today = new Intent(ctx, MusePanelActivity.class);
        today.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        views.setOnClickPendingIntent(R.id.iv_logo,
                PendingIntent.getActivity(ctx, widgetId * 10 + 3,
                        today, PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE));

        Intent voice = new Intent(ctx, PackActionReceiver.class);
        voice.setAction(PackActionReceiver.ACTION_VOICE);
        views.setOnClickPendingIntent(R.id.iv_mic,
                PendingIntent.getBroadcast(ctx, widgetId * 10 + 4,
                        voice, PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE));
        return views;
    }

    private String hintText(Context ctx) {
        JSONObject data = PackData.cachedData(ctx);
        if (data != null) {
            JSONObject transit = data.optJSONObject("transit");
            if (transit != null) {
                String leaveBy = transit.optString("leave_by", "");
                if (!leaveBy.isEmpty()) {
                    return ctx.getString(R.string.pack_leave_by,
                            leaveBy);
                }
            }
            JSONObject upNext = data.optJSONObject("up_next");
            if (upNext != null) {
                String title = upNext.optString("title", "");
                if (!title.isEmpty()) return title;
            }
            PackData.TileContent house =
                    PackData.contentFor(ctx, "house", data);
            if (house.value != null && !house.value.isEmpty()) {
                return house.value;
            }
        }
        String[] ev = PanelWidgetData.hasCalendarPermission(ctx)
                ? PanelWidgetData.queryNextEvent(ctx) : null;
        if (ev != null && ev[0] != null && !ev[0].isEmpty()) {
            return ev[0];
        }
        return ctx.getString(R.string.pack_omnibar_hint);
    }
}
