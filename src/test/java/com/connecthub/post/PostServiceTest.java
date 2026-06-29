package com.connecthub.post;

import com.connecthub.common.dto.response.CursorResponse;
import com.connecthub.common.util.AppUtil;
import com.connecthub.modules.features.post.dto.request.PostRequest;
import com.connecthub.modules.features.post.dto.request.UpdatePostRequest;
import com.connecthub.modules.features.post.dto.response.PostResponse;
import com.connecthub.modules.features.post.entity.*;
import com.connecthub.modules.features.post.enums.Visibility;
import com.connecthub.modules.features.post.exception.HashtagNotFoundException;
import com.connecthub.modules.features.post.exception.MentionedUserNotFoundException;
import com.connecthub.modules.features.post.exception.PostAccessDeniedException;
import com.connecthub.modules.features.post.exception.PostNotFoundException;
import com.connecthub.modules.features.post.mapper.PostMapper;
import com.connecthub.modules.features.post.repository.*;
import com.connecthub.modules.features.post.service.MediaService;
import com.connecthub.modules.features.post.service.PostService;
import com.connecthub.modules.features.user.entity.User;
import com.connecthub.modules.features.user.exception.UserNotFoundException;
import com.connecthub.modules.features.user.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock private PostRepository postRepository;
    @Mock private UserRepository userRepository;
    @Mock private HashtagRepository hashtagRepository;
    @Mock private PostHashtagRepository postHashtagRepository;
    @Mock private MentionRepository mentionRepository;
    @Mock private MediaService mediaService;
    @Mock private PostMapper postMapper;

    @InjectMocks
    private PostService postService;

    private MockedStatic<AppUtil> mockedAppUtil;
    private final UUID MOCK_USER_ID = UUID.fromString("019ed9d6-65e9-7267-b396-7ac0ad80ded8");

    @BeforeEach
    void setUp() {
        mockedAppUtil = Mockito.mockStatic(AppUtil.class);
        mockedAppUtil.when(AppUtil::userIdFromAuthentication).thenReturn(MOCK_USER_ID);
        mockedAppUtil.when(AppUtil::generateUUID).thenReturn(UUID.randomUUID());
    }

    @AfterEach
    void tearDown() {
        mockedAppUtil.close();
    }

    private void stubMapToResponse() {
        when(postMapper.mapToResponse(any(Post.class))).thenReturn(new PostResponse());
    }

    private Post activePostOwnedByCurrentUser(UUID postId) {
        User owner = User.builder().id(MOCK_USER_ID).build();
        return Post.builder()
                .id(postId)
                .user(owner)
                .content("Original content")
                .visibility(Visibility.PUBLIC)
                .postHashtags(new HashSet<>())
                .mentions(new HashSet<>())
                .build();
    }

    // =====================================================================
    // CREATE POST
    // =====================================================================
    @Nested
    @DisplayName("createPost()")
    class CreatePostTest {

        @Test
        @DisplayName("Thành công - Bài viết thường không có hashtag/mention/media")
        void createPost_BasicPost_Success() {
            User user = User.builder().id(MOCK_USER_ID).build();
            PostRequest request = PostRequest.builder().content("Hello World").build();
            Post mockPost = new Post();
            Post savedPost = Post.builder().id(UUID.randomUUID()).user(user).content("Hello World").build();

            when(userRepository.findById(MOCK_USER_ID)).thenReturn(Optional.of(user));
            when(postMapper.toPost(request)).thenReturn(mockPost);
            when(postRepository.save(any(Post.class))).thenReturn(savedPost);
            stubMapToResponse();

            PostResponse result = postService.createPost(request);

            assertNotNull(result);
            verify(postRepository).save(any(Post.class));
            verify(postMapper).mapToResponse(savedPost);
            verifyNoInteractions(mediaService, hashtagRepository, mentionRepository);
        }

        @Test
        @DisplayName("Thành công - Bài viết có parentPostId")
        void createPost_WithParentPost_Success() {
            User user = User.builder().id(MOCK_USER_ID).build();
            UUID parentId = UUID.randomUUID();
            Post parentPost = Post.builder().id(parentId).build();
            PostRequest request = PostRequest.builder()
                    .content("This is a reply")
                    .parentPostId(parentId)
                    .build();
            Post mockPost = new Post();
            Post savedPost = Post.builder().id(UUID.randomUUID()).user(user).parentPost(parentPost).build();

            when(userRepository.findById(MOCK_USER_ID)).thenReturn(Optional.of(user));
            when(postMapper.toPost(request)).thenReturn(mockPost);
            when(postRepository.findById(parentId)).thenReturn(Optional.of(parentPost));
            when(postRepository.save(any(Post.class))).thenReturn(savedPost);
            stubMapToResponse();

            PostResponse result = postService.createPost(request);

            assertNotNull(result);
            verify(postRepository).findById(parentId);
        }

        @Test
        @DisplayName("Thất bại - Parent Post không tồn tại")
        void createPost_ParentPostNotFound_ThrowsPostNotFoundException() {
            User user = User.builder().id(MOCK_USER_ID).build();
            UUID fakeParentId = UUID.randomUUID();
            PostRequest request = PostRequest.builder().parentPostId(fakeParentId).build();

            when(userRepository.findById(MOCK_USER_ID)).thenReturn(Optional.of(user));
            when(postMapper.toPost(request)).thenReturn(new Post());
            when(postRepository.findById(fakeParentId)).thenReturn(Optional.empty());

            assertThrows(PostNotFoundException.class, () -> postService.createPost(request));
            verify(postRepository, never()).save(any());
        }

        @Test
        @DisplayName("Thành công - Bài viết có quotePostId")
        void createPost_WithQuotePost_Success() {
            User user = User.builder().id(MOCK_USER_ID).build();
            UUID quoteId = UUID.randomUUID();
            Post quotePost = Post.builder().id(quoteId).build();
            PostRequest request = PostRequest.builder()
                    .content("Check this out!")
                    .quotePostId(quoteId)
                    .build();
            Post mockPost = new Post();
            Post savedPost = Post.builder().id(UUID.randomUUID()).user(user).quotePost(quotePost).build();

            when(userRepository.findById(MOCK_USER_ID)).thenReturn(Optional.of(user));
            when(postMapper.toPost(request)).thenReturn(mockPost);
            when(postRepository.findById(quoteId)).thenReturn(Optional.of(quotePost));
            when(postRepository.save(any(Post.class))).thenReturn(savedPost);
            stubMapToResponse();

            assertNotNull(postService.createPost(request));
            verify(postRepository).findById(quoteId);
        }

        @Test
        @DisplayName("Thất bại - Quote Post không tồn tại")
        void createPost_QuotePostNotFound_ThrowsPostNotFoundException() {
            User user = User.builder().id(MOCK_USER_ID).build();
            UUID fakeQuoteId = UUID.randomUUID();
            PostRequest request = PostRequest.builder().quotePostId(fakeQuoteId).build();

            when(userRepository.findById(MOCK_USER_ID)).thenReturn(Optional.of(user));
            when(postMapper.toPost(request)).thenReturn(new Post());
            when(postRepository.findById(fakeQuoteId)).thenReturn(Optional.empty());

            assertThrows(PostNotFoundException.class, () -> postService.createPost(request));
        }

        @Test
        @DisplayName("Thành công - Bài viết có hashtags mới → batch insert")
        void createPost_WithNewHashtags_BatchInserts() {
            User user = User.builder().id(MOCK_USER_ID).build();
            UUID postId = UUID.randomUUID();
            Post savedPost = Post.builder().id(postId).user(user)
                    .postHashtags(new HashSet<>()).build();
            PostRequest request = PostRequest.builder()
                    .content("Hello #java")
                    .hashtags(List.of("java"))
                    .build();
            Hashtag newHashtag = Hashtag.builder().id(UUID.randomUUID()).name("java").build();

            when(userRepository.findById(MOCK_USER_ID)).thenReturn(Optional.of(user));
            when(postMapper.toPost(request)).thenReturn(new Post());
            when(postRepository.save(any())).thenReturn(savedPost);
            when(hashtagRepository.findAllByNameIn(List.of("java"))).thenReturn(List.of());
            when(hashtagRepository.saveAll(anyList())).thenReturn(List.of(newHashtag));
            when(postHashtagRepository.findHashtagIdsByPostId(postId)).thenReturn(new HashSet<>());
            when(postHashtagRepository.saveAll(anyList())).thenReturn(List.of());
            stubMapToResponse();

            postService.createPost(request);

            verify(hashtagRepository).findAllByNameIn(List.of("java"));
            verify(hashtagRepository).saveAll(anyList());
            verify(postHashtagRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("Thành công - Hashtag đã tồn tại → không insert hashtag mới")
        void createPost_WithExistingHashtag_OnlyLinks() {
            User user = User.builder().id(MOCK_USER_ID).build();
            UUID postId = UUID.randomUUID();
            Post savedPost = Post.builder().id(postId).user(user)
                    .postHashtags(new HashSet<>()).build();
            Hashtag existing = Hashtag.builder().id(UUID.randomUUID()).name("spring").build();
            PostRequest request = PostRequest.builder()
                    .content("Hello #spring")
                    .hashtags(List.of("spring"))
                    .build();

            when(userRepository.findById(MOCK_USER_ID)).thenReturn(Optional.of(user));
            when(postMapper.toPost(request)).thenReturn(new Post());
            when(postRepository.save(any())).thenReturn(savedPost);
            when(hashtagRepository.findAllByNameIn(List.of("spring"))).thenReturn(List.of(existing));
            when(postHashtagRepository.findHashtagIdsByPostId(postId)).thenReturn(new HashSet<>());
            when(postHashtagRepository.saveAll(anyList())).thenReturn(List.of());
            stubMapToResponse();

            postService.createPost(request);

            verify(hashtagRepository, never()).saveAll(anyList()); // không tạo hashtag mới
            verify(postHashtagRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("Thành công - Hashtag đã liên kết → bỏ qua, không insert trùng")
        void createPost_HashtagAlreadyLinked_SkipsDuplicate() {
            User user = User.builder().id(MOCK_USER_ID).build();
            UUID postId = UUID.randomUUID();
            Hashtag existing = Hashtag.builder().id(UUID.randomUUID()).name("java").build();
            Post savedPost = Post.builder().id(postId).user(user)
                    .postHashtags(new HashSet<>()).build();
            PostRequest request = PostRequest.builder()
                    .content("Hello #java")
                    .hashtags(List.of("java"))
                    .build();

            when(userRepository.findById(MOCK_USER_ID)).thenReturn(Optional.of(user));
            when(postMapper.toPost(request)).thenReturn(new Post());
            when(postRepository.save(any())).thenReturn(savedPost);
            when(hashtagRepository.findAllByNameIn(List.of("java"))).thenReturn(List.of(existing));
            // PostHashtag đã tồn tại
            when(postHashtagRepository.findHashtagIdsByPostId(postId))
                    .thenReturn(new HashSet<>(Set.of(existing.getId())));
            when(postHashtagRepository.saveAll(anyList())).thenReturn(List.of());
            stubMapToResponse();

            postService.createPost(request);

            // saveAll với list rỗng vì đã filter hết
            verify(postHashtagRepository).saveAll(argThat(list -> ((List<?>) list).isEmpty()));
        }

        @Test
        @DisplayName("Thành công - Bài viết có mentions → batch insert")
        void createPost_WithMentions_BatchInserts() {
            User user = User.builder().id(MOCK_USER_ID).build();
            UUID postId = UUID.randomUUID();
            Post savedPost = Post.builder().id(postId).user(user)
                    .mentions(new HashSet<>()).build();
            User mentioned = User.builder().id(UUID.randomUUID()).username("john_doe").build();
            PostRequest request = PostRequest.builder()
                    .content("Hello @john_doe")
                    .mentionUsernames(List.of("john_doe"))
                    .build();

            when(userRepository.findById(MOCK_USER_ID)).thenReturn(Optional.of(user));
            when(postMapper.toPost(request)).thenReturn(new Post());
            when(postRepository.save(any())).thenReturn(savedPost);
            when(userRepository.findAllByUsernameIn(List.of("john_doe")))
                    .thenReturn(List.of(mentioned));
            when(mentionRepository.saveAll(anyList())).thenReturn(List.of());
            stubMapToResponse();

            postService.createPost(request);

            verify(userRepository).findAllByUsernameIn(List.of("john_doe"));
            verify(mentionRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("Thất bại - Mention username không tồn tại")
        void createPost_MentionedUserNotFound_ThrowsMentionedUserNotFoundException() {
            User user = User.builder().id(MOCK_USER_ID).build();
            UUID postId = UUID.randomUUID();
            Post savedPost = Post.builder().id(postId).user(user)
                    .mentions(new HashSet<>()).build();
            PostRequest request = PostRequest.builder()
                    .content("Hello @ghost")
                    .mentionUsernames(List.of("ghost"))
                    .build();

            when(userRepository.findById(MOCK_USER_ID)).thenReturn(Optional.of(user));
            when(postMapper.toPost(request)).thenReturn(new Post());
            when(postRepository.save(any())).thenReturn(savedPost);
            // findAllByUsernameIn trả về empty → ghost không tồn tại
            when(userRepository.findAllByUsernameIn(List.of("ghost"))).thenReturn(List.of());

            assertThrows(MentionedUserNotFoundException.class, () -> postService.createPost(request));
        }

        @Test
        @DisplayName("Thành công - Bài viết có file media")
        void createPost_WithMedia_UploadsAndAttaches() {
            User user = User.builder().id(MOCK_USER_ID).build();
            MultipartFile mockFile = mock(MultipartFile.class);
            Post savedPost = Post.builder().id(UUID.randomUUID()).user(user)
                    .media(new HashSet<>()).build();
            PostRequest request = PostRequest.builder()
                    .content("Post with image")
                    .files(List.of(mockFile))
                    .build();
            Media uploadedMedia = Media.builder().id(UUID.randomUUID()).build();

            when(userRepository.findById(MOCK_USER_ID)).thenReturn(Optional.of(user));
            when(postMapper.toPost(request)).thenReturn(new Post());
            when(postRepository.save(any())).thenReturn(savedPost);
            when(mediaService.uploadAndAttachToPost(eq(List.of(mockFile)), any()))
                    .thenReturn(List.of(uploadedMedia));
            stubMapToResponse();

            postService.createPost(request);

            verify(mediaService).uploadAndAttachToPost(eq(List.of(mockFile)), any());
            assertTrue(savedPost.getMedia().contains(uploadedMedia));
        }

        @Test
        @DisplayName("Thất bại - User không tồn tại")
        void createPost_UserNotFound_ThrowsUserNotFoundException() {
            when(userRepository.findById(MOCK_USER_ID)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class,
                    () -> postService.createPost(new PostRequest()));
            verify(postMapper, never()).toPost(any());
            verify(postRepository, never()).save(any());
        }
    }

    // =====================================================================
    // GET POST
    // =====================================================================
    @Nested
    @DisplayName("getPost()")
    class GetPostTest {

        @Test
        @DisplayName("Thành công - Lấy bài viết đang hoạt động")
        void getPost_ActivePost_Success() {
            UUID postId = UUID.randomUUID();
            Post post = Post.builder().id(postId).build();

            when(postRepository.findByIdWithDetails(postId)).thenReturn(Optional.of(post));
            stubMapToResponse();

            assertNotNull(postService.getPost(postId));
            verify(postMapper).mapToResponse(post);
        }

        @Test
        @DisplayName("Thất bại - ID không tồn tại")
        void getPost_PostNotFound_ThrowsPostNotFoundException() {
            UUID postId = UUID.randomUUID();
            when(postRepository.findByIdWithDetails(postId)).thenReturn(Optional.empty());

            assertThrows(PostNotFoundException.class, () -> postService.getPost(postId));
        }
    }

    // =====================================================================
    // UPDATE POST
    // =====================================================================
    @Nested
    @DisplayName("updatePost()")
    class UpdatePostTest {

        @Test
        @DisplayName("Thành công - Cập nhật content và visibility")
        void updatePost_ContentAndVisibility_Success() {
            UUID postId = UUID.randomUUID();
            Post post = activePostOwnedByCurrentUser(postId);
            UpdatePostRequest request = UpdatePostRequest.builder()
                    .content("Updated content")
                    .visibility(Visibility.PRIVATE)
                    .build();

            // Service dùng findByIdAndUserIdWithDetails
            when(postRepository.findByIdAndUserIdWithDetails(postId, MOCK_USER_ID))
                    .thenReturn(Optional.of(post));
            when(postRepository.save(any())).thenReturn(post);
            stubMapToResponse();

            PostResponse result = postService.updatePost(postId, request);

            assertNotNull(result);
            assertEquals("Updated content", post.getContent());
            assertEquals(Visibility.PRIVATE, post.getVisibility());
        }

        @Test
        @DisplayName("Thành công - Null visibility → giữ nguyên visibility cũ")
        void updatePost_NullVisibility_Unchanged() {
            UUID postId = UUID.randomUUID();
            Post post = activePostOwnedByCurrentUser(postId);
            post.setVisibility(Visibility.FOLLOWERS);
            UpdatePostRequest request = UpdatePostRequest.builder()
                    .content("Only content")
                    .visibility(null)
                    .build();

            when(postRepository.findByIdAndUserIdWithDetails(postId, MOCK_USER_ID))
                    .thenReturn(Optional.of(post));
            when(postRepository.save(any())).thenReturn(post);
            stubMapToResponse();

            postService.updatePost(postId, request);

            assertEquals(Visibility.FOLLOWERS, post.getVisibility());
        }

        @Test
        @DisplayName("Thành công - Cập nhật hashtags → xóa cũ, batch insert mới")
        void updatePost_ReplaceHashtags_DeletesOldAndBatchInserts() {
            UUID postId = UUID.randomUUID();
            Post post = activePostOwnedByCurrentUser(postId);
            Hashtag newHashtag = Hashtag.builder().id(UUID.randomUUID()).name("newtag").build();
            UpdatePostRequest request = UpdatePostRequest.builder()
                    .hashtags(List.of("newtag"))
                    .build();

            when(postRepository.findByIdAndUserIdWithDetails(postId, MOCK_USER_ID))
                    .thenReturn(Optional.of(post));
            when(postRepository.save(any())).thenReturn(post);
            when(hashtagRepository.findAllByNameIn(List.of("newtag"))).thenReturn(List.of(newHashtag));
            when(postHashtagRepository.findHashtagIdsByPostId(postId)).thenReturn(new HashSet<>());
            when(postHashtagRepository.saveAll(anyList())).thenReturn(List.of());
            stubMapToResponse();

            postService.updatePost(postId, request);

            verify(postHashtagRepository).deleteByPostId(postId);
            verify(postHashtagRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("Thành công - Hashtags rỗng [] → xóa toàn bộ, không insert mới")
        void updatePost_EmptyHashtags_DeletesAll() {
            UUID postId = UUID.randomUUID();
            Post post = activePostOwnedByCurrentUser(postId);
            UpdatePostRequest request = UpdatePostRequest.builder()
                    .hashtags(Collections.emptyList())
                    .build();

            when(postRepository.findByIdAndUserIdWithDetails(postId, MOCK_USER_ID))
                    .thenReturn(Optional.of(post));
            when(postRepository.save(any())).thenReturn(post);
            stubMapToResponse();

            postService.updatePost(postId, request);

            verify(postHashtagRepository).deleteByPostId(postId);
            verify(postHashtagRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("Thành công - Null hashtags → giữ nguyên")
        void updatePost_NullHashtags_KeepsExisting() {
            UUID postId = UUID.randomUUID();
            Post post = activePostOwnedByCurrentUser(postId);
            UpdatePostRequest request = UpdatePostRequest.builder()
                    .hashtags(null)
                    .build();

            when(postRepository.findByIdAndUserIdWithDetails(postId, MOCK_USER_ID))
                    .thenReturn(Optional.of(post));
            when(postRepository.save(any())).thenReturn(post);
            stubMapToResponse();

            postService.updatePost(postId, request);

            verify(postHashtagRepository, never()).deleteByPostId(any());
        }

        @Test
        @DisplayName("Thành công - Cập nhật mentions → xóa cũ, batch insert mới")
        void updatePost_ReplaceMentions_DeletesOldAndBatchInserts() {
            UUID postId = UUID.randomUUID();
            Post post = activePostOwnedByCurrentUser(postId);
            User mentioned = User.builder().id(UUID.randomUUID()).username("alice").build();
            UpdatePostRequest request = UpdatePostRequest.builder()
                    .mentionUsernames(List.of("alice"))
                    .build();

            when(postRepository.findByIdAndUserIdWithDetails(postId, MOCK_USER_ID))
                    .thenReturn(Optional.of(post));
            when(postRepository.save(any())).thenReturn(post);
            when(userRepository.findAllByUsernameIn(List.of("alice"))).thenReturn(List.of(mentioned));
            when(mentionRepository.saveAll(anyList())).thenReturn(List.of());
            stubMapToResponse();

            postService.updatePost(postId, request);

            verify(mentionRepository).deleteByPostId(postId);
            verify(mentionRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("Thành công - Mentions rỗng [] → xóa toàn bộ")
        void updatePost_EmptyMentions_DeletesAll() {
            UUID postId = UUID.randomUUID();
            Post post = activePostOwnedByCurrentUser(postId);
            UpdatePostRequest request = UpdatePostRequest.builder()
                    .mentionUsernames(Collections.emptyList())
                    .build();

            when(postRepository.findByIdAndUserIdWithDetails(postId, MOCK_USER_ID))
                    .thenReturn(Optional.of(post));
            when(postRepository.save(any())).thenReturn(post);
            stubMapToResponse();

            postService.updatePost(postId, request);

            verify(mentionRepository).deleteByPostId(postId);
            verify(mentionRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("Thất bại - Bài viết không tồn tại hoặc không phải chủ")
        void updatePost_NotFoundOrNotOwner_ThrowsPostAccessDeniedException() {
            UUID postId = UUID.randomUUID();
            // findByIdAndUserIdWithDetails trả empty = không tồn tại HOẶC không phải chủ
            when(postRepository.findByIdAndUserIdWithDetails(postId, MOCK_USER_ID))
                    .thenReturn(Optional.empty());

            assertThrows(PostAccessDeniedException.class,
                    () -> postService.updatePost(postId, new UpdatePostRequest()));
            verify(postRepository, never()).save(any());
        }

        @Test
        @DisplayName("Thất bại - Mention username không tồn tại")
        void updatePost_MentionedUserNotFound_Throws() {
            UUID postId = UUID.randomUUID();
            Post post = activePostOwnedByCurrentUser(postId);
            UpdatePostRequest request = UpdatePostRequest.builder()
                    .mentionUsernames(List.of("ghost"))
                    .build();

            when(postRepository.findByIdAndUserIdWithDetails(postId, MOCK_USER_ID))
                    .thenReturn(Optional.of(post));
            when(userRepository.findAllByUsernameIn(List.of("ghost"))).thenReturn(List.of());

            assertThrows(MentionedUserNotFoundException.class,
                    () -> postService.updatePost(postId, request));
        }
    }

    // =====================================================================
    // DELETE POST
    // =====================================================================
    @Nested
    @DisplayName("deletePost()")
    class DeletePostTest {

        @Test
        @DisplayName("Thành công - Xóa mềm bài viết")
        void deletePost_OwnPost_SetsDeletedTrue() {
            UUID postId = UUID.randomUUID();
            User user = User.builder().id(MOCK_USER_ID).build();
            Post post = Post.builder().id(postId).user(user).build();

            // Service dùng findByIdAndUserId
            when(postRepository.findByIdAndUserId(postId, MOCK_USER_ID))
                    .thenReturn(Optional.of(post));
            when(postRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            postService.deletePost(postId);

            assertTrue(post.isDeleted());
            verify(postRepository).save(post);
        }

        @Test
        @DisplayName("Thành công - Xóa reply → decrement commentCount của parent")
        void deletePost_Reply_DecrementsParentCommentCount() {
            UUID postId = UUID.randomUUID();
            UUID parentId = UUID.randomUUID();
            Post parentPost = Post.builder().id(parentId).build();
            Post post = Post.builder().id(postId).user(User.builder().id(MOCK_USER_ID).build())
                    .parentPost(parentPost).build();

            when(postRepository.findByIdAndUserId(postId, MOCK_USER_ID))
                    .thenReturn(Optional.of(post));
            when(postRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            postService.deletePost(postId);

            verify(postRepository).decrementCommentCount(parentId);
        }

        @Test
        @DisplayName("Thành công - Xóa bài gốc → không gọi decrementCommentCount")
        void deletePost_RootPost_NoDecrement() {
            UUID postId = UUID.randomUUID();
            Post post = Post.builder().id(postId).user(User.builder().id(MOCK_USER_ID).build())
                    .parentPost(null).build();

            when(postRepository.findByIdAndUserId(postId, MOCK_USER_ID))
                    .thenReturn(Optional.of(post));
            when(postRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            postService.deletePost(postId);

            verify(postRepository, never()).decrementCommentCount(any());
        }

        @Test
        @DisplayName("Thất bại - Không tìm thấy hoặc không phải chủ → PostAccessDeniedException")
        void deletePost_NotFoundOrNotOwner_ThrowsPostAccessDeniedException() {
            UUID postId = UUID.randomUUID();
            when(postRepository.findByIdAndUserId(postId, MOCK_USER_ID))
                    .thenReturn(Optional.empty());

            assertThrows(PostAccessDeniedException.class, () -> postService.deletePost(postId));
            verify(postRepository, never()).save(any());
        }

        @Test
        @DisplayName("Thất bại - Bài viết đã bị xóa trước đó")
        void deletePost_AlreadyDeleted_ThrowsPostNotFoundException() {
            UUID postId = UUID.randomUUID();
            Post post = Post.builder().id(postId).user(User.builder().id(MOCK_USER_ID).build()).build();
            post.setDeleted(true);

            when(postRepository.findByIdAndUserId(postId, MOCK_USER_ID))
                    .thenReturn(Optional.of(post));

            assertThrows(PostNotFoundException.class, () -> postService.deletePost(postId));
            verify(postRepository, never()).save(any());
        }
    }

    // =====================================================================
    // GET USER FEED
    // =====================================================================
    @Nested
    @DisplayName("getUserFeed()")
    class GetUserFeedTest {

        @Test
        @DisplayName("Thành công - Có trang kế tiếp")
        void getUserFeed_HasNextPage() {
            int size = 2;
            UUID cursor = UUID.randomUUID();
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();
            UUID id3 = UUID.randomUUID();

            Post post1 = Post.builder().id(id1).build();
            Post post2 = Post.builder().id(id2).build();

            when(postRepository.findPublicFeedIds(cursor, Limit.of(size + 1)))
                    .thenReturn(List.of(id1, id2, id3));
            when(postRepository.findAllWithDetailsByIds(List.of(id1, id2)))
                    .thenReturn(List.of(post1, post2));
            stubMapToResponse();

            CursorResponse<PostResponse> response = postService.getUserFeed(cursor, size);

            assertTrue(response.isHasNext());
            assertEquals(2, response.getContent().size());
            assertEquals(id2.toString(), response.getNextCursor());
        }

        @Test
        @DisplayName("Thành công - Trang cuối")
        void getUserFeed_LastPage() {
            int size = 5;
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();

            when(postRepository.findPublicFeedIds(null, Limit.of(size + 1)))
                    .thenReturn(List.of(id1, id2));
            when(postRepository.findAllWithDetailsByIds(List.of(id1, id2)))
                    .thenReturn(List.of(Post.builder().id(id1).build(), Post.builder().id(id2).build()));
            stubMapToResponse();

            CursorResponse<PostResponse> response = postService.getUserFeed(null, size);

            assertFalse(response.isHasNext());
            assertNull(response.getNextCursor());
            assertEquals(2, response.getContent().size());
        }

        @Test
        @DisplayName("Thành công - Feed trống")
        void getUserFeed_Empty() {
            when(postRepository.findPublicFeedIds(null, Limit.of(11))).thenReturn(List.of());

            CursorResponse<PostResponse> response = postService.getUserFeed(null, 10);

            assertTrue(response.getContent().isEmpty());
            assertFalse(response.isHasNext());
            verify(postRepository, never()).findAllWithDetailsByIds(any());
        }
    }

    // =====================================================================
    // GET POSTS BY HASHTAG
    // =====================================================================
    @Nested
    @DisplayName("getPostsByHashtag()")
    class GetPostsByHashtagTest {

        private Hashtag mockHashtag;

        @BeforeEach
        void setUpHashtag() {
            mockHashtag = Hashtag.builder().id(UUID.randomUUID()).name("java").build();
        }

        @Test
        @DisplayName("Thất bại - Hashtag không tồn tại")
        void getPostsByHashtag_HashtagNotFound() {
            when(hashtagRepository.findByName("unknown")).thenReturn(Optional.empty());

            assertThrows(HashtagNotFoundException.class,
                    () -> postService.getPostsByHashtag("unknown", null, 5));
            verifyNoInteractions(postHashtagRepository);
        }

        @Test
        @DisplayName("Thành công - Có trang kế tiếp")
        void getPostsByHashtag_HasNextPage() {
            int size = 2;
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();
            UUID id3 = UUID.randomUUID();

            when(hashtagRepository.findByName("java")).thenReturn(Optional.of(mockHashtag));
            // Service dùng findPostIdsByHashtagId trả về List<UUID>
            when(postHashtagRepository.findPostIdsByHashtagId(
                    eq(mockHashtag.getId()), isNull(), eq(Limit.of(size + 1))))
                    .thenReturn(List.of(id1, id2, id3));
            when(postRepository.findAllWithDetailsByIds(List.of(id1, id2)))
                    .thenReturn(List.of(Post.builder().id(id1).build(), Post.builder().id(id2).build()));
            stubMapToResponse();

            CursorResponse<PostResponse> response = postService.getPostsByHashtag("java", null, size);

            assertTrue(response.isHasNext());
            assertEquals(2, response.getContent().size());
        }

        @Test
        @DisplayName("Thành công - Trang cuối")
        void getPostsByHashtag_LastPage() {
            int size = 5;
            UUID id1 = UUID.randomUUID();

            when(hashtagRepository.findByName("java")).thenReturn(Optional.of(mockHashtag));
            when(postHashtagRepository.findPostIdsByHashtagId(
                    eq(mockHashtag.getId()), isNull(), eq(Limit.of(size + 1))))
                    .thenReturn(List.of(id1));
            when(postRepository.findAllWithDetailsByIds(List.of(id1)))
                    .thenReturn(List.of(Post.builder().id(id1).build()));
            stubMapToResponse();

            CursorResponse<PostResponse> response = postService.getPostsByHashtag("java", null, size);

            assertFalse(response.isHasNext());
            assertEquals(1, response.getContent().size());
        }

        @Test
        @DisplayName("Thành công - Không có bài viết")
        void getPostsByHashtag_Empty() {
            when(hashtagRepository.findByName("java")).thenReturn(Optional.of(mockHashtag));
            when(postHashtagRepository.findPostIdsByHashtagId(any(), any(), any()))
                    .thenReturn(List.of());

            CursorResponse<PostResponse> response = postService.getPostsByHashtag("java", null, 5);

            assertTrue(response.getContent().isEmpty());
            assertFalse(response.isHasNext());
        }

        @Test
        @DisplayName("Thành công - Hashtag normalize về lowercase")
        void getPostsByHashtag_NormalizesToLowercase() {
            when(hashtagRepository.findByName("java")).thenReturn(Optional.of(mockHashtag));
            when(postHashtagRepository.findPostIdsByHashtagId(any(), any(), any()))
                    .thenReturn(List.of());

            postService.getPostsByHashtag("JAVA", null, 5);

            verify(hashtagRepository).findByName("java");
        }
    }

    // =====================================================================
    // CREATE REPLY
    // =====================================================================
    @Nested
    @DisplayName("createReply()")
    class CreateReplyTest {

        @Test
        @DisplayName("Thất bại - Bài viết cha không tồn tại")
        void createReply_ParentPostNotFound() {
            UUID parentId = UUID.randomUUID();
            when(postRepository.findById(parentId)).thenReturn(Optional.empty());

            assertThrows(PostNotFoundException.class,
                    () -> postService.createReply(parentId, new PostRequest()));
            verify(postRepository, never()).save(any());
        }

        @Test
        @DisplayName("Thành công - Tạo reply và increment commentCount")
        void createReply_Success_IncrementsCommentCount() {
            UUID parentId = UUID.randomUUID();
            User user = User.builder().id(MOCK_USER_ID).build();
            Post parentPost = Post.builder().id(parentId).build();
            PostRequest request = PostRequest.builder().content("Reply content").build();
            Post savedReply = Post.builder().id(UUID.randomUUID()).user(user)
                    .parentPost(parentPost).build();

            when(postRepository.findById(parentId)).thenReturn(Optional.of(parentPost));
            when(userRepository.findById(MOCK_USER_ID)).thenReturn(Optional.of(user));
            when(postMapper.toPost(any())).thenReturn(new Post());
            when(postRepository.save(any())).thenReturn(savedReply);
            stubMapToResponse();

            PostResponse result = postService.createReply(parentId, request);

            assertNotNull(result);
            assertEquals(parentId, request.getParentPostId());
            verify(postRepository).incrementCommentCount(parentId);
        }
    }

    // =====================================================================
    // GET REPLIES
    // =====================================================================
    @Nested
    @DisplayName("getReplies()")
    class GetRepliesTest {

        @Test
        @DisplayName("Thất bại - Bài viết gốc không tồn tại")
        void getReplies_ParentNotFound() {
            UUID postId = UUID.randomUUID();
            when(postRepository.findById(postId)).thenReturn(Optional.empty());

            assertThrows(PostNotFoundException.class,
                    () -> postService.getReplies(postId, null, 5));
            verify(postRepository, never()).findRepliesIds(any(), any(), any());
        }

        @Test
        @DisplayName("Thành công - Có trang kế tiếp")
        void getReplies_HasNextPage() {
            int size = 2;
            UUID postId = UUID.randomUUID();
            UUID r1 = UUID.randomUUID(), r2 = UUID.randomUUID(), r3 = UUID.randomUUID();
            Post parent = Post.builder().id(postId).build();

            when(postRepository.findById(postId)).thenReturn(Optional.of(parent));
            when(postRepository.findRepliesIds(postId, null, Limit.of(size + 1)))
                    .thenReturn(List.of(r1, r2, r3));
            when(postRepository.findAllWithDetailsByIds(List.of(r1, r2)))
                    .thenReturn(List.of(Post.builder().id(r1).build(), Post.builder().id(r2).build()));
            stubMapToResponse();

            CursorResponse<PostResponse> response = postService.getReplies(postId, null, size);

            assertTrue(response.isHasNext());
            assertEquals(2, response.getContent().size());
            assertEquals(r2.toString(), response.getNextCursor());
        }

        @Test
        @DisplayName("Thành công - Không có reply")
        void getReplies_NoReplies() {
            UUID postId = UUID.randomUUID();
            when(postRepository.findById(postId)).thenReturn(Optional.of(Post.builder().id(postId).build()));
            when(postRepository.findRepliesIds(postId, null, Limit.of(11))).thenReturn(List.of());

            CursorResponse<PostResponse> response = postService.getReplies(postId, null, 10);

            assertTrue(response.getContent().isEmpty());
            verify(postRepository, never()).findAllWithDetailsByIds(any());
        }
    }
}