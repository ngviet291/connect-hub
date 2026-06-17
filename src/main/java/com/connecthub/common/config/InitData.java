package com.connecthub.common.config;

import com.connecthub.modules.features.chat.entity.Conversation;
import com.connecthub.modules.features.chat.entity.ConversationMember;
import com.connecthub.modules.features.chat.entity.Message;
import com.connecthub.modules.features.chat.entity.MessageReceipt;
import com.connecthub.modules.features.chat.enums.ConversationType;
import com.connecthub.modules.features.chat.enums.MessageStatus;
import com.connecthub.modules.features.chat.repository.ConversationMemberRepository;
import com.connecthub.modules.features.chat.repository.ConversationRepository;
import com.connecthub.modules.features.chat.repository.MessageReceiptRepository;
import com.connecthub.modules.features.chat.repository.MessageRepository;
import com.connecthub.modules.features.moderation.entity.Ban;
import com.connecthub.modules.features.moderation.entity.Report;
import com.connecthub.modules.features.moderation.enums.ReportStatus;
import com.connecthub.modules.features.moderation.repository.BanRepository;
import com.connecthub.modules.features.moderation.repository.ReportRepository;
import com.connecthub.modules.features.notification.entity.Notification;
import com.connecthub.modules.features.notification.enums.NotificationType;
import com.connecthub.modules.features.notification.repository.NotificationRepository;
import com.connecthub.modules.features.post.entity.*;
import com.connecthub.modules.features.post.enums.MediaType;
import com.connecthub.modules.features.post.enums.ReactionType;
import com.connecthub.modules.features.post.enums.Visibility;
import com.connecthub.modules.features.post.repository.*;
import com.connecthub.modules.features.social.entity.FeedItem;
import com.connecthub.modules.features.social.entity.Follow;
import com.connecthub.modules.features.social.repository.FeedItemRepository;
import com.connecthub.modules.features.social.repository.FollowRepository;
import com.connecthub.modules.features.user.entity.Role;
import com.connecthub.modules.features.user.entity.User;
import com.connecthub.modules.features.user.entity.UserBlock;
import com.connecthub.modules.features.user.enums.RoleName;
import com.connecthub.modules.features.user.enums.UserStatus;
import com.connecthub.modules.features.user.repository.RoleRepository;
import com.connecthub.modules.features.user.repository.UserBlockRepository;
import com.connecthub.modules.features.user.repository.UserRepository;
import com.github.f4b6a3.uuid.UuidCreator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Profile("dev")
@RequiredArgsConstructor
@Component
public class InitData implements CommandLineRunner {
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final PostRepository postRepository;
    private final ReactionRepository reactionRepository;
    private final BookmarkRepository bookmarkRepository;
    private final RepostRepository repostRepository;
    private final PostViewRepository postViewRepository;
    private final PostHashtagRepository postHashtagRepository;
    private final HashtagRepository hashtagRepository;
    private final MentionRepository mentionRepository;
    private final NotificationRepository notificationRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final MessageRepository messageRepository;
    private final MessageReceiptRepository messageReceiptRepository;
    private final UserBlockRepository userBlockRepository;
    private final ReportRepository reportRepository;
    private final BanRepository banRepository;
    private final FeedItemRepository feedItemRepository;
    private final PasswordEncoder passwordEncoder;
    private final MediaRepository mediaRepository;

    @Override
    public void run(String... args) throws Exception {
        initRoles();
        initUsers();
        initFollows();
        initPosts();
        initPostReactions();
        initBookmarks();
        initReposts();
        initPostViews();
        initHashtagsAndMentions();
        initNotifications();
        initConversations();
        initUserBlocks();
        initReports();
        initBans();
        initFeedItems();
    }

    private void initRoles() {
        if (roleRepository.count() == 0) {
            roleRepository.saveAll(List.of(
                    Role.builder().id(RoleName.ROLE_ADMIN.name()).name(RoleName.ROLE_ADMIN).build(),
                    Role.builder().id(RoleName.ROLE_USER.name()).name(RoleName.ROLE_USER).build()
            ));
            log.info("✓ Initialized 2 roles");
        }
    }

