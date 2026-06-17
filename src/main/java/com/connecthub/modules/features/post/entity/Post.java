package com.connecthub.modules.features.post.entity;

import com.connecthub.common.entity.BaseEntity;
import com.connecthub.modules.features.post.enums.Visibility;
import com.connecthub.modules.features.moderation.entity.Report;
import com.connecthub.modules.features.notification.entity.Notification;
import com.connecthub.modules.features.social.entity.FeedItem;
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
@SQLDelete(sql = "UPDATE post SET deleted_at = NOW() ,is_deleted = true WHERE id = ?")
@Table(name = "post")
public class Post extends BaseEntity {
    @Id
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    @ManyToOne
    @JoinColumn(name = "parent_post_id")
    private Post parentPost;
    private String content;
    @ManyToOne
    @JoinColumn(name = "quote_post_id")
    private Post quotePost;
    @Enumerated(EnumType.STRING)
    private Visibility visibility;
    private boolean isDeleted;
    private LocalDateTime deletedAt;
    @OneToMany(mappedBy = "post")
    
    private Set<Media> media;
    @OneToMany(mappedBy = "post")
    
    private Set<Reaction> reactions;
    @OneToMany(mappedBy = "post")
    
    private Set<Bookmark> bookmarks;
    @OneToMany(mappedBy = "post")
    
    private Set<Repost> reposts;
    @OneToMany(mappedBy = "post")
    
    private Set<PostView> postViews;

    @OneToMany(mappedBy = "post")
    
    private Set<FeedItem> feedItems;
    @OneToMany(mappedBy = "post")
    
    private Set<Mention> mentions;

    @OneToMany(mappedBy = "post")
    
    private Set<PostHashtag> postHashtags;
    @OneToMany(mappedBy = "post")
    
    private Set<Notification> notifications;

    @OneToMany(mappedBy = "post")
    
    private Set<Report> reports;
}
