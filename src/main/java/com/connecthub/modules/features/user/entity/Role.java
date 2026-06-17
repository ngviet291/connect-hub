package com.connecthub.modules.features.user.entity;

import com.connecthub.common.entity.BaseEntity;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import lombok.*;

import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Role extends BaseEntity {
    @Id
    private UUID id = UuidCreator.getTimeOrderedEpoch();
    private String name;

    @ManyToMany(mappedBy = "roles")
    private Set<User> users;
}