    private void initUsers() {
        if (userRepository.count() == 0) {
            Role adminRole = roleRepository.findById(RoleName.ROLE_ADMIN.name()).orElseThrow();
            Role userRole = roleRepository.findById(RoleName.ROLE_USER.name()).orElseThrow();

            User admin = User.builder()
                    .id(UuidCreator.getTimeOrderedEpoch())
                    .username("admin")
                    .email("admin@connecthub.com")
                    .password(passwordEncoder.encode("admin123"))
                    .fullName("Admin User")
                    .phoneNumber("+84901234567")
                    .avatarUrl("https://api.dicebear.com/7.x/avataaars/svg?seed=admin")
                    .bio("I am the administrator of ConnectHub")
                    .status(UserStatus.ACTIVE)
                    .isActive(true)
                    .isLocked(false)
                    .roles(new HashSet<>(Collections.singletonList(adminRole)))
                    .build();

            User user1 = User.builder()
                    .id(UuidCreator.getTimeOrderedEpoch())
                    .username("john_doe")
                    .email("john.doe@gmail.com")
                    .password(passwordEncoder.encode("password123"))
                    .fullName("John Doe")
                    .phoneNumber("+84912345678")
                    .avatarUrl("https://api.dicebear.com/7.x/avataaars/svg?seed=john")
                    .bio("Software Developer | Tech Enthusiast")
                    .status(UserStatus.ACTIVE)
                    .isActive(true)
                    .isLocked(false)
                    .roles(new HashSet<>(Collections.singletonList(userRole)))
                    .build();

            User user2 = User.builder()
                    .id(UuidCreator.getTimeOrderedEpoch())
                    .username("jane_smith")
                    .email("jane.smith@gmail.com")
                    .password(passwordEncoder.encode("password123"))
                    .fullName("Jane Smith")
                    .phoneNumber("+84923456789")
                    .avatarUrl("https://api.dicebear.com/7.x/avataaars/svg?seed=jane")
                    .bio("UI/UX Designer | Coffee Lover")
                    .status(UserStatus.ACTIVE)
                    .isActive(true)
                    .isLocked(false)
                    .roles(new HashSet<>(Collections.singletonList(userRole)))
                    .build();

            User user3 = User.builder()
                    .id(UuidCreator.getTimeOrderedEpoch())
                    .username("mike_wilson")
                    .email("mike.wilson@gmail.com")
                    .password(passwordEncoder.encode("password123"))
                    .fullName("Mike Wilson")
                    .phoneNumber("+84934567890")
                    .avatarUrl("https://api.dicebear.com/7.x/avataaars/svg?seed=mike")
                    .bio("Project Manager | Startup Founder")
                    .status(UserStatus.ACTIVE)
                    .isActive(true)
                    .isLocked(false)
                    .roles(new HashSet<>(Collections.singletonList(userRole)))
                    .build();

            User user4 = User.builder()
                    .id(UuidCreator.getTimeOrderedEpoch())
                    .username("sarah_johnson")
                    .email("sarah.johnson@gmail.com")
                    .password(passwordEncoder.encode("password123"))
                    .fullName("Sarah Johnson")
                    .phoneNumber("+84945678901")
                    .avatarUrl("https://api.dicebear.com/7.x/avataaars/svg?seed=sarah")
                    .bio("Content Creator | Digital Marketer")
                    .status(UserStatus.ACTIVE)
                    .isActive(true)
                    .isLocked(false)
                    .roles(new HashSet<>(Collections.singletonList(userRole)))
                    .build();

            User user5 = User.builder()
                    .id(UuidCreator.getTimeOrderedEpoch())
                    .username("bob_brown")
                    .email("bob.brown@gmail.com")
                    .password(passwordEncoder.encode("password123"))
                    .fullName("Bob Brown")
                    .phoneNumber("+84956789012")
                    .avatarUrl("https://api.dicebear.com/7.x/avataaars/svg?seed=bob")
                    .bio("Data Scientist | ML Enthusiast")
                    .status(UserStatus.ACTIVE)
                    .isActive(true)
                    .isLocked(false)
                    .roles(new HashSet<>(Collections.singletonList(userRole)))
                    .build();

            userRepository.saveAll(Arrays.asList(admin, user1, user2, user3, user4, user5));
            log.info("✓ Initialized 6 users");
        }
    }

