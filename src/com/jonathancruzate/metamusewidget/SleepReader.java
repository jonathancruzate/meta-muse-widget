package com.jonathancruzate.metamusewidget;

import android.content.Context;
import android.os.Build;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Sleep tile reader (Pack v5.2.0, design D4): last night's
 * sleep summary from Health Connect, framework APIs only
 * (android.health.connect, API 34+). Reads SleepSessionRecords
 * from the last 36 hours, sums the sessions that ended after
 * yesterday evening, and caches one line ("7 h 12 min · up at
 * 6:40 AM") in PackPrefs for the tile to render. The Health
 * Connect grant is Jonathan's in the Health Connect app; when
 * it is absent (or the platform is older) every path here
 * degrades silently and the tile keeps its designed empty
 * state. Nothing read here leaves the phone.
 */
final class SleepReader {

    private static final long STALE_MS = 6L * 60L * 60L * 1000L;

    private SleepReader() {}

    /** Blocking refresh; call from the pack fetch thread only. */
    static boolean refresh(Context ctx) {
        if (Build.VERSION.SDK_INT < 34) return false;
        if (System.currentTimeMillis() - PackPrefs.sleepSummaryAt(ctx)
                < STALE_MS && PackPrefs.sleepSummary(ctx) != null) {
            return false;
        }
        try {
            return readOnce(ctx);
        } catch (Throwable t) {
            // No grant, no Health Connect, or a platform change:
            // the empty state is the designed answer.
            return false;
        }
    }

    private static boolean readOnce(final Context ctx) throws Exception {
        final android.health.connect.HealthConnectManager manager =
                (android.health.connect.HealthConnectManager)
                        ctx.getSystemService(
                                android.health.connect.HealthConnectManager.class);
        if (manager == null) return false;
        Instant now = Instant.now();
        Instant from = now.minus(Duration.ofHours(36));
        // API 36 shape: the concrete instant range filter (the
        // TimeRangeFilter.between factory is absent from the
        // vendored stub jar; the Builder is the same object).
        android.health.connect.TimeRangeFilter range =
                new android.health.connect.TimeInstantRangeFilter.Builder()
                        .setStartTime(from)
                        .setEndTime(now)
                        .build();
        android.health.connect.ReadRecordsRequestUsingFilters<
                android.health.connect.datatypes.SleepSessionRecord>
                request = new android.health.connect
                .ReadRecordsRequestUsingFilters.Builder<>(
                        android.health.connect.datatypes.SleepSessionRecord.class)
                .setTimeRangeFilter(range)
                .build();
        final CountDownLatch latch = new CountDownLatch(1);
        final String[] summary = new String[1];
        manager.readRecords(request,
                Executors.newSingleThreadExecutor(),
                new android.os.OutcomeReceiver<
                        android.health.connect.ReadRecordsResponse<
                                android.health.connect.datatypes.SleepSessionRecord>,
                        android.health.connect.HealthConnectException>() {
                    @Override
                    public void onResult(
                            android.health.connect.ReadRecordsResponse<
                                    android.health.connect.datatypes.SleepSessionRecord> response) {
                        try {
                            summary[0] = summarize(response.getRecords());
                        } catch (Throwable ignored) {
                            summary[0] = null;
                        }
                        latch.countDown();
                    }

                    @Override
                    public void onError(
                            android.health.connect.HealthConnectException e) {
                        latch.countDown();
                    }
                });
        if (!latch.await(6, TimeUnit.SECONDS)) return false;
        if (summary[0] == null) return false;
        PackPrefs.putSleepSummary(ctx, summary[0]);
        return true;
    }

    private static String summarize(
            List<android.health.connect.datatypes.SleepSessionRecord>
                    records) {
        if (records == null || records.isEmpty()) return null;
        ZoneId zone = ZoneId.systemDefault();
        Instant evening = Instant.now().minus(Duration.ofHours(30));
        long totalMinutes = 0;
        Instant latestEnd = null;
        for (android.health.connect.datatypes.SleepSessionRecord r
                : records) {
            Instant end = r.getEndTime();
            if (end == null || end.isBefore(evening)) continue;
            totalMinutes += Duration.between(r.getStartTime(), end)
                    .toMinutes();
            if (latestEnd == null || end.isAfter(latestEnd)) {
                latestEnd = end;
            }
        }
        if (totalMinutes <= 0 || latestEnd == null) return null;
        long h = totalMinutes / 60;
        long m = totalMinutes % 60;
        String up = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
                .withZone(zone).format(latestEnd);
        return h + " h " + m + " min · up at " + up;
    }
}
