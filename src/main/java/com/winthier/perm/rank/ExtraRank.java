package com.winthier.perm.rank;

import com.winthier.perm.Perm;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;

@Getter
public enum ExtraRank implements Rank {
    BUILDER,
    DUTYMODE,
    GOAT("GOAT"),
    STREAMER;

    public static final List<String> KEYS;
    public static final Map<String, ExtraRank> KEY_MAP;
    public final String key;
    public final String displayName;

    ExtraRank() {
        this.key = name().toLowerCase();
        this.displayName = name().substring(0, 1) + name().substring(1).toLowerCase();
    }

    ExtraRank(final String displayName) {
        this.key = name().toLowerCase();
        this.displayName = displayName;
    }

    static {
        KEYS = Stream.of(ExtraRank.values())
            .map(s -> s.key)
            .collect(Collectors.toUnmodifiableList());
        KEY_MAP = Stream.of(ExtraRank.values())
            .collect(Collectors.toUnmodifiableMap(ExtraRank::getKey, Function.identity()));
    }

    public static Set<ExtraRank> ofPlayer(UUID uuid) {
        Collection<String> groups = Perm.getGroups(uuid);
        if (groups.isEmpty()) return EnumSet.noneOf(ExtraRank.class);
        Set<ExtraRank> set = EnumSet.noneOf(ExtraRank.class);
        for (String group : groups) {
            ExtraRank it = KEY_MAP.get(group);
            if (it != null) set.add(it);
        }
        return set;
    }
}