    private void initFollows() {
        if (followRepository.count() == 0) {
            List<User> users = userRepository.findAll();
            Collections.shuffle(users);

            if (users.size() >= 6) {
                User user1 = users.get(0);
                User user2 = users.get(1);
                User user3 = users.get(2);
                User user4 = users.get(3);
                User user5 = users.get(4);
                User user6 = users.get(5);

                List<Follow> follows = Arrays.asList(
                        Follow.builder().id(UuidCreator.getTimeOrderedEpoch()).follower(user1).following(user2).build(),
                        Follow.builder().id(UuidCreator.getTimeOrderedEpoch()).follower(user1).following(user3).build(),
                        Follow.builder().id(UuidCreator.getTimeOrderedEpoch()).follower(user2).following(user1).build(),
                        Follow.builder().id(UuidCreator.getTimeOrderedEpoch()).follower(user2).following(user4).build(),
                        Follow.builder().id(UuidCreator.getTimeOrderedEpoch()).follower(user3).following(user5).build(),
                        Follow.builder().id(UuidCreator.getTimeOrderedEpoch()).follower(user4).following(user1).build(),
                        Follow.builder().id(UuidCreator.getTimeOrderedEpoch()).follower(user5).following(user2).build(),
                        Follow.builder().id(UuidCreator.getTimeOrderedEpoch()).follower(user6).following(user1).build()
                );
                followRepository.saveAll(follows);
                log.info("✓ Initialized 8 follow relationships");
            }
        }
    }

    private void initPosts() {
        if (postRepository.count() == 0) {
            List<User> users = userRepository.findAll();

            if (users.size() >= 3) {
                User user1 = users.get(0);
                User user2 = users.get(1);
                User user3 = users.get(2);

                // Regular posts
                Post post1 = Post.builder()
                        .id(UuidCreator.getTimeOrderedEpoch())
                        .user(user1)
                        .content("Just started learning Spring Boot! 🚀 #java #backend #learning")
                        .visibility(Visibility.PUBLIC)
                        .isDeleted(false)
                        .build();

                Post post2 = Post.builder()
                        .id(UuidCreator.getTimeOrderedEpoch())
                        .user(user2)
                        .content("Beautiful sunset at the beach today 🌅 #nature #photography")
                        .visibility(Visibility.PUBLIC)
                        .isDeleted(false)
                        .build();

                Post post3 = Post.builder()
                        .id(UuidCreator.getTimeOrderedEpoch())
                        .user(user3)
                        .content("Excited to announce our new product launch next week! 🎉")
                        .visibility(Visibility.PUBLIC)
                        .isDeleted(false)
                        .build();

                // Reply post (parent)
                Post reply1 = Post.builder()
                        .id(UuidCreator.getTimeOrderedEpoch())
                        .user(user2)
                        .content("That's awesome! I'm also learning Spring Boot. Let's connect!")
                        .visibility(Visibility.PUBLIC)
                        .parentPost(post1)
                        .isDeleted(false)
                        .build();

                // Quote post
                Post quotePost1 = Post.builder()
                        .id(UuidCreator.getTimeOrderedEpoch())
                        .user(user1)
                        .content("Check out what Sarah did with her design skills!")
                        .visibility(Visibility.PUBLIC)
                        .quotePost(post2)
                        .isDeleted(false)
                        .build();

                List<Post> posts = Arrays.asList(post1, post2, post3, reply1, quotePost1);
                postRepository.saveAll(posts);

                // Add media to posts
                Media media1 = Media.builder()
                        .id(UuidCreator.getTimeOrderedEpoch())
                        .post(post2)
                        .url("https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d")
                        .type(MediaType.IMAGE)
                        .size(BigInteger.valueOf(1024000L))
                        .build();

                Media media2 = Media.builder()
                        .id(UuidCreator.getTimeOrderedEpoch())
                        .post(post3)
                        .url("https://images.unsplash.com/photo-1614730321146-b6fa6a46bcb4")
                        .type(MediaType.IMAGE)
                        .size(BigInteger.valueOf(2048000L))
                        .build();

                mediaRepository.saveAll(Arrays.asList(media1, media2));

                log.info("✓ Initialized 5 posts with 2 media items");
            }
        }
    }

