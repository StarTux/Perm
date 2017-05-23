package com.winthier.perm;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import lombok.Value;

final class Legacy {
    private Legacy() { }

    @Value static final class ZEntity {
        private int id;
        private String name;
        private boolean isGroup;
        private String displayName;
        private int priority;
        private UUID getUuid() {
            return makeUuid(name);
        }
    }

    @Value static final class ZEntry {
        private int id;
        private int entityId;
        private String permission;
        private boolean value;
    }

    @Value static final class ZInheritance {
        private int id;
        private int childId, parentId;
    }

    @Value static final class ZMembership {
        private int id;
        private UUID member;
        private int groupId;
    }

    static UUID makeUuid(String string) {
        return UUID.fromString(string.substring(0, 8)
                               + "-" + string.substring(8, 12)
                               + "-" + string.substring(12, 16)
                               + "-" + string.substring(16, 20)
                               + "-" + string.substring(20));
    }

    static void migrate(PermPlugin plugin) throws SQLException {
        // Read
        ResultSet row;
        HashMap<Integer, ZEntity> zentities = new HashMap<>();
        HashMap<Integer, ZEntry> zentries = new HashMap<>();
        HashMap<Integer, ZInheritance> zinheritances = new HashMap<>();
        HashMap<Integer, ZMembership> zmemberships = new HashMap<>();
        row = plugin.getDb().executeQuery("SELECT * FROM zPermissions.entities");
        while (row.next()) {
            ZEntity zentity = new ZEntity(row.getInt("id"),
                                          row.getString("name"),
                                          row.getBoolean("is_group"),
                                          row.getString("display_name"),
                                          row.getInt("priority"));
            zentities.put(zentity.id, zentity);
        }
        System.out.println(zentities.size() + " zEntities");
        row = plugin.getDb().executeQuery("SELECT * FROM zPermissions.entries WHERE region_id IS NULL AND world_id IS NULL");
        while (row.next()) {
            ZEntry zentry = new ZEntry(row.getInt("id"),
                                       row.getInt("entity_id"),
                                       row.getString("permission"),
                                       row.getBoolean("value"));
            zentries.put(zentry.id, zentry);
        }
        System.out.println(zentries.size() + " zEntries");
        row = plugin.getDb().executeQuery("SELECT * FROM zPermissions.inheritances");
        while (row.next()) {
            ZInheritance zinheritance = new ZInheritance(row.getInt("id"),
                                                         row.getInt("child_id"),
                                                         row.getInt("parent_id"));
            zinheritances.put(zinheritance.id, zinheritance);
        }
        System.out.println(zinheritances.size() + " zInheritances");
        row = plugin.getDb().executeQuery("SELECT * FROM zPermissions.memberships");
        while (row.next()) {
            ZMembership zmembership = new ZMembership(row.getInt("id"),
                                                      makeUuid(row.getString("member")),
                                                      row.getInt("group_id"));
            zmemberships.put(zmembership.id, zmembership);
        }
        System.out.println(zmemberships.size() + " zMemberships");
        // Migrate
        ArrayList<SQLPermission> permissions = new ArrayList<>();
        ArrayList<SQLGroup> groups = new ArrayList<>();
        ArrayList<SQLMember> members = new ArrayList<>();
        for (ZEntry zentry: zentries.values()) {
            SQLPermission permission;
            ZEntity zentity = zentities.get(zentry.entityId);
            if (zentity.isGroup()) {
                permission = new SQLPermission(zentity.name.toLowerCase(),
                                               true,
                                               zentry.permission,
                                               zentry.value);
            } else {
                permission = new SQLPermission(zentity.getUuid().toString(),
                                               false,
                                               zentry.permission,
                                               zentry.value);
            }
            permissions.add(permission);
        }
        System.out.println(permissions.size() + " Permissions");
        for (ZEntity zentity: zentities.values()) {
            if (zentity.isGroup) {
                String parent = null;
                for (ZInheritance zinheritance: zinheritances.values()) {
                    if (zinheritance.childId == zentity.id) {
                        parent = zentities.get(zinheritance.parentId).name.toLowerCase();
                    }
                }
                SQLGroup group = new SQLGroup(zentity.name.toLowerCase(),
                                              zentity.priority,
                                              zentity.displayName,
                                              parent);
                groups.add(group);
            }
        }
        System.out.println(groups.size() + " Groups");
        for (ZMembership zmembership: zmemberships.values()) {
            ZEntity zgroup = zentities.get(zmembership.groupId);
            SQLMember member = new SQLMember(zmembership.member,
                                             zgroup.name.toLowerCase());
            members.add(member);
        }
        System.out.println(members.size() + " Members");
        plugin.getDb().save(permissions);
        plugin.getDb().save(groups);
        plugin.getDb().save(members);
    }
}
