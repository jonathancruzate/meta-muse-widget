package com.samsung.oneui9.metamusewidget;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The hub: every Muse destination from UriCatalog, grouped in One UI
 * focus blocks (18dp cards, spec sections 1-3), with in-app search
 * (suggestions/filtering, spec section 2.9) and a bottom counter
 * (spec section 2.8). Sentence-case labels, Roboto (system sans).
 */
public class MainActivity extends Activity {

    private LinearLayout groupsBox;
    private TextView counter;
    private EditText search;
    private TextView themeValue;
    private TextView shortcutValue;
    private final List<View> allRows = new ArrayList<>();
    private final Map<View, String> rowGroup = new LinkedHashMap<>();
    private final Map<String, View> groupHeaders = new LinkedHashMap<>();
    private final Map<String, View> groupCards = new LinkedHashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // App-icon shortcuts land here with a widget://shortcut/*
        // data URI: route and finish before the hub draws.
        if (routeShortcut(getIntent())) return;
        setContentView(R.layout.activity_hub);
        groupsBox = findViewById(R.id.hub_groups);
        counter = findViewById(R.id.hub_counter);
        search = findViewById(R.id.hub_search);
        themeValue = findViewById(R.id.settings_theme_value);
        shortcutValue = findViewById(R.id.settings_shortcut_value);
        findViewById(R.id.settings_theme_row).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showThemeDialog(); }
        });
        findViewById(R.id.settings_shortcut_row).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showShortcutDialog(); }
        });
        findViewById(R.id.settings_customize_row).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new android.content.Intent(
                        MainActivity.this, WidgetCustomizeActivity.class));
            }
        });
        refreshSettingsUi();
        buildList("");
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { buildList(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });
        search.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    // Muse opens with the typed text in its composer
                    // (the ungated ASSIST_QUERY surface).
                    if (!Router.assist(MainActivity.this, v.getText().toString().trim())) {
                        Router.dispatch(MainActivity.this, UriCatalog.MODE_LAUNCH, null, null);
                    }
                    return true;
                }
                return false;
            }
        });
    }

    // ---- Widget settings (theme + search-bar shortcut) ----

    private void refreshSettingsUi() {
        String mode = WidgetPrefs.themeMode(this);
        themeValue.setText(WidgetPrefs.THEME_SYSTEM.equals(mode) ? R.string.theme_system
                : WidgetPrefs.THEME_STEPHANY.equals(mode) ? R.string.theme_stephany
                : WidgetPrefs.THEME_CUSTOM.equals(mode) ? R.string.theme_custom
                : R.string.theme_oneui);
        shortcutValue.setText(WidgetPrefs.shortcutActionLabel(this));
        themeValue.setTextColor(BarStyle.accent(this));
        shortcutValue.setTextColor(BarStyle.accent(this));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // The Customize screen edits the same settings; reflect them.
        refreshSettingsUi();
    }

    private void showThemeDialog() {
        final String[] choices = {
                getString(R.string.theme_choice_oneui),
                getString(R.string.theme_choice_system),
                getString(R.string.theme_choice_stephany),
                getString(R.string.theme_choice_custom)};
        final String[] modes = {
                WidgetPrefs.THEME_ONEUI, WidgetPrefs.THEME_SYSTEM,
                WidgetPrefs.THEME_STEPHANY, WidgetPrefs.THEME_CUSTOM};
        String mode = WidgetPrefs.themeMode(this);
        int current = WidgetPrefs.THEME_SYSTEM.equals(mode) ? 1
                : WidgetPrefs.THEME_STEPHANY.equals(mode) ? 2
                : WidgetPrefs.THEME_CUSTOM.equals(mode) ? 3 : 0;
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.theme_dialog_title)
                .setSingleChoiceItems(choices, current, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        WidgetPrefs.setTheme(MainActivity.this, modes[which]);
                        WidgetPrefs.refreshWidgets(MainActivity.this);
                        refreshSettingsUi();
                        dialog.dismiss();
                    }
                })
                .create();
        dialog.show();
        toBottom(dialog);
    }

    private void showShortcutDialog() {
        // The shortcut is an action, not a URI (Muse gates URIs by
        // caller signature — see Router). Bottom-anchored per the
        // One UI spec: dialogs rise from the bottom.
        final String[] labels = {
                getString(R.string.shortcut_action_assist),
                getString(R.string.shortcut_action_launch),
                getString(R.string.shortcut_none)};
        final String[] actions = {
                WidgetPrefs.SHORTCUT_ASSIST, WidgetPrefs.SHORTCUT_LAUNCH,
                WidgetPrefs.SHORTCUT_NONE};
        String current = WidgetPrefs.shortcutAction(this);
        int checked = WidgetPrefs.SHORTCUT_LAUNCH.equals(current) ? 1
                : WidgetPrefs.SHORTCUT_NONE.equals(current) ? 2 : 0;
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.shortcut_dialog_title)
                .setSingleChoiceItems(labels, checked,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int which) {
                                WidgetPrefs.setShortcutAction(MainActivity.this, actions[which]);
                                WidgetPrefs.refreshWidgets(MainActivity.this);
                                refreshSettingsUi();
                                d.dismiss();
                            }
                        })
                .create();
        dialog.show();
        toBottom(dialog);
    }

    /** One UI spec: dialogs rise from the bottom of the screen. */
    /** App-icon shortcut routing (see res/xml/shortcuts.xml). */
    private boolean routeShortcut(android.content.Intent intent) {
        android.net.Uri data = intent != null ? intent.getData() : null;
        if (data == null || !"widget".equals(data.getScheme())) return false;
        String uri = data.toString();
        if (uri.endsWith("/ask")) {
            startActivity(new android.content.Intent(this, ComposeActivity.class));
        } else if (uri.endsWith("/new")) {
            Router.dispatch(this, UriCatalog.MODE_ASSIST, null, null);
        } else if (uri.endsWith("/customize")) {
            startActivity(new android.content.Intent(this, WidgetCustomizeActivity.class));
        } else {
            return false;
        }
        finish();
        return true;
    }

    static void toBottom(AlertDialog dialog) {
        android.view.Window w = dialog.getWindow();
        if (w == null) return;
        w.setGravity(Gravity.BOTTOM);
        android.view.WindowManager.LayoutParams lp = w.getAttributes();
        lp.width = android.view.WindowManager.LayoutParams.MATCH_PARENT;
        w.setAttributes(lp);
    }

    private void buildList(String query) {
        groupsBox.removeAllViews();
        allRows.clear();
        rowGroup.clear();
        groupHeaders.clear();
        groupCards.clear();
        String q = query.trim().toLowerCase(Locale.US);

        Map<String, List<UriCatalog.Entry>> byGroup = new LinkedHashMap<>();
        for (UriCatalog.Entry e : UriCatalog.ENTRIES) {
            if (!q.isEmpty()
                    && !e.label.toLowerCase(Locale.US).contains(q)
                    && !(e.uri != null && e.uri.toLowerCase(Locale.US).contains(q))) {
                continue;
            }
            List<UriCatalog.Entry> bucket = byGroup.get(e.group);
            if (bucket == null) {
                bucket = new ArrayList<>();
                byGroup.put(e.group, bucket);
            }
            bucket.add(e);
        }

        int shown = 0;
        for (Map.Entry<String, List<UriCatalog.Entry>> g : byGroup.entrySet()) {
            TextView header = new TextView(this);
            header.setText(g.getKey());
            header.setTextSize(14);
            header.setTextColor(getColor(R.color.secondary));
            header.setPadding(dp(4), dp(18), dp(4), dp(8));
            groupsBox.addView(header);
            groupHeaders.put(g.getKey(), header);

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.bg_card);
            card.setPadding(dp(8), dp(4), dp(8), dp(4));
            groupsBox.addView(card);
            groupCards.put(g.getKey(), card);

            List<UriCatalog.Entry> entries = g.getValue();
            for (int i = 0; i < entries.size(); i++) {
                View row = buildRow(entries.get(i));
                card.addView(row);
                allRows.add(row);
                rowGroup.put(row, g.getKey());
                shown++;
                if (i < entries.size() - 1) {
                    View divider = new View(this);
                    divider.setBackgroundColor(getColor(R.color.divider));
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
                    lp.setMarginStart(dp(60));
                    card.addView(divider, lp);
                }
            }
        }

        if (shown == 0) {
            TextView empty = new TextView(this);
            empty.setText(R.string.no_results);
            empty.setTextSize(15);
            empty.setTextColor(getColor(R.color.secondary));
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(8), dp(32), dp(8), dp(4));
            groupsBox.addView(empty);
            TextView action = new TextView(this);
            action.setText(R.string.open_muse_search);
            action.setTextSize(15);
            action.setTextColor(getColor(R.color.primary_dark));
            action.setGravity(Gravity.CENTER);
            action.setPadding(dp(8), dp(8), dp(8), dp(16));
            action.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!Router.assist(MainActivity.this, query)) {
                        Router.dispatch(MainActivity.this, UriCatalog.MODE_LAUNCH, null, null);
                    }
                }
            });
            groupsBox.addView(action);
        }
        counter.setText(getString(R.string.counter_template, shown));
    }

    private View buildRow(UriCatalog.Entry e) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(56));
        row.setPadding(dp(10), dp(8), dp(10), dp(8));
        row.setClickable(true);
        row.setFocusable(true);
        int[] attrs = {android.R.attr.selectableItemBackground};
        android.content.res.TypedArray ta = obtainStyledAttributes(attrs);
        Drawable ripple = ta.getDrawable(0);
        ta.recycle();
        row.setBackground(ripple);
        row.setContentDescription(e.label);
        final UriCatalog.Entry entry = e;
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // ASSIST and LAUNCH rows act; URI destinations are
                // reference (Muse gates them by caller signature),
                // so a tap copies the URI for use inside Muse.
                if (entry.mode == UriCatalog.MODE_ASSIST) {
                    if (!Router.assist(MainActivity.this, null)) {
                        Router.dispatch(MainActivity.this, UriCatalog.MODE_LAUNCH, null, null);
                    }
                } else if (entry.mode == UriCatalog.MODE_LAUNCH) {
                    Router.dispatch(MainActivity.this, UriCatalog.MODE_LAUNCH, null, null);
                } else {
                    Router.copy(MainActivity.this,
                            entry.display != null ? entry.display : entry.uri);
                }
            }
        });

        ImageView icon = new ImageView(this);
        icon.setImageResource(e.icon);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(28), dp(28));
        row.addView(icon, iconLp);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textsLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textsLp.setMarginStart(dp(14));
        row.addView(texts, textsLp);

        TextView label = new TextView(this);
        label.setText(e.label);
        label.setTextSize(16);
        label.setTextColor(getColor(R.color.foreground));
        label.setMaxLines(1);
        texts.addView(label);

        TextView sub = new TextView(this);
        sub.setText(e.display != null ? e.display : (e.uri != null ? e.uri : "Opens the Muse app"));
        sub.setTextSize(13);
        sub.setTextColor(getColor(R.color.secondary));
        sub.setMaxLines(1);
        texts.addView(sub);

        ImageView chevron = new ImageView(this);
        chevron.setImageResource(R.drawable.ic_open);
        chevron.setImageTintList(ColorStateList.valueOf(getColor(R.color.secondary)));
        LinearLayout.LayoutParams chLp = new LinearLayout.LayoutParams(dp(18), dp(18));
        row.addView(chevron, chLp);
        return row;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
