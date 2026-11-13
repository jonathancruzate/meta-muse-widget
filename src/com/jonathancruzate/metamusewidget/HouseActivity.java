package com.jonathancruzate.metamusewidget;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

/**
 * The Muse house surface (Pack v5.0.0): verified house state
 * from the feed (desired versus actual, with its report age,
 * straight from PanelWidgetData's house shaping) and the
 * Device Assistance action row. Actions execute through the
 * authenticated local channel and report from its
 * acknowledgement. Mode requests are NOT made here: desired
 * state is owned by the mode-sync lane, and this surface says
 * so rather than pretending a button wrote it.
 */
public class HouseActivity extends Activity {

    private TextView stateView;
    private TextView statusView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.pack_kind_pick_house);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(48, 48, 48, 48);
        stateView = new TextView(this);
        stateView.setTextSize(20);
        root.addView(stateView);
        statusView = new TextView(this);
        root.addView(statusView);
        root.addView(actionButton(getString(R.string.pack_house_dnd),
                new Action() {
                    @Override public void run() {
                        runAction(PackActionReceiver.ACTION_DND_TOGGLE);
                    }
                }));
        root.addView(actionButton(getString(R.string.pack_house_media),
                new Action() {
                    @Override public void run() {
                        runAction(PackActionReceiver.ACTION_MEDIA_TOGGLE);
                    }
                }));
        root.addView(actionButton(getString(R.string.pack_house_voice),
                new Action() {
                    @Override public void run() {
                        startActivity(new Intent(HouseActivity.this,
                                PackVoiceActivity.class));
                    }
                }));
        Button today = new Button(this);
        today.setText(R.string.pack_house_today);
        today.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                startActivity(new Intent(HouseActivity.this,
                        MusePanelActivity.class));
            }
        });
        root.addView(today);
        Button smartThings = new Button(this);
        smartThings.setText("SmartThings");
        smartThings.setOnClickListener(
                new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                Intent launch = getPackageManager()
                        .getLaunchIntentForPackage(
                                "com.samsung.android.oneconnect");
                if (launch != null) startActivity(launch);
            }
        });
        root.addView(smartThings);
        setContentView(root);
        refresh();
    }

    private interface Action { void run(); }

    private Button actionButton(String label, final Action action) {
        Button b = new Button(this);
        b.setText(label);
        b.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                action.run();
            }
        });
        return b;
    }

    private void runAction(String broadcastAction) {
        if (!PackPrefs.daPaired(this)) {
            Toast.makeText(this, R.string.pack_house_unpaired,
                    Toast.LENGTH_LONG).show();
            return;
        }
        Intent i = new Intent(this, PackActionReceiver.class);
        i.setAction(broadcastAction);
        sendBroadcast(i);
        statusView.setText(R.string.panel_refreshing);
    }

    private void refresh() {
        JSONObject data = PackData.cachedData(this);
        PackData.TileContent house =
                PackData.contentFor(this, "house", data);
        stateView.setText(house.value);
        statusView.setText(house.body == null ? "" : house.body);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }
}
