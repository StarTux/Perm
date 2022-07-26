package com.winthier.perm;

import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandContext;
import com.cavetale.core.command.CommandNode;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.core.perm.ExtraRank;
import com.cavetale.core.perm.StaffRank;
import com.cavetale.mytems.item.font.Glyph;
import com.winthier.perm.sql.SQLGroup;
import com.winthier.perm.sql.SQLLevel;
import com.winthier.perm.sql.SQLMember;
import com.winthier.perm.sql.SQLPermission;
import com.winthier.playercache.PlayerCache;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;
import static net.kyori.adventure.text.JoinConfiguration.separator;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;

public final class PermCommand implements TabExecutor {
    private final PermPlugin plugin;
    private CommandNode rootNode;
    private CommandNode levelNode;
    private static final CommandArgCompleter COMPLETE_PERMS = CommandArgCompleter
        .supplyStream(() -> Bukkit.getPluginManager().getPermissions().stream().map(Permission::getName));

    protected PermCommand(final PermPlugin plugin) {
        this.plugin = plugin;
        rootNode = new CommandNode("perm");
        levelNode = rootNode.addChild("level")
            .description("Level commands");
        levelNode.addChild("list").denyTabCompletion()
            .description("List levels")
            .senderCaller(this::levelList);
        levelNode.addChild("info").arguments("<level>")
            .description("Print level info")
            .completers(CommandArgCompleter.integer(i -> i >= 0))
            .senderCaller(this::levelInfo);
        levelNode.addChild("set").arguments("<level> <permission> [value]")
            .description("Set level permission")
            .completers(CommandArgCompleter.integer(i -> i >= 0),
                        COMPLETE_PERMS,
                        CommandArgCompleter.BOOLEAN)
            .senderCaller(this::levelSet);
        levelNode.addChild("unset").arguments("<level> <permission>")
            .description("Unset level permission")
            .completers(CommandArgCompleter.integer(i -> i >= 0),
                        COMPLETE_PERMS)
            .senderCaller(this::levelUnset);
        levelNode.addChild("find").arguments("<permission>")
            .description("Find level permission")
            .completers(COMPLETE_PERMS)
            .senderCaller(this::levelFind);
        levelNode.addChild("tofile").denyTabCompletion()
            .description("Dump levels to file")
            .senderCaller(this::levelToFile);
        levelNode.addChild("fromfile").denyTabCompletion()
            .description("Load levels from file")
            .senderCaller(this::levelFromFile);
    }

    @Override
    public boolean onCommand(final CommandSender sender,
                             final Command command,
                             final String label,
                             final String[] args) {
        if (args.length == 0) return false;
        switch (args[0]) {
        case "refresh": {
            if (args.length != 1) return false;
            plugin.refreshPermissionsAsync();
            sender.sendMessage("Permissions refreshed.");
            return true;
        }
        case "player": return playerCommand(sender, argl(args));
        case "group": return groupCommand(sender, argl(args));
        case "list": return listCommand(sender, argl(args));
        case "local": return localCommand(sender, argl(args));
        case "level": return levelNode.call(new CommandContext(sender, command, label, args), argl(args));
        default:
            return false;
        }
    }

    private static void list(CommandSender sender, String perm, boolean value) {
        if (value) {
            sender.sendMessage(text("+ " + perm, GREEN));
        } else {
            sender.sendMessage(text("- " + perm, RED));
        }
    }