    private void initPostReactions() {
        if (reactionRepository.count() == 0) {
            List<User> users = userRepository.findAll();
            List<Post> posts = postRepository.findAll();

            if (users.size() >= 3 && posts.size() >= 3) {
                User user1 = users.get(0);
                User user2 = users.get(1);
                User user3 = users.get(2);

                Post post1 = posts.get(0);
                Post post2 = posts.get(1);
                Post post3 = posts.get(2);

                List<Reaction> reactions = Arrays.asList(
                        Reaction.builder()
                                .id(UuidCreator.getTimeOrderedEpoch())
                                .user(user2)
                                .post(post1)
                                .type(ReactionType.LIKE)
                                .build(),
                        Reaction.builder()
                                .id(UuidCreator.getTimeOrderedEpoch())
                                .user(user3)
                                .post(post1)
                                .type(ReactionType.LOVE)
                                .build(),
                        Reaction.builder()
                                .id(UuidCreator.getTimeOrderedEpoch())
                                .user(user1)
                                .post(post2)
                                .type(ReactionType.WOW)
                                .build(),
                        Reaction.builder()
                                .id(UuidCreator.getTimeOrderedEpoch())
                                .user(user1)
                                .post(post3)
                                .type(ReactionType.LIKE)
                                .build()
                );
                reactionRepository.saveAll(reactions);
                log.info("✓ Initialized 4 reactions");
            }
        }
    }

    private void initBookmarks() {
        if (bookmarkRepository.count() == 0) {
            List<User> users = userRepository.findAll();
            List<Post> posts = postRepository.findAll();

            if (users.size() >= 2 && posts.size() >= 2) {
                User user1 = users.get(0);
                User user2 = users.get(1);

                Post post1 = posts.get(0);
                Post post2 = posts.get(1);

                List<Bookmark> bookmarks = Arrays.asList(
                        Bookmark.builder()
                                .id(UuidCreator.getTimeOrderedEpoch())
                                .user(user1)
                                .post(post1)
                                .build(),
                        Bookmark.builder()
                                .id(UuidCreator.getTimeOrderedEpoch())
                                .user(user2)
                                .post(post2)
                                .build()
                );
                bookmarkRepository.saveAll(bookmarks);
                log.info("✓ Initialized 2 bookmarks");
            }
        }
    }

    private void initReposts() {
        if (repostRepository.count() == 0) {
            List<User> users = userRepository.findAll();
            List<Post> posts = postRepository.findAll();

            if (users.size() >= 2 && posts.size() >= 2) {
                User user1 = users.get(0);
                User user3 = users.get(2);

                Post post2 = posts.stream().skip(1).findFirst().orElse(null);
                Post post3 = posts.stream().skip(2).findFirst().orElse(null);

                if (post2 != null && post3 != null) {
                    List<Repost> reposts = Arrays.asList(
                            Repost.builder()
                                    .id(UuidCreator.getTimeOrderedEpoch())
                                    .user(user1)
                                    .post(post2)
                                    .build(),
                            Repost.builder()
                                    .id(UuidCreator.getTimeOrderedEpoch())
                                    .user(user3)
                                    .post(post3)
                                    .build()
                    );
                    repostRepository.saveAll(reposts);
                    log.info("✓ Initialized 2 reposts");
                }
            }
        }
    }

    private void initPostViews() {
        if (postViewRepository.count() == 0) {
            List<User> users = userRepository.findAll();
            List<Post> posts = postRepository.findAll();

            if (users.size() >= 3 && posts.size() >= 2) {
                User user1 = users.get(0);
                User user2 = users.get(1);
                User user3 = users.get(2);

                Post post1 = posts.get(0);
                Post post2 = posts.get(1);

                List<PostView> views = Arrays.asList(
                        PostView.builder()
                                .id(UuidCreator.getTimeOrderedEpoch())
                                .post(post1)
                                .user(user2)
                                .viewedAt(LocalDateTime.now().minusHours(2))
                                .build(),
                        PostView.builder()
                                .id(UuidCreator.getTimeOrderedEpoch())
                                .post(post1)
                                .user(user3)
                                .viewedAt(LocalDateTime.now().minusHours(1))
                                .build(),
                        PostView.builder()
                                .id(UuidCreator.getTimeOrderedEpoch())
                                .post(post2)
                                .user(user1)
                                .viewedAt(LocalDateTime.now())
                                .build()
                );
                postViewRepository.saveAll(views);
                log.info("✓ Initialized 3 post views");
            }
        }
    }

