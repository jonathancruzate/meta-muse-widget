package com.jonathancruzate.metamusewidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

import java.util.List;

/**
 * The shortcut panel widget (v4.10.0): the shortcut grid is back,
 * rebuilt on the activities. A One UI focus block (bg_card, 18dp)
 * holding up to eight medium-card cells (bg_cell, 14dp) in two
 * rows of four — the old grid's shape — but every cell is one of
 * the picker activities and fires the activity's prompt/direct
 * surface through ShortcutTargets, never a catalog URI (the old
 * grid died on the signature gate; see UriCatalog).
 *
 * The cell set is one global setting, edited in the Customize
 * screen's Shortcut panel section; WidgetPrefs.refreshWidgets
 * re-renders every placed panel after an edit, so there is no
 * polling and no update period. An empty set renders a single
 * setup cell that opens the settings.
 *
 * Layout discipline (the v4.3.2 lesson, REA-RENDER-DIFF-431):
 * the layout uses only RemoteViews-supported classes —
 * LinearLayout, ImageView, TextView.
 */
public class ShortcutPanelWidgetProvider extends AppWidgetProvider {

    private static final int[] CELLS = {
            R.id.cell_0, R.id.cell_1, R.id.cell_2, R.id.cell_3,
            R.id.cell_4, R.id.cell_5, R.id.cell_6, R.id.cell_7};
    private static final int[] ICONS = {
            R.id.cell_icon_0, R.id.cell_icon_1, R.id.cell_icon_2, R.id.cell_icon_3,
            R.id.cell_icon_4, R.id.cell_icon_5, R.id.cell_icon_6, R.id.cell_icon_7};
    private static final int[] LABELS = {
            R.id.cell_label_0, R.id.cell_label_1, R.id.cell_label_2, R.id.cell_label_3,
            R.id.cell_label_4, R.id.cell_label_5, R.id.cell_label_6, R.id.cell_label_7};

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            updateOne(context, manager, id);
        }
    }

    private void updateOne(Context context, AppWidgetManager manager, int id) {
        RemoteViews views = new RemoteViews(context.getPackageName(),
                R.layout.widget_shortcuts);
        int accent = BarStyle.accent(context);
        int foreground = context.getColor(R.color.foreground);
        List<String> labels = WidgetPrefs.panelShortcuts(context);

        if (labels.isEmpty()) {
            // Setup cell: the panel explains itself and opens the
            // settings section that fills it.
            views.setViewVisibility(CELLS[0], View.VISIBLE);
            views.setImageViewResource(ICONS[0], R.drawable.ic_plus);
            views.setInt(ICONS[0], "setColorFilter", accent);
            views.setTextViewText(LABELS[0], context.getString(R.string.add_shortcut));
            views.setTextColor(LABELS[0], foreground);
            Intent settings = new Intent(context, WidgetCustomizeActivity.class);
            settings.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            views.setOnClickPendingIntent(CELLS[0], PendingIntent.getActivity(
                    context, 4000, settings,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
            for (int i = 1; i < CELLS.length; i++) {
                views.setViewVisibility(CELLS[i], View.GONE);
            }
            manager.updateAppWidget(id, views);
            return;
        }

        for (int i = 0; i < CELLS.length; i++) {
            UriCatalog.Entry e = i < labels.size()
                    ? ShortcutTargets.entryForLabel(labels.get(i)) : null;
            if (e == null) {
                views.setViewVisibility(CELLS[i], View.GONE);
                continue;
            }
            views.setViewVisibility(CELLS[i], View.VISIBLE);
            views.setImageViewResource(ICONS[i], e.icon);
            views.setInt(ICONS[i], "setColorFilter", accent);
            views.setTextViewText(LABELS[i], e.label);
            views.setTextColor(LABELS[i], foreground);
            views.setContentDescription(CELLS[i], e.label);
            views.setOnClickPendingIntent(CELLS[i], PendingIntent.getActivity(
                    context, 4000 + i, ShortcutTargets.intentFor(context, e),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        }
        manager.updateAppWidget(id, views);
    }
}
