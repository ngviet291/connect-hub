package com.connecthub.modules.features.moderation.entity;

import com.connecthub.common.entity.BaseEntity;
import com.connecthub.modules.features.moderation.enums.ReasonType;
import com.connecthub.modules.features.post.entity.Post;
import com.connecthub.modules.features.moderation.enums.ReportStatus;
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
@Table(name = "reports")
public class Report extends BaseEntity {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id")
    private User reporter;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id")
    private User targetUser;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @Enumerated(EnumType.STRING)
    private ReasonType reason;

    private String description;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ReportStatus status = ReportStatus.PENDING;


    @ManyToOne
    @JoinColumn(name = "resolved_by")
    private User resolvedBy;    //Admin/Mod đã xử lý report này (audit trail)
    private LocalDateTime resolvedAt;    // Thời điểm report được xử lý xong

    private String resolutionNote;    //  chú về cách xử lý report (audit trail)

}
