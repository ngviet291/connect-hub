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
        mockedAppUtil.when(AppUtil::userIdFormAuthentication).thenReturn(MOCK_USER_ID);
        mockedAppUtil.when(AppUtil::generateUUID).thenReturn(UUID.randomUUID());
    }

    @AfterEach
    void tearDown() {
        mockedAppUtil.close();
    }

    // Helper: stub postMapper.mapToResponse trả về PostResponse rỗng
    private void stubMapToResponse() {
        when(postMapper.mapToResponse(any(Post.class))).thenReturn(new PostResponse());
    }

    // Helper: tạo Post đang active của MOCK_USER_ID
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
        @DisplayName("Thành công - Bài viết có parentPostId (reply/comment)")
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
        @DisplayName("Thất bại - Parent Post đã bị xóa mềm")
        void createPost_ParentPostDeleted_ThrowsPostNotFoundException() {
            User user = User.builder().id(MOCK_USER_ID).build();
            UUID parentId = UUID.randomUUID();
            Post deletedParent = Post.builder().id(parentId).build();
            deletedParent.setDeleted(true);
            PostRequest request = PostRequest.builder().parentPostId(parentId).build();

            when(userRepository.findById(MOCK_USER_ID)).thenReturn(Optional.of(user));
            when(postMapper.toPost(request)).thenReturn(new Post());
            when(postRepository.findById(parentId)).thenReturn(Optional.of(deletedParent));

            assertThrows(PostNotFoundException.class, () -> postService.createPost(request));
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

            PostResponse result = postService.createPost(request);

            assertNotNull(result);
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
        @DisplayName("Thành công - Bài viết có hashtags (hashtag mới chưa tồn tại → tạo mới)")
        void createPost_WithNewHashtags_CreatesAndLinks() {
            User user = User.builder().id(MOCK_USER_ID).build();
            PostRequest request = PostRequest.builder()
                    .content("Hello #Java")
                    .hashtags(List.of("Java"))
                    .build();
            Post mockPost = new Post();
            Post savedPost = Post.builder().id(UUID.randomUUID()).user(user).build();
            Hashtag newHashtag = Hashtag.builder().id(UUID.randomUUID()).name("java").build();

            when(userRepository.findById(MOCK_USER_ID)).thenReturn(Optional.of(user));
            when(postMapper.toPost(request)).thenReturn(mockPost);
            when(postRepository.save(any(Post.class))).thenReturn(savedPost);
            when(hashtagRepository.findByName("java")).thenReturn(Optional.empty());
            when(hashtagRepository.save(any(Hashtag.class))).thenReturn(newHashtag);
            when(postHashtagRepository.existsByPostIdAndHashtagId(any(), any())).thenReturn(false);
            when(postHashtagRepository.save(any(PostHashtag.class)))
                    .thenReturn(PostHashtag.builder().hashtag(newHashtag).post(savedPost).build());
            stubMapToResponse();

            PostResponse result = postService.createPost(request);

            assertNotNull(result);
            verify(hashtagRepository).findByName("java"); // lowercase normalization
            verify(hashtagRepository).save(any(Hashtag.class));
            verify(postHashtagRepository).save(any(PostHashtag.class));
        }

        @Test
        @DisplayName("Thành công - Bài viết có hashtags (hashtag đã tồn tại → chỉ tạo liên kết)")
        void createPost_WithExistingHashtag_OnlyLinks() {
            User user = User.builder().id(MOCK_USER_ID).build();
            PostRequest request = PostRequest.builder()
                    .content("Hello #spring")
                    .hashtags(List.of("spring"))
                    .build();
            Post mockPost = new Post();
            Post savedPost = Post.builder().id(UUID.randomUUID()).user(user).build();
            Hashtag existingHashtag = Hashtag.builder().id(UUID.randomUUID()).name("spring").build();

            when(userRepository.findById(MOCK_USER_ID)).thenReturn(Optional.of(user));
            when(postMapper.toPost(request)).thenReturn(mockPost);
            when(postRepository.save(any(Post.class))).thenReturn(savedPost);
            when(hashtagRepository.findByName("spring")).thenReturn(Optional.of(existingHashtag));
            when(postHashtagRepository.existsByPostIdAndHashtagId(any(), eq(existingHashtag.getId()))).thenReturn(false);
            when(postHashtagRepository.save(any(PostHashtag.class)))
                    .thenReturn(PostHashtag.builder().hashtag(existingHashtag).post(savedPost).build());
            stubMapToResponse();

            postService.createPost(request);

            verify(hashtagRepository, never()).save(any()); // không tạo hashtag mới
            verify(postHashtagRepository).save(any(PostHashtag.class));
        }

        @Test
        @DisplayName("Thành công - Bài viết có mentions (mention username hợp lệ)")
        void createPost_WithMentions_Success() {
            User user = User.builder().id(MOCK_USER_ID).build();
            User mentionedUser = User.builder().id(UUID.randomUUID()).username("john_doe").build();
            PostRequest request = PostRequest.builder()
                    .content("Hello @john_doe")
                    .mentionUsernames(List.of("john_doe"))
                    .build();
            Post mockPost = new Post();
            Post savedPost = Post.builder().id(UUID.randomUUID()).user(user).build();
            Mention mention = Mention.builder().id(UUID.randomUUID()).post(savedPost).user(mentionedUser).build();

            when(userRepository.findById(MOCK_USER_ID)).thenReturn(Optional.of(user));
            when(postMapper.toPost(request)).thenReturn(mockPost);
            when(postRepository.save(any(Post.class))).thenReturn(savedPost);
            when(userRepository.findExactByUsername("john_doe")).thenReturn(Optional.of(mentionedUser));
            when(mentionRepository.saveAll(anyList())).thenReturn(List.of(mention));
            stubMapToResponse();

            PostResponse result = postService.createPost(request);

            assertNotNull(result);
            verify(userRepository).findExactByUsername("john_doe");
            verify(mentionRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("Thất bại - Mention username không tồn tại")
        void createPost_MentionedUserNotFound_ThrowsMentionedUserNotFoundException() {
            User user = User.builder().id(MOCK_USER_ID).build();
            PostRequest request = PostRequest.builder()
                    .content("Hello @ghost")
                    .mentionUsernames(List.of("ghost"))
                    .build();
            Post mockPost = new Post();
            Post savedPost = Post.builder().id(UUID.randomUUID()).user(user).build();

            when(userRepository.findById(MOCK_USER_ID)).thenReturn(Optional.of(user));
            when(postMapper.toPost(request)).thenReturn(mockPost);
            when(postRepository.save(any(Post.class))).thenReturn(savedPost);
            when(userRepository.findExactByUsername("ghost")).thenReturn(Optional.empty());

            assertThrows(MentionedUserNotFoundException.class, () -> postService.createPost(request));
        }

        @Test
        @DisplayName("Thành công - Bài viết có file media đính kèm")
        void createPost_WithMedia_UploadsAndAttaches() {
            User user = User.builder().id(MOCK_USER_ID).build();
            MultipartFile mockFile = mock(MultipartFile.class);
            PostRequest request = PostRequest.builder()
                    .content("Post with image")
                    .files(List.of(mockFile))
                    .build();
            Post mockPost = new Post();
            Post savedPost = Post.builder().id(UUID.randomUUID()).user(user).media(new HashSet<>()).build();
            Media uploadedMedia = Media.builder().id(UUID.randomUUID()).build();

            when(userRepository.findById(MOCK_USER_ID)).thenReturn(Optional.of(user));
            when(postMapper.toPost(request)).thenReturn(mockPost);
            when(postRepository.save(any(Post.class))).thenReturn(savedPost);
            when(mediaService.uploadAndAttachToPost(eq(List.of(mockFile)), any(Post.class)))
                    .thenReturn(List.of(uploadedMedia));
            stubMapToResponse();

            postService.createPost(request);

            verify(mediaService).uploadAndAttachToPost(eq(List.of(mockFile)), any(Post.class));
            assertTrue(savedPost.getMedia().contains(uploadedMedia));
        }

        @Test
        @DisplayName("Thất bại - User không tồn tại trong hệ thống")
        void createPost_UserNotFound_ThrowsUserNotFoundException() {
            when(userRepository.findById(MOCK_USER_ID)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class,
                    () -> postService.createPost(new PostRequest()));
            verify(postMapper, never()).toPost(any());
            verify(postRepository, never()).save(any());
        }

        @Test
        @DisplayName("Thành công - hashtag bị bỏ qua khi đã liên kết (không insert trùng)")
        void createPost_HashtagAlreadyLinked_SkipsDuplicate() {
            User user = User.builder().id(MOCK_USER_ID).build();
            Hashtag existingHashtag = Hashtag.builder().id(UUID.randomUUID()).name("java").build();
            Post mockPost = new Post();
            Post savedPost = Post.builder().id(UUID.randomUUID()).user(user).build();
            PostRequest request = PostRequest.builder()
                    .content("Hello #java")
                    .hashtags(List.of("java"))
                    .build();

            when(userRepository.findById(MOCK_USER_ID)).thenReturn(Optional.of(user));
            when(postMapper.toPost(request)).thenReturn(mockPost);
            when(postRepository.save(any(Post.class))).thenReturn(savedPost);
            when(hashtagRepository.findByName("java")).thenReturn(Optional.of(existingHashtag));
            when(postHashtagRepository.existsByPostIdAndHashtagId(any(), eq(existingHashtag.getId()))).thenReturn(true);
            stubMapToResponse();

            postService.createPost(request);

            verify(postHashtagRepository, never()).save(any()); // không lưu liên kết trùng
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
            post.setDeleted(false);

            when(postRepository.findByIdWithDetails(postId)).thenReturn(Optional.of(post));
            stubMapToResponse();

            PostResponse result = postService.getPost(postId);

            assertNotNull(result);
            verify(postRepository).findByIdWithDetails(postId);
            verify(postMapper).mapToResponse(post);
        }

        @Test
        @DisplayName("Thất bại - ID bài viết không tồn tại")
        void getPost_PostNotFound_ThrowsPostNotFoundException() {
            UUID postId = UUID.randomUUID();
            when(postRepository.findByIdWithDetails(postId)).thenReturn(Optional.empty());

            assertThrows(PostNotFoundException.class, () -> postService.getPost(postId));
        }

        @Test
        @DisplayName("Thất bại - Bài viết đã bị xóa mềm (isDeleted = true)")
        void getPost_DeletedPost_ThrowsPostNotFoundException() {
            UUID postId = UUID.randomUUID();
            Post deletedPost = Post.builder().id(postId).build();
            deletedPost.setDeleted(true);

            when(postRepository.findByIdWithDetails(postId)).thenReturn(Optional.of(deletedPost));

            assertThrows(PostNotFoundException.class, () -> postService.getPost(postId));
            verify(postMapper, never()).mapToResponse(any());
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

            when(postRepository.findByIdWithDetails(postId))
                    .thenReturn(Optional.of(post))  // lần 1: load để update
                    .thenReturn(Optional.of(post)); // lần 2: load để map response
            when(postRepository.save(any(Post.class))).thenReturn(post);
            stubMapToResponse();

            PostResponse result = postService.updatePost(postId, request);

            assertNotNull(result);
            assertEquals("Updated content", post.getContent());
            assertEquals(Visibility.PRIVATE, post.getVisibility());
            verify(postRepository).save(post);
        }

        @Test
        @DisplayName("Thành công - Chỉ gửi content (null visibility → giữ nguyên visibility cũ)")
        void updatePost_OnlyContent_VisibilityUnchanged() {
            UUID postId = UUID.randomUUID();
            Post post = activePostOwnedByCurrentUser(postId);
            post.setVisibility(Visibility.FOLLOWERS);
            UpdatePostRequest request = UpdatePostRequest.builder()
                    .content("Only content changed")
                    .visibility(null)
                    .build();

            when(postRepository.findByIdWithDetails(postId))
                    .thenReturn(Optional.of(post))
                    .thenReturn(Optional.of(post));
            when(postRepository.save(any(Post.class))).thenReturn(post);
            stubMapToResponse();

            postService.updatePost(postId, request);

            assertEquals("Only content changed", post.getContent());
            assertEquals(Visibility.FOLLOWERS, post.getVisibility()); // không đổi
        }

        @Test
        @DisplayName("Thành công - Cập nhật hashtags (thay toàn bộ hashtag cũ bằng mới)")
        void updatePost_ReplaceHashtags_DeletesOldAndAddsNew() {
            UUID postId = UUID.randomUUID();
            Post post = activePostOwnedByCurrentUser(postId);
            Hashtag newHashtag = Hashtag.builder().id(UUID.randomUUID()).name("newtag").build();
            UpdatePostRequest request = UpdatePostRequest.builder()
                    .hashtags(List.of("newtag"))
                    .build();

            when(postRepository.findByIdWithDetails(postId))
                    .thenReturn(Optional.of(post))
                    .thenReturn(Optional.of(post));
            when(postRepository.save(any(Post.class))).thenReturn(post);
            when(hashtagRepository.findByName("newtag")).thenReturn(Optional.of(newHashtag));
            when(postHashtagRepository.existsByPostIdAndHashtagId(any(), eq(newHashtag.getId()))).thenReturn(false);
            when(postHashtagRepository.save(any(PostHashtag.class)))
                    .thenReturn(PostHashtag.builder().post(post).hashtag(newHashtag).build());
            stubMapToResponse();

            postService.updatePost(postId, request);

            verify(postHashtagRepository).deleteByPostId(postId); // xóa hashtag cũ
            verify(postHashtagRepository).save(any(PostHashtag.class)); // thêm hashtag mới
        }

        @Test
        @DisplayName("Thành công - Gửi hashtags rỗng [] → xóa toàn bộ hashtag cũ, không thêm mới")
        void updatePost_EmptyHashtagList_DeletesAllHashtags() {
            UUID postId = UUID.randomUUID();
            Post post = activePostOwnedByCurrentUser(postId);
            UpdatePostRequest request = UpdatePostRequest.builder()
                    .hashtags(Collections.emptyList())
                    .build();

            when(postRepository.findByIdWithDetails(postId))
                    .thenReturn(Optional.of(post))
                    .thenReturn(Optional.of(post));
            when(postRepository.save(any(Post.class))).thenReturn(post);
            stubMapToResponse();

            postService.updatePost(postId, request);

            verify(postHashtagRepository).deleteByPostId(postId);
            verify(postHashtagRepository, never()).save(any()); // không thêm mới
        }

        @Test
        @DisplayName("Thành công - Không gửi hashtags (null) → giữ nguyên hashtag cũ")
        void updatePost_NullHashtags_KeepsExisting() {
            UUID postId = UUID.randomUUID();
            Post post = activePostOwnedByCurrentUser(postId);
            UpdatePostRequest request = UpdatePostRequest.builder()
                    .content("Only update content")
                    .hashtags(null)
                    .build();

            when(postRepository.findByIdWithDetails(postId))
                    .thenReturn(Optional.of(post))
                    .thenReturn(Optional.of(post));
            when(postRepository.save(any(Post.class))).thenReturn(post);
            stubMapToResponse();

            postService.updatePost(postId, request);

            verify(postHashtagRepository, never()).deleteByPostId(any()); // không xóa
            verify(postHashtagRepository, never()).save(any());
        }

        @Test
        @DisplayName("Thành công - Cập nhật mentions (thay toàn bộ mention cũ bằng mới)")
        void updatePost_ReplaceMentions_DeletesOldAndAddsNew() {
            UUID postId = UUID.randomUUID();
            Post post = activePostOwnedByCurrentUser(postId);
            User newMentioned = User.builder().id(UUID.randomUUID()).username("alice").build();
            Mention newMention = Mention.builder().id(UUID.randomUUID()).post(post).user(newMentioned).build();
            UpdatePostRequest request = UpdatePostRequest.builder()
                    .mentionUsernames(List.of("alice"))
                    .build();

            when(postRepository.findByIdWithDetails(postId))
                    .thenReturn(Optional.of(post))
                    .thenReturn(Optional.of(post));
            when(postRepository.save(any(Post.class))).thenReturn(post);
            when(userRepository.findExactByUsername("alice")).thenReturn(Optional.of(newMentioned));
            when(mentionRepository.saveAll(anyList())).thenReturn(List.of(newMention));
            stubMapToResponse();

            postService.updatePost(postId, request);

            verify(mentionRepository).deleteByPostId(postId); // xóa mention cũ
            verify(mentionRepository).saveAll(anyList());     // thêm mention mới
        }

        @Test
        @DisplayName("Thành công - Gửi mentionUsernames rỗng [] → xóa toàn bộ mention cũ")
        void updatePost_EmptyMentionList_DeletesAllMentions() {
            UUID postId = UUID.randomUUID();
            Post post = activePostOwnedByCurrentUser(postId);
            UpdatePostRequest request = UpdatePostRequest.builder()
                    .mentionUsernames(Collections.emptyList())
                    .build();

            when(postRepository.findByIdWithDetails(postId))
                    .thenReturn(Optional.of(post))
                    .thenReturn(Optional.of(post));
            when(postRepository.save(any(Post.class))).thenReturn(post);
            stubMapToResponse();

            postService.updatePost(postId, request);

            verify(mentionRepository).deleteByPostId(postId);
            verify(mentionRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("Thành công - Không gửi mentionUsernames (null) → giữ nguyên mention cũ")
        void updatePost_NullMentions_KeepsExisting() {
            UUID postId = UUID.randomUUID();
            Post post = activePostOwnedByCurrentUser(postId);
            UpdatePostRequest request = UpdatePostRequest.builder()
                    .content("Only update content")
                    .mentionUsernames(null)
                    .build();

            when(postRepository.findByIdWithDetails(postId))
                    .thenReturn(Optional.of(post))
                    .thenReturn(Optional.of(post));
            when(postRepository.save(any(Post.class))).thenReturn(post);
            stubMapToResponse();

            postService.updatePost(postId, request);

            verify(mentionRepository, never()).deleteByPostId(any());
            verify(mentionRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("Thất bại - Username mention mới không tồn tại")
        void updatePost_MentionedUserNotFound_ThrowsMentionedUserNotFoundException() {
            UUID postId = UUID.randomUUID();
            Post post = activePostOwnedByCurrentUser(postId);
            UpdatePostRequest request = UpdatePostRequest.builder()
                    .mentionUsernames(List.of("ghost_user"))
                    .build();

            when(postRepository.findByIdWithDetails(postId)).thenReturn(Optional.of(post));
            when(userRepository.findExactByUsername("ghost_user")).thenReturn(Optional.empty());

            assertThrows(MentionedUserNotFoundException.class,
                    () -> postService.updatePost(postId, request));
        }

        @Test
        @DisplayName("Thất bại - Bài viết không tồn tại")
        void updatePost_PostNotFound_ThrowsPostNotFoundException() {
            UUID postId = UUID.randomUUID();
            when(postRepository.findByIdWithDetails(postId)).thenReturn(Optional.empty());

            assertThrows(PostNotFoundException.class,
                    () -> postService.updatePost(postId, new UpdatePostRequest()));
        }

        @Test
        @DisplayName("Thất bại - Bài viết đã bị xóa mềm")
        void updatePost_DeletedPost_ThrowsPostNotFoundException() {
            UUID postId = UUID.randomUUID();
            User owner = User.builder().id(MOCK_USER_ID).build();
            Post post = Post.builder().id(postId).user(owner).build();
            post.setDeleted(true);

            when(postRepository.findByIdWithDetails(postId)).thenReturn(Optional.of(post));

            assertThrows(PostNotFoundException.class,
                    () -> postService.updatePost(postId, new UpdatePostRequest()));
            verify(postRepository, never()).save(any());
        }

        @Test
        @DisplayName("Thất bại - Không phải chủ bài viết")
        void updatePost_NotOwner_ThrowsPostAccessDeniedException() {
            UUID postId = UUID.randomUUID();
            User otherUser = User.builder().id(UUID.randomUUID()).build();
            Post post = Post.builder().id(postId).user(otherUser).build();
            post.setDeleted(false);

            when(postRepository.findByIdWithDetails(postId)).thenReturn(Optional.of(post));

            assertThrows(PostAccessDeniedException.class,
                    () -> postService.updatePost(postId, new UpdatePostRequest()));
            verify(postRepository, never()).save(any());
        }
    }

    // =====================================================================
    // DELETE POST
    // =====================================================================
    @Nested
    @DisplayName("deletePost()")
    class DeletePostTest {

        @Test
        @DisplayName("Thành công - Xóa mềm bài viết của chính mình (isDeleted = true)")
        void deletePost_OwnPost_SetsDeletedTrue() {
            UUID postId = UUID.randomUUID();
            User user = User.builder().id(MOCK_USER_ID).build();
            Post post = Post.builder().id(postId).user(user).build();
            post.setDeleted(false);

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
            Post post = Post.builder().id(postId).user(user).parentPost(parentPost).build();
            post.setDeleted(false);

            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

            postService.deletePost(postId);

            assertTrue(post.isDeleted());
            verify(postRepository).decrementCommentCount(parentId);
        }

        @Test
        @DisplayName("Thành công - Xóa bài viết gốc (không phải reply) → không gọi decrementCommentCount")
        void deletePost_RootPost_NoDecrementCommentCount() {
            UUID postId = UUID.randomUUID();
            User user = User.builder().id(MOCK_USER_ID).build();
            Post post = Post.builder().id(postId).user(user).parentPost(null).build();
            post.setDeleted(false);

            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

            postService.deletePost(postId);

            verify(postRepository, never()).decrementCommentCount(any());
        }

        @Test
        @DisplayName("Thất bại - Bài viết không tồn tại")
        void deletePost_PostNotFound_ThrowsPostNotFoundException() {
            UUID postId = UUID.randomUUID();
            when(postRepository.findById(postId)).thenReturn(Optional.empty());

            assertThrows(PostNotFoundException.class, () -> postService.deletePost(postId));
        }

        @Test
        @DisplayName("Thất bại - Bài viết đã bị xóa trước đó")
        void deletePost_AlreadyDeleted_ThrowsPostNotFoundException() {
            UUID postId = UUID.randomUUID();
            User user = User.builder().id(MOCK_USER_ID).build();
            Post post = Post.builder().id(postId).user(user).build();
            post.setDeleted(true);

            when(postRepository.findById(postId)).thenReturn(Optional.of(post));

            assertThrows(PostNotFoundException.class, () -> postService.deletePost(postId));
            verify(postRepository, never()).save(any());
        }

        @Test
        @DisplayName("Thất bại - Không có quyền xóa bài viết của người khác")
        void deletePost_NotOwner_ThrowsPostAccessDeniedException() {
            UUID postId = UUID.randomUUID();
            User otherUser = User.builder().id(UUID.randomUUID()).build();
            Post post = Post.builder().id(postId).user(otherUser).build();
            post.setDeleted(false);

            when(postRepository.findById(postId)).thenReturn(Optional.of(post));

            assertThrows(PostAccessDeniedException.class, () -> postService.deletePost(postId));
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
        @DisplayName("Thành công - Có trang kế tiếp (DB trả về size+1 IDs)")
        void getUserFeed_HasNextPage() {
            int size = 2;
            UUID cursor = UUID.randomUUID();
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();
            UUID id3 = UUID.randomUUID(); // phần tử thừa → hasNext = true
            List<UUID> ids = List.of(id1, id2, id3);

            Post post1 = Post.builder().id(id1).build();
            Post post2 = Post.builder().id(id2).build();

            when(postRepository.findPublicFeedIds(cursor, Limit.of(size + 1))).thenReturn(ids);
            when(postRepository.findAllWithDetailsByIds(List.of(id1, id2)))
                    .thenReturn(List.of(post1, post2));
            stubMapToResponse();

            CursorResponse<PostResponse> response = postService.getUserFeed(cursor, size);

            assertTrue(response.isHasNext());
            assertEquals(2, response.getContent().size());
            // nextCursor là id cuối của pageIds (id2)
            assertEquals(id2.toString(), response.getNextCursor());
        }

        @Test
        @DisplayName("Thành công - Trang cuối (DB trả về <= size IDs)")
        void getUserFeed_LastPage() {
            int size = 5;
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();
            List<UUID> ids = List.of(id1, id2);

            Post post1 = Post.builder().id(id1).build();
            Post post2 = Post.builder().id(id2).build();

            when(postRepository.findPublicFeedIds(null, Limit.of(size + 1))).thenReturn(ids);
            when(postRepository.findAllWithDetailsByIds(ids)).thenReturn(List.of(post1, post2));
            stubMapToResponse();

            CursorResponse<PostResponse> response = postService.getUserFeed(null, size);

            assertFalse(response.isHasNext());
            assertNull(response.getNextCursor());
            assertEquals(2, response.getContent().size());
        }

        @Test
        @DisplayName("Thành công - Feed trống rỗng")
        void getUserFeed_EmptyFeed() {
            int size = 10;
            when(postRepository.findPublicFeedIds(null, Limit.of(size + 1))).thenReturn(List.of());

            CursorResponse<PostResponse> response = postService.getUserFeed(null, size);

            assertFalse(response.isHasNext());
            assertNull(response.getNextCursor());
            assertTrue(response.getContent().isEmpty());
            verify(postRepository, never()).findAllWithDetailsByIds(any());
            verify(postMapper, never()).mapToResponse(any());
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
            UUID cursor = UUID.randomUUID();
            Post p1 = Post.builder().id(UUID.randomUUID()).build();
            Post p2 = Post.builder().id(UUID.randomUUID()).build();
            Post p3 = Post.builder().id(UUID.randomUUID()).build();
            PostHashtag ph1 = PostHashtag.builder().post(p1).hashtag(mockHashtag).build();
            PostHashtag ph2 = PostHashtag.builder().post(p2).hashtag(mockHashtag).build();
            PostHashtag ph3 = PostHashtag.builder().post(p3).hashtag(mockHashtag).build();

            // Normalize: service lowercase hashtag name
            when(hashtagRepository.findByName("java")).thenReturn(Optional.of(mockHashtag));
            when(postHashtagRepository.findPostsByHashtagId(
                    eq(mockHashtag.getId()), eq(cursor), eq(Limit.of(size + 1))))
                    .thenReturn(new ArrayList<>(List.of(ph1, ph2, ph3)));
            stubMapToResponse();

            CursorResponse<PostResponse> response = postService.getPostsByHashtag("java", cursor, size);

            assertTrue(response.isHasNext());
            assertEquals(2, response.getContent().size());
        }

        @Test
        @DisplayName("Thành công - Trang cuối")
        void getPostsByHashtag_LastPage() {
            int size = 5;
            Post p1 = Post.builder().id(UUID.randomUUID()).build();
            PostHashtag ph1 = PostHashtag.builder().post(p1).hashtag(mockHashtag).build();

            when(hashtagRepository.findByName("java")).thenReturn(Optional.of(mockHashtag));
            when(postHashtagRepository.findPostsByHashtagId(
                    eq(mockHashtag.getId()), isNull(), eq(Limit.of(size + 1))))
                    .thenReturn(new ArrayList<>(List.of(ph1)));
            stubMapToResponse();

            CursorResponse<PostResponse> response = postService.getPostsByHashtag("java", null, size);

            assertFalse(response.isHasNext());
            assertEquals(1, response.getContent().size());
        }

        @Test
        @DisplayName("Thành công - Không có bài viết nào gắn hashtag này")
        void getPostsByHashtag_EmptyResult() {
            int size = 5;
            when(hashtagRepository.findByName("java")).thenReturn(Optional.of(mockHashtag));
            when(postHashtagRepository.findPostsByHashtagId(
                    eq(mockHashtag.getId()), isNull(), eq(Limit.of(size + 1))))
                    .thenReturn(new ArrayList<>());

            CursorResponse<PostResponse> response = postService.getPostsByHashtag("java", null, size);

            assertFalse(response.isHasNext());
            assertNull(response.getNextCursor());
            assertTrue(response.getContent().isEmpty());
        }

        @Test
        @DisplayName("Thành công - Hashtag name được normalize về lowercase trước khi tìm")
        void getPostsByHashtag_NormalizesToLowercase() {
            when(hashtagRepository.findByName("java")).thenReturn(Optional.of(mockHashtag));
            when(postHashtagRepository.findPostsByHashtagId(any(), any(), any()))
                    .thenReturn(new ArrayList<>());

            postService.getPostsByHashtag("JAVA", null, 5);

            verify(hashtagRepository).findByName("java"); // phải lowercase
        }
    }

    // =====================================================================
    // ADD HASHTAG TO POST
    // =====================================================================
    @Nested
    @DisplayName("addHashtagToPost()")
    class AddHashtagToPostTest {

        @Test
        @DisplayName("Thất bại - Bài viết không tồn tại")
        void addHashtagToPost_PostNotFound() {
            UUID postId = UUID.randomUUID();
            when(postRepository.findById(postId)).thenReturn(Optional.empty());

            assertThrows(PostNotFoundException.class,
                    () -> postService.addHashtagToPost(postId, "java"));
            verifyNoInteractions(hashtagRepository, postHashtagRepository);
        }

        @Test
        @DisplayName("Thất bại - Bài viết đã bị xóa mềm")
        void addHashtagToPost_DeletedPost() {
            UUID postId = UUID.randomUUID();
            Post post = Post.builder().id(postId).build();
            post.setDeleted(true);
            when(postRepository.findById(postId)).thenReturn(Optional.of(post));

            assertThrows(PostNotFoundException.class,
                    () -> postService.addHashtagToPost(postId, "java"));
        }

        @Test
        @DisplayName("Thành công - Hashtag chưa tồn tại → tạo mới và liên kết")
        void addHashtagToPost_NewHashtag_CreatesAndLinks() {
            UUID postId = UUID.randomUUID();
            Post post = Post.builder().id(postId).build();
            post.setDeleted(false);
            Hashtag savedHashtag = Hashtag.builder().id(UUID.randomUUID()).name("java").build();

            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            when(hashtagRepository.findByName("java")).thenReturn(Optional.empty());
            when(hashtagRepository.save(any(Hashtag.class))).thenReturn(savedHashtag);
            when(postHashtagRepository.existsByPostIdAndHashtagId(postId, savedHashtag.getId())).thenReturn(false);

            postService.addHashtagToPost(postId, "java");

            verify(hashtagRepository).save(any(Hashtag.class));
            verify(postHashtagRepository).save(any(PostHashtag.class));
        }

        @Test
        @DisplayName("Thành công - Hashtag đã tồn tại, chưa liên kết → chỉ tạo liên kết")
        void addHashtagToPost_ExistingHashtag_OnlyLinks() {
            UUID postId = UUID.randomUUID();
            Post post = Post.builder().id(postId).build();
            post.setDeleted(false);
            Hashtag existing = Hashtag.builder().id(UUID.randomUUID()).name("java").build();

            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            when(hashtagRepository.findByName("java")).thenReturn(Optional.of(existing));
            when(postHashtagRepository.existsByPostIdAndHashtagId(postId, existing.getId())).thenReturn(false);

            postService.addHashtagToPost(postId, "java");

            verify(hashtagRepository, never()).save(any());
            verify(postHashtagRepository).save(any(PostHashtag.class));
        }

        @Test
        @DisplayName("Thành công - Hashtag đã liên kết rồi → bỏ qua, không lưu trùng")
        void addHashtagToPost_AlreadyLinked_SkipsSave() {
            UUID postId = UUID.randomUUID();
            Post post = Post.builder().id(postId).build();
            post.setDeleted(false);
            Hashtag existing = Hashtag.builder().id(UUID.randomUUID()).name("java").build();

            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            when(hashtagRepository.findByName("java")).thenReturn(Optional.of(existing));
            when(postHashtagRepository.existsByPostIdAndHashtagId(postId, existing.getId())).thenReturn(true);

            postService.addHashtagToPost(postId, "java");

            verify(postHashtagRepository, never()).save(any());
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
        @DisplayName("Thất bại - Bài viết cha đã bị xóa")
        void createReply_ParentPostDeleted() {
            UUID parentId = UUID.randomUUID();
            Post deletedParent = Post.builder().id(parentId).build();
            deletedParent.setDeleted(true);
            when(postRepository.findById(parentId)).thenReturn(Optional.of(deletedParent));

            assertThrows(PostNotFoundException.class,
                    () -> postService.createReply(parentId, new PostRequest()));
        }

        @Test
        @DisplayName("Thành công - Tạo reply và tăng commentCount của parent")
        void createReply_Success_IncrementsCommentCount() {
            UUID parentId = UUID.randomUUID();
            User user = User.builder().id(MOCK_USER_ID).build();
            Post parentPost = Post.builder().id(parentId).build();
            parentPost.setDeleted(false);

            PostRequest request = PostRequest.builder().content("This is a reply").build();
            Post mockPost = new Post();
            Post savedReply = Post.builder()
                    .id(UUID.randomUUID())
                    .user(user)
                    .parentPost(parentPost)
                    .build();

            // findById gọi 2 lần: lần 1 trong createReply (getActivePostOrThrow),
            // lần 2 trong createPost (getActivePostOrThrow cho parentPostId)
            when(postRepository.findById(parentId)).thenReturn(Optional.of(parentPost));
            when(userRepository.findById(MOCK_USER_ID)).thenReturn(Optional.of(user));
            when(postMapper.toPost(any(PostRequest.class))).thenReturn(mockPost);
            when(postRepository.save(any(Post.class))).thenReturn(savedReply);
            stubMapToResponse();

            PostResponse result = postService.createReply(parentId, request);

            assertNotNull(result);
            assertEquals(parentId, request.getParentPostId()); // set vào request
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
        void getReplies_ParentPostNotFound() {
            UUID postId = UUID.randomUUID();
            when(postRepository.findById(postId)).thenReturn(Optional.empty());

            assertThrows(PostNotFoundException.class,
                    () -> postService.getReplies(postId, null, 5));
            verify(postRepository, never()).findRepliesIds(any(), any(), any());
        }

        @Test
        @DisplayName("Thất bại - Bài viết gốc đã bị xóa")
        void getReplies_ParentPostDeleted() {
            UUID postId = UUID.randomUUID();
            Post deletedPost = Post.builder().id(postId).build();
            deletedPost.setDeleted(true);
            when(postRepository.findById(postId)).thenReturn(Optional.of(deletedPost));

            assertThrows(PostNotFoundException.class,
                    () -> postService.getReplies(postId, null, 5));
        }

        @Test
        @DisplayName("Thành công - Có trang kế tiếp")
        void getReplies_HasNextPage() {
            int size = 2;
            UUID postId = UUID.randomUUID();
            UUID cursor = UUID.randomUUID();
            Post parentPost = Post.builder().id(postId).build();
            parentPost.setDeleted(false);

            UUID r1 = UUID.randomUUID();
            UUID r2 = UUID.randomUUID();
            UUID r3 = UUID.randomUUID();
            List<UUID> ids = List.of(r1, r2, r3);

            Post reply1 = Post.builder().id(r1).build();
            Post reply2 = Post.builder().id(r2).build();

            when(postRepository.findById(postId)).thenReturn(Optional.of(parentPost));
            when(postRepository.findRepliesIds(postId, cursor, Limit.of(size + 1))).thenReturn(ids);
            when(postRepository.findAllWithDetailsByIds(List.of(r1, r2))).thenReturn(List.of(reply1, reply2));
            stubMapToResponse();

            CursorResponse<PostResponse> response = postService.getReplies(postId, cursor, size);

            assertTrue(response.isHasNext());
            assertEquals(2, response.getContent().size());
            assertEquals(r2.toString(), response.getNextCursor());
        }

        @Test
        @DisplayName("Thành công - Trang cuối (DB trả về <= size)")
        void getReplies_LastPage() {
            int size = 5;
            UUID postId = UUID.randomUUID();
            Post parentPost = Post.builder().id(postId).build();
            parentPost.setDeleted(false);

            UUID r1 = UUID.randomUUID();
            Post reply1 = Post.builder().id(r1).build();

            when(postRepository.findById(postId)).thenReturn(Optional.of(parentPost));
            when(postRepository.findRepliesIds(postId, null, Limit.of(size + 1))).thenReturn(List.of(r1));
            when(postRepository.findAllWithDetailsByIds(List.of(r1))).thenReturn(List.of(reply1));
            stubMapToResponse();

            CursorResponse<PostResponse> response = postService.getReplies(postId, null, size);

            assertFalse(response.isHasNext());
            assertNull(response.getNextCursor());
            assertEquals(1, response.getContent().size());
        }

        @Test
        @DisplayName("Thành công - Không có reply nào")
        void getReplies_NoReplies() {
            int size = 10;
            UUID postId = UUID.randomUUID();
            Post parentPost = Post.builder().id(postId).build();
            parentPost.setDeleted(false);

            when(postRepository.findById(postId)).thenReturn(Optional.of(parentPost));
            when(postRepository.findRepliesIds(postId, null, Limit.of(size + 1))).thenReturn(List.of());

            CursorResponse<PostResponse> response = postService.getReplies(postId, null, size);

            assertFalse(response.isHasNext());
            assertNull(response.getNextCursor());
            assertTrue(response.getContent().isEmpty());
            verify(postRepository, never()).findAllWithDetailsByIds(any());
            verify(postMapper, never()).mapToResponse(any());
        }
    }
}