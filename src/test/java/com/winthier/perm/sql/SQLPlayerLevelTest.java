package com.winthier.perm.sql;

import org.junit.Assert;
import org.junit.Test;

public final class SQLPlayerLevelTest {
    @Test
    public void test() {
        test(100);
    }

    public void test(int amount) {
        final SQLPlayerLevel row = new SQLPlayerLevel();
        for (int i = 0; i < amount; i += 1) {
            row.addProgress();
            Assert.assertEquals(i, row.getTotalProgress());
        }
    }
}
