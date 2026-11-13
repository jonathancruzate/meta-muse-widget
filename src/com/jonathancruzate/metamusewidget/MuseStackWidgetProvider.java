package com.jonathancruzate.metamusewidget;

import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import org.json.JSONObject;

import java.util.Arrays;

/**
 * The Context Stack card set (Pack v5.0.0, design W3): every
 * instance is exactly 4x2 so one Samsung widget stack can hold
 * them; Samsung owns the stack's rotation and order, the pack
 * owns freshness. Kinds are assigned by instance order (Brief,
 * Transit, Media, House, Up Next) and stored per widget id.
 */
public class MuseStackWidgetProvider extends PackWidgetProvider {

    private static final String[] STACK_KINDS = {
            "brief", "transit", "media", "house", "up_next"};

    @Override
    protected Class<?> providerClass() {
        return MuseStackWidgetProvider.class;
    }

    @Override
    public void onDeleted(Context context, int[] widgetIds) {
        super.onDeleted(context, widgetIds);
        for (int id : widgetIds) PackPrefs.clearKind(context, id);
    }

    @Override
    protected RemoteViews render(Context ctx, int widgetId) {
        String kind = PackPrefs.kindFor(ctx, widgetId, "");
        if (kind.isEmpty()) {
            kind = assignKind(ctx, widgetId);
            PackPrefs.setKind(ctx, widgetId, kind);
        }
        JSONObject data = PackData.cachedData(ctx);
        PackData.TileContent content =
                PackData.contentFor(ctx, kind, data);
        Intent refresh = new Intent(ctx,
                MuseStackWidgetProvider.class);
        refresh.setAction(ACTION_REFRESH);
        return TileRenderer.build(ctx, R.layout.widget_stack_card,
                TileRenderer.kindLabelRes(kind), content,
                MuseTileWidgetProvider.tapIntentFor(ctx, kind),
                widgetId * 100, refresh);
    }

    private String assignKind(Context ctx, int widgetId) {
        android.appwidget.AppWidgetManager mgr =
                android.appwidget.AppWidgetManager.getInstance(ctx);
        int[] ids = mgr.getAppWidgetIds(new android.content.ComponentName(
                ctx, MuseStackWidgetProvider.class));
        Arrays.sort(ids);
        int index = 0;
        for (int i = 0; i < ids.length; i++) {
            if (ids[i] == widgetId) { index = i; break; }
        }
        return STACK_KINDS[index % STACK_KINDS.length];
    }
}
