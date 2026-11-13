package com.jonathancruzate.metamusewidget;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Feed bundle v2 shaping for the Muse Launcher Pack tiles
 * (design Section 8). Reads the SAME merged JSON the panel lane
 * already delivers (PanelDataClient cache/fetch, user-configured
 * endpoint, distillate only) and turns it into per-kind tile
 * content. v2 fields (up_next / transit / money, feed_version)
 * are read when present and degrade to designed empty states
 * when absent, so a v1-shaped bundle still renders every tile.
 * Every value carries its feed's own timestamp into the age
 * line; nothing here fakes freshness.
 */
final class PackData {

    static final class TileContent {
        String value = "";
        String body = "";
        String age = "";
    }

    private PackData() {}

    /** The cached last-good bundle, or null. */
    static JSONObject cachedData(Context ctx) {
        PanelDataClient.Result r = PanelDataClient.loadCache(ctx);
        return r == null ? null : r.data;
    }

    static TileContent contentFor(Context ctx, String kind,
                                  JSONObject data) {
        TileContent c = new TileContent();
        if ("clock".equals(kind)) {
            c.value = new SimpleDateFormat("h:mm a", Locale.getDefault())
                    .format(new Date());
            c.body = PanelWidgetData.dateText();
            return c;
        }
        if ("voice".equals(kind)) {
            c.value = ctx.getString(R.string.pack_voice_value);
            c.body = ctx.getString(PackPrefs.daPaired(ctx)
                    ? R.string.pack_voice_body
                    : R.string.pack_voice_unpaired);
            return c;
        }
        if ("media".equals(kind)) {
            String media = PackPrefs.contextMedia(ctx);
            c.value = media == null
                    ? ctx.getString(R.string.pack_media_idle) : media;
            return c;
        }
        if ("sleep".equals(kind)) {
            String sleep = PackPrefs.sleepSummary(ctx);
            c.value = sleep == null
                    ? ctx.getString(R.string.pack_sleep_none) : sleep;
            return c;
        }
        if ("setup".equals(kind)) {
            boolean ok = PackDrift.layoutMatchesSpec(ctx);
            c.value = ctx.getString(ok
                    ? R.string.pack_setup_ok : R.string.pack_setup_drift);
            return c;
        }
        if (data == null) {
            c.value = ctx.getString(R.string.pack_no_data);
            return c;
        }
        PanelWidgetData.Model model = PanelWidgetData.fromResult(ctx,
                wrapResult(data));
        switch (kind) {
            case "brief": {
                c.value = model.headline;
                StringBuilder b = new StringBuilder();
                for (int i = 0; i < model.highlights.size()
                        && i < 3; i++) {
                    if (b.length() > 0) b.append("\n");
                    b.append(model.highlights.get(i)[0]);
                }
                c.body = b.toString();
                if (model.statusLine != null) c.age = model.statusLine;
                break;
            }
            case "decide": {
                JSONObject brief = data.optJSONObject("brief");
                int n = brief == null ? 0
                        : brief.optInt("decide_count", 0);
                c.value = n > 0 ? String.valueOf(n)
                        : ctx.getString(R.string.pack_decide_none);
                c.body = n > 0 ? ctx.getString(
                        R.string.panel_waiting_on_you, n) : "";
                break;
            }
            case "house": {
                c.value = model.houseTitle;
                c.body = model.houseSub == null ? "" : model.houseSub;
                break;
            }
            case "up_next": {
                String[] ev = PanelWidgetData.hasCalendarPermission(ctx)
                        ? PanelWidgetData.queryNextEvent(ctx) : null;
                if (ev != null) {
                    c.value = ev[0];
                    c.body = ev[1]
                            + (ev[2] != null && !ev[2].isEmpty()
                                    ? " · " + ev[2] : "");
                    break;
                }
                JSONObject upNext = data.optJSONObject("up_next");
                if (upNext != null) {
                    c.value = upNext.optString("title",
                            ctx.getString(R.string.pack_transit_none));
                    String start = upNext.optString("start_text", "");
                    String leaveBy = upNext.optString("leave_by", "");
                    c.body = !leaveBy.isEmpty()
                            ? ctx.getString(R.string.pack_leave_by,
                                    leaveBy)
                            : start;
                    c.age = ageOf(upNext.optString("updated_at", ""));
                } else {
                    c.value = ctx.getString(
                            R.string.pack_transit_none);
                }
                break;
            }
            case "transit": {
                JSONObject transit = data.optJSONObject("transit");
                if (transit != null) {
                    c.value = transit.optString("summary",
                            ctx.getString(R.string.pack_transit_none));
                    String leaveBy = transit.optString("leave_by", "");
                    c.body = leaveBy.isEmpty() ? ""
                            : ctx.getString(R.string.pack_leave_by,
                                    leaveBy);
                    c.age = ageOf(transit.optString("updated_at", ""));
                } else {
                    return contentFor(ctx, "up_next", data);
                }
                break;
            }
            case "money": {
                JSONObject money = data.optJSONObject("money");
                if (money != null) {
                    c.value = money.optString("summary", "");
                    c.body = money.optString("detail", "");
                    c.age = ageOf(money.optString("updated_at", ""));
                } else {
                    c.value = ctx.getString(R.string.pack_no_data);
                }
                break;
            }
            case "people": {
                JSONObject house = data.optJSONObject("house");
                if (house != null && house.has("stephany_present")) {
                    c.value = ctx.getString(
                            house.optBoolean("stephany_present")
                                    ? R.string.pack_people_stephany_home
                                    : R.string.pack_people_stephany_away);
                } else {
                    c.value = ctx.getString(
                            R.string.pack_people_unknown);
                }
                break;
            }
            default:
                c.value = model.headline;
                break;
        }
        return c;
    }

    /** Wrap a bare bundle the way PanelWidgetData expects. */
    private static PanelDataClient.Result wrapResult(JSONObject data) {
        PanelDataClient.Result r = new PanelDataClient.Result();
        r.status = PanelDataClient.OK;
        r.data = data;
        return r;
    }

    /** Honest age text for an ISO-8601 UTC timestamp, or "". */
    static String ageOf(String iso) {
        Date d = parseIso(iso);
        if (d == null) return "";
        long mins = Math.max(0L,
                (System.currentTimeMillis() - d.getTime()) / 60000L);
        if (mins < 1) return "just now";
        if (mins < 60) return mins + " min ago";
        if (mins < 24 * 60) return (mins / 60) + " h ago";
        return (mins / (24 * 60)) + " d ago";
    }

    static Date parseIso(String s) {
        if (s == null || s.isEmpty()) return null;
        String[] patterns = {
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'"};
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
}
