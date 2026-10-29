package com.samsung.oneui9.metamusewidget;

import android.content.Context;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Read-only client for the Muse panel's live-data lane (v4.7.0,
 * LIVE-DATA-LANE-DESIGN.md §4–§6).
 *
 * Fetches ONE merged JSON (Daily Brief distillate + house mode) from
 * the mode-sync Worker's capability-gated /panel-data/v1/&lt;slug&gt;
 * endpoint. The URL — slug included — is not in source: build.sh
 * generates PanelConfig from a local 0600 file at build time, and an
 * empty URL means this build carries no live-data link (the panel
 * shows its "not configured" state). No Worker credential of any
 * kind is ever held by this class; the slug opens this read-only,
 * pre-redacted payload and nothing else.
 *
 * The last good response is cached in app-private storage with the
 * client-side fetch time. The cache is a failure fallback, never a
 * freshness claim — callers render it with its age (see
 * MusePanelActivity).
 *
 * All methods block; call them off the UI thread.
 */
final class PanelDataClient {

    static final int OK = 0;
    static final int FAILED = 1;
    static final int LINK_EXPIRED = 2;
    static final int NOT_CONFIGURED = 3;

    private static final int TIMEOUT_MS = 8000;
    private static final int MAX_BODY_BYTES = 32 * 1024;
    private static final String CACHE_FILE = "panel-cache.json";

    static final class Result {
        int status = FAILED;
        /** Parsed panel JSON (fresh or cached), or null when none. */
        JSONObject data;
        /** Client-side fetch time of {@link #data}, epoch millis. */
        long fetchedAt;
        /** True when {@link #data} came from the cache fallback. */
        boolean fromCache;
    }

    private PanelDataClient() {}

    static boolean isConfigured() {
        return PanelConfig.PANEL_DATA_URL != null
                && !PanelConfig.PANEL_DATA_URL.isEmpty();
    }

    /**
     * Fetch fresh data; on any failure, fall back to the cache.
     * The returned status always describes the network attempt, so
     * the UI can say "couldn't refresh" while still rendering the
     * cached copy underneath.
     */
    static Result fetch(Context ctx) {
        Result r = new Result();
        if (!isConfigured()) {
            r.status = NOT_CONFIGURED;
            return r;
        }
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(PanelConfig.PANEL_DATA_URL).openConnection();
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", "MuseWidget/4.9.0");
            int code = conn.getResponseCode();
            if (code == 404 || code == 403) {
                r.status = LINK_EXPIRED;
            } else if (code != 200) {
                r.status = FAILED;
            } else {
                String body = readBody(conn.getInputStream());
                JSONObject parsed = parsePanel(body);
                if (parsed == null) {
                    r.status = FAILED;
                } else {
                    r.status = OK;
                    r.data = parsed;
                    r.fetchedAt = System.currentTimeMillis();
                    writeCache(ctx, body, r.fetchedAt);
                    return r;
                }
            }
        } catch (Exception e) {
            r.status = FAILED;
        } finally {
            if (conn != null) conn.disconnect();
        }
        // Network attempt failed (or the link expired): cache fallback.
        Result cached = loadCache(ctx);
        if (cached != null) {
            r.data = cached.data;
            r.fetchedAt = cached.fetchedAt;
            r.fromCache = true;
        }
        return r;
    }

    /** The cached last-good response, or null when none parses. */
    static Result loadCache(Context ctx) {
        try {
            File f = new File(ctx.getFilesDir(), CACHE_FILE);
            if (!f.exists()) return null;
            String text = readFile(f);
            JSONObject cache = new JSONObject(text);
            JSONObject parsed = parsePanel(cache.optString("body", ""));
            if (parsed == null) return null;
            Result r = new Result();
            r.status = OK;
            r.data = parsed;
            r.fetchedAt = cache.optLong("fetched_at", 0L);
            r.fromCache = true;
            return r;
        } catch (Exception e) {
            return null;
        }
    }

    private static void writeCache(Context ctx, String body, long fetchedAt) {
        try {
            JSONObject cache = new JSONObject();
            cache.put("fetched_at", fetchedAt);
            cache.put("body", body);
            File f = new File(ctx.getFilesDir(), CACHE_FILE);
            FileOutputStream out = new FileOutputStream(f);
            try {
                out.write(cache.toString().getBytes("UTF-8"));
            } finally {
                out.close();
            }
        } catch (Exception ignored) {
            // Cache is best-effort; a failed write only loses the fallback.
        }
    }

    /** Parse + shape-check a panel response; null when unacceptable. */
    private static JSONObject parsePanel(String body) {
        try {
            if (body == null || body.isEmpty()) return null;
            JSONObject obj = new JSONObject(body);
            if (obj.optInt("v", 0) != 1) return null;
            if (!obj.has("house")) return null;
            return obj;
        } catch (Exception e) {
            return null;
        }
    }

    private static String readBody(InputStream in) throws Exception {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int n;
        try {
            while ((n = in.read(chunk)) != -1) {
                buf.write(chunk, 0, n);
                if (buf.size() > MAX_BODY_BYTES) {
                    throw new Exception("panel response too large");
                }
            }
        } finally {
            in.close();
        }
        return new String(buf.toByteArray(), "UTF-8");
    }

    private static String readFile(File f) throws Exception {
        FileInputStream in = new FileInputStream(f);
        try {
            return readBody(in);
        } finally {
            // readBody closes the stream.
        }
    }
}
