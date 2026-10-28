package com.samsung.oneui9.metamusewidget;

import android.Manifest;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Widget-side shaping of the panel JSON (v4.8.0). The Muse panel
 * WIDGET renders the same live data as MusePanelActivity through
 * {@link PanelDataClient}; this class turns a fetch {@code Result}
 * into the plain strings the RemoteViews layouts bind, mirroring
 * the activity's rendering semantics exactly (design §6): the
 * brief's age is stated, the house mode always carries its report
 * age in words, decide items appear only as the "N waiting on
 * you" count, and empty states are explicit text, never blanks.
 *
 * The activity keeps its own programmatic rendering; this helper
 * exists so the provider never reaches into activity internals.
 * All methods are static and side-effect free except the calendar
 * query, which degrades to null on any failure.
 */
final class PanelWidgetData {

    /** Widget-ready strings for one panel render. */
    static final class Model {
        /** Text for the headline slot (brief headline, or a state line). */
        String headline;
        /** Brief meta / cache note under the headline, or null. */
        String statusLine;
        /** Highlight (title, detail) pairs, in brief order. */
        final List<String[]> highlights = new ArrayList<>();
        /** "N waiting on you" when decide_count &gt; 0, else null. */
        String decideText;
        /** House status title line (never null). */
        String houseTitle;
        /** House status subtext (report age), or null. */
        String houseSub;
    }

    private PanelWidgetData() {}

