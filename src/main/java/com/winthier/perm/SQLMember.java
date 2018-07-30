package com.winthier.perm;

import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.Data;

@Data
@Table(name = "members",
       uniqueConstraints = { @UniqueConstraint(columnNames = { "member", "group" }) })
public final class SQLMember {
    @Id private Integer id;
    @Column(nullable = false) private UUID member;
    @Column(nullable = false, length = 16) private String group;

    public SQLMember() { }

    SQLMember(UUID member, String group) {
        this.member = member;
        this.group = group;
    }
}
