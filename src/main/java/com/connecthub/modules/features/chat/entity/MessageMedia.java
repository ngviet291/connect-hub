package com.connecthub.modules.features.chat.entity;

import com.connecthub.common.entity.BaseEntity;
import com.connecthub.modules.features.post.enums.MediaType;
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
@Table(name = "message_media")
public class MessageMedia extends BaseEntity {
    @Id
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "message_id")
    private Message message;
    private String url;
    @Enumerated(EnumType.STRING)
    private MediaType type;

    private String publicId;
}
