package com.connecthub.modules.features.post.entity;

import com.connecthub.common.entity.BaseEntity;
import com.connecthub.modules.features.post.enums.MediaType;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigInteger;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "media")
public class Media extends BaseEntity {
    @Id
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "post_id")
    private Post post;
    private String url;
    @Enumerated(EnumType.STRING)
    private MediaType type;
    private BigInteger size;
}
