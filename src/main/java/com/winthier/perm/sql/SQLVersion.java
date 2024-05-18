package com.winthier.perm.sql;

import com.winthier.sql.SQLRow;
import com.winthier.sql.SQLRow.Name;
import com.winthier.sql.SQLRow.NotNull;
import java.util.Date;
import lombok.Data;

@Data @NotNull @Name("versions")
public final class SQLVersion implements SQLRow {
    @Id private Integer id;
    @Unique @VarChar(16) private String name;
    private Date version;

    public SQLVersion() { }

    public SQLVersion(final String name) {
        this.name = name;
        this.version = new Date();
    }

    public void setNow() {
        version = new Date();
    }
}
