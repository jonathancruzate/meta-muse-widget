package com.samsung.oneui9.metamusewidget;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;

/**
 * The search bar's resolved look, shared by the widget provider and
 * the Customize screen's live preview.
 *
 * Scope rule (Jonathan, 2026-10-26): the theme color affects ONLY
 * the logo and the shortcut icon — nothing else.
 *
 * Bar style (Jonathan, 2026-10-27 — "One UI theme matches One UI,
 * Material You matches Pixel"): a separate setting chooses WHICH
 * platform's bar is ported. One UI = the exact One UI 9 Finder bar,
 * decomposed from One UI Home 18.0.07.59 (refs/REFS-FINDINGS.md):
 * fill #f2e9e9e9 light / #f23d3d3d night (95% alpha), 0.5dp outline
 * (#33000000 / #26ffffff), hint #99000000 / #99ffffff. Pixel = the
 * Pixel Launcher 17 QSB (refs/PIXEL-FINDINGS.md): a stadium plate
 * of the framework's dynamic surfaceContainerLow with a soft
 * shadow, the shortcut in a filled circle — and the logo as a
 * sweep gradient of framework accent tones, Pixel's "dynamic G"
 * treatment (see dynamicLogoBitmap). In Pixel style the accent
 * theme tints only the shortcut glyph.
 * The transparency slider still fades the whole pill (an opacity
 * control, not a color). Light/dark follows the device.
 */
public final class BarStyle {

    private BarStyle() {}

