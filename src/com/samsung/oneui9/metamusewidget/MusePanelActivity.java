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
import android.os.Handler;
import android.os.Looper;
import android.provider.CalendarContract;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

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

    // Live-data lane (v4.7.0): Muse today card + house status row.
    private LinearLayout todayBody;
    private LinearLayout houseBody;
    private TextView refreshLabel;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private long lastFetchStartMs;
    private boolean fetchInFlight;
    private PanelDataClient.Result lastPanelResult;

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

        // ---- Muse today (live data: brief distillate) ----
        LinearLayout todayCard = addCard(root, getString(R.string.panel_today));
        todayBody = new LinearLayout(this);
        todayBody.setOrientation(LinearLayout.VERTICAL);
        todayCard.addView(todayBody);
        refreshLabel = new TextView(this);
        refreshLabel.setText(R.string.panel_refresh);
        refreshLabel.setTextSize(15);
        refreshLabel.setTextColor(getColor(R.color.primary));
        refreshLabel.setClickable(true);
        refreshLabel.setFocusable(true);
        refreshLabel.setContentDescription(getString(R.string.panel_refresh));
        LinearLayout.LayoutParams refreshLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        refreshLp.topMargin = dp(8);
        refreshLabel.setLayoutParams(refreshLp);
        refreshLabel.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                maybeFetchPanel(true);
            }
        });
        todayCard.addView(refreshLabel);
        // Instant first paint from the last-good cache; onResume fetches.
        PanelDataClient.Result cached = PanelDataClient.loadCache(this);
        if (cached != null) {
            renderPanel(cached);
        } else if (!PanelDataClient.isConfigured()) {
            PanelDataClient.Result r = new PanelDataClient.Result();
            r.status = PanelDataClient.NOT_CONFIGURED;
            renderPanel(r);
        } else {
            renderFetching();
        }

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
        addRow(askCard, getString(R.string.picker_title), R.drawable.ic_grid,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        // The activity picker (v4.9.0): destinations
                        // as prompt stems on the ask page, not URIs.
                        startActivity(new Intent(MusePanelActivity.this,
                                PickerActivity.class));
                    }
                });

        // ---- Home controls (house status row first) ----
        LinearLayout homeCard = addCard(root, getString(R.string.home_controls));
        houseBody = new LinearLayout(this);
        houseBody.setOrientation(LinearLayout.VERTICAL);
        homeCard.addView(houseBody);
        renderHouse(lastPanelResult != null ? lastPanelResult.data : null);
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
        maybeFetchPanel(false);
    }

    // ---- Live data (brief distillate + house mode) ----

    /**
     * Fetch the panel JSON: on panel open (throttled to one fetch
     * per 5 minutes of panel time) and from the Refresh row (always).
     * No background polling — panel-open freshness is the use case
     * (design §6).
     */
    private void maybeFetchPanel(boolean force) {
        if (!PanelDataClient.isConfigured() || fetchInFlight) return;
        long now = System.currentTimeMillis();
        if (!force && lastFetchStartMs > 0 && now - lastFetchStartMs < 5 * 60 * 1000L) return;
        lastFetchStartMs = now;
        fetchInFlight = true;
        if (refreshLabel != null) refreshLabel.setText(R.string.panel_refreshing);
        if (lastPanelResult == null || lastPanelResult.data == null) renderFetching();
        new Thread(new Runnable() {
            @Override public void run() {
                final PanelDataClient.Result r = PanelDataClient.fetch(MusePanelActivity.this);
                mainHandler.post(new Runnable() {
                    @Override public void run() {
                        fetchInFlight = false;
                        if (refreshLabel != null) refreshLabel.setText(R.string.panel_refresh);
                        renderPanel(r);
                        // The fetch just refreshed the shared cache:
                        // poke the home-screen panel widget (v4.8.0)
                        // so it re-renders from the same fresh data.
                        if (r.status == PanelDataClient.OK && !r.fromCache) {
                            MusePanelWidgetProvider.poke(MusePanelActivity.this);
                        }
                    }
                });
            }
        }).start();
    }

    /** In-place progress inside the Muse today card; never full-screen. */
    private void renderFetching() {
        if (todayBody == null) return;
        todayBody.removeAllViews();
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(6), 0, dp(6));
        ProgressBar spinner = new ProgressBar(this);
        row.addView(spinner, new LinearLayout.LayoutParams(dp(24), dp(24)));
        TextView label = new TextView(this);
        label.setText(R.string.panel_refreshing);
        label.setTextSize(14);
        label.setTextColor(getColor(R.color.secondary));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMarginStart(dp(12));
        label.setLayoutParams(lp);
        row.addView(label);
        todayBody.addView(row);
    }

    private void renderPanel(PanelDataClient.Result r) {
        lastPanelResult = r;
        if (todayBody == null) return;
        todayBody.removeAllViews();
        if (r.status == PanelDataClient.NOT_CONFIGURED) {
            addBodyText(todayBody, getString(R.string.panel_not_configured));
        } else if (r.status == PanelDataClient.LINK_EXPIRED) {
            addBodyText(todayBody, getString(R.string.panel_link_expired));
        } else if (r.data == null) {
            addBodyText(todayBody, getString(R.string.panel_unavailable));
        } else {
            renderBrief(r.data.optJSONObject("brief"));
            if (r.fromCache) {
                addSubText(todayBody, getString(R.string.panel_showing_cached,
                        timeText(new Date(r.fetchedAt))));
            }
        }
        renderHouse(r.data);
    }

    private void renderBrief(JSONObject brief) {
        if (brief == null) {
            addBodyText(todayBody, getString(R.string.panel_no_brief));
            return;
        }
        String headline = brief.optString("headline", "");
        if (!headline.isEmpty()) addBodyText(todayBody, headline);
        JSONArray highlights = brief.optJSONArray("highlights");
        if (highlights != null) {
            for (int i = 0; i < highlights.length(); i++) {
                JSONObject h = highlights.optJSONObject(i);
                if (h == null) continue;
                String title = h.optString("title", "");
                String detail = h.optString("detail", "");
                if (!title.isEmpty()) addBodyText(todayBody, title);
                if (!detail.isEmpty()) addSubText(todayBody, detail);
            }
        }
        // Meta line: the brief's own date/time — its age is stated,
        // never implied fresh (design §6).
        String date = brief.optString("date", "");
        Date generated = parseIso(brief.optString("generated_at", ""));
        if (generated != null) {
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
            if (!date.isEmpty() && !date.equals(today)) {
                Date briefDate = parseDay(date);
                String dayText = briefDate != null
                        ? new SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(briefDate)
                        : date;
                addSubText(todayBody, getString(R.string.panel_brief_from_date,
                        dayText, timeText(generated)));
            } else {
                addSubText(todayBody, getString(R.string.panel_brief_from,
                        timeText(generated)));
            }
        }
        int decideCount = brief.optInt("decide_count", 0);
        if (decideCount > 0) {
            addSubText(todayBody, getString(R.string.panel_waiting_on_you, decideCount));
        }
    }

    /**
     * House status row (first row of Home controls). Default (3):
     * desired-vs-actual when out of sync; actual + report age
     * otherwise. Past 30 minutes the age becomes the explicit stale
     * form — in words, never colour alone (design §6/§7).
     */
    private void renderHouse(JSONObject panelData) {
        if (houseBody == null) return;
        houseBody.removeAllViews();
        JSONObject house = panelData != null ? panelData.optJSONObject("house") : null;
        if (house == null) {
            if (lastPanelResult != null
                    && lastPanelResult.status == PanelDataClient.LINK_EXPIRED) {
                addStatusRow(houseBody, getString(R.string.panel_link_expired), null);
            } else if (lastPanelResult != null
                    && lastPanelResult.status == PanelDataClient.NOT_CONFIGURED) {
                addStatusRow(houseBody, getString(R.string.panel_not_configured), null);
            } else if (panelData == null && lastPanelResult == null) {
                addStatusRow(houseBody, getString(R.string.panel_house_not_reported), null);
            } else {
                addStatusRow(houseBody, getString(R.string.panel_unavailable), null);
            }
            return;
        }
        String mode = house.isNull("mode") ? null : house.optString("mode", null);
        if (mode == null || mode.isEmpty()) {
            addStatusRow(houseBody, getString(R.string.panel_house_not_reported), null);
            return;
        }
        String desired = house.isNull("desired_mode")
                ? null : house.optString("desired_mode", null);
        boolean inSync = house.optBoolean("in_sync", false);
        Date reported = parseIso(house.optString("reported_at", ""));
        String age = reported != null ? ageText(reported) : null;
        boolean stale = reported != null
                && System.currentTimeMillis() - reported.getTime() > 30 * 60 * 1000L;
        if (!inSync && desired != null && !desired.isEmpty() && !desired.equals(mode)) {
            String sub = null;
            if (age != null) {
                sub = stale ? getString(R.string.panel_house_last_reported, age)
                        : getString(R.string.panel_house_reported, age);
            }
            addStatusRow(houseBody,
                    getString(R.string.panel_house_out_of_sync, modeLabel(desired), modeLabel(mode)),
                    sub);
        } else if (age == null) {
            addStatusRow(houseBody, modeLabel(mode), null);
        } else if (stale) {
            addStatusRow(houseBody, modeLabel(mode),
                    getString(R.string.panel_house_last_reported, age));
        } else {
            addStatusRow(houseBody,
                    getString(R.string.panel_house_recent, modeLabel(mode), age), null);
        }
    }

    /** Icon + title (+ optional subtext) row, the house status pattern. */
    private void addStatusRow(LinearLayout parent, String title, String subtext) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(56));
        row.setPadding(0, dp(6), 0, dp(6));
        row.setContentDescription(subtext != null ? title + ". " + subtext : title);
        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.ic_home);
        icon.setColorFilter(getColor(R.color.secondary));
        row.addView(icon, new LinearLayout.LayoutParams(dp(24), dp(24)));
        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textsLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textsLp.setMarginStart(dp(16));
        texts.setLayoutParams(textsLp);
        TextView titleTv = new TextView(this);
        titleTv.setText(title);
        titleTv.setTextSize(16);
        titleTv.setTextColor(getColor(R.color.foreground));
        texts.addView(titleTv);
        if (subtext != null && !subtext.isEmpty()) {
            TextView subTv = new TextView(this);
            subTv.setText(subtext);
            subTv.setTextSize(14);
            subTv.setTextColor(getColor(R.color.secondary));
            texts.addView(subTv);
        }
        row.addView(texts);
        parent.addView(row);
    }

    private String modeLabel(String mode) {
        if (mode == null || mode.isEmpty()) return "";
        return mode.substring(0, 1).toUpperCase(Locale.US) + mode.substring(1);
    }

    private String ageText(Date when) {
        long mins = Math.max(0L,
                (System.currentTimeMillis() - when.getTime()) / 60000L);
        if (mins < 1) return getString(R.string.panel_age_now);
        if (mins < 60) return getString(R.string.panel_age_min, (int) mins);
        if (mins < 24 * 60) return getString(R.string.panel_age_hours, (int) (mins / 60));
        return getString(R.string.panel_age_days, (int) (mins / (24 * 60)));
    }

    private String timeText(Date when) {
        return new SimpleDateFormat("h:mm a", Locale.getDefault()).format(when);
    }

    /** Parse the Worker's ISO-8601 UTC timestamps (with or without millis). */
    private Date parseIso(String s) {
        if (s == null || s.isEmpty()) return null;
        String[] patterns = {"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd'T'HH:mm:ss'Z'"};
        for (String p : patterns) {
            try {
                SimpleDateFormat f = new SimpleDateFormat(p, Locale.US);
                f.setTimeZone(TimeZone.getTimeZone("UTC"));
                return f.parse(s);
            } catch (Exception ignored) {
                // try the next pattern
            }
        }
        return null;
    }

    private Date parseDay(String s) {
        try {
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            f.setTimeZone(TimeZone.getTimeZone("UTC"));
            return f.parse(s);
        } catch (Exception e) {
            return null;
        }
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
