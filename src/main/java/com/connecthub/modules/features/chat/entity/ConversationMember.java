package com.connecthub.modules.features.chat.entity;

import com.connecthub.common.entity.BaseEntity;
import com.connecthub.modules.features.chat.enums.MemberRole;
import com.connecthub.modules.features.chat.enums.MemberStatus;
import com.connecthub.modules.features.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@IdClass(ConversationMember.ConversationMemberId.class)
public class ConversationMember extends BaseEntity {
    @Id
    @ManyToOne
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;
    @Id
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    private LocalDateTime joinedAt;
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private MemberStatus status = MemberStatus.PENDING;  // mặc định PENDING

    // chỉ có giá trị khi là thành viên của GROUP, với PRIVATE luôn null
    @Enumerated(EnumType.STRING)
    private MemberRole role;


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class ConversationMemberId implements Serializable {
        private UUID conversation;
        private UUID user;
    }

}
