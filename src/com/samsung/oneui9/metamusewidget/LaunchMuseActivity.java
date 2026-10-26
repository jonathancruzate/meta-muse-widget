package com.samsung.oneui9.metamusewidget;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/** Trampoline: dispatches the requested action, then disappears. */
public class LaunchMuseActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent i = getIntent();
        int mode = i != null ? i.getIntExtra(Router.EXTRA_MODE, UriCatalog.MODE_LAUNCH)
                : UriCatalog.MODE_LAUNCH;
        String uri = i != null ? i.getStringExtra(Router.EXTRA_URI) : null;
        String query = i != null ? i.getStringExtra(Router.EXTRA_QUERY) : null;
        Router.dispatch(this, mode, uri, query);
        finish();
        overridePendingTransition(0, 0);
    }
}
