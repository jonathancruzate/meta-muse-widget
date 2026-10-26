package com.samsung.oneui9.metamusewidget;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The ask page: a Finder/Google-style search screen in this app.
 * Tapping the widget's bar lands here — type, with recent asks and
 * predictive suggestions below (the One UI spec's search pattern:
 * suggestions before input, auto-complete as you type) — and
 * Enter puts the text into Muse's composer through the ungated
 * ASSIST_QUERY surface, then this page disappears.
 */
public class ComposeActivity extends Activity {

    private static final String PREFS = "compose";
    private static final String KEY_RECENTS = "recents";
    private static final int MAX_RECENTS = 12;
    private static final String SEP = "";

    /** Starter prompts shown before any history exists. */
    private static final String[] STARTERS = {
            "Plan my day",
            "What should I focus on today?",
            "Help me prepare for a meeting",
            "Brainstorm ideas for a project",
            "Summarize an article for me",
            "Draft a polite follow-up email",
            "Explain a topic to me simply",
            "Give me a workout plan",
            "Suggest a dinner recipe",
            "Quiz me on a topic",
    };

    private EditText input;
    private LinearLayout list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose);
        input = findViewById(R.id.compose_input);
        list = findViewById(R.id.compose_list);

        // The bar is the widget's own pill: same glass, same accent.
        LinearLayout bar = findViewById(R.id.compose_bar);
        GradientDrawable pill = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{BarStyle.fillTop(this), BarStyle.fill(this)});
        pill.setShape(GradientDrawable.RECTANGLE);
        pill.setCornerRadius(100f * getResources().getDisplayMetrics().density);
        pill.setStroke(Math.max(1, Math.round(0.5f
                * getResources().getDisplayMetrics().density)), BarStyle.outlineColor(this));
        bar.setBackground(pill);
        ImageView logo = findViewById(R.id.compose_logo);
        logo.setColorFilter(BarStyle.accent(this));
        input.setHintTextColor(BarStyle.hintColor(this));

        // The shortcut glyph mirrors the widget bar's right button.
        ImageView shortcut = findViewById(R.id.compose_shortcut);
        String action = WidgetPrefs.shortcutAction(this);
        if (WidgetPrefs.SHORTCUT_NONE.equals(action)) {
            shortcut.setVisibility(View.GONE);
        } else {
            shortcut.setImageResource(WidgetPrefs.SHORTCUT_ASSIST.equals(action)
                    ? R.drawable.ic_assistant : R.drawable.ic_open);
            shortcut.setColorFilter(BarStyle.accent(this));
            shortcut.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    Router.dispatch(ComposeActivity.this,
                            WidgetPrefs.SHORTCUT_ASSIST.equals(
                                    WidgetPrefs.shortcutAction(ComposeActivity.this))
                                    ? UriCatalog.MODE_ASSIST : UriCatalog.MODE_LAUNCH,
                            null, null);
                    finish();
                }
            });
        }

        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                buildList(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH
                        || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                            && event.getAction() == KeyEvent.ACTION_DOWN)) {
                    submit(input.getText().toString());
                    return true;
                }
                return false;
            }
        });

        // Dock the pill over the keyboard: with targetSdk 35 the
        // window is edge-to-edge and adjustResize alone no longer
        // lifts the layout — consume the IME inset as bottom
        // padding on the root so the whole column (pill last)
        // rides above the keyboard, Finder-style.
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            final View root = findViewById(R.id.compose_root);
            root.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @Override
                public android.view.WindowInsets onApplyWindowInsets(
                        View v, android.view.WindowInsets insets) {
                    int ime = insets.getInsets(
                            android.view.WindowInsets.Type.ime()).bottom;
                    int bars = insets.getInsets(
                            android.view.WindowInsets.Type.systemBars()).bottom;
                    v.setPadding(0, 0, 0, Math.max(ime, bars));
                    return insets;
                }
            });
            root.requestApplyInsets();
        }

        buildList("");
        input.requestFocus();
        input.post(new Runnable() {
            @Override public void run() {
                InputMethodManager imm = (InputMethodManager)
                        getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }

    // ---- Suggestions ----

    private void buildList(String rawQuery) {
        list.removeAllViews();
        String q = rawQuery.trim().toLowerCase(Locale.US);
        List<String> recents = loadRecents();

        if (q.isEmpty()) {
            if (!recents.isEmpty()) {
                addSectionHeader(getString(R.string.recent_title), true);
                for (String r : recents) addRow(r, R.drawable.ic_history, true);
            }
            addSectionHeader(getString(R.string.try_asking_title), false);
            for (String s : STARTERS) addRow(s, R.drawable.ic_search, false);
            return;
        }
        // Predictive: matching recents first (prefix beats substring),
        // then matching starters; the typed text itself is row zero —
        // tapping it (or Enter) sends exactly what was typed.
        List<String> recentHits = new ArrayList<>();
        for (String r : recents) {
            String low = r.toLowerCase(Locale.US);
            if (low.startsWith(q)) recentHits.add(0, r);
            else if (low.contains(q)) recentHits.add(r);
        }
        if (!recentHits.isEmpty()) {
            addSectionHeader(getString(R.string.recent_title), false);
            for (String r : recentHits) addRow(r, R.drawable.ic_history, true);
        }
        List<String> starterHits = new ArrayList<>();
        for (String s : STARTERS) {
            if (s.toLowerCase(Locale.US).contains(q) && !recentHits.contains(s)) {
                starterHits.add(s);
            }
        }
        if (!starterHits.isEmpty()) {
            addSectionHeader(getString(R.string.suggestions_title), false);
            for (String s : starterHits) addRow(s, R.drawable.ic_search, false);
        }
        if (recentHits.isEmpty() && starterHits.isEmpty()) {
            TextView hint = new TextView(this);
            hint.setText(R.string.compose_no_suggestions);
            hint.setTextSize(14);
            hint.setTextColor(getColor(R.color.secondary));
            hint.setPadding(0, dp(18), 0, 0);
            list.addView(hint);
        }
    }

    private void addSectionHeader(String title, boolean withClearAll) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(14);
        row.setLayoutParams(lp);
        TextView label = new TextView(this);
        label.setText(title);
        label.setTextSize(14);
        label.setTextColor(getColor(R.color.secondary));
        label.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(label);
        if (withClearAll) {
            TextView clear = new TextView(this);
            clear.setText(R.string.clear_all);
            clear.setTextSize(14);
            clear.setTextColor(getColor(R.color.primary_dark));
            clear.setPadding(dp(8), dp(4), 0, dp(4));
            clear.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    saveRecents(new ArrayList<String>());
                    buildList(input.getText().toString());
                }
            });
            row.addView(clear);
        }
        list.addView(row);
    }

    private void addRow(final String text, int iconRes, boolean recent) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(56));
        row.setPadding(0, dp(6), 0, dp(6));
        row.setClickable(true);
        row.setFocusable(true);
        int[] attrs = {android.R.attr.selectableItemBackground};
        android.content.res.TypedArray ta = obtainStyledAttributes(attrs);
        Drawable ripple = ta.getDrawable(0);
        ta.recycle();
        row.setBackground(ripple);
        row.setContentDescription(text);
        row.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { submit(text); }
        });
        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(getColor(R.color.secondary));
        row.addView(icon, new LinearLayout.LayoutParams(dp(24), dp(24)));
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextSize(16);
        label.setTextColor(getColor(R.color.foreground));
        label.setSingleLine(true);
        label.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        labelLp.setMarginStart(dp(16));
        row.addView(label, labelLp);
        list.addView(row);
    }

    // ---- Submit + history ----

    private void submit(String raw) {
        String text = raw == null ? "" : raw.trim();
        if (!text.isEmpty()) {
            List<String> recents = loadRecents();
            recents.remove(text);
            recents.add(0, text);
            while (recents.size() > MAX_RECENTS) recents.remove(recents.size() - 1);
            saveRecents(recents);
        }
        // Into Muse's composer; empty text just opens the composer.
        if (!Router.assist(this, text.isEmpty() ? null : text)) {
            Router.dispatch(this, UriCatalog.MODE_LAUNCH, null, null);
        }
        finish();
    }

    private List<String> loadRecents() {
        String raw = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_RECENTS, "");
        List<String> out = new ArrayList<>();
        if (raw != null && !raw.isEmpty()) {
            for (String s : raw.split(SEP)) if (!s.isEmpty()) out.add(s);
        }
        return out;
    }

    private void saveRecents(List<String> recents) {
        StringBuilder sb = new StringBuilder();
        for (String s : recents) {
            if (sb.length() > 0) sb.append(SEP);
            sb.append(s);
        }
        SharedPreferences.Editor ed = getSharedPreferences(PREFS, MODE_PRIVATE).edit();
        ed.putString(KEY_RECENTS, sb.toString());
        ed.apply();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
