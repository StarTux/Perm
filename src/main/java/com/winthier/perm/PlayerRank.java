package com.winthier.perm;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;

@Getter
public enum PlayerRank implements Rank {
    GUEST,
    FRIENDLY,
    MEMBER,
    CAVER,
    SPELEOLOGIST,
    EXPLORER;
    public static final List<String> KEYS;

    public final String key;

    PlayerRank() {
        this.key = name().toLowerCase();
    }

    static {
        KEYS = Stream.of(StaffRank.values())
            .map(s -> s.key)
            .collect(Collectors.toUnmodifiableList());
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
        PermPlugin.instance.refreshPermissionsAsync();
        return true;
    }
}