    private boolean playerCommand(CommandSender sender, String[] args) {
        if (args.length < 1) return false;
        PlayerCache target = PlayerCache.require(args[0]);
        String subcmd = args.length >= 2 ? args[1] : null;
        if ("get".equals(subcmd) && args.length == 3) {
            String perm = args[2];
            Boolean value = plugin.cache.findPlayerPerms(target.uuid).get(perm);
            sender.sendMessage(String.format("Setting for %s of %s: %s",
                                             target.name, perm, value));
            return true;
        } else if ("show".equals(subcmd) && (args.length == 2 || args.length == 3)) {
            String pattern = args.length >= 3
                ? args[2]
                : null;
            if (pattern == null) {
                sender.sendMessage("Declared permissions of "
                                   + target.name + ":");
            } else {
                sender.sendMessage("Declared permissions of "
                                   + target.name + " matching "
                                   + pattern + ":");
            }
            String entityName = target.uuid.toString();
            int count = 0;
            for (SQLPermission permission : plugin.cache.permissions) {
                if (permission.isGroup()) continue;
                if (!entityName.equals(permission.getEntity())) continue;
                if (pattern == null || permission.getPermission().contains(pattern)) {
                    list(sender, permission.getPermission(), permission.isValue());
                    count += 1;
                }
            }
            sender.sendMessage("Total " + count);
            return true;
        } else if ("dump".equals(subcmd) && (args.length == 2 || args.length == 3)) {
            String pattern = args.length >= 3
                ? args[2]
                : null;
            if (pattern == null) {
                sender.sendMessage("All permissions of "
                                   + target.name + ":");
            } else {
                sender.sendMessage("All permissions of "
                                   + target.name + " matching "
                                   + pattern + ":");
            }
            int count = 0;
            for (Map.Entry<String, Boolean> entry : plugin.cache.findPlayerPerms(target.uuid).entrySet()) {
                String perm = entry.getKey();
                if (pattern == null || perm.contains(pattern)) {
                    list(sender, entry.getKey(), entry.getValue());
                    count += 1;
                }
            }
            sender.sendMessage("Total " + count);
            return true;
        } else if ("has".equals(subcmd) && args.length == 3) {
            String perm = args[2];
            Player player = plugin.getServer().getPlayer(target.uuid);
            if (player == null) {
                sender.sendMessage(target.name + " is not online!");
                return true;
            }
            sender.sendMessage(String
                               .format("%s.hasPermission(%s) = %s",
                                       player.getName(),
                                       perm,
                                       player.hasPermission(perm)));
            return true;
        } else if ("groups".equals(subcmd) && args.length == 2) {
            sender.sendMessage(target.name + " is in groups: "
                               + plugin.findPlayerGroups(target.uuid));
            return true;
        } else if ("set".equals(subcmd)
                   && (args.length == 3 || args.length == 4)) {
            String perm = args[2];
            boolean value = args.length >= 4
                ? Boolean.parseBoolean(args[3])
                : true;
            plugin.setPlayerPerm(target.uuid, perm, value);
            sender.sendMessage(perm + " set to " + value
                               + " for " + target.name);
            return true;
        } else if ("unset".equals(subcmd) && args.length == 3) {
            String perm = args[2];
            if (plugin.setPlayerPerm(target.uuid, perm, null)) {
                sender.sendMessage(perm + " unset for " + target.name);
            } else {
                sender.sendMessage(target.name + " does not set " + perm);
            }
            return true;
        } else if ("addgroup".equals(subcmd) && args.length == 3) {
            String groupName = args[2];
            if (plugin.cache.findGroup(groupName) == null) {
                sender.sendMessage("Group not found: " + groupName);
                return true;
            }
            if (plugin.setMembership(target.uuid, groupName, true)) {
                sender.sendMessage(target.name + " added to group "
                                   + groupName);
            } else {
                sender.sendMessage(target.name + " already in group "
                                   + groupName);
            }
            return true;
        } else if ("removegroup".equals(subcmd) && args.length == 3) {
            String groupName = args[2];
            if (plugin.setMembership(target.uuid, groupName, false)) {
                sender.sendMessage(target.name + " removed from group "
                                   + groupName);
            } else {
                sender.sendMessage(target.name + " is not in group "
                                   + groupName);
            }
            return true;
        } else if ("setgroup".equals(subcmd) && args.length == 3) {
            String groupName = args[2];
            if (plugin.cache.findGroup(groupName) == null) {
                sender.sendMessage("Group not found: " + groupName);
                return true;
            }
            List<String> groups = plugin.findPlayerGroups(target.uuid);
            if (groups.size() == 1
                && groups.get(0).equals(groupName)) {
                sender.sendMessage(target.name + " already in group "
                                   + groupName);
                return true;
            }
            plugin.db.find(SQLMember.class)
                .eq("member", target.uuid)
                .delete();
            plugin.db.insert(new SQLMember(target.uuid, groupName));
            plugin.updateVersionAndRefresh();
            sender.sendMessage(target.name + " now in group " + groupName);
            return true;
        } else if ("replacegroup".equals(subcmd) && args.length == 4) {
            final String fromGroupArg = args[2];
            final String toGroupArg = args[3];
            SQLGroup fromGroup = plugin.cache.findGroup(fromGroupArg);
            if (fromGroup == null) {
                sender.sendMessage("Group not found: " + fromGroupArg);
                return true;
            }
            SQLGroup toGroup = plugin.cache.findGroup(toGroupArg);
            if (toGroup == null) {
                sender.sendMessage("Group not found: " + toGroupArg);
                return true;
            }
            List<String> groups = plugin.findPlayerGroups(target.uuid);
            if (!groups.contains(fromGroup.getKey())) {
                sender.sendMessage(target.name + " not in group "
                                   + fromGroup.getDisplayName());
                return true;
            }
            if (groups.contains(toGroup.getKey())) {
                sender.sendMessage(target.name + " already in group "
                                   + toGroup.getDisplayName());
                return true;
            }
            plugin.db.find(SQLMember.class)
                .eq("member", target.uuid)
                .eq("group", fromGroup.getKey())
                .delete();
            plugin.db.insert(new SQLMember(target.uuid, toGroup.getKey()));
            plugin.updateVersionAndRefresh();
            sender.sendMessage(target.name + " removed from "
                               + fromGroup.getDisplayName()
                               + " and added to "
                               + toGroup.getDisplayName());
            return true;
        } else if ("info".equals(subcmd) && args.length == 2) {
            StaffRank staffRank = StaffRank.ofPlayer(target.uuid);
            Set<ExtraRank> extraRanks = ExtraRank.ofPlayer(target.uuid);
            sender.sendMessage(text("Ranks of " + target.name + ":"
                                    + " staff=" + staffRank
                                    + " extra=" + extraRanks,
                                    YELLOW));
            return true;
        } else if ("level".equals(subcmd) && args.length == 2) {
            sender.sendMessage(text(target.name + " has"
                                    + " level " + plugin.getPlayerLevel(target.uuid)
                                    + " progress " + plugin.getPlayerLevelProgress(target.uuid),
                                    YELLOW));
            return true;
        } else if ("addlevel".equals(subcmd) && args.length == 2) {
            plugin.addPlayerLevelProgress(target.uuid, () -> {
                    sender.sendMessage(text(target.name + " now has"
                                            + " level " + plugin.getPlayerLevel(target.uuid)
                                            + " progress " + plugin.getPlayerLevelProgress(target.uuid),
                                            YELLOW));
                });
            return true;
        } else {
            sender.sendMessage("Usage");
            sender.sendMessage("/perm player <name> get <perm>"
                               + " - Get stored permission value");
            sender.sendMessage("/perm player <name> show [pattern]"
                               + " - List assigned permissions");
            sender.sendMessage("/perm player <name> dump [pattern]"
                               + " - List all permissions");
            sender.sendMessage("/perm player <name> has <perm>"
                               + " - Bukkit hasPermission check");
            sender.sendMessage("/perm player <name> set <perm> [value]"
                               + " - Assign permission");
            sender.sendMessage("/perm player <name> unset <perm>"
                               + " - Unassign permission");
            sender.sendMessage("/perm player <name> addgroup <group>"
                               + " - Add player to group");
            sender.sendMessage("/perm player <name> removegroup <group>"
                               + " - Remove player from group");
            sender.sendMessage("/perm player <name> setgroup <group>"
                               + " - Set sole player group");
            sender.sendMessage("/perm player <name> replacegroup <from> <to>"
                               + " - Replace player group");
            sender.sendMessage("/perm player <name> level"
                               + " - View player level");
            sender.sendMessage("/perm player <name> addlevel"
                               + " - Increase player level progress");
            return true;
        }
    }

