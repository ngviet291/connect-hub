package com.connecthub.modules.features.moderation.entity;

import com.connecthub.common.entity.BaseEntity;
import com.connecthub.modules.features.moderation.enums.BanReason;
import com.connecthub.modules.features.user.entity.User;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "bans")
public class Ban extends BaseEntity {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banned_by")
    private User bannedBy;

    @Enumerated(EnumType.STRING)
    private BanReason reason;
    private String description;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unbanned_by")
    private User unbannedBy;

    private LocalDateTime unbannedAt;

    @Column(columnDefinition = "TEXT")
    private String unbanReason;// dùng để audit trail khi unban user


    // Check if the ban is currently active
    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        return unbannedAt == null
                && (endDate == null || now.isBefore(endDate));
    }

}
