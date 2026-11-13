package com.jonathancruzate.metamusewidget;

import android.content.Context;
import android.graphics.Color;
import android.widget.RemoteViews;

/**
 * Per-mode theming (Pack v5.2.0, design Phase 3): the pack's
 * surfaces follow the house mode. Night (the phone reports
 * Sleep, or the feed's desired mode is night) renders tiles on
 * the night-dim card with light text, whatever the system
 * theme says; Work renders the rose-tint card; anything else
 * keeps the standard Material You / One UI card. The mode is a
 * user setting too (PackPrefs.themeMode): auto follows the
 * house, day / night / work pin a variant.
 */
final class PackThemes {

    static final int AUTO = 0;
    static final int NIGHT = 1;
    static final int WORK = 2;

    private PackThemes() {}

    static int resolve(Context ctx) {
        String pin = PackPrefs.themeMode(ctx);
        if ("night".equals(pin)) return NIGHT;
        if ("work".equals(pin)) return WORK;
        if ("day".equals(pin)) return AUTO;
        String gt = PackPrefs.contextHouseMode(ctx);
        if ("Sleep".equalsIgnoreCase(gt)) return NIGHT;
        org.json.JSONObject data = PackData.cachedData(ctx);
        if (data != null) {
            org.json.JSONObject house = data.optJSONObject("house");
            if (house != null) {
                String desired = house.optString("desired_mode", "");
                String mode = house.optString("mode", "");
                if ("night".equals(desired) || "night".equals(mode)) {
                    return NIGHT;
                }
                if ("work".equals(desired)) return WORK;
            }
        }
        return AUTO;
    }

    /** Recolor a tile layout's root + texts for the variant. */
    static void applyTile(Context ctx, RemoteViews views,
                          boolean hasMeta) {
        int variant = resolve(ctx);
        if (variant == AUTO) return;
        try {
            if (variant == NIGHT) {
                views.setInt(R.id.tile_root, "setBackgroundResource",
                        R.drawable.bg_pack_card_nightdim);
                views.setTextColor(R.id.tv_kind,
                        Color.parseColor("#F06A9B"));
                views.setTextColor(R.id.tv_value,
                        Color.parseColor("#F5F5F7"));
                views.setTextColor(R.id.tv_body,
                        Color.parseColor("#AEAEB2"));
                if (hasMeta) {
                    views.setTextColor(R.id.tv_age,
                            Color.parseColor("#AEAEB2"));
                    views.setTextColor(R.id.tv_refresh,
                            Color.parseColor("#F06A9B"));
                }
            } else if (variant == WORK) {
                views.setInt(R.id.tile_root, "setBackgroundResource",
                        R.drawable.bg_pack_card_work);
            }
        } catch (Exception ignored) {
            // Theming never breaks a render.
        }
    }
}
