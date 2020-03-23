package com.winthier.perm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class Cache {
    List<SQLGroup> groups;
    List<SQLMember> members;
    List<SQLPermission> permissions;
    SQLVersion version;

    SQLGroup findGroup(final String name) {
        for (SQLGroup group: groups) {
            if (name.equals(group.getKey())) return group;
        }
        return null;
    }
    // Map groups to list of parent groups.
    final HashMap<String, List<String>>
        deepGroupParents = new HashMap<>();
    // Map UUID to assigned perms.
    final HashMap<UUID, HashMap<String, Boolean>>
        flatPlayerPerms = new HashMap<>();
    // Map group key to assigned perms.
    final HashMap<String, HashMap<String, Boolean>>
        flatGroupPerms = new HashMap<>();
    // Map group key to list of members.  Without inheritance!
    final HashMap<String, List<UUID>>
        groupMembers = new HashMap<>();
    // Map set of group id to permissions.
    final HashMap<Set<String>, HashMap<String, Boolean>>
        deepGroupPerms = new HashMap<>();
    // Map group id to priority.
    final HashMap<String, Integer>
        groupPrios = new HashMap<>();

    // Map UUID to permissions.  This is populated lazily and may
    // be flushed any time.
    final HashMap<UUID, Map<String, Boolean>>
        deepPlayerPerms = new HashMap<>();
}