    static Model fromResult(Context ctx, PanelDataClient.Result r) {
        Model m = new Model();
        if (r == null || r.status == PanelDataClient.NOT_CONFIGURED) {
            m.headline = ctx.getString(R.string.panel_not_configured);
            m.houseTitle = ctx.getString(R.string.panel_house_not_reported);
            return m;
        }
        if (r.status == PanelDataClient.LINK_EXPIRED) {
            m.headline = ctx.getString(R.string.panel_link_expired);
            m.houseTitle = ctx.getString(R.string.panel_house_not_reported);
            return m;
        }
        if (r.data == null) {
            m.headline = ctx.getString(R.string.panel_unavailable);
            m.houseTitle = ctx.getString(R.string.panel_house_not_reported);
            return m;
        }
        // ---- Brief ----
        JSONObject brief = r.data.optJSONObject("brief");
        if (brief == null) {
            m.headline = ctx.getString(R.string.panel_no_brief);
        } else {
            String headline = brief.optString("headline", "");
            m.headline = headline.isEmpty()
                    ? ctx.getString(R.string.panel_no_brief) : headline;
            JSONArray highlights = brief.optJSONArray("highlights");
            if (highlights != null) {
                for (int i = 0; i < highlights.length(); i++) {
                    JSONObject h = highlights.optJSONObject(i);
                    if (h == null) continue;
                    String title = h.optString("title", "");
                    String detail = h.optString("detail", "");
                    if (title.isEmpty() && detail.isEmpty()) continue;
                    m.highlights.add(new String[]{title, detail});
                }
            }
            // The brief's own date/time — its age is stated, never
            // implied fresh (design §6), same strings as the panel.
            String date = brief.optString("date", "");
            Date generated = parseIso(brief.optString("generated_at", ""));
            if (generated != null) {
                String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US)
                        .format(new Date());
                if (!date.isEmpty() && !date.equals(today)) {
                    Date briefDate = parseDay(date);
                    String dayText = briefDate != null
                            ? new SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
                                    .format(briefDate)
                            : date;
                    m.statusLine = ctx.getString(R.string.panel_brief_from_date,
                            dayText, timeText(generated));
                } else {
                    m.statusLine = ctx.getString(R.string.panel_brief_from,
                            timeText(generated));
                }
            }
            int decideCount = brief.optInt("decide_count", 0);
            if (decideCount > 0) {
                m.decideText = ctx.getString(R.string.panel_waiting_on_you, decideCount);
            }
        }
        if (r.fromCache) {
            String cached = ctx.getString(R.string.panel_showing_cached,
                    timeText(new Date(r.fetchedAt)));
            m.statusLine = m.statusLine == null ? cached : m.statusLine + " · " + cached;
        }
        // ---- House ----
        fillHouse(ctx, r.data.optJSONObject("house"), m);
        return m;
    }

    /**
     * House status, mirroring MusePanelActivity.renderHouse
     * (approved default 3): desired-vs-actual when out of sync;
     * actual + report age otherwise; past 30 minutes the age is
     * the explicit stale form — in words, never colour alone.
     */
    private static void fillHouse(Context ctx, JSONObject house, Model m) {
        if (house == null) {
            m.houseTitle = ctx.getString(R.string.panel_house_not_reported);
            return;
        }
        String mode = house.isNull("mode") ? null : house.optString("mode", null);
        if (mode == null || mode.isEmpty()) {
            m.houseTitle = ctx.getString(R.string.panel_house_not_reported);
            return;
        }
        String desired = house.isNull("desired_mode")
                ? null : house.optString("desired_mode", null);
        boolean inSync = house.optBoolean("in_sync", false);
        Date reported = parseIso(house.optString("reported_at", ""));
        String age = reported != null ? ageText(ctx, reported) : null;
        boolean stale = reported != null
                && System.currentTimeMillis() - reported.getTime() > 30 * 60 * 1000L;
        if (!inSync && desired != null && !desired.isEmpty() && !desired.equals(mode)) {
            m.houseTitle = ctx.getString(R.string.panel_house_out_of_sync,
                    modeLabel(desired), modeLabel(mode));
            if (age != null) {
                m.houseSub = stale
                        ? ctx.getString(R.string.panel_house_last_reported, age)
                        : ctx.getString(R.string.panel_house_reported, age);
            }
        } else if (age == null) {
            m.houseTitle = modeLabel(mode);
        } else if (stale) {
            m.houseTitle = modeLabel(mode);
            m.houseSub = ctx.getString(R.string.panel_house_last_reported, age);
        } else {
            m.houseTitle = ctx.getString(R.string.panel_house_recent, modeLabel(mode), age);
        }
    }

    // ---- Greeting / date (full density header, the panel's own) ----

    static String greetingText(Context ctx) {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int res = hour < 12 ? R.string.muse_companion_greeting_morning
                : hour < 18 ? R.string.muse_companion_greeting_afternoon
                : R.string.muse_companion_greeting_evening;
        return ctx.getString(res);
    }

    static String dateText() {
        return new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
                .format(Calendar.getInstance().getTime());
    }

    // ---- Up next (full density only) ----

    static boolean hasCalendarPermission(Context ctx) {
        return ctx.checkSelfPermission(Manifest.permission.READ_CALENDAR)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * The next calendar event in 24 hours, the panel's own query.
     *
     * @return {title, formatted start, location} or null when there
     *         is none — and null on ANY failure: a widget degrades
     *         silently, it never crashes and never asks (the panel
     *         owns the permission ask).
     */
    static String[] queryNextEvent(Context ctx) {
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
            cursor = ctx.getContentResolver().query(uri, projection, null, null, null);
            if (cursor == null || !cursor.moveToFirst()) return null;
            String title = cursor.getString(0);
            if (title == null || title.isEmpty()) {
                title = ctx.getString(R.string.nothing_upcoming);
            }
            long begin = cursor.getLong(1);
            String when = DateFormat.getDateTimeInstance(
                    DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault())
                    .format(new Date(begin));
            String location = cursor.getString(2);
            return new String[]{title, when, location};
        } catch (Exception e) {
            return null;
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    // ---- Formatting helpers (the panel's, mirrored) ----

    private static String modeLabel(String mode) {
        if (mode == null || mode.isEmpty()) return "";
        return mode.substring(0, 1).toUpperCase(Locale.US) + mode.substring(1);
    }

    private static String ageText(Context ctx, Date when) {
        long mins = Math.max(0L,
                (System.currentTimeMillis() - when.getTime()) / 60000L);
        if (mins < 1) return ctx.getString(R.string.panel_age_now);
        if (mins < 60) return ctx.getString(R.string.panel_age_min, (int) mins);
        if (mins < 24 * 60) return ctx.getString(R.string.panel_age_hours, (int) (mins / 60));
        return ctx.getString(R.string.panel_age_days, (int) (mins / (24 * 60)));
    }

    private static String timeText(Date when) {
        return new SimpleDateFormat("h:mm a", Locale.getDefault()).format(when);
    }

    /** Parse the Worker's ISO-8601 UTC timestamps (with or without millis). */
    private static Date parseIso(String s) {
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

    private static Date parseDay(String s) {
        try {
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            f.setTimeZone(TimeZone.getTimeZone("UTC"));
            return f.parse(s);
        } catch (Exception e) {
            return null;
        }
    }
}
