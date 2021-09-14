package com.winthier.perm;

import lombok.Getter;

@Getter
public enum ExtraRank implements Rank {
    BUILDER,
    DUTYMODE,
    GOAT,
    STREAMER;

    public final String key;

    ExtraRank() {
        this.key = name().toLowerCase();
    }
}