    private boolean groupCommand(CommandSender sender, String[] args) {
        if (args.length < 1) return false;
        String groupName = args[0];
        SQLGroup group = plugin.cache.findGroup(groupName);
        if (group == null) {
            if (args.length == 2 && args[1].equals("create")) {
                group = new SQLGroup(groupName.toLowerCase(), 0, groupName, null);
                plugin.db.save(group);
                plugin.cache.groups.add(group);
                sender.sendMessage("Group created: " + groupName);
            } else {
                sender.sendMessage("Group not found: " + groupName);
            }
            return true;
        }
        groupName = group.getKey();
        String subcmd = args.length >= 2
            ? args[1]
            : null;
        if ("info".equals(subcmd) && args.length == 2) {
            sender.sendMessage(ChatColor.YELLOW + "Group Info");
            sender.sendMessage(ChatColor.GRAY + "Key: "
                               + ChatColor.WHITE + group.getKey());
            sender.sendMessage(ChatColor.GRAY + "Display: "
                               + ChatColor.WHITE + group.getDisplayName());
            sender.sendMessage(ChatColor.GRAY + "Members: "
                               + ChatColor.WHITE
                               + plugin.findGroupMembers(group.getKey()).size());
            sender.sendMessage(ChatColor.GRAY + "Prio: "
                               + ChatColor.WHITE + group.getPriority());
            StringBuilder sb = new StringBuilder("");
            String warnAboutParent = null;
            String warnAboutPrio = null;
            SQLGroup parentGroup = group;
            int prio = parentGroup.getPriority();
            while (parentGroup != null) {
                if (parentGroup != group) {
                    sb.append(" ").append(ChatColor.WHITE)
                        .append(parentGroup.getKey())
                        .append(ChatColor.GRAY).append("(")
                        .append(parentGroup.getPriority() + ")");
                }
                if (parentGroup.getParent() != null) {
                    parentGroup = plugin.cache.findGroup(parentGroup.getParent());
                    if (parentGroup == null) {
                        warnAboutParent = parentGroup.getKey();
                    } else {
                        if (prio <= parentGroup.getPriority()) {
                            warnAboutPrio = parentGroup.getKey();
                        }
                        prio = parentGroup.getPriority();
                    }
                } else {
                    parentGroup = null;
                    break;
                }
            }
            sender.sendMessage(ChatColor.GRAY + "Inherit:"
                               + ChatColor.WHITE + sb.toString());
            if (warnAboutParent != null) {
                sender.sendMessage(ChatColor.RED + "Warning: "
                                   + warnAboutParent
                                   + " has missing parent.");
            }
            if (warnAboutPrio != null) {
                sender.sendMessage(ChatColor.RED + "Warning: "
                                   + warnAboutPrio
                                   + " has priority higher than"
                                   + " or equal to at least one parent.");
            }
            return true;
        } else if ("get".equals(subcmd) && args.length == 3) {
            String perm = args[2];
            Boolean value = plugin.cache.findGroupPerms(groupName).get(perm);
            sender.sendMessage(String
                               .format("Setting for group %s of %s: %s",
                                       groupName,
                                       perm,
                                       value));
            return true;
        } else if ("show".equals(subcmd) && (args.length == 2 || args.length == 3)) {
            String pattern = args.length >= 3
                ? args[2]
                : null;
            if (pattern == null) {
                sender.sendMessage("Declared permissions of group "
                                   + groupName + ":");
            } else {
                sender.sendMessage("Declared permissions of group "
                                   + groupName + " matching "
                                   + pattern + ":");
            }
            int count = 0;
            for (SQLPermission permission : plugin.cache.permissions) {
                if (!permission.isGroup()) continue;
                if (!groupName.equals(permission.getEntity())) continue;
                if (pattern == null || permission.getPermission().contains(pattern)) {
                    list(sender, permission.getPermission(), permission.isValue());
                    count += 1;
                }
            }
            sender.sendMessage("Total " + count);
            return true;
        } else if ("dump".equals(subcmd) && (args.length == 2 || args.length == 3)) {
            String pattern = args.length >= 3
                ? args[2]
                : null;
            if (pattern == null) {
                sender.sendMessage("All permissions of group "
                                   + groupName + ":");
            } else {
                sender.sendMessage("All permissions of group "
                                   + groupName + " matching "
                                   + pattern + ":");
            }
            int count = 0;
            for (Map.Entry<String, Boolean> entry : plugin.cache.findGroupPerms(groupName).entrySet()) {
                String perm = entry.getKey();
                if (pattern == null || perm.contains(pattern)) {
                    list(sender, entry.getKey(), entry.getValue());
                    count += 1;
                }
            }
            sender.sendMessage("Total " + count);
            return true;
        } else if ("members".equals(subcmd) && args.length == 2) {
            sender.sendMessage("Members of group " + groupName + ":");
            int count = 0;
            List<Component> lines = new ArrayList<>();
            for (UUID uuid : plugin.findGroupMembers(groupName)) {
                String name = PlayerCache.nameForUuid(uuid);
                lines.add(join(noSeparators(), new Component[] {
                            text("- ", GRAY),
                            suggest(text("[-]", RED),
                                    "/perm player " + name + " removegroup " + group.getKey()),
                            suggest(text("[+]", BLUE),
                                    "/perm player " + name + " addgroup "),
                            suggest(text("[~]", GOLD),
                                    "/perm player " + name + " replacegroup " + group.getKey() + " "),
                            space(),
                            suggest(text(name, GRAY),
                                    "/perm player " + name + " "),
                        }));
                count += 1;
            }
            lines.add(text("Total " + count, YELLOW));
            sender.sendMessage(join(separator(newline()), lines));
            return true;
        } else if ("set".equals(subcmd)
                   && (args.length == 3 || args.length == 4)) {
            String perm = args[2];
            boolean value = args.length >= 4
                ? Boolean.parseBoolean(args[3])
                : true;
            plugin.setGroupPerm(groupName, perm, value);
            sender.sendMessage(perm + " set to " + value
                               + " for group " + groupName);
            return true;
        } else if ("unset".equals(subcmd) && args.length == 3) {
            String perm = args[2];
            if (plugin.setGroupPerm(groupName, perm, null)) {
                sender.sendMessage(perm + " unset for group " + groupName);
            } else {
                sender.sendMessage("Group " + groupName
                                   + " does not set " + perm);
            }
            return true;
        } else if ("add".equals(subcmd) && args.length == 3) {
            String playerName = args[2];
            UUID playerUuid = PlayerCache.uuidForName(playerName);
            if (playerUuid == null) {
                sender.sendMessage("Player not found: " + playerName);
                return true;
            }
            playerName = PlayerCache.nameForUuid(playerUuid);
            if (plugin.setMembership(playerUuid, groupName, true)) {
                sender.sendMessage(playerName + " added to group "
                                   + groupName);
            } else {
                sender.sendMessage(playerName + " already in group "
                                   + groupName);
            }
            return true;
        } else if ("remove".equals(subcmd) && args.length == 3) {
            String playerName = args[2];
            UUID playerUuid = PlayerCache.uuidForName(playerName);
            if (playerUuid == null) {
                sender.sendMessage("Player not found: " + playerName);
                return true;
            }
            playerName = PlayerCache.nameForUuid(playerUuid);
            if (plugin.setMembership(playerUuid, groupName, false)) {
                sender.sendMessage(playerName + " removed from group "
                                   + groupName);
            } else {
                sender.sendMessage(playerName + " is not in group "
                                   + groupName);
            }
            return true;
        } else if ("setpriority".equals(subcmd) && args.length == 3) {
            int prio;
            try {
                prio = Integer.parseInt(args[2]);
            } catch (NumberFormatException nfe) {
                sender.sendMessage("Not a number: " + args[2]);
                return true;
            }
            group.setPriority(prio);
            plugin.db.save(group);
            sender.sendMessage("Set priority of group " + groupName
                               + " to " + prio);
            plugin.updateVersionAndRefresh();
            return true;
        } else if ("setparent".equals(subcmd) && args.length == 3) {
            String parentName = args[2];
            SQLGroup parentGroup = plugin.cache.findGroup(parentName);
            if (parentGroup == null) {
                sender.sendMessage("Group not found: " + parentName);
                return true;
            }
            parentName = parentGroup.getKey();
            group.setParent(parentName);
            plugin.db.save(group);
            sender.sendMessage("Set parent of group of "
                               + group.getDisplayName()
                               + " to " + parentGroup.getDisplayName());
            plugin.updateVersionAndRefresh();
            return true;
        } else if ("resetparent".equals(subcmd) && args.length == 2) {
            if (group.getParent() == null) {
                sender.sendMessage(group.getDisplayName() + " has no parent");
                return true;
            }
            group.setParent(null);
            plugin.db.save(group);
            sender.sendMessage("Removed parent of group of "
                               + group.getDisplayName());
            plugin.updateVersionAndRefresh();
            return true;
        } else {
            sender.sendMessage("Usage");
            sender.sendMessage("/perm group <name> info"
                               + " - List some information");
            sender.sendMessage("/perm group <name> get <perm>"
                               + " - Get stored permission value");
            sender.sendMessage("/perm group <name> show [pattern]"
                               + " - List assigned permissions");
            sender.sendMessage("/perm group <name> dump [pattern]"
                               + " - List all permissions");
            sender.sendMessage("/perm group <name> set <perm> [value]"
                               + " - Assign permission");
            sender.sendMessage("/perm group <name> unset <perm>"
                               + " - Unassign permission");
            sender.sendMessage("/perm group <name> add <player>"
                               + " - Add player to group");
            sender.sendMessage("/perm group <name> remove <player>"
                               + " - Remove player from group");
            sender.sendMessage("/perm group <name> setpriority <prio>"
                               + " - Set group priority");
            sender.sendMessage("/perm group <name> setparent <group>"
                               + " - Set parent group");
            sender.sendMessage("/perm group <name> resetparent"
                               + " - Remove parent group");
            return true;
        }
    }

