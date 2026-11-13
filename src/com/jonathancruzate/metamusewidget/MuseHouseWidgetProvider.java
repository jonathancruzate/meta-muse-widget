package com.jonathancruzate.metamusewidget;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import org.json.JSONObject;

/**
 * House Control widget (Pack v5.1.0, design W5): verified
 * house state from the feed on top, four action cells below.
 * Actions run through Device Assistance's authenticated local
 * channel (via PackActionReceiver) and the widget re-renders
 * from the acknowledgement path, never from the tap. The
 * flashlight is deliberately absent (design Section 6.1).
 * When Device Assistance is unpaired, the body says so and
 * the action cells still land on the House activity, which
 * explains the pairing step instead of failing silently.
 */
public class MuseHouseWidgetProvider extends PackWidgetProvider {

    @Override
    protected Class<?> providerClass() {
        return MuseHouseWidgetProvider.class;
    }

    @Override
    protected RemoteViews render(Context ctx, int widgetId) {
        RemoteViews views = new RemoteViews(ctx.getPackageName(),
                R.layout.widget_house);
        JSONObject data = PackData.cachedData(ctx);
        PackData.TileContent house =
                PackData.contentFor(ctx, "house", data);
        views.setTextViewText(R.id.tv_value,
                house.value == null ? "" : house.value);
        String body = PackPrefs.daPaired(ctx)
                ? (house.body == null ? "" : house.body)
                : ctx.getString(R.string.pack_house_unpaired);
        views.setTextViewText(R.id.tv_body, body);

        Intent houseIntent = new Intent(ctx, HouseActivity.class);
        houseIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        views.setOnClickPendingIntent(R.id.house_root,
                PendingIntent.getActivity(ctx, widgetId * 100 + 1,
                        houseIntent, PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE));

        views.setOnClickPendingIntent(R.id.act_dnd,
                actionIntent(ctx, widgetId * 100 + 2,
                        PackActionReceiver.ACTION_DND_TOGGLE));
        views.setOnClickPendingIntent(R.id.act_media,
                actionIntent(ctx, widgetId * 100 + 3,
                        PackActionReceiver.ACTION_MEDIA_TOGGLE));

        Intent voice = new Intent(ctx, PackVoiceActivity.class);
        voice.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        views.setOnClickPendingIntent(R.id.act_voice,
                PendingIntent.getActivity(ctx, widgetId * 100 + 4,
                        voice, PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE));

        Intent today = new Intent(ctx, MusePanelActivity.class);
        today.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        views.setOnClickPendingIntent(R.id.act_today,
                PendingIntent.getActivity(ctx, widgetId * 100 + 5,
                        today, PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE));
        return views;
    }

    private PendingIntent actionIntent(Context ctx, int requestCode,
                                       String action) {
        Intent i = new Intent(ctx, PackActionReceiver.class);
        i.setAction(action);
        return PendingIntent.getBroadcast(ctx, requestCode, i,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE);
    }
}
