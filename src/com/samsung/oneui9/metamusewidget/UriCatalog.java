package com.samsung.oneui9.metamusewidget;

import java.util.ArrayList;
import java.util.List;

/**
 * Every URI the Meta Muse app (Muse from Meta, com.facebook.aura) exposes.
 *
 * Sources, all from the Muse APK 13.0.0.4.159 (the build installed on
 * the target device; APKMirror copy):
 *   [M] AndroidManifest intent-filters of AuraDeeplinkHandlerActivity:
 *       scheme "hatch" (any host/path), https app links (autoVerify) for
 *       agent.meta.ai, applink.muse.ai, muse.ai, www.muse.ai, http for
 *       muse.ai / www.muse.ai, plus the /account_link_auth web-auth route.
 *   [R] AuraDeeplinkRouteMapper route specs (44 in <clinit>) and the
 *       literal hatch:// URI templates in the DEX string pool.
 *   [D] Muse client-surface docs (hatch://search deep link, muse.ai routes).
 *
 * Template URIs (with an <id> or <token> placeholder) launch their base
 * screen; the template is what the row displays.
 */
public final class UriCatalog {

    // How a destination can actually be reached from a third-party
    // app (REA-verified against Muse 13.0.0.4.159, 2026-10-26):
    // every payload URI is gated by Meta's TrustedCaller check in
    // both the deeplink handler and the main activity, so URI
    // destinations are reference-only (copy). The ungated surfaces
    // are the assist actions and a plain launch.
    public static final int MODE_ASSIST = 0;    // ACTION_ASSIST into Muse (composer)
    public static final int MODE_LAUNCH = 1;    // plain app launch
    public static final int MODE_COPY = 2;      // copy the URI (reference)
    public static final int MODE_HUB = 3;       // open this app's hub
    public static final int MODE_CUSTOMIZE = 4; // open this app's customize screen
    public static final int MODE_COMPOSE = 5;   // open this app's ask page

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
            return MODE_COPY;
        }
    }

    private static Entry e(String g, String label, String uri, String fb, int icon) {
        return new Entry(g, label, uri, null, fb, icon, false);
    }

    private static Entry grid(String g, String label, String uri, String fb, int icon) {
        return new Entry(g, label, uri, null, fb, icon, true);
    }

    private static Entry tpl(String g, String label, String launchUri, String template, String fb, int icon) {
        return new Entry(g, label, launchUri, template, fb, icon, false).withMode(MODE_COPY);
    }

    public static final Entry[] ENTRIES = {
            // ---- Start ----
            grid("Start", "Chat", "hatch://chat", "https://muse.ai/", R.drawable.ic_chat),
            grid("Start", "New thread", "hatch://thread/new", "hatch://chat", R.drawable.ic_plus),
            e("Start", "Main thread", "hatch://thread/main", "hatch://chat", R.drawable.ic_chat),
            tpl("Start", "Thread", "hatch://thread/main", "hatch://thread/<id>", "hatch://chat", R.drawable.ic_chat),
            tpl("Start", "Chat message", "hatch://chat", "hatch://chat?message_id=<id>", null, R.drawable.ic_chat),
            e("Start", "Home", "hatch://home", "https://muse.ai/", R.drawable.ic_home),
            e("Start", "Voice", "hatch://voice", "hatch://chat", R.drawable.ic_pulse),
            tpl("Start", "Assistant", Router.ASSIST_URI, "Muse assistant surface (voice and keyboard)", null, R.drawable.ic_pulse),
            e("Start", "Open Muse app", null, null, R.drawable.ic_meta_muse_circle),

            // ---- Tabs ----
            grid("Tabs", "Search", "hatch://search", null, R.drawable.ic_search),
            tpl("Tabs", "Search query", "hatch://search", "hatch://search?q=<query>", null, R.drawable.ic_search),
            grid("Tabs", "Feed", "hatch://feed", "https://applink.muse.ai/feed", R.drawable.ic_article),
            tpl("Tabs", "Feed post", "hatch://feed", "hatch://feed?unit_id=<id>", "https://applink.muse.ai/feed", R.drawable.ic_article),
            grid("Tabs", "Explore", "hatch://explore", "https://applink.muse.ai/explore", R.drawable.ic_bulb),
            e("Tabs", "Ideas", "https://applink.muse.ai/ideas", "hatch://explore", R.drawable.ic_bulb),
            grid("Tabs", "Goals", "hatch://goals", "https://applink.muse.ai/goals", R.drawable.ic_flag),
            grid("Tabs", "Library", "hatch://library", "https://applink.muse.ai/library", R.drawable.ic_folder),
            e("Tabs", "Library · Artifacts", "hatch://library?tab=artifacts", "hatch://library", R.drawable.ic_grid),
            e("Tabs", "Library · Media", "hatch://library?tab=media", "hatch://library", R.drawable.ic_image),
            tpl("Tabs", "Library item", "hatch://library", "hatch://library/item?id=<id>", null, R.drawable.ic_folder),
            e("Tabs", "Spaces", "hatch://spaces", "hatch://chat", R.drawable.ic_grid),
            tpl("Tabs", "Space", "hatch://spaces", "hatch://space/<id>", null, R.drawable.ic_grid),
            e("Tabs", "Memory", "hatch://memory", "hatch://library", R.drawable.ic_history),
            tpl("Tabs", "Memory item", "hatch://memory", "hatch://memory?path=<path>", null, R.drawable.ic_history),

            // ---- Status and account ----
            grid("Status and account", "Status", "hatch://status", "hatch://chat", R.drawable.ic_pulse),
            e("Status and account", "Activity", "hatch://status?tab=activity", "hatch://status", R.drawable.ic_pulse),
            tpl("Status and account", "Activity item", "hatch://status?tab=activity", "hatch://activity/<id>", "hatch://status", R.drawable.ic_pulse),
            e("Status and account", "Upcoming tasks", "hatch://status?tab=tasks", "hatch://status", R.drawable.ic_event),
            tpl("Status and account", "Task", "hatch://status?tab=tasks", "hatch://task/<id>", "hatch://status", R.drawable.ic_event),
            tpl("Status and account", "Approval", "hatch://status", "hatch://chat?approval_id=<id>", null, R.drawable.ic_check_circle),
            e("Status and account", "Profile and identity", "hatch://status?tab=identity", "hatch://status", R.drawable.ic_person),
            e("Status and account", "Settings", "hatch://settings", null, R.drawable.ic_more),
            tpl("Status and account", "Settings section", "hatch://settings", "hatch://settings?section=<section>", null, R.drawable.ic_more),
            e("Status and account", "Subscriptions", "hatch://settings?section=subscriptions", "https://applink.muse.ai/subscriptions", R.drawable.ic_star),
            e("Status and account", "Subscription hub", "hatch://subscription_hub?page=paywall&entry_point=chat", "https://applink.muse.ai/subscription_hub", R.drawable.ic_star),
            e("Status and account", "Buy usage", "https://applink.muse.ai/buy_usage", "hatch://subscription_hub", R.drawable.ic_star),
            e("Status and account", "Invite friends", "hatch://invite_friends", "https://applink.muse.ai/invite_friends", R.drawable.ic_gift),
            tpl("Status and account", "Invite link", "hatch://invite_friends", "hatch://invite_friends?src=<source>", null, R.drawable.ic_gift),
            e("Status and account", "Redeem invite code", "https://applink.muse.ai/redeem_invite_code", "hatch://invite_friends", R.drawable.ic_gift),
            e("Status and account", "Join Muse", "hatch://join", "hatch://invite_friends", R.drawable.ic_gift),
            tpl("Status and account", "Join with code", "hatch://join", "hatch://join?code=<code>", null, R.drawable.ic_gift),
            e("Status and account", "Invite", "hatch://invite", "hatch://invite_friends", R.drawable.ic_gift),

            // ---- Devices ----
            e("Devices", "Settings · Devices", "hatch://settings/devices", "https://applink.muse.ai/settings/devices", R.drawable.ic_more),
            e("Devices", "Device permissions", "hatch://device_permissions", "hatch://settings/devices", R.drawable.ic_shield),
            e("Devices", "Repair a device", "hatch://devices/repair", "https://applink.muse.ai/devices/repair", R.drawable.ic_shield),
            tpl("Devices", "Device action", "hatch://settings/devices", "hatch://devices/invoke/<command>", null, R.drawable.ic_shield),
            e("Devices", "Device invite", "hatch://devices/invoke/invite.upsell", "hatch://settings/devices", R.drawable.ic_shield),
            e("Devices", "Home Link order", "hatch://home_link_order", "https://applink.muse.ai/home_link_order", R.drawable.ic_home),
            e("Devices", "Enable wearables", "https://applink.muse.ai/enable-wearables", "hatch://settings/devices", R.drawable.ic_clock),

            // ---- Connect ----
            e("Connect", "Connectors", "hatch://connectors", "hatch://settings", R.drawable.ic_grid),
            e("Connect", "Connect a service", "hatch://connect", "hatch://connectors", R.drawable.ic_plus),
            e("Connect", "Connect a channel", "https://applink.muse.ai/connect/channel", "hatch://connect", R.drawable.ic_chat),
            e("Connect", "Browser credentials", "https://applink.muse.ai/connect/browser-credentials", "hatch://connect", R.drawable.ic_shield),
            e("Connect", "Disconnect a service", "hatch://disconnect", "hatch://connectors", R.drawable.ic_more),
            e("Connect", "Email setup", "hatch://email_setup", "https://applink.muse.ai/email_setup", R.drawable.ic_article),
            e("Connect", "Account link", "hatch://foa_ac_link", "hatch://connectors", R.drawable.ic_person),
            e("Connect", "Account link auth", "https://muse.ai/account_link_auth", null, R.drawable.ic_person),
            e("Connect", "Connect complete", "https://applink.muse.ai/connect/complete", "hatch://connectors", R.drawable.ic_check_circle),
            e("Connect", "Stripe Link complete", "https://applink.muse.ai/stripe_link/connect/complete", "hatch://connectors", R.drawable.ic_check_circle),
            e("Connect", "OAuth callback", "https://applink.muse.ai/api/hatch/oauth/callback", "hatch://connectors", R.drawable.ic_check_circle),

            // ---- Link types ----
            tpl("Link types", "Node card", "hatch://chat", "hatch://node-card?<params>", null, R.drawable.ic_grid),
            tpl("Link types", "Peer accept", "hatch://chat", "hatch://peer/accept#<token>", null, R.drawable.ic_person),
            tpl("Link types", "Short link", "hatch://home", "hatch://s/<code>", null, R.drawable.ic_open),
            e("Link types", "MHL link", "hatch://mhl", "https://applink.muse.ai/mhl", R.drawable.ic_open),

            // ---- Web ----
            e("Web", "Muse on the web", "https://muse.ai/", null, R.drawable.ic_open),
            e("Web", "Muse subscriptions", "https://muse.ai/subscriptions", null, R.drawable.ic_star),
            e("Web", "Terms of service", "https://muse.ai/terms", null, R.drawable.ic_article),
            e("Web", "Privacy policy", "https://muse.ai/privacy", null, R.drawable.ic_article),
            e("Web", "Download the Muse app", "https://ai.meta.com/muse/download/", null, R.drawable.ic_download),
            e("Web", "App link host", "https://applink.muse.ai/", "https://muse.ai/", R.drawable.ic_open),
            e("Web", "Agent host", "https://agent.meta.ai/", "https://muse.ai/", R.drawable.ic_open),
    };

    public static Entry findByUri(String uri) {
        if (uri == null) return null;
        for (Entry e : ENTRIES) {
            if (uri.equals(e.uri)) return e;
        }
        return null;
    }

    public static Entry firstOfGroup(String group) {
        for (Entry e : ENTRIES) {
            if (e.group.equals(group)) return e;
        }
        return null;
    }

    /**
     * The grid's cells are ACTIONS — only surfaces that actually
     * work from a third-party app (see the mode note above).
     */
    public static List<Entry> gridEntries() {
        List<Entry> out = new ArrayList<>();
        out.add(new Entry("Actions", "New chat", "hatch://thread/new", null, null,
                R.drawable.ic_plus, false));
        out.add(new Entry("Actions", "Assistant", Router.ASSIST_URI, null, null,
                R.drawable.ic_assistant, false));
        out.add(new Entry("Actions", "Search", "hatch://search", null, null,
                R.drawable.ic_search, false).withMode(MODE_COMPOSE));
        out.add(new Entry("Actions", "Voice", "hatch://voice", null, null,
                R.drawable.ic_mic, false).withMode(MODE_ASSIST));
        out.add(new Entry("Actions", "Open app", null, null, null,
                R.drawable.ic_open, false));
        out.add(new Entry("Actions", "Copy chat URI", "hatch://chat", null, null,
                R.drawable.ic_link, false).withMode(MODE_COPY));
        out.add(new Entry("Actions", "Destinations", "widget://hub", null, null,
                R.drawable.ic_grid, false).withMode(MODE_HUB));
        out.add(new Entry("Actions", "Customize", "widget://customize", null, null,
                R.drawable.ic_palette, false).withMode(MODE_CUSTOMIZE));
        return out;
    }

    private UriCatalog() {}
}
