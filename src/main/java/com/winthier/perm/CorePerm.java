package com.winthier.perm;

import com.cavetale.core.perm.Perm;
import com.winthier.perm.sql.SQLMember;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class CorePerm implements Perm {
    private final PermPlugin plugin;

    @Override
    public PermPlugin getPlugin() {
        return plugin;
    }

    @Override
    public boolean has(UUID uuid, String permission) {
        return plugin.playerHasPerm(uuid, permission);
    }

    @Override
    public boolean set(UUID uuid, String permission, boolean value) {
        return plugin.setPlayerPerm(uuid, permission, value);
    }

    @Override
    public boolean unset(UUID uuid, String permission) {
        return plugin.setPlayerPerm(uuid, permission, null);
    }

    @Override
    public boolean isInGroup(UUID uuid, String groupName) {
        return plugin.playerInGroup(uuid, groupName);
    }

    @Override
    public Collection<String> getGroups(UUID uuid) {
        return plugin.cache.findAssignedGroups(uuid);
    }

    @Override
    public Collection<String> getAllGroups(UUID uuid) {
        return plugin.cache.findDeepPlayerGroups(uuid);
    }

    @Override
    public Map<String, Boolean> getPerms(UUID uuid) {
        return plugin.cache.findPlayerPerms(uuid);
    }

    @Override
    public boolean removeGroup(UUID uuid, String group) {
        int count = plugin.db.find(SQLMember.class)
            .eq("member", uuid)
            .eq("group", group)
            .delete();
        if (count == 0) return false;
        plugin.updateVersion();
        plugin.refreshPermissionsAsync();
        return true;
    }

    @Override
    public boolean addGroup(UUID uuid, String group) {
        int count = plugin.db.insert(new SQLMember(uuid, group));
        if (count == 0) return false;
        plugin.updateVersion();
        plugin.refreshPermissionsAsync();
        return true;
    }

    @Override
    public boolean replaceGroup(UUID uuid, String oldGroup, String newGroup) {
        int count = plugin.db.find(SQLMember.class)
            .eq("member", uuid)
            .eq("group", oldGroup)
            .delete();
        if (count == 0) return false;
        count = plugin.db.insert(new SQLMember(uuid, newGroup));
        if (count == 0) return false;
        plugin.updateVersion();
        plugin.refreshPermissionsAsync();
        return true;
    }

    @Override
    public int getLevel(UUID uuid) {
        return plugin.getPlayerLevel(uuid);
    }

    @Override
    public int getLevelProgress(UUID uuid) {
        return plugin.getPlayerLevelProgress(uuid);
    }

    @Override
    public void addLevelProgress(UUID uuid) {
        plugin.addPlayerLevelProgress(uuid);
    }
}
