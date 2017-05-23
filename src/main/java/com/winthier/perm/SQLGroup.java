package com.winthier.perm;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.Data;

@Data
@Table(name = "groups",
       uniqueConstraints = { @UniqueConstraint(columnNames = { "key" }) })
public final class SQLGroup {
    @Id private Integer id;
    @Column(nullable = false) private String key; // Always lower case!
    @Column(nullable = false) private Integer priority;
    @Column(nullable = false) private String displayName;
    @Column private String parent;

    public SQLGroup() { }

    SQLGroup(String key, int priority, String displayName, String parent) {
        this.key = key;
        this.priority = priority;
        this.displayName = displayName;
        this.parent = parent;
    }
}
