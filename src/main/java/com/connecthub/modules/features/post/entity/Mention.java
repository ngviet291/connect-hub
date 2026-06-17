package com.connecthub.modules.features.post.entity;

import com.connecthub.common.entity.BaseEntity;
import com.connecthub.modules.features.user.entity.User;
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
public class Mention extends BaseEntity {
    @Id
    private UUID id = UuidCreator.getTimeOrderedEpoch();
    @ManyToOne
    @JoinColumn(name = "post_id")
    private Post post;
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}
