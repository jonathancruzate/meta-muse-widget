package com.jonathancruzate.metamusewidget;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

/**
 * Shared tile rendering for the tile and stack providers
 * (design visual system: one anatomy, tokenized padding, the
 * rose accent reserved for the kind label and the refresh
 * affordance, no silent truncation beyond the layout's own
 * ellipsize).
 */
final class TileRenderer {

    private TileRenderer() {}

    static RemoteViews build(Context ctx, int layoutRes, int kindRes,
                             PackData.TileContent content,
                             Intent tapIntent, int requestCode,
                             Intent refreshIntent) {
        RemoteViews views = new RemoteViews(ctx.getPackageName(),
                layoutRes);
        views.setTextViewText(R.id.tv_kind, ctx.getString(kindRes));
        views.setTextViewText(R.id.tv_value,
                content.value == null ? "" : content.value);
        views.setTextViewText(R.id.tv_body,
                content.body == null ? "" : content.body);
        views.setViewVisibility(R.id.tv_body,
                content.body == null || content.body.isEmpty()
                        ? View.GONE : View.VISIBLE);
        try {
            views.setTextViewText(R.id.tv_age,
                    content.age == null ? "" : content.age);
            views.setViewVisibility(R.id.tv_age,
                    content.age == null || content.age.isEmpty()
                            ? View.GONE : View.VISIBLE);
        } catch (Exception ignored) {
            // Square layout has no meta row.
        }
        if (tapIntent != null) {
            PendingIntent pi = PendingIntent.getActivity(ctx,
                    requestCode, tapIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                            | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.tile_root, pi);
        }
        PackThemes.applyTile(ctx, views,
                layoutRes != R.layout.widget_tile_square);
        if (refreshIntent != null) {
            try {
                PendingIntent rpi = PendingIntent.getBroadcast(ctx,
                        requestCode + 1, refreshIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE);
                views.setOnClickPendingIntent(R.id.tv_refresh, rpi);
            } catch (Exception ignored) {
                // Square layout has no refresh affordance.
            }
        }
        return views;
    }

    static int kindLabelRes(String kind) {
        switch (kind) {
            case "up_next": return R.string.pack_kind_up_next;
            case "house": return R.string.pack_kind_house;
            case "transit": return R.string.pack_kind_transit;
            case "media": return R.string.pack_kind_media;
            case "voice": return R.string.pack_kind_voice;
            case "decide": return R.string.pack_kind_decide;
            case "clock": return R.string.pack_kind_clock;
            case "money": return R.string.pack_kind_money;
            case "people": return R.string.pack_kind_people;
            case "sleep": return R.string.pack_kind_sleep;
            case "setup": return R.string.pack_kind_setup;
            case "brief":
            default: return R.string.pack_kind_brief;
        }
    }
}
