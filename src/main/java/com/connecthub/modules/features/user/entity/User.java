package com.connecthub.modules.features.user.entity;

import com.connecthub.common.entity.BaseEntity;
import com.connecthub.modules.features.chat.entity.ConversationMember;
import com.connecthub.modules.features.chat.entity.Message;
import com.connecthub.modules.features.chat.entity.MessageReceipt;
import com.connecthub.modules.features.moderation.entity.Ban;
import com.connecthub.modules.features.moderation.entity.Report;
import com.connecthub.modules.features.notification.entity.Notification;
import com.connecthub.modules.features.post.entity.*;
import com.connecthub.modules.features.social.entity.FeedItem;
import com.connecthub.modules.features.social.entity.Follow;
import com.connecthub.modules.features.user.enums.UserStatus;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import lombok.*;

import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class User extends BaseEntity {
    @Id
    private UUID id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;
    @Column(nullable = false)
    private String password;
    @Column(nullable = false)
    private String fullName;
    @Column(nullable = false, unique = true)
    private String phoneNumber;
    private String avatarUrl;
    private String bio;

    @Builder.Default
    private boolean isLocked = false;

    @Builder.Default
    private boolean isActive = true;

    @Enumerated(EnumType.STRING)
    private UserStatus status;

    @OneToMany(mappedBy = "follower")
    private Set<Follow> followers;

    @OneToMany(mappedBy = "following")
    private Set<Follow> following;

    @OneToMany(mappedBy = "blocked", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserBlock> blocked; // Users this user has blocked

    @OneToMany(mappedBy = "user")
    private Set<Post> posts;

    @OneToMany(mappedBy = "user")
    private Set<Reaction> reactions;

    @OneToMany(mappedBy = "user")
    private Set<Repost> reposts;

    @OneToMany(mappedBy = "user")
    private Set<PostView> postViews;

    @OneToMany(mappedBy = "user")
    private Set<Bookmark> bookmarks;

    @OneToMany(mappedBy = "user")
    private Set<FeedItem> feedItems;

    @OneToMany(mappedBy = "user")
    private Set<Mention> mentions;

    @OneToMany(mappedBy = "recipient")
    private Set<Notification> notifications;

    @OneToMany(mappedBy = "actor")
    private Set<Notification> actedNotifications;

    @OneToMany(mappedBy = "user")
    private Set<ConversationMember> conversationMembers;

    @OneToMany(mappedBy = "sender")
    private Set<Message> messages;

    @OneToMany(mappedBy = "user")
    private Set<MessageReceipt> messageReceipts;

    @OneToMany(mappedBy = "reporter")
    private Set<Report> reporter;

    @OneToMany(mappedBy = "targetUser")
    private Set<Report> reported;

    @OneToMany(mappedBy = "user")
    private Set<Ban> target;

    @OneToMany(mappedBy = "bannedBy")
    private Set<Ban> admin;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles;
}
