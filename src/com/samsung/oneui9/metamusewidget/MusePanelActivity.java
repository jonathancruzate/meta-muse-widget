package com.samsung.oneui9.metamusewidget;

import android.Manifest;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * The Muse companion panel: Muse on stock One UI Home.
 *
 * A single first-depth screen, built programmatically in the
 * same pattern as ComposeActivity's rows: viewing area on top
 * (a time-aware greeting and today's date), then One UI focus
 * blocks (bg_card, 18dp radius) — Up next (the next calendar
 * event in 24 hours), Ask Muse (the ask page, chat, and new
 * chat through the existing ungated Router surfaces), and Home
 * controls (the SmartThings and Google Home apps, opened by
 * package; house modes still run through Muse, these rows just
 * open the apps). A footer counter/note closes the screen.
 *
 * Entry points are all in-app: the app-icon shortcut and the
 * Muse QS tile both route here (the shortcut via
 * WidgetCustomizeActivity's widget://shortcut/* routing, the
 * tile directly), so this activity is not exported.
 */
public class MusePanelActivity extends Activity {

    private static final int REQ_READ_CALENDAR = 42;
    private static final String PKG_SMARTTHINGS = "com.samsung.android.oneconnect";
    private static final String PKG_GOOGLE_HOME = "com.google.android.apps.chromecast.app";

    private LinearLayout upNextBody;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.muse_companion_title);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(getColor(R.color.ground));
        scroll.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(24), dp(24), dp(24));
        scroll.addView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(scroll);

        // ---- Viewing area: greeting + date ----
        TextView greeting = new TextView(this);
        greeting.setText(greetingText());
        greeting.setTextSize(32);
        greeting.setTextColor(getColor(R.color.foreground));
        greeting.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        root.addView(greeting);

        TextView date = new TextView(this);
        date.setText(new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
                .format(Calendar.getInstance().getTime()));
        date.setTextSize(15);
        date.setTextColor(getColor(R.color.secondary));
        LinearLayout.LayoutParams dateLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dateLp.topMargin = dp(4);
        date.setLayoutParams(dateLp);
        root.addView(date);

        // ---- Up next ----
        LinearLayout upNextCard = addCard(root, getString(R.string.up_next));
        upNextBody = new LinearLayout(this);
        upNextBody.setOrientation(LinearLayout.VERTICAL);
        upNextCard.addView(upNextBody);
        refreshUpNext();

        // ---- Ask Muse ----
        LinearLayout askCard = addCard(root, getString(R.string.muse_companion_ask));
        addLogoRow(askCard, getString(R.string.muse_companion_ask), new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(MusePanelActivity.this, ComposeActivity.class));
            }
        });
        addRow(askCard, getString(R.string.muse_companion_chat), R.drawable.ic_chat,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        Router.assist(MusePanelActivity.this, null);
                    }
                });
        addRow(askCard, getString(R.string.sc_new), R.drawable.ic_plus,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        // The catalog's new-thread mode is the assist
                        // surface (see UriCatalog: hatch://thread/new
                        // defaults to MODE_ASSIST) — reuse it, do not
                        // invent a new Muse deeplink.
                        Router.dispatch(MusePanelActivity.this,
                                UriCatalog.MODE_ASSIST, null, null);
                    }
                });

        // ---- Home controls ----
        LinearLayout homeCard = addCard(root, getString(R.string.home_controls));
        addRow(homeCard, getString(R.string.smartthings), R.drawable.ic_home,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        launchPackage(PKG_SMARTTHINGS, getString(R.string.smartthings));
                    }
                });
        addRow(homeCard, getString(R.string.google_home), R.drawable.ic_home,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        launchPackage(PKG_GOOGLE_HOME, getString(R.string.google_home));
                    }
                });
        TextView homeNote = new TextView(this);
        homeNote.setText(R.string.home_controls_note);
        homeNote.setTextSize(13);
        homeNote.setTextColor(getColor(R.color.secondary));
        LinearLayout.LayoutParams noteLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        noteLp.topMargin = dp(4);
        homeNote.setLayoutParams(noteLp);
        homeCard.addView(homeNote);

        // ---- Footer ----
        TextView footer = new TextView(this);
        footer.setText(R.string.muse_companion_footer);
        footer.setTextSize(13);
        footer.setTextColor(getColor(R.color.secondary));
        footer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams footerLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        footerLp.topMargin = dp(20);
        footer.setLayoutParams(footerLp);
        root.addView(footer);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshUpNext();
    }

    // ---- Greeting ----

    private String greetingText() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int res = hour < 12 ? R.string.muse_companion_greeting_morning
                : hour < 18 ? R.string.muse_companion_greeting_afternoon
                : R.string.muse_companion_greeting_evening;
        return getString(res);
    }

    // ---- Up next (CalendarContract.Instances) ----

    private void refreshUpNext() {
        if (upNextBody == null) return;
        upNextBody.removeAllViews();
        if (checkSelfPermission(Manifest.permission.READ_CALENDAR)
                != PackageManager.PERMISSION_GRANTED) {
            addRow(upNextBody, getString(R.string.show_next_event), R.drawable.ic_event,
                    new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            requestPermissions(
                                    new String[]{Manifest.permission.READ_CALENDAR},
                                    REQ_READ_CALENDAR);
                        }
                    });
            return;
        }
        String[] event = queryNextEvent();
        if (event == null) {
            addBodyText(upNextBody, getString(R.string.nothing_upcoming));
            return;
        }
        addBodyText(upNextBody, event[0]);
        if (event[1] != null && !event[1].isEmpty()) {
            addSubText(upNextBody, event[1]);
        }
        if (event[2] != null && !event[2].isEmpty()) {
            addSubText(upNextBody, event[2]);
        }
    }

    /** @return {title, formatted start, location} or null for the empty state. */
    private String[] queryNextEvent() {
        Cursor cursor = null;
        try {
            long now = System.currentTimeMillis();
            long end = now + 24L * 60L * 60L * 1000L;
            Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();
            ContentUris.appendId(builder, now);
            ContentUris.appendId(builder, end);
            Uri uri = builder.build();
            String[] projection = {
                    CalendarContract.Instances.TITLE,
                    CalendarContract.Instances.BEGIN,
                    CalendarContract.Instances.EVENT_LOCATION,
            };
            cursor = getContentResolver().query(uri, projection, null, null, null);
            if (cursor == null || !cursor.moveToFirst()) return null;
            String title = cursor.getString(0);
            if (title == null || title.isEmpty()) title = getString(R.string.nothing_upcoming);
            long begin = cursor.getLong(1);
            String when = DateFormat.getDateTimeInstance(
                    DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault())
                    .format(new java.util.Date(begin));
            String location = cursor.getString(2);
            return new String[]{title, when, location};
        } catch (Exception e) {
            return null; // any calendar failure degrades to the empty state
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_READ_CALENDAR) refreshUpNext();
    }

    // ---- Home controls ----

    private void launchPackage(String pkg, String label) {
        try {
            Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
            if (i == null) throw new Exception("no launch intent");
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.app_not_found, label),
                    Toast.LENGTH_SHORT).show();
        }
    }

    // ---- Building blocks (ComposeActivity's row pattern) ----

    private LinearLayout addCard(LinearLayout root, String title) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card);
        card.setPadding(dp(18), dp(14), dp(18), dp(14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(16);
        card.setLayoutParams(lp);
        root.addView(card);
        TextView header = new TextView(this);
        header.setText(title);
        header.setTextSize(16);
        header.setTextColor(getColor(R.color.foreground));
        header.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        card.addView(header);
        return card;
    }

    private void addRow(LinearLayout parent, final String text, int iconRes,
                        View.OnClickListener onClick) {
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
        row.setOnClickListener(onClick);
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
        parent.addView(row);
    }

    /** The Ask Muse row: the Muse M logo in the accent tint, not a glyph. */
    private void addLogoRow(LinearLayout parent, final String text,
                            View.OnClickListener onClick) {
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
        row.setOnClickListener(onClick);
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.ic_muse_m);
        logo.setColorFilter(getColor(R.color.primary));
        logo.setContentDescription(getString(R.string.muse_logo_desc));
        row.addView(logo, new LinearLayout.LayoutParams(dp(34), dp(24)));
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
        parent.addView(row);
    }

    private void addBodyText(LinearLayout parent, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(16);
        tv.setTextColor(getColor(R.color.foreground));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(6);
        tv.setLayoutParams(lp);
        parent.addView(tv);
    }

    private void addSubText(LinearLayout parent, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(14);
        tv.setTextColor(getColor(R.color.secondary));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(2);
        tv.setLayoutParams(lp);
        parent.addView(tv);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
