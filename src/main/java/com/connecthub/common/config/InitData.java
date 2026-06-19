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
import net.datafaker.Faker;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
                    Role.builder().id(RoleName.ROLE_USER.name()).name(RoleName.ROLE_USER).build(),
                    Role.builder().id(RoleName.ROLE_MODERATOR.name()).name(RoleName.ROLE_MODERATOR).build()
            ));
            log.info("✓ Initialized 3 roles");
        }
    }

    private void initUsers() {
        if (userRepository.count() == 0) {
            Role adminRole = roleRepository.findById(RoleName.ROLE_ADMIN.name()).orElseThrow();
            Role userRole = roleRepository.findById(RoleName.ROLE_USER.name()).orElseThrow();
            Role moderatorRole = roleRepository.findById(RoleName.ROLE_MODERATOR.name()).orElseThrow();
            // Khởi tạo List chứa tất cả user sẽ save
            List<User> usersToSave = new ArrayList<>();

            // 1. Tạo tài khoản Admin cố định như cũ
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
                    .roles(new HashSet<>(List.of(adminRole, userRole)))
                    .build();

            usersToSave.add(admin);
            User moderator = User.builder()
                    .id(UuidCreator.getTimeOrderedEpoch())
                    .username("moderator")
                    .email("moderator@connecthub.com")
                    .password(passwordEncoder.encode("moderator123"))
                    .fullName("Moderator User")
                    .phoneNumber("+84901234545")
                    .avatarUrl("https://api.dicebear.com/7.x/avataaars/svg?seed=admin")
                    .bio("I am the moderator of ConnectHub")
                    .status(UserStatus.ACTIVE)
                    .isActive(true)
                    .isLocked(false)
                    .roles(Set.of(moderatorRole, userRole))
                    .build();

            usersToSave.add(moderator);
            // 2. Sử dụng Faker để tự động generate thêm dữ liệu cho các Regular Users
            Faker faker = new Faker(new Locale("en")); // Bạn có thể đổi sang "vi" nếu muốn tên tiếng Việt
            String defaultPassword = passwordEncoder.encode("password123");

            // Thay đổi số 20 thành số lượng user bạn muốn tạo thêm
            for (int i = 0; i < 20; i++) {
                String firstName = faker.name().firstName().toLowerCase();
                String lastName = faker.name().lastName().toLowerCase();
                // Đảm bảo username không trùng bằng cách thêm index hoặc số ngẫu nhiên
                String username = firstName + "_" + lastName + faker.random().nextInt(10, 99);
                String email = username + "@gmail.com";

                // Tạo số điện thoại định dạng Việt Nam ngẫu nhiên
                String phoneNumber = "+849" + faker.number().digits(8);

                // Tạo bio ngẫu nhiên theo nghề nghiệp/sở thích
                String bio = faker.job().title() + " | " + faker.expression("#{regexify '(Tech|Coffee|Travel|Music|Sports)'} Lover");
                User user = User.builder()
                        .id(UuidCreator.getTimeOrderedEpoch())
                        .username(username)
                        .email(email)
                        .password(defaultPassword)
                        .fullName(faker.name().fullName())
                        .phoneNumber(phoneNumber)
                        .avatarUrl("https://api.dicebear.com/7.x/avataaars/svg?seed=" + username)
                        .bio(bio)
                        .status(UserStatus.ACTIVE)
                        .isActive(true)
                        .isLocked(false)
                        .roles(new HashSet<>(Collections.singletonList(userRole)))
                        .build();

                usersToSave.add(user);
            }

            // Lưu toàn bộ list vào database một lần duy nhất để tối ưu hiệu năng
            userRepository.saveAll(usersToSave);
            log.info("✓ Initialized {} users successfully!", usersToSave.size());
        }
    }


    private void initFollows() {
        if (followRepository.count() == 0) {
            List<User> users = userRepository.findAll();

            // Điều kiện cần: Phải có ít nhất 2 user thì mới follow nhau được
            if (users.size() < 2) {
                log.warn("⚠ Không đủ user để khởi tạo dữ liệu Follow");
                return;
            }

            List<Follow> followsToSave = new ArrayList<>();
            Random random = new Random();

            // Sử dụng Set để tracking tránh tạo trùng lặp cặp (followerId + "-" + followingId)
            Set<String> existingPairs = new HashSet<>();

            for (User follower : users) {
                // Mỗi user sẽ follow ngẫu nhiên từ 2 đến 7 user khác (bạn có thể tùy chỉnh số này)
                int numberOfFollowings = random.nextInt(6) + 2;

                // Khống chế không vượt quá tổng số user hiện có
                numberOfFollowings = Math.min(numberOfFollowings, users.size() - 1);

                while (existingPairs.size() < numberOfFollowings) {
                    // Lấy ngẫu nhiên một target user trong danh sách
                    User following = users.get(random.nextInt(users.size()));

                    // Điều kiện: Không tự follow chính mình
                    if (follower.getId().equals(following.getId())) {
                        continue;
                    }

                    String pairKey = follower.getId() + "-" + following.getId();

                    // Nếu cặp này chưa từng được tạo thì thêm vào list
                    if (!existingPairs.contains(pairKey)) {
                        existingPairs.add(pairKey);

                        Follow follow = Follow.builder()
                                .id(UuidCreator.getTimeOrderedEpoch())
                                .follower(follower)
                                .following(following)
                                .build();

                        followsToSave.add(follow);
                    }
                }
                // Clear set của user hiện tại để chuẩn bị vòng lặp cho user tiếp theo
                existingPairs.clear();
            }

            // Lưu toàn bộ các mối quan hệ follow vào DB
            followRepository.saveAll(followsToSave);
            log.info("✓ Initialized {} follow relationships automatically!", followsToSave.size());
        }
    }

    private void initPosts() {
        if (postRepository.count() == 0) {
            List<User> users = userRepository.findAll();
            if (users.isEmpty()) {
                log.warn("⚠ Không tìm thấy user nào trong database để tạo bài viết!");
                return;
            }

            Faker faker = new Faker();
            Random random = new Random();

            List<Post> allPosts = new ArrayList<>();
            List<Media> allMedia = new ArrayList<>();

            String[] hashtags = {"#tech", "#java", "#springboot", "#lifestyle", "#travel", "#photography", "#coffee"};
            String[] sampleImageUrls = {
                    "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d",
                    "https://images.unsplash.com/photo-1614730321146-b6fa6a46bcb4",
                    "https://images.unsplash.com/photo-1498050108023-c5249f4df085",
                    "https://images.unsplash.com/photo-1488590528505-98d2b5aba04b"
            };

            log.info("⏳ Đang khởi tạo bài viết (Đảm bảo mỗi user có ít nhất 3 bài)...");

            // === GIAI ĐOẠN 1: Duyệt qua từng user, tặng sẵn mỗi người 3 bài post ===
            int mediaCounter = 0;
            for (User user : users) {
                for (int k = 0; k < 3; k++) {
                    String content = faker.lorem().sentence(random.nextInt(7) + 5) + " " + hashtags[random.nextInt(hashtags.length)];

                    Post post = Post.builder()
                            .id(UuidCreator.getTimeOrderedEpoch())
                            .user(user)
                            .content(content)
                            .visibility(Visibility.PUBLIC)
                            .isDeleted(false)
                            .build();

                    allPosts.add(post);

                    // Thỉnh thoảng đính kèm ảnh cho bài viết đầu tiên của user
                    if (k == 0 && random.nextBoolean()) {
                        Media media = Media.builder()
                                .id(UuidCreator.getTimeOrderedEpoch())
                                .post(post)
                                .url(sampleImageUrls[random.nextInt(sampleImageUrls.length)] + "?sig=" + (mediaCounter++))
                                .type(MediaType.IMAGE)
                                .size(BigInteger.valueOf(faker.number().numberBetween(500000L, 2500000L)))
                                .build();
                        allMedia.add(media);
                    }
                }
            }

            // === GIAI ĐOẠN 2: Tạo thêm các bài post ngẫu nhiên để đạt đủ số lượng 500 ===
            int totalTargetPosts = 500;
            int remainingPosts = totalTargetPosts - allPosts.size();

            if (remainingPosts > 0) {
                for (int i = 0; i < remainingPosts; i++) {
                    User randomUser = users.get(random.nextInt(users.size()));
                    String content = faker.lorem().sentence(random.nextInt(10) + 5) + " " + hashtags[random.nextInt(hashtags.length)];

                    Post parentPost = null;
                    Post quotePost = null;
                    int postTypeRoll = random.nextInt(100);

                    // Trộn thêm bài Reply và Quote dựa trên các bài viết ĐÃ ĐƯỢC TẠO
                    if (postTypeRoll < 15) {
                        parentPost = allPosts.get(random.nextInt(allPosts.size()));
                        content = "Replying to this: " + faker.yoda().quote();
                    } else if (postTypeRoll < 30) {
                        quotePost = allPosts.get(random.nextInt(allPosts.size()));
                        content = "Totally agree with this! " + faker.buffy().quotes();
                    }

                    Post post = Post.builder()
                            .id(UuidCreator.getTimeOrderedEpoch())
                            .user(randomUser)
                            .content(content)
                            .visibility(Visibility.PUBLIC)
                            .parentPost(parentPost)
                            .quotePost(quotePost)
                            .isDeleted(false)
                            .build();

                    allPosts.add(post);

                    // Thêm media ngẫu nhiên cho bài viết thường ở giai đoạn 2
                    if (parentPost == null && quotePost == null && random.nextInt(100) < 25) {
                        Media media = Media.builder()
                                .id(UuidCreator.getTimeOrderedEpoch())
                                .post(post)
                                .url(sampleImageUrls[random.nextInt(sampleImageUrls.length)] + "?sig=" + (mediaCounter++))
                                .type(MediaType.IMAGE)
                                .size(BigInteger.valueOf(faker.number().numberBetween(500000L, 2500000L)))
                                .build();
                        allMedia.add(media);
                    }
                }
            } else {
                log.warn("⚠ Số lượng user hiện tại quá lớn ({}), Giai đoạn 1 đã tạo ra {} bài viết, vượt chỉ tiêu tổng số {} bài!",
                        users.size(), allPosts.size(), totalTargetPosts);
            }

            // === GIAI ĐOẠN 3: Lưu toàn bộ dữ liệu xuống database ===
            postRepository.saveAll(allPosts);
            mediaRepository.saveAll(allMedia);

            log.info("✓ Khởi tạo thành công tổng cộng {} bài viết (Đã bảo đảm mỗi user có ít nhất 3 bài) và {} tệp media!",
                    allPosts.size(), allMedia.size());
        }
    }

    private void initPostReactions() {
        if (reactionRepository.count() == 0) {
            List<User> users = userRepository.findAll();
            List<Post> posts = postRepository.findAll();

            // Kiểm tra điều kiện cần để thực hiện map dữ liệu
            if (users.isEmpty() || posts.isEmpty()) {
                log.warn("⚠ Không đủ dữ liệu User hoặc Post trong database để khởi tạo Reaction!");
                return;
            }

            Random random = new Random();
            List<Reaction> reactionsToSave = new ArrayList<>();

            // Sử dụng Set để lưu vết cặp (userId + "-" + postId) nhằm chống trùng lặp trong bộ nhớ
            Set<String> uniqueUserPostPairs = new HashSet<>();

            // Lấy toàn bộ danh sách các loại Reaction hiện có (LIKE, LOVE, WOW, HAHA,...)
            ReactionType[] reactionTypes = ReactionType.values();

            // Đặt mục tiêu tạo số lượng reaction mong muốn (ví dụ: 300 tương tác ngẫu nhiên)
            int targetReactionsCount = 300;

            // Tính toán giới hạn vật lý tối đa của database để tránh vòng lặp vô tận
            // (Ví dụ nếu tổng số cặp User x Post thực tế trong DB còn ít hơn target)
            long maxPossibleReactions = (long) users.size() * posts.size();
            int finalCount = (int) Math.min(targetReactionsCount, maxPossibleReactions);

            log.info("⏳ Đang khởi tạo {} tương tác bài viết ngẫu nhiên...", finalCount);

            while (reactionsToSave.size() < finalCount) {
                // 1. Chọn ngẫu nhiên một User và một Post
                User randomUser = users.get(random.nextInt(users.size()));
                Post randomPost = posts.get(random.nextInt(posts.size()));

                // Tạo một key duy nhất đại diện cho mối quan hệ giữa User và Post này
                String pairKey = randomUser.getId() + "-" + randomPost.getId();

                // 2. Nếu User này CHƯA TỪNG tương tác với Post này, tiến hành tạo mới
                if (!uniqueUserPostPairs.contains(pairKey)) {
                    uniqueUserPostPairs.add(pairKey);

                    // Chọn ngẫu nhiên 1 loại reaction trong Enum của bạn
                    ReactionType randomType = reactionTypes[random.nextInt(reactionTypes.length)];

                    Reaction reaction = Reaction.builder()
                            .id(UuidCreator.getTimeOrderedEpoch())
                            .user(randomUser)
                            .post(randomPost)
                            .type(randomType)
                            .build();

                    reactionsToSave.add(reaction);
                }
            }

            // 3. Lưu toàn bộ danh sách xuống DB một lần duy nhất (Batch Insert)
            reactionRepository.saveAll(reactionsToSave);
            log.info("✓ Khởi tạo thành công {} tương tác (Reaction) bài viết!", reactionsToSave.size());
        }
    }
    private void initBookmarks() {
        if (bookmarkRepository.count() == 0) {
            List<User> users = userRepository.findAll();
            List<Post> posts = postRepository.findAll();

            if (users.isEmpty() || posts.isEmpty()) return;

            Random random = new Random();
            List<Bookmark> bookmarksToSave = new ArrayList<>();
            Set<String> uniqueUserPostPairs = new HashSet<>();

            int finalCount = (int) Math.min(150, (long) users.size() * posts.size());

            while (bookmarksToSave.size() < finalCount) {
                User randomUser = users.get(random.nextInt(users.size()));
                Post randomPost = posts.get(random.nextInt(posts.size()));

                String pairKey = randomUser.getId() + "-" + randomPost.getId();

                if (!uniqueUserPostPairs.contains(pairKey)) {
                    uniqueUserPostPairs.add(pairKey);

                    // Dùng hàm có sẵn của JpaRepository để lấy Proxy
                    User userProxy = userRepository.getReferenceById(randomUser.getId());
                    Post postProxy = postRepository.getReferenceById(randomPost.getId());

                    Bookmark bookmark = Bookmark.builder()
                            .id(UuidCreator.getTimeOrderedEpoch())
                            .user(userProxy)
                            .post(postProxy)
                            .build();

                    bookmarksToSave.add(bookmark);
                }
            }
            bookmarkRepository.saveAll(bookmarksToSave);
            log.info("✓ Khởi tạo thành công {} bookmark!", bookmarksToSave.size());
        }
    }

    private void initReposts() {
        if (repostRepository.count() == 0) {
            List<User> users = userRepository.findAll();
            List<Post> posts = postRepository.findAll();

            // Kiểm tra điều kiện cần để thực hiện map dữ liệu
            if (users.isEmpty() || posts.isEmpty()) {
                log.warn("⚠ Không đủ dữ liệu User hoặc Post trong database để khởi tạo Repost!");
                return;
            }

            Random random = new Random();
            List<Repost> repostsToSave = new ArrayList<>();

            // Sử dụng Set để lưu vết cặp (userId + "-" + postId) nhằm chống trùng lặp dữ liệu
            Set<String> uniqueUserPostPairs = new HashSet<>();

            // Đặt mục tiêu tạo 100 bản ghi Repost ngẫu nhiên
            int targetRepostsCount = 100;

            // Tính toán giới hạn vật lý tối đa để tránh vòng lặp vô tận (Infinite Loop)
            long maxPossibleReposts = (long) users.size() * posts.size();
            int finalCount = (int) Math.min(targetRepostsCount, maxPossibleReposts);

            log.info("⏳ Đang khởi tạo {} bản ghi Repost ngẫu nhiên bằng getReferenceById...", finalCount);

            while (repostsToSave.size() < finalCount) {
                // 1. Chọn ngẫu nhiên một User và một Post từ danh sách có sẵn
                User randomUser = users.get(random.nextInt(users.size()));
                Post randomPost = posts.get(random.nextInt(posts.size()));

                String pairKey = randomUser.getId() + "-" + randomPost.getId();

                // 2. Nếu User này chưa từng Repost bài viết này, tiến hành tạo mới
                if (!uniqueUserPostPairs.contains(pairKey)) {
                    uniqueUserPostPairs.add(pairKey);

                    // Tận dụng hàm có sẵn của JpaRepository để tạo Proxy Object siêu nhẹ
                    User userProxy = userRepository.getReferenceById(randomUser.getId());
                    Post postProxy = postRepository.getReferenceById(randomPost.getId());

                    Repost repost = Repost.builder()
                            .id(UuidCreator.getTimeOrderedEpoch())
                            .user(userProxy)
                            .post(postProxy)
                            .build();

                    repostsToSave.add(repost);
                }
            }

            // 3. Lưu toàn bộ danh sách xuống DB một lần duy nhất (Batch Insert)
            repostRepository.saveAll(repostsToSave);
            log.info("✓ Khởi tạo thành công {} dữ liệu chia sẻ lại (Repost)!", repostsToSave.size());
        }
    }


    private void initPostViews() {
        if (postViewRepository.count() == 0) {
            List<User> users = userRepository.findAll();
            List<Post> posts = postRepository.findAll();

            if (posts.isEmpty()) {
                log.warn("⚠ Không tìm thấy bài viết nào trong database để tạo lượt xem!");
                return;
            }

            Faker faker = new Faker();
            Random random = new Random();
            List<PostView> viewsToSave = new ArrayList<>();

            // Đặt mục tiêu tạo 1000 lượt xem giả lập
            int totalViewsTarget = 1000;

            log.info("⏳ Đang khởi tạo {} lượt xem bài viết mô phỏng thực tế...", totalViewsTarget);

            for (int i = 0; i < totalViewsTarget; i++) {
                // 1. Chọn bài viết ngẫu nhiên được xem
                Post randomPost = posts.get(random.nextInt(posts.size()));
                Post postProxy = postRepository.getReferenceById(randomPost.getId());

                // 2. Mô phỏng hành vi người xem:
                // - ~70% là người dùng đã đăng nhập trong hệ thống
                // - ~30% là khách vãng lai xem ẩn danh (user = null)
                User userProxy = null;
                if (!users.isEmpty() && random.nextInt(100) < 70) {
                    User randomUser = users.get(random.nextInt(users.size()));
                    userProxy = userRepository.getReferenceById(randomUser.getId());
                }

                // 3. Giả lập thời gian xem ngẫu nhiên trong vòng 7 ngày qua cho thật dữ liệu
                java.util.Date randomDate = faker.date().past(7, TimeUnit.DAYS);
                LocalDateTime viewedAt = randomDate.toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();

                // 4. Build thực thể PostView
                PostView postView = PostView.builder()
                        .id(UuidCreator.getTimeOrderedEpoch())
                        .post(postProxy)
                        .user(userProxy) // Có thể có hoặc null
                        .viewedAt(viewedAt)
                        .build();

                viewsToSave.add(postView);
            }

            // 5. Lưu toàn bộ xuống DB bằng batch insert
            postViewRepository.saveAll(viewsToSave);
            log.info("✓ Khởi tạo thành công {} lượt xem bài viết (Post Views)!", viewsToSave.size());
        }
    }


    private void initHashtagsAndMentions() {
        if (hashtagRepository.count() == 0) {
            List<Post> posts = postRepository.findAll();
            List<User> users = userRepository.findAll();

            if (posts.isEmpty()) {
                log.warn("⚠ Không tìm thấy bài viết nào để phân tích Hashtag và Mention!");
                return;
            }

            // Map dùng để lưu cache các Hashtag đã tạo nhằm tránh trùng lặp tên Hashtag trong DB
            Map<String, Hashtag> hashtagMap = new HashMap<>();

            // Map dùng để tìm kiếm nhanh User qua username phục vụ việc xử lý @mention
            Map<String, User> userMap = new HashMap<>();
            for (User u : users) {
                userMap.put(u.getUsername().toLowerCase(), u);
            }

            List<PostHashtag> postHashtagsToSave = new ArrayList<>();
            List<Mention> mentionsToSave = new ArrayList<>();

            // Định nghĩa các Regex Pattern để nhận diện #hashtag và @mention từ text
            Pattern hashtagPattern = Pattern.compile("#(\\w+)");
            Pattern mentionPattern = Pattern.compile("@(\\w+)");

            log.info("⏳ Đang quét nội dung {} bài viết để trích xuất Hashtag và Mention thực tế...", posts.size());

            for (Post post : posts) {
                String content = post.getContent();
                if (content == null || content.isEmpty()) continue;

                // Dùng JpaRepositoryProxy cho chính bài viết hiện tại để tối ưu liên kết
                Post postProxy = postRepository.getReferenceById(post.getId());

                // 1. XỬ LÝ TRÍCH XUẤT HASHTAG (#)
                Matcher hashtagMatcher = hashtagPattern.matcher(content);
                Set<String> hashtagsInPost = new HashSet<>(); // Tránh việc 1 bài viết viết 2 chữ #java giống nhau

                while (hashtagMatcher.find()) {
                    String hashtagName = hashtagMatcher.group(1).toLowerCase(); // Lấy phần chữ bỏ dấu #

                    if (!hashtagsInPost.contains(hashtagName)) {
                        hashtagsInPost.add(hashtagName);

                        // Nếu hashtag này chưa từng tồn tại trong DB, tạo mới và save ngay lập tức để lấy ID
                        Hashtag hashtag = hashtagMap.get(hashtagName);
                        if (hashtag == null) {
                            hashtag = Hashtag.builder()
                                    .id(UuidCreator.getTimeOrderedEpoch())
                                    .name(hashtagName)
                                    .build();
                            hashtag = hashtagRepository.save(hashtag); // Lưu ngay để đồng bộ thực thể
                            hashtagMap.put(hashtagName, hashtag);
                        }

                        // Tạo thực thể liên kết nhiều-nhiều (PostHashtag)
                        PostHashtag postHashtag = PostHashtag.builder()
                                .post(postProxy)
                                .hashtag(hashtag)
                                .build();

                        postHashtagsToSave.add(postHashtag);
                    }
                }

                // 2. XỬ LÝ TRÍCH XUẤT MENTION (@)
                Matcher mentionMatcher = mentionPattern.matcher(content);
                Set<String> mentionsInPost = new HashSet<>();

                while (mentionMatcher.find()) {
                    String mentionedUsername = mentionMatcher.group(1).toLowerCase(); // Lấy phần chữ bỏ dấu @

                    if (!mentionsInPost.contains(mentionedUsername)) {
                        mentionsInPost.add(mentionedUsername);

                        // Kiểm tra xem username được tag có thực sự tồn tại trong hệ thống không
                        User mentionedUser = userMap.get(mentionedUsername);
                        if (mentionedUser != null) {
                            User userProxy = userRepository.getReferenceById(mentionedUser.getId());

                            Mention mention = Mention.builder()
                                    .id(UuidCreator.getTimeOrderedEpoch())
                                    .post(postProxy)
                                    .user(userProxy)
                                    .build();

                            mentionsToSave.add(mention);
                        }
                    }
                }
            }

            // 3. Lưu toàn bộ dữ liệu liên kết xuống DB theo Batch
            if (!postHashtagsToSave.isEmpty()) {
                postHashtagRepository.saveAll(postHashtagsToSave);
            }
            if (!mentionsToSave.isEmpty()) {
                mentionRepository.saveAll(mentionsToSave);
            }

            log.info("✓ Trích xuất thành công: Khởi tạo thêm {} bảng ghi Hashtag gốc, {} liên kết bài viết và {} lượt nhắc tên (Mention)!",
                    hashtagMap.size(), postHashtagsToSave.size(), mentionsToSave.size());
        }
    }


    private void initNotifications() {
        if (notificationRepository.count() == 0) {
            List<User> users = userRepository.findAll();
            List<Post> posts = postRepository.findAll();

            if (users.size() < 2) {
                log.warn("⚠ Không đủ user để khởi tạo thông báo (Cần ít nhất 2 user)!");
                return;
            }

            Random random = new Random();
            List<Notification> notificationsToSave = new ArrayList<>();
            NotificationType[] types = NotificationType.values();

            // Đặt mục tiêu tạo 300 thông báo mẫu
            int targetNotificationsCount = 300;

            log.info("⏳ Đang khởi tạo {} thông báo ngẫu nhiên mô phỏng thực tế...", targetNotificationsCount);

            for (int i = 0; i < targetNotificationsCount; i++) {
                // 1. Chọn ngẫu nhiên loại thông báo từ Enum của bạn
                NotificationType randomType = types[random.nextInt(types.length)];

                User recipientUser = null;
                User actorUser = null;
                Post postProxy = null;
                String content = "";

                // 2. Tách logic xử lý theo loại thông báo để sinh content và binding chính xác
                if (randomType == NotificationType.FOLLOW || posts.isEmpty()) {
                    // Nhóm 1: Thông báo FOLLOW (Không cần Post)
                    recipientUser = users.get(random.nextInt(users.size()));

                    // Tìm actor khác với recipient
                    do {
                        actorUser = users.get(random.nextInt(users.size()));
                    } while (actorUser.getId().equals(recipientUser.getId()));

                    content = actorUser.getFullName() + " started following you";
                    // Ép type về FOLLOW phòng trường hợp posts rỗng nhảy vào đây
                    randomType = NotificationType.FOLLOW;
                } else {
                    // Nhóm 2: Thông báo tương tác Bài viết (LIKE, COMMENT/REPLY, QUOTE, MENTION...)
                    Post randomPost = posts.get(random.nextInt(posts.size()));
                    postProxy = postRepository.getReferenceById(randomPost.getId());

                    // Người nhận thông báo chính là chủ nhân của bài viết đó
                    recipientUser = randomPost.getUser();

                    // Người thực hiện hành động (actor) phải là một người khác
                    do {
                        actorUser = users.get(random.nextInt(users.size()));
                    } while (actorUser.getId().equals(recipientUser.getId()));

                    // Tạo nội dung thông báo tương ứng với loại tương tác
                    switch (randomType) {
                        case LIKE -> content = actorUser.getFullName() + " liked your post";
                        case FOLLOW -> content = actorUser.getFullName() + " started following you"; // Backup
                        default -> content = actorUser.getFullName() + " interacted with your post";
                        // Bạn có thể thêm case REPLY, QUOTE... tùy thuộc vào các giá trị có trong NotificationType enum của bạn
                    }
                }

                // Tạo các Proxy Object siêu nhẹ để map mối quan hệ
                User recipientProxy = userRepository.getReferenceById(recipientUser.getId());
                User actorProxy = userRepository.getReferenceById(actorUser.getId());

                // 3. Build thực thể Notification
                Notification notification = Notification.builder()
                        .id(UuidCreator.getTimeOrderedEpoch())
                        .recipient(recipientProxy)
                        .actor(actorProxy)
                        .post(postProxy) // Sẽ nhận proxy post hoặc null tùy loại thông báo
                        .content(content)
                        .type(randomType)
                        .isRead(random.nextBoolean()) // Trộn ngẫu nhiên thông báo đã đọc (true) và chưa đọc (false)
                        .build();

                notificationsToSave.add(notification);
            }

            // 4. Lưu toàn bộ danh sách xuống DB
            notificationRepository.saveAll(notificationsToSave);
            log.info("✓ Khởi tạo thành công {} thông báo (Notifications) thành công!", notificationsToSave.size());
        }
    }

    private void initConversations() {
        if (conversationRepository.count() == 0) {
            List<User> users = userRepository.findAll();

            if (users.size() < 2) {
                log.warn("⚠ Không đủ user để khởi tạo hội thoại chat (Cần ít nhất 2 user)!");
                return;
            }

            Faker faker = new Faker();
            Random random = new Random();

            List<ConversationMember> allMembersToSave = new ArrayList<>();
            List<Message> allMessagesToSave = new ArrayList<>();
            List<MessageReceipt> allReceiptsToSave = new ArrayList<>();

            // Định nghĩa số lượng cuộc hội thoại Chat 1-1 muốn tạo
            int targetConversationsCount = 50;

            // Dùng Set để tránh tạo trùng phòng chat PRIVATE giữa cùng 1 cặp user trong lượt chạy này
            Set<String> uniqueChatPairs = new HashSet<>();

            log.info("⏳ Đang khởi tạo {} cuộc hội thoại với hàng trăm tin nhắn mô phỏng...", targetConversationsCount);

            for (int i = 0; i < targetConversationsCount; i++) {
                // 1. Chọn ngẫu nhiên cặp User tham gia chat 1-1
                User userA = users.get(random.nextInt(users.size()));
                User userB;
                do {
                    userB = users.get(random.nextInt(users.size()));
                } while (userA.getId().equals(userB.getId()));

                // Sắp xếp ID để tạo key duy nhất cho cặp này (Tránh việc A-B và B-A thành 2 phòng PRIVATE)
                String pairKey = userA.getId().compareTo(userB.getId()) < 0
                        ? userA.getId() + "-" + userB.getId()
                        : userB.getId() + "-" + userA.getId();

                if (uniqueChatPairs.contains(pairKey)) {
                    continue; // Cặp này đã có phòng chat rồi, bỏ qua để tìm cặp khác
                }
                uniqueChatPairs.add(pairKey);

                // 2. Tạo thực thể cuộc hội thoại (Conversation)
                Conversation conversation = Conversation.builder()
                        .id(UuidCreator.getTimeOrderedEpoch())
                        .type(ConversationType.PRIVATE)
                        .build();
                conversation = conversationRepository.save(conversation); // Lưu ngay để lấy Context liên kết

                Conversation conversationProxy = conversationRepository.getReferenceById(conversation.getId());
                User userAProxy = userRepository.getReferenceById(userA.getId());
                User userBProxy = userRepository.getReferenceById(userB.getId());

                // 3. Thêm 2 thành viên vào cuộc trò chuyện (ConversationMember)
                LocalDateTime joinTime = LocalDateTime.now().minusDays(10);
                ConversationMember memberA = ConversationMember.builder()
                        .conversation(conversationProxy)
                        .user(userAProxy)
                        .joinedAt(joinTime)
                        .build();
                ConversationMember memberB = ConversationMember.builder()
                        .conversation(conversationProxy)
                        .user(userBProxy)
                        .joinedAt(joinTime)
                        .build();

                allMembersToSave.add(memberA);
                allMembersToSave.add(memberB);

                // 4. Giả lập từ 10 đến 20 tin nhắn nhắn qua lại trong phòng chat này
                int messageCount = random.nextInt(11) + 10;
                List<User> chatParticipants = Arrays.asList(userA, userB);

                LocalDateTime msgTime = joinTime;

                for (int m = 0; m < messageCount; m++) {
                    // Tăng thời gian tiến dần về hiện tại để tin nhắn có trình tự trước/sau
                    msgTime = msgTime.plusMinutes(random.nextInt(30) + 5);
                    if (msgTime.isAfter(LocalDateTime.now())) {
                        msgTime = LocalDateTime.now();
                    }

                    // Chọn ngẫu nhiên 1 trong 2 người là người gửi (Sender)
                    User sender = chatParticipants.get(random.nextInt(2));
                    User recipient = sender.getId().equals(userA.getId()) ? userB : userA;

                    User senderProxy = userRepository.getReferenceById(sender.getId());
                    User recipientProxy = userRepository.getReferenceById(recipient.getId());

                    // Sinh câu chat ngẫu nhiên sinh động từ Faker
                    String chatContent = faker.lorem().sentence();
                    if (random.nextInt(100) < 20) {
                        chatContent = faker.backToTheFuture().quote(); //  Chuyển thành 'quote()' số ít
                    }

                    Message message = Message.builder()
                            .id(UuidCreator.getTimeOrderedEpoch())
                            .conversation(conversationProxy)
                            .sender(senderProxy)
                            .content(chatContent)
                            .build();

                    allMessagesToSave.add(message);

                    // 5. Tạo biên nhận trạng thái đọc tin nhắn (MessageReceipt) cho người nhận
                    // Giả lập: 90% các tin nhắn cũ đã được đọc, các tin mới nhất có thể chưa đọc (SENT)
                    MessageStatus status = (m < messageCount - 1 || random.nextBoolean())
                            ? MessageStatus.READ
                            : MessageStatus.SENT;

                    LocalDateTime readTime = status == MessageStatus.READ
                            ? msgTime.plusSeconds(random.nextInt(120) + 5)
                            : null;

                    MessageReceipt receipt = MessageReceipt.builder()
                            .id(UuidCreator.getTimeOrderedEpoch())
                            .message(message) // Liên kết trực tiếp thực thể trong bộ nhớ
                            .user(recipientProxy) // Người nhận là người gửi biên nhận READ
                            .status(status)
                            .readAt(readTime)
                            .build();

                    allReceiptsToSave.add(receipt);
                }
            }

            // 6. Lưu toàn bộ dữ liệu bắc cầu xuống DB bằng Batch Insert
            conversationMemberRepository.saveAll(allMembersToSave);
            messageRepository.saveAll(allMessagesToSave);
            messageReceiptRepository.saveAll(allReceiptsToSave);

            log.info("✓ Khởi tạo thành công: {} phòng chat, {} thành viên, {} tin nhắn và {} biên nhận đọc!",
                    uniqueChatPairs.size(), allMembersToSave.size(), allMessagesToSave.size(), allReceiptsToSave.size());
        }
    }

    private void initUserBlocks() {
        if (userBlockRepository.count() == 0) {
            List<User> users = userRepository.findAll();

            if (users.size() < 2) {
                log.warn("⚠ Không đủ user để khởi tạo dữ liệu chặn (Cần ít nhất 2 user)!");
                return;
            }

            Random random = new Random();
            List<UserBlock> blocksToSave = new ArrayList<>();

            // Sử dụng Set để lưu vết cặp (blockerId + "-" + blockedId) nhằm chống trùng lặp dữ liệu
            Set<String> uniqueBlockPairs = new HashSet<>();

            int targetBlocksCount = 30;
            long maxPossibleBlocks = (long) users.size() * (users.size() - 1);
            int finalCount = (int) Math.min(targetBlocksCount, maxPossibleBlocks);

            log.info("⏳ Đang khởi tạo {} lượt chặn người dùng ngẫu nhiên...", finalCount);

            while (blocksToSave.size() < finalCount) {
                User blocker = users.get(random.nextInt(users.size()));
                User blocked;

                // Đảm bảo không tự chặn chính mình
                do {
                    blocked = users.get(random.nextInt(users.size()));
                } while (blocker.getId().equals(blocked.getId()));

                String pairKey = blocker.getId() + "-" + blocked.getId();

                if (!uniqueBlockPairs.contains(pairKey)) {
                    uniqueBlockPairs.add(pairKey);

                    // Sử dụng Proxy Object siêu nhẹ để map mối quan hệ
                    User blockerProxy = userRepository.getReferenceById(blocker.getId());
                    User blockedProxy = userRepository.getReferenceById(blocked.getId());

                    UserBlock userBlock = UserBlock.builder()
                            .id(UuidCreator.getTimeOrderedEpoch())
                            .blocker(blockerProxy)
                            .blocked(blockedProxy)
                            .build();

                    blocksToSave.add(userBlock);
                }
            }

            userBlockRepository.saveAll(blocksToSave);
            log.info("✓ Khởi tạo thành công {} dữ liệu chặn (User Blocks)!", blocksToSave.size());
        }
    }


    private void initReports() {
        if (reportRepository.count() == 0) {
            List<User> users = userRepository.findAll();
            List<Post> posts = postRepository.findAll();

            if (users.size() < 2) {
                log.warn("⚠ Không đủ user để khởi tạo dữ liệu báo cáo!");
                return;
            }

            Random random = new Random();
            List<Report> reportsToSave = new ArrayList<>();

            // Danh sách lý do báo cáo thực tế trên các mạng xã hội
            String[] reportReasons = {
                    "Inappropriate content",
                    "Spam or misleading",
                    "Harassment or hate speech",
                    "Violence or dangerous organizations",
                    "Intellectual property violation"
            };

            ReportStatus[] statuses = ReportStatus.values();
            int targetReportsCount = 40;

            log.info("⏳ Đang khởi tạo {} báo cáo bài viết/người dùng ngẫu nhiên...", targetReportsCount);

            for (int i = 0; i < targetReportsCount; i++) {
                User reporter = users.get(random.nextInt(users.size()));
                User targetUser = null;
                Post postProxy = null;

                // Giả lập mô phỏng:
                // - 70% báo cáo tập trung vào Bài viết rác (Post Report)
                // - 30% báo cáo trực tiếp tài khoản người dùng phá hoại (User Report)
                if (!posts.isEmpty() && random.nextInt(100) < 70) {
                    Post randomPost = posts.get(random.nextInt(posts.size()));
                    postProxy = postRepository.getReferenceById(randomPost.getId());

                    // Đối tượng bị báo cáo (targetUser) chính là chủ nhân bài viết đó
                    targetUser = randomPost.getUser();
                } else {
                    // Báo cáo tài khoản cá nhân trực tiếp
                    do {
                        targetUser = users.get(random.nextInt(users.size()));
                    } while (targetUser.getId().equals(reporter.getId()));
                }

                // Đảm bảo người đi báo cáo không phải là người bị báo cáo (phòng trường hợp post report)
                if (reporter.getId().equals(targetUser.getId())) {
                    // Nếu trùng, đổi người báo cáo sang một user ngẫu nhiên khác
                    do {
                        reporter = users.get(random.nextInt(users.size()));
                    } while (reporter.getId().equals(targetUser.getId()));
                }

                User reporterProxy = userRepository.getReferenceById(reporter.getId());
                User targetUserProxy = userRepository.getReferenceById(targetUser.getId());

                String randomReason = reportReasons[random.nextInt(reportReasons.length)];
                ReportStatus randomStatus = statuses[random.nextInt(statuses.length)];

                Report report = Report.builder()
                        .id(UuidCreator.getTimeOrderedEpoch())
                        .reporter(reporterProxy)
                        .targetUser(targetUserProxy)
                        .post(postProxy) // Nhận proxy bài viết hoặc null nếu là báo cáo user
                        .reason(randomReason)
                        .status(randomStatus)
                        .build();

                reportsToSave.add(report);
            }

            reportRepository.saveAll(reportsToSave);
            log.info("✓ Khởi tạo thành công {} dữ liệu báo cáo (Reports)!", reportsToSave.size());
        }
    }

    private void initBans() {
        if (banRepository.count() == 0) {
            List<User> users = userRepository.findAll();

            if (users.size() < 2) {
                log.warn("⚠ Không đủ user để khởi tạo dữ liệu khóa tài khoản (Ban)!");
                return;
            }

            Random random = new Random();
            List<Ban> bansToSave = new ArrayList<>();

            // Sử dụng Set để đảm bảo mỗi user chỉ bị bốc trúng khóa 1 lần trong dữ liệu mẫu
            Set<UUID> bannedUserIds = new HashSet<>(); // Đổi UUID thành Long/String nếu cần

            // Danh sách lý do phạt thực tế
            String[] banReasons = {
                    "Violating community guidelines",
                    "Spamming behavior detected",
                    "Using abusive language or hate speech",
                    "Multiple reports from other users",
                    "Suspicious login activity"
            };

            // Mục tiêu tạo 15 bản ghi Ban ngẫu nhiên
            int targetBansCount = 15;
            // Giới hạn an toàn dựa trên tổng số user hiện có (trừ đi 1 người làm admin)
            int finalCount = Math.min(targetBansCount, users.size() - 1);

            log.info("⏳ Đang khởi tạo {} lệnh khóa tài khoản (Ban) mô phỏng...", finalCount);

            // Giả định chọn cố định User đầu tiên (Index 0) làm Admin thực hiện lệnh Ban để dữ liệu nhất quán
            User admin = users.get(0);
            User adminProxy = userRepository.getReferenceById(admin.getId());

            while (bansToSave.size() < finalCount) {
                // Lấy ngẫu nhiên một user bị phạt (bắt đầu chọn từ Index 1 để tránh trùng với Admin)
                User bannedUser = users.get(random.nextInt(users.size() - 1) + 1);

                // Kiểm tra xem User này đã nằm trong danh sách phạt của lượt sinh dữ liệu này chưa
                if (!bannedUserIds.contains(bannedUser.getId())) {
                    bannedUserIds.add(bannedUser.getId());

                    User bannedUserProxy = userRepository.getReferenceById(bannedUser.getId());
                    String randomReason = banReasons[random.nextInt(banReasons.length)];

                    // Thiết lập ngày bắt đầu phạt ngẫu nhiên từ vài ngày trước cho đến hôm nay
                    LocalDateTime startDate = LocalDateTime.now().minusDays(random.nextInt(5));
                    LocalDateTime endDate = null;

                    // Quy định thời hạn khóa:
                    // - 80% khóa có thời hạn (3, 7, 30 ngày)
                    // - 20% khóa vĩnh viễn (endDate = null)
                    int durationRoll = random.nextInt(100);
                    if (durationRoll < 30) {
                        endDate = startDate.plusDays(3); // Khóa 3 ngày
                    } else if (durationRoll < 60) {
                        endDate = startDate.plusDays(7); // Khóa 7 ngày
                    } else if (durationRoll < 80) {
                        endDate = startDate.plusDays(30); // Khóa 30 ngày
                    }

                    Ban ban = Ban.builder()
                            .id(UuidCreator.getTimeOrderedEpoch())
                            .user(bannedUserProxy)
                            .bannedBy(adminProxy)
                            .reason(randomReason)
                            .startDate(startDate)
                            .endDate(endDate)
                            .build();

                    bansToSave.add(ban);
                }
            }

            // Lưu toàn bộ danh sách lệnh phạt xuống DB một lần duy nhất
            banRepository.saveAll(bansToSave);
            log.info("✓ Khởi tạo thành công {} lệnh khóa tài khoản (Ban)!", bansToSave.size());
        }
    }


    private void initFeedItems() {
        if (feedItemRepository.count() == 0) {
            List<User> users = userRepository.findAll();
            List<Post> posts = postRepository.findAll();

            if (users.isEmpty() || posts.isEmpty()) {
                log.warn("⚠ Không đủ dữ liệu User hoặc Post trong database để khởi tạo Feed Items!");
                return;
            }

            Random random = new Random();
            List<FeedItem> feedItemsToSave = new ArrayList<>();

            log.info("⏳ Đang khởi tạo danh sách News Feed mẫu cho từng User...");

            // Duyệt qua TỪNG NGƯỜI DÙNG để xây dựng bảng Feed riêng cho họ
            for (User viewer : users) {
                User viewerProxy = userRepository.getReferenceById(viewer.getId());

                // Set dùng để chặn việc cùng 1 bài viết xuất hiện 2 lần trên Feed của 1 người
                Set<UUID> postsInFeed = new HashSet<>(); // Đổi UUID thành Long/String nếu cần

                // Quy định: Mỗi người dùng sẽ có ngẫu nhiên từ 15 đến 25 bài viết hiển thị trên Feed
                int feedSize = random.nextInt(11) + 15;
                // Giới hạn an toàn phòng trường hợp tổng số post trong DB ít hơn feedSize mong muốn
                int actualFeedSize = Math.min(feedSize, posts.size());

                while (postsInFeed.size() < actualFeedSize) {
                    // Lấy một bài viết ngẫu nhiên bất kỳ trong hệ thống
                    Post randomPost = posts.get(random.nextInt(posts.size()));

                    if (!postsInFeed.contains(randomPost.getId())) {
                        postsInFeed.add(randomPost.getId());

                        Post postProxy = postRepository.getReferenceById(randomPost.getId());

                        // Giả lập điểm số thuật toán EdgeRank / AI Recommendation (Score chạy từ 0.10 đến 0.99)
                        // Bài viết càng phân phối về sau điểm sẽ càng thấp dần theo logic thực tế
                        double score = 0.1 + (0.99 - 0.1) * random.nextDouble();

                        // Làm tròn lấy 2 chữ số thập phân cho đẹp dữ liệu (ví dụ: 0.85)
                        score = Math.round(score * 100.0) / 100.0;

                        FeedItem feedItem = FeedItem.builder()
                                .id(UuidCreator.getTimeOrderedEpoch())
                                .user(viewerProxy) // Người sở hữu dòng thời gian này (Viewer)
                                .post(postProxy)   // Bài viết được phân phối tới Feed
                                .score(score)      // Điểm gợi ý hiển thị
                                .build();

                        feedItemsToSave.add(feedItem);
                    }
                }
            }

            // Lưu toàn bộ danh sách xuống DB một lần duy nhất bằng Batch Insert
            feedItemRepository.saveAll(feedItemsToSave);
            log.info("✓ Khởi tạo thành công tổng cộng {} bản ghi Feed Item cho toàn bộ hệ thống User!", feedItemsToSave.size());
        }
    }
}
