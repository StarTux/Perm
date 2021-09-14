package com.winthier.perm;

import com.winthier.perm.sql.SQLMember;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Perm {
    private Perm() { }

    public static boolean has(UUID uuid, String permission) {
        return PermPlugin.instance.playerHasPerm(uuid, permission);
    }

    public static boolean isInGroup(UUID uuid, String groupName) {
        return PermPlugin.instance.playerInGroup(uuid, groupName);
    }

    public static Collection<String> getGroups(UUID uuid) {
        return PermPlugin.instance.findPlayerGroups(uuid);
    }

    public static Collection<String> getPerms(UUID uuid) {
        Map<String, Boolean> map = PermPlugin.instance.cache.findPlayerPerms(uuid);
        List<String> result = new ArrayList<>(map.size());
        for (Map.Entry<String, Boolean> entry : map.entrySet()) {
            if (entry.getValue() == Boolean.TRUE) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    public static boolean removeGroup(UUID uuid, String group) {
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

    public static boolean addGroup(UUID uuid, String group) {
        PermPlugin plugin = PermPlugin.getInstance();
        int count = plugin.db.insert(new SQLMember(uuid, group));
        if (count == 0) return false;
        plugin.updateVersion();
        plugin.refreshPermissionsAsync();
        return true;
    }

    public static boolean replaceGroup(UUID uuid, String oldGroup, String newGroup) {
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
