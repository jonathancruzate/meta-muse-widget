package com.jonathancruzate.metamusewidget;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

/**
 * Runs pack widget actions that are commands, not navigation:
 * voice and (from v5.1.0) house and media actions. Each runs on
 * a worker thread through the authenticated DaClient lane and
 * re-renders from Device Assistance's acknowledgement, never
 * from the tap itself. Unpaired voice falls back to the Muse
 * Voice Mode Shortcut app, then to the panel.
 */
public class PackActionReceiver extends BroadcastReceiver {

    public static final String ACTION_VOICE =
            "com.jonathancruzate.metamusewidget.action.PACK_VOICE";
    public static final String ACTION_DND_TOGGLE =
            "com.jonathancruzate.metamusewidget.action.PACK_DND_TOGGLE";
    public static final String ACTION_MEDIA_TOGGLE =
            "com.jonathancruzate.metamusewidget.action.PACK_MEDIA_TOGGLE";

    @Override
    public void onReceive(final Context context, Intent intent) {
        String action = intent.getAction();
        if (ACTION_VOICE.equals(action)) {
            final PendingResult pending = goAsync();
            final Context app = context.getApplicationContext();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (PackPrefs.daPaired(app)) {
                            DaClient.execute(app, "voice_mode");
                        } else {
                            fireVoiceShortcut(app);
                        }
                    } finally {
                        pending.finish();
                    }
                }
            }).start();
        } else if (ACTION_DND_TOGGLE.equals(action)
                || ACTION_MEDIA_TOGGLE.equals(action)) {
            final boolean dnd = ACTION_DND_TOGGLE.equals(action);
            final PendingResult pending = goAsync();
            final Context app = context.getApplicationContext();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (dnd) {
                            boolean on = PackPrefs.contextDndOn(app);
                            Bundle r = DaClient.execute(app,
                                    on ? "dnd_off" : "dnd_on");
                            if (r.getBoolean(DaContract.KEY_OK, false)) {
                                PackPrefs.putContextDndOn(app, !on);
                            }
                        } else {
                            DaClient.execute(app, "media_play_pause");
                        }
                        PackWidgetProvider.pokeAll(app);
                    } finally {
                        pending.finish();
                    }
                }
            }).start();
        }
    }

    private void fireVoiceShortcut(Context ctx) {
        try {
            Intent i = new Intent();
            i.setComponent(new ComponentName(
                    "com.jonathancruzate.metamusevoicemodeshortcut",
                    "com.jonathancruzate.metamusevoicemodeshortcut.ShortcutActivity"));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
        } catch (Exception e) {
            try {
                Intent panel = new Intent(ctx, MusePanelActivity.class);
                panel.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(panel);
            } catch (Exception ignored) {
                // Nothing sensible left to open.
            }
        }
    }
}
