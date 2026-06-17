package com.connecthub.modules.features.user.entity;

import com.connecthub.common.entity.BaseEntity;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user_blocks", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"blocker_id", "blocked_id"})
})
public class UserBlock extends BaseEntity {
    @Id
    private UUID id;
    @ManyToOne
    @JoinColumn(name = "blocker_id")
    private User blocker; // User who blocks
    @ManyToOne
    @JoinColumn(name = "blocked_id")
    private User blocked; // User who is blocked
}
