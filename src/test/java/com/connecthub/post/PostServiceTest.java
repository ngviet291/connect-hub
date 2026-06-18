package com.connecthub.post;

import com.connecthub.common.dto.response.CursorResponse;
import com.connecthub.common.util.AppUtil;
import com.connecthub.modules.features.post.dto.request.PostRequest;
import com.connecthub.modules.features.post.dto.response.PostResponse;
import com.connecthub.modules.features.post.entity.Hashtag;
import com.connecthub.modules.features.post.entity.Mention;
import com.connecthub.modules.features.post.entity.Post;
import com.connecthub.modules.features.post.entity.PostHashtag;
import com.connecthub.modules.features.post.exception.PostAccessDeniedException;
import com.connecthub.modules.features.post.exception.PostNotFoundException;
import com.connecthub.modules.features.post.mapper.PostMapper;
import com.connecthub.modules.features.post.repository.*;
import com.connecthub.modules.features.post.service.PostService;
import com.connecthub.modules.features.user.entity.User;
import com.connecthub.modules.features.user.exception.UserNotFoundException;
import com.connecthub.modules.features.user.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock private PostRepository postRepository;
    @Mock private UserRepository userRepository;
    @Mock private HashtagRepository hashtagRepository;
    @Mock private PostHashtagRepository postHashtagRepository;
    @Mock private MentionRepository mentionRepository;
    @Mock private MediaRepository mediaRepository;
    @Mock private ReactionRepository reactionRepository;
    @Mock private BookmarkRepository bookmarkRepository;
    @Mock private RepostRepository repostRepository;
    @Mock private PostViewRepository postViewRepository;
    @Mock private PostMapper postMapper;

    @InjectMocks
    private PostService postService;

    private MockedStatic<AppUtil> mockedAppUtil;
    private final String MOCK_USERNAME = "test_user";

    @BeforeEach
    void setUp() {
        mockedAppUtil = Mockito.mockStatic(AppUtil.class);
        mockedAppUtil.when(AppUtil::usernameFromAuthentication).thenReturn(MOCK_USERNAME);
        mockedAppUtil.when(AppUtil::generateUUID).thenReturn(UUID.randomUUID());
    }

    @AfterEach
    void tearDown() {
        mockedAppUtil.close();
    }

    private void mockCommonMappingDependencies() {
        when(postRepository.countByParentPostIdAndIsDeletedFalse(any())).thenReturn(1L);
        when(reactionRepository.countByPost_Id(any())).thenReturn(2L);
        when(repostRepository.countByPost_Id(any())).thenReturn(3L);
        when(bookmarkRepository.countByPost_Id(any())).thenReturn(4L);
        when(postViewRepository.countByPost_Id(any())).thenReturn(5L);
        when(mediaRepository.findByPost_Id(any())).thenReturn(List.of());
        when(postMapper.mapToResponse(any(), any(), anyLong(), anyLong(), anyLong(), anyLong(), anyLong()))
                .thenReturn(new PostResponse());
    }

    // ==========================================================
    // CREATE POST
    // ==========================================================
    @Nested
    @DisplayName("Test createPost()")
    class CreatePostTest {

        @Test
        @DisplayName("Thành công - Tạo bài viết thường (Không có quan hệ/hashtag/mention)")
        void createPost_Success() {
            // Arrange
            User user = User.builder().id(UUID.randomUUID()).username(MOCK_USERNAME).build();
            PostRequest request = new PostRequest();
            request.setContent("Hello World");

            Post mockPost = new Post(); // Thêm post mồi tránh NPE
            Post savedPost = Post.builder().id(UUID.randomUUID()).user(user).content("Hello World").build();

            when(userRepository.findByUsername(MOCK_USERNAME)).thenReturn(Optional.of(user));
            when(postMapper.toPost(any(PostRequest.class))).thenReturn(mockPost); // Mock mapper
            when(postRepository.save(any(Post.class))).thenReturn(savedPost);

            mockCommonMappingDependencies();

            // Act
            PostResponse result = postService.createPost(request);

            // Assert
            assertNotNull(result);
            verify(userRepository).findByUsername(MOCK_USERNAME);
            verify(postRepository).save(any(Post.class));
        }

        @Test
        @DisplayName("Thành công - Tạo bài viết phản hồi (Có parentPostId)")
        void createPost_WithParentPost_Success() {
            // Arrange
            User user = User.builder().id(UUID.randomUUID()).username(MOCK_USERNAME).build();
            UUID parentId = UUID.randomUUID();
            PostRequest request = new PostRequest();
            request.setContent("This is a reply");
            request.setParentPostId(parentId);

            Post parentPost = Post.builder().id(parentId).build();
            Post mockPost = new Post(); // Thêm post mồi tránh NPE
            Post savedPost = Post.builder().id(UUID.randomUUID()).user(user).parentPost(parentPost).build();

            when(userRepository.findByUsername(MOCK_USERNAME)).thenReturn(Optional.of(user));
            when(postMapper.toPost(any(PostRequest.class))).thenReturn(mockPost); // Mock mapper
            when(postRepository.findById(parentId)).thenReturn(Optional.of(parentPost));
            when(postRepository.save(any(Post.class))).thenReturn(savedPost);

            mockCommonMappingDependencies();

            // Act
            PostResponse result = postService.createPost(request);

            // Assert
            assertNotNull(result);
            verify(postRepository).findById(parentId);
        }

        @Test
        @DisplayName("Thất bại - Không tìm thấy bài viết cha (Parent Post)")
        void createPost_ParentPostNotFound() {
            // Arrange
            User user = User.builder().id(UUID.randomUUID()).username(MOCK_USERNAME).build();
            UUID fakeParentId = UUID.randomUUID();
            PostRequest request = new PostRequest();
            request.setParentPostId(fakeParentId);

            Post mockPost = new Post(); // Thêm post mồi tránh NPE

            when(userRepository.findByUsername(MOCK_USERNAME)).thenReturn(Optional.of(user));
            when(postMapper.toPost(any(PostRequest.class))).thenReturn(mockPost); // Mock mapper
            when(postRepository.findById(fakeParentId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(PostNotFoundException.class, () -> postService.createPost(request));
        }

        @Test
        @DisplayName("Thành công - Tạo bài viết trích dẫn (Có quotePostId)")
        void createPost_WithQuotePost_Success() {
            // Arrange
            User user = User.builder().id(UUID.randomUUID()).username(MOCK_USERNAME).build();
            UUID quoteId = UUID.randomUUID();
            PostRequest request = new PostRequest();
            request.setContent("Check this out!");
            request.setQuotePostId(quoteId);

            Post quotePost = Post.builder().id(quoteId).build();
            Post mockPost = new Post(); // Thêm post mồi tránh NPE
            Post savedPost = Post.builder().id(UUID.randomUUID()).user(user).quotePost(quotePost).build();

            when(userRepository.findByUsername(MOCK_USERNAME)).thenReturn(Optional.of(user));
            when(postMapper.toPost(any(PostRequest.class))).thenReturn(mockPost); // Mock mapper
            when(postRepository.findById(quoteId)).thenReturn(Optional.of(quotePost));
            when(postRepository.save(any(Post.class))).thenReturn(savedPost);

            mockCommonMappingDependencies();

            // Act
            PostResponse result = postService.createPost(request);

            // Assert
            assertNotNull(result);
            verify(postRepository).findById(quoteId);
        }

        @Test
        @DisplayName("Thất bại - Không tìm thấy bài viết trích dẫn (Quote Post)")
        void createPost_QuotePostNotFound() {
            // Arrange
            User user = User.builder().id(UUID.randomUUID()).username(MOCK_USERNAME).build();
            UUID fakeQuoteId = UUID.randomUUID();
            PostRequest request = new PostRequest();
            request.setQuotePostId(fakeQuoteId);

            Post mockPost = new Post(); // Thêm post mồi tránh NPE

            when(userRepository.findByUsername(MOCK_USERNAME)).thenReturn(Optional.of(user));
            when(postMapper.toPost(any(PostRequest.class))).thenReturn(mockPost); // Mock mapper
            when(postRepository.findById(fakeQuoteId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(PostNotFoundException.class, () -> postService.createPost(request));
        }

        @Test
        @DisplayName("Thành công - Có đính kèm Hashtags và Mentions")
        void createPost_WithHashtagsAndMentions_Success() {
            // Arrange
            User user = User.builder().id(UUID.randomUUID()).username(MOCK_USERNAME).build();
            PostRequest request = new PostRequest();
            request.setContent("Hello #Java @user123");
            request.setHashtags(List.of("Java"));

            UUID mentionedUserId = UUID.randomUUID();
            request.setMentionUserIds(List.of(mentionedUserId));

            Post mockPost = new Post(); // Thêm post mồi tránh NPE
            Post savedPost = Post.builder().id(UUID.randomUUID()).user(user).build();

            when(userRepository.findByUsername(MOCK_USERNAME)).thenReturn(Optional.of(user));
            when(postMapper.toPost(any(PostRequest.class))).thenReturn(mockPost); // Mock mapper
            when(postRepository.save(any(Post.class))).thenReturn(savedPost);
            when(hashtagRepository.findByName("Java")).thenReturn(Optional.empty());

            Hashtag createdHashtag = Hashtag.builder().id(UUID.randomUUID()).name("Java").build();
            when(hashtagRepository.save(any(Hashtag.class))).thenReturn(createdHashtag);
            when(postHashtagRepository.existsByPostIdAndHashtagId(any(), any())).thenReturn(false);

            User mentionedUser = User.builder().id(mentionedUserId).username("user123").build();
            when(userRepository.findById(mentionedUserId)).thenReturn(Optional.of(mentionedUser));

            mockCommonMappingDependencies();

            // Act
            PostResponse result = postService.createPost(request);

            // Assert
            assertNotNull(result);
            verify(hashtagRepository).findByName("Java");
            verify(userRepository).findById(mentionedUserId);
        }

        @Test
        @DisplayName("Thất bại - Không tìm thấy user thực hiện bài viết")
        void createPost_UserNotFound() {
            // Arrange
            PostRequest request = new PostRequest();
            when(userRepository.findByUsername(MOCK_USERNAME)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(UserNotFoundException.class, () -> postService.createPost(request));

            // Do hàm getUserOrThrow ném Exception ngay từ đầu, postMapper.toPost() không bao giờ được chạy
            // nên kịch bản này không cần mock postMapper.
        }
    }

    // ==========================================================
    // GET POST
    // ==========================================================
    @Nested
    @DisplayName("Test getPost()")
    class GetPostTest {

        @Test
        @DisplayName("Thành công - Lấy bài viết đang hoạt động")
        void getPost_Success() {
            UUID postId = UUID.randomUUID();
            Post post = Post.builder().id(postId).isDeleted(false).build();

            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            mockCommonMappingDependencies();

            PostResponse result = postService.getPost(postId);
            assertNotNull(result);
        }

        @Test
        @DisplayName("Thất bại - ID bài viết không tồn tại trong hệ thống")
        void getPost_NotFound() {
            UUID postId = UUID.randomUUID();
            when(postRepository.findById(postId)).thenReturn(Optional.empty());

            assertThrows(PostNotFoundException.class, () -> postService.getPost(postId));
        }

        @Test
        @DisplayName("Thất bại - Bài viết đã bị đánh dấu xóa (isDeleted = true)")
        void getPost_PostDeleted() {
            UUID postId = UUID.randomUUID();
            Post deletedPost = Post.builder().id(postId).isDeleted(true).build();

            when(postRepository.findById(postId)).thenReturn(Optional.of(deletedPost));

            PostNotFoundException exception = assertThrows(PostNotFoundException.class, () -> postService.getPost(postId));
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
        @DisplayName("Thành công - Cập nhật bài viết")
        void updatePost_Success() {
            UUID postId = UUID.randomUUID();
            User user = User.builder().id(UUID.randomUUID()).username(MOCK_USERNAME).build();
            Post post = Post.builder().id(postId).user(user).isDeleted(false).build();
            PostRequest request = new PostRequest();
            request.setContent("Updated content");

            when(userRepository.findByUsername(MOCK_USERNAME)).thenReturn(Optional.of(user));
            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            when(postRepository.save(any(Post.class))).thenReturn(post);

            mockCommonMappingDependencies();

            PostResponse result = postService.updatePost(postId, request);
            assertNotNull(result);
        }

        @Test
        @DisplayName("Thất bại - Không phải chủ bài viết")
        void updatePost_AccessDenied() {
            UUID postId = UUID.randomUUID();
            User owner = User.builder().id(UUID.randomUUID()).build();
            User currentUser = User.builder().id(UUID.randomUUID()).build();
            Post post = Post.builder().id(postId).user(owner).build();

            when(userRepository.findByUsername(MOCK_USERNAME)).thenReturn(Optional.of(currentUser));
            when(postRepository.findById(postId)).thenReturn(Optional.of(post));

            assertThrows(PostAccessDeniedException.class, () -> postService.updatePost(postId, new PostRequest()));
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
            User user = User.builder().id(UUID.randomUUID()).username(MOCK_USERNAME).build();
            Post post = Post.builder().id(postId).user(user).isDeleted(false).build();

            when(userRepository.findByUsername(MOCK_USERNAME)).thenReturn(Optional.of(user));
            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

            postService.deletePost(postId);

            assertTrue(post.isDeleted());
        }

        @Test
        @DisplayName("Thất bại - Không tìm thấy User đang thao tác")
        void deletePost_UserNotFound() {
            UUID postId = UUID.randomUUID();
            when(userRepository.findByUsername(MOCK_USERNAME)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class, () -> postService.deletePost(postId));
        }

        @Test
        @DisplayName("Thất bại - Không tìm thấy bài viết cần xóa")
        void deletePost_PostNotFound() {
            UUID postId = UUID.randomUUID();
            User user = User.builder().id(UUID.randomUUID()).username(MOCK_USERNAME).build();

            when(userRepository.findByUsername(MOCK_USERNAME)).thenReturn(Optional.of(user));
            when(postRepository.findById(postId)).thenReturn(Optional.empty());

            assertThrows(PostNotFoundException.class, () -> postService.deletePost(postId));
        }

        @Test
        @DisplayName("Thất bại - Không có quyền xóa bài viết của người khác")
        void deletePost_AccessDenied() {
            UUID postId = UUID.randomUUID();
            User currentUser = User.builder().id(UUID.randomUUID()).username(MOCK_USERNAME).build();
            User postOwner = User.builder().id(UUID.randomUUID()).username("other_user").build();
            Post post = Post.builder().id(postId).user(postOwner).isDeleted(false).build();

            when(userRepository.findByUsername(MOCK_USERNAME)).thenReturn(Optional.of(currentUser));
            when(postRepository.findById(postId)).thenReturn(Optional.of(post));

            PostAccessDeniedException exception = assertThrows(PostAccessDeniedException.class, () -> postService.deletePost(postId));
            assertEquals("delete", exception.getMessage());
        }
    }

    // ==========================================================
    // GET USER FEED & BUILD CURSOR RESPONSE
    // ==========================================================
    @Nested
    @DisplayName("Test getUserFeed() & buildCursorResponse()")
    class GetUserFeedTest {

        @Test
        @DisplayName("Thành công - Số lượng bài viết vượt quá 'size' (Có trang kế tiếp)")
        void getUserFeed_HasNextPage_Success() {
            int size = 2;
            UUID cursor = UUID.randomUUID();

            Post post1 = Post.builder().id(UUID.randomUUID()).content("Post 1").build();
            Post post2 = Post.builder().id(UUID.randomUUID()).content("Post 2").build();
            Post post3 = Post.builder().id(UUID.randomUUID()).content("Post thừa").build();

            List<Post> mockFeed = new ArrayList<>(List.of(post1, post2, post3));

            when(postRepository.findPublicFeed(eq(cursor), eq(Limit.of(size + 1)))).thenReturn(mockFeed);

            mockCommonMappingDependencies();

            CursorResponse<PostResponse> response = postService.getUserFeed(cursor, size);

            assertNotNull(response);
            assertTrue(response.isHasNext());
            assertEquals(2, response.getContent().size());
            assertEquals(post2.getId().toString(), response.getNextCursor());
        }

        @Test
        @DisplayName("Thành công - Số lượng bài viết nhỏ hơn hoặc bằng 'size' (Là trang cuối cùng)")
        void getUserFeed_IsLastPage_Success() {
            int size = 5;

            Post post1 = Post.builder().id(UUID.randomUUID()).content("Post 1").build();
            Post post2 = Post.builder().id(UUID.randomUUID()).content("Post 2").build();

            List<Post> mockFeed = new ArrayList<>(List.of(post1, post2));

            when(postRepository.findPublicFeed(isNull(), eq(Limit.of(size + 1)))).thenReturn(mockFeed);

            mockCommonMappingDependencies();

            CursorResponse<PostResponse> response = postService.getUserFeed(null, size);

            assertNotNull(response);
            assertFalse(response.isHasNext());
            assertEquals(2, response.getContent().size());
            assertEquals(post2.getId().toString(), response.getNextCursor());
        }

        @Test
        @DisplayName("Thành công - Feed trống rỗng không có bài viết nào")
        void getUserFeed_EmptyFeed_Success() {
            int size = 10;

            when(postRepository.findPublicFeed(isNull(), eq(Limit.of(size + 1)))).thenReturn(new ArrayList<>());

            CursorResponse<PostResponse> response = postService.getUserFeed(null, size);

            assertNotNull(response);
            assertFalse(response.isHasNext());
            assertNull(response.getNextCursor());
            assertTrue(response.getContent().isEmpty());
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
            mockHashtagEntity = Hashtag.builder()
                    .id(UUID.randomUUID())
                    .name(VALID_TAG)
                    .build();
        }

        @Test
        @DisplayName("Thất bại - Không tìm thấy thẻ Hashtag trong hệ thống")
        void getPostsByHashtag_HashtagNotFound_ThrowsException() {
            // Arrange
            int size = 5;
            when(hashtagRepository.findByName(INVALID_TAG)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(
                    com.connecthub.modules.features.post.exception.HashtagNotFoundException.class,
                    () -> postService.getPostsByHashtag(INVALID_TAG, null, size)
            );

            verify(hashtagRepository).findByName(INVALID_TAG);
            verifyNoInteractions(postHashtagRepository); // Đảm bảo không truy vấn bảng PostHashtag khi tag lỗi
        }

        @Test
        @DisplayName("Thành công - Số bài viết tìm thấy lớn hơn 'size' (Có trang kế tiếp)")
        void getPostsByHashtag_HasNextPage_Success() {
            // Arrange
            int size = 2; // Yêu cầu lấy 2 bài viết
            UUID cursor = UUID.randomUUID();

            Post post1 = Post.builder().id(UUID.randomUUID()).content("Post 1").build();
            Post post2 = Post.builder().id(UUID.randomUUID()).content("Post 2").build();
            Post post3 = Post.builder().id(UUID.randomUUID()).content("Post 3 (phần tử thừa)").build();

            PostHashtag ph1 = PostHashtag.builder().post(post1).hashtag(mockHashtagEntity).build();
            PostHashtag ph2 = PostHashtag.builder().post(post2).hashtag(mockHashtagEntity).build();
            PostHashtag ph3 = PostHashtag.builder().post(post3).hashtag(mockHashtagEntity).build();

            // Giả lập DB trả về size + 1 phần tử (3 phần tử)
            List<PostHashtag> mockPostHashtags = new ArrayList<>(List.of(ph1, ph2, ph3));

            when(hashtagRepository.findByName(VALID_TAG)).thenReturn(Optional.of(mockHashtagEntity));
            when(postHashtagRepository.findPostsByHashtagId(eq(mockHashtagEntity.getId()), eq(cursor), eq(Limit.of(size + 1))))
                    .thenReturn(mockPostHashtags);

            mockCommonMappingDependencies();

            // Act
            CursorResponse<PostResponse> response = postService.getPostsByHashtag(VALID_TAG, cursor, size);

            // Assert
            assertNotNull(response);
            assertTrue(response.isHasNext(), "hasNext phải bằng true vì DB trả về 3 > size(2)");
            assertEquals(2, response.getContent().size(), "Danh sách kết quả trả ra cho client phải được rút gọn về đúng size (2)");
            assertEquals(post2.getId().toString(), response.getNextCursor(), "nextCursor phải là ID của Post2 (phần tử thực tế cuối cùng sau khi cắt)");

            verify(hashtagRepository).findByName(VALID_TAG);
            verify(postHashtagRepository).findPostsByHashtagId(eq(mockHashtagEntity.getId()), eq(cursor), eq(Limit.of(size + 1)));
        }

        @Test
        @DisplayName("Thành công - Số bài viết tìm thấy nhỏ hơn hoặc bằng 'size' (Trang cuối cùng)")
        void getPostsByHashtag_IsLastPage_Success() {
            // Arrange
            int size = 5; // Client xin tối đa 5 bài viết

            Post post1 = Post.builder().id(UUID.randomUUID()).content("Post 1").build();
            PostHashtag ph1 = PostHashtag.builder().post(post1).hashtag(mockHashtagEntity).build();

            // Giả lập DB chỉ có 1 bài viết độc nhất phù hợp
            List<PostHashtag> mockPostHashtags = new ArrayList<>(List.of(ph1));

            when(hashtagRepository.findByName(VALID_TAG)).thenReturn(Optional.of(mockHashtagEntity));
            when(postHashtagRepository.findPostsByHashtagId(eq(mockHashtagEntity.getId()), isNull(), eq(Limit.of(size + 1))))
                    .thenReturn(mockPostHashtags);

            mockCommonMappingDependencies();

            // Act
            CursorResponse<PostResponse> response = postService.getPostsByHashtag(VALID_TAG, null, size);

            // Assert
            assertNotNull(response);
            assertFalse(response.isHasNext(), "hasNext phải bằng false vì DB trả ra ít hơn hoặc bằng size mong muốn");
            assertEquals(1, response.getContent().size());
            assertEquals(post1.getId().toString(), response.getNextCursor(), "nextCursor vẫn trỏ vào ID của bài viết cuối");
        }

        @Test
        @DisplayName("Thành công - Không có bất kỳ bài viết nào gắn Hashtag này")
        void getPostsByHashtag_EmptyResult_Success() {
            // Arrange
            int size = 5;

            when(hashtagRepository.findByName(VALID_TAG)).thenReturn(Optional.of(mockHashtagEntity));
            when(postHashtagRepository.findPostsByHashtagId(eq(mockHashtagEntity.getId()), isNull(), eq(Limit.of(size + 1))))
                    .thenReturn(new ArrayList<>()); // Trả về list trống rỗng

            // Act
            CursorResponse<PostResponse> response = postService.getPostsByHashtag(VALID_TAG, null, size);

            // Assert
            assertNotNull(response);
            assertFalse(response.isHasNext());
            assertNull(response.getNextCursor(), "Mảng trống thì nextCursor bắt buộc phải bằng null");
            assertTrue(response.getContent().isEmpty());

            // Đảm bảo không map hay tính toán số lượng comment/reaction khi danh sách rỗng
            verify(postMapper, never()).mapToResponse(any(), any(), anyLong(), anyLong(), anyLong(), anyLong(), anyLong());
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
            mockPost = Post.builder()
                    .id(postId)
                    .content("Learning Unit Test in Java")
                    .build();
        }

        @Test
        @DisplayName("Thất bại - Không tìm thấy bài viết cần gắn hashtag")
        void addHashtagToPost_PostNotFound_ThrowsException() {
            // Arrange
            when(postRepository.findById(postId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(
                    com.connecthub.modules.features.post.exception.PostNotFoundException.class,
                    () -> postService.addHashtagToPost(postId, TAG_NAME)
            );

            verify(postRepository).findById(postId);
            verifyNoInteractions(hashtagRepository, postHashtagRepository); // Đảm bảo không xử lý hashtag nếu post lỗi
        }

        @Test
        @DisplayName("Thành công - Hashtag chưa tồn tại trong hệ thống (Tạo mới Hashtag & Liên kết)")
        void addHashtagToPost_HashtagNotExists_CreatesNewAndSaves() {
            // Arrange
            Hashtag savedHashtag = Hashtag.builder().id(UUID.randomUUID()).name(TAG_NAME).build();

            when(postRepository.findById(postId)).thenReturn(Optional.of(mockPost));
            // Ép rẽ vào nhánh tạo mới .orElseGet() khi tìm không ra tên hashtag
            when(hashtagRepository.findByName(TAG_NAME)).thenReturn(Optional.empty());
            when(hashtagRepository.save(any(Hashtag.class))).thenReturn(savedHashtag);

            // Ép rẽ vào nhánh tạo liên kết mới khi kiểm tra chưa tồn tại
            when(postHashtagRepository.existsByPostIdAndHashtagId(postId, savedHashtag.getId())).thenReturn(false);

            // Act
            postService.addHashtagToPost(postId, TAG_NAME);

            // Assert
            verify(postRepository).findById(postId);
            verify(hashtagRepository).findByName(TAG_NAME);
            verify(hashtagRepository).save(any(Hashtag.class)); // Xác nhận hệ thống có tạo mới tag
            verify(postHashtagRepository).existsByPostIdAndHashtagId(postId, savedHashtag.getId());
            verify(postHashtagRepository).save(any(PostHashtag.class)); // Xác nhận có lưu mối liên kết
        }

        @Test
        @DisplayName("Thành công - Hashtag đã tồn tại nhưng bài viết chưa gắn (Chỉ tạo mới Liên kết)")
        void addHashtagToPost_HashtagExistsButNotAttached_SavesOnlyLink() {
            // Arrange
            Hashtag existingHashtag = Hashtag.builder().id(UUID.randomUUID()).name(TAG_NAME).build();

            when(postRepository.findById(postId)).thenReturn(Optional.of(mockPost));
            // Đã tìm thấy Hashtag trong DB
            when(hashtagRepository.findByName(TAG_NAME)).thenReturn(Optional.of(existingHashtag));
            // Cặp liên kết chưa tồn tại
            when(postHashtagRepository.existsByPostIdAndHashtagId(postId, existingHashtag.getId())).thenReturn(false);

            // Act
            postService.addHashtagToPost(postId, TAG_NAME);

            // Assert
            verify(postRepository).findById(postId);
            verify(hashtagRepository).findByName(TAG_NAME);
            verify(hashtagRepository, never()).save(any(Hashtag.class)); // Không được phép lưu trùng hashtag cũ
            verify(postHashtagRepository).existsByPostIdAndHashtagId(postId, existingHashtag.getId());
            verify(postHashtagRepository).save(any(PostHashtag.class)); // Lưu liên kết mới bình thường
        }

        @Test
        @DisplayName("Thành công - Bài viết đã được gắn hashtag này từ trước (Bỏ qua, không lưu trùng)")
        void addHashtagToPost_AlreadyAttached_ReturnsEarly() {
            // Arrange
            Hashtag existingHashtag = Hashtag.builder().id(UUID.randomUUID()).name(TAG_NAME).build();

            when(postRepository.findById(postId)).thenReturn(Optional.of(mockPost));
            when(hashtagRepository.findByName(TAG_NAME)).thenReturn(Optional.of(existingHashtag));
            // Cố tình giả lập mối quan hệ đã được tạo từ trước trong DB (exists = true)
            when(postHashtagRepository.existsByPostIdAndHashtagId(postId, existingHashtag.getId())).thenReturn(true);

            // Act
            postService.addHashtagToPost(postId, TAG_NAME);

            // Assert
            verify(postRepository).findById(postId);
            verify(hashtagRepository).findByName(TAG_NAME);
            verify(postHashtagRepository).existsByPostIdAndHashtagId(postId, existingHashtag.getId());

            // Logic phải return early ở đây, tuyệt đối không gọi hành động save trùng lặp xuống DB
            verify(postHashtagRepository, never()).save(any(PostHashtag.class));
        }
    }
    // ==========================================================
    // CREATE REPLY & GET REPLIES
    // ==========================================================
    @Nested
    @DisplayName("Test createReply() & getReplies()")
    class ReplyAndFeedRepliesTest {

        private UUID parentPostId;
        private Post parentPost;
        private PostRequest replyRequest;

        @BeforeEach
        void setUp() {
            parentPostId = UUID.randomUUID();
            parentPost = Post.builder()
                    .id(parentPostId)
                    .content("Bài viết gốc")
                    .build();

            replyRequest = new PostRequest();
            replyRequest.setContent("Đây là bài viết phản hồi");
        }

        // --- TEST CASES CHO CREATE REPLY ---

        @Test
        @DisplayName("createReply() - Thất bại - Không tìm thấy bài viết cha")
        void createReply_ParentPostNotFound_ThrowsException() {
            // Arrange
            when(postRepository.findById(parentPostId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(
                    com.connecthub.modules.features.post.exception.PostNotFoundException.class,
                    () -> postService.createReply(parentPostId, replyRequest)
            );

            verify(postRepository).findById(parentPostId);
            verify(postRepository, never()).save(any(Post.class)); // Không tiến hành tạo nếu bài gốc không có
        }

        @Test
        @DisplayName("createReply() - Thành công - Tạo phản hồi cho bài viết hợp lệ")
        void createReply_Success() {
            // Arrange
            User currentUser = User.builder().id(UUID.randomUUID()).username(MOCK_USERNAME).build();
            Post mockReplyPost = new Post();
            Post savedReply = Post.builder()
                    .id(UUID.randomUUID())
                    .parentPost(parentPost)
                    .content("Đây là bài viết phản hồi")
                    .user(currentUser)
                    .build();

            // Mock cho getPostOrThrow(parentPostId) dòng đầu tiên trong hàm createReply
            when(postRepository.findById(parentPostId)).thenReturn(Optional.of(parentPost));

            // Mock luồng chạy bên trong của hàm createPost(request) được gọi kế tiếp
            when(userRepository.findByUsername(MOCK_USERNAME)).thenReturn(Optional.of(currentUser));
            when(postMapper.toPost(any(PostRequest.class))).thenReturn(mockReplyPost);
            when(postRepository.save(any(Post.class))).thenReturn(savedReply);

            mockCommonMappingDependencies();

            // Act
            PostResponse response = postService.createReply(parentPostId, replyRequest);

            // Assert
            assertNotNull(response);
            assertEquals(parentPostId, replyRequest.getParentPostId(), "parentPostId phải được set vào request trước khi tạo");

            // Xác minh hàm tìm bài viết gốc được gọi 2 lần (1 lần ở createReply, 1 lần ở logic nhánh parentPostId của createPost)
            verify(postRepository, times(2)).findById(parentPostId);
            verify(postRepository).save(any(Post.class));
        }

        // --- TEST CASES CHO GET REPLIES (CURSOR PAGINATION) ---

        @Test
        @DisplayName("getReplies() - Thất bại - Bài viết gốc cần lấy phản hồi không tồn tại")
        void getReplies_TargetPostNotFound_ThrowsException() {
            // Arrange
            int size = 5;
            when(postRepository.findById(parentPostId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(
                    com.connecthub.modules.features.post.exception.PostNotFoundException.class,
                    () -> postService.getReplies(parentPostId, null, size)
            );

            verify(postRepository).findById(parentPostId);
            verify(postRepository, never()).findRepliesByParentPostId(any(), any(), any());
        }

        @Test
        @DisplayName("getReplies() - Thành công - Có trang kế tiếp (Kích thước trả về từ DB > size)")
        void getReplies_HasNextPage_Success() {
            // Arrange
            int size = 2;
            UUID cursor = UUID.randomUUID();

            Post reply1 = Post.builder().id(UUID.randomUUID()).content("Reply 1").build();
            Post reply2 = Post.builder().id(UUID.randomUUID()).content("Reply 2").build();
            Post reply3 = Post.builder().id(UUID.randomUUID()).content("Reply thừa để test hasNext").build();
            List<Post> mockReplies = new ArrayList<>(List.of(reply1, reply2, reply3));

            when(postRepository.findById(parentPostId)).thenReturn(Optional.of(parentPost));
            when(postRepository.findRepliesByParentPostId(eq(parentPostId), eq(cursor), eq(Limit.of(size + 1))))
                    .thenReturn(mockReplies);

            mockCommonMappingDependencies();

            // Act
            CursorResponse<PostResponse> response = postService.getReplies(parentPostId, cursor, size);

            // Assert
            assertNotNull(response);
            assertTrue(response.isHasNext(), "hasNext phải bằng true vì DB trả về 3 bản ghi > size yêu cầu (2)");
            assertEquals(2, response.getContent().size(), "Danh sách kết quả phải bị cắt bớt phần tử thừa cuối cùng");
            assertEquals(reply2.getId().toString(), response.getNextCursor(), "nextCursor phải trỏ vào ID của phần tử thực tế cuối (Reply 2)");

            verify(postRepository).findRepliesByParentPostId(eq(parentPostId), eq(cursor), eq(Limit.of(size + 1)));
        }

        @Test
        @DisplayName("getReplies() - Thành công - Là trang cuối cùng (Kích thước trả về từ DB <= size)")
        void getReplies_IsLastPage_Success() {
            // Arrange
            int size = 5;

            Post reply1 = Post.builder().id(UUID.randomUUID()).content("Reply 1").build();
            List<Post> mockReplies = new ArrayList<>(List.of(reply1));

            when(postRepository.findById(parentPostId)).thenReturn(Optional.of(parentPost));
            when(postRepository.findRepliesByParentPostId(eq(parentPostId), isNull(), eq(Limit.of(size + 1))))
                    .thenReturn(mockReplies);

            mockCommonMappingDependencies();

            // Act
            CursorResponse<PostResponse> response = postService.getReplies(parentPostId, null, size);

            // Assert
            assertNotNull(response);
            assertFalse(response.isHasNext(), "hasNext phải bằng false vì đây là trang cuối");
            assertEquals(1, response.getContent().size());
            assertEquals(reply1.getId().toString(), response.getNextCursor());
        }

        @Test
        @DisplayName("getReplies() - Thành công - Bài viết gốc chưa có phản hồi nào (Trả về mảng rỗng)")
        void getReplies_EmptyReplies_Success() {
            // Arrange
            int size = 10;

            when(postRepository.findById(parentPostId)).thenReturn(Optional.of(parentPost));
            when(postRepository.findRepliesByParentPostId(eq(parentPostId), isNull(), eq(Limit.of(size + 1))))
                    .thenReturn(new ArrayList<>()); // Trả về mảng rỗng

            // Act
            CursorResponse<PostResponse> response = postService.getReplies(parentPostId, null, size);

            // Assert
            assertNotNull(response);
            assertFalse(response.isHasNext());
            assertNull(response.getNextCursor(), "Mảng trống thì nextCursor bắt buộc phải bằng null");
            assertTrue(response.getContent().isEmpty());

            // Chắc chắn không chạy qua các hàm tính tương tác (like, comment, share) khi mảng phản hồi trống rỗng
            verify(postMapper, never()).mapToResponse(any(), any(), anyLong(), anyLong(), anyLong(), anyLong(), anyLong());
        }
    }
    // ==========================================================
    // GET REPLIES (CURSOR PAGINATION)
    // ==========================================================
    @Nested
    @DisplayName("Test getReplies()")
    class GetRepliesTest {

        private UUID parentPostId;
        private Post parentPost;

        @BeforeEach
        void setUp() {
            parentPostId = UUID.randomUUID();
            parentPost = Post.builder()
                    .id(parentPostId)
                    .content("Bài viết gốc")
                    .build();
        }

        @Test
        @DisplayName("Thất bại - Bài viết gốc cần lấy phản hồi không tồn tại trong hệ thống")
        void getReplies_TargetPostNotFound_ThrowsException() {
            // Arrange
            int size = 5;
            when(postRepository.findById(parentPostId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(
                    com.connecthub.modules.features.post.exception.PostNotFoundException.class,
                    () -> postService.getReplies(parentPostId, null, size)
            );

            verify(postRepository).findById(parentPostId);
            verify(postRepository, never()).findRepliesByParentPostId(any(), any(), any());
        }

        @Test
        @DisplayName("Thành công - Số lượng phản hồi vượt quá 'size' (Có trang kế tiếp)")
        void getReplies_HasNextPage_Success() {
            // Arrange
            int size = 2; // Yêu cầu lấy 2 bản ghi
            UUID cursor = UUID.randomUUID();

            Post reply1 = Post.builder().id(UUID.randomUUID()).content("Reply 1").build();
            Post reply2 = Post.builder().id(UUID.randomUUID()).content("Reply 2").build();
            Post reply3 = Post.builder().id(UUID.randomUUID()).content("Reply thừa để test hasNext").build();

            // Giả lập Database trả về size + 1 phần tử (3 phần tử)
            List<Post> mockReplies = new ArrayList<>(List.of(reply1, reply2, reply3));

            when(postRepository.findById(parentPostId)).thenReturn(Optional.of(parentPost));
            when(postRepository.findRepliesByParentPostId(eq(parentPostId), eq(cursor), eq(Limit.of(size + 1))))
                    .thenReturn(mockReplies);

            mockCommonMappingDependencies();

            // Act
            CursorResponse<PostResponse> response = postService.getReplies(parentPostId, cursor, size);

            // Assert
            assertNotNull(response);
            assertTrue(response.isHasNext(), "hasNext phải bằng true vì DB trả về 3 > size(2)");
            assertEquals(2, response.getContent().size(), "Danh sách kết quả trả về phải bị cắt bớt phần tử thừa cuối cùng");
            assertEquals(reply2.getId().toString(), response.getNextCursor(), "nextCursor phải trỏ vào ID của phần tử thực tế cuối cùng (Reply 2)");

            verify(postRepository).findById(parentPostId);
            verify(postRepository).findRepliesByParentPostId(eq(parentPostId), eq(cursor), eq(Limit.of(size + 1)));
        }

        @Test
        @DisplayName("Thành công - Số lượng phản hồi nhỏ hơn hoặc bằng 'size' (Là trang cuối cùng)")
        void getReplies_IsLastPage_Success() {
            // Arrange
            int size = 5; // Xin tối đa 5 bản ghi

            Post reply1 = Post.builder().id(UUID.randomUUID()).content("Reply 1").build();
            List<Post> mockReplies = new ArrayList<>(List.of(reply1)); // Chỉ có 1 bản ghi từ DB

            when(postRepository.findById(parentPostId)).thenReturn(Optional.of(parentPost));
            when(postRepository.findRepliesByParentPostId(eq(parentPostId), isNull(), eq(Limit.of(size + 1))))
                    .thenReturn(mockReplies);

            mockCommonMappingDependencies();

            // Act
            CursorResponse<PostResponse> response = postService.getReplies(parentPostId, null, size);

            // Assert
            assertNotNull(response);
            assertFalse(response.isHasNext(), "hasNext phải bằng false vì DB trả về ít hơn hoặc bằng size mong muốn");
            assertEquals(1, response.getContent().size());
            assertEquals(reply1.getId().toString(), response.getNextCursor(), "nextCursor vẫn trỏ vào phần tử cuối cùng để Client lưu vết");
        }

        @Test
        @DisplayName("Thành công - Bài viết gốc chưa có phản hồi nào (Trả về mảng rỗng)")
        void getReplies_EmptyReplies_Success() {
            // Arrange
            int size = 10;

            when(postRepository.findById(parentPostId)).thenReturn(Optional.of(parentPost));
            when(postRepository.findRepliesByParentPostId(eq(parentPostId), isNull(), eq(Limit.of(size + 1))))
                    .thenReturn(new ArrayList<>()); // Trả về list trống

            // Act
            CursorResponse<PostResponse> response = postService.getReplies(parentPostId, null, size);

            // Assert
            assertNotNull(response);
            assertFalse(response.isHasNext());
            assertNull(response.getNextCursor(), "Mảng trống rỗng thì nextCursor bắt buộc phải bằng null");
            assertTrue(response.getContent().isEmpty());

            // Chắc chắn không chạy qua ánh xạ mapToResponse khi danh sách rỗng
            verify(postMapper, never()).mapToResponse(any(), any(), anyLong(), anyLong(), anyLong(), anyLong(), anyLong());
        }
    }
}