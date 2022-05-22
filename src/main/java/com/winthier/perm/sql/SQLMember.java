package com.winthier.perm.sql;

import com.winthier.sql.SQLRow;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.Data;

@Data
@Table(name = "members",
       uniqueConstraints =
       @UniqueConstraint(columnNames = { "member",
                                         "group" }))
public final class SQLMember implements SQLRow {
    @Id private Integer id;
    @Column(nullable = false) private UUID member;
    @Column(nullable = false, length = 16) private String group;

    public SQLMember() { }

    public SQLMember(final UUID member, final String group) {
        this.member = member;
        this.group = group;
    }
}
