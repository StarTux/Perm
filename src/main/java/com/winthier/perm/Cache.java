package com.winthier.perm;

import com.winthier.sql.SQLDatabase;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class Cache {
    final List<SQLGroup> groups = new ArrayList<>();
    final List<SQLMember> members = new ArrayList<>();
    final List<SQLPermission> permissions = new ArrayList<>();
    SQLVersion version;

    SQLGroup findGroup(final String name) {
        for (SQLGroup group : groups) {
            if (name.equals(group.getKey())) return group;
        }
        return null;
    }
    // Map groups to list of parent groups.
    final HashMap<String, List<String>> deepGroupParents = new HashMap<>();
    // Map UUID to assigned perms.
    final HashMap<UUID, HashMap<String, Boolean>> flatPlayerPerms = new HashMap<>();
    // Map group key to assigned perms.
    final HashMap<String, HashMap<String, Boolean>> flatGroupPerms = new HashMap<>();
    // Map group key to list of members.  Without inheritance!
    final HashMap<String, Set<UUID>> groupMembers = new HashMap<>();
    // Map group id to priority.
    final HashMap<String, Integer> groupPrios = new HashMap<>();

    // Map set of group id to permissions. This is populated lazily and may be flushed any time.
    final HashMap<Set<String>, HashMap<String, Boolean>> deepGroupPerms = new HashMap<>();
    // Map UUID to permissions. This is populated lazily and may be flushed any time.
    final HashMap<UUID, Map<String, Boolean>> deepPlayerPerms = new HashMap<>();

    protected void load(SQLDatabase db) {
        this.groups.addAll(db.find(SQLGroup.class).findList());
        this.members.addAll(db.find(SQLMember.class).findList());
        this.permissions.addAll(db.find(SQLPermission.class).findList());
        this.version = db.find(SQLVersion.class).eq("name", "Perm").findUnique();
        if (version == null) version = new SQLVersion("Perm");
        deepGroupParents.put(PermPlugin.DEFAULT_GROUP, new ArrayList<>());
        for (SQLGroup row : groups) {
            final List<String> deepGroupParentList = new ArrayList<>();
            deepGroupParents.put(row.getKey(), deepGroupParentList);
            SQLGroup currentRow = row;
            do {
                deepGroupParentList.add(currentRow.getKey());
                if (currentRow.getParent() == null) {
                    currentRow = null;
                } else {
                    currentRow = findGroup(currentRow.getParent());
                }
            } while (currentRow != null);
            groupMembers.put(row.getKey(), new HashSet<>());
            groupPrios.put(row.getKey(), row.getPriority());
            flatGroupPerms.put(row.getKey(), new HashMap<>());
        }
        for (SQLMember row : members) {
            Set<UUID> set = groupMembers.get(row.getGroup());
            if (set == null) continue;
            set.add(row.getMember());
        }
        for (SQLPermission row : permissions) {
            if (row.getIsGroup()) {
                SQLGroup group = findGroup(row.getEntity());
                if (group == null) continue;
                flatGroupPerms.get(group.getKey())
                    .put(row.getPermission(), row.getValue());
            } else {
                UUID uuid = row.getUuid();
                HashMap<String, Boolean> perms = flatPlayerPerms.get(uuid);
                if (perms == null) {
                    perms = new HashMap<>();
                    flatPlayerPerms.put(uuid, perms);
                }
                perms.put(row.getPermission(), row.getValue());
            }
        }
    }

    protected Map<String, Boolean> findPlayerPerms(final UUID uuid) {
        Map<String, Boolean> perms = deepPlayerPerms.get(uuid);
        if (perms != null) return perms;
        Set<String> assignedGroups = new HashSet<>();
        for (SQLGroup group : groups) {
            if (groupMembers.get(group.getKey()).contains(uuid)) {
                assignedGroups.add(group.getKey());
            }
        }
        if (assignedGroups.isEmpty()) assignedGroups.add(PermPlugin.DEFAULT_GROUP);
        HashMap<String, Boolean> groupPerms = deepGroupPerms.get(assignedGroups);
        if (groupPerms == null) {
            groupPerms = new HashMap<>();
            List<String> groupList = new ArrayList<>();
            for (String key : assignedGroups) {
                groupPerms.put("rank." + key, true);
                groupList.addAll(deepGroupParents.get(key));
            }
            for (String key : groupList) {
                groupPerms.put("group." + key, true);
            }
            Collections.sort(groupList, (a, b) -> Integer.compare(groupPrios.get(a),
                                                                  groupPrios.get(b)));
            for (String key : groupList) {
                groupPerms.putAll(flatGroupPerms.get(key));
            }
            deepGroupPerms.put(assignedGroups, groupPerms);
        }
        perms = new HashMap<>(groupPerms);
        HashMap<String, Boolean> playerPerms = flatPlayerPerms.get(uuid);
        if (playerPerms != null) perms.putAll(playerPerms);
        deepPlayerPerms.put(uuid, perms);
        return perms;
    }

    protected Map<String, Boolean> findGroupPerms(final String groupName) {
        final Map<String, Boolean> perms = new HashMap<>();
        SQLGroup group = findGroup(groupName);
        if (group == null) return perms;
        Map<String, Integer> groupMap = new HashMap<>();
        while (group != null && !groupMap.containsKey(group.getKey())) {
            groupMap.put(group.getKey(), group.getPriority());
            if (group.getParent() == null) {
                group = null;
            } else {
                group = findGroup(group.getParent());
            }
        }
        final Map<String, Integer> prios = new HashMap<>();
        for (SQLPermission perm : permissions) {
            if (perm.getIsGroup()) {
                if (groupMap.containsKey(perm.getEntity())) {
                    Boolean oldPerm = perms.get(perm.getPermission());
                    if (oldPerm == null) {
                        perms.put(perm.getPermission(), perm.getValue());
                        prios.put(perm.getPermission(), groupMap.get(perm.getEntity()));
                    } else {
                        Integer oldPrio = prios.get(perm.getPermission());
                        if (oldPrio < groupMap.get(perm.getEntity())) {
                            perms.put(perm.getPermission(), perm.getValue());
                            prios.put(perm.getPermission(), groupMap.get(perm.getEntity()));
                        }
                    }
                }
            }
        }
        return perms;
    }
}
