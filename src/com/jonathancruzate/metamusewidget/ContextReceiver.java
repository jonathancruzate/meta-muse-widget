package com.jonathancruzate.metamusewidget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Receives Device Assistance context broadcasts (design
 * Section 7.3) and pokes only the widget kinds they affect.
 * Broadcasts cannot be sender-authenticated on Android, so this
 * receiver treats them strictly as re-render hints over cached
 * or on-device data: a spoofed poke can repaint a tile, never
 * run an action. Actions ride the authenticated DaClient lane.
 */
public class ContextReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;
        if (DaContract.ACTION_CONTEXT_MODE.equals(action)) {
            String mode = intent.getStringExtra(DaContract.EXTRA_MODE);
            if (mode != null && !mode.isEmpty()) {
                PackPrefs.putContextHouseMode(context, mode);
            }
            PackWidgetProvider.poke(context,
                    MuseTileWidgetProvider.class);
            PackWidgetProvider.poke(context,
                    MuseHouseWidgetProvider.class);
            PackWidgetProvider.poke(context,
                    MuseOmnibarWidgetProvider.class);
        } else if (DaContract.ACTION_CONTEXT_MEDIA.equals(action)) {
            String text = intent.getStringExtra(DaContract.EXTRA_TEXT);
            if (text != null) {
                PackPrefs.putContextMedia(context, text);
            }
            PackWidgetProvider.poke(context,
                    MuseTileWidgetProvider.class);
            PackWidgetProvider.poke(context,
                    MuseStackWidgetProvider.class);
        }
    }
}
