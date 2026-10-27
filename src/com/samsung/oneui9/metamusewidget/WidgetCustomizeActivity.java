package com.samsung.oneui9.metamusewidget;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * The Customize screen, modeled on the Google search widget's:
 * a live preview, theme presets (One UI / System palette / Custom,
 * plus the hidden Stephany option — five taps on the preview pill
 * unlock it), Hue and Saturation sliders for the Custom theme, a
 * Transparency slider for the bar, and the shortcut picker. Every
 * change saves immediately, re-renders the placed widgets, and
 * updates the preview — the way Google's screen behaves.
 *
 * Also registered as the widgets' configure activity, so it opens
 * when a widget is added; Done then returns RESULT_OK for that
 * widget id.
 */
public class WidgetCustomizeActivity extends Activity {

    private int widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private android.widget.ImageView previewPill;
    private ImageView previewLogo;
    private ImageView previewShortcut;
    private TextView previewHint;
    private TextView shortcutValue;
    private View customSection;
    private RadioGroup themeGroup;
    private RadioGroup barStyleGroup;
    private boolean binding;
    private int previewTaps;
    private long previewTapWindowStart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);
        setContentView(R.layout.activity_customize);
        Intent intent = getIntent();
        if (intent != null) {
            widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        previewPill = findViewById(R.id.preview_pill);
        previewLogo = findViewById(R.id.preview_logo);
        previewShortcut = findViewById(R.id.preview_shortcut);
        previewHint = findViewById(R.id.preview_hint);
        shortcutValue = findViewById(R.id.customize_shortcut_value);
        customSection = findViewById(R.id.custom_section);
        themeGroup = findViewById(R.id.theme_group);

        String mode = WidgetPrefs.themeMode(this, widgetId);
        // The Stephany theme is a hidden option: its radio stays
        // gone until unlocked — tapping the preview pill five times
        // unlocks it — or while Stephany is the active theme.
        final android.widget.RadioButton stephanyRadio = findViewById(R.id.theme_stephany);
        if (WidgetPrefs.stephanyUnlocked(this)
                || WidgetPrefs.THEME_STEPHANY.equals(mode)) {
            WidgetPrefs.setStephanyUnlocked(this, true);
            stephanyRadio.setVisibility(View.VISIBLE);
        }
        previewPill.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (stephanyRadio.getVisibility() == View.VISIBLE) return;
                long nowTap = System.currentTimeMillis();
                if (nowTap - previewTapWindowStart > 3000) {
                    previewTaps = 0;
                    previewTapWindowStart = nowTap;
                }
                previewTaps++;
                if (previewTaps >= 5) {
                    WidgetPrefs.setStephanyUnlocked(WidgetCustomizeActivity.this, true);
                    stephanyRadio.setVisibility(View.VISIBLE);
                    android.widget.Toast.makeText(WidgetCustomizeActivity.this,
                            R.string.stephany_unlocked,
                            android.widget.Toast.LENGTH_SHORT).show();
                }
            }
        });
        binding = true;
        themeGroup.check(WidgetPrefs.THEME_SYSTEM.equals(mode) ? R.id.theme_system
                : WidgetPrefs.THEME_STEPHANY.equals(mode) ? R.id.theme_stephany
                : WidgetPrefs.THEME_CUSTOM.equals(mode) ? R.id.theme_custom
                : R.id.theme_oneui);
        binding = false;
        themeGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (binding) return;
                String m = checkedId == R.id.theme_system ? WidgetPrefs.THEME_SYSTEM
                        : checkedId == R.id.theme_stephany ? WidgetPrefs.THEME_STEPHANY
                        : checkedId == R.id.theme_custom ? WidgetPrefs.THEME_CUSTOM
                        : WidgetPrefs.THEME_ONEUI;
                WidgetPrefs.setTheme(WidgetCustomizeActivity.this, widgetId, m);
                changed();
            }
        });

        barStyleGroup = findViewById(R.id.barstyle_group);
        binding = true;
        barStyleGroup.check(WidgetPrefs.BAR_STYLE_PIXEL.equals(
                WidgetPrefs.barStyle(this, widgetId))
                ? R.id.barstyle_pixel : R.id.barstyle_oneui);
        binding = false;
        barStyleGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (binding) return;
                WidgetPrefs.setBarStyle(WidgetCustomizeActivity.this, widgetId,
                        checkedId == R.id.barstyle_pixel
                                ? WidgetPrefs.BAR_STYLE_PIXEL : WidgetPrefs.BAR_STYLE_ONEUI);
                changed();
            }
        });

        SeekBar hue = findViewById(R.id.hue_seek);
        hue.setProgress(WidgetPrefs.customHue(this, widgetId));
        hue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (!fromUser) return;
                WidgetPrefs.setCustomHue(WidgetCustomizeActivity.this, widgetId, progress);
                changed();
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });
        SeekBar sat = findViewById(R.id.sat_seek);
        sat.setProgress(WidgetPrefs.customSat(this, widgetId));
        sat.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (!fromUser) return;
                WidgetPrefs.setCustomSat(WidgetCustomizeActivity.this, widgetId, progress);
                changed();
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });
        SeekBar transparency = findViewById(R.id.transparency_seek);
        transparency.setProgress(WidgetPrefs.transparency(this, widgetId));
        transparency.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (!fromUser) return;
                WidgetPrefs.setTransparency(WidgetCustomizeActivity.this, widgetId, progress);
                changed();
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });

        findViewById(R.id.change_shortcut_button).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showShortcutDialog(); }
        });
        findViewById(R.id.done_button).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                WidgetPrefs.refreshWidgets(WidgetCustomizeActivity.this);
                if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    Intent result = new Intent();
                    result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
                    setResult(RESULT_OK, result);
                }
                finish();
            }
        });
        refreshPreview();
    }

    private void changed() {
        WidgetPrefs.refreshWidgets(this);
        refreshPreview();
    }

    private void refreshPreview() {
        boolean custom = WidgetPrefs.THEME_CUSTOM.equals(WidgetPrefs.themeMode(this, widgetId));
        customSection.setVisibility(custom ? View.VISIBLE : View.GONE);
        // Same rendered pill as the widget itself (wallpaper frost
        // included) — what you tune is what you get.
        previewPill.post(new Runnable() {
            @Override public void run() {
                float d = getResources().getDisplayMetrics().density;
                int wDp = Math.max(48, Math.round(previewPill.getWidth() / d));
                previewPill.setImageBitmap(BarStyle.pillBitmap(
                        WidgetCustomizeActivity.this, wDp, 52, widgetId));
            }
        });
        int accent = BarStyle.accent(this, widgetId);
        boolean pixel = BarStyle.isPixel(this, widgetId);
        if (pixel) {
            previewLogo.setImageBitmap(BarStyle.dynamicLogoBitmap(this));
        } else {
            previewLogo.setImageResource(R.drawable.ic_muse_m);
            previewLogo.setColorFilter(accent);
        }
        previewHint.setTextColor(BarStyle.hintColor(this, widgetId));
        String action = WidgetPrefs.shortcutAction(this, widgetId);
        if (WidgetPrefs.SHORTCUT_NONE.equals(action)) {
            previewShortcut.setVisibility(View.GONE);
            shortcutValue.setText(R.string.value_hidden);
        } else {
            previewShortcut.setVisibility(View.VISIBLE);
            previewShortcut.setImageResource(WidgetPrefs.SHORTCUT_ASSIST.equals(action)
                    ? R.drawable.ic_assistant : R.drawable.ic_open);
            previewShortcut.setColorFilter(accent);
            if (pixel) {
                android.graphics.drawable.GradientDrawable circle =
                        new android.graphics.drawable.GradientDrawable();
                circle.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                circle.setColor(BarStyle.pixelCircleFill(this));
                previewShortcut.setBackground(circle);
            } else {
                previewShortcut.setBackground(null);
            }
            shortcutValue.setText(WidgetPrefs.shortcutActionLabel(this, widgetId));
        }
    }

    private void showShortcutDialog() {
        // Actions only — Muse gates destination URIs by caller
        // signature (see Router). Bottom-anchored per the spec.
        final String[] labels = {
                getString(R.string.shortcut_action_assist),
                getString(R.string.shortcut_action_launch),
                getString(R.string.shortcut_none)};
        final String[] actions = {
                WidgetPrefs.SHORTCUT_ASSIST, WidgetPrefs.SHORTCUT_LAUNCH,
                WidgetPrefs.SHORTCUT_NONE};
        String current = WidgetPrefs.shortcutAction(this, widgetId);
        int checked = WidgetPrefs.SHORTCUT_LAUNCH.equals(current) ? 1
                : WidgetPrefs.SHORTCUT_NONE.equals(current) ? 2 : 0;
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setTitle(R.string.shortcut_dialog_title)
                .setSingleChoiceItems(labels, checked,
                        new android.content.DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(android.content.DialogInterface d, int which) {
                                WidgetPrefs.setShortcutAction(
                                        WidgetCustomizeActivity.this, widgetId, actions[which]);
                                changed();
                                d.dismiss();
                            }
                        })
                .create();
        dialog.show();
        MainActivity.toBottom(dialog);
    }

}
