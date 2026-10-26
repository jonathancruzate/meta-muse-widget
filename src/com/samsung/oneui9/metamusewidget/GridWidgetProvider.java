package com.samsung.oneui9.metamusewidget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.widget.RemoteViews;

import java.util.List;

/**
 * 4x2 focus-block grid: the first 8 catalog entries flagged for the
 * grid. The theme color touches only the icons (header logo + cell
 * glyphs) — card and cell backgrounds always stay the One UI spec
 * styling (Jonathan's scope rule, 2026-10-26).
 */
public class GridWidgetProvider extends AppWidgetProvider {

    private static final int[] CELL_IDS = {
            R.id.cell_0, R.id.cell_1, R.id.cell_2, R.id.cell_3,
            R.id.cell_4, R.id.cell_5, R.id.cell_6, R.id.cell_7};
    private static final int[] ICON_IDS = {
            R.id.icon_0, R.id.icon_1, R.id.icon_2, R.id.icon_3,
            R.id.icon_4, R.id.icon_5, R.id.icon_6, R.id.icon_7};
    private static final int[] LABEL_IDS = {
            R.id.label_0, R.id.label_1, R.id.label_2, R.id.label_3,
            R.id.label_4, R.id.label_5, R.id.label_6, R.id.label_7};

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        List<UriCatalog.Entry> cells = UriCatalog.gridEntries();
        for (int id : appWidgetIds) {
            int accent = BarStyle.accent(context, id);
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_grid);
            views.setInt(R.id.iv_grid_logo, "setColorFilter", accent);
            for (int i = 0; i < CELL_IDS.length && i < cells.size(); i++) {
                UriCatalog.Entry e = cells.get(i);
                views.setImageViewResource(ICON_IDS[i], e.icon);
                views.setInt(ICON_IDS[i], "setColorFilter", accent);
                views.setTextViewText(LABEL_IDS[i], e.label);
                views.setContentDescription(CELL_IDS[i], e.label);
                views.setOnClickPendingIntent(CELL_IDS[i],
                        Router.routerPendingIntent(context, e, 3000 + id * 16 + i));
            }
            manager.updateAppWidget(id, views);
        }
    }
}
