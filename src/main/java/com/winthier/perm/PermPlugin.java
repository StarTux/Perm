package com.winthier.perm;

import com.winthier.playercache.PlayerCache;
import com.winthier.sql.SQLDatabase;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class PermPlugin extends JavaPlugin implements Listener {
    private SQLDatabase db;
    private Cache cache;
    private String defaultGroup = "Guest";
    private VaultPerm vaultPerm;

    static final class Cache {
        private List<SQLGroup> groups;
        private List<SQLMember> members;
        private List<SQLPermission> permissions;
        private SQLVersion version;
        SQLGroup findGroup(String name) {
            name = name.toLowerCase();
            for (SQLGroup group: groups) {
                if (name.equals(group.getKey())) return group;
            }
            return null;
        }
    }

    @Override
    public void onEnable() {
        readConfiguration();
        db = new SQLDatabase(this);
        db.registerTables(SQLGroup.class,
                          SQLMember.class,
                          SQLPermission.class,
                          SQLVersion.class);
        db.createAllTables();
        getServer().getPluginManager().registerEvents(this, this);
        refreshPermissions();
    }

    @Override
    public void onDisable() {
        for (Player player: getServer().getOnlinePlayers()) {
            resetPlayerPerms(player);
        }
    }

    void readConfiguration() {
        reloadConfig();
        defaultGroup = getConfig().getString("DefaultGroup");
    }

    void refreshPermissions() {
        this.cache = loadCache();
        for (Player player: getServer().getOnlinePlayers()) {
            resetPlayerPerms(player);
            setupPlayerPerms(player);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmd = args.length == 0 ? null : args[0].toLowerCase();
        if (cmd == null) {
            return false;
        } else if ("reload".equals(cmd) && args.length == 1) {
            readConfiguration();
            sender.sendMessage("Configuration reloaded.");
        } else if ("refresh".equals(cmd) && args.length == 1) {
            refreshPermissions();
            sender.sendMessage("Permissions refreshed.");
        } else if ("migrate".equals(cmd)) {
            try {
                Legacy.migrate(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
            sender.sendMessage("Migration complete. See console.");
        } else if ("player".equals(cmd) && args.length >= 2) {
            String playerName = args[1];
            UUID playerUuid = PlayerCache.uuidForName(playerName);
            if (playerUuid == null) {
                sender.sendMessage("Player not found: " + playerName);
                return true;
            }
            playerName = PlayerCache.nameForUuid(playerUuid);
            String subcmd = args.length >= 3 ? args[2].toLowerCase() : null;
            if ("get".equals(subcmd) && args.length == 4) {
                String perm = args[3];
                Boolean value = findPlayerPerms(playerUuid).get(perm);
                sender.sendMessage(String.format("Setting for %s of %s: %s", playerName, perm, value));
            } else if ("show".equals(subcmd) && (args.length == 3 || args.length == 4)) {
                String pattern = args.length >= 4 ? args[3].toLowerCase() : null;
                if (pattern == null) {
                    sender.sendMessage("Declared permissions of " + playerName + ":");
                } else {
                    sender.sendMessage("Declared permissions of " + playerName + " matching " + pattern + ":");
                }
                String entityName = playerUuid.toString();
                int count = 0;
                for (SQLPermission permission: getCache().permissions) {
                    if (permission.getIsGroup()) continue;
                    if (!entityName.equals(permission.getEntity())) continue;
                    if (pattern == null || permission.getPermission().contains(pattern)) {
                        sender.sendMessage("- " + permission.getPermission() + ": " + permission.getValue());
                        count += 1;
                    }
                }
                sender.sendMessage("Total " + count);
            } else if ("dump".equals(subcmd) && (args.length == 3 || args.length == 4)) {
                String pattern = args.length >= 4 ? args[3].toLowerCase() : null;
                if (pattern == null) {
                    sender.sendMessage("All permissions of " + playerName + ":");
                } else {
                    sender.sendMessage("All permissions of " + playerName + " matching " + pattern + ":");
                }
                int count = 0;
                for (Map.Entry<String, Boolean> entry: findPlayerPerms(playerUuid).entrySet()) {
                    String perm = entry.getKey();
                    if (pattern == null || perm.contains(pattern)) {
                        sender.sendMessage("- " + perm + ": " + entry.getValue());
                        count += 1;
                    }
                }
                sender.sendMessage("Total " + count);
            } else if ("has".equals(subcmd) && args.length == 4) {
                String perm = args[3];
                Player player = getServer().getPlayer(playerUuid);
                if (player == null) {
                    sender.sendMessage(playerName + " is not online!");
                    return true;
                }
                sender.sendMessage(String.format("%s.hasPermission(%s) = %s", player.getName(), perm, player.hasPermission(perm)));
            } else if ("groups".equals(subcmd) && args.length == 3) {
                sender.sendMessage(playerName + " is in groups: " + findPlayerGroups(playerUuid));
            } else if ("set".equals(subcmd) && (args.length == 4 || args.length == 5)) {
                String perm = args[3];
                boolean value = args.length >= 5 ? value = Boolean.parseBoolean(args[4]) : true;
                setPlayerPerm(playerUuid, perm, value);
                sender.sendMessage(perm + " set to " + value + " for " + playerName);
            } else if ("unset".equals(subcmd) && args.length == 4) {
                String perm = args[3];
                if (setPlayerPerm(playerUuid, perm, null)) {
                    sender.sendMessage(perm + " unset for " + playerName);
                } else {
                    sender.sendMessage(playerName + " does not set " + perm);
                }
            } else if ("addgroup".equals(subcmd) && args.length == 4) {
                String groupName = args[3];
                if (getCache().findGroup(groupName) == null) {
                    sender.sendMessage("Group not found: " + groupName);
                    return true;
                }
                if (setMembership(playerUuid, groupName, true)) {
                    sender.sendMessage(playerName + " added to group " + groupName);
                } else {
                    sender.sendMessage(playerName + " already in group " + groupName);
                }
            } else if ("removegroup".equals(subcmd) && args.length == 4) {
                String groupName = args[3];
                if (setMembership(playerUuid, groupName, false)) {
                    sender.sendMessage(playerName + " removed from group " + groupName);
                } else {
                    sender.sendMessage(playerName + " is not in group " + groupName);
                }
            } else {
                return false; // TODO usage
            }
        } else if ("group".equals(cmd) && args.length >= 2) {
            String groupName = args[1];
            SQLGroup group = getCache().findGroup(groupName);
            if (group == null) {
                sender.sendMessage("Group not found: " + groupName);
                return true;
            }
            groupName = group.getKey();
            String subcmd = args.length >= 3 ? args[2].toLowerCase() : null;
            if ("get".equals(subcmd) && args.length == 4) {
                String perm = args[3];
                Boolean value = findGroupPerms(groupName).get(perm);
                sender.sendMessage(String.format("Setting for group %s of %s: %s", groupName, perm, value));
            } else if ("show".equals(subcmd) && (args.length == 3 || args.length == 4)) {
                String pattern = args.length >= 4 ? args[3].toLowerCase() : null;
                if (pattern == null) {
                    sender.sendMessage("Declared permissions of group " + groupName + ":");
                } else {
                    sender.sendMessage("Declared permissions of group " + groupName + " matching " + pattern + ":");
                }
                int count = 0;
                for (SQLPermission permission: getCache().permissions) {
                    if (!permission.getIsGroup()) continue;
                    if (!groupName.equals(permission.getEntity())) continue;
                    if (pattern == null || permission.getPermission().contains(pattern)) {
                        sender.sendMessage("- " + permission.getPermission() + ": " + permission.getValue());
                        count += 1;
                    }
                }
                sender.sendMessage("Total " + count);
            } else if ("dump".equals(subcmd) && (args.length == 3 || args.length == 4)) {
                String pattern = args.length >= 4 ? args[3].toLowerCase() : null;
                if (pattern == null) {
                    sender.sendMessage("All permissions of group " + groupName + ":");
                } else {
                    sender.sendMessage("All permissions of group " + groupName + " matching " + pattern + ":");
                }
                int count = 0;
                for (Map.Entry<String, Boolean> entry: findGroupPerms(groupName).entrySet()) {
                    String perm = entry.getKey();
                    if (pattern == null || perm.contains(pattern)) {
                        sender.sendMessage("- " + perm + ": " + entry.getValue());
                        count += 1;
                    }
                }
                sender.sendMessage("Total " + count);
            } else if ("members".equals(subcmd) && args.length == 3) {
                sender.sendMessage("Members of group " + groupName + ":");
                int count = 0;
                for (UUID uuid: findGroupMembers(groupName)) {
                    sender.sendMessage("- " + PlayerCache.nameForUuid(uuid));
                    count += 1;
                }
                sender.sendMessage("Total " + count);
            } else if ("set".equals(subcmd) && (args.length == 4 || args.length == 5)) {
                String perm = args[3];
                boolean value = args.length >= 5 ? value = Boolean.parseBoolean(args[4]) : true;
                setGroupPerm(groupName, perm, value);
                sender.sendMessage(perm + " set to " + value + " for group " + groupName);
            } else if ("unset".equals(subcmd) && args.length == 4) {
                String perm = args[3];
                if (setGroupPerm(groupName, perm, null)) {
                    sender.sendMessage(perm + " unset for group " + groupName);
                } else {
                    sender.sendMessage("Group " + groupName + " does not set " + perm);
                }
            } else if ("add".equals(subcmd) && args.length == 4) {
                String playerName = args[3];
                UUID playerUuid = PlayerCache.uuidForName(playerName);
                if (playerUuid == null) {
                    sender.sendMessage("Player not found: " + playerName);
                    return true;
                }
                playerName = PlayerCache.nameForUuid(playerUuid);
                if (setMembership(playerUuid, groupName, true)) {
                    sender.sendMessage(playerName + " added to group " + groupName);
                } else {
                    sender.sendMessage(playerName + " already in group " + groupName);
                }
            } else if ("remove".equals(subcmd) && args.length == 4) {
                String playerName = args[3];
                UUID playerUuid = PlayerCache.uuidForName(playerName);
                if (playerUuid == null) {
                    sender.sendMessage("Player not found: " + playerName);
                    return true;
                }
                playerName = PlayerCache.nameForUuid(playerUuid);
                if (setMembership(playerUuid, groupName, false)) {
                    sender.sendMessage(playerName + " removed from group " + groupName);
                } else {
                    sender.sendMessage(playerName + " is not in group " + groupName);
                }
            } else {
                return false; // TODO usage
            }
        } else if ("list".equals(cmd)) {
            String subcmd = args.length >= 2 ? args[1].toLowerCase() : null;
            if ("groups".equals(subcmd)) {
                sender.sendMessage("Groups: " + getGroups());
            } else {
                return false; // TODO usage
            }
        } else {
            return false;
        }
        return true;
    }

    Cache loadCache() {
        Cache newCache = new Cache();
        newCache.groups = db.find(SQLGroup.class).findList();
        newCache.members = db.find(SQLMember.class).findList();
        newCache.permissions = db.find(SQLPermission.class).findList();
        newCache.version = db.find(SQLVersion.class).findUnique();
        return newCache;
    }

    Cache getCache() {
        if (cache == null) cache = loadCache();
        return cache;
    }

    Map<String, Boolean> findPlayerPerms(UUID uuid) {
        getCache();
        Map<String, Integer> groups = new HashMap<>();
        for (SQLMember mem: cache.members) {
            if (uuid.equals(mem.getMember())) {
                String groupName = mem.getGroup();
                SQLGroup group = cache.findGroup(groupName);
                while (group != null && !groups.containsKey(group.getKey())) {
                    groups.put(group.getKey(), group.getPriority());
                    if (group.getParent() == null) {
                        group = null;
                    } else {
                        group = cache.findGroup(group.getParent());
                    }
                }
            }
        }
        if (groups.isEmpty()) {
            SQLGroup group = cache.findGroup(defaultGroup);
            if (group != null) groups.put(group.getKey(), group.getPriority());
        }
        final String uuidString = uuid.toString();
        final Map<String, Boolean> perms = new HashMap<>();
        final Map<String, Integer> prios = new HashMap<>();
        for (SQLPermission perm: cache.permissions) {
            if (perm.getIsGroup()) {
                if (groups.containsKey(perm.getEntity())) {
                    Boolean oldPerm = perms.get(perm.getPermission());
                    if (oldPerm == null) {
                        perms.put(perm.getPermission(), perm.getValue());
                        prios.put(perm.getPermission(), groups.get(perm.getEntity()));
                    } else {
                        Integer oldPrio = prios.get(perm.getPermission());
                        if (oldPrio != null && oldPrio < groups.get(perm.getEntity())) {
                            perms.put(perm.getPermission(), perm.getValue());
                            prios.put(perm.getPermission(), groups.get(perm.getEntity()));
                        }
                    }
                }
            } else {
                if (uuidString.equals(perm.getEntity())) {
                    perms.put(perm.getPermission(), perm.getValue());
                    prios.remove(perm.getPermission());
                }
            }
        }
        return perms;
    }

    Map<String, Boolean> findGroupPerms(String groupName) {
        getCache();
        final Map<String, Boolean> perms = new HashMap<>();
        SQLGroup group = cache.findGroup(groupName.toLowerCase());
        if (group == null) return perms;
        Map<String, Integer> groups = new HashMap<>();
        while (group != null && !groups.containsKey(group.getKey())) {
            groups.put(group.getKey(), group.getPriority());
            if (group.getParent() == null) {
                group = null;
            } else {
                group = cache.findGroup(group.getParent());
            }
        }
        final Map<String, Integer> prios = new HashMap<>();
        for (SQLPermission perm: cache.permissions) {
            if (perm.getIsGroup()) {
                if (groups.containsKey(perm.getEntity())) {
                    Boolean oldPerm = perms.get(perm.getPermission());
                    if (oldPerm == null) {
                        perms.put(perm.getPermission(), perm.getValue());
                        prios.put(perm.getPermission(), groups.get(perm.getEntity()));
                    } else {
                        Integer oldPrio = prios.get(perm.getPermission());
                        if (oldPrio < groups.get(perm.getEntity())) {
                            perms.put(perm.getPermission(), perm.getValue());
                            prios.put(perm.getPermission(), groups.get(perm.getEntity()));
                        }
                    }
                }
            }
        }
        return perms;
    }

    void resetPlayerPerms(Player player) {
        List<PermissionAttachment> list = new ArrayList<>();
        for (PermissionAttachmentInfo info: player.getEffectivePermissions()) {
            PermissionAttachment attach = info.getAttachment();
            if (attach != null && attach.getPlugin().equals(this)) {
                list.add(info.getAttachment());
            }
        }
        for (PermissionAttachment attach: list) {
            player.removeAttachment(attach);
        }
    }

    void setupPlayerPerms(Player player) {
        Map<String, Boolean> perms = findPlayerPerms(player.getUniqueId());
        PermissionAttachment attach = player.addAttachment(this);
        for (Map.Entry<String, Boolean> entry: perms.entrySet()) {
            attach.setPermission(entry.getKey(), entry.getValue());
        }
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        vaultPerm = new VaultPerm(this);
        vaultPerm.register();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        setupPlayerPerms(event.getPlayer());
    }

    public boolean playerHasPerm(UUID uuid, String perm) {
        Map<String, Boolean> perms = findPlayerPerms(uuid);
        Boolean result = perms.get(perm);
        if (result != null) return result;
        return false;
    }

    public boolean groupHasPerm(String name, String perm) {
        Map<String, Boolean> perms = findGroupPerms(name);
        Boolean result = perms.get(perm);
        if (result != null) return result;
        return false;
    }

    public boolean playerInGroup(UUID uuid, String groupName) {
        groupName = groupName.toLowerCase();
        getCache();
        for (SQLMember mem: cache.members) {
            if (!uuid.equals(mem.getMember())) continue;
            if (groupName.equals(mem.getGroup())) return true;
            SQLGroup group = cache.findGroup(mem.getGroup());
            while (group != null && group.getParent() != null) {
                if (groupName.equals(group.getParent())) return true;
                group = cache.findGroup(group.getParent());
            }
        }
        return false;
    }

    public List<String> findPlayerGroups(UUID uuid) {
        getCache();
        List<String> result = new ArrayList<>();
        for (SQLMember mem: cache.members) {
            if (!uuid.equals(mem.getMember())) continue;
            SQLGroup group = cache.findGroup(mem.getGroup());
            if (group != null) result.add(group.getDisplayName());
        }
        return result;
    }

    public List<UUID> findGroupMembers(String groupName) {
        getCache();
        groupName = groupName.toLowerCase();
        List<UUID> result = new ArrayList<>();
        for (SQLMember mem: cache.members) {
            if (!groupName.equals(mem.getGroup())) continue;
            result.add(mem.getMember());
        }
        return result;
    }
    public boolean setPlayerPerm(UUID uuid, String perm, Boolean value) {
        perm = perm.toLowerCase();
        SQLPermission row = db.find(SQLPermission.class).eq("entity", uuid.toString()).eq("isGroup", false).eq("permission", perm).findUnique();
        if (row == null && value == null) {
            return false;
        } else if (value == null) {
            db.delete(row);
        } else if (row == null) {
            row = new SQLPermission(uuid.toString(), false, perm, value);
            db.save(row);
        } else {
            if (row.getValue() == value) return false;
            row.setValue(value);
            db.save(row);
        }
        refreshPermissions();
        return true;
    }

    public boolean setGroupPerm(String group, String perm, Boolean value) {
        perm = perm.toLowerCase();
        group = group.toLowerCase();
        SQLPermission row = db.find(SQLPermission.class).eq("entity", group).eq("isGroup", true).eq("permission", perm).findUnique();
        if (row == null && value == null) {
            return false;
        } else if (value == null) {
            db.delete(row);
        } else if (row == null) {
            row = new SQLPermission(group, true, perm, value);
            db.save(row);
        } else {
            if (row.getValue() == value) return false;
            row.setValue(value);
            db.save(row);
        }
        refreshPermissions();
        return true;
    }

    public boolean setMembership(UUID uuid, String group, boolean value) {
        group = group.toLowerCase();
        SQLMember row = db.find(SQLMember.class).eq("member", uuid).eq("group", group).findUnique();
        if (value && row != null) {
            return false;
        } else if (!value && row == null) {
            return false;
        } else if (row == null) {
            row = new SQLMember(uuid, group);
            db.save(row);
        } else {
            db.delete(row);
        }
        refreshPermissions();
        return true;
    }

    public List<String> getGroups() {
        List<String> result = new ArrayList<>();
        for (SQLGroup group: getCache().groups) {
            result.add(group.getDisplayName());
        }
        return result;
    }
}
