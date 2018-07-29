package com.winthier.perm;

import com.winthier.playercache.PlayerCache;
import java.util.List;
import java.util.UUID;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;

final class VaultPerm extends Permission {
    private final PermPlugin permPlugin;

    VaultPerm(PermPlugin plugin) {
        this.plugin = plugin;
        this.permPlugin = plugin;
    }

    void register() {
        permPlugin.getServer().getServicesManager().register(Permission.class, this, permPlugin, ServicePriority.Highest);
        plugin.getLogger().info("Vault permissions provider registered");
    }

    /**
     * Gets name of permission method
     * @return Name of Permission Method
     */
    @Override
    public String getName() {
        return "Perm";
    }

    /**
     * Checks if permission method is enabled.
     * @return Success or Failure
     */
    @Override
    public boolean isEnabled() {
        return permPlugin.isEnabled();
    }

    /**
     * Returns if the permission system is or attempts to be compatible with super-perms.
     * @return True if this permission implementation works with super-perms
     */
    @Override
    public boolean hasSuperPermsCompat() {
        return true;
    }

    /**
     * @deprecated As of VaultAPI 1.4 use {@link #playerHas(String, OfflinePlayer, String)} instead.
     */
    @Override
    public boolean has(String world, String player, String permission) {
        UUID uuid = PlayerCache.uuidForName(player);
        if (uuid == null) return false;
        return permPlugin.playerHasPerm(uuid, permission);
    }

    /**
     * @deprecated As of VaultAPI 1.4 use {@link #playerHas(String, OfflinePlayer, String)} instead.
     */
    @Override
    public boolean has(World world, String player, String permission) {
        UUID uuid = PlayerCache.uuidForName(player);
        if (uuid == null) return false;
        return permPlugin.playerHasPerm(uuid, permission);
    }

    /**
     * Checks if a CommandSender has a permission node.
     * This will return the result of bukkits, generic .hasPermission() method and is identical in all cases.
     * This method will explicitly fail if the registered permission system does not register permissions in bukkit.
     *
     * For easy checking of a commandsender
     * @param sender to check permissions on
     * @param permission to check for
     * @return true if the sender has the permission
     */
    @Override
    public boolean has(CommandSender sender, String permission) {
        if (!(sender instanceof OfflinePlayer)) return false;
        return permPlugin.playerHasPerm(((OfflinePlayer)sender).getUniqueId(), permission);
    }

    /**
     * Checks if player has a permission node. (Short for playerHas(...)
     * @param player Player Object
     * @param permission Permission node
     * @return Success or Failure
     */
    @Override
    public boolean has(Player player, String permission) {
        return permPlugin.playerHasPerm(player.getUniqueId(), permission);
    }

    /**
     * @deprecated As of VaultAPI 1.4 use {@link #playerHas(String, OfflinePlayer, String)} instead.
     */
    @Override
    public boolean playerHas(String world, String player, String permission) {
        UUID uuid = PlayerCache.uuidForName(player);
        if (uuid == null) return false;
        return permPlugin.playerHasPerm(uuid, permission);
    }

    /**
     * @deprecated As of VaultAPI 1.4 use {@link #playerHas(String, OfflinePlayer, String)} instead.
     */
    @Override
    public boolean playerHas(World world, String player, String permission) {
        UUID uuid = PlayerCache.uuidForName(player);
        if (uuid == null) return false;
        return permPlugin.playerHasPerm(uuid, permission);
    }

    /**
     * Checks if player has a permission node.
     * Supports NULL value for World if the permission system registered supports global permissions.
     * But May return odd values if the servers registered permission system does not have a global permission store.
     *
     * @param world String world name
     * @param player to check
     * @param permission Permission node
     * @return Success or Failure
     */
    @Override
    public boolean playerHas(String world, OfflinePlayer player, String permission) {
        return permPlugin.playerHasPerm(player.getUniqueId(), permission);
    }

