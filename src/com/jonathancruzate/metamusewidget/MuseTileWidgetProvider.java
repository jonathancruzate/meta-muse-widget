package com.jonathancruzate.metamusewidget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.widget.RemoteViews;

import org.json.JSONObject;

/**
 * The Muse tile framework (Pack v5.0.0, design W2): ONE
 * provider, many kinds. The kind is chosen per instance in the
 * configure activity and stored per appWidgetId in PackPrefs,
 * so one picker entry yields Brief, Up Next, House, Transit,
 * Media, Voice, Decide and Clock tiles (Money, People and Sleep
 * kinds join in v5.1.0 and v5.2.0 through PackData).
 */
public class MuseTileWidgetProvider extends PackWidgetProvider {

    static final String[] KINDS = {
            "brief", "up_next", "house", "transit", "media",
            "voice", "decide", "clock", "money", "people",
            "sleep", "setup"};

    @Override
    protected Class<?> providerClass() {
        return MuseTileWidgetProvider.class;
    }

    @Override
    public void onDeleted(Context context, int[] widgetIds) {
        super.onDeleted(context, widgetIds);
        for (int id : widgetIds) PackPrefs.clearKind(context, id);
    }

    @Override
    protected RemoteViews render(Context ctx, int widgetId) {
        String kind = PackPrefs.kindFor(ctx, widgetId, "brief");
        JSONObject data = PackData.cachedData(ctx);
        PackData.TileContent content =
                PackData.contentFor(ctx, kind, data);
        int layout = layoutFor(ctx, widgetId);
        Intent refresh = new Intent(ctx,
                MuseTileWidgetProvider.class);
        refresh.setAction(ACTION_REFRESH);
        return TileRenderer.build(ctx, layout,
                TileRenderer.kindLabelRes(kind), content,
                tapIntentFor(ctx, kind), widgetId * 100, refresh);
    }

    private int layoutFor(Context ctx, int widgetId) {
        try {
            Bundle opts = AppWidgetManager.getInstance(ctx)
                    .getAppWidgetOptions(widgetId);
            int w = opts.getInt(
                    AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0);
            int h = opts.getInt(
                    AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0);
            if ((w > 0 && w < 200) || (h > 0 && h < 90)) {
                return R.layout.widget_tile_square;
            }
        } catch (Exception ignored) {
            // Fall through to the standard tile layout.
        }
        return R.layout.widget_tile;
    }

    static Intent tapIntentFor(Context ctx, String kind) {
        Intent i;
        switch (kind) {
            case "voice":
                i = new Intent(ctx, PackVoiceActivity.class);
                break;
            case "house":
                i = new Intent(ctx, HouseActivity.class);
                break;
            case "setup":
                i = new Intent(ctx, SetupWizardActivity.class);
                break;
            case "up_next": {
                long now = System.currentTimeMillis();
                Uri uri = CalendarContract.CONTENT_URI.buildUpon()
                        .appendPath("time")
                        .appendPath(String.valueOf(now)).build();
                i = new Intent(Intent.ACTION_VIEW, uri);
                break;
            }
            case "transit": {
                i = ctx.getPackageManager().getLaunchIntentForPackage(
                        "com.google.android.apps.maps");
                if (i == null) i = new Intent(ctx,
                        MusePanelActivity.class);
                break;
            }
            case "media": {
                i = ctx.getPackageManager().getLaunchIntentForPackage(
                        "com.spotify.music");
                if (i == null) i = new Intent(ctx,
                        MusePanelActivity.class);
                break;
            }
            default:
                i = new Intent(ctx, MusePanelActivity.class);
                break;
        }
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return i;
    }
}
