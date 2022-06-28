package com.winthier.perm.sql;

import com.winthier.sql.SQLDatabase;
import org.junit.Test;

public final class SQLTest {
    @Test
    public void testTalentTypes() {
        System.out.println(SQLDatabase.testTableCreation(SQLGroup.class));
        System.out.println(SQLDatabase.testTableCreation(SQLMember.class));
        System.out.println(SQLDatabase.testTableCreation(SQLPermission.class));
        System.out.println(SQLDatabase.testTableCreation(SQLLevel.class));
        System.out.println(SQLDatabase.testTableCreation(SQLPlayerLevel.class));
        System.out.println(SQLDatabase.testTableCreation(SQLVersion.class));
    }
}
