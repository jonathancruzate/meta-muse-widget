package com.jonathancruzate.metamusewidget;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * The Muse Launcher Pack setup wizard (Pack v5.0.0, design
 * Section 6.2): every step checks itself against live state,
 * so the wizard doubles as the drift report. Steps that are
 * Jonathan's on-phone acts (Good Lock layout work, pairing
 * confirmation inside Device Assistance, permission grants)
 * are stated as such, with the button that takes him to the
 * exact place.
 */
public class SetupWizardActivity extends Activity {

    private static final int REQ_CALENDAR = 7001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.pack_wizard_title);
        build();
    }

    @Override
    protected void onResume() {
        super.onResume();
        build();
    }

    private void build() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(48, 48, 48, 48);
        scroll.addView(root);
        TextView intro = new TextView(this);
        intro.setText(R.string.pack_wizard_intro);
        root.addView(intro);

        // Panel endpoint
        boolean endpoint = PanelDataClient.isConfigured(this);
        root.addView(row(getString(R.string.pack_wizard_endpoint),
                getString(endpoint
                        ? R.string.pack_wizard_endpoint_ok
                        : R.string.pack_wizard_endpoint_missing)));
        Button settingsBtn = new Button(this);
        settingsBtn.setText(R.string.pack_wizard_open_settings);
        settingsBtn.setOnClickListener(
                new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                startActivity(new Intent(SetupWizardActivity.this,
                        WidgetCustomizeActivity.class));
            }
        });
        root.addView(settingsBtn);

        // Widgets placed / drift
        root.addView(row(getString(R.string.pack_wizard_widgets),
                PackDrift.summary(this) + " · " + getString(
                        PackDrift.layoutMatchesSpec(this)
                                ? R.string.pack_setup_ok
                                : R.string.pack_setup_drift)));

        // Device Assistance
        boolean installed = DaClient.daInstalled(this);
        boolean paired = PackPrefs.daPaired(this);
        root.addView(row(getString(R.string.pack_wizard_da),
                getString(!installed
                        ? R.string.pack_wizard_da_missing
                        : paired ? R.string.pack_wizard_da_paired
                                : R.string.pack_wizard_da_unpaired)));
        if (installed && !paired) {
            Button pairBtn = new Button(this);
            pairBtn.setText(R.string.pack_wizard_pair_button);
            pairBtn.setOnClickListener(
                    new android.view.View.OnClickListener() {
                @Override
                public void onClick(android.view.View v) {
                    final android.content.Context app =
                            getApplicationContext();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            final boolean ok =
                                    DaClient.claimPairing(app);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(
                                            SetupWizardActivity.this,
                                            ok ? R.string.pack_pair_ok
                                                    : R.string.pack_pair_failed,
                                            Toast.LENGTH_LONG).show();
                                    PackWidgetProvider.pokeAll(app);
                                    build();
                                }
                            });
                        }
                    }).start();
                }
            });
            root.addView(pairBtn);
        }
        if (paired) {
            Button revokeBtn = new Button(this);
            revokeBtn.setText(R.string.pack_wizard_revoke_button);
            revokeBtn.setOnClickListener(
                    new android.view.View.OnClickListener() {
                @Override
                public void onClick(android.view.View v) {
                    final android.content.Context app =
                            getApplicationContext();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            DaClient.revoke(app);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    build();
                                }
                            });
                        }
                    }).start();
                }
            });
            root.addView(revokeBtn);
        }

        // Calendar permission
        boolean calendar = checkSelfPermission(
                Manifest.permission.READ_CALENDAR)
                == PackageManager.PERMISSION_GRANTED;
        root.addView(row(getString(R.string.pack_wizard_calendar),
                getString(calendar
                        ? R.string.pack_wizard_calendar_ok
                        : R.string.pack_wizard_calendar_missing)));
        if (!calendar) {
            Button calBtn = new Button(this);
            calBtn.setText(R.string.pack_wizard_calendar);
            calBtn.setOnClickListener(
                    new android.view.View.OnClickListener() {
                @Override
                public void onClick(android.view.View v) {
                    requestPermissions(
                            new String[]{
                                    Manifest.permission.READ_CALENDAR},
                            REQ_CALENDAR);
                }
            });
            root.addView(calBtn);
        }

        // Good Lock
        boolean goodLock = isInstalled(
                "com.samsung.android.goodlock");
        root.addView(row(getString(R.string.pack_wizard_goodlock),
                getString(goodLock
                        ? R.string.pack_wizard_goodlock_ok
                        : R.string.pack_wizard_goodlock_missing)));

        // Health Connect (Sleep tile, v5.2.0)
        boolean health = PackPrefs.sleepSummary(this) != null;
        root.addView(row(getString(R.string.pack_wizard_health),
                getString(health
                        ? R.string.pack_wizard_health_ok
                        : R.string.pack_wizard_health_missing)));
        if (!health) {
            Button healthBtn = new Button(this);
            healthBtn.setText(R.string.pack_wizard_health);
            healthBtn.setOnClickListener(
                    new android.view.View.OnClickListener() {
                @Override
                public void onClick(android.view.View v) {
                    try {
                        startActivity(new Intent(
                                "android.health.connect.action.HEALTH_HOME_SETTINGS"));
                    } catch (Exception e) {
                        try {
                            startActivity(new Intent(
                                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    android.net.Uri.parse(
                                            "package:" + getPackageName())));
                        } catch (Exception ignored) {
                            // Nowhere sensible to land.
                        }
                    }
                }
            });
            root.addView(healthBtn);
        }

        Button recheck = new Button(this);
        recheck.setText(R.string.pack_wizard_recheck);
        recheck.setOnClickListener(
                new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                build();
            }
        });
        root.addView(recheck);
        setContentView(scroll);
    }

    private TextView row(String title, String status) {
        TextView t = new TextView(this);
        t.setText("\n" + title + "\n" + status);
        t.setPadding(0, 16, 0, 16);
        return t;
    }

    private boolean isInstalled(String pkg) {
        try {
            getPackageManager().getPackageInfo(pkg, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
