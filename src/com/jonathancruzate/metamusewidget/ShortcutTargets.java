package com.jonathancruzate.metamusewidget;

import android.content.Context;
import android.content.Intent;

/**
 * How a picker activity (see UriCatalog.pickerEntries) is fired
 * as a shortcut — from the search-bar widget's right button, the
 * shortcut panel widget's cells, the ask page's glyph, or any
 * other surface that assigns one (v4.10.0). One code path for
 * every surface:
 *
 *   - ASK activities open the ask page with their prompt stem
 *     prefilled (the picker's own behavior), in a fresh task.
 *   - DIRECT activities (Voice, Assistant, Open Muse app) ride
 *     the LaunchMuseActivity trampoline into Router's ungated
 *     surfaces.
 *
 * Nothing here fires a catalog URI: Muse gates those by caller
 * signature, which is the whole reason the activities exist.
 */
public final class ShortcutTargets {

    private ShortcutTargets() {}

    public static Intent intentFor(Context ctx, UriCatalog.Entry e) {
        if (UriCatalog.pickerAction(e) == UriCatalog.PICKER_DIRECT) {
            Intent i = new Intent(ctx, LaunchMuseActivity.class);
            int mode = ("Voice".equals(e.label) || "Assistant".equals(e.label))
                    ? UriCatalog.MODE_ASSIST : UriCatalog.MODE_LAUNCH;
            i.putExtra(Router.EXTRA_MODE, mode);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            return i;
        }
        Intent i = new Intent(ctx, ComposeActivity.class);
        i.putExtra(ComposeActivity.EXTRA_PREFILL, UriCatalog.pickerPrompt(e));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return i;
    }

    public static void fire(Context ctx, UriCatalog.Entry e) {
        ctx.startActivity(intentFor(ctx, e));
    }

    /** Resolve a stored shortcut-panel cell label to its entry. */
    public static UriCatalog.Entry entryForLabel(String label) {
        if (label == null) return null;
        for (UriCatalog.Entry e : UriCatalog.pickerEntries()) {
            if (e.label.equals(label)) return e;
        }
        return null;
    }
}