    private void initHashtagsAndMentions() {
        if (hashtagRepository.count() == 0) {
            List<Hashtag> hashtags = Arrays.asList(
                    Hashtag.builder().id(UuidCreator.getTimeOrderedEpoch()).name("java").build(),
                    Hashtag.builder().id(UuidCreator.getTimeOrderedEpoch()).name("backend").build(),
                    Hashtag.builder().id(UuidCreator.getTimeOrderedEpoch()).name("learning").build(),
                    Hashtag.builder().id(UuidCreator.getTimeOrderedEpoch()).name("nature").build(),
                    Hashtag.builder().id(UuidCreator.getTimeOrderedEpoch()).name("photography").build()
            );
            hashtagRepository.saveAll(hashtags);

            // Add post hashtags
            List<Post> posts = postRepository.findAll();
            List<Hashtag> savedHashtags = hashtagRepository.findAll();

            if (posts.size() >= 1 && savedHashtags.size() >= 3) {
                Post post1 = posts.get(0);
                List<PostHashtag> postHashtags = Arrays.asList(
                        PostHashtag.builder()
                                .post(post1)
                                .hashtag(savedHashtags.get(0))
                                .build(),
                        PostHashtag.builder()
                                .post(post1)
                                .hashtag(savedHashtags.get(1))
                                .build()
                );
                postHashtagRepository.saveAll(postHashtags);
            }

            // Add mentions
            List<User> users = userRepository.findAll();
            if (posts.size() >= 2 && users.size() >= 2) {
                Post post1 = posts.get(0);
                User user2 = users.get(1);

                Mention mention = Mention.builder()
                        .id(UuidCreator.getTimeOrderedEpoch())
                        .post(post1)
                        .user(user2)
                        .build();
                mentionRepository.save(mention);
            }

            log.info("✓ Initialized 5 hashtags, post-hashtags, and mentions");
        }
    }

    private void initNotifications() {
        if (notificationRepository.count() == 0) {
            List<User> users = userRepository.findAll();
            List<Post> posts = postRepository.findAll();

            if (users.size() >= 2 && posts.size() >= 1) {
                User recipient = users.get(0);
                User actor = users.get(1);
                Post post = posts.get(0);

                List<Notification> notifications = Arrays.asList(
                        Notification.builder()
                                .id(UuidCreator.getTimeOrderedEpoch())
                                .recipient(recipient)
                                .actor(actor)
                                .post(post)
                                .content(actor.getFullName() + " liked your post")
                                .type(NotificationType.LIKE)
                                .isRead(false)
                                .build(),
                        Notification.builder()
                                .id(UuidCreator.getTimeOrderedEpoch())
                                .recipient(recipient)
                                .actor(actor)
                                .content(actor.getFullName() + " started following you")
                                .type(NotificationType.FOLLOW)
                                .isRead(false)
                                .build()
                );
                notificationRepository.saveAll(notifications);
                log.info("✓ Initialized 2 notifications");
            }
        }
    }

