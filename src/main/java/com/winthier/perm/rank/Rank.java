package com.winthier.perm.rank;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public interface Rank {
    String getKey();
    String getDisplayName();

    static List<Rank> all() {
        List<Rank> all = new ArrayList<>();
        all.addAll(Arrays.asList(PlayerRank.values()));
        all.addAll(Arrays.asList(StaffRank.values()));
        all.addAll(Arrays.asList(ExtraRank.values()));
        return all;
    }
}
