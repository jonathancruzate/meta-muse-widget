package com.jonathancruzate.metamusewidget;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;

/**
 * Voice trampoline (Pack v5.0.0): fired by the Voice tile and
 * other tap surfaces that cannot carry a broadcast
 * PendingIntent. Runs the same lane as PackActionReceiver's
 * voice action and finishes immediately.
 */
public class PackVoiceActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final android.content.Context app =
                getApplicationContext();
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (PackPrefs.daPaired(app)) {
                    DaClient.execute(app, "voice_mode");
                } else {
                    try {
                        Intent i = new Intent();
                        i.setComponent(new ComponentName(
                                "com.jonathancruzate.metamusevoicemodeshortcut",
                                "com.jonathancruzate.metamusevoicemodeshortcut.ShortcutActivity"));
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        app.startActivity(i);
                    } catch (Exception ignored) {
                        // No voice lane available; nothing to open.
                    }
                }
            }
        }).start();
        finish();
    }
}
