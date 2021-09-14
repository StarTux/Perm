package com.winthier.perm.rank;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;

@Getter
public enum StaffRank implements Rank {
    TRUSTED,
    MODERATOR,
    ADMIN;
    public static final List<String> KEYS;

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
    }
}
