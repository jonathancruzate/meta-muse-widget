package com.jonathancruzate.metamusewidget;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.RemoteViews;

/**
 * The Muse Grid widget (Pack v5.0.0, design W4): favorite apps
 * in a 4x2 focus block, direct launches through PackageManager,
 * and an All apps header that opens the full Muse Grid. App
 * Pairs are a Muse Grid activity feature (sequenced launches
 * through Device Assistance), not a widget surface.
 */
public class MuseGridWidgetProvider extends PackWidgetProvider {

    private static final int[] CELL_IDS = {
            R.id.cell_0, R.id.cell_1, R.id.cell_2, R.id.cell_3,
            R.id.cell_4, R.id.cell_5, R.id.cell_6, R.id.cell_7};
    private static final int[] ICON_IDS = {
            R.id.cell_icon_0, R.id.cell_icon_1, R.id.cell_icon_2,
            R.id.cell_icon_3, R.id.cell_icon_4, R.id.cell_icon_5,
            R.id.cell_icon_6, R.id.cell_icon_7};
    private static final int[] LABEL_IDS = {
            R.id.cell_label_0, R.id.cell_label_1, R.id.cell_label_2,
            R.id.cell_label_3, R.id.cell_label_4, R.id.cell_label_5,
            R.id.cell_label_6, R.id.cell_label_7};

    @Override
    protected Class<?> providerClass() {
        return MuseGridWidgetProvider.class;
    }

    @Override
    protected RemoteViews render(Context ctx, int widgetId) {
        RemoteViews views = new RemoteViews(ctx.getPackageName(),
                R.layout.widget_grid);
        Intent grid = new Intent(ctx, MuseGridActivity.class);
        grid.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        views.setOnClickPendingIntent(R.id.grid_all,
                PendingIntent.getActivity(ctx, widgetId * 100 + 50,
                        grid, PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE));
        PackageManager pm = ctx.getPackageManager();
        String[] packages = PackPrefs.gridPackages(ctx);
        for (int i = 0; i < CELL_IDS.length; i++) {
            if (i >= packages.length) {
                views.setViewVisibility(CELL_IDS[i], View.GONE);
                continue;
            }
            String pkg = packages[i];
            Intent launch = pm.getLaunchIntentForPackage(pkg);
            if (launch == null) {
                views.setViewVisibility(CELL_IDS[i], View.GONE);
                continue;
            }
            views.setViewVisibility(CELL_IDS[i], View.VISIBLE);
            try {
                CharSequence label =
                        pm.getApplicationLabel(pm.getApplicationInfo(
                                pkg, 0));
                views.setTextViewText(LABEL_IDS[i],
                        label == null ? pkg : label.toString());
                Bitmap icon = toBitmap(pm.getApplicationIcon(pkg));
                if (icon != null) {
                    views.setImageViewBitmap(ICON_IDS[i], icon);
                }
            } catch (Exception ignored) {
                views.setTextViewText(LABEL_IDS[i], pkg);
            }
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            views.setOnClickPendingIntent(CELL_IDS[i],
                    PendingIntent.getActivity(ctx,
                            widgetId * 100 + i, launch,
                            PendingIntent.FLAG_UPDATE_CURRENT
                                    | PendingIntent.FLAG_IMMUTABLE));
        }
        return views;
    }

    private Bitmap toBitmap(Drawable d) {
        try {
            Bitmap b = Bitmap.createBitmap(96, 96,
                    Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(b);
            d.setBounds(0, 0, 96, 96);
            d.draw(canvas);
            return b;
        } catch (Exception e) {
            return null;
        }
    }
}