    public static boolean isNight(Context ctx) {
        return (ctx.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    /** Pill fill color, before the transparency alpha is applied. */
    public static int baseFill(Context ctx) {
        return ctx.getColor(R.color.finder_fill);
    }

    /** Fill color with the Finder alpha and the slider applied. */
    public static int fill(Context ctx) { return fill(ctx, 0); }

    public static int fill(Context ctx, int widgetId) {
        int alpha = Math.round(255f * (100 - WidgetPrefs.transparency(ctx, widgetId)) / 100f);
        // The real Finder bar is never fully opaque: #f2...... (242/255).
        alpha = Math.round(alpha * 242f / 255f);
        return (alpha << 24) | (baseFill(ctx) & 0x00FFFFFF);
    }

    /** The Finder bar's 0.5dp outline color. */
    public static int outlineColor(Context ctx) {
        // finder_search_bar_background_border_color, One UI Home.
        return isNight(ctx) ? 0x26FFFFFF : 0x33000000;
    }

    /**
     * Top color of the glass gradient: the fill lightened toward
     * white — the One UI 9 §9 glassy-blur treatment (a widget cannot
     * backdrop-blur; the frosted read comes from gradient + sheen +
     * hairlines, the way the spec's glass layer is drawn).
     */
    public static int fillTop(Context ctx) { return fillTop(ctx, 0); }

    public static int fillTop(Context ctx, int widgetId) {
        int base = baseFill(ctx);
        float f = isNight(ctx) ? 0.07f : 0.12f;
        int r = Math.round(Color.red(base) + (255 - Color.red(base)) * f);
        int g = Math.round(Color.green(base) + (255 - Color.green(base)) * f);
        int b = Math.round(Color.blue(base) + (255 - Color.blue(base)) * f);
        int alpha = fill(ctx, widgetId) >>> 24;
        return (alpha << 24) | (r << 16) | (g << 8) | b;
    }

    /** Glyph tint — the ONLY thing the theme color touches. */
    public static int accent(Context ctx) { return accent(ctx, 0); }

    public static int accent(Context ctx, int widgetId) {
        String mode = WidgetPrefs.themeMode(ctx, widgetId);
        boolean night = isNight(ctx);
        if (WidgetPrefs.THEME_SYSTEM.equals(mode)) {
            return Build.VERSION.SDK_INT >= 31
                    ? ctx.getColor(android.R.color.system_accent1_600)
                    : ctx.getColor(R.color.primary_dark);
        }
        if (WidgetPrefs.THEME_STEPHANY.equals(mode)) {
            return ctx.getColor(R.color.stephany_accent);
        }
        if (WidgetPrefs.THEME_CUSTOM.equals(mode)) {
            float sat = Math.min(1f, WidgetPrefs.customSat(ctx, widgetId) / 100f * 1.15f);
            float[] hsv = {WidgetPrefs.customHue(ctx, widgetId), sat, night ? 0.92f : 0.40f};
            return Color.HSVToColor(hsv);
        }
        return ctx.getColor(R.color.primary_dark);
    }

    public static int hintColor(Context ctx) {
        // finder_search_bar_hint_text_color, One UI Home.
        return isNight(ctx) ? 0x99FFFFFF : 0x99000000;
    }

    // ---- Pixel (Material You) bar style ----
    // Decomposed from Pixel Launcher 17 — refs/PIXEL-FINDINGS.md.

    /** True when the Pixel (Material You) bar style is selected. */
    public static boolean isPixel(Context ctx, int widgetId) {
        return WidgetPrefs.BAR_STYLE_PIXEL.equals(WidgetPrefs.barStyle(ctx, widgetId));
    }

    /** A framework dynamic color by name, or 0 when absent. */
    private static int systemColor(Context ctx, String name) {
        int id = ctx.getResources().getIdentifier(name, "color", "android");
        if (id == 0) return 0;
        try { return ctx.getColor(id); } catch (Exception e) { return 0; }
    }

    /**
     * The Pixel bar's fill: Pixel Launcher's
     * qsbFillColorThemedHomeScreen = the framework's dynamic
     * surfaceContainerLow. Fallback: the Material 3 baseline.
     */
    public static int pixelFill(Context ctx) { return pixelFill(ctx, 0); }

    public static int pixelFill(Context ctx, int widgetId) {
        int base = systemColor(ctx, "system_surface_container_low");
        if (base == 0) base = isNight(ctx) ? 0xFF1D1B20 : 0xFFF7F2FA;
        int alpha = Math.round(255f * (100 - WidgetPrefs.transparency(ctx, widgetId)) / 100f);
        return (alpha << 24) | (base & 0x00FFFFFF);
    }

    /** Pixel hint: the dynamic onSurfaceVariant. */
    public static int pixelHint(Context ctx) {
        int c = systemColor(ctx, "system_on_surface_variant");
        return c != 0 ? c : (isNight(ctx) ? 0xFFCAC4D0 : 0xFF49454F);
    }

    /** The Pixel shortcut circle: surfaceContainerHighest tone. */
    public static int pixelCircleFill(Context ctx) {
        int c = systemColor(ctx, "system_surface_container_highest");
        return c != 0 ? c : (isNight(ctx) ? 0xFF36343B : 0xFFECE6F0);
    }

    /** Hint color for the effective bar style. */
    public static int hintColor(Context ctx, int widgetId) {
        return isPixel(ctx, widgetId) ? pixelHint(ctx) : hintColor(ctx);
    }

    // ---- The dynamic logo (Pixel's "Super G" treatment) ----
    // REA + agy dig, refs/REA-G-COLOR-ENGINE.md: Pixel's dynamic G
    // is a 17-stop SWEEP gradient whose stops are 8 colors bound to
    // framework dynamic tones (system_accent1/2/3) with lStar
    // lightness tweaks, swapped in for the fixed brand-color G when
    // Themed Icons is on. The launcher runs no engine of its own —
    // it consumes the framework scheme. Ours does the same: same
    // stops, same sources, swept around the M's own center and
    // clipped to the glyph's alpha.

    private static final float[] SWEEP_POSITIONS = {
            0f, 0.080f, 0.150f, 0.220f, 0.280f, 0.350f, 0.375f, 0.450f,
            0.500f, 0.550f, 0.575f, 0.635f, 0.680f, 0.780f, 0.880f,
            0.920f, 1f};

    private static int sysColorOr(Context ctx, String name, int fallback) {
        int c = systemColor(ctx, name);
        return c != 0 ? c : fallback;
    }

    /** sRGB color with its CIE L* (perceptual lightness) replaced —
     * the framework's android:lStar modifier, applied by hand. */
    private static int withLStar(int color, double targetL) {
        double[] rgb = new double[3];
        int[] ch = {Color.red(color), Color.green(color), Color.blue(color)};
        for (int i = 0; i < 3; i++) {
            double c = ch[i] / 255.0;
            rgb[i] = c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
        }
        double x = (0.4124 * rgb[0] + 0.3576 * rgb[1] + 0.1805 * rgb[2]) / 0.95047;
        double y = (0.2126 * rgb[0] + 0.7152 * rgb[1] + 0.0722 * rgb[2]) / 1.0;
        double z = (0.0193 * rgb[0] + 0.1192 * rgb[1] + 0.9505 * rgb[2]) / 1.08883;
        double fx = labF(x), fy = labF(y), fz = labF(z);
        double a = 500 * (fx - fy), b = 200 * (fy - fz);
        double fy2 = (targetL + 16) / 116.0;
        double fx2 = fy2 + a / 500.0, fz2 = fy2 - b / 200.0;
        double xr = labFInv(fx2) * 0.95047, yr = labFInv(fy2), zr = labFInv(fz2) * 1.08883;
        double[] lin = {
                3.2406 * xr - 1.5372 * yr - 0.4986 * zr,
                -0.9689 * xr + 1.8758 * yr + 0.0415 * zr,
                0.0557 * xr - 0.2040 * yr + 1.0570 * zr};
        int[] out = new int[3];
        for (int i = 0; i < 3; i++) {
            double c = lin[i] <= 0.0031308 ? 12.92 * lin[i]
                    : 1.055 * Math.pow(lin[i], 1.0 / 2.4) - 0.055;
            out[i] = Math.max(0, Math.min(255, (int) Math.round(c * 255)));
        }
        return Color.rgb(out[0], out[1], out[2]);
    }

    private static double labF(double t) {
        return t > 0.008856 ? Math.cbrt(t) : 7.787 * t + 16.0 / 116.0;
    }

    private static double labFInv(double f) {
        double f3 = f * f * f;
        return f3 > 0.008856 ? f3 : (116 * f - 16) / 903.3;
    }

    /** The 17 sweep stops, in Pixel's order, from the 8 dynamic
     * role colors (light / night bindings per the REA table). */
    private static int[] sweepColors(Context ctx) {
        boolean night = isNight(ctx);
        int a1_200 = sysColorOr(ctx, "system_accent1_200", 0xFFB0C4F5);
        int a1_400 = sysColorOr(ctx, "system_accent1_400", 0xFF6B8AFD);
        int a1_500 = sysColorOr(ctx, "system_accent1_500", 0xFF3B78E8);
        int a1_600 = sysColorOr(ctx, "system_accent1_600", 0xFF1A5FD7);
        int a2_10 = sysColorOr(ctx, "system_accent2_10", 0xFFF1F3F4);
        int a2_300 = sysColorOr(ctx, "system_accent2_300", 0xFFA8C7FA);
        int a3_100 = sysColorOr(ctx, "system_accent3_100", 0xFFC2E7FF);
        int a3_400 = sysColorOr(ctx, "system_accent3_400", 0xFF5AB0F0);
        int primary = night ? a3_100 : a3_400;
        int primaryTransition = night ? withLStar(a1_400, 65) : a1_500;
        int secondary = night ? a1_400 : a1_500;
        int secondaryTransition = night ? withLStar(a1_400, 65) : withLStar(a1_500, 53);
        int tertiary = night ? withLStar(a2_10, 98) : a2_300;
        int quatTransitionOne = night ? withLStar(a1_200, 85) : withLStar(a1_600, 50);
        int quatTransitionTwo = night ? a1_200 : withLStar(a1_600, 45);
        int quaternary = night ? a1_200 : a1_600;
        return new int[]{primary, primary, primary, primaryTransition,
                secondary, secondary, secondaryTransition,
                tertiary, tertiary, tertiary, tertiary,
                quatTransitionOne, quatTransitionTwo,
                quaternary, quaternary, quaternary, primary};
    }

    /** The dynamic sweep belongs to the System theme (the
     * framework tones ARE the system palette). Any other theme
     * tints the glyph with its own accent instead — theming
     * applies to the bar's left logo in every style (Jonathan,
     * 2026-10-27). */
    public static boolean useDynamicLogo(Context ctx, int widgetId) {
        return isPixel(ctx, widgetId)
                && WidgetPrefs.THEME_SYSTEM.equals(WidgetPrefs.themeMode(ctx, widgetId));
    }

    /**
     * The Muse M as Pixel's dynamic G: a sweep gradient of the
     * framework's dynamic accent tones, clipped to the glyph.
     * Rendered at 102x72 (34x24dp at 3x); views scale it down.
     */
    public static Bitmap dynamicLogoBitmap(Context ctx) {
        android.graphics.Bitmap mask = android.graphics.BitmapFactory.decodeResource(
                ctx.getResources(), R.drawable.ic_muse_m);
        if (mask == null) return null;
        int w = 102, h = 72;
        Bitmap scaledMask = Bitmap.createScaledBitmap(mask, w, h, true);
        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        Paint sweep = new Paint(Paint.ANTI_ALIAS_FLAG);
        sweep.setShader(new android.graphics.SweepGradient(
                w / 2f, h / 2f, sweepColors(ctx), SWEEP_POSITIONS));
        canvas.drawRect(0, 0, w, h, sweep);
        Paint maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        maskPaint.setXfermode(new android.graphics.PorterDuffXfermode(
                android.graphics.PorterDuff.Mode.DST_IN));
        canvas.drawBitmap(scaledMask, 0, 0, maskPaint);
        return out;
    }

    /**
     * The shortcut as Pixel presents it: a filled circle with the
     * glyph centered — the circular end button on the Pixel bar.
     * The glyph keeps the theme accent tint.
     */
    public static Bitmap shortcutCircleBitmap(Context ctx, int widgetId, int glyphRes) {
        float density = ctx.getResources().getDisplayMetrics().density;
        int size = Math.round(40f * density);
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        Paint circle = new Paint(Paint.ANTI_ALIAS_FLAG);
        circle.setColor(pixelCircleFill(ctx));
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, circle);
        android.graphics.Bitmap glyph = android.graphics.BitmapFactory.decodeResource(
                ctx.getResources(), glyphRes);
        if (glyph != null) {
            Paint gp = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
            gp.setColorFilter(new android.graphics.PorterDuffColorFilter(
                    accent(ctx, widgetId), android.graphics.PorterDuff.Mode.SRC_IN));
            float gs = size * 0.55f;
            canvas.drawBitmap(glyph, null, new RectF(
                    (size - gs) / 2f, (size - gs) / 2f,
                    (size + gs) / 2f, (size + gs) / 2f), gp);
        }
        return bmp;
    }

    /**
     * The pill as a bitmap at the widget's real size, so the
     * transparency slider is exact (a static drawable cannot carry
     * a slider-driven alpha).
     */
    public static Bitmap pillBitmap(Context ctx, int widthDp, int heightDp) {
        return pillBitmap(ctx, widthDp, heightDp, 0);
    }

    public static Bitmap pillBitmap(Context ctx, int widthDp, int heightDp, int widgetId) {
        try {
            return renderPill(ctx, widthDp, heightDp, widgetId);
        } catch (Throwable t) {
            // A render failure must never take the widget down
            // with it (the host shows an error placeholder when an
            // update dies): fall back to the flat plate.
            return flatPillBitmap(ctx, widthDp, heightDp, widgetId);
        }
    }

    /** The flat plate: the style's fill as a plain stadium, no
     * wallpaper texture, no shadow. The guaranteed-render path. */
    public static Bitmap flatPillBitmap(Context ctx, int widthDp, int heightDp, int widgetId) {
        float density = ctx.getResources().getDisplayMetrics().density;
        int w = Math.max(48, Math.round(widthDp * density));
        int h = Math.max(24, Math.round(heightDp * density));
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setColor(isPixel(ctx, widgetId) ? pixelFill(ctx, widgetId) : fill(ctx, widgetId));
        canvas.drawRoundRect(new RectF(0, 0, w, h), h / 2f, h / 2f, fillPaint);
        if (!isPixel(ctx, widgetId)) {
            float sw = Math.max(1f, 0.5f * density);
            Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(sw);
            stroke.setColor(outlineColor(ctx));
            float inset = sw / 2f;
            canvas.drawRoundRect(new RectF(inset, inset, w - inset, h - inset),
                    h / 2f - inset, h / 2f - inset, stroke);
        }
        return bmp;
    }

    private static Bitmap renderPill(Context ctx, int widthDp, int heightDp, int widgetId) {
        float density = ctx.getResources().getDisplayMetrics().density;
        int w = Math.max(48, Math.round(widthDp * density));
        int h = Math.max(24, Math.round(heightDp * density));
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        if (isPixel(ctx, widgetId)) {
            // Pixel plate (refs/PIXEL-FINDINGS.md): a stadium of
            // solid dynamic surfaceContainerLow with a soft shadow
            // in its 6dp inset band — no glass, no sheen, no
            // hairline.
            RectF plate = new RectF(0, 0.5f * density, w, h - 2.5f * density);
            Paint platePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            platePaint.setColor(pixelFill(ctx, widgetId));
            platePaint.setShadowLayer(2f * density, 0, 1f * density,
                    isNight(ctx) ? 0x80000000 : 0x40000000);
            canvas.drawRoundRect(plate, plate.height() / 2f, plate.height() / 2f, platePaint);
            return bmp;
        }
        float r = h / 2f;
        RectF pill = new RectF(0, 0, w, h);
        android.graphics.Path pillPath = new android.graphics.Path();
        pillPath.addRoundRect(pill, r, r, android.graphics.Path.Direction.CW);
        // The launcher's recipe: the actual wallpaper, blurred,
        // under the tint. Drawn first, clipped to the pill.
        Bitmap texture = WallpaperGlass.texture(ctx);
        if (texture != null) {
            canvas.save();
            canvas.clipPath(pillPath);
            canvas.drawBitmap(texture, null, pill, new Paint(Paint.FILTER_BITMAP_FLAG));
            canvas.restore();
        }
        // Frosted-glass tint: vertical gradient, lighter at the top,
        // composited over the texture at the Finder's real alpha.
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setShader(new android.graphics.LinearGradient(0, 0, 0, h,
                fillTop(ctx, widgetId), fill(ctx, widgetId), android.graphics.Shader.TileMode.CLAMP));
        canvas.drawRoundRect(pill, r, r, paint);
        // Sheen: soft white wash over the top half, fading out —
        // light sitting on blurred glass (spec §9, clean blur).
        android.graphics.Path clip = new android.graphics.Path();
        clip.addRoundRect(pill, r, r, android.graphics.Path.Direction.CW);
        canvas.save();
        canvas.clipPath(clip);
        Paint sheen = new Paint(Paint.ANTI_ALIAS_FLAG);
        int sheenAlpha = isNight(ctx) ? 0x12 : 0x22;
        sheen.setShader(new android.graphics.LinearGradient(0, 0, 0, h * 0.55f,
                (sheenAlpha << 24) | 0x00FFFFFF, 0x00FFFFFF,
                android.graphics.Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h * 0.55f, sheen);
        canvas.restore();
        // Inner hairline just inside the edge (the §9 glass border).
        float sw = Math.max(1f, 0.5f * density);
        Paint inner = new Paint(Paint.ANTI_ALIAS_FLAG);
        inner.setStyle(Paint.Style.STROKE);
        inner.setStrokeWidth(sw);
        inner.setColor(isNight(ctx) ? 0x14FFFFFF : 0x2EFFFFFF);
        float inInset = sw * 1.5f;
        canvas.drawRoundRect(new RectF(inInset, inInset, w - inInset, h - inInset),
                r - inInset, r - inInset, inner);
        // homescreen_search_bar_outline_width = 0.5dp, One UI Home.
        Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(sw);
        stroke.setColor(outlineColor(ctx));
        float inset = sw / 2f;
        canvas.drawRoundRect(new RectF(inset, inset, w - inset, h - inset),
                r - inset, r - inset, stroke);
        return bmp;
    }
}