    /**
     * Checks if player has a permission node.
     * Defaults to world-specific permission check if the permission system supports it.
     * See {@link #playerHas(String, OfflinePlayer, String)} for explicit global or world checks.
     *
     * @param player Player Object
     * @param permission Permission node
     * @return Success or Failure
     */
    @Override
    public boolean playerHas(Player player, String permission) {
        return permPlugin.playerHasPerm(player.getUniqueId(), permission);
    }

    /**
     * @deprecated As of VaultAPI 1.4 use {@link #playerAdd(String, OfflinePlayer, String)} instead.
     * Add permission to a player.
     * Supports NULL value for World if the permission system registered supports global permissions.
     * But May return odd values if the servers registered permission system does not have a global permission store.
     *
     * @param world World name
     * @param player Player name
     * @param permission Permission node
     * @return Success or Failure
     */
    @Override
    public boolean playerAdd(String world, String player, String permission) {
        UUID uuid = PlayerCache.uuidForName(player);
        if (uuid == null) return false;
        return permPlugin.setPlayerPerm(uuid, permission, true);
    }

    /**
     * @deprecated As of VaultAPI 1.4 use {@link #playerAdd(String, OfflinePlayer, String)} instead.
     */
    @Override
    public boolean playerAdd(World world, String player, String permission) {
        UUID uuid = PlayerCache.uuidForName(player);
        if (uuid == null) return false;
        return permPlugin.setPlayerPerm(uuid, permission, true);
    }

    /**
     * Add permission to a player.
     * Supports NULL value for World if the permission system registered supports global permissions.
     * But May return odd values if the servers registered permission system does not have a global permission store.
     *
     * @param world String world name
     * @param player to add to
     * @param permission Permission node
     * @return Success or Failure
     */
    @Override
    public boolean playerAdd(String world, OfflinePlayer player, String permission) {
        return permPlugin.setPlayerPerm(player.getUniqueId(), permission, true);
    }

    /**
     * Add permission to a player ONLY for the world the player is currently on.
     * This is a world-specific operation, if you want to add global permission you must explicitly use NULL for the world.
     * See {@link #playerAdd(String, OfflinePlayer, String)} for global permission use.
     *
     * @param player Player Object
     * @param permission Permission node
     * @return Success or Failure
     */
    @Override
    public boolean playerAdd(Player player, String permission) {
        return playerAdd(player.getWorld().getName(), player, permission);
    }

