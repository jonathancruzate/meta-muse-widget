package com.jonathancruzate.metamusewidget;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Now Bar feeder (Pack v5.1.0, design Section 7.4): sparing,
 * time-bound Live Updates only. Tonight's feeder carries the
 * leave-by countdown: while the feed's transit leave-by is
 * still ahead today, an ongoing notification counts down to
 * it; on Android 16+ (API 36) it requests promoted-ongoing
 * status so it can surface on the Now Bar, and it expires
 * (cancels itself) once the leave-by passes. No standing
 * notifications, no content beyond the leave-by the brief
 * already published to the feed.
 */
final class LiveUpdateFeeder {

    private static final String CHANNEL_ID = "pack_live";
    private static final int NOTIFICATION_ID = 5101;

    private LiveUpdateFeeder() {}

    /** Sync the feeder with the latest cached feed. Never throws. */
    static void syncFromFeed(Context ctx) {
        try {
            JSONObject data = PackData.cachedData(ctx);
            JSONObject transit = data == null ? null
                    : data.optJSONObject("transit");
            String leaveBy = transit == null ? ""
                    : transit.optString("leave_by", "");
            NotificationManager nm = (NotificationManager)
                    ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            if (leaveBy.isEmpty()) {
                nm.cancel(NOTIFICATION_ID);
                return;
            }
            Date when = parseTodayTime(leaveBy);
            if (when == null || when.getTime() < System.currentTimeMillis()) {
                nm.cancel(NOTIFICATION_ID);
                return;
            }
            long mins = (when.getTime()
                    - System.currentTimeMillis()) / 60000L;
            String summary = transit.optString("summary", "");
            String text = "Leave by " + leaveBy + " · in "
                    + Math.max(1L, mins) + " min"
                    + (summary.isEmpty() ? "" : " · " + summary);
            ensureChannel(nm);
            Intent open = new Intent(ctx, MusePanelActivity.class);
            open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Notification.Builder b = new Notification.Builder(ctx,
                    CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_muse_m)
                    .setContentTitle("Muse · leave-by")
                    .setContentText(text)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setWhen(when.getTime())
                    .setShowWhen(true)
                    .setContentIntent(PendingIntent.getActivity(ctx,
                            NOTIFICATION_ID, open,
                            PendingIntent.FLAG_UPDATE_CURRENT
                                    | PendingIntent.FLAG_IMMUTABLE));
            if (Build.VERSION.SDK_INT >= 36) {
                // Android 16 Live Updates: promoted ongoing. The
                // vendored android-36 stub predates the final
                // Builder method, so the promotion is requested
                // reflectively, with the documented extras key as
                // the fallback; both are runtime-guarded.
                b.setStyle(new Notification.ProgressStyle());
                b.setShortCriticalText(leaveBy);
                try {
                    java.lang.reflect.Method m =
                            Notification.Builder.class.getMethod(
                                    "setRequestPromotedOngoing",
                                    boolean.class);
                    m.invoke(b, Boolean.TRUE);
                } catch (Exception reflectionMiss) {
                    android.os.Bundle extras = new android.os.Bundle();
                    extras.putBoolean(
                            "android.requestPromotedOngoing", true);
                    b.addExtras(extras);
                }
            }
            nm.notify(NOTIFICATION_ID, b.build());
        } catch (Exception ignored) {
            // The feeder is an enhancement; it never breaks a render.
        }
    }

    private static void ensureChannel(NotificationManager nm) {
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return;
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Muse leave-by",
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(
                "Time-bound leave-by countdowns from the Muse feed");
        nm.createNotificationChannel(channel);
    }

    /** Parse "9:40 AM" as a time today; null when unparseable. */
    private static Date parseTodayTime(String text) {
        try {
            SimpleDateFormat f = new SimpleDateFormat("h:mm a",
                    Locale.US);
            Date t = f.parse(text.trim());
            if (t == null) return null;
            java.util.Calendar parsed = java.util.Calendar.getInstance();
            parsed.setTime(t);
            java.util.Calendar today = java.util.Calendar.getInstance();
            today.set(java.util.Calendar.HOUR_OF_DAY,
                    parsed.get(java.util.Calendar.HOUR_OF_DAY));
            today.set(java.util.Calendar.MINUTE,
                    parsed.get(java.util.Calendar.MINUTE));
            today.set(java.util.Calendar.SECOND, 0);
            return today.getTime();
        } catch (Exception e) {
            return null;
        }
    }
}
