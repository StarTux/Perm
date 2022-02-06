package com.winthier.perm;

import com.cavetale.core.perm.Perm;
import com.winthier.perm.sql.SQLMember;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public final class CorePerm implements Perm {
    @Override
    public boolean has(UUID uuid, String permission) {
        return PermPlugin.instance.playerHasPerm(uuid, permission);
    }

    @Override
    public boolean isInGroup(UUID uuid, String groupName) {
        return PermPlugin.instance.playerInGroup(uuid, groupName);
    }

    @Override
    public Collection<String> getGroups(UUID uuid) {
        return PermPlugin.instance.cache.findAssignedGroups(uuid);
    }

    @Override
    public Collection<String> getAllGroups(UUID uuid) {
        return PermPlugin.instance.cache.findDeepPlayerGroups(uuid);
    }

    @Override
    public Map<String, Boolean> getPerms(UUID uuid) {
        return PermPlugin.instance.cache.findPlayerPerms(uuid);
    }

    @Override
    public boolean removeGroup(UUID uuid, String group) {
        PermPlugin plugin = PermPlugin.instance;
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
        PermPlugin plugin = PermPlugin.getInstance();
        int count = plugin.db.insert(new SQLMember(uuid, group));
        if (count == 0) return false;
        plugin.updateVersion();
        plugin.refreshPermissionsAsync();
        return true;
    }

    @Override
    public boolean replaceGroup(UUID uuid, String oldGroup, String newGroup) {
        PermPlugin plugin = PermPlugin.instance;
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
}
