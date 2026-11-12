package com.dg.client.command;

import com.dg.client.DGClient;
import com.dg.client.core.DGLogger;
import com.dg.client.core.FriendManager;
import com.dg.client.core.WaypointManager;
import com.dg.client.core.detection.DetectionEntry;
import com.dg.client.module.Module;
import com.dg.client.module.modules.world.YZeroBypassModule;
import com.dg.client.module.modules.world.YZeroDiagnosticsModule;
import com.dg.client.util.ChatUtil;
import com.dg.client.util.KeyUtil;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.MinecraftClient;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Prefix "." chat command dispatcher (.help, .toggle, .bind, .friend, .waypoint,
 * .profile, .basefinder, .y0, .say, .debug).
 */
public final class CommandManager {
    public static final String PREFIX = ".";

    public interface CommandExecutor {
        void execute(String[] args);
    }

    public record CommandEntry(String name, String usage, String description, CommandExecutor executor) {
    }

    private final Map<String, CommandEntry> commands = new LinkedHashMap<>();
    private final Map<String, String> aliases = new LinkedHashMap<>();
    private boolean bypassIntercept = false;

    public void init() {
        registerBuiltins();

        ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
            if (bypassIntercept) {
                return true;
            }
            if (message != null && message.startsWith(PREFIX) && message.length() > 1) {
                handleCommandLine(message.substring(PREFIX.length()).trim());
                return false;
            }
            return true;
        });
    }

    public void register(String name, String usage, String description, CommandExecutor executor, String... aliasNames) {
        String key = name.toLowerCase(Locale.ROOT);
        commands.put(key, new CommandEntry(key, usage, description, executor));
        for (String a : aliasNames) {
            aliases.put(a.toLowerCase(Locale.ROOT), key);
        }
    }

    public void sendRawChatBypassingChecks(String text) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getNetworkHandler() == null) {
            return;
        }
        try {
            bypassIntercept = true;
            mc.getNetworkHandler().sendChatMessage(text);
        } finally {
            bypassIntercept = false;
        }
    }

    public boolean tryExecute(String rawMessage) {
        if (rawMessage == null || !rawMessage.startsWith(PREFIX) || rawMessage.length() <= PREFIX.length()) {
            return false;
        }
        handleCommandLine(rawMessage.substring(PREFIX.length()).trim());
        return true;
    }

    public void handleCommandLine(String line) {
        if (line.isEmpty()) {
            return;
        }
        String[] parts = line.split("\\s+");
        String cmdKey = parts[0].toLowerCase(Locale.ROOT);
        if (aliases.containsKey(cmdKey)) {
            cmdKey = aliases.get(cmdKey);
        }
        CommandEntry entry = commands.get(cmdKey);
        if (entry == null) {
            ChatUtil.warn("Unknown command '." + parts[0] + "'. Type .help for all commands.");
            return;
        }
        String[] args = parts.length > 1 ? Arrays.copyOfRange(parts, 1, parts.length) : new String[0];
        try {
            entry.executor().execute(args);
        } catch (Throwable t) {
            DGLogger.error("Command failed: ." + line, t);
            ChatUtil.warn("Command error: " + t.getMessage());
        }
    }

    private void registerBuiltins() {
        register("help", ".help", "Lists all DGClient commands", args -> {
            ChatUtil.accent("Commands", "(" + commands.size() + " available):");
            for (CommandEntry c : commands.values()) {
                ChatUtil.accent("  " + c.usage(), "- " + c.description());
            }
        }, "h", "cmds");

        register("toggle", ".toggle <module>", "Toggles a module on or off", args -> {
            if (args.length < 1) {
                ChatUtil.warn("Usage: .toggle <module>");
                return;
            }
            String target = String.join("", args);
            Module module = DGClient.getInstance().getModuleManager().getByName(target);
            if (module == null) {
                ChatUtil.warn("Module not found: " + args[0]);
                return;
            }
            module.toggle();
            ChatUtil.accent(module.getName(), module.isEnabled() ? "ENABLED" : "DISABLED");
        }, "t");

        register("bind", ".bind <module> <key|none>", "Binds a module to a keyboard key", args -> {
            if (args.length < 2) {
                ChatUtil.warn("Usage: .bind <module> <key|none>");
                return;
            }
            String keyStr = args[args.length - 1];
            String modName = String.join("", Arrays.copyOf(args, args.length - 1));
            Module mod = DGClient.getInstance().getModuleManager().getByName(modName);
            if (mod == null) {
                ChatUtil.warn("Module not found: " + modName);
                return;
            }
            if (keyStr.equalsIgnoreCase("none") || keyStr.equalsIgnoreCase("clear")) {
                mod.getKeybind().unbind();
                DGClient.getInstance().syncGuiKeybindFromModule();
                DGClient.getInstance().getConfigManager().saveAll();
                ChatUtil.accent(mod.getName(), "Keybind cleared.");
                return;
            }
            int code = KeyUtil.parseKey(keyStr);
            if (code <= 0) {
                ChatUtil.warn("Unknown key '" + keyStr + "' (example: R, G, V, F6, RSHIFT, none)");
                return;
            }
            mod.getKeybind().setValue(code);
            DGClient.getInstance().syncGuiKeybindFromModule();
            DGClient.getInstance().getConfigManager().saveAll();
            ChatUtil.accent(mod.getName(), "Bound to " + KeyUtil.getKeyName(code));
        }, "b");

        register("gui", ".gui", "Opens DGClient's ClickGUI screen", args -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null) {
                mc.send(() -> DGClient.getInstance().openClickGui());
            }
        }, "clickgui");

        register("friend", ".friend <add|remove|list|clear> [player]", "Manages your friends list", args -> {
            FriendManager fm = DGClient.getInstance().getFriendManager();
            if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
                List<String> list = fm.getFriends();
                ChatUtil.accent("Friends (" + list.size() + "):", list.isEmpty() ? "None" : String.join(", ", list));
                return;
            }
            String sub = args[0].toLowerCase(Locale.ROOT);
            switch (sub) {
                case "add" -> {
                    if (args.length < 2) {
                        ChatUtil.warn("Usage: .friend add <username>");
                        return;
                    }
                    boolean added = fm.addFriend(args[1]);
                    DGClient.getInstance().getConfigManager().saveAll();
                    ChatUtil.accent("Friend", added ? ("Added " + args[1]) : (args[1] + " is already a friend"));
                }
                case "remove", "del" -> {
                    if (args.length < 2) {
                        ChatUtil.warn("Usage: .friend remove <username>");
                        return;
                    }
                    boolean removed = fm.removeFriend(args[1]);
                    DGClient.getInstance().getConfigManager().saveAll();
                    ChatUtil.accent("Friend", removed ? ("Removed " + args[1]) : (args[1] + " was not in friends list"));
                }
                case "clear" -> {
                    fm.clear();
                    DGClient.getInstance().getConfigManager().saveAll();
                    ChatUtil.info("Cleared all friends.");
                }
                default -> ChatUtil.warn("Usage: .friend <add|remove|list|clear> [player]");
            }
        }, "f", "friends");

        register("waypoint", ".waypoint <add|remove|list|clear> [name] [x y z]", "Manages 3D world waypoints", args -> {
            WaypointManager wm = DGClient.getInstance().getWaypointManager();
            MinecraftClient mc = MinecraftClient.getInstance();
            if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
                List<WaypointManager.Waypoint> list = wm.getWaypointsForCurrentServer();
                if (list.isEmpty()) {
                    ChatUtil.info("No waypoints saved for this server.");
                } else {
                    ChatUtil.accent("Waypoints (" + list.size() + "):", "");
                    for (WaypointManager.Waypoint wp : list) {
                        ChatUtil.accent("  " + wp.getName(), String.format(Locale.ROOT, "(%.0f, %.0f, %.0f) [%s]",
                                wp.getX(), wp.getY(), wp.getZ(), wp.getDimension()));
                    }
                }
                return;
            }
            String sub = args[0].toLowerCase(Locale.ROOT);
            switch (sub) {
                case "add" -> {
                    if (args.length < 2 || mc.player == null) {
                        ChatUtil.warn("Usage: .waypoint add <name> [x y z]");
                        return;
                    }
                    String wpName = args[1];
                    double x = mc.player.getX();
                    double y = mc.player.getY();
                    double z = mc.player.getZ();
                    if (args.length >= 5) {
                        x = Double.parseDouble(args[2]);
                        y = Double.parseDouble(args[3]);
                        z = Double.parseDouble(args[4]);
                    }
                    wm.addWaypoint(wpName, x, y, z, false);
                    DGClient.getInstance().getConfigManager().saveAll();
                    ChatUtil.accent("Waypoint Added:", String.format(Locale.ROOT, "%s at %.0f, %.0f, %.0f", wpName, x, y, z));
                }
                case "remove", "del" -> {
                    if (args.length < 2) {
                        ChatUtil.warn("Usage: .waypoint remove <name>");
                        return;
                    }
                    boolean ok = wm.removeWaypoint(args[1]);
                    DGClient.getInstance().getConfigManager().saveAll();
                    ChatUtil.accent("Waypoint", ok ? ("Removed " + args[1]) : ("Not found: " + args[1]));
                }
                case "clear" -> {
                    wm.clearCurrentServer();
                    DGClient.getInstance().getConfigManager().saveAll();
                    ChatUtil.info("Cleared waypoints for current server.");
                }
                default -> ChatUtil.warn("Usage: .waypoint <add|remove|list|clear> [name] [x y z]");
            }
        }, "wp", "waypoints");

        register("profile", ".profile <list|save|load|create|delete|reset> [name]", "Manages DGClient config profiles", args -> {
            var cm = DGClient.getInstance().getConfigManager();
            if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
                ChatUtil.accent("Profiles (active=" + cm.getActiveProfile() + "):", String.join(", ", cm.listProfiles()));
                return;
            }
            String sub = args[0].toLowerCase(Locale.ROOT);
            switch (sub) {
                case "save" -> {
                    String target = args.length >= 2 ? args[1] : cm.getActiveProfile();
                    cm.saveProfile(target);
                    ChatUtil.accent("Profile Saved:", target);
                }
                case "load", "create" -> {
                    if (args.length < 2) {
                        ChatUtil.warn("Usage: .profile " + sub + " <name>");
                        return;
                    }
                    cm.loadProfile(args[1]);
                    ChatUtil.accent("Active Profile:", cm.getActiveProfile());
                }
                case "delete" -> {
                    if (args.length < 2) {
                        ChatUtil.warn("Usage: .profile delete <name>");
                        return;
                    }
                    boolean ok = cm.deleteProfile(args[1]);
                    ChatUtil.info(ok ? ("Deleted profile " + args[1]) : "Cannot delete 'default' or missing profile.");
                }
                case "reset" -> {
                    cm.resetCurrentProfileToDefaults();
                    ChatUtil.info("Reset profile '" + cm.getActiveProfile() + "' to defaults.");
                }
                default -> ChatUtil.warn("Usage: .profile <list|save|load|create|delete|reset> [name]");
            }
        }, "cfg", "config");

        register("basefinder", ".basefinder <list|export|import|clear>", "Inspects or exports BaseFinder detections", args -> {
            var reg = DGClient.getInstance().getDetectionRegistry();
            if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
                List<DetectionEntry> clusters = reg.getDeduplicatedClusters();
                ChatUtil.accent("BaseFinder Clusters:", clusters.size() + " bases (" + reg.getAll().size() + " chunks).");
                for (int i = 0; i < Math.min(6, clusters.size()); i++) {
                    DetectionEntry d = clusters.get(i);
                    String sz = d.getClusterSize() > 1 ? " [" + d.getClusterSize() + "ch]" : "";
                    ChatUtil.accent(String.format(Locale.ROOT, "  [%s %.0f%%%s]", d.getConfidence().name(), d.getScore(), sz),
                            String.format(Locale.ROOT, "X:%d Z:%d (%s)", d.getCenterX(), d.getCenterZ(), d.getFormattedReasons()));
                }
                return;
            }
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "export" -> {
                    String server = WaypointManager.getCurrentServerId();
                    Path json = reg.exportJson(server);
                    Path csv = reg.exportCsv(server);
                    ChatUtil.accent("Exported:", (json != null ? json.getFileName().toString() : "err")
                            + " & " + (csv != null ? csv.getFileName().toString() : "err"));
                }
                case "import" -> {
                    int count = reg.loadForServer(WaypointManager.getCurrentServerId());
                    ChatUtil.accent("Imported:", count + " detections for " + WaypointManager.getCurrentServerId());
                }
                case "clear" -> {
                    reg.clearAll();
                    ChatUtil.info("Cleared BaseFinder detections.");
                }
                default -> ChatUtil.warn("Usage: .basefinder <list|export|import|clear>");
            }
        }, "bf");

        register("y0", ".y0 <test [chunks]|save|reset|clear>", "Controls Y-0 Diagnostics and Underground Store", args -> {
            YZeroDiagnosticsModule diag = DGClient.getInstance().getModuleManager().get(YZeroDiagnosticsModule.class);
            YZeroBypassModule bypass = DGClient.getInstance().getModuleManager().get(YZeroBypassModule.class);
            if (diag == null || bypass == null) {
                return;
            }
            if (args.length == 0) {
                for (String line : diag.buildSummaryLines()) {
                    ChatUtil.info(line);
                }
                return;
            }
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "test" -> {
                    int count = 16;
                    if (args.length >= 2) {
                        try {
                            count = Math.max(4, Math.min(64, Integer.parseInt(args[1])));
                        } catch (NumberFormatException ignored) {
                        }
                    }
                    diag.beginSampleTest(count);
                }
                case "save", "report" -> {
                    Path p = diag.saveDiagnosticReport();
                    bypass.getStore().saveForCurrentServer();
                    ChatUtil.accent("Saved:", p != null ? p.getFileName().toString() : "error");
                }
                case "reset" -> {
                    diag.resetStats();
                    ChatUtil.info("Reset Y-0 diagnostic packet counters.");
                }
                case "clear" -> {
                    bypass.getStore().clear();
                    ChatUtil.info("Cleared cached Y<0 store.");
                }
                default -> ChatUtil.warn("Usage: .y0 <test [chunks]|save|reset|clear>");
            }
        }, "underground");

        register("say", ".say <message>", "Sends a raw chat message bypassing command/coordinate checks", args -> {
            if (args.length == 0) {
                ChatUtil.warn("Usage: .say <message>");
                return;
            }
            sendRawChatBypassingChecks(String.join(" ", args));
        });

        register("debug", ".debug [on|off]", "Toggles DGClient debug logging", args -> {
            boolean next = args.length == 0 ? !DGLogger.isDebugEnabled()
                    : (args[0].equalsIgnoreCase("on") || args[0].equalsIgnoreCase("true"));
            DGLogger.setDebugEnabled(next);
            ChatUtil.accent("Debug Mode:", next ? "ON" : "OFF");
        });
    }
}
