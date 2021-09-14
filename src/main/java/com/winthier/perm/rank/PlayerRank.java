package com.winthier.perm.rank;

import com.winthier.perm.Perm;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
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
    public static final Map<String, PlayerRank> KEY_MAP;

    public final String key;
    public final String displayName;

    PlayerRank() {
        this.key = name().toLowerCase();
        this.displayName = name().substring(0, 1) + name().substring(1).toLowerCase();
    }

    static {
        KEYS = Stream.of(PlayerRank.values())
            .map(PlayerRank::getKey)
            .collect(Collectors.toUnmodifiableList());
        KEY_MAP = Stream.of(PlayerRank.values())
            .collect(Collectors.toUnmodifiableMap(PlayerRank::getKey, Function.identity()));
    }

    public boolean gt(PlayerRank other) {
        return ordinal() > other.ordinal();
    }

    public boolean gte(PlayerRank other) {
        return ordinal() >= other.ordinal();
    }

    public boolean has(UUID uuid) {
        PlayerRank[] all = values();
        for (int i = ordinal(); i < all.length; i += 1) {
            if (Perm.isInGroup(uuid, all[i].key)) return true;
        }
        return false;
    }

    public boolean promote(UUID uuid) {
        if (ordinal() == 0) return false;
        PlayerRank below = PlayerRank.values()[ordinal() - 1];
        return below.ordinal() != 0
            ? Perm.replaceGroup(uuid, below.key, key)
            : Perm.addGroup(uuid, key);
    }

    public static PlayerRank ofKey(String key) {
        return KEY_MAP.get(key);
    }
}
