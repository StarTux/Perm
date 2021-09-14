package com.winthier.perm;

import com.winthier.perm.sql.SQLGroup;
import com.winthier.perm.sql.SQLMember;
import com.winthier.perm.sql.SQLPermission;
import com.winthier.playercache.PlayerCache;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;

@RequiredArgsConstructor
public final class PermCommand implements TabExecutor {
    private final PermPlugin plugin;

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
        default:
            return false;
        }
    }

    boolean playerCommand(CommandSender sender, String[] args) {
        if (args.length < 1) return false;
        String playerName = args[0];
        UUID playerUuid = PlayerCache.uuidForName(playerName);
        if (playerUuid == null) {
            sender.sendMessage("Player not found: " + playerName);
            return true;
        }
        playerName = PlayerCache.nameForUuid(playerUuid);
        String subcmd = args.length >= 2 ? args[1] : null;
        if ("get".equals(subcmd) && args.length == 3) {
            String perm = args[2];
            Boolean value = plugin.cache.findPlayerPerms(playerUuid).get(perm);
            sender.sendMessage(String.format("Setting for %s of %s: %s",
                                             playerName, perm, value));
            return true;
        } else if ("show".equals(subcmd)
                   && (args.length == 2 || args.length == 3)) {
            String pattern = args.length >= 3
                ? args[2]
                : null;
            if (pattern == null) {
                sender.sendMessage("Declared permissions of "
                                   + playerName + ":");
            } else {
                sender.sendMessage("Declared permissions of "
                                   + playerName + " matching "
                                   + pattern + ":");
            }
            String entityName = playerUuid.toString();
            int count = 0;
            for (SQLPermission permission : plugin.cache.permissions) {
                if (permission.getIsGroup()) continue;
                if (!entityName.equals(permission.getEntity())) continue;
                if (pattern == null
                    || permission.getPermission().contains(pattern)) {
                    sender.sendMessage("- " + permission.getPermission()
                                       + ": " + permission.getValue());
                    count += 1;
                }
            }
            sender.sendMessage("Total " + count);
            return true;
        } else if ("dump".equals(subcmd)
                   && (args.length == 2 || args.length == 3)) {
            String pattern = args.length >= 3
                ? args[2]
                : null;
            if (pattern == null) {
                sender.sendMessage("All permissions of "
                                   + playerName + ":");
            } else {
                sender.sendMessage("All permissions of "
                                   + playerName + " matching "
                                   + pattern + ":");
            }
            int count = 0;
            for (Map.Entry<String, Boolean> entry : plugin.cache.findPlayerPerms(playerUuid).entrySet()) {
                String perm = entry.getKey();
                if (pattern == null || perm.contains(pattern)) {
                    sender.sendMessage("- " + perm + ": "
                                       + entry.getValue());
                    count += 1;
                }
            }
            sender.sendMessage("Total " + count);
            return true;
        } else if ("has".equals(subcmd) && args.length == 3) {
            String perm = args[2];
            Player player = plugin.getServer().getPlayer(playerUuid);
            if (player == null) {
                sender.sendMessage(playerName + " is not online!");
                return true;
            }
            sender.sendMessage(String
                               .format("%s.hasPermission(%s) = %s",
                                       player.getName(),
                                       perm,
                                       player.hasPermission(perm)));
            return true;
        } else if ("groups".equals(subcmd) && args.length == 2) {
            sender.sendMessage(playerName + " is in groups: "
                               + plugin.findPlayerGroups(playerUuid));
            return true;
        } else if ("set".equals(subcmd)
                   && (args.length == 3 || args.length == 4)) {
            String perm = args[2];
            boolean value = args.length >= 4
                ? Boolean.parseBoolean(args[3])
                : true;
            plugin.setPlayerPerm(playerUuid, perm, value);
            sender.sendMessage(perm + " set to " + value
                               + " for " + playerName);
            return true;
        } else if ("unset".equals(subcmd) && args.length == 3) {
            String perm = args[2];
            if (plugin.setPlayerPerm(playerUuid, perm, null)) {
                sender.sendMessage(perm + " unset for " + playerName);
            } else {
                sender.sendMessage(playerName + " does not set " + perm);
            }
            return true;
        } else if ("addgroup".equals(subcmd) && args.length == 3) {
            String groupName = args[2];
            if (plugin.cache.findGroup(groupName) == null) {
                sender.sendMessage("Group not found: " + groupName);
                return true;
            }
            if (plugin.setMembership(playerUuid, groupName, true)) {
                sender.sendMessage(playerName + " added to group "
                                   + groupName);
            } else {
                sender.sendMessage(playerName + " already in group "
                                   + groupName);
            }
            return true;
        } else if ("removegroup".equals(subcmd) && args.length == 3) {
            String groupName = args[2];
            if (plugin.setMembership(playerUuid, groupName, false)) {
                sender.sendMessage(playerName + " removed from group "
                                   + groupName);
            } else {
                sender.sendMessage(playerName + " is not in group "
                                   + groupName);
            }
            return true;
        } else if ("setgroup".equals(subcmd) && args.length == 3) {
            String groupName = args[2];
            if (plugin.cache.findGroup(groupName) == null) {
                sender.sendMessage("Group not found: " + groupName);
                return true;
            }
            List<String> groups = plugin.findPlayerGroups(playerUuid);
            if (groups.size() == 1
                && groups.get(0).equals(groupName)) {
                sender.sendMessage(playerName + " already in group "
                                   + groupName);
                return true;
            }
            plugin.db.find(SQLMember.class)
                .eq("member", playerUuid)
                .delete();
            plugin.db.insert(new SQLMember(playerUuid, groupName));
            plugin.updateVersion();
            plugin.refreshPermissionsAsync();
            sender.sendMessage(playerName + " now in group " + groupName);
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
            List<String> groups = plugin.findPlayerGroups(playerUuid);
            if (!groups.contains(fromGroup.getKey())) {
                sender.sendMessage(playerName + " not in group "
                                   + fromGroup.getDisplayName());
                return true;
            }
            if (groups.contains(toGroup.getKey())) {
                sender.sendMessage(playerName + " already in group "
                                   + toGroup.getDisplayName());
                return true;
            }
            plugin.db.find(SQLMember.class)
                .eq("member", playerUuid)
                .eq("group", fromGroup.getKey())
                .delete();
            plugin.db.insert(new SQLMember(playerUuid, toGroup.getKey()));
            plugin.updateVersion();
            plugin.refreshPermissionsAsync();
            sender.sendMessage(playerName + " removed from "
                               + fromGroup.getDisplayName()
                               + " and added to "
                               + toGroup.getDisplayName());
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
            return true;
        }
    }

    boolean groupCommand(CommandSender sender, String[] args) {
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
        } else if ("show".equals(subcmd)
                   && (args.length == 2 || args.length == 3)) {
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
                if (!permission.getIsGroup()) continue;
                if (!groupName.equals(permission.getEntity())) continue;
                if (pattern == null
                    || permission.getPermission().contains(pattern)) {
                    sender.sendMessage("- " + permission.getPermission()
                                       + ": " + permission.getValue());
                    count += 1;
                }
            }
            sender.sendMessage("Total " + count);
            return true;
        } else if ("dump".equals(subcmd)
                   && (args.length == 2 || args.length == 3)) {
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
                    sender.sendMessage("- " + perm + ": "
                                       + entry.getValue());
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
                TextComponent.Builder cb = Component.text();
                cb.append(Component.text("- ", NamedTextColor.GRAY));
                cb.append(Component.text(name, NamedTextColor.GRAY));
                suggest(cb, "/perm player " + name + " ");
                cb.append(Component.space());
                cb.append(Component.text("[-]", NamedTextColor.RED));
                suggest(cb, "/perm player " + name + " removegroup " + group.getKey());
                cb.append(Component.space());
                cb.append(Component.text("[+]", NamedTextColor.BLUE));
                suggest(cb, "/perm player " + name + " addgroup ");
                cb.append(Component.space());
                cb.append(Component.text("[~]", NamedTextColor.GOLD));
                suggest(cb, "/perm player " + name + " replacegroup " + group.getKey() + " ");
                lines.add(cb.build());
                count += 1;
            }
            lines.add(Component.text("Total " + count, NamedTextColor.YELLOW));
            sender.sendMessage(Component.join(Component.newline(), lines));
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
            plugin.updateVersion();
            plugin.refreshPermissionsAsync();
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
            plugin.updateVersion();
            plugin.refreshPermissionsAsync();
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
            plugin.updateVersion();
            plugin.refreshPermissionsAsync();
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

    boolean listCommand(CommandSender sender, String[] args) {
        if (args.length > 1) return false;
        String subcmd = args.length >= 1
            ? args[0]
            : null;
        if ("groups".equals(subcmd)) {
            sender.sendMessage(Component.text("Total " + plugin.cache.groups.size() + " groups:", NamedTextColor.YELLOW));
            List<SQLGroup> groups = new ArrayList<>(plugin.cache.groups);
            Collections.sort(groups, (a, b) -> Integer.compare(a.getPriority(), b.getPriority()));
            for (SQLGroup group : groups) {
                sender.sendMessage(TextComponent.ofChildren(new Component[] {
                            Component.text("\u2022", NamedTextColor.GRAY),
                            Component.text(" " + group.getPriority(), NamedTextColor.WHITE),
                            Component.text(" " + group.getKey(), NamedTextColor.YELLOW),
                            Component.text(" " + group.getDisplayName(), NamedTextColor.GRAY),
                            (group.getParent() != null
                             ? Component.text(" \u2192" + group.getParent(), NamedTextColor.YELLOW)
                             : Component.empty()),
                        }));
            }
        } else if ("playerperms".equals(subcmd)) {
            sender.sendMessage("Assigned player permissions:");
            int count = 0;
            for (SQLPermission permission : plugin.cache.permissions) {
                if (permission.getIsGroup()) continue;
                final UUID uuid = UUID.fromString(permission.getEntity());
                String playerName = PlayerCache.nameForUuid(uuid);
                sender.sendMessage(playerName + ": "
                                   + permission.getPermission()
                                   + ": " + permission.getValue());
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

    @Override
    public List<String> onTabComplete(final CommandSender sender,
                                      final Command command,
                                      final String alias,
                                      final String[] args) {
        if (args.length == 0) return null;
        String cmd = args[0];
        String arg = args[args.length - 1];
        if (args.length == 1) {
            return contains(arg, Stream.of("player", "group", "list", "refresh"));
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
                                          "setgroup", "replacegroup"));
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
            if (in(sub, "set", "unset")) {
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
            if (in(sub, "set", "unset")) {
                if (args.length == 4) {
                    return completePermissions(arg);
                }
            }
            return Collections.emptyList();
        }
        default:
            return null;
        }
    }

    private static String[] argl(String[] args) {
        return Arrays.copyOfRange(args, 1, args.length);
    }

    private static void suggest(TextComponent.Builder cb, String cmd) {
        cb.clickEvent(ClickEvent.suggestCommand(cmd));
        Component tooltip = Component.text().content(cmd).color(NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false).build();
        cb.hoverEvent(HoverEvent.showText(tooltip));
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
