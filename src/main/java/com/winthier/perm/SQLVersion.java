package com.winthier.perm;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.Data;

@Data
@Table(name = "versions",
       uniqueConstraints =
       @UniqueConstraint(columnNames = { "name" }))
public final class SQLVersion {
    @Id private Integer id;
    @Column(nullable = false, length = 16) private String name;
    @Column(nullable = false) private Date version;

    public SQLVersion() { }

    public SQLVersion(final String name) {
        this.name = name;
        this.version = new Date();
    }

    public void setNow() {
        version = new Date();
    }
}
