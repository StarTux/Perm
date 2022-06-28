package com.winthier.perm.sql;

import com.winthier.sql.SQLRow;
import com.winthier.sql.SQLRow.Name;
import com.winthier.sql.SQLRow.NotNull;
import com.winthier.sql.SQLRow.UniqueKey;
import java.util.UUID;
import lombok.Data;

@Data @NotNull @Name("permission")
@UniqueKey({"entity", "permission"})
public final class SQLPermission implements SQLRow {
    @Id private Integer id;
    @VarChar(40) private String entity;
    private boolean isGroup;
    @VarChar(64) private String permission;
    private boolean value;

    public SQLPermission() { }

    public SQLPermission(final String entity, final boolean isGroup, final String permission, final boolean value) {
        this.entity = entity;
        this.isGroup = isGroup;
        this.permission = permission;
        this.value = value;
    }

    public UUID getUuid() {
        return UUID.fromString(entity);
    }
}