    private boolean listCommand(CommandSender sender, String[] args) {
        if (args.length > 1) return false;
        String subcmd = args.length >= 1
            ? args[0]
            : null;
        if ("groups".equals(subcmd)) {
            sender.sendMessage(text("Total " + plugin.cache.groups.size() + " groups:", YELLOW));
            List<SQLGroup> groups = new ArrayList<>(plugin.cache.groups);
            Collections.sort(groups, (a, b) -> Integer.compare(a.getPriority(), b.getPriority()));
            for (SQLGroup group : groups) {
                sender.sendMessage(join(noSeparators(), new Component[] {
                            text("\u2022", GRAY),
                            text(" " + group.getPriority(), WHITE),
                            text(" " + group.getKey(), YELLOW),
                            text(" " + group.getDisplayName(), GRAY),
                            (group.getParent() != null
                             ? text(" \u2192" + group.getParent(), YELLOW)
                             : empty()),
                        }));
            }
        } else if ("playerperms".equals(subcmd)) {
            sender.sendMessage("Assigned player permissions:");
            int count = 0;
            for (SQLPermission permission : plugin.cache.permissions) {
                if (permission.isGroup()) continue;
                final UUID uuid = UUID.fromString(permission.getEntity());
                String playerName = PlayerCache.nameForUuid(uuid);
                sender.sendMessage(playerName + ": "
                                   + permission.getPermission()
                                   + ": " + permission.isValue());
                count += 1;
            }
            sender.sendMessage("Total " + count);
        } else {
            sender.sendMessage("Usage");
            sender.sendMessage("/perm list groups"
                               + " - List groups");
            sender.sendMessage("/perm list playerperms"
                               + " - List assigned player permissions");
        }
        return true;
    }

