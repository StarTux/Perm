package com.winthier.perm.sql;

import com.winthier.sql.SQLRow;
import com.winthier.sql.SQLRow.Name;
import com.winthier.sql.SQLRow.NotNull;
import com.winthier.sql.SQLRow.UniqueKey;
import lombok.Data;

@Data @NotNull @Name("levels")
@UniqueKey({"level", "permission"})
public final class SQLLevel implements SQLRow, Comparable<SQLLevel> {
    @Id private int id;
    private int level;
    @VarChar(40) private String permission;
    private boolean value;
    @Nullable @VarChar(255) private String description;

    public SQLLevel() { }

    public SQLLevel(final int level, final String permission, final boolean value, final String description) {
        this.level = level;
        this.permission = permission;
        this.value = value;
        this.description = description;
    }

    @Override
    public int compareTo(SQLLevel other) {
        int r = Integer.compare(level, other.level);
        return r != 0
            ? r
            : String.CASE_INSENSITIVE_ORDER.compare(permission, other.permission);
    }
}
