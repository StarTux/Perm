package com.winthier.perm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public enum PlayerRank {
    GUEST,
    FRIENDLY,
    MEMBER;
    public static final List<String> KEYS;

    public final String key;

    PlayerRank() {
        this.key = name().toLowerCase();
    }

    static {
        List<String> keys = new ArrayList<>();
        for (PlayerRank it : PlayerRank.values()) {
            keys.add(it.key);
        }
        KEYS = Collections.unmodifiableList(keys);
    }

    public boolean promote(UUID uuid) {
        if (ordinal() == 0) return false;
        PlayerRank below = PlayerRank.values()[ordinal() - 1];
        int result;
        if (below.ordinal() != 0) {
            result = PermPlugin.instance.db.find(SQLMember.class)
                .eq("member", uuid)
                .eq("group", below.key)
                .delete();
            if (result != 1) return false;
        }
        result = PermPlugin.instance.db.insert(new SQLMember(uuid, key));
        if (result != 1) return false;
        PermPlugin.instance.updateVersion();
        PermPlugin.instance.refreshPermissions();
        return true;
    }
}
