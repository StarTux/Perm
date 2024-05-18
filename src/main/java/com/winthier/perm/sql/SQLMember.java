package com.winthier.perm.sql;

import com.winthier.sql.SQLRow;
import com.winthier.sql.SQLRow.Name;
import com.winthier.sql.SQLRow.NotNull;
import com.winthier.sql.SQLRow.UniqueKey;
import java.util.UUID;
import lombok.Data;

@Data @NotNull @Name("members")
@UniqueKey({"member", "group"})
public final class SQLMember implements SQLRow {
    @Id private Integer id;
    private UUID member;
    @VarChar(16) private String group;

    public SQLMember() { }

    public SQLMember(final UUID member, final String group) {
        this.member = member;
        this.group = group;
    }
}
