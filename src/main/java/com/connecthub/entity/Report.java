package com.connecthub.entity;

import com.connecthub.enums.ReportStatus;
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
public class Report extends BaseEntity{
    @Id
    private UUID id = UuidCreator.getTimeOrderedEpoch();
    @ManyToOne
    @JoinColumn(name = "reporter_id")
    private User reporter;
    @ManyToOne
    @JoinColumn(name = "target_user_id")
    private User targetUser;
    @ManyToOne
    @JoinColumn(name = "post_id")
    private Post post;
    private String reason;
    @Enumerated(EnumType.STRING)
    private ReportStatus status;

}