    protected boolean localCommand(CommandSender sender, String[] args) {
        if (args.length != 0) return false;
        sender.sendMessage("Local permissions: " + plugin.localPermissionsCache);
        return true;
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender,
                                      final Command command,
                                      final String alias,
                                      final String[] args) {
        if (args.length == 0) return null;
        String cmd = args[0];
        String arg = args[args.length - 1];
        if (args.length == 1) {
            return contains(arg, Stream.of("player", "group", "list", "refresh", "level"));
        }
        switch (cmd) {
        case "list":
            if (args.length == 2) {
                return contains(arg,
                                Stream.of("groups", "playerperms"));
            }
            return Collections.emptyList();
        case "player": {
            // /perm 0player 1NAME 2sub ...
            if (args.length <= 2) return null;
            UUID uuid = PlayerCache.uuidForName(args[1]);
            if (uuid == null) return Collections.emptyList();
            if (args.length == 3) {
                return contains(arg,
                                Stream.of("get", "show", "dump", "has",
                                          "set", "unset",
                                          "groups",
                                          "addgroup", "removegroup",
                                          "setgroup", "replacegroup",
                                          "info", "level", "addlevel"));
            }
            String sub = args[2];
            if (in(sub, "addgroup", "setgroup")) {
                if (args.length == 4) {
                    return contains(arg, plugin.getGroups().stream());
                }
                return Collections.emptyList();
            }
            if (in(sub, "removegroup")) {
                if (args.length == 4) {
                    return contains(arg, plugin.findPlayerGroups(uuid).stream());
                }
                return Collections.emptyList();
            }
            if (in(sub, "replacegroup")) {
                if (args.length == 4) {
                    return contains(arg, plugin.findPlayerGroups(uuid).stream());
                }
                if (args.length == 5) {
                    return contains(arg, plugin.getGroups().stream());
                }
                return Collections.emptyList();
            }
            if (in(sub, "set", "unset", "show", "dump")) {
                if (args.length == 4) {
                    return completePermissions(arg);
                }
            }
            return Collections.emptyList();
        }
        case "group": {
            // /perm 0group 1GROUP 2sub
            if (args.length == 2) {
                return contains(arg, plugin.getGroups().stream());
            }
            if (args.length == 3) {
                return contains(arg,
                                Stream.of("info", "show", "dump",
                                          "get", "set", "unset",
                                          "members", "add", "remove",
                                          "create", "setpriority",
                                          "setparent", "resetparent"));
            }
            String sub = args[2];
            if (in(sub, "info", "remove", "resetparent")) {
                return Collections.emptyList();
            }
            if (in(sub, "setparent")) {
                if (args.length == 4) {
                    return contains(arg, plugin.getGroups().stream());
                }
                return Collections.emptyList();
            }
            if (in(sub, "setpriority")) {
                if (args.length == 4) {
                    try {
                        int i = Integer.parseInt(arg);
                        return Arrays.asList("" + i, "" + (i * 10));
                    } catch (NumberFormatException nfe) { }
                }
                return Collections.emptyList();
            }
            if (in(sub, "set", "unset", "show", "dump")) {
                if (args.length == 4) {
                    return completePermissions(arg);
                }
            }
            return Collections.emptyList();
        }
        case "level": return levelNode.complete(new CommandContext(sender, command, alias, args), argl(args));
        default:
            return null;
        }
    }

