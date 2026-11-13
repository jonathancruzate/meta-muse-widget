package com.jonathancruzate.metamusewidget;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Muse Grid (Pack v5.0.0, design Section 6.2): the full app
 * list with filter, and App Pair launching. Pair mode: tap two
 * apps, then Launch pair: the launches are sequenced through
 * Device Assistance's app_launch template when paired (the
 * engine owns timing), or fired back to back directly when it
 * is not.
 */
public class MuseGridActivity extends Activity {

    private static final class AppEntry {
        final String label;
        final String pkg;
        final Drawable icon;
        AppEntry(String label, String pkg, Drawable icon) {
            this.label = label;
            this.pkg = pkg;
            this.icon = icon;
        }
    }

    private final List<AppEntry> all = new ArrayList<>();
    private final List<AppEntry> shown = new ArrayList<>();
    private final List<AppEntry> pair = new ArrayList<>();
    private boolean pairMode = false;
    private GridAdapter adapter;
    private Button pairButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.pack_grid_title);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        EditText filter = new EditText(this);
        filter.setHint(R.string.pack_grid_search_hint);
        filter.setSingleLine(true);
        root.addView(filter);
        pairButton = new Button(this);
        pairButton.setText("Pair mode: off");
        root.addView(pairButton);
        GridView grid = new GridView(this);
        grid.setNumColumns(4);
        root.addView(grid);
        setContentView(root);

        loadApps();
        adapter = new GridAdapter();
        grid.setAdapter(adapter);
        applyFilter("");
        pairButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pairMode = !pairMode;
                pair.clear();
                pairButton.setText(pairMode
                        ? "Pair mode: tap two apps" : "Pair mode: off");
            }
        });
        grid.setOnItemClickListener(
                new android.widget.AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(
                    android.widget.AdapterView<?> parent, View view,
                    int position, long id) {
                if (position < 0 || position >= shown.size()) return;
                AppEntry entry = shown.get(position);
                if (!pairMode) {
                    launch(entry.pkg);
                    return;
                }
                pair.add(entry);
                if (pair.size() < 2) {
                    pairButton.setText(
                            "Pair mode: one picked, tap one more");
                    return;
                }
                launchPair(pair.get(0).pkg, pair.get(1).pkg);
                pair.clear();
                pairMode = false;
                pairButton.setText("Pair mode: off");
            }
        });
        filter.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s,
                    int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s,
                    int start, int before, int count) {
                applyFilter(s == null ? "" : s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void loadApps() {
        try {
            PackageManager pm = getPackageManager();
            Intent main = new Intent(Intent.ACTION_MAIN);
            main.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> infos =
                    pm.queryIntentActivities(main, 0);
            for (ResolveInfo info : infos) {
                CharSequence label = info.loadLabel(pm);
                all.add(new AppEntry(
                        label == null ? info.activityInfo.packageName
                                : label.toString(),
                        info.activityInfo.packageName,
                        info.loadIcon(pm)));
            }
            Collections.sort(all, new Comparator<AppEntry>() {
                @Override
                public int compare(AppEntry a, AppEntry b) {
                    return String.CASE_INSENSITIVE_ORDER.compare(
                            a.label, b.label);
                }
            });
        } catch (Exception ignored) {
            // Empty grid beats a crash.
        }
    }

    private void applyFilter(String query) {
        shown.clear();
        String q = query.trim().toLowerCase();
        for (AppEntry e : all) {
            if (q.isEmpty() || e.label.toLowerCase().contains(q)) {
                shown.add(e);
            }
        }
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void launch(String pkg) {
        Intent launch = getPackageManager()
                .getLaunchIntentForPackage(pkg);
        if (launch != null) startActivity(launch);
    }

    private void launchPair(final String first, final String second) {
        if (PackPrefs.daPaired(this)) {
            final android.content.Context app =
                    getApplicationContext();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    DaClient.execute(app, "app_launch", first);
                    try { Thread.sleep(700L); }
                    catch (InterruptedException ignored) {}
                    DaClient.execute(app, "app_launch", second);
                }
            }).start();
            Toast.makeText(this, "Launching pair through Device "
                    + "Assistance", Toast.LENGTH_SHORT).show();
        } else {
            launch(first);
            launch(second);
        }
    }

    private final class GridAdapter extends BaseAdapter {
        @Override public int getCount() { return shown.size(); }
        @Override public Object getItem(int position) {
            return shown.get(position);
        }
        @Override public long getItemId(int position) {
            return position;
        }
        @Override public View getView(int position, View convert,
                                        ViewGroup parent) {
            LinearLayout cell;
            ImageView icon;
            TextView label;
            if (convert == null) {
                cell = new LinearLayout(MuseGridActivity.this);
                cell.setOrientation(LinearLayout.VERTICAL);
                cell.setGravity(android.view.Gravity.CENTER);
                cell.setPadding(8, 12, 8, 12);
                icon = new ImageView(MuseGridActivity.this);
                icon.setLayoutParams(new LinearLayout.LayoutParams(
                        120, 120));
                cell.addView(icon);
                label = new TextView(MuseGridActivity.this);
                label.setMaxLines(1);
                label.setEllipsize(android.text.TextUtils.TruncateAt.END);
                label.setGravity(android.view.Gravity.CENTER);
                cell.addView(label);
                cell.setTag(new Object[]{icon, label});
            } else {
                cell = (LinearLayout) convert;
                Object[] parts = (Object[]) cell.getTag();
                icon = (ImageView) parts[0];
                label = (TextView) parts[1];
            }
            AppEntry entry = shown.get(position);
            icon.setImageDrawable(entry.icon);
            label.setText(entry.label);
            return cell;
        }
    }
}
