package com.connecthub.entity;

import com.connecthub.enums.UserStatus;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.*;

import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity

public class User extends BaseEntity {
    @Id
    private UUID id = UuidCreator.getTimeOrderedEpoch();
    private String username;
    private String email;
    private String passwordHash;
    private String fullName;
    private String avatarUrl;
    private String bio;
    @Enumerated(jakarta.persistence.EnumType.STRING)
    private UserStatus status;
    @OneToMany(mappedBy = "user")
    @ToString.Exclude
    private Set<UserRole> userRoles;
    @OneToMany(mappedBy = "follower")
    @ToString.Exclude
    private Set<Follow> followers;
    @OneToMany(mappedBy = "following")
    @ToString.Exclude
    private Set<Follow> following;
    @OneToMany(mappedBy = "blocker")
    @ToString.Exclude
    private Set<UserBlock> blocker;
    @OneToMany(mappedBy = "blocked")
    @ToString.Exclude
    private Set<UserBlock> blocked;
    @OneToMany(mappedBy = "user")
    @ToString.Exclude
    private Set<Post> posts;
    @OneToMany(mappedBy = "user")
    @ToString.Exclude
    private Set<Reaction> reactions;
    @OneToMany(mappedBy = "user")
    @ToString.Exclude
    private Set<Repost> reposts;
    @OneToMany(mappedBy = "user")
    @ToString.Exclude
    private Set<PostView> postViews;
    @OneToMany(mappedBy = "user")
    @ToString.Exclude
    private Set<Bookmark> bookmarks;
    @OneToMany(mappedBy = "user")
    @ToString.Exclude
    private Set<FeedItem> feedItems;
    @OneToMany(mappedBy = "user")
    @ToString.Exclude
    private Set<Mention> mentions;
    @OneToMany(mappedBy = "recipient")
    @ToString.Exclude
    private Set<Notification> notifications;
    @OneToMany(mappedBy = "actor")
    @ToString.Exclude
    private Set<Notification> actedNotifications;

    @OneToMany(mappedBy = "user")
    @ToString.Exclude
    private Set<ConversationMember> conversationMembers;

    @OneToMany(mappedBy = "sender")
    @ToString.Exclude
    private Set<Message> messages;

    @OneToMany(mappedBy = "user")
    @ToString.Exclude
    private Set<MessageReceipt> messageReceipts;
    @OneToMany(mappedBy = "reporter")
    @ToString.Exclude
    private Set<Report> reporter;
    @OneToMany(mappedBy = "targetUser")
    @ToString.Exclude
    private Set<Report> reported;

    @OneToMany(mappedBy = "user")
    @ToString.Exclude
    private Set<Ban> target;
    @OneToMany(mappedBy = "bannedBy")
    @ToString.Exclude
    private Set<Ban> admin;
}
