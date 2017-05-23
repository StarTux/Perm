package com.winthier.perm;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;

@Data @Table(name = "version")
public final class SQLVersion {
    @Id private Integer id;
    @Column(nullable = false) private Date version;
}
