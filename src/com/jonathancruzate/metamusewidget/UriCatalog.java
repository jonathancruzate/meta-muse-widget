package com.jonathancruzate.metamusewidget;

import java.util.ArrayList;
import java.util.List;

/**
 * The Muse destinations this app can actually open — the activity
 * catalog. Through v4.12.0 this class carried every URI the Meta
 * Muse app (Muse from Meta, com.facebook.aura) exposes (71 entries,
 * REA catalogued from the Muse APK 13.0.0.4.159: manifest
 * intent-filters, AuraDeeplinkRouteMapper route specs, and the
 * hatch:// URI templates in the DEX string pool). v4.12.1 removed
 * every entry that cannot function from a third-party app — the
 * gated URI destinations whose only behavior was copying the URI.
 * What remains is exactly the picker's working set: prompt-stem
 * activities plus the ungated direct surfaces. The full REA
 * catalog survives in this repo's git history and in refs/.
 */
public final class UriCatalog {

    // How a destination can actually be reached from a third-party
    // app (REA-verified against Muse 13.0.0.4.159, 2026-10-26):
    // every payload URI is gated by Meta's TrustedCaller check in
    // both the deeplink handler and the main activity. The ungated
    // surfaces are the assist actions and a plain launch; every
    // catalog entry below works through one of them (or, for
    // Voice, through the voice-shortcut app's accessibility
    // toggle).
    public static final int MODE_ASSIST = 0;    // ACTION_ASSIST into Muse (composer)
    public static final int MODE_LAUNCH = 1;    // plain app launch
    public static final int MODE_HUB = 3;       // open this app's hub
    public static final int MODE_CUSTOMIZE = 4; // open this app's customize screen
    public static final int MODE_COMPOSE = 5;   // open this app's ask page
    public static final int MODE_VOICE_MODE = 6; // Muse Voice Mode Shortcut toggle (accessibility)

    public static final class Entry {
        public final String group;
        public final String label;
        public final String uri;          // launched; null = plain app launch
        public final String display;      // shown; null = same as uri
        public final String fallbackUri;  // tried after uri; may be null
        public final int icon;
        public final boolean widgetGrid;
        public int mode;

        Entry(String group, String label, String uri, String display, String fallbackUri, int icon, boolean widgetGrid) {
            this.group = group;
            this.label = label;
            this.uri = uri;
            this.display = display;
            this.fallbackUri = fallbackUri;
            this.icon = icon;
            this.widgetGrid = widgetGrid;
            this.mode = defaultMode(uri);
        }

        Entry withMode(int m) {
            this.mode = m;
            return this;
        }

        private static int defaultMode(String uri) {
            if (uri == null) return MODE_LAUNCH;
            if (Router.ASSIST_URI.equals(uri)
                    || "hatch://thread/new".equals(uri)
                    || "hatch://chat".equals(uri)
                    || uri.startsWith("hatch://search")) return MODE_ASSIST;
            return MODE_LAUNCH;
        }
    }

    private static Entry e(String g, String label, String uri, String fb, int icon) {
        return new Entry(g, label, uri, null, fb, icon, false);
    }

    private static Entry grid(String g, String label, String uri, String fb, int icon) {
        return new Entry(g, label, uri, null, fb, icon, true);
    }

    private static Entry tpl(String g, String label, String launchUri, String template, String fb, int icon) {
        return new Entry(g, label, launchUri, template, fb, icon, false);
    }

