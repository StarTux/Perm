package com.winthier.perm.sql;

import com.winthier.sql.SQLRow.Name;
import com.winthier.sql.SQLRow.NotNull;
import com.winthier.sql.SQLRow;
import java.util.Date;
import java.util.UUID;
import lombok.Data;

@Data @NotNull @Name("player_levels")
public final class SQLPlayerLevel implements SQLRow {
    @Id private Integer id;
    @Unique private UUID player;
    @Default("0") private int level;
    @Default("0") private int progress;
    @Default("NOW()") private Date updated;

    public SQLPlayerLevel() { }

    public SQLPlayerLevel(final UUID player) {
        this.player = player;
        this.updated = new Date();
    }
}
