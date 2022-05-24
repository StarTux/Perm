package com.winthier.perm;

import com.winthier.perm.event.PlayerPermissionUpdateEvent;
import com.winthier.perm.rank.Rank;
import com.winthier.perm.sql.SQLGroup;
import com.winthier.perm.sql.SQLMember;
import com.winthier.perm.sql.SQLPermission;
import com.winthier.perm.sql.SQLVersion;
import com.winthier.sql.SQLDatabase;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

@Getter
public final class PermPlugin extends JavaPlugin {
    protected static final String DEFAULT_GROUP = "guest";
    protected static final long REFRESH_INTERVAL = 30L * 20L;
    protected SQLDatabase db;
    protected Cache cache;
    protected boolean vaultEnabled = false;
    protected BukkitRunnable updateTask;
    protected PermCommand permCommand = new PermCommand(this);
    protected PromoteCommand promoteCommand = new PromoteCommand(this);
    protected EventListener listener = new EventListener(this);
    protected ConnectHandler connectHandler;
    @Getter protected static PermPlugin instance;
    protected File localPermissionsFile;
    protected List<SQLPermission> localPermissionsCache;
    protected final CorePerm corePerm = new CorePerm(this);

    @Override
    public void onLoad() {
        instance = this;
        tryToLoadVault();
    }

    @Override
    public void onEnable() {
        corePerm.register();
        db = new SQLDatabase(this);
        db.registerTables(SQLGroup.class,
                          SQLMember.class,
                          SQLPermission.class,
                          SQLVersion.class);
        if (!db.createAllTables()) {
            throw new IllegalStateException("Table creation failed!");
        }
        for (Rank rank : Rank.all()) {
            String key = rank.getKey();
            String groupPerm = "group." + rank.getKey();
            String rankPerm = "rank." + rank.getKey();
            Permission groupPermission = new Permission(groupPerm,
                                                        "Member of " + rank.getDisplayName(),
                                                        PermissionDefault.FALSE);
            Permission rankPermission = new Permission(rankPerm,
                                                       "Assigned member of " + rank.getDisplayName(),
                                                       PermissionDefault.FALSE,
                                                       Map.of(groupPerm, true));
            Bukkit.getPluginManager().removePermission(groupPerm);
            Bukkit.getPluginManager().removePermission(rankPerm);
            Bukkit.getPluginManager().addPermission(groupPermission);
            Bukkit.getPluginManager().addPermission(rankPermission);
        }
        Bukkit.getPluginManager().registerEvents(listener, this);
        getCommand("perm").setExecutor(permCommand);
        getCommand("promote").setExecutor(promoteCommand);
        localPermissionsFile = new File(getDataFolder(), "local.yml");
        refreshPermissionsSync();
        tryToLoadVault();
        if (Bukkit.getPluginManager().isPluginEnabled("Connect")) {
            connectHandler = new ConnectHandler(this).enable();
        }
        Bukkit.getScheduler().runTaskTimer(this, this::testVersion, 0L, REFRESH_INTERVAL);
    }

    @Override
    public void onDisable() {
        for (Player player : getServer().getOnlinePlayers()) {
            resetPlayerPerms(player);
        }
        corePerm.unregister();
    }

    protected void tryToLoadVault() {
        if (vaultEnabled) return;
        try {
            Class.forName("net.milkbowl.vault.permission.Permission");
            VaultPerm vaultPerm = new VaultPerm(this);
            vaultPerm.register();
            vaultEnabled = true;
        } catch (ClassNotFoundException ncfe) { }
    }

    /**
     * Update the version in the database, and broadcast it. Thich
     * will trigger other servers to refresh permissions as soon as
     * possible. Writing to the database is done asynchronously.
     */
    protected void updateVersion() {
        SQLVersion version;
        if (cache != null) {
            version = cache.version;
        } else {
            version = db.find(SQLVersion.class).eq("name", "Perm").findUnique();
            if (version == null) version = new SQLVersion("Perm");
        }
        version.setNow();
        db.saveAsync(version, null);
        if (connectHandler != null) {
            connectHandler.broadcastRefresh();
        }
    }