    public static final Entry[] ENTRIES = {
            // ---- Start ----
            grid("Start", "Chat", "hatch://chat", "https://muse.ai/", R.drawable.ic_chat),
            grid("Start", "New thread", "hatch://thread/new", "hatch://chat", R.drawable.ic_plus),
            e("Start", "Home", "hatch://home", "https://muse.ai/", R.drawable.ic_home),
            e("Start", "Voice", "hatch://voice", "hatch://chat", R.drawable.ic_mic),
            e("Start", "Assistant", Router.ASSIST_URI, null, R.drawable.ic_pulse),
            e("Start", "Open Muse app", null, null, R.drawable.ic_meta_muse_circle),

            // ---- Tabs ----
            grid("Tabs", "Search", "hatch://search", null, R.drawable.ic_search),
            grid("Tabs", "Feed", "hatch://feed", "https://applink.muse.ai/feed", R.drawable.ic_article),
            grid("Tabs", "Explore", "hatch://explore", "https://applink.muse.ai/explore", R.drawable.ic_bulb),
            grid("Tabs", "Goals", "hatch://goals", "https://applink.muse.ai/goals", R.drawable.ic_flag),
            grid("Tabs", "Library", "hatch://library", "https://applink.muse.ai/library", R.drawable.ic_folder),
            e("Tabs", "Spaces", "hatch://spaces", "hatch://chat", R.drawable.ic_grid),
            e("Tabs", "Memory", "hatch://memory", "hatch://library", R.drawable.ic_history),

            // ---- Status and account ----
            grid("Status and account", "Status", "hatch://status", "hatch://chat", R.drawable.ic_pulse),
            e("Status and account", "Activity", "hatch://status?tab=activity", "hatch://status", R.drawable.ic_pulse),
            e("Status and account", "Upcoming tasks", "hatch://status?tab=tasks", "hatch://status", R.drawable.ic_event),
            tpl("Status and account", "Approval", "hatch://status", "hatch://chat?approval_id=<id>", null, R.drawable.ic_check_circle),
            e("Status and account", "Settings", "hatch://settings", null, R.drawable.ic_more),
            e("Status and account", "Subscriptions", "hatch://settings?section=subscriptions", "https://applink.muse.ai/subscriptions", R.drawable.ic_star),
            e("Status and account", "Invite friends", "hatch://invite_friends", "https://applink.muse.ai/invite_friends", R.drawable.ic_gift),

            // ---- Devices ----
            e("Devices", "Settings · Devices", "hatch://settings/devices", "https://applink.muse.ai/settings/devices", R.drawable.ic_more),
            e("Devices", "Device permissions", "hatch://device_permissions", "hatch://settings/devices", R.drawable.ic_shield),
            e("Devices", "Repair a device", "hatch://devices/repair", "https://applink.muse.ai/devices/repair", R.drawable.ic_shield),

            // ---- Connect ----
            e("Connect", "Connectors", "hatch://connectors", "hatch://settings", R.drawable.ic_grid),
            e("Connect", "Connect a service", "hatch://connect", "hatch://connectors", R.drawable.ic_plus),
            e("Connect", "Disconnect a service", "hatch://disconnect", "hatch://connectors", R.drawable.ic_more),
            e("Connect", "Email setup", "hatch://email_setup", "https://applink.muse.ai/email_setup", R.drawable.ic_article),
    };

    // ---- The activity picker (v4.9.0) ----
    //
    // The picker revives the old hub's browsable destination list,
    // but no row fires a URI: Muse gates every payload deeplink by
    // caller signature (see the mode note at the top), so a row
    // instead opens the ask page with a prompt stem already typed —
    // the way Muse's own Goals and Tracking flows start, with a
    // sentence like "I want to start a goal about " waiting to be
    // finished. Enter carries the finished text into Muse through
    // the ungated ASSIST_QUERY surface. A stem ending in a space or
    // colon is meant to be completed; a full sentence can be sent
    // as-is. The few rows whose catalog mode is an ungated surface
    // (Voice, Assistant, a plain app launch) still fire directly.

    public static final int PICKER_NONE = -1;   // not a picker row
    public static final int PICKER_ASK = 0;     // ask page, prefilled
    public static final int PICKER_DIRECT = 1;  // ungated surface, now

    public static int pickerAction(Entry e) {
        String prompt = pickerPrompt(e);
        if (prompt != null) return PICKER_ASK;
        if ("Voice".equals(e.label) || "Assistant".equals(e.label)
                || "Open Muse app".equals(e.label)) return PICKER_DIRECT;
        return PICKER_NONE;
    }

    /** The ask-page prefill for a picker row; null = not a row. */
    public static String pickerPrompt(Entry e) {
        switch (e.label) {
            // Start
            case "Chat": return "";
            case "New thread": return "";
            case "Home": return "Catch me up on what's new";
            // Tabs
            case "Search": return "Look up ";
            case "Feed": return "Catch me up on my feed";
            case "Explore": return "Show me ideas about ";
            case "Goals": return "I want to start a goal about ";
            case "Library": return "Find in my library: ";
            case "Spaces": return "Tell me about my space ";
            case "Memory": return "What do you remember about ";
            // Status and account
            case "Status": return "What's my status?";
            case "Activity": return "Show me my recent activity";
            case "Upcoming tasks": return "I want to start tracking ";
            case "Approval": return "What's waiting for my approval?";
            case "Settings": return "I want to change a setting: ";
            case "Subscriptions": return "Show me my subscriptions";
            case "Invite friends": return "I want to invite a friend";
            // Devices
            case "Settings · Devices": return "Show me my devices";
            case "Device permissions": return "Check my device permissions";
            case "Repair a device": return "Help me repair my device: ";
            // Connect
            case "Connectors": return "Show me my connectors";
            case "Connect a service": return "I want to connect ";
            case "Disconnect a service": return "I want to disconnect ";
            case "Email setup": return "Help me set up my email";
            default: return null;
        }
    }

    /** Picker rows in catalog order (curated by pickerAction). */
    public static List<Entry> pickerEntries() {
        List<Entry> out = new ArrayList<>();
        for (Entry e : ENTRIES) {
            if (pickerAction(e) != PICKER_NONE) out.add(e);
        }
        return out;
    }

    private UriCatalog() {}
}
