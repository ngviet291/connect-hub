package com.connecthub.post;

import com.connecthub.modules.features.post.dto.request.PostRequest;
import com.connecthub.modules.features.post.dto.response.PostResponse;
import com.connecthub.modules.features.post.entity.Media;
import com.connecthub.modules.features.post.entity.Mention;
import com.connecthub.modules.features.post.entity.Post;
import com.connecthub.modules.features.post.entity.PostHashtag;
import com.connecthub.modules.features.post.exception.PostNotFoundException;
import com.connecthub.modules.features.post.mapper.PostMapper;
import com.connecthub.modules.features.post.repository.PostRepository;
import com.connecthub.modules.features.post.service.HashtagService;
import com.connecthub.modules.features.post.service.MediaService;
import com.connecthub.modules.features.post.service.MentionService;
import com.connecthub.modules.features.post.service.PostWriteService;
import com.connecthub.modules.features.post.enums.MediaType;
import com.connecthub.modules.features.user.entity.User;
import com.connecthub.modules.features.user.exception.UserNotFoundException;
import com.connecthub.modules.features.user.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostWriteServiceTest {

    @Mock private PostRepository postRepository;
    @Mock private UserRepository userRepository;
    @Mock private MediaService mediaService;
    @Mock private HashtagService hashtagService;
    @Mock private MentionService mentionService;
    @Mock private PostMapper postMapper;

    @InjectMocks
    private PostWriteService postWriteService;

    private UUID userId;
    private User mockUser;
    private PostRequest request;
    private Post mappedPost;
    private Post savedPost;
    private PostResponse mockResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        mockUser = User.builder().id(userId).build();

        request = mock(PostRequest.class);

        // Post mà postMapper.toPost(request) trả về trước khi save
        mappedPost = Post.builder()
                .media(new HashSet<>())
                .postHashtags(new HashSet<>())
                .mentions(new HashSet<>())
                .build();

        // Post sau khi postRepository.save() — id đã được gán, collection vẫn còn mutable
        savedPost = Post.builder()
                .id(UUID.randomUUID())
                .media(new HashSet<>())
                .postHashtags(new HashSet<>())
                .mentions(new HashSet<>())
                .build();

        mockResponse = mock(PostResponse.class);
    }

    private void stubHappyPathBasics() {
        when(postMapper.toPost(request)).thenReturn(mappedPost);
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(postRepository.save(mappedPost)).thenReturn(savedPost);
        when(postMapper.mapToResponse(savedPost)).thenReturn(mockResponse);
    }

    // =====================================================================
    // Trường hợp cơ bản — không media/hashtag/mention/parent/quote
    // =====================================================================
    @Nested
    @DisplayName("createPostTx() — trường hợp cơ bản")
    class BasicCaseTest {

        @Test
        @DisplayName("Thành công - Tạo post tối giản, không có media/hashtag/mention/parent/quote")
        void createPostTx_MinimalRequest_Success() {
            when(request.getParentPostId()).thenReturn(null);
            when(request.getQuotePostId()).thenReturn(null);
            when(request.getHashtags()).thenReturn(null);
            when(request.getMentionUsernames()).thenReturn(null);
            stubHappyPathBasics();

            PostResponse result = postWriteService.createPostTx(request, userId, List.of());

            assertSame(mockResponse, result);
            verify(postMapper).initNewPost(mappedPost, mockUser);
            verify(postRepository).save(mappedPost);
            verifyNoInteractions(mediaService, hashtagService, mentionService);
            verify(postRepository, never()).findByIdAndIsDeletedFalse(any());
        }

        @Test
        @DisplayName("Thất bại - userId không tồn tại → UserNotFoundException, không save post")
        void createPostTx_UserNotFound_ThrowsUserNotFoundException() {
            when(postMapper.toPost(request)).thenReturn(mappedPost);
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class,
                    () -> postWriteService.createPostTx(request, userId, List.of()));

            verify(postRepository, never()).save(any());
            verifyNoInteractions(mediaService, hashtagService, mentionService);
        }
    }

    // =====================================================================
    // parentPost / quotePost
    // =====================================================================
    @Nested
    @DisplayName("createPostTx() — parentPost & quotePost")
    class ParentQuoteTest {

        @Test
        @DisplayName("Thành công - Có parentPostId hợp lệ → gắn parentPost trước khi save")
        void createPostTx_ValidParentPostId_SetsParentPost() {
            UUID parentId = UUID.randomUUID();
            Post parentPost = Post.builder().id(parentId).build();

            when(request.getParentPostId()).thenReturn(parentId);
            when(request.getQuotePostId()).thenReturn(null);
            when(request.getHashtags()).thenReturn(null);
            when(request.getMentionUsernames()).thenReturn(null);
            when(postRepository.findByIdAndIsDeletedFalse(parentId)).thenReturn(Optional.of(parentPost));
            stubHappyPathBasics();

            PostResponse result = postWriteService.createPostTx(request, userId, List.of());

            assertSame(mockResponse, result);
            assertEquals(parentPost, mappedPost.getParentPost());
            verify(postRepository).findByIdAndIsDeletedFalse(parentId);
        }

        @Test
        @DisplayName("Thất bại - parentPostId không tồn tại hoặc đã xóa → PostNotFoundException")
        void createPostTx_InvalidParentPostId_ThrowsPostNotFoundException() {
            UUID parentId = UUID.randomUUID();

            when(postMapper.toPost(request)).thenReturn(mappedPost);
            when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
            when(request.getParentPostId()).thenReturn(parentId);
            when(postRepository.findByIdAndIsDeletedFalse(parentId)).thenReturn(Optional.empty());

            assertThrows(PostNotFoundException.class,
                    () -> postWriteService.createPostTx(request, userId, List.of()));

            verify(postRepository, never()).save(any());
        }

        @Test
        @DisplayName("Thành công - Có quotePostId hợp lệ → gắn quotePost trước khi save")
        void createPostTx_ValidQuotePostId_SetsQuotePost() {
            UUID quoteId = UUID.randomUUID();
            Post quotePost = Post.builder().id(quoteId).build();

            when(request.getParentPostId()).thenReturn(null);
            when(request.getQuotePostId()).thenReturn(quoteId);
            when(request.getHashtags()).thenReturn(null);
            when(request.getMentionUsernames()).thenReturn(null);
            when(postRepository.findByIdAndIsDeletedFalse(quoteId)).thenReturn(Optional.of(quotePost));
            stubHappyPathBasics();

            PostResponse result = postWriteService.createPostTx(request, userId, List.of());

            assertSame(mockResponse, result);
            assertEquals(quotePost, mappedPost.getQuotePost());
            verify(postRepository).findByIdAndIsDeletedFalse(quoteId);
        }

        @Test
        @DisplayName("Thất bại - quotePostId không tồn tại hoặc đã xóa → PostNotFoundException")
        void createPostTx_InvalidQuotePostId_ThrowsPostNotFoundException() {
            UUID quoteId = UUID.randomUUID();

            when(postMapper.toPost(request)).thenReturn(mappedPost);
            when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
            when(request.getParentPostId()).thenReturn(null);
            when(request.getQuotePostId()).thenReturn(quoteId);
            when(postRepository.findByIdAndIsDeletedFalse(quoteId)).thenReturn(Optional.empty());

            assertThrows(PostNotFoundException.class,
                    () -> postWriteService.createPostTx(request, userId, List.of()));

            verify(postRepository, never()).save(any());
        }

        @Test
        @DisplayName("Thành công - Có cả parentPostId và quotePostId hợp lệ")
        void createPostTx_BothParentAndQuotePostId_SetsBoth() {
            UUID parentId = UUID.randomUUID();
            UUID quoteId = UUID.randomUUID();
            Post parentPost = Post.builder().id(parentId).build();
            Post quotePost = Post.builder().id(quoteId).build();

            when(request.getParentPostId()).thenReturn(parentId);
            when(request.getQuotePostId()).thenReturn(quoteId);
            when(request.getHashtags()).thenReturn(null);
            when(request.getMentionUsernames()).thenReturn(null);
            when(postRepository.findByIdAndIsDeletedFalse(parentId)).thenReturn(Optional.of(parentPost));
            when(postRepository.findByIdAndIsDeletedFalse(quoteId)).thenReturn(Optional.of(quotePost));
            stubHappyPathBasics();

            postWriteService.createPostTx(request, userId, List.of());

            assertEquals(parentPost, mappedPost.getParentPost());
            assertEquals(quotePost, mappedPost.getQuotePost());
        }
    }

    // =====================================================================
    // Media
    // =====================================================================
    @Nested
    @DisplayName("createPostTx() — đính kèm media")
    class MediaAttachTest {

        @Test
        @DisplayName("uploadedMedia rỗng → không gọi mediaService.attachToPost")
        void createPostTx_EmptyMediaList_SkipsMediaService() {
            when(request.getParentPostId()).thenReturn(null);
            when(request.getQuotePostId()).thenReturn(null);
            when(request.getHashtags()).thenReturn(null);
            when(request.getMentionUsernames()).thenReturn(null);
            stubHappyPathBasics();

            postWriteService.createPostTx(request, userId, List.of());

            verify(mediaService, never()).attachToPost(any(), any());
            assertTrue(savedPost.getMedia().isEmpty());
        }

        @Test
        @DisplayName("Thành công - uploadedMedia không rỗng → gọi attachToPost và thêm vào savedPost.getMedia()")
        void createPostTx_NonEmptyMediaList_AttachesMedia() {
            MediaService.UploadedMedia uploaded = new MediaService.UploadedMedia(
                    "http://cdn.com/img.jpg", "pub1", MediaType.IMAGE, 1024L);
            Media attachedMedia = Media.builder().id(UUID.randomUUID()).url("http://cdn.com/img.jpg").build();

            when(request.getParentPostId()).thenReturn(null);
            when(request.getQuotePostId()).thenReturn(null);
            when(request.getHashtags()).thenReturn(null);
            when(request.getMentionUsernames()).thenReturn(null);
            stubHappyPathBasics();
            when(mediaService.attachToPost(List.of(uploaded), savedPost))
                    .thenReturn(List.of(attachedMedia));

            postWriteService.createPostTx(request, userId, List.of(uploaded));

            verify(mediaService).attachToPost(List.of(uploaded), savedPost);
            assertEquals(1, savedPost.getMedia().size());
            assertTrue(savedPost.getMedia().contains(attachedMedia));
        }

        @Test
        @DisplayName("attachToPost được gọi với savedPost (đã có id) chứ không phải mappedPost")
        void createPostTx_AttachToPost_UsesSavedPostNotMappedPost() {
            MediaService.UploadedMedia uploaded = new MediaService.UploadedMedia(
                    "url", "pub", MediaType.VIDEO, 2048L);

            when(request.getParentPostId()).thenReturn(null);
            when(request.getQuotePostId()).thenReturn(null);
            when(request.getHashtags()).thenReturn(null);
            when(request.getMentionUsernames()).thenReturn(null);
            stubHappyPathBasics();
            when(mediaService.attachToPost(anyList(), eq(savedPost))).thenReturn(List.of());

            postWriteService.createPostTx(request, userId, List.of(uploaded));

            ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);
            verify(mediaService).attachToPost(anyList(), postCaptor.capture());
            assertEquals(savedPost.getId(), postCaptor.getValue().getId());
        }
    }

    // =====================================================================
    // Hashtags
    // =====================================================================
    @Nested
    @DisplayName("createPostTx() — gắn hashtag")
    class HashtagTest {

        @Test
        @DisplayName("hashtags = null → không gọi hashtagService")
        void createPostTx_NullHashtags_SkipsHashtagService() {
            when(request.getParentPostId()).thenReturn(null);
            when(request.getQuotePostId()).thenReturn(null);
            when(request.getHashtags()).thenReturn(null);
            when(request.getMentionUsernames()).thenReturn(null);
            stubHappyPathBasics();

            postWriteService.createPostTx(request, userId, List.of());

            verify(hashtagService, never()).addHashtagsToPost(any(), any());
        }

        @Test
        @DisplayName("hashtags = [] → không gọi hashtagService")
        void createPostTx_EmptyHashtags_SkipsHashtagService() {
            when(request.getParentPostId()).thenReturn(null);
            when(request.getQuotePostId()).thenReturn(null);
            when(request.getHashtags()).thenReturn(List.of());
            when(request.getMentionUsernames()).thenReturn(null);
            stubHappyPathBasics();

            postWriteService.createPostTx(request, userId, List.of());

            verify(hashtagService, never()).addHashtagsToPost(any(), any());
        }

        @Test
        @DisplayName("Thành công - hashtags không rỗng → gọi addHashtagsToPost và thêm vào savedPost.getPostHashtags()")
        void createPostTx_NonEmptyHashtags_AddsHashtags() {
            List<String> hashtags = List.of("java", "spring");
            PostHashtag hashtagEntity = PostHashtag.builder().build();

            when(request.getParentPostId()).thenReturn(null);
            when(request.getQuotePostId()).thenReturn(null);
            when(request.getHashtags()).thenReturn(hashtags);
            when(request.getMentionUsernames()).thenReturn(null);
            stubHappyPathBasics();
            doReturn(Set.of(hashtagEntity)).when(hashtagService).addHashtagsToPost(savedPost, hashtags);

            postWriteService.createPostTx(request, userId, List.of());

            verify(hashtagService).addHashtagsToPost(savedPost, hashtags);
            assertEquals(1, savedPost.getPostHashtags().size());
        }
    }

    // =====================================================================
    // Mentions
    // =====================================================================
    @Nested
    @DisplayName("createPostTx() — gắn mention")
    class MentionTest {

        @Test
        @DisplayName("mentionUsernames = null → không gọi mentionService")
        void createPostTx_NullMentions_SkipsMentionService() {
            when(request.getParentPostId()).thenReturn(null);
            when(request.getQuotePostId()).thenReturn(null);
            when(request.getHashtags()).thenReturn(null);
            when(request.getMentionUsernames()).thenReturn(null);
            stubHappyPathBasics();

            postWriteService.createPostTx(request, userId, List.of());

            verify(mentionService, never()).addMentionsByUsername(any(), any());
        }

        @Test
        @DisplayName("mentionUsernames = [] → không gọi mentionService")
        void createPostTx_EmptyMentions_SkipsMentionService() {
            when(request.getParentPostId()).thenReturn(null);
            when(request.getQuotePostId()).thenReturn(null);
            when(request.getHashtags()).thenReturn(null);
            when(request.getMentionUsernames()).thenReturn(List.of());
            stubHappyPathBasics();

            postWriteService.createPostTx(request, userId, List.of());

            verify(mentionService, never()).addMentionsByUsername(any(), any());
        }

        @Test
        @DisplayName("Thành công - mentionUsernames không rỗng → gọi addMentionsByUsername và thêm vào savedPost.getMentions()")
        void createPostTx_NonEmptyMentions_AddsMentions() {
            List<String> usernames = List.of("alice", "bob");
            Mention mentionEntity = Mention.builder().build();

            when(request.getParentPostId()).thenReturn(null);
            when(request.getQuotePostId()).thenReturn(null);
            when(request.getHashtags()).thenReturn(null);
            when(request.getMentionUsernames()).thenReturn(usernames);
            stubHappyPathBasics();
            doReturn(Set.of(mentionEntity)).when(mentionService).addMentionsByUsername(savedPost, usernames);

            postWriteService.createPostTx(request, userId, List.of());

            verify(mentionService).addMentionsByUsername(savedPost, usernames);
            assertEquals(1, savedPost.getMentions().size());
        }
    }

    // =====================================================================
    // Tổng hợp đầy đủ tất cả thành phần
    // =====================================================================
    @Nested
    @DisplayName("createPostTx() — kết hợp đầy đủ media + hashtag + mention + parent + quote")
    class FullCombinationTest {

        @Test
        @DisplayName("Thành công - Tạo post với đầy đủ media, hashtag, mention, parentPost, quotePost")
        void createPostTx_AllFieldsPresent_Success() {
            UUID parentId = UUID.randomUUID();
            UUID quoteId = UUID.randomUUID();
            Post parentPost = Post.builder().id(parentId).build();
            Post quotePost = Post.builder().id(quoteId).build();

            MediaService.UploadedMedia uploaded = new MediaService.UploadedMedia(
                    "http://cdn.com/img.jpg", "pub1", MediaType.IMAGE, 1024L);
            Media attachedMedia = Media.builder().id(UUID.randomUUID()).build();

            List<String> hashtags = List.of("java");
            List<String> mentions = List.of("alice");
            PostHashtag hashtagEntity = PostHashtag.builder().build();
            Mention mentionEntity = Mention.builder().build();

            when(request.getParentPostId()).thenReturn(parentId);
            when(request.getQuotePostId()).thenReturn(quoteId);
            when(request.getHashtags()).thenReturn(hashtags);
            when(request.getMentionUsernames()).thenReturn(mentions);
            when(postRepository.findByIdAndIsDeletedFalse(parentId)).thenReturn(Optional.of(parentPost));
            when(postRepository.findByIdAndIsDeletedFalse(quoteId)).thenReturn(Optional.of(quotePost));
            stubHappyPathBasics();
            when(mediaService.attachToPost(List.of(uploaded), savedPost)).thenReturn(List.of(attachedMedia));
            doReturn(Set.of(hashtagEntity)).when(hashtagService).addHashtagsToPost(savedPost, hashtags);
            doReturn(Set.of(mentionEntity)).when(mentionService).addMentionsByUsername(savedPost, mentions);

            PostResponse result = postWriteService.createPostTx(request, userId, List.of(uploaded));

            assertSame(mockResponse, result);
            assertEquals(parentPost, mappedPost.getParentPost());
            assertEquals(quotePost, mappedPost.getQuotePost());
            assertEquals(1, savedPost.getMedia().size());
            assertEquals(1, savedPost.getPostHashtags().size());
            assertEquals(1, savedPost.getMentions().size());

            verify(postMapper).initNewPost(mappedPost, mockUser);
            verify(postRepository).save(mappedPost);
            verify(mediaService).attachToPost(List.of(uploaded), savedPost);
            verify(hashtagService).addHashtagsToPost(savedPost, hashtags);
            verify(mentionService).addMentionsByUsername(savedPost, mentions);
            verify(postMapper).mapToResponse(savedPost);
        }
    }

    // =====================================================================
    // Thứ tự gọi & mapping
    // =====================================================================
    @Nested
    @DisplayName("createPostTx() — thứ tự gọi và mapping")
    class OrderingAndMappingTest {

        @Test
        @DisplayName("initNewPost được gọi trước khi save, mapToResponse được gọi sau cùng với savedPost")
        void createPostTx_CallOrder_IsCorrect() {
            when(request.getParentPostId()).thenReturn(null);
            when(request.getQuotePostId()).thenReturn(null);
            when(request.getHashtags()).thenReturn(null);
            when(request.getMentionUsernames()).thenReturn(null);
            stubHappyPathBasics();

            postWriteService.createPostTx(request, userId, List.of());

            InOrder inOrder = inOrder(postMapper, postRepository);
            inOrder.verify(postMapper).toPost(request);
            inOrder.verify(postMapper).initNewPost(eq(mappedPost), eq(mockUser));
            inOrder.verify(postRepository).save(mappedPost);
            inOrder.verify(postMapper).mapToResponse(savedPost);
        }

        @Test
        @DisplayName("Kết quả trả về đúng là object do postMapper.mapToResponse(savedPost) sinh ra")
        void createPostTx_ReturnsMapperResponse() {
            when(request.getParentPostId()).thenReturn(null);
            when(request.getQuotePostId()).thenReturn(null);
            when(request.getHashtags()).thenReturn(null);
            when(request.getMentionUsernames()).thenReturn(null);
            stubHappyPathBasics();

            PostResponse result = postWriteService.createPostTx(request, userId, List.of());

            assertSame(mockResponse, result);
            verify(postMapper).mapToResponse(savedPost);
        }
    }
}