    private void levelList(CommandSender sender) {
        Map<Integer, Integer> map = new TreeMap<>();
        for (SQLLevel row : plugin.cache.levels) {
            int count = map.getOrDefault(row.getLevel(), 0);
            map.put(row.getLevel(), count + 1);
        }
        if (map.isEmpty()) throw new CommandWarn("No levels to show!");
        for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
            final int level = entry.getKey();
            final int count = entry.getValue();
            sender.sendMessage(join(noSeparators(), text("[" + level + "] ", GRAY), text(count, WHITE)));
        }
        sender.sendMessage(text("Total " + map.size() + " levels, " + plugin.cache.levels.size() + " entries"));
    }

    private boolean levelInfo(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        int level = CommandArgCompleter.requireInt(args[0], i -> i >= 0);
        List<SQLLevel> rows = new ArrayList<>();
        for (SQLLevel row : plugin.cache.levels) {
            if (row.getLevel() == level) rows.add(row);
        }
        if (rows.isEmpty()) throw new CommandWarn("Nothing to show for level " + level);
        sender.sendMessage(join(noSeparators(), text("Info for level "), Glyph.toComponent("" + level)));
        for (SQLLevel row : rows) {
            sender.sendMessage(join(noSeparators(),
                                    (row.isValue() ? text("+", AQUA) : text("-", RED)),
                                    space(),
                                    text(row.getPermission(), row.isValue() ? AQUA : RED),
                                    space(),
                                    text((row.getDescription() != null ? row.getDescription() : ""), GRAY, ITALIC)));
        }
        return true;
    }

    private boolean levelSet(CommandSender sender, String[] args) {
        if (args.length != 2 && args.length != 3) return false;
        final int level = CommandArgCompleter.requireInt(args[0], i -> i >= 0);
        final String permission = args[1];
        final boolean value = args.length >= 3
            ? CommandArgCompleter.requireBoolean(args[2])
            : true;
        SQLLevel theRow = null;
        for (SQLLevel row : plugin.cache.levels) {
            if (row.getLevel() == level && row.getPermission().equalsIgnoreCase(permission)) {
                if (value == row.isValue()) {
                    throw new CommandWarn("Level " + level + " already sets " + permission + " to " + value);
                }
                theRow = row;
                break;
            }
        }
        if (theRow != null) {
            theRow.setValue(value);
            plugin.db.updateAsync(theRow, r -> {
                    plugin.updateVersionAndRefresh();
                    sender.sendMessage(text("Level " + level + " now sets " + permission + " to " + value, AQUA));
                });
        } else {
            theRow = new SQLLevel(level, permission, value);
            plugin.db.insertAsync(theRow, r -> {
                    plugin.updateVersionAndRefresh();
                    sender.sendMessage(text("Level " + level + " now sets " + permission + " to " + value, AQUA));
                });
        }
        return true;
    }

    private boolean levelUnset(CommandSender sender, String[] args) {
        if (args.length != 2) return false;
        final int level = CommandArgCompleter.requireInt(args[0], i -> i >= 0);
        final String permission = args[1];
        SQLLevel theRow = null;
        for (SQLLevel row : plugin.cache.levels) {
            if (row.getLevel() == level && row.getPermission().equalsIgnoreCase(permission)) {
                theRow = row;
                break;
            }
        }
        if (theRow == null) {
            throw new CommandWarn("Level " + level + " does not set " + permission);
        }
        plugin.db.deleteAsync(theRow, r -> {
                plugin.updateVersionAndRefresh();
                sender.sendMessage(text("Level " + level + " no longer sets " + permission, AQUA));
            });
        return true;
    }

    private boolean levelFind(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        String term = args[0];
        String lower = term.toLowerCase();
        int total = 0;
        for (SQLLevel row : plugin.cache.levels) {
            if (row.getPermission().toLowerCase().contains(lower)) {
                boolean value = row.isValue();
                sender.sendMessage(text((value ? "+" : "-")
                                        + " [" + row.getLevel() + "]"
                                        + " " + row.getPermission(),
                                        value ? GREEN : RED));
                total += 1;
            }
        }
        if (total == 0) throw new CommandWarn("Not found: " + term);
        return true;
    }

    private void levelToFile(CommandSender sender) {
        plugin.getDataFolder().mkdirs();
        File file = new File(plugin.getDataFolder(), "levels.txt");
        int lineCount = 0;
        try (PrintStream out = new PrintStream(new FileOutputStream(file))) {
            int level = -1;
            for (SQLLevel row : plugin.cache.levels) {
                if (row.getLevel() != level) {
                    level = row.getLevel();
                    out.println("");
                    out.println("" + row.getLevel());
                    out.println("");
                }
                out.println(String.join(" ",
                                        (row.isValue() ? "+" : "-"),
                                        row.getPermission(),
                                        row.getDescription() != null ? row.getDescription() : ""));
                lineCount += 1;
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new CommandWarn(ioe.getClass().getName() + " see console");
        }
        sender.sendMessage(text(lineCount + " lines written to " + file, YELLOW));
    }

    private void levelFromFile(CommandSender sender) {
        File file = new File(plugin.getDataFolder(), "levels.txt");
        if (!file.exists()) throw new CommandWarn("File not found: " + file);
        List<SQLLevel> rows = new ArrayList<>();
        try (BufferedReader in = new BufferedReader(new FileReader(file))) {
            int level = 0;
            while (true) {
                String line = in.readLine();
                if (line == null) break;
                do {
                    int hashIndex = line.indexOf("#");
                    if (hashIndex < 0) break;
                    line = line.substring(0, hashIndex);
                } while (true);
                line = line.strip();
                if (line.isEmpty()) continue;
                String[] tokens = line.split(" ", 3);
                if (tokens.length == 1) {
                    level = CommandArgCompleter.requireInt(tokens[0], i -> i >= 0);
                } else {
                    if (tokens.length < 2) throw new CommandWarn("Invalid line: " + line);
                    boolean value = tokens[0].equals("+") ? true : false;
                    String permission = tokens[1];
                    SQLLevel row = new SQLLevel(level, permission, value);
                    if (tokens.length >= 3) row.setDescription(tokens[2]);
                    rows.add(row);
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new CommandWarn(ioe.getClass().getName() + " see console");
        }
        plugin.db.find(SQLLevel.class).delete();
        plugin.db.insert(rows);
        plugin.updateVersionAndRefresh();
        sender.sendMessage(text(rows.size() + " level lines parsed", YELLOW));
    }

    private static String[] argl(String[] args) {
        return Arrays.copyOfRange(args, 1, args.length);
    }

    private static Component suggest(Component component, String cmd) {
        Component tooltip = text().content(cmd).color(GRAY)
            .decoration(ITALIC, false).build();
        return component
            .clickEvent(ClickEvent.suggestCommand(cmd))
            .hoverEvent(HoverEvent.showText(tooltip));
    }

    private static boolean in(String key, String... haystack) {
        for (String h : haystack) {
            if (h.equals(key)) return true;
        }
        return false;
    }

    private static List<String> contains(String key, Stream<String> keys) {
        return keys
            .filter(k -> k.contains(key))
            .collect(Collectors.toList());
    }

    private List<String> completePermissions(String key) {
        return Bukkit.getPluginManager().getPermissions().stream()
            .map(Permission::getName)
            .filter(p -> p.contains(key))
            .collect(Collectors.toList());
    }
}
