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
import com.connecthub.modules.features.post.service.HashtagService;
import com.connecthub.modules.features.post.service.MediaService;
import com.connecthub.modules.features.post.service.MentionService;
import com.connecthub.modules.features.post.service.PostService;
import com.connecthub.modules.features.post.service.PostWriteService;
import com.connecthub.modules.features.user.entity.User;
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

/**
 * PostService giờ chỉ là lớp điều phối (orchestrator):
 * - createPost(): upload file (I/O thuần) rồi delegate toàn bộ phần DB sang PostWriteService.createPostTx()
 * - updatePost(): tự xử lý field đơn giản (content/visibility), delegate hashtag/mention sang HashtagService/MentionService
 * - deletePost()/createReply()/getReplies(): dùng existsByIdAndIsDeletedFalse / findByIdAndUserIdAndIsDeletedFalse
 *
 * Logic chi tiết tạo post (parent/quote/hashtag/mention/media) đã chuyển sang PostWriteServiceTest.
 */
@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock private PostRepository postRepository;
    @Mock private HashtagRepository hashtagRepository;
    @Mock private PostHashtagRepository postHashtagRepository;
    @Mock private MentionRepository mentionRepository;
    @Mock private MediaService mediaService;
    @Mock private PostMapper postMapper;
    @Mock private HashtagService hashtagService;
    @Mock private MentionService mentionService;
    @Mock private PostWriteService postWriteService;

    @InjectMocks
    private PostService postService;

    private MockedStatic<AppUtil> mockedAppUtil;
    private final UUID MOCK_USER_ID = UUID.fromString("019ed9d6-65e9-7267-b396-7ac0ad80ded8");

    @BeforeEach
    void setUp() {
        mockedAppUtil = Mockito.mockStatic(AppUtil.class);
        mockedAppUtil.when(AppUtil::userIdFromAuthentication).thenReturn(MOCK_USER_ID);
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
        @DisplayName("Thành công - Không có file → không gọi mediaService.uploadFiles, delegate sang PostWriteService với list rỗng")
        void createPost_NoFiles_DelegatesWithEmptyMediaList() {
            PostRequest request = PostRequest.builder().content("Hello World").build();
            PostResponse expected = new PostResponse();

            when(postWriteService.createPostTx(eq(request), eq(MOCK_USER_ID), eq(List.of())))
                    .thenReturn(expected);

            PostResponse result = postService.createPost(request);

            assertSame(expected, result);
            verify(mediaService, never()).uploadFiles(any());
            verify(postWriteService).createPostTx(request, MOCK_USER_ID, List.of());
            verifyNoInteractions(postRepository, hashtagService, mentionService);
        }

        @Test
        @DisplayName("Thành công - files rỗng [] → coi như không có file")
        void createPost_EmptyFileList_DelegatesWithEmptyMediaList() {
            PostRequest request = PostRequest.builder().content("Hello").files(List.of()).build();
            when(postWriteService.createPostTx(eq(request), eq(MOCK_USER_ID), eq(List.of())))
                    .thenReturn(new PostResponse());

            postService.createPost(request);

            verify(mediaService, never()).uploadFiles(any());
            verify(postWriteService).createPostTx(request, MOCK_USER_ID, List.of());
        }

        @Test
        @DisplayName("Thành công - Có file → upload trước (I/O), rồi mới delegate phần DB với kết quả upload")
        void createPost_WithFiles_UploadsThenDelegates() {
            MultipartFile mockFile = mock(MultipartFile.class);
            PostRequest request = PostRequest.builder()
                    .content("Post with image")
                    .files(List.of(mockFile))
                    .build();
            MediaService.UploadedMedia uploaded =
                    new MediaService.UploadedMedia("http://cdn/img.jpg", "pub1",
                            com.connecthub.modules.features.post.enums.MediaType.IMAGE, 1024L);
            List<MediaService.UploadedMedia> uploadedList = List.of(uploaded);
            PostResponse expected = new PostResponse();

            when(mediaService.uploadFiles(List.of(mockFile))).thenReturn(uploadedList);
            when(postWriteService.createPostTx(request, MOCK_USER_ID, uploadedList)).thenReturn(expected);

            PostResponse result = postService.createPost(request);

            assertSame(expected, result);
            verify(mediaService).uploadFiles(List.of(mockFile));
            verify(postWriteService).createPostTx(request, MOCK_USER_ID, uploadedList);
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
        @DisplayName("Thành công - Cập nhật hashtags → xóa cũ, delegate sang HashtagService để thêm mới")
        void updatePost_ReplaceHashtags_DeletesOldAndDelegatesToHashtagService() {
            UUID postId = UUID.randomUUID();
            Post post = activePostOwnedByCurrentUser(postId);
            Hashtag newHashtag = Hashtag.builder().id(UUID.randomUUID()).name("newtag").build();
            PostHashtag newPostHashtag = PostHashtag.builder().post(post).hashtag(newHashtag).build();
            UpdatePostRequest request = UpdatePostRequest.builder()
                    .hashtags(List.of("newtag"))
                    .build();

            when(postRepository.findByIdAndUserIdWithDetails(postId, MOCK_USER_ID))
                    .thenReturn(Optional.of(post));
            when(hashtagService.addHashtagsToPost(post, List.of("newtag")))
                    .thenReturn(List.of(newPostHashtag));
            when(postRepository.save(any())).thenReturn(post);
            stubMapToResponse();

            postService.updatePost(postId, request);

            verify(postHashtagRepository).deleteByPostId(postId);
            verify(hashtagService).addHashtagsToPost(post, List.of("newtag"));
            assertTrue(post.getPostHashtags().contains(newPostHashtag));
        }

        @Test
        @DisplayName("Thành công - Hashtags rỗng [] → xóa toàn bộ, không gọi HashtagService")
        void updatePost_EmptyHashtags_DeletesAllWithoutCallingHashtagService() {
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
            verify(hashtagService, never()).addHashtagsToPost(any(), any());
            assertTrue(post.getPostHashtags().isEmpty());
        }

        @Test
        @DisplayName("Thành công - Null hashtags → giữ nguyên, không xóa không thêm")
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
            verify(hashtagService, never()).addHashtagsToPost(any(), any());
        }

        @Test
        @DisplayName("Thành công - Cập nhật mentions → xóa cũ, delegate sang MentionService để thêm mới")
        void updatePost_ReplaceMentions_DeletesOldAndDelegatesToMentionService() {
            UUID postId = UUID.randomUUID();
            Post post = activePostOwnedByCurrentUser(postId);
            User mentionedUser = User.builder().id(UUID.randomUUID()).username("alice").build();
            Mention mention = Mention.builder().id(UUID.randomUUID()).post(post).user(mentionedUser).build();
            UpdatePostRequest request = UpdatePostRequest.builder()
                    .mentionUsernames(List.of("alice"))
                    .build();

            when(postRepository.findByIdAndUserIdWithDetails(postId, MOCK_USER_ID))
                    .thenReturn(Optional.of(post));
            when(mentionService.addMentionsByUsername(post, List.of("alice")))
                    .thenReturn(List.of(mention));
            when(postRepository.save(any())).thenReturn(post);
            stubMapToResponse();

            postService.updatePost(postId, request);

            verify(mentionRepository).deleteByPostId(postId);
            verify(mentionService).addMentionsByUsername(post, List.of("alice"));
            assertTrue(post.getMentions().contains(mention));
        }

        @Test
        @DisplayName("Thành công - Mentions rỗng [] → xóa toàn bộ, không gọi MentionService")
        void updatePost_EmptyMentions_DeletesAllWithoutCallingMentionService() {
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
            verify(mentionService, never()).addMentionsByUsername(any(), any());
        }

        @Test
        @DisplayName("Thất bại - Bài viết không tồn tại hoặc không phải chủ → PostAccessDeniedException")
        void updatePost_NotFoundOrNotOwner_ThrowsPostAccessDeniedException() {
            UUID postId = UUID.randomUUID();
            when(postRepository.findByIdAndUserIdWithDetails(postId, MOCK_USER_ID))
                    .thenReturn(Optional.empty());

            assertThrows(PostAccessDeniedException.class,
                    () -> postService.updatePost(postId, new UpdatePostRequest()));
            verify(postRepository, never()).save(any());
        }

        @Test
        @DisplayName("Thất bại - Mention username không tồn tại → exception từ MentionService được ném ra ngoài")
        void updatePost_MentionedUserNotFound_PropagatesException() {
            UUID postId = UUID.randomUUID();
            Post post = activePostOwnedByCurrentUser(postId);
            UpdatePostRequest request = UpdatePostRequest.builder()
                    .mentionUsernames(List.of("ghost"))
                    .build();

            when(postRepository.findByIdAndUserIdWithDetails(postId, MOCK_USER_ID))
                    .thenReturn(Optional.of(post));
            when(mentionService.addMentionsByUsername(post, List.of("ghost")))
                    .thenThrow(new MentionedUserNotFoundException("ghost"));

            assertThrows(MentionedUserNotFoundException.class,
                    () -> postService.updatePost(postId, request));
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
        @DisplayName("Thành công - Xóa mềm bài viết (chỉ tìm bài chưa bị xóa)")
        void deletePost_OwnPost_SetsDeletedTrue() {
            UUID postId = UUID.randomUUID();
            User user = User.builder().id(MOCK_USER_ID).build();
            Post post = Post.builder().id(postId).user(user).build();

            when(postRepository.findByIdAndUserIdAndIsDeletedFalse(postId, MOCK_USER_ID))
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

            when(postRepository.findByIdAndUserIdAndIsDeletedFalse(postId, MOCK_USER_ID))
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

            when(postRepository.findByIdAndUserIdAndIsDeletedFalse(postId, MOCK_USER_ID))
                    .thenReturn(Optional.of(post));
            when(postRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            postService.deletePost(postId);

            verify(postRepository, never()).decrementCommentCount(any());
        }

        @Test
        @DisplayName("Thất bại - Không tìm thấy / không phải chủ / đã bị xóa trước đó → đều trả Optional.empty → PostAccessDeniedException")
        void deletePost_NotFoundOrNotOwnerOrAlreadyDeleted_ThrowsPostAccessDeniedException() {
            UUID postId = UUID.randomUUID();
            // findByIdAndUserIdAndIsDeletedFalse tự lọc isDeleted=false ở tầng query,
            // nên "đã xóa trước đó" và "không tồn tại" đều rơi vào cùng nhánh Optional.empty()
            when(postRepository.findByIdAndUserIdAndIsDeletedFalse(postId, MOCK_USER_ID))
                    .thenReturn(Optional.empty());

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

        @Test
        @DisplayName("Thất bại - Hashtag không tồn tại")
        void getPostsByHashtag_HashtagNotFound() {
            when(hashtagRepository.findIdByName("unknown"))
                    .thenReturn(Optional.empty());

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
            UUID hashtagId = UUID.randomUUID();

            when(hashtagRepository.findIdByName("java")).thenReturn(Optional.of(hashtagId));
            when(postHashtagRepository.findPostIdsByHashtagId(hashtagId, null, Limit.of(size + 1)))
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
            UUID hashtagId = UUID.randomUUID();

            when(hashtagRepository.findIdByName("java")).thenReturn(Optional.of(hashtagId));
            when(postHashtagRepository.findPostIdsByHashtagId(hashtagId, null, Limit.of(size + 1)))
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
            UUID hashtagId = UUID.randomUUID();
            when(hashtagRepository.findIdByName("java")).thenReturn(Optional.of(hashtagId));
            when(postHashtagRepository.findPostIdsByHashtagId(any(), any(), any()))
                    .thenReturn(List.of());

            CursorResponse<PostResponse> response = postService.getPostsByHashtag("java", null, 5);

            assertTrue(response.getContent().isEmpty());
            assertFalse(response.isHasNext());
        }

        @Test
        @DisplayName("Thành công - Hashtag normalize về lowercase")
        void getPostsByHashtag_NormalizesToLowercase() {
            UUID hashtagId = UUID.randomUUID();
            when(hashtagRepository.findIdByName("java")).thenReturn(Optional.of(hashtagId));
            when(postHashtagRepository.findPostIdsByHashtagId(any(), any(), any()))
                    .thenReturn(List.of());

            postService.getPostsByHashtag("JAVA", null, 5);

            verify(hashtagRepository).findIdByName("java");
        }
    }

    // =====================================================================
    // CREATE REPLY
    // =====================================================================
    @Nested
    @DisplayName("createReply()")
    class CreateReplyTest {

        @Test
        @DisplayName("Thất bại - Bài viết cha không tồn tại (hoặc đã bị xóa)")
        void createReply_ParentPostNotFound() {
            UUID parentId = UUID.randomUUID();
            when(postRepository.existsByIdAndIsDeletedFalse(parentId)).thenReturn(false);

            assertThrows(PostNotFoundException.class,
                    () -> postService.createReply(parentId, new PostRequest()));
            verifyNoInteractions(postWriteService);
            verify(postRepository, never()).incrementCommentCount(any());
        }

        @Test
        @DisplayName("Thành công - Tạo reply: set parentPostId vào request, gọi createPost rồi increment commentCount")
        void createReply_Success_IncrementsCommentCount() {
            UUID parentId = UUID.randomUUID();
            PostRequest request = PostRequest.builder().content("Reply content").build();
            PostResponse expected = new PostResponse();

            when(postRepository.existsByIdAndIsDeletedFalse(parentId)).thenReturn(true);
            when(postWriteService.createPostTx(eq(request), eq(MOCK_USER_ID), eq(List.of())))
                    .thenReturn(expected);

            PostResponse result = postService.createReply(parentId, request);

            assertSame(expected, result);
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
            when(postRepository.existsByIdAndIsDeletedFalse(postId)).thenReturn(false);

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

            when(postRepository.existsByIdAndIsDeletedFalse(postId)).thenReturn(true);
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
            when(postRepository.existsByIdAndIsDeletedFalse(postId)).thenReturn(true);
            when(postRepository.findRepliesIds(postId, null, Limit.of(11))).thenReturn(List.of());

            CursorResponse<PostResponse> response = postService.getReplies(postId, null, 10);

            assertTrue(response.getContent().isEmpty());
            verify(postRepository, never()).findAllWithDetailsByIds(any());
        }
    }
}