    /**
     * @deprecated As of VaultAPI 1.4 use {@link #playerAddTransient(OfflinePlayer, String)} instead.
     */
    @Override
    public boolean playerAddTransient(String player, String permission) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("playerAddTransient() not supported!");
    }

    /**
     * Add transient permission to a player.
     * This implementation can be used by any subclass which implements a "pure" superperms plugin, i.e.
     * one that only needs the built-in Bukkit API to add transient permissions to a player.
     *
     * @param player to add to
     * @param permission Permission node
     * @return Success or Failure
     */
    @Override
    public boolean playerAddTransient(OfflinePlayer player, String permission) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("playerAddTransient() not supported!");
    }

    /**
     * Add transient permission to a player.
     * This operation adds a permission onto the player object in bukkit via Bukkit's permission interface.
     *
     * @param player Player Object
     * @param permission Permission node
     * @return Success or Failure
     */
    @Override
    public boolean playerAddTransient(Player player, String permission) {
        throw new UnsupportedOperationException("playerAddTransient() not supported!");
    }

    /**
     * Adds a world specific transient permission to the player, may only work with some permission managers.
     * Defaults to GLOBAL permissions for any permission system that does not support world-specific transient permissions!
     *
     * @param worldName to check on
     * @param player to add to
     * @param permission to test
     * @return Success or Failure
     */
    @Override
    public boolean playerAddTransient(String worldName, OfflinePlayer player, String permission) {
        throw new UnsupportedOperationException("playerAddTransient() not supported!");
    }

    /**
     * Adds a world specific transient permission to the player, may only work with some permission managers.
     * Defaults to GLOBAL permissions for any permission system that does not support world-specific transient permissions!
     *
     * @param worldName to check on
     * @param player to check
     * @param permission to check for
     * @return Success or Failure
     */
    @Override
    public boolean playerAddTransient(String worldName, Player player, String permission) {
        throw new UnsupportedOperationException("playerAddTransient() not supported!");
    }

    /**
     * @deprecated As of VaultAPI 1.4 use {@link #playerAddTransient(String, OfflinePlayer, String)} instead.
     */
    @Override
    public boolean playerAddTransient(String worldName, String player, String permission) {
        throw new UnsupportedOperationException("playerAddTransient() not supported!");
    }

    /**
     * @deprecated As of VaultAPI 1.4 use {@link #playerRemoveTransient(String, OfflinePlayer, String)} instead.
     */
    @Override
    public boolean playerRemoveTransient(String worldName, String player, String permission) {
        throw new UnsupportedOperationException("playerRemoveTransient() not supported!");
    }

    /**
     * Removes a world specific transient permission from the player, may only work with some permission managers.
     * Defaults to GLOBAL permissions for any permission system that does not support world-specific transient permissions!
     *
     * @param worldName to remove for
     * @param player to remove for
     * @param permission to remove
     * @return Success or Failure
     */
    @Override
    public boolean playerRemoveTransient(String worldName, OfflinePlayer player, String permission) {
        throw new UnsupportedOperationException("playerRemoveTransient() not supported!");
    }

    /**
     * Removes a world specific transient permission from the player, may only work with some permission managers.
     * Defaults to GLOBAL permissions for any permission system that does not support world-specific transient permissions!
     *
     * @param worldName to check on
     * @param player to check
     * @param permission to check for
     * @return Success or Failure
     */
    @Override
    public boolean playerRemoveTransient(String worldName, Player player, String permission) {
        throw new UnsupportedOperationException("playerRemoveTransient() not supported!");
    }

    /**
     * @deprecated As of VaultAPI 1.4 use {@link #playerRemove(String, OfflinePlayer, String)} instead.
     */
    @Override
    public boolean playerRemove(String world, String player, String permission) {
        UUID uuid = PlayerCache.uuidForName(player);
        if (uuid == null) return false;
        return permPlugin.setPlayerPerm(uuid, permission, null);
    }

    /**
     * Remove permission from a player.
     * Supports NULL value for World if the permission system registered supports global permissions.
     * But May return odd values if the servers registered permission system does not have a global permission store.
     *
     * @param world World name
     * @param player OfflinePlayer
     * @param permission Permission node
     * @return Success or Failure
     */
    @Override
    public boolean playerRemove(String world, OfflinePlayer player, String permission) {
        return permPlugin.setPlayerPerm(player.getUniqueId(), permission, null);
    }

    /**
     * Remove permission from a player.
     * Supports NULL value for World if the permission system registered supports global permissions.
     * But May return odd values if the servers registered permission system does not have a global permission store.
     *
     * @param world World name
     * @param player Player name
     * @param permission Permission node
     * @return Success or Failure
     */
    @Override
    public boolean playerRemove(World world, String player, String permission) {
        UUID uuid = PlayerCache.uuidForName(player);
        if (uuid == null) return false;
        return permPlugin.setPlayerPerm(uuid, permission, null);
    }

    /**
     * Remove permission from a player.
     * Will attempt to remove permission from the player on the player's current world.  This is NOT a global operation.
     *
     * @param player Player Object
     * @param permission Permission node
     * @return Success or Failure
     */
    @Override
    public boolean playerRemove(Player player, String permission) {
        return playerRemove(player.getWorld().getName(), player, permission);
    }

    /**
     * @deprecated As of VaultAPI 1.4 use {@link #playerRemoveTransient(OfflinePlayer, String)} instead.
     */
    @Override
    public boolean playerRemoveTransient(String player, String permission) {
        throw new UnsupportedOperationException("playerRemoveTransient() not supported!");
    }

    /**
     * Remove transient permission from a player.
     * This implementation can be used by any subclass which implements a "pure" superperms plugin, i.e.
     * one that only needs the built-in Bukkit API to remove transient permissions from a player.  Any subclass
     * implementing a plugin which provides its own API for this needs to override this method.
     *
     * @param player OfflinePlayer
     * @param permission Permission node
     * @return Success or Failure
     */
    @Override
    public boolean playerRemoveTransient(OfflinePlayer player, String permission) {
        throw new UnsupportedOperationException("playerRemoveTransient() not supported!");
    }

    /**
     * Remove transient permission from a player.
     *
     * @param player Player Object
     * @param permission Permission node
     * @return Success or Failure
     */
    @Override
    public boolean playerRemoveTransient(Player player, String permission) {
        throw new UnsupportedOperationException("playerRemoveTransient() not supported!");
    }

    /**
     * Checks if group has a permission node.
     * Supports NULL value for World if the permission system registered supports global permissions.
     * But May return odd values if the servers registered permission system does not have a global permission store.
     *
     * @param world World name
     * @param group Group name
     * @param permission Permission node
     * @return Success or Failure
     */
    @Override
    public boolean groupHas(String world, String group, String permission) {
        return permPlugin.groupHasPerm(group, permission);
    }

    /**
     * Checks if group has a permission node.
     * Supports NULL value for World if the permission system registered supports global permissions.
     * But May return odd values if the servers registered permission system does not have a global permission store.
     *
     * @param world World Object
     * @param group Group name
     * @param permission Permission node
     * @return Success or Failure
     */
    @Override
    public boolean groupHas(World world, String group, String permission) {
        return permPlugin.groupHasPerm(group, permission);
    }

    /**
     * Add permission to a group.
     * Supports NULL value for World if the permission system registered supports global permissions.
     * But May return odd values if the servers registered permission system does not have a global permission store.
     *
     * @param world World name
     * @param group Group name
     * @param permission Permission node
     * @return Success or Failure
     */
    @Override
    public boolean groupAdd(String world, String group, String permission) {
        return permPlugin.setGroupPerm(group, permission, true);
    }

    /**
     * Add permission to a group.
     * Supports NULL value for World if the permission system registered supports global permissions.
     * But May return odd values if the servers registered permission system does not have a global permission store.
     *
     * @param world World Object
     * @param group Group name
     * @param permission Permission node
     * @return Success or Failure
     */
    @Override
    public boolean groupAdd(World world, String group, String permission) {
        return permPlugin.setGroupPerm(group, permission, true);
    }

    /**
     * Remove permission from a group.
     * Supports NULL value for World if the permission system registered supports global permissions.
     * But May return odd values if the servers registered permission system does not have a global permission store.
     *
     * @param world World name
     * @param group Group name
     * @param permission Permission node
     * @return Success or Failure
     */
    @Override
    public boolean groupRemove(String world, String group, String permission) {
        return permPlugin.setGroupPerm(group, permission, null);
    }

    /**
     * Remove permission from a group.
     * Supports NULL value for World if the permission system registered supports global permissions.
     * But May return odd values if the servers registered permission system does not have a global permission store.
     *
     * @param world World Object
     * @param group Group name
     * @param permission Permission node
     * @return Success or Failure
     */
    @Override
    public boolean groupRemove(World world, String group, String permission) {
        return permPlugin.setGroupPerm(group, permission, null);
    }

    /**
     * @deprecated As of VaultAPI 1.4 use {@link #playerInGroup(String, OfflinePlayer, String)} instead.
     */
    @Override
    public boolean playerInGroup(String world, String player, String group) {
        UUID uuid = PlayerCache.uuidForName(player);
        if (uuid == null) return false;
        return permPlugin.playerInGroup(uuid, group);
    }

    /**
     * @deprecated As of VaultAPI 1.4 use {@link #playerInGroup(String, OfflinePlayer, String)} instead.
     */
    @Override
    public boolean playerInGroup(World world, String player, String group) {
        UUID uuid = PlayerCache.uuidForName(player);
        if (uuid == null) return false;
        return permPlugin.playerInGroup(uuid, group);
    }

    /**
     * Check if player is member of a group.
     * Supports NULL value for World if the permission system registered supports global permissions.
     * But May return odd values if the servers registered permission system does not have a global permission store.
     *
     * @param world World Object
     * @param player to check
     * @param group Group name
     * @return Success or Failure
     */
    @Override
    public boolean playerInGroup(String world, OfflinePlayer player, String group) {
        return permPlugin.playerInGroup(player.getUniqueId(), group);
    }

    /**
     * Check if player is member of a group.
     * This method will ONLY check groups for which the player is in that are defined for the current world.
     * This may result in odd return behaviour depending on what permission system has been registered.
     *
     * @param player Player Object
     * @param group Group name
     * @return Success or Failure
     */
    @Override
    public boolean playerInGroup(Player player, String group) {
        return permPlugin.playerInGroup(player.getUniqueId(), group);
    }

    /**
     * @deprecated As of VaultAPI 1.4 use {@link #playerAddGroup(String, OfflinePlayer, String)} instead.
     */
    @Override
    public boolean playerAddGroup(String world, String player, String group) {
        UUID uuid = PlayerCache.uuidForName(player);
        if (uuid == null) return false;
        return permPlugin.setMembership(uuid, group, true);
    }

    /**
     * @deprecated As of VaultAPI 1.4 use {@link #playerAddGroup(String, OfflinePlayer, String)} instead.
     */
    @Override
    public boolean playerAddGroup(World world, String player, String group) {
        UUID uuid = PlayerCache.uuidForName(player);
        if (uuid == null) return false;
        return permPlugin.setMembership(uuid, group, true);
    }

    /**
     * Add player to a group.
     * Supports NULL value for World if the permission system registered supports global permissions.
     * But May return odd values if the servers registered permission system does not have a global permission store.
     *
     * @param world String world name
     * @param player to add
     * @param group Group name
     * @return Success or Failure
     */
    @Override
    public boolean playerAddGroup(String world, OfflinePlayer player, String group) {
        return permPlugin.setMembership(player.getUniqueId(), group, true);
    }

    /**
     * Add player to a group.
     * This will add a player to the group on the current World.  This may return odd results if the permission system
     * being used on the server does not support world-specific groups, or if the group being added to is a global group.
     *
     * @param player Player Object
     * @param group Group name
     * @return Success or Failure
     */
    @Override
    public boolean playerAddGroup(Player player, String group) {
        return permPlugin.setMembership(player.getUniqueId(), group, true);
    }

    /**
     * @deprecated As of VaultAPI 1.4 use {@link #playerRemoveGroup(String, OfflinePlayer, String)} instead.
     */
    @Override
    public boolean playerRemoveGroup(String world, String player, String group) {
        UUID uuid = PlayerCache.uuidForName(player);
        if (uuid == null) return false;
        return permPlugin.setMembership(uuid, group, false);
    }

    /**
     * @deprecated As of VaultAPI 1.4 use {@link #playerRemoveGroup(String, OfflinePlayer, String)} instead.
     */
    @Override
    public boolean playerRemoveGroup(World world, String player, String group) {
        UUID uuid = PlayerCache.uuidForName(player);
        if (uuid == null) return false;
        return permPlugin.setMembership(uuid, group, false);
    }

    /**
     * Remove player from a group.
     * Supports NULL value for World if the permission system registered supports global permissions.
     * But May return odd values if the servers registered permission system does not have a global permission store.
     *
     * @param world World Object
     * @param player to remove
     * @param group Group name
     * @return Success or Failure
     */
    @Override
    public boolean playerRemoveGroup(String world, OfflinePlayer player, String group) {
        return permPlugin.setMembership(player.getUniqueId(), group, false);
    }

    /**
     * Remove player from a group.
     * This will add a player to the group on the current World.  This may return odd results if the permission system
     * being used on the server does not support world-specific groups, or if the group being added to is a global group.
     *
     * @param player Player Object
     * @param group Group name
     * @return Success or Failure
     */
    @Override
    public boolean playerRemoveGroup(Player player, String group) {
        return permPlugin.setMembership(player.getUniqueId(), group, false);
    }

    /**
     * @deprecated As of VaultAPI 1.4 use {@link #getPlayerGroups(String, OfflinePlayer)} instead.
     */
    @Override
    public String[] getPlayerGroups(String world, String player) {
        UUID uuid = PlayerCache.uuidForName(player);
        if (uuid == null) return new String[0];
        return permPlugin.findPlayerGroups(uuid).toArray(new String[0]);
    }

    /**
     * @deprecated As of VaultAPI 1.4 use {@link #getPlayerGroups(String, OfflinePlayer)} instead.
     */
    @Override
    public String[] getPlayerGroups(World world, String player) {
        UUID uuid = PlayerCache.uuidForName(player);
        if (uuid == null) return new String[0];
        return permPlugin.findPlayerGroups(uuid).toArray(new String[0]);
    }

    /**
     * Gets the list of groups that this player has
     * Supports NULL value for World if the permission system registered supports global permissions.
     * But May return odd values if the servers registered permission system does not have a global permission store.
     *
     * @param world String world name
     * @param player OfflinePlayer
     * @return Array of groups
     */
    @Override
    public String[] getPlayerGroups(String world, OfflinePlayer player) {
        return permPlugin.findPlayerGroups(player.getUniqueId()).toArray(new String[0]);
    }

    /**
     * Returns a list of world-specific groups that this player is currently in. May return unexpected results if
     * you are looking for global groups, or if the registered permission system does not support world-specific groups.
     * See {@link #getPlayerGroups(String, OfflinePlayer)} for better control of World-specific or global groups.
     *
     * @param player Player Object
     * @return Array of groups
     */
    @Override
    public String[] getPlayerGroups(Player player) {
        return permPlugin.findPlayerGroups(player.getUniqueId()).toArray(new String[0]);
    }

    /**
     * @deprecated As of VaultAPI 1.4 use {@link #getPrimaryGroup(String, OfflinePlayer)} instead.
     */
    @Override
    public String getPrimaryGroup(String world, String player) {
        UUID uuid = PlayerCache.uuidForName(player);
        if (uuid == null) return permPlugin.getDefaultGroup();
        List<String> groups = permPlugin.findPlayerGroups(uuid);
        if (groups.isEmpty()) return permPlugin.getDefaultGroup();
        String result = groups.get(0);
        int prio = 0;
        for (String name: groups) {
            SQLGroup group = permPlugin.getCache().findGroup(name);
            if (group != null && group.getPriority() > prio) {
                result = group.getDisplayName();
                prio = group.getPriority();
            }
        }
        return result;
    }

    /**
     * @deprecated As of VaultAPI 1.4 use {@link #getPrimaryGroup(String, OfflinePlayer)} instead.
     */
    @Override
    public String getPrimaryGroup(World world, String player) {
        UUID uuid = PlayerCache.uuidForName(player);
        if (uuid == null) return permPlugin.getDefaultGroup();
        List<String> groups = permPlugin.findPlayerGroups(uuid);
        if (groups.isEmpty()) return permPlugin.getDefaultGroup();
        return groups.get(0);
    }

    /**
     * Gets players primary group
     * Supports NULL value for World if the permission system registered supports global permissions.
     * But May return odd values if the servers registered permission system does not have a global permission store.
     *
     * @param world String world name
     * @param player to get from
     * @return Players primary group
     */
    @Override
    public String getPrimaryGroup(String world, OfflinePlayer player) {
        List<String> groups = permPlugin.findPlayerGroups(player.getUniqueId());
        if (groups.isEmpty()) return permPlugin.getDefaultGroup();
        return groups.get(0);
    }

    /**
     * Get players primary group.
     * Defaults to the players current world, so may return only world-specific groups.
     * In most cases {@link #getPrimaryGroup(String, OfflinePlayer)} is preferable.
     *
     * @param player Player Object
     * @return Players primary group
     */
    @Override
    public String getPrimaryGroup(Player player) {
        List<String> groups = permPlugin.findPlayerGroups(player.getUniqueId());
        if (groups.isEmpty()) return permPlugin.getDefaultGroup();
        return groups.get(0);
    }

    /**
     * Returns a list of all known groups
     * @return an Array of String of all groups
     */
    @Override
    public String[] getGroups() {
        return permPlugin.getGroups().toArray(new String[0]);
    }

    /**
     * Returns true if the given implementation supports groups.
     * @return true if the implementation supports groups
     */
    @Override
    public boolean hasGroupSupport() {
        return true;
    }
}
