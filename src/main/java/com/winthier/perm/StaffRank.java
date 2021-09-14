package com.winthier.perm;

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

    StaffRank() {
        this.key = name().toLowerCase();
    }

    static {
        KEYS = Stream.of(StaffRank.values())
            .map(s -> s.key)
            .collect(Collectors.toUnmodifiableList());
    }
}
