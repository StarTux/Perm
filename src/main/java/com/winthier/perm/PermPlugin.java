package com.winthier.perm;

import com.winthier.sql.SQLDatabase;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

@Getter
public final class PermPlugin extends JavaPlugin {
    SQLDatabase db;
    Cache cache;
    String defaultGroup = "Guest";
    int refreshInterval = 30;
    boolean migrationEnabled = false;
    boolean refreshScheduled = false;
    boolean vaultEnabled = false;
    BukkitRunnable updateTask;
    PermCommand command = new PermCommand(this);
    EventListener listener = new EventListener(this);
    ConnectHandler connectHandler;

    @Override
    public void onLoad() {
        if (!vaultEnabled) {
            try {
                Class.forName("net.milkbowl.vault.permission.Permission");
                VaultPerm vaultPerm = new VaultPerm(this);
                vaultPerm.register();
            } catch (ClassNotFoundException ncfe) { }
        }
    }

    @Override
    public void onEnable() {
        reloadConfig();
        saveDefaultConfig();
        readConfiguration();
        db = new SQLDatabase(this);
        db.registerTables(SQLGroup.class,
                          SQLMember.class,
                          SQLPermission.class,
                          SQLVersion.class);
        db.createAllTables();
        getServer().getPluginManager().registerEvents(listener, this);
        getCommand("perm").setExecutor(command);
        refreshPermissions();
        if (!vaultEnabled && getServer().getPluginManager().isPluginEnabled("Vault")) {
            VaultPerm vaultPerm = new VaultPerm(this);
            vaultPerm.register();
        }
        if (Bukkit.getPluginManager().isPluginEnabled("Connect")) {
            connectHandler = new ConnectHandler(this).enable();
        }
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
        refreshInterval = getConfig().getInt("RefreshInterval");
        migrationEnabled = getConfig().getBoolean("MigrationEnabled");
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
        if (refreshInterval > 0) {
            updateTask = new BukkitRunnable() {
                    @Override public void run() {
                        testVersion();
                    }
                };
            updateTask.runTaskTimer(this, 0, refreshInterval * 20);
        }
    }

    void updateVersion() {
        SQLVersion version;
        if (cache != null) {
            version = cache.version;
        } else {
            version = db.find(SQLVersion.class).eq("name", "Perm").findUnique();
            if (version == null) version = new SQLVersion("Perm");
        }
        version.setVersion(new Date());
        db.save(version);
        if (connectHandler != null) {
            connectHandler.broadcastRefresh();
        }
    }

    void refreshPermissions() {
        Cache newCache = new Cache();
        newCache.groups = db.find(SQLGroup.class).findList();
        newCache.members = db.find(SQLMember.class).findList();
        newCache.permissions = db.find(SQLPermission.class).findList();
        newCache.version = db.find(SQLVersion.class).eq("name", "Perm")
            .findUnique();
        if (newCache.version == null) newCache.version = new SQLVersion("Perm");
        newCache.deepGroupParents.put(defaultGroup,
                                      new ArrayList<>());
        for (SQLGroup row : newCache.groups) {
            List<String> deepGroupParents = new ArrayList<>();
            newCache.deepGroupParents.put(row.getKey(), deepGroupParents);
            SQLGroup currentRow = row;
            do {
                deepGroupParents.add(currentRow.getKey());
                if (currentRow.getParent() == null) {
                    currentRow = null;
                } else {
                    currentRow = newCache.findGroup(currentRow.getParent());
                }
            } while (currentRow != null);
            newCache.groupMembers.put(row.getKey(), new ArrayList<>());
            newCache.groupPrios.put(row.getKey(), row.getPriority());
            newCache.flatGroupPerms.put(row.getKey(), new HashMap<>());
        }
        for (SQLMember row : newCache.members) {
            List<UUID> list = newCache.groupMembers.get(row.getGroup());
            if (list == null) continue;
            list.add(row.getMember());
        }
        for (SQLPermission row : newCache.permissions) {
            if (row.getIsGroup()) {
                SQLGroup group = newCache.findGroup(row.getEntity());
                if (group == null) continue;
                newCache.flatGroupPerms.get(group.getKey())
                    .put(row.getPermission(), row.getValue());
            } else {
                UUID uuid = row.getUuid();
                HashMap<String, Boolean> perms = newCache.flatPlayerPerms
                    .get(uuid);
                if (perms == null) {
                    perms = new HashMap<>();
                    newCache.flatPlayerPerms.put(uuid, perms);
                }
                perms.put(row.getPermission(), row.getValue());
            }
        }
        this.cache = newCache;
        for (Player player : getServer().getOnlinePlayers()) {
            setupPlayerPerms(player);
        }
    }

    void testVersion() {
        SQLVersion version = db.find(SQLVersion.class)
            .eq("name", "Perm")
            .findUnique();
        if (version != null
            && !version.getVersion().equals(cache.version.getVersion())) {
            getLogger().info("Refreshing permissions from database");
            refreshPermissions();
        }
    }

