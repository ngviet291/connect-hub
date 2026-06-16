package com.connecthub.entity;

import com.connecthub.enums.MediaType;
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
public class MessageMedia extends BaseEntity{
    @Id
    private UUID id = UuidCreator.getTimeOrderedEpoch();
    @ManyToOne
    @JoinColumn(name = "message_id")
    private Message message;
    private String url;
    @Enumerated(EnumType.STRING)
    private MediaType type;
}
