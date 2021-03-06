package com.winthier.perm;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.Data;

@Data
@Table(name = "groups",
       uniqueConstraints =
       @UniqueConstraint(columnNames = { "key" }))
public final class SQLGroup {
    @Id private Integer id;
    // Always lower case!
    @Column(nullable = false, length = 16)
    private String key;
    @Column(nullable = false)
    private Integer priority;
    @Column(nullable = false, length = 32)
    private String displayName;
    @Column(nullable = true, length = 16)
    private String parent;

    public SQLGroup() { }

    SQLGroup(final String key,
             final int priority,
             final String displayName,
             final String parent) {
        this.key = key;
        this.priority = priority;
        this.displayName = displayName;
        this.parent = parent;
    }
}
