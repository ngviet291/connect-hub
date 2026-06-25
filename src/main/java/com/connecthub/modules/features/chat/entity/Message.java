package com.connecthub.modules.features.chat.entity;

import com.connecthub.common.entity.BaseEntity;
import com.connecthub.modules.features.user.entity.User;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "messages")
public class Message extends BaseEntity {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_to_message_id")
    private Message replyTo;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "replyTo")
    private Set<Message> replies;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private User sender;
    @Column(columnDefinition = "TEXT")
    private String content;

    @OneToMany(mappedBy = "message")
    private Set<MessageReceipt> messageReceipts;
    @OneToMany(mappedBy = "message")
    private Set<MessageMedia> messageMedia;

    // Chỉ dùng cho GROUP: cờ "đã delivered tới ít nhất 1 ai đó", không
    // theo từng thành viên — tránh ghi N row mỗi tin trong group lớn.
    // Với PRIVATE, field này không dùng (delivered theo từng người ở
    // MessageReceipt thay vì ở đây).
    private LocalDateTime deliveredAt;


    private boolean deleted;
    private LocalDateTime deletedAt;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by")
    private User deletedBy;
}
