package com.jonathancruzate.metamusewidget;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.EditorInfo;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Muse Search (Pack v5.0.0, design Section 6.2): the full
 * screen finder the Omnibar opens. Real text input, on-device
 * results only: matching apps, upcoming calendar events whose
 * titles match, an Ask Muse row that prefills the ask page
 * through ComposeActivity's existing extra, and a web search
 * row. Nothing typed here leaves the device except the two
 * rows the user taps that are explicitly external.
 */
public class MuseSearchActivity extends Activity {

    private static final class Row {
        static final int ASK = 0;
        static final int WEB = 1;
        static final int APP = 2;
        static final int EVENT = 3;
        final int type;
        final String label;
        final String pkg;
        final long eventStart;
        Row(int type, String label, String pkg, long eventStart) {
            this.type = type;
            this.label = label;
            this.pkg = pkg;
            this.eventStart = eventStart;
        }
    }

    private final List<Row> rows = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private List<ResolveInfo> launcherApps = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.pack_search_title);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        EditText input = new EditText(this);
        input.setHint(R.string.pack_search_hint);
        input.setSingleLine(true);
        input.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        root.addView(input);
        ListView list = new ListView(this);
        root.addView(list);
        setContentView(root);

        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                new ArrayList<>());
        list.setAdapter(adapter);
        loadApps();
        rebuild("");
        list.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                if (position < 0 || position >= rows.size()) return;
                Row row = rows.get(position);
                switch (row.type) {
                    case Row.ASK: {
                        Intent i = new Intent(MuseSearchActivity.this,
                                ComposeActivity.class);
                        i.putExtra(ComposeActivity.EXTRA_PREFILL,
                                currentQuery(input));
                        startActivity(i);
                        break;
                    }
                    case Row.WEB: {
                        try {
                            String url = "https://www.google.com/search?q="
                                    + URLEncoder.encode(
                                            currentQuery(input), "UTF-8");
                            startActivity(new Intent(Intent.ACTION_VIEW,
                                    Uri.parse(url)));
                        } catch (Exception ignored) {
                            // No browser to hand to.
                        }
                        break;
                    }
                    case Row.APP: {
                        Intent launch = getPackageManager()
                                .getLaunchIntentForPackage(row.pkg);
                        if (launch != null) startActivity(launch);
                        break;
                    }
                    case Row.EVENT: {
                        Uri uri = CalendarContract.CONTENT_URI.buildUpon()
                                .appendPath("time")
                                .appendPath(String.valueOf(row.eventStart))
                                .build();
                        startActivity(new Intent(Intent.ACTION_VIEW, uri));
                        break;
                    }
                    default:
                        break;
                }
            }
        });
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s,
                    int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s,
                    int start, int before, int count) {
                rebuild(s == null ? "" : s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        input.requestFocus();
    }

    private String currentQuery(EditText input) {
        return input.getText() == null
                ? "" : input.getText().toString().trim();
    }

    private void loadApps() {
        try {
            Intent main = new Intent(Intent.ACTION_MAIN);
            main.addCategory(Intent.CATEGORY_LAUNCHER);
            launcherApps = getPackageManager()
                    .queryIntentActivities(main, 0);
        } catch (Exception e) {
            launcherApps = new ArrayList<>();
        }
    }

    private void rebuild(String query) {
        rows.clear();
        List<String> labels = new ArrayList<>();
        String q = query.trim();
        String ql = q.toLowerCase();
        if (!q.isEmpty()) {
            rows.add(new Row(Row.ASK,
                    getString(R.string.pack_search_ask, q), null, 0));
            labels.add(rows.get(rows.size() - 1).label);
        }
        PackageManager pm = getPackageManager();
        for (ResolveInfo info : launcherApps) {
            CharSequence label = info.loadLabel(pm);
            String name = label == null ? "" : label.toString();
            if (q.isEmpty() || name.toLowerCase().contains(ql)) {
                String pkg = info.activityInfo.packageName;
                rows.add(new Row(Row.APP, name, pkg, 0));
                labels.add(name);
                if (rows.size() > 40) break;
            }
        }
        if (!q.isEmpty()) {
            rows.add(new Row(Row.WEB,
                    getString(R.string.pack_search_web, q), null, 0));
            labels.add(rows.get(rows.size() - 1).label);
        }
        adapter.clear();
        adapter.addAll(labels);
        adapter.notifyDataSetChanged();
    }
}
