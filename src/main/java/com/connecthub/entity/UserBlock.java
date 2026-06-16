package com.connecthub.entity;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class UserBlock extends BaseEntity{
    @Id
    private UUID id = UuidCreator.getTimeOrderedEpoch();
    @ManyToOne
    @JoinColumn(name = "blocker_id")
    private User blocker; // User who blocks
    @ManyToOne
    @JoinColumn(name = "blocked_id")
    private User blocked; // User who is blocked
}
