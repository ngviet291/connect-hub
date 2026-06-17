package com.connecthub.modules.features.social.entity;

import com.connecthub.common.entity.BaseEntity;
import com.connecthub.modules.features.post.entity.Post;
import com.connecthub.modules.features.user.entity.User;
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
@Table(name = "feed_items")
public class FeedItem extends BaseEntity {
    @Id
    private UUID id;
    @ManyToOne
    @JoinColumn(name = "post_id")
    private Post post;
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    private double score; // Relevance score for ranking
}
