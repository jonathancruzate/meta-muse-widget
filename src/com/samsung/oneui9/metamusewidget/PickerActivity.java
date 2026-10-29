package com.samsung.oneui9.metamusewidget;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

/**
 * The activity picker (v4.9.0): the catalog's destinations as a
 * browsable list again — the old hub, rebuilt for how Muse
 * actually accepts input from a third-party app. Muse gates every
 * payload deeplink by caller signature, so no row fires a URI.
 * Instead a row opens the ask page with a prompt stem already
 * typed, the way Muse's own Goals and Tracking flows start
 * ("I want to start a goal about ", "I want to start tracking "):
 * finish the sentence, Enter carries it into Muse through the
 * ungated ASSIST_QUERY surface. The few rows whose surface is
 * ungated (Voice, Assistant, a plain app launch) fire directly.
 *
 * One first-depth screen, built programmatically in the panel's
 * pattern: grouped sections, icon rows with the stem previewed on
 * a second line, and a bottom counter (One UI list pattern).
 */
public class PickerActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.picker_title);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(getColor(R.color.ground));
        scroll.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(16), dp(24), dp(24));
        scroll.addView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(scroll);

        List<UriCatalog.Entry> entries = UriCatalog.pickerEntries();
        String lastGroup = null;
        for (final UriCatalog.Entry e : entries) {
            if (!e.group.equals(lastGroup)) {
                lastGroup = e.group;
                addSectionHeader(root, e.group);
            }
            addRow(root, e);
        }

        // One UI list pattern: a bottom counter closes the list.
        TextView counter = new TextView(this);
        counter.setText(getString(R.string.picker_count, entries.size()));
        counter.setTextSize(13);
        counter.setTextColor(getColor(R.color.secondary));
        counter.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams counterLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        counterLp.topMargin = dp(20);
        counter.setLayoutParams(counterLp);
        root.addView(counter);
    }

    private void addSectionHeader(LinearLayout root, String title) {
        TextView label = new TextView(this);
        label.setText(title);
        label.setTextSize(14);
        label.setTextColor(getColor(R.color.secondary));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(14);
        label.setLayoutParams(lp);
        root.addView(label);
    }

    private void addRow(LinearLayout root, final UriCatalog.Entry e) {
        final int action = UriCatalog.pickerAction(e);
        final String prompt = UriCatalog.pickerPrompt(e);

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
        row.setContentDescription(prompt != null && !prompt.isEmpty()
                ? e.label + ": " + prompt : e.label);
        row.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (action == UriCatalog.PICKER_DIRECT) {
                    // Voice and Assistant ride the assist surface;
                    // Open Muse app is a plain launch (its mode).
                    if ("Voice".equals(e.label) || "Assistant".equals(e.label)) {
                        Router.dispatch(PickerActivity.this,
                                UriCatalog.MODE_ASSIST, null, null);
                    } else {
                        Router.dispatch(PickerActivity.this, e.mode, e.uri, e.fallbackUri);
                    }
                } else {
                    Intent intent = new Intent(PickerActivity.this, ComposeActivity.class);
                    intent.putExtra(ComposeActivity.EXTRA_PREFILL, prompt);
                    startActivity(intent);
                }
                finish();
            }
        });

        ImageView icon = new ImageView(this);
        icon.setImageResource(e.icon);
        icon.setColorFilter(getColor(R.color.secondary));
        row.addView(icon, new LinearLayout.LayoutParams(dp(24), dp(24)));

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textsLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textsLp.setMarginStart(dp(16));
        row.addView(texts, textsLp);

        TextView label = new TextView(this);
        label.setText(e.label);
        label.setTextSize(16);
        label.setTextColor(getColor(R.color.foreground));
        label.setSingleLine(true);
        label.setEllipsize(android.text.TextUtils.TruncateAt.END);
        texts.addView(label);

        if (prompt != null && !prompt.isEmpty()) {
            TextView sub = new TextView(this);
            sub.setText(promptPreview(prompt));
            sub.setTextSize(13);
            sub.setTextColor(getColor(R.color.secondary));
            sub.setSingleLine(true);
            sub.setEllipsize(android.text.TextUtils.TruncateAt.END);
            texts.addView(sub);
        }
        root.addView(row);
    }

    /** A stem that trails off (space/colon) previews with an ellipsis. */
    private static String promptPreview(String prompt) {
        String p = prompt.trim();
        if (prompt.endsWith(" ") || prompt.endsWith(":")) return p + " …";
        return p;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
