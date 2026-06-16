package com.connecthub.entity;

import com.connecthub.enums.Visibility;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Post extends BaseEntity{
    @Id
    private UUID id = UuidCreator.getTimeOrderedEpoch();
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
    private LocalDateTime deletedAt;
    @OneToMany(mappedBy = "post")
    @ToString.Exclude
    private Set<Media> media;
    @OneToMany(mappedBy = "post")
    @ToString.Exclude
    private Set<Reaction> reactions;
    @OneToMany(mappedBy = "post")
    @ToString.Exclude
    private Set<Bookmark> bookmarks;
    @OneToMany(mappedBy = "post")
    @ToString.Exclude
    private Set<Repost> reposts;
    @OneToMany(mappedBy = "post")
    @ToString.Exclude
    private Set<PostView> postViews;

    @OneToMany(mappedBy = "post")
    @ToString.Exclude
    private Set<FeedItem> feedItems;
    @OneToMany(mappedBy = "post")
    @ToString.Exclude
    private Set<Mention> mentions;

    @OneToMany(mappedBy = "post")
    @ToString.Exclude
    private Set<PostHashtag> postHashtags;
    @OneToMany(mappedBy = "post")
    @ToString.Exclude
    private Set<Notification> notifications;

    @OneToMany(mappedBy = "post")
    @ToString.Exclude
    private Set<Report> reports;
}
