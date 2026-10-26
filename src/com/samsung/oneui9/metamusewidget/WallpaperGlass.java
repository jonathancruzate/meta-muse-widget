package com.samsung.oneui9.metamusewidget;

import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;

/**
 * The real blur fix: a widget cannot backdrop-blur (RemoteViews
 * never sees the pixels behind it), so the pill composites the
 * launcher's own recipe instead — the device's actual wallpaper,
 * heavily blurred, drawn under the Finder tint at its real alpha.
 * What shows through the tint is the wallpaper itself, frosted,
 * exactly the way the launcher's compositor presents its bar.
 *
 * The texture is cached and keyed to the system wallpaper id, so
 * it refreshes itself when the wallpaper changes. Any failure
 * (live wallpaper, access denied) returns null and the bar falls
 * back to the flat glass treatment.
 */
public final class WallpaperGlass {

    private WallpaperGlass() {}

    private static Bitmap cached;
    private static int cachedWallpaperId = Integer.MIN_VALUE;

    /** Blurred wallpaper texture, or null when unavailable. */
    public static Bitmap texture(Context ctx) {
        try {
            WallpaperManager wm = WallpaperManager.getInstance(ctx);
            int id = Build.VERSION.SDK_INT >= 27
                    ? wm.getWallpaperId(WallpaperManager.FLAG_SYSTEM)
                    : (int) (System.currentTimeMillis() / 60000L);
            if (cached != null && id == cachedWallpaperId) return cached;
            Bitmap raw = readWallpaper(ctx, wm);
            if (raw == null) return null;
            cached = blur(raw);
            cachedWallpaperId = id;
            return cached;
        } catch (Throwable t) {
            return null;
        }
    }

    private static Bitmap readWallpaper(Context ctx, WallpaperManager wm) {
        // Lane 1: the drawable (no permission needed).
        try {
            Drawable d = wm.getDrawable();
            if (d != null && d.getIntrinsicWidth() > 0 && d.getIntrinsicHeight() > 0) {
                int maxDim = 320;
                float scale = Math.min(1f, (float) maxDim
                        / Math.max(d.getIntrinsicWidth(), d.getIntrinsicHeight()));
                int w = Math.max(2, Math.round(d.getIntrinsicWidth() * scale));
                int h = Math.max(2, Math.round(d.getIntrinsicHeight() * scale));
                Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                Canvas c = new Canvas(bmp);
                d.setBounds(0, 0, w, h);
                d.draw(c);
                return bmp;
            }
        } catch (Throwable ignored) {}
        // Lane 2: the wallpaper file itself.
        try {
            android.os.ParcelFileDescriptor pfd =
                    wm.getWallpaperFile(WallpaperManager.FLAG_SYSTEM);
            if (pfd != null) {
                java.io.InputStream in = new java.io.FileInputStream(pfd.getFileDescriptor());
                Bitmap bmp = android.graphics.BitmapFactory.decodeStream(in);
                in.close();
                pfd.close();
                if (bmp != null) return bmp;
            }
        } catch (Throwable ignored) {}
        return null;
    }

    /** Downscale + double box blur: a smooth, heavy frost. */
    private static Bitmap blur(Bitmap src) {
        int maxDim = 144;
        float scale = Math.min(1f, (float) maxDim
                / Math.max(src.getWidth(), src.getHeight()));
        int w = Math.max(2, Math.round(src.getWidth() * scale));
        int h = Math.max(2, Math.round(src.getHeight() * scale));
        Bitmap small = Bitmap.createScaledBitmap(src, w, h, true);
        int[] px = new int[w * h];
        small.getPixels(px, 0, w, 0, 0, w, h);
        boxBlur(px, w, h, 9);
        boxBlur(px, w, h, 9);
        small.setPixels(px, 0, w, 0, 0, w, h);
        return small;
    }

    /** Separable box blur over packed ARGB pixels. */
    private static void boxBlur(int[] px, int w, int h, int radius) {
        int[] tmp = new int[px.length];
        // Horizontal pass.
        for (int y = 0; y < h; y++) {
            long r = 0, g = 0, b = 0, a = 0;
            int row = y * w;
            for (int x = -radius; x <= radius; x++) {
                int p = px[row + clamp(x, w)];
                a += (p >>> 24); r += (p >> 16) & 0xFF; g += (p >> 8) & 0xFF; b += p & 0xFF;
            }
            int n = 2 * radius + 1;
            for (int x = 0; x < w; x++) {
                tmp[row + x] = ((int) (a / n) << 24) | ((int) (r / n) << 16)
                        | ((int) (g / n) << 8) | (int) (b / n);
                int out = px[row + clamp(x - radius, w)];
                int in = px[row + clamp(x + radius + 1, w)];
                a += (in >>> 24) - (out >>> 24);
                r += ((in >> 16) & 0xFF) - ((out >> 16) & 0xFF);
                g += ((in >> 8) & 0xFF) - ((out >> 8) & 0xFF);
                b += (in & 0xFF) - (out & 0xFF);
            }
        }
        // Vertical pass.
        System.arraycopy(tmp, 0, px, 0, px.length);
        for (int x = 0; x < w; x++) {
            long r = 0, g = 0, b = 0, a = 0;
            for (int y = -radius; y <= radius; y++) {
                int p = px[clamp(y, h) * w + x];
                a += (p >>> 24); r += (p >> 16) & 0xFF; g += (p >> 8) & 0xFF; b += p & 0xFF;
            }
            int n = 2 * radius + 1;
            for (int y = 0; y < h; y++) {
                tmp[y * w + x] = ((int) (a / n) << 24) | ((int) (r / n) << 16)
                        | ((int) (g / n) << 8) | (int) (b / n);
                int out = px[clamp(y - radius, h) * w + x];
                int in = px[clamp(y + radius + 1, h) * w + x];
                a += (in >>> 24) - (out >>> 24);
                r += ((in >> 16) & 0xFF) - ((out >> 16) & 0xFF);
                g += ((in >> 8) & 0xFF) - ((out >> 8) & 0xFF);
                b += (in & 0xFF) - (out & 0xFF);
            }
        }
        System.arraycopy(tmp, 0, px, 0, px.length);
    }

    private static int clamp(int v, int size) {
        return v < 0 ? 0 : (v >= size ? size - 1 : v);
    }
}
