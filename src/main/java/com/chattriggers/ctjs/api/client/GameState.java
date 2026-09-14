package com.chattriggers.ctjs.api.client;

import com.chattriggers.ctjs.api.triggers.TriggerType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.regex.Pattern;

/** Connection causes are captured at their origin; script callbacks run only on the client thread. */
public final class GameState {
    private static final Map<Connection, Cause> causes = new WeakHashMap<>();
    private static final Pattern BAN = Pattern.compile(
        "(?i)(\\byou (?:are|have been) (?:permanently |temporarily )?banned\\b|\\bban id\\s*:|\\bbanned from this server\\b|你.{0,12}(?:被封禁|已被封禁)|封禁 ID)");
    private static volatile Connection active;
    private static String server = "";
    private static String connectingSource = "user";
    private static boolean connecting;
    private static boolean closed = true;
    private static long sequence;
    private static volatile Event current = new Event(0, "OFFLINE", "none", "client", "", "");

    private GameState() {}

    private record Cause(String cause, String source) {}

    public static final class Event {
        public final long id;
        public final String state;
        public final String cause;
        public final String source;
        public final String reason;
        public final String server;

        private Event(long id, String state, String cause, String source, String reason, String server) {
            this.id = id;
            this.state = state;
            this.cause = cause;
            this.source = source;
            this.reason = reason;
            this.server = server;
        }
    }

    public static Event getCurrent() { return current; }

    private static void emit(String state, String cause, String source, String reason) {
        current = new Event(++sequence, state, cause, source, reason, server);
        TriggerType.GAME_STATE_CHANGED.triggerAll(current);
    }

    public static synchronized void mark(Connection connection, String cause, String source) {
        if (connection != null) causes.putIfAbsent(connection, new Cause(cause, source));
    }

    public static void markActive(String cause, String source) {
        var listener = Minecraft.getInstance().getConnection();
        mark(listener == null ? active : listener.getConnection(), cause, source);
    }

    public static boolean isScriptCall() {
        return StackWalker.getInstance().walk(frames -> frames.anyMatch(frame ->
            frame.getClassName().equals("org.mozilla.javascript.NativeJavaMethod")));
    }

    public static void markLocal(Connection connection) {
        boolean script = isScriptCall();
        mark(connection, script ? "script" : "unexpected", script ? "unattributed" : "client");
    }

    public static void withConnectingSource(String source, Runnable action) {
        String previous = connectingSource;
        connectingSource = source;
        try { action.run(); }
        finally { connectingSource = previous; }
    }

    public static void connecting(ServerData data, boolean transferring) {
        if (!closed) {
            markActive(transferring ? "transfer" : "manual", transferring ? "server" : connectingSource);
            disconnecting(transferring);
        }
        active = null;
        server = data.ip;
        closed = false;
        connecting = true;
        emit("CONNECTING", transferring ? "transfer" : connectingSource.equals("user") ? "manual" : "script", connectingSource, "");
    }

    public static void bind(Connection connection) { active = connection; }

    public static void worldChanging() {
        if (closed) return;
        emit("TRANSITION", "world_change", "server", "");
    }

    public static void worldLoaded() {
        var mc = Minecraft.getInstance();
        if (mc.getConnection() != null) active = mc.getConnection().getConnection();
        if (mc.getCurrentServer() != null) server = mc.getCurrentServer().ip;
        else server = "";
        closed = false;
        connecting = false;
        emit("PLAYING", "world_load", "server", "");
    }

    public static void connectionClosed(Connection connection, DisconnectionDetails details) {
        if (connection != active || closed) return;
        finish(connection, details == null ? Component.empty() : details.reason(), false);
    }

    public static void disconnecting(boolean transferring) {
        if (closed || connecting) return;
        var mc = Minecraft.getInstance();
        var listener = mc.getConnection();
        var connection = listener == null ? active : listener.getConnection();
        var details = connection == null ? null : connection.getDisconnectionDetails();
        finish(connection, details == null ? Component.empty() : details.reason(), transferring);
    }

    private static void finish(Connection connection, Component reason, boolean transferring) {
        Cause cause;
        synchronized (GameState.class) { cause = causes.get(connection); }
        if (cause == null) cause = connecting ? new Cause("unexpected", "network") : new Cause("manual", "user");
        String text = reason.getString();
        if (transferring) cause = new Cause("transfer", "server");
        else if (cause.source.equals("server") && BAN.matcher(text.replaceAll("§.", "")).find())
            cause = new Cause("banned", "server");
        closed = true;
        connecting = false;
        emit(cause.cause.equals("transfer") ? "TRANSITION" : "DISCONNECTED", cause.cause, cause.source, text);
    }

    public static void cancelConnecting() {
        if (!connecting || closed) return;
        closed = true;
        connecting = false;
        emit("DISCONNECTED", "manual", "user", "Connection cancelled");
    }

    public static void connectionFailed(Component reason) {
        if (!connecting || closed) return;
        mark(active, "unexpected", "network");
        finish(active, reason, false);
    }
}
