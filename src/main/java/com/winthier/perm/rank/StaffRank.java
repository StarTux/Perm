package com.winthier.perm.rank;

import com.winthier.perm.Perm;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;

@Getter
public enum StaffRank implements Rank {
    TRUSTED,
    MODERATOR,
    ADMIN;
    public static final List<String> KEYS;
    public static final Map<String, StaffRank> KEY_MAP;

    public final String key;
    public final String displayName;

    StaffRank() {
        this.key = name().toLowerCase();
        this.displayName = name().substring(0, 1) + name().substring(1).toLowerCase();
    }

    static {
        KEYS = Stream.of(StaffRank.values())
            .map(s -> s.key)
            .collect(Collectors.toUnmodifiableList());
        KEY_MAP = Stream.of(StaffRank.values())
            .collect(Collectors.toUnmodifiableMap(StaffRank::getKey, Function.identity()));
    }

    public static StaffRank ofPlayer(UUID uuid) {
        Collection<String> groups = Perm.getGroups(uuid);
        if (groups.isEmpty()) return null;
        for (String group : groups) {
            StaffRank it = KEY_MAP.get(group);
            if (it != null) return it;
        }
        return null;
    }
}
