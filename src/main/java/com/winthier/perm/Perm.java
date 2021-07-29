package com.winthier.perm;

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
        Map<String, Boolean> map = PermPlugin.instance.findPlayerPerms(uuid);
        List<String> result = new ArrayList<>(map.size());
        for (Map.Entry<String, Boolean> entry : map.entrySet()) {
            if (entry.getValue() == Boolean.TRUE) {
                result.add(entry.getKey());
            }
        }
        return result;
    }
}
