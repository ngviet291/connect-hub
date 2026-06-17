package com.connecthub.modules.features.user.entity;

import com.connecthub.common.entity.BaseEntity;
import com.connecthub.modules.features.user.enums.RoleName;
import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "roles")
public class Role extends BaseEntity {
    @Id
    private String id;

    @Column(unique = true)
    @Enumerated(jakarta.persistence.EnumType.STRING)
    private RoleName name;

    @ManyToMany(mappedBy = "roles")
    private Set<User> users;
}