    Map<String, Boolean> findPlayerPerms(final UUID uuid) {
        Map<String, Boolean> perms = cache.deepPlayerPerms.get(uuid);
        if (perms != null) return perms;
        Set<String> assignedGroups = new HashSet<>();
        for (SQLGroup group: cache.groups) {
            if (cache.groupMembers.get(group.getKey()).contains(uuid)) {
                assignedGroups.add(group.getKey());
            }
        }
        if (assignedGroups.isEmpty()) assignedGroups.add(defaultGroup);
        HashMap<String, Boolean> deepGroupPerms =
            cache.deepGroupPerms.get(assignedGroups);
        if (deepGroupPerms == null) {
            deepGroupPerms = new HashMap<>();
            List<String> groupList = new ArrayList<>();
            for (String key: assignedGroups) {
                deepGroupPerms.put("rank." + key, true);
                groupList.addAll(cache.deepGroupParents.get(key));
            }
            for (String key : groupList) {
                deepGroupPerms.put("group." + key, true);
            }
            Collections.sort(groupList, (a, b) ->
                             Integer.compare(cache.groupPrios.get(a),
                                             cache.groupPrios.get(b)));
            for (String key: groupList) {
                deepGroupPerms.putAll(cache.flatGroupPerms.get(key));
            }
            cache.deepGroupPerms.put(assignedGroups, deepGroupPerms);
        }
        perms = new HashMap<>(deepGroupPerms);
        HashMap<String, Boolean> flatPlayerPerms =
            cache.flatPlayerPerms.get(uuid);
        if (flatPlayerPerms != null) perms.putAll(flatPlayerPerms);
        cache.deepPlayerPerms.put(uuid, perms);
        return perms;
    }

    Map<String, Boolean> findGroupPerms(final String groupName) {
        final Map<String, Boolean> perms = new HashMap<>();
        SQLGroup group = cache.findGroup(groupName);
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
                        perms.put(perm.getPermission(),
                                  perm.getValue());
                        prios.put(perm.getPermission(),
                                  groups.get(perm.getEntity()));
                    } else {
                        Integer oldPrio = prios.get(perm.getPermission());
                        if (oldPrio < groups.get(perm.getEntity())) {
                            perms.put(perm.getPermission(),
                                      perm.getValue());
                            prios.put(perm.getPermission(),
                                      groups.get(perm.getEntity()));
                        }
                    }
                }
            }
        }
        return perms;
    }

    void resetPlayerPerms(final Player player) {
        for (PermissionAttachmentInfo info: player.getEffectivePermissions()) {
            PermissionAttachment attach = info.getAttachment();
            if (attach != null && attach.getPlugin().equals(this)) {
                attach.remove();
            }
            final UUID uuid = player.getUniqueId();
            final String motherPerm = "Perm-" + player.getUniqueId();
            getServer().getPluginManager().removePermission(motherPerm);
        }
        cache.deepPlayerPerms.remove(player.getUniqueId());
    }

    void setupPlayerPerms(final Player player) {
        Map<String, Boolean> perms = findPlayerPerms(player.getUniqueId());
        // This is a little trick I learned from zPermissions.  Do not
        // add an attachment as adding permissions to it is slow.
        // Instead, create a parent permission for the player
        // containing all their effective permissions as children.
        String motherPerm = "Perm-" + player.getUniqueId();
        Permission permission = getServer().getPluginManager()
            .getPermission(motherPerm);
        if (permission == null) {
            permission = new Permission(motherPerm,
                                        PermissionDefault.FALSE,
                                        perms);
            getServer().getPluginManager().addPermission(permission);
        } else {
            permission.getChildren().clear();
            permission.getChildren().putAll(perms);
        }
        if (!player.isPermissionSet(motherPerm)
            || !player.hasPermission(motherPerm)) {
            PermissionAttachment attach =
                player.addAttachment(this, motherPerm, true);
        } else {
            permission.recalculatePermissibles();
        }
    }

    public boolean playerHasPerm(final UUID uuid,
                                 final String perm) {
        Map<String, Boolean> perms = findPlayerPerms(uuid);
        Boolean result = perms.get(perm);
        if (result != null) return result;
        return false;
    }

    public boolean groupHasPerm(final String name,
                                final String perm) {
        Map<String, Boolean> perms = findGroupPerms(name);
        Boolean result = perms.get(perm);
        if (result != null) return result;
        return false;
    }

    public boolean playerInGroup(final UUID uuid,
                                 final String groupName) {
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

    public List<String> findPlayerGroups(final UUID uuid) {
        List<String> result = new ArrayList<>();
        for (SQLMember mem: cache.members) {
            if (!uuid.equals(mem.getMember())) continue;
            SQLGroup group = cache.findGroup(mem.getGroup());
            if (group != null) result.add(group.getKey());
        }
        return result;
    }

    public List<UUID> findGroupMembers(final String groupName) {
        List<UUID> result = new ArrayList<>();
        for (SQLMember mem: cache.members) {
            if (!groupName.equals(mem.getGroup())) continue;
            result.add(mem.getMember());
        }
        return result;
    }

    public boolean setPlayerPerm(final UUID uuid,
                                 final String perm,
                                 final Boolean value) {
        SQLPermission row = db.find(SQLPermission.class)
            .eq("entity", uuid.toString())
            .eq("isGroup", false)
            .eq("permission", perm)
            .findUnique();
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
        updateVersion();
        refreshPermissions();
        return true;
    }

    public boolean setGroupPerm(final String group,
                                final String perm,
                                final Boolean value) {
        SQLPermission row = db.find(SQLPermission.class)
            .eq("entity", group)
            .eq("isGroup", true)
            .eq("permission", perm)
            .findUnique();
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
        updateVersion();
        refreshPermissions();
        return true;
    }

    public boolean setMembership(final UUID uuid,
                                 final String group,
                                 final boolean value) {
        SQLMember row = db.find(SQLMember.class)
            .eq("member", uuid)
            .eq("group", group)
            .findUnique();
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
        updateVersion();
        refreshPermissions();
        return true;
    }

    public List<String> getGroups() {
        List<String> result = new ArrayList<>();
        for (SQLGroup group: cache.groups) {
            result.add(group.getKey());
        }
        return result;
    }
}
