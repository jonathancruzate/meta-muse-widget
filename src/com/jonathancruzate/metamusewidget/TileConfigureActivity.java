package com.jonathancruzate.metamusewidget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

/**
 * Tile kind picker (Pack v5.0.0): the configure activity for
 * MuseTileWidgetProvider. One provider, per-instance kind,
 * stored in PackPrefs under the widget id. The kind list grows
 * by version: v5.0.0 ships the eight v1 kinds; Money and People
 * arrive in v5.1.0, Sleep and Launcher setup in v5.2.0 (current).
 */
public class TileConfigureActivity extends Activity {

    static final String[][] KINDS_V1 = {
            {"brief", "pack_kind_pick_brief"},
            {"up_next", "pack_kind_pick_up_next"},
            {"house", "pack_kind_pick_house"},
            {"transit", "pack_kind_pick_transit"},
            {"media", "pack_kind_pick_media"},
            {"voice", "pack_kind_pick_voice"},
            {"decide", "pack_kind_pick_decide"},
            {"clock", "pack_kind_pick_clock"},
            {"money", "pack_kind_pick_money"},
            {"people", "pack_kind_pick_people"},
            {"sleep", "pack_kind_pick_sleep"},
            {"setup", "pack_kind_pick_setup"},
    };

    private int widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);
        Intent intent = getIntent();
        if (intent != null) {
            widgetId = intent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        List<String> labels = new ArrayList<>();
        for (String[] k : KINDS_V1) {
            int res = getResources().getIdentifier(k[1], "string",
                    getPackageName());
            labels.add(res == 0 ? k[0] : getString(res));
        }
        ListView list = new ListView(this);
        list.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, labels));
        list.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                String kind = KINDS_V1[position][0];
                if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    PackPrefs.setKind(TileConfigureActivity.this,
                            widgetId, kind);
                    PackWidgetProvider.poke(TileConfigureActivity.this,
                            MuseTileWidgetProvider.class);
                    Intent result = new Intent();
                    result.putExtra(
                            AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
                    setResult(RESULT_OK, result);
                }
                finish();
            }
        });
        setContentView(list);
    }
}