    protected List<SQLPermission> loadLocalPermissions() {
        if (!localPermissionsFile.exists()) {
            localPermissionsCache = List.of();
            return List.of();
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(localPermissionsFile);
        List<SQLPermission> list = new ArrayList<>();
        for (Map<?, ?> map : yaml.getMapList("permissions")) {
            ConfigurationSection section = yaml.createSection("tmp", map);
            String entity = section.getString("entity");
            boolean isGroup = section.getBoolean("isGroup");
            String permission = section.getString("permission");
            boolean value = section.getBoolean("value");
            if (entity == null || permission == null) {
                getLogger().warning(localPermissionsFile + ": Invalid permission: "
                                    + map);
                continue;
            }
            try {
                UUID.fromString(entity);
            } catch (IllegalArgumentException iae) {
                isGroup = true;
            }
            list.add(new SQLPermission(entity, isGroup, permission, value));
        }
        localPermissionsCache = list;
        return list;
    }

    protected void refreshPermissionsSync() {
        Cache newCache = new Cache();
        newCache.load(this.db, loadLocalPermissions());
        loadNewCache(newCache);
    }

    protected void refreshPermissionsAsync() {
        db.scheduleAsyncTask(() -> {
                Cache newCache = new Cache();
                newCache.load(this.db, loadLocalPermissions());
                Bukkit.getScheduler().runTask(this, () -> {
                        loadNewCache(newCache);
                    });
            });
    }

    /**
     * Update the cache and setup all players.
     * Must be called in the main thread!
     */
    private void loadNewCache(Cache newCache) {
        this.cache = newCache;
        for (Player player : getServer().getOnlinePlayers()) {
            setupPlayerPerms(player);
        }
    }

    /**
     * Fetch the version from the database and trigger a refresh if it
     * doesn't match.
     */
    private void testVersion() {
        db.find(SQLVersion.class).eq("name", "Perm").findUniqueAsync(version -> {
                if (version == null) return;
                if (version.getVersion().equals(cache.version.getVersion())) return;
                getLogger().info("Refreshing permissions from database");
                refreshPermissionsAsync();
            });
    }

    protected void resetPlayerPerms(final Player player) {
        for (PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            PermissionAttachment attach = info.getAttachment();
            if (attach != null && attach.getPlugin().equals(this)) {
                attach.remove();
            }
            final UUID uuid = player.getUniqueId();
            final String motherPerm = "perm-" + player.getUniqueId();
            getServer().getPluginManager().removePermission(motherPerm);
        }
        cache.deepPlayerPerms.remove(player.getUniqueId());
    }

    protected void setupPlayerPerms(final Player player) {
        Map<String, Boolean> perms = cache.findPlayerPerms(player.getUniqueId());
        // This is a little trick I learned from zPermissions.  Do not
        // add an attachment as adding permissions to it is slow.
        // Instead, create a parent permission for the player
        // containing all their effective permissions as children.
        String motherPerm = "perm-" + player.getUniqueId();
        Permission permission = getServer().getPluginManager().getPermission(motherPerm);
        final boolean updateRequired;
        final Map<String, Boolean> oldPerms = new HashMap<>();
        final boolean isInitialSetup;
        if (permission == null) {
            updateRequired = true;
            isInitialSetup = true;
            permission = new Permission(motherPerm,
                                        "Mother permission of " + player.getName(),
                                        PermissionDefault.FALSE, perms);
            getServer().getPluginManager().addPermission(permission);
        } else {
            isInitialSetup = false;
            oldPerms.putAll(permission.getChildren());
            updateRequired = !perms.equals(oldPerms);
            if (updateRequired) {
                permission.getChildren().clear();
                permission.getChildren().putAll(perms);
            }
        }
        if (!player.isPermissionSet(motherPerm) || !player.hasPermission(motherPerm)) {
            // This should only happen if isInitialSetup = true and
            // updateRequired = true, but why not check if the
            // attachment is set anyway.
            PermissionAttachment attach = player.addAttachment(this, motherPerm, true);
        } else if (updateRequired) {
            player.recalculatePermissions();
            if (!isInitialSetup) {
                Bukkit.getScheduler().runTask(this, () -> {
                        player.updateCommands();
                        if (!player.isOnline()) return;
                        new PlayerPermissionUpdateEvent(player, oldPerms, perms).call();
                    });
            }
        }
    }

    public boolean playerHasPerm(final UUID uuid, final String perm) {
        Map<String, Boolean> perms = cache.findPlayerPerms(uuid);
        Boolean result = perms.get(perm);
        if (result != null) return result;
        return false;
    }

    public boolean groupHasPerm(final String name, final String perm) {
        Map<String, Boolean> perms = cache.findGroupPerms(name);
        Boolean result = perms.get(perm);
        if (result != null) return result;
        return false;
    }

    public boolean playerInGroup(final UUID uuid, final String groupName) {
        return cache.findDeepPlayerGroups(uuid).contains(groupName);
    }

    public List<String> findPlayerGroups(final UUID uuid) {
        return new ArrayList<>(cache.findAssignedGroups(uuid));
    }

    public List<UUID> findGroupMembers(final String groupName) {
        List<UUID> result = new ArrayList<>();
        for (SQLMember mem : cache.members) {
            if (!groupName.equals(mem.getGroup())) continue;
            result.add(mem.getMember());
        }
        return result;
    }

    public boolean setPlayerPerm(final UUID uuid, final String perm, final Boolean value) {
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
        refreshPermissionsAsync();
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
        refreshPermissionsAsync();
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
        refreshPermissionsAsync();
        return true;
    }

    public List<String> getGroups() {
        List<String> result = new ArrayList<>();
        for (SQLGroup group : cache.groups) {
            result.add(group.getKey());
        }
        return result;
    }
}
