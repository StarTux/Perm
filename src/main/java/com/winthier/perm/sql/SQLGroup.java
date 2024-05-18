package com.winthier.perm.sql;

import com.winthier.sql.SQLRow;
import com.winthier.sql.SQLRow.Name;
import com.winthier.sql.SQLRow.NotNull;
import lombok.Data;

@Data @NotNull @Name("groups")
public final class SQLGroup implements SQLRow {
    @Id private Integer id;
    @Unique @VarChar(16) private String key;
    private int priority;
    @VarChar(32) private String displayName;
    @Nullable @VarChar(16) private String parent;

    public SQLGroup() { }

    public SQLGroup(final String key, final int priority, final String displayName, final String parent) {
        this.key = key;
        this.priority = priority;
        this.displayName = displayName;
        this.parent = parent;
    }
}