    private void initConversations() {
        if (conversationRepository.count() == 0) {
            List<User> users = userRepository.findAll();

            if (users.size() >= 3) {
                User user1 = users.get(0);
                User user2 = users.get(1);
                User user3 = users.get(2);

                // Private conversation between user1 and user2
                Conversation privateConv = Conversation.builder()
                        .id(UuidCreator.getTimeOrderedEpoch())
                        .type(ConversationType.PRIVATE)
                        .build();
                conversationRepository.save(privateConv);

                // Add conversation members
                ConversationMember member1 = ConversationMember.builder()
                        .conversation(privateConv)
                        .user(user1)
                        .joinedAt(LocalDateTime.now().minusDays(7))
                        .build();
                ConversationMember member2 = ConversationMember.builder()
                        .conversation(privateConv)
                        .user(user2)
                        .joinedAt(LocalDateTime.now().minusDays(7))
                        .build();
                conversationMemberRepository.saveAll(Arrays.asList(member1, member2));

                // Add messages
                Message msg1 = Message.builder()
                        .id(UuidCreator.getTimeOrderedEpoch())
                        .conversation(privateConv)
                        .sender(user1)
                        .content("Hey! How are you doing?")
                        .build();
                Message msg2 = Message.builder()
                        .id(UuidCreator.getTimeOrderedEpoch())
                        .conversation(privateConv)
                        .sender(user2)
                        .content("I'm doing great! How about you?")
                        .build();
                messageRepository.saveAll(Arrays.asList(msg1, msg2));

                // Add message receipts
                MessageReceipt receipt1 = MessageReceipt.builder()
                        .id(UuidCreator.getTimeOrderedEpoch())
                        .message(msg1)
                        .user(user2)
                        .status(MessageStatus.READ)
                        .readAt(LocalDateTime.now().minusHours(1))
                        .build();
                MessageReceipt receipt2 = MessageReceipt.builder()
                        .id(UuidCreator.getTimeOrderedEpoch())
                        .message(msg2)
                        .user(user1)
                        .status(MessageStatus.READ)
                        .readAt(LocalDateTime.now())
                        .build();
                messageReceiptRepository.saveAll(Arrays.asList(receipt1, receipt2));

                log.info("✓ Initialized 1 conversation with 2 messages and receipts");
            }
        }
    }

    private void initUserBlocks() {
        if (userBlockRepository.count() == 0) {
            List<User> users = userRepository.findAll();

            if (users.size() >= 3) {
                User user1 = users.get(0);
                User user5 = users.get(4);

                UserBlock block = UserBlock.builder()
                        .id(UuidCreator.getTimeOrderedEpoch())
                        .blocker(user1)
                        .blocked(user5)
                        .build();
                userBlockRepository.save(block);

                log.info("✓ Initialized 1 user block");
            }
        }
    }

    private void initReports() {
        if (reportRepository.count() == 0) {
            List<User> users = userRepository.findAll();
            List<Post> posts = postRepository.findAll();

            if (users.size() >= 3 && posts.size() >= 1) {
                User reporter = users.get(0);
                User targetUser = users.get(2);
                Post post = posts.get(0);

                Report report = Report.builder()
                        .id(UuidCreator.getTimeOrderedEpoch())
                        .reporter(reporter)
                        .targetUser(targetUser)
                        .post(post)
                        .reason("Inappropriate content")
                        .status(ReportStatus.PENDING)
                        .build();
                reportRepository.save(report);

                log.info("✓ Initialized 1 report");
            }
        }
    }

    private void initBans() {
        if (banRepository.count() == 0) {
            List<User> users = userRepository.findAll();

            if (users.size() >= 3) {
                User bannedUser = users.get(4);
                User admin = users.get(0);

                Ban ban = Ban.builder()
                        .id(UuidCreator.getTimeOrderedEpoch())
                        .user(bannedUser)
                        .bannedBy(admin)
                        .reason("Spamming behavior")
                        .startDate(LocalDateTime.now())
                        .endDate(LocalDateTime.now().plusDays(30))
                        .build();
                banRepository.save(ban);

                log.info("✓ Initialized 1 ban");
            }
        }
    }

    private void initFeedItems() {
        if (feedItemRepository.count() == 0) {
            List<User> users = userRepository.findAll();
            List<Post> posts = postRepository.findAll();

            if (users.size() >= 2 && posts.size() >= 2) {
                User user1 = users.get(0);
                Post post1 = posts.get(0);
                Post post2 = posts.get(1);

                List<FeedItem> feedItems = Arrays.asList(
                        FeedItem.builder()
                                .id(UuidCreator.getTimeOrderedEpoch())
                                .post(post1)
                                .user(user1)
                                .score(0.95)
                                .build(),
                        FeedItem.builder()
                                .id(UuidCreator.getTimeOrderedEpoch())
                                .post(post2)
                                .user(user1)
                                .score(0.85)
                                .build()
                );
                feedItemRepository.saveAll(feedItems);

                log.info("✓ Initialized 2 feed items");
            }
        }
    }
}
