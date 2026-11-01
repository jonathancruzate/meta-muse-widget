package com.jonathancruzate.metamusewidget;

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
 * Also registered as the widget's configure activity, so it opens
 * when the widget is added (and from the long-press pencil);
 * Done then returns RESULT_OK for that widget id.
 *
 * Since v4.5.0 this page IS the app: the launcher opens it in
 * global scope, the way the Google search widget's app is its
 * Customize screen. The old catalog hub is gone — Muse gates
 * its destination URIs by caller signature, so a browsable list
 * of them was reference material nobody could use.
 */
public class WidgetCustomizeActivity extends Activity {

    private int widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private android.widget.ImageView previewPill;
    private ImageView previewLogo;
    private ImageView previewShortcut;
    private TextView previewHint;
    private TextView shortcutValue;
    private TextView panelEndpointState;
    private android.widget.LinearLayout panelShortcutsContainer;
    private View panelShortcutAddButton;
    private View customSection;
    private RadioGroup themeGroup;
    private RadioGroup barStyleGroup;
    private boolean binding;
    private int previewTaps;
    private long previewTapWindowStart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // App-icon shortcuts land here with a widget://shortcut/*
        // data URI: route and finish before the page draws.
        if (routeShortcut(getIntent())) return;
        // The launcher label is the app name ("Muse Widget"); the
        // page itself is still titled Customize.
        setTitle(R.string.customize_title);
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
        findViewById(R.id.open_activities_button).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(WidgetCustomizeActivity.this, PickerActivity.class));
            }
        });
        panelEndpointState = findViewById(R.id.panel_endpoint_state);
        findViewById(R.id.panel_endpoint_set_button).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showPanelEndpointDialog(); }
        });
        findViewById(R.id.panel_endpoint_clear_button).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                WidgetPrefs.clearPanelEndpoint(WidgetCustomizeActivity.this);
                updatePanelEndpointState();
                MusePanelWidgetProvider.poke(WidgetCustomizeActivity.this);
            }
        });
        updatePanelEndpointState();
        panelShortcutsContainer = findViewById(R.id.panel_shortcuts_container);
        panelShortcutAddButton = findViewById(R.id.panel_shortcut_add_button);
        panelShortcutAddButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showPanelShortcutAddDialog(); }
        });
        renderPanelShortcuts();
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
        if (BarStyle.useDynamicLogo(this, widgetId)) {
            previewLogo.setImageBitmap(BarStyle.dynamicLogoBitmap(this));
        } else {
            previewLogo.setImageResource(R.drawable.ic_muse_m);
            previewLogo.setColorFilter(accent);
        }
        previewHint.setTextColor(BarStyle.hintColor(this, widgetId));
        String action = WidgetPrefs.shortcutAction(this, widgetId);
        UriCatalog.Entry target = WidgetPrefs.shortcutTargetEntry(this, widgetId);
        if (target == null && WidgetPrefs.SHORTCUT_NONE.equals(action)) {
            previewShortcut.setVisibility(View.GONE);
            shortcutValue.setText(R.string.value_hidden);
        } else {
            previewShortcut.setVisibility(View.VISIBLE);
            previewShortcut.setImageResource(target != null ? target.icon
                    : WidgetPrefs.SHORTCUT_ASSIST.equals(action)
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

    /**
     * The panel endpoint row shows only the configured STATE —
     * "Configured · <host>" or the not-set line. The full URL is
     * never rendered back onto the screen after it is saved.
     */
    private void updatePanelEndpointState() {
        if (panelEndpointState == null) return;
        String url = WidgetPrefs.panelEndpoint(this);
        if (url.isEmpty()) {
            panelEndpointState.setText(R.string.panel_endpoint_not_set);
            return;
        }
        String host = android.net.Uri.parse(url).getHost();
        panelEndpointState.setText(host != null && !host.isEmpty()
                ? "Configured \u00b7 " + host : "Configured");
    }

    /** The ONLY configuration path for the panel endpoint: paste
     *  the full URL, save, done. Nothing ships pre-configured. */
    private void showPanelEndpointDialog() {
        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_URI
                | android.text.InputType.TYPE_CLASS_TEXT);
        input.setHint(R.string.panel_endpoint_hint);
        input.setSingleLine(false);
        int pad = Math.round(20 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad / 2, pad, pad / 2);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.panel_endpoint_section)
                .setMessage(R.string.panel_endpoint_dialog_msg)
                .setView(input)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save,
                        new DialogInterface.OnClickListener() {
                            @Override public void onClick(DialogInterface d, int which) {
                                String url = input.getText().toString().trim();
                                if (url.startsWith("http://") || url.startsWith("https://")) {
                                    WidgetPrefs.setPanelEndpoint(
                                            WidgetCustomizeActivity.this, url);
                                    updatePanelEndpointState();
                                    // Light the panel + its widgets up now.
                                    MusePanelWidgetProvider.poke(
                                            WidgetCustomizeActivity.this);
                                }
                            }
                        })
                .create();
        dialog.show();
        toBottom(dialog);
    }

    private void showShortcutDialog() {
        // The right button can be any picker activity (v4.10.0):
        // its tap fires the activity's prompt/direct surface
        // (ShortcutTargets). The old Assistant / Open app choices
        // are picker entries now too; Hidden stays as the last
        // choice. Bottom-anchored per the spec.
        final java.util.List<UriCatalog.Entry> entries = UriCatalog.pickerEntries();
        final String[] labels = new String[entries.size() + 1];
        for (int i = 0; i < entries.size(); i++) labels[i] = entries.get(i).label;
        labels[entries.size()] = getString(R.string.shortcut_none);
        int checked = entries.size();
        UriCatalog.Entry current = WidgetPrefs.shortcutTargetEntry(this, widgetId);
        if (current != null) {
            for (int i = 0; i < entries.size(); i++) {
                if (entries.get(i).label.equals(current.label)) checked = i;
            }
        } else if (!WidgetPrefs.SHORTCUT_NONE.equals(
                WidgetPrefs.shortcutAction(this, widgetId))) {
            String want = WidgetPrefs.SHORTCUT_ASSIST.equals(
                    WidgetPrefs.shortcutAction(this, widgetId))
                    ? "Assistant" : "Open Muse app";
            for (int i = 0; i < entries.size(); i++) {
                if (entries.get(i).label.equals(want)) checked = i;
            }
        }
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setTitle(R.string.shortcut_dialog_title)
                .setSingleChoiceItems(labels, checked,
                        new android.content.DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(android.content.DialogInterface d, int which) {
                                if (which >= entries.size()) {
                                    WidgetPrefs.setShortcutAction(
                                            WidgetCustomizeActivity.this, widgetId,
                                            WidgetPrefs.SHORTCUT_NONE);
                                } else {
                                    WidgetPrefs.setShortcutActivity(
                                            WidgetCustomizeActivity.this, widgetId,
                                            entries.get(which).label);
                                }
                                changed();
                                d.dismiss();
                            }
                        })
                .create();
        dialog.show();
        toBottom(dialog);
    }

    /** The Shortcut panel section: the widget's cells, edited in
     *  place — a row per cell with a Remove button, plus the Add
     *  button (gone at the 8-cell cap). A global setting; every
     *  change re-renders placed panels immediately. */
    private void renderPanelShortcuts() {
        if (panelShortcutsContainer == null) return;
        panelShortcutsContainer.removeAllViews();
        final java.util.List<String> labels = WidgetPrefs.panelShortcuts(this);
        for (final String label : labels) {
            UriCatalog.Entry e = ShortcutTargets.entryForLabel(label);
            android.widget.LinearLayout row = new android.widget.LinearLayout(this);
            row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setMinimumHeight(dp(52));
            android.widget.ImageView icon = new android.widget.ImageView(this);
            icon.setImageResource(e != null ? e.icon : R.drawable.ic_grid);
            icon.setColorFilter(getColor(R.color.secondary));
            row.addView(icon, new android.widget.LinearLayout.LayoutParams(dp(24), dp(24)));
            TextView text = new TextView(this);
            text.setText(label);
            text.setTextSize(16);
            text.setTextColor(getColor(R.color.foreground));
            android.widget.LinearLayout.LayoutParams textLp =
                    new android.widget.LinearLayout.LayoutParams(
                            0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            textLp.setMarginStart(dp(16));
            row.addView(text, textLp);
            android.widget.Button remove = new android.widget.Button(this);
            remove.setText(R.string.remove);
            remove.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    java.util.List<String> next = WidgetPrefs.panelShortcuts(
                            WidgetCustomizeActivity.this);
                    next.remove(label);
                    WidgetPrefs.setPanelShortcuts(WidgetCustomizeActivity.this, next);
                    WidgetPrefs.refreshWidgets(WidgetCustomizeActivity.this);
                    renderPanelShortcuts();
                }
            });
            row.addView(remove);
            panelShortcutsContainer.addView(row);
        }
        if (panelShortcutAddButton != null) {
            panelShortcutAddButton.setVisibility(
                    labels.size() < WidgetPrefs.MAX_PANEL_SHORTCUTS
                            ? View.VISIBLE : View.GONE);
        }
    }

    private void showPanelShortcutAddDialog() {
        final java.util.List<String> current = WidgetPrefs.panelShortcuts(this);
        final java.util.List<UriCatalog.Entry> choices = new java.util.ArrayList<>();
        for (UriCatalog.Entry e : UriCatalog.pickerEntries()) {
            if (!current.contains(e.label)) choices.add(e);
        }
        final String[] labels = new String[choices.size()];
        for (int i = 0; i < choices.size(); i++) labels[i] = choices.get(i).label;
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setTitle(R.string.add_shortcut)
                .setItems(labels, new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int which) {
                        java.util.List<String> next = WidgetPrefs.panelShortcuts(
                                WidgetCustomizeActivity.this);
                        next.add(choices.get(which).label);
                        WidgetPrefs.setPanelShortcuts(WidgetCustomizeActivity.this, next);
                        WidgetPrefs.refreshWidgets(WidgetCustomizeActivity.this);
                        renderPanelShortcuts();
                    }
                })
                .create();
        dialog.show();
        toBottom(dialog);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    /** App-icon shortcut routing (see res/xml/shortcuts.xml). */
    private boolean routeShortcut(Intent intent) {
        android.net.Uri data = intent != null ? intent.getData() : null;
        if (data == null || !"widget".equals(data.getScheme())) return false;
        String uri = data.toString();
        if (uri.endsWith("/ask")) {
            startActivity(new Intent(this, ComposeActivity.class));
        } else if (uri.endsWith("/muse")) {
            startActivity(new Intent(this, MusePanelActivity.class));
        } else if (uri.endsWith("/new")) {
            Router.dispatch(this, UriCatalog.MODE_ASSIST, null, null);
        } else if (uri.endsWith("/activities")) {
            startActivity(new Intent(this, PickerActivity.class));
        } else {
            return false;
        }
        finish();
        return true;
    }

    /** One UI spec: dialogs rise from the bottom of the screen. */
    static void toBottom(android.app.AlertDialog dialog) {
        android.view.Window w = dialog.getWindow();
        if (w == null) return;
        w.setGravity(android.view.Gravity.BOTTOM);
        android.view.WindowManager.LayoutParams lp = w.getAttributes();
        lp.width = android.view.WindowManager.LayoutParams.MATCH_PARENT;
        w.setAttributes(lp);
    }

}
