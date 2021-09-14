package com.winthier.perm.sql;

import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.Data;
import lombok.NonNull;

@Data
@Table(name = "permissions",
       uniqueConstraints = @UniqueConstraint(columnNames = {"entity", "permission"}))
public final class SQLPermission {
    @Id
    private Integer id;
    @Column(nullable = false, length = 40)
    private String entity;
    @Column(nullable = false)
    private Boolean isGroup;
    @Column(nullable = false, length = 64)
    private String permission;
    @Column(nullable = false)
    private Boolean value;

    public SQLPermission() { }

    public SQLPermission(@NonNull final String entity,
                         final boolean isGroup,
                         @NonNull final String permission,
                         final boolean value) {
        this.entity = entity;
        this.isGroup = isGroup;
        this.permission = permission;
        this.value = value;
    }

    public UUID getUuid() {
        return UUID.fromString(entity);
    }
}
