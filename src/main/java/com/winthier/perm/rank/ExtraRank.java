package com.winthier.perm.rank;

import lombok.Getter;

@Getter
public enum ExtraRank implements Rank {
    BUILDER,
    DUTYMODE,
    GOAT,
    STREAMER;

    public final String key;
    public final String displayName;

    ExtraRank() {
        this.key = name().toLowerCase();
        this.displayName = name().substring(0, 1) + name().substring(1).toLowerCase();
    }
}
