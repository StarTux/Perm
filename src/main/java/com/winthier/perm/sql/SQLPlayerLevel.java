package com.winthier.perm.sql;

import com.winthier.sql.SQLRow;
import com.winthier.sql.SQLRow.Name;
import com.winthier.sql.SQLRow.NotNull;
import java.util.Date;
import java.util.UUID;
import lombok.Data;

@Data
@NotNull
@Name("player_levels")
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

    public SQLPlayerLevel clone() {
        final SQLPlayerLevel result = new SQLPlayerLevel();
        result.id = id;
        result.player = player;
        result.level = level;
        result.progress = progress;
        result.updated = updated;
        return result;
    }

    public int getTotalProgress() {
        int result = progress;
        for (int i = 0; i < level; i += 1) {
            result += i;
        }
        return result;
    }

    public void addProgress() {
        progress += 1;
        if (progress >= level) {
            level += 1;
            progress = 0;
        }
    }

    public void addProgress(int amount) {
        for (int i = 0; i < amount; i += 1) {
            addProgress();
        }
    }
}
