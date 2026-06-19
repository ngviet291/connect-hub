package com.connecthub.post;

import com.connecthub.common.dto.response.CursorResponse;
import com.connecthub.common.util.AppUtil;
import com.connecthub.modules.features.post.dto.request.PostRequest;
import com.connecthub.modules.features.post.dto.response.PostResponse;
import com.connecthub.modules.features.post.entity.Hashtag;
import com.connecthub.modules.features.post.entity.Media;
import com.connecthub.modules.features.post.entity.Mention;
import com.connecthub.modules.features.post.entity.Post;
import com.connecthub.modules.features.post.entity.PostHashtag;
import com.connecthub.modules.features.post.exception.HashtagNotFoundException;
import com.connecthub.modules.features.post.exception.PostAccessDeniedException;
import com.connecthub.modules.features.post.exception.PostNotFoundException;
import com.connecthub.modules.features.post.mapper.PostMapper;
import com.connecthub.modules.features.post.repository.HashtagRepository;
import com.connecthub.modules.features.post.repository.MediaRepository;
import com.connecthub.modules.features.post.repository.MentionRepository;
import com.connecthub.modules.features.post.repository.PostHashtagRepository;
import com.connecthub.modules.features.post.repository.PostRepository;
import com.connecthub.modules.features.post.service.PostService;
import com.connecthub.modules.features.user.entity.User;
import com.connecthub.modules.features.user.exception.UserNotFoundException;
import com.connecthub.modules.features.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
    @Mock private MediaRepository mediaRepository;
    @Mock private PostMapper postMapper;

    @InjectMocks
    private PostService postService;

    private MockedStatic<AppUtil> mockedAppUtil;
    private final UUID MOCK_USER_ID = UUID.fromString("019ed9d6-65e9-7267-b396-7ac0ad80ded8");

    @BeforeEach
    void setUp() {
        mockedAppUtil = Mockito.mockStatic(AppUtil.class);
        mockedAppUtil.when(AppUtil::userIdFormAuthentication).thenReturn(MOCK_USER_ID);
        mockedAppUtil.when(AppUtil::generateUUID).thenReturn(UUID.randomUUID());
    }

    @AfterEach
    void tearDown() {
        mockedAppUtil.close();
    }

    /**
     * Mock postMapper.mapToResponse(Post) — signature mới chỉ nhận Post
     */
    private void mockMapToResponse() {
        when(postMapper.mapToResponse(any(Post.class))).thenReturn(new PostResponse());
    }

    // ==========================================================
    // CREATE POST
    // ==========================================================
    @Nested
    @DisplayName("Test createPost()")
    class CreatePostTest {

        @Test
        @DisplayName("Thành công - Tạo bài viết thường (Không có quan hệ/hashtag/mention/media)")
        void createPost_Success() {
            User user = User.builder().id(MOCK_USER_ID).build();
            PostRequest request = new PostRequest();
            request.setContent("Hello World");

            Post mockPost = new Post();
            Post savedPost = Post.builder().id(UUID.randomUUID()).user(user).content("Hello World").build();

            when(userRepository.findById(MOCK_USER_ID)).thenReturn(Optional.of(user));
            when(postMapper.toPost(any(PostRequest.class))).thenReturn(mockPost);
            when(postRepository.save(any(Post.class))).thenReturn(savedPost);
            mockMapToResponse();

            PostResponse result = postService.createPost(request);

            assertNotNull(result);
            verify(userRepository).findById(MOCK_USER_ID);
            verify(postRepository).save(any(Post.class));
            verify(postMapper).mapToResponse(savedPost);
        }

        @Test
        @DisplayName("Thành công - Tạo bài viết phản hồi (Có parentPostId)")
        void createPost_WithParentPost_Success() {
            User user = User.builder().id(MOCK_USER_ID).build();
            UUID parentId = UUID.randomUUID();
            PostRequest request = new PostRequest();
            request.setContent("This is a reply");
            request.setParentPostId(parentId);

            Post parentPost = Post.builder().id(parentId).build();
            Post mockPost = new Post();
            Post savedPost = Post.builder().id(UUID.randomUUID()).user(user).parentPost(parentPost).build();

            when(userRepository.findById(MOCK_USER_ID)).thenReturn(Optional.of(user));
            when(postMapper.toPost(any(PostRequest.class))).thenReturn(mockPost);
            when(postRepository.findById(parentId)).thenReturn(Optional.of(parentPost));
            when(postRepository.save(any(Post.class))).thenReturn(savedPost);
            mockMapToResponse();

            PostResponse result = postService.createPost(request);

            assertNotNull(result);
            verify(postRepository).findById(parentId);
        }

        @Test
        @DisplayName("Thất bại - Không tìm thấy bài viết cha (Parent Post)")
        void createPost_ParentPostNotFound() {
            User user = User.builder().id(MOCK_USER_ID).build();
            UUID fakeParentId = UUID.randomUUID();
            PostRequest request = new PostRequest();
            request.setParentPostId(fakeParentId);

            when(userRepository.findById(MOCK_USER_ID)).thenReturn(Optional.of(user));
            when(postMapper.toPost(any(PostRequest.class))).thenReturn(new Post());
            when(postRepository.findById(fakeParentId)).thenReturn(Optional.empty());

            assertThrows(PostNotFoundException.class, () -> postService.createPost(request));
        }

        @Test
        @DisplayName("Thành công - Tạo bài viết trích dẫn (Có quotePostId)")
        void createPost_WithQuotePost_Success() {
            User user = User.builder().id(MOCK_USER_ID).build();
            UUID quoteId = UUID.randomUUID();
            PostRequest request = new PostRequest();
            request.setContent("Check this out!");
            request.setQuotePostId(quoteId);

            Post quotePost = Post.builder().id(quoteId).build();
            Post mockPost = new Post();
            Post savedPost = Post.builder().id(UUID.randomUUID()).user(user).quotePost(quotePost).build();

            when(userRepository.findById(MOCK_USER_ID)).thenReturn(Optional.of(user));
            when(postMapper.toPost(any(PostRequest.class))).thenReturn(mockPost);
            when(postRepository.findById(quoteId)).thenReturn(Optional.of(quotePost));
            when(postRepository.save(any(Post.class))).thenReturn(savedPost);
            mockMapToResponse();

            PostResponse result = postService.createPost(request);

            assertNotNull(result);
            verify(postRepository).findById(quoteId);
        }

        @Test
        @DisplayName("Thất bại - Không tìm thấy bài viết trích dẫn (Quote Post)")
        void createPost_QuotePostNotFound() {
            User user = User.builder().id(MOCK_USER_ID).build();
            UUID fakeQuoteId = UUID.randomUUID();
            PostRequest request = new PostRequest();
            request.setQuotePostId(fakeQuoteId);

            when(userRepository.findById(MOCK_USER_ID)).thenReturn(Optional.of(user));
            when(postMapper.toPost(any(PostRequest.class))).thenReturn(new Post());
            when(postRepository.findById(fakeQuoteId)).thenReturn(Optional.empty());

            assertThrows(PostNotFoundException.class, () -> postService.createPost(request));
        }

        @Test
        @DisplayName("Thành công - Có đính kèm Hashtags và Mentions")
        void createPost_WithHashtagsAndMentions_Success() {
            User user = User.builder().id(MOCK_USER_ID).build();
            PostRequest request = new PostRequest();
            request.setContent("Hello #Java @user123");
            request.setHashtags(List.of("Java"));

            UUID mentionedUserId = UUID.randomUUID();
            request.setMentionUserIds(List.of(mentionedUserId));

            Post mockPost = new Post();
            Post savedPost = Post.builder().id(UUID.randomUUID()).user(user).build();

            when(userRepository.findById(MOCK_USER_ID)).thenReturn(Optional.of(user));
            when(postMapper.toPost(any(PostRequest.class))).thenReturn(mockPost);
            when(postRepository.save(any(Post.class))).thenReturn(savedPost);
            when(hashtagRepository.findByName("Java")).thenReturn(Optional.empty());

            Hashtag createdHashtag = Hashtag.builder().id(UUID.randomUUID()).name("Java").build();
            when(hashtagRepository.save(any(Hashtag.class))).thenReturn(createdHashtag);
            when(postHashtagRepository.existsByPostIdAndHashtagId(any(), any())).thenReturn(false);

            User mentionedUser = User.builder().id(mentionedUserId).username("user123").build();
            when(userRepository.findById(mentionedUserId)).thenReturn(Optional.of(mentionedUser));
            mockMapToResponse();

            PostResponse result = postService.createPost(request);

            assertNotNull(result);
            verify(hashtagRepository).findByName("Java");
            verify(userRepository).findById(mentionedUserId);
        }

        @Test
        @DisplayName("Thành công - Có đính kèm Media (mediaIds hợp lệ, chưa thuộc post nào)")
        void createPost_WithMedia_Success() {
            User user = User.builder().id(MOCK_USER_ID).build();
            UUID mediaId = UUID.randomUUID();
            PostRequest request = new PostRequest();
            request.setContent("Post with image");
            request.setMediaIds(List.of(mediaId));

            Post mockPost = new Post();
            Post savedPost = Post.builder().id(UUID.randomUUID()).user(user).build();
            Media media = Media.builder().id(mediaId).build();

            when(userRepository.findById(MOCK_USER_ID)).thenReturn(Optional.of(user));
            when(postMapper.toPost(any(PostRequest.class))).thenReturn(mockPost);
            when(postRepository.save(any(Post.class))).thenReturn(savedPost);
            when(mediaRepository.findAllByIdInAndPostIsNull(List.of(mediaId))).thenReturn(List.of(media));
            mockMapToResponse();

            PostResponse result = postService.createPost(request);

            assertNotNull(result);
            verify(mediaRepository).findAllByIdInAndPostIsNull(List.of(mediaId));
            verify(mediaRepository).saveAll(anyList());
            assertEquals(savedPost, media.getPost()); // media đã được link vào post
        }

        @Test
        @DisplayName("Thất bại - Không tìm thấy user thực hiện bài viết")
        void createPost_UserNotFound() {
            PostRequest request = new PostRequest();
            when(userRepository.findById(MOCK_USER_ID)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class, () -> postService.createPost(request));
            verify(postMapper, never()).toPost(any());
        }
    }

    // ==========================================================
    // GET POST
    // ==========================================================
    @Nested
    @DisplayName("Test getPost()")
    class GetPostTest {

        @Test
        @DisplayName("Thành công - Lấy bài viết đang hoạt động (kèm media, quotePost)")
        void getPost_Success() {
            UUID postId = UUID.randomUUID();
            Post post = Post.builder().id(postId).isDeleted(false).build();

            // ✅ Dùng findByIdWithDetails thay vì findById
            when(postRepository.findByIdWithDetails(postId)).thenReturn(Optional.of(post));
            mockMapToResponse();

            PostResponse result = postService.getPost(postId);

            assertNotNull(result);
            verify(postRepository).findByIdWithDetails(postId);
            verify(postMapper).mapToResponse(post);
        }

        @Test
        @DisplayName("Thất bại - ID bài viết không tồn tại trong hệ thống")
        void getPost_NotFound() {
            UUID postId = UUID.randomUUID();
            when(postRepository.findByIdWithDetails(postId)).thenReturn(Optional.empty());

            assertThrows(PostNotFoundException.class, () -> postService.getPost(postId));
        }

        @Test
        @DisplayName("Thất bại - Bài viết đã bị đánh dấu xóa (isDeleted = true)")
        void getPost_PostDeleted() {
            UUID postId = UUID.randomUUID();
            Post deletedPost = Post.builder().id(postId).isDeleted(true).build();

            when(postRepository.findByIdWithDetails(postId)).thenReturn(Optional.of(deletedPost));

            PostNotFoundException exception = assertThrows(PostNotFoundException.class,
                    () -> postService.getPost(postId));
            assertEquals("Post has been deleted", exception.getMessage());
        }
    }

    // ==========================================================
    // UPDATE POST
    // ==========================================================
    @Nested
    @DisplayName("Test updatePost()")
    class UpdatePostTest {

        @Test
        @DisplayName("Thành công - Cập nhật nội dung bài viết")
        void updatePost_Success() {
            UUID postId = UUID.randomUUID();
            User user = User.builder().id(MOCK_USER_ID).build();
            Post post = Post.builder().id(postId).user(user).isDeleted(false).build();
            PostRequest request = new PostRequest();
            request.setContent("Updated content");

            // ✅ Dùng findByIdWithDetails
            when(postRepository.findByIdWithDetails(postId)).thenReturn(Optional.of(post));
            when(postRepository.save(any(Post.class))).thenReturn(post);
            mockMapToResponse();

            PostResponse result = postService.updatePost(postId, request);

            assertNotNull(result);
            assertEquals("Updated content", post.getContent());
            verify(postRepository).findByIdWithDetails(postId);
            verify(postRepository).save(post);
        }

        @Test
        @DisplayName("Thất bại - Không phải chủ bài viết")
        void updatePost_AccessDenied() {
            UUID postId = UUID.randomUUID();
            User owner = User.builder().id(UUID.randomUUID()).build();
            Post post = Post.builder().id(postId).user(owner).isDeleted(false).build();

            when(postRepository.findByIdWithDetails(postId)).thenReturn(Optional.of(post));

            assertThrows(PostAccessDeniedException.class,
                    () -> postService.updatePost(postId, new PostRequest()));
            verify(postRepository, never()).save(any());
        }

        @Test
        @DisplayName("Thất bại - Bài viết đã bị xóa, không thể cập nhật")
        void updatePost_PostDeleted() {
            UUID postId = UUID.randomUUID();
            User user = User.builder().id(MOCK_USER_ID).build();
            Post post = Post.builder().id(postId).user(user).isDeleted(true).build();

            when(postRepository.findByIdWithDetails(postId)).thenReturn(Optional.of(post));

            assertThrows(PostNotFoundException.class,
                    () -> postService.updatePost(postId, new PostRequest()));
        }

        @Test
        @DisplayName("Thất bại - Bài viết không tồn tại")
        void updatePost_PostNotFound() {
            UUID postId = UUID.randomUUID();
            when(postRepository.findByIdWithDetails(postId)).thenReturn(Optional.empty());

            assertThrows(PostNotFoundException.class,
                    () -> postService.updatePost(postId, new PostRequest()));
        }
    }

    // ==========================================================
    // DELETE POST
    // ==========================================================
    @Nested
    @DisplayName("Test deletePost()")
    class DeletePostTest {

        @Test
        @DisplayName("Thành công - Xóa mềm bài viết của chính mình")
        void deletePost_Success() {
            UUID postId = UUID.randomUUID();
            User user = User.builder().id(MOCK_USER_ID).build();
            Post post = Post.builder().id(postId).user(user).isDeleted(false).build();

            when(userRepository.findById(MOCK_USER_ID)).thenReturn(Optional.of(user));
            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

            postService.deletePost(postId);

            assertTrue(post.isDeleted());
            verify(postRepository).save(post);
        }

        @Test
        @DisplayName("Thành công - Xóa reply → giảm commentCount của parent")
        void deletePost_Reply_DecrementsParentCommentCount() {
            UUID postId = UUID.randomUUID();
            UUID parentId = UUID.randomUUID();
            User user = User.builder().id(MOCK_USER_ID).build();
            Post parentPost = Post.builder().id(parentId).build();
            Post post = Post.builder().id(postId).user(user).parentPost(parentPost).isDeleted(false).build();

            when(userRepository.findById(MOCK_USER_ID)).thenReturn(Optional.of(user));
            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

            postService.deletePost(postId);

            assertTrue(post.isDeleted());
            verify(postRepository).decrementCommentCount(parentId);
        }

        @Test
        @DisplayName("Thất bại - Không tìm thấy User đang thao tác")
        void deletePost_UserNotFound() {
            UUID postId = UUID.randomUUID();
            when(userRepository.findById(MOCK_USER_ID)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class, () -> postService.deletePost(postId));
        }

        @Test
        @DisplayName("Thất bại - Không tìm thấy bài viết cần xóa")
        void deletePost_PostNotFound() {
            UUID postId = UUID.randomUUID();
            User user = User.builder().id(MOCK_USER_ID).build();

            when(userRepository.findById(MOCK_USER_ID)).thenReturn(Optional.of(user));
            when(postRepository.findById(postId)).thenReturn(Optional.empty());

            assertThrows(PostNotFoundException.class, () -> postService.deletePost(postId));
        }

        @Test
        @DisplayName("Thất bại - Không có quyền xóa bài viết của người khác")
        void deletePost_AccessDenied() {
            UUID postId = UUID.randomUUID();
            User currentUser = User.builder().id(MOCK_USER_ID).build();
            User postOwner = User.builder().id(UUID.randomUUID()).username("other_user").build();
            Post post = Post.builder().id(postId).user(postOwner).isDeleted(false).build();

            when(userRepository.findById(MOCK_USER_ID)).thenReturn(Optional.of(currentUser));
            when(postRepository.findById(postId)).thenReturn(Optional.of(post));

            PostAccessDeniedException exception = assertThrows(PostAccessDeniedException.class,
                    () -> postService.deletePost(postId));
            assertEquals("delete", exception.getMessage());
        }
    }

    // ==========================================================
    // GET USER FEED
    // ==========================================================
    @Nested
    @DisplayName("Test getUserFeed()")
    class GetUserFeedTest {

        @Test
        @DisplayName("Thành công - Có trang kế tiếp (DB trả về > size)")
        void getUserFeed_HasNextPage_Success() {
            int size = 2;
            UUID cursor = UUID.randomUUID();

            Post post1 = Post.builder().id(UUID.randomUUID()).content("Post 1").build();
            Post post2 = Post.builder().id(UUID.randomUUID()).content("Post 2").build();
            Post post3 = Post.builder().id(UUID.randomUUID()).content("Post thừa").build();
            List<Post> mockFeed = new ArrayList<>(List.of(post1, post2, post3));

            // ✅ Dùng findPublicFeedWithDetails
            when(postRepository.findPublicFeedWithDetails(eq(cursor), eq(Limit.of(size + 1)))).thenReturn(mockFeed);
            mockMapToResponse();

            CursorResponse<PostResponse> response = postService.getUserFeed(cursor, size);

            assertNotNull(response);
            assertTrue(response.isHasNext());
            assertEquals(2, response.getContent().size());
            assertEquals(post2.getId().toString(), response.getNextCursor());
        }

        @Test
        @DisplayName("Thành công - Là trang cuối cùng (DB trả về <= size)")
        void getUserFeed_IsLastPage_Success() {
            int size = 5;

            Post post1 = Post.builder().id(UUID.randomUUID()).content("Post 1").build();
            Post post2 = Post.builder().id(UUID.randomUUID()).content("Post 2").build();
            List<Post> mockFeed = new ArrayList<>(List.of(post1, post2));

            when(postRepository.findPublicFeedWithDetails(isNull(), eq(Limit.of(size + 1)))).thenReturn(mockFeed);
            mockMapToResponse();

            CursorResponse<PostResponse> response = postService.getUserFeed(null, size);

            assertNotNull(response);
            assertFalse(response.isHasNext());
            assertEquals(2, response.getContent().size());
        }

        @Test
        @DisplayName("Thành công - Feed trống rỗng")
        void getUserFeed_EmptyFeed_Success() {
            int size = 10;
            when(postRepository.findPublicFeedWithDetails(isNull(), eq(Limit.of(size + 1))))
                    .thenReturn(new ArrayList<>());

            CursorResponse<PostResponse> response = postService.getUserFeed(null, size);

            assertNotNull(response);
            assertFalse(response.isHasNext());
            assertNull(response.getNextCursor());
            assertTrue(response.getContent().isEmpty());
            verify(postMapper, never()).mapToResponse(any(Post.class));
        }
    }

    // ==========================================================
    // GET POSTS BY HASHTAG
    // ==========================================================
    @Nested
    @DisplayName("Test getPostsByHashtag()")
    class GetPostsByHashtagTest {

        private final String VALID_TAG = "Java";
        private final String INVALID_TAG = "UnknownTag";
        private Hashtag mockHashtagEntity;

        @BeforeEach
        void setUp() {
            mockHashtagEntity = Hashtag.builder().id(UUID.randomUUID()).name(VALID_TAG).build();
        }

        @Test
        @DisplayName("Thất bại - Không tìm thấy Hashtag")
        void getPostsByHashtag_HashtagNotFound() {
            when(hashtagRepository.findByName(INVALID_TAG)).thenReturn(Optional.empty());

            assertThrows(HashtagNotFoundException.class,
                    () -> postService.getPostsByHashtag(INVALID_TAG, null, 5));
            verifyNoInteractions(postHashtagRepository);
        }

        @Test
        @DisplayName("Thành công - Có trang kế tiếp")
        void getPostsByHashtag_HasNextPage_Success() {
            int size = 2;
            UUID cursor = UUID.randomUUID();

            Post post1 = Post.builder().id(UUID.randomUUID()).build();
            Post post2 = Post.builder().id(UUID.randomUUID()).build();
            Post post3 = Post.builder().id(UUID.randomUUID()).build();

            PostHashtag ph1 = PostHashtag.builder().post(post1).hashtag(mockHashtagEntity).build();
            PostHashtag ph2 = PostHashtag.builder().post(post2).hashtag(mockHashtagEntity).build();
            PostHashtag ph3 = PostHashtag.builder().post(post3).hashtag(mockHashtagEntity).build();

            when(hashtagRepository.findByName(VALID_TAG)).thenReturn(Optional.of(mockHashtagEntity));
            when(postHashtagRepository.findPostsByHashtagId(
                    eq(mockHashtagEntity.getId()), eq(cursor), eq(Limit.of(size + 1))))
                    .thenReturn(new ArrayList<>(List.of(ph1, ph2, ph3)));
            mockMapToResponse();

            CursorResponse<PostResponse> response = postService.getPostsByHashtag(VALID_TAG, cursor, size);

            assertNotNull(response);
            assertTrue(response.isHasNext());
            assertEquals(2, response.getContent().size());
            assertEquals(post2.getId().toString(), response.getNextCursor());
        }

        @Test
        @DisplayName("Thành công - Là trang cuối cùng")
        void getPostsByHashtag_IsLastPage_Success() {
            int size = 5;
            Post post1 = Post.builder().id(UUID.randomUUID()).build();
            PostHashtag ph1 = PostHashtag.builder().post(post1).hashtag(mockHashtagEntity).build();

            when(hashtagRepository.findByName(VALID_TAG)).thenReturn(Optional.of(mockHashtagEntity));
            when(postHashtagRepository.findPostsByHashtagId(
                    eq(mockHashtagEntity.getId()), isNull(), eq(Limit.of(size + 1))))
                    .thenReturn(new ArrayList<>(List.of(ph1)));
            mockMapToResponse();

            CursorResponse<PostResponse> response = postService.getPostsByHashtag(VALID_TAG, null, size);

            assertNotNull(response);
            assertFalse(response.isHasNext());
            assertEquals(1, response.getContent().size());
        }

        @Test
        @DisplayName("Thành công - Không có bài viết nào gắn Hashtag")
        void getPostsByHashtag_EmptyResult_Success() {
            int size = 5;
            when(hashtagRepository.findByName(VALID_TAG)).thenReturn(Optional.of(mockHashtagEntity));
            when(postHashtagRepository.findPostsByHashtagId(
                    eq(mockHashtagEntity.getId()), isNull(), eq(Limit.of(size + 1))))
                    .thenReturn(new ArrayList<>());

            CursorResponse<PostResponse> response = postService.getPostsByHashtag(VALID_TAG, null, size);

            assertNotNull(response);
            assertFalse(response.isHasNext());
            assertNull(response.getNextCursor());
            assertTrue(response.getContent().isEmpty());
            verify(postMapper, never()).mapToResponse(any(Post.class));
        }
    }

    // ==========================================================
    // ADD HASHTAG TO POST
    // ==========================================================
    @Nested
    @DisplayName("Test addHashtagToPost()")
    class AddHashtagToPostTest {

        private final String TAG_NAME = "Backend";
        private UUID postId;
        private Post mockPost;

        @BeforeEach
        void setUp() {
            postId = UUID.randomUUID();
            mockPost = Post.builder().id(postId).content("Learning Unit Test in Java").build();
        }

        @Test
        @DisplayName("Thất bại - Không tìm thấy bài viết")
        void addHashtagToPost_PostNotFound() {
            when(postRepository.findById(postId)).thenReturn(Optional.empty());

            assertThrows(PostNotFoundException.class,
                    () -> postService.addHashtagToPost(postId, TAG_NAME));
            verifyNoInteractions(hashtagRepository, postHashtagRepository);
        }

        @Test
        @DisplayName("Thành công - Hashtag chưa tồn tại → Tạo mới và liên kết")
        void addHashtagToPost_HashtagNotExists_CreatesNewAndSaves() {
            Hashtag savedHashtag = Hashtag.builder().id(UUID.randomUUID()).name(TAG_NAME).build();

            when(postRepository.findById(postId)).thenReturn(Optional.of(mockPost));
            when(hashtagRepository.findByName(TAG_NAME)).thenReturn(Optional.empty());
            when(hashtagRepository.save(any(Hashtag.class))).thenReturn(savedHashtag);
            when(postHashtagRepository.existsByPostIdAndHashtagId(postId, savedHashtag.getId())).thenReturn(false);

            postService.addHashtagToPost(postId, TAG_NAME);

            verify(hashtagRepository).save(any(Hashtag.class));
            verify(postHashtagRepository).save(any(PostHashtag.class));
        }

        @Test
        @DisplayName("Thành công - Hashtag đã tồn tại, bài viết chưa gắn → Chỉ tạo liên kết")
        void addHashtagToPost_HashtagExistsButNotAttached_SavesOnlyLink() {
            Hashtag existingHashtag = Hashtag.builder().id(UUID.randomUUID()).name(TAG_NAME).build();

            when(postRepository.findById(postId)).thenReturn(Optional.of(mockPost));
            when(hashtagRepository.findByName(TAG_NAME)).thenReturn(Optional.of(existingHashtag));
            when(postHashtagRepository.existsByPostIdAndHashtagId(postId, existingHashtag.getId())).thenReturn(false);

            postService.addHashtagToPost(postId, TAG_NAME);

            verify(hashtagRepository, never()).save(any(Hashtag.class));
            verify(postHashtagRepository).save(any(PostHashtag.class));
        }

        @Test
        @DisplayName("Thành công - Đã gắn hashtag từ trước → Bỏ qua, không lưu trùng")
        void addHashtagToPost_AlreadyAttached_ReturnsEarly() {
            Hashtag existingHashtag = Hashtag.builder().id(UUID.randomUUID()).name(TAG_NAME).build();

            when(postRepository.findById(postId)).thenReturn(Optional.of(mockPost));
            when(hashtagRepository.findByName(TAG_NAME)).thenReturn(Optional.of(existingHashtag));
            when(postHashtagRepository.existsByPostIdAndHashtagId(postId, existingHashtag.getId())).thenReturn(true);

            postService.addHashtagToPost(postId, TAG_NAME);

            verify(postHashtagRepository, never()).save(any(PostHashtag.class));
        }
    }

    // ==========================================================
    // CREATE REPLY
    // ==========================================================
    @Nested
    @DisplayName("Test createReply()")
    class CreateReplyTest {

        private UUID parentPostId;
        private Post parentPost;
        private PostRequest replyRequest;

        @BeforeEach
        void setUp() {
            parentPostId = UUID.randomUUID();
            parentPost = Post.builder().id(parentPostId).content("Bài viết gốc").build();
            replyRequest = new PostRequest();
            replyRequest.setContent("Đây là bài viết phản hồi");
        }

        @Test
        @DisplayName("Thất bại - Không tìm thấy bài viết cha")
        void createReply_ParentPostNotFound() {
            when(postRepository.findById(parentPostId)).thenReturn(Optional.empty());

            assertThrows(PostNotFoundException.class,
                    () -> postService.createReply(parentPostId, replyRequest));
            verify(postRepository, never()).save(any(Post.class));
        }

        @Test
        @DisplayName("Thành công - Tạo reply và tăng commentCount của parent")
        void createReply_Success_AndIncrementsCommentCount() {
            User currentUser = User.builder().id(MOCK_USER_ID).build();
            Post mockReplyPost = new Post();
            Post savedReply = Post.builder()
                    .id(UUID.randomUUID())
                    .parentPost(parentPost)
                    .content("Đây là bài viết phản hồi")
                    .user(currentUser)
                    .build();

            // lần 1: getPostOrThrow trong createReply, lần 2: getPostOrThrow trong createPost (parentPostId)
            when(postRepository.findById(parentPostId)).thenReturn(Optional.of(parentPost));
            when(userRepository.findById(MOCK_USER_ID)).thenReturn(Optional.of(currentUser));
            when(postMapper.toPost(any(PostRequest.class))).thenReturn(mockReplyPost);
            when(postRepository.save(any(Post.class))).thenReturn(savedReply);
            mockMapToResponse();

            PostResponse response = postService.createReply(parentPostId, replyRequest);

            assertNotNull(response);
            assertEquals(parentPostId, replyRequest.getParentPostId());
            // ✅ Phải gọi incrementCommentCount
            verify(postRepository).incrementCommentCount(parentPostId);
            verify(postRepository, times(2)).findById(parentPostId);
        }
    }

    // ==========================================================
    // GET REPLIES
    // ==========================================================
    @Nested
    @DisplayName("Test getReplies()")
    class GetRepliesTest {

        private UUID parentPostId;
        private Post parentPost;

        @BeforeEach
        void setUp() {
            parentPostId = UUID.randomUUID();
            parentPost = Post.builder().id(parentPostId).content("Bài viết gốc").build();
        }

        @Test
        @DisplayName("Thất bại - Bài viết gốc không tồn tại")
        void getReplies_TargetPostNotFound() {
            when(postRepository.findById(parentPostId)).thenReturn(Optional.empty());

            assertThrows(PostNotFoundException.class,
                    () -> postService.getReplies(parentPostId, null, 5));
            verify(postRepository, never()).findRepliesByParentPostIdWithDetails(any(), any(), any());
        }

        @Test
        @DisplayName("Thành công - Có trang kế tiếp")
        void getReplies_HasNextPage_Success() {
            int size = 2;
            UUID cursor = UUID.randomUUID();

            Post reply1 = Post.builder().id(UUID.randomUUID()).content("Reply 1").build();
            Post reply2 = Post.builder().id(UUID.randomUUID()).content("Reply 2").build();
            Post reply3 = Post.builder().id(UUID.randomUUID()).content("Reply thừa").build();

            when(postRepository.findById(parentPostId)).thenReturn(Optional.of(parentPost));
            // ✅ Dùng findRepliesByParentPostIdWithDetails
            when(postRepository.findRepliesByParentPostIdWithDetails(
                    eq(parentPostId), eq(cursor), eq(Limit.of(size + 1))))
                    .thenReturn(new ArrayList<>(List.of(reply1, reply2, reply3)));
            mockMapToResponse();

            CursorResponse<PostResponse> response = postService.getReplies(parentPostId, cursor, size);

            assertNotNull(response);
            assertTrue(response.isHasNext());
            assertEquals(2, response.getContent().size());
            assertEquals(reply2.getId().toString(), response.getNextCursor());
        }

        @Test
        @DisplayName("Thành công - Là trang cuối cùng")
        void getReplies_IsLastPage_Success() {
            int size = 5;
            Post reply1 = Post.builder().id(UUID.randomUUID()).content("Reply 1").build();

            when(postRepository.findById(parentPostId)).thenReturn(Optional.of(parentPost));
            when(postRepository.findRepliesByParentPostIdWithDetails(
                    eq(parentPostId), isNull(), eq(Limit.of(size + 1))))
                    .thenReturn(new ArrayList<>(List.of(reply1)));
            mockMapToResponse();

            CursorResponse<PostResponse> response = postService.getReplies(parentPostId, null, size);

            assertNotNull(response);
            assertFalse(response.isHasNext());
            assertEquals(1, response.getContent().size());
            assertEquals(reply1.getId().toString(), response.getNextCursor());
        }

        @Test
        @DisplayName("Thành công - Không có phản hồi nào (Trả về mảng rỗng)")
        void getReplies_EmptyReplies_Success() {
            int size = 10;

            when(postRepository.findById(parentPostId)).thenReturn(Optional.of(parentPost));
            when(postRepository.findRepliesByParentPostIdWithDetails(
                    eq(parentPostId), isNull(), eq(Limit.of(size + 1))))
                    .thenReturn(new ArrayList<>());

            CursorResponse<PostResponse> response = postService.getReplies(parentPostId, null, size);

            assertNotNull(response);
            assertFalse(response.isHasNext());
            assertNull(response.getNextCursor());
            assertTrue(response.getContent().isEmpty());
            verify(postMapper, never()).mapToResponse(any(Post.class));
        }
    }
}