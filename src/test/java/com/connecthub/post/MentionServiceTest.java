package com.connecthub.post;

import com.connecthub.common.dto.response.CursorResponse;
import com.connecthub.common.util.AppUtil;
import com.connecthub.modules.features.post.dto.response.MentionResponse;
import com.connecthub.modules.features.post.dto.response.PostResponse;
import com.connecthub.modules.features.post.entity.Mention;
import com.connecthub.modules.features.post.entity.Post;
import com.connecthub.modules.features.post.exception.PostNotFoundException;
import com.connecthub.modules.features.post.mapper.PostMapper;
import com.connecthub.modules.features.post.repository.MentionRepository;
import com.connecthub.modules.features.post.repository.PostRepository;
import com.connecthub.modules.features.post.service.MentionService;
import com.connecthub.modules.features.user.dto.response.UserSummaryResponse;
import com.connecthub.modules.features.user.entity.User;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MentionServiceTest {

    @Mock private MentionRepository mentionRepository;
    @Mock private PostRepository postRepository;
    @Mock private PostMapper postMapper;

    @InjectMocks
    private MentionService mentionService;

    private MockedStatic<AppUtil> mockedAppUtil;
    private final UUID MOCK_USER_ID = UUID.fromString("019ed9d6-65e9-7267-b396-7ac0ad80ded8");

    @BeforeEach
    void setUp() {
        mockedAppUtil = Mockito.mockStatic(AppUtil.class);
        mockedAppUtil.when(AppUtil::userIdFormAuthentication).thenReturn(MOCK_USER_ID);
    }

    @AfterEach
    void tearDown() {
        mockedAppUtil.close();
    }

    // Helper: tạo Mention với đầy đủ user
    private Mention buildMention(UUID mentionId, UUID postId) {
        User user = User.builder().id(UUID.randomUUID()).username("user_" + mentionId).build();
        Post post = Post.builder().id(postId).build();
        Mention mention = Mention.builder().id(mentionId).user(user).post(post).build();
        return mention;
    }

    // Helper: stub AppUtil.buildCursorResponse thực tế (không mock static vì logic quan trọng)
    // → ta dùng real AppUtil bằng cách unmock buildCursorResponse
    private void stubBuildCursorResponse() {
        mockedAppUtil.when(() -> AppUtil.buildCursorResponse(anyList(), anyInt(), any(), any()))
                .thenCallRealMethod();
    }

    // =====================================================================
    // getMentionsByPost()
    // =====================================================================
    @Nested
    @DisplayName("getMentionsByPost()")
    class GetMentionsByPostTest {

        @Test
        @DisplayName("Thất bại - Post không tồn tại")
        void getMentionsByPost_PostNotFound_ThrowsPostNotFoundException() {
            UUID postId = UUID.randomUUID();
            when(postRepository.findById(postId)).thenReturn(Optional.empty());

            assertThrows(PostNotFoundException.class,
                    () -> mentionService.getMentionsByPost(postId, null, 5));
            verifyNoInteractions(mentionRepository);
        }

        @Test
        @DisplayName("Thành công - Danh sách mentions rỗng")
        void getMentionsByPost_NoMentions_ReturnsEmptyCursor() {
            UUID postId = UUID.randomUUID();
            stubBuildCursorResponse();

            when(postRepository.findById(postId))
                    .thenReturn(Optional.of(Post.builder().id(postId).build()));
            when(mentionRepository.findByPostId(postId, null, Limit.of(6)))
                    .thenReturn(new ArrayList<>());

            CursorResponse<MentionResponse> response =
                    mentionService.getMentionsByPost(postId, null, 5);

            assertFalse(response.isHasNext());
            assertNull(response.getNextCursor());
            assertTrue(response.getContent().isEmpty());
            verify(postMapper, never()).toUserSummaryResponse(any());
        }

        @Test
        @DisplayName("Thành công - Trả đúng số lượng, trang cuối (không hasNext)")
        void getMentionsByPost_LastPage_ReturnsCorrectContent() {
            UUID postId = UUID.randomUUID();
            int size = 5;
            stubBuildCursorResponse();

            UUID m1Id = UUID.randomUUID();
            UUID m2Id = UUID.randomUUID();
            Mention m1 = buildMention(m1Id, postId);
            Mention m2 = buildMention(m2Id, postId);
            UserSummaryResponse ur1 = UserSummaryResponse.builder().build();
            UserSummaryResponse ur2 = UserSummaryResponse.builder().build();

            when(postRepository.findById(postId))
                    .thenReturn(Optional.of(Post.builder().id(postId).build()));
            when(mentionRepository.findByPostId(postId, null, Limit.of(size + 1)))
                    .thenReturn(new ArrayList<>(List.of(m1, m2)));
            when(postMapper.toUserSummaryResponse(m1.getUser())).thenReturn(ur1);
            when(postMapper.toUserSummaryResponse(m2.getUser())).thenReturn(ur2);

            CursorResponse<MentionResponse> response =
                    mentionService.getMentionsByPost(postId, null, size);

            assertFalse(response.isHasNext());
            assertEquals(2, response.getContent().size());
            assertNull(response.getNextCursor());

            MentionResponse first = response.getContent().get(0);
            assertEquals(m1Id, first.getId());
            assertEquals(ur1, first.getUser());
        }

        @Test
        @DisplayName("Thành công - Có trang kế tiếp (DB trả về size+1 items)")
        void getMentionsByPost_HasNextPage_ReturnsCorrectCursor() {
            UUID postId = UUID.randomUUID();
            int size = 2;
            UUID cursor = UUID.randomUUID();
            stubBuildCursorResponse();

            UUID m1Id = UUID.randomUUID();
            UUID m2Id = UUID.randomUUID();
            UUID m3Id = UUID.randomUUID(); // phần tử thừa
            Mention m1 = buildMention(m1Id, postId);
            Mention m2 = buildMention(m2Id, postId);
            Mention m3 = buildMention(m3Id, postId);

            when(postRepository.findById(postId))
                    .thenReturn(Optional.of(Post.builder().id(postId).build()));
            when(mentionRepository.findByPostId(postId, cursor, Limit.of(size + 1)))
                    .thenReturn(new ArrayList<>(List.of(m1, m2, m3)));
            when(postMapper.toUserSummaryResponse(any(User.class)))
                    .thenReturn(UserSummaryResponse.builder().build());

            CursorResponse<MentionResponse> response =
                    mentionService.getMentionsByPost(postId, cursor, size);

            assertTrue(response.isHasNext());
            assertEquals(2, response.getContent().size());
            // nextCursor = id của phần tử cuối trong pageItems (m2)
            assertEquals(m2Id.toString(), response.getNextCursor());
        }

        @Test
        @DisplayName("Thành công - MentionResponse có đủ các field id, user, createdAt")
        void getMentionsByPost_ResponseMapsAllFields() {
            UUID postId = UUID.randomUUID();
            UUID mentionId = UUID.randomUUID();
            stubBuildCursorResponse();

            User user = User.builder().id(UUID.randomUUID()).username("alice").build();
            Post post = Post.builder().id(postId).build();
            Mention mention = Mention.builder()
                    .id(mentionId)
                    .user(user)
                    .post(post)
                    .build();
            UserSummaryResponse userSummary = UserSummaryResponse.builder().build();

            when(postRepository.findById(postId))
                    .thenReturn(Optional.of(post));
            when(mentionRepository.findByPostId(postId, null, Limit.of(6)))
                    .thenReturn(new ArrayList<>(List.of(mention)));
            when(postMapper.toUserSummaryResponse(user)).thenReturn(userSummary);

            CursorResponse<MentionResponse> response =
                    mentionService.getMentionsByPost(postId, null, 5);

            assertEquals(1, response.getContent().size());
            MentionResponse mr = response.getContent().get(0);
            assertEquals(mentionId, mr.getId());
            assertEquals(userSummary, mr.getUser());
        }

        @Test
        @DisplayName("Thành công - Có cursor → truyền đúng cursor vào repository")
        void getMentionsByPost_WithCursor_PassesCursorToRepository() {
            UUID postId = UUID.randomUUID();
            UUID cursor = UUID.randomUUID();
            stubBuildCursorResponse();

            when(postRepository.findById(postId))
                    .thenReturn(Optional.of(Post.builder().id(postId).build()));
            when(mentionRepository.findByPostId(postId, cursor, Limit.of(6)))
                    .thenReturn(new ArrayList<>());

            mentionService.getMentionsByPost(postId, cursor, 5);

            verify(mentionRepository).findByPostId(postId, cursor, Limit.of(6));
        }
    }

    // =====================================================================
    // getMyMentions()
    // =====================================================================
    @Nested
    @DisplayName("getMyMentions()")
    class GetMyMentionsTest {

        @Test
        @DisplayName("Thành công - Danh sách rỗng, không có bài nào mention mình")
        void getMyMentions_NoMentions_ReturnsEmptyCursor() {
            stubBuildCursorResponse();

            when(mentionRepository.findByUserId(MOCK_USER_ID, null, Limit.of(6)))
                    .thenReturn(new ArrayList<>());

            CursorResponse<PostResponse> response = mentionService.getMyMentions(null, 5);

            assertFalse(response.isHasNext());
            assertNull(response.getNextCursor());
            assertTrue(response.getContent().isEmpty());
            verify(postMapper, never()).mapToResponse(any());
        }

        @Test
        @DisplayName("Thành công - Trang cuối, map đúng Post từ Mention")
        void getMyMentions_LastPage_MapsPostsCorrectly() {
            int size = 5;
            UUID postId1 = UUID.randomUUID();
            UUID postId2 = UUID.randomUUID();
            stubBuildCursorResponse();

            Post post1 = Post.builder().id(postId1).build();
            Post post2 = Post.builder().id(postId2).build();
            Mention m1 = Mention.builder().id(UUID.randomUUID()).post(post1).build();
            Mention m2 = Mention.builder().id(UUID.randomUUID()).post(post2).build();

            when(mentionRepository.findByUserId(MOCK_USER_ID, null, Limit.of(size + 1)))
                    .thenReturn(new ArrayList<>(List.of(m1, m2)));
            when(postMapper.mapToResponse(post1)).thenReturn(new PostResponse());
            when(postMapper.mapToResponse(post2)).thenReturn(new PostResponse());

            CursorResponse<PostResponse> response = mentionService.getMyMentions(null, size);

            assertFalse(response.isHasNext());
            assertEquals(2, response.getContent().size());
            verify(postMapper).mapToResponse(post1);
            verify(postMapper).mapToResponse(post2);
        }

        @Test
        @DisplayName("Thành công - Có trang kế tiếp (DB trả về size+1 items)")
        void getMyMentions_HasNextPage_ReturnsCorrectCursor() {
            int size = 2;
            UUID cursor = UUID.randomUUID();
            stubBuildCursorResponse();

            UUID m1Id = UUID.randomUUID();
            UUID m2Id = UUID.randomUUID();
            UUID m3Id = UUID.randomUUID(); // phần tử thừa
            Mention m1 = Mention.builder().id(m1Id).post(Post.builder().id(UUID.randomUUID()).build()).build();
            Mention m2 = Mention.builder().id(m2Id).post(Post.builder().id(UUID.randomUUID()).build()).build();
            Mention m3 = Mention.builder().id(m3Id).post(Post.builder().id(UUID.randomUUID()).build()).build();

            when(mentionRepository.findByUserId(MOCK_USER_ID, cursor, Limit.of(size + 1)))
                    .thenReturn(new ArrayList<>(List.of(m1, m2, m3)));
            when(postMapper.mapToResponse(any(Post.class))).thenReturn(new PostResponse());

            CursorResponse<PostResponse> response = mentionService.getMyMentions(cursor, size);

            assertTrue(response.isHasNext());
            assertEquals(2, response.getContent().size());
            assertEquals(m2Id.toString(), response.getNextCursor());
        }

        @Test
        @DisplayName("Thành công - Dùng đúng userId từ authentication")
        void getMyMentions_UsesAuthenticatedUserId() {
            stubBuildCursorResponse();

            when(mentionRepository.findByUserId(MOCK_USER_ID, null, Limit.of(6)))
                    .thenReturn(new ArrayList<>());

            mentionService.getMyMentions(null, 5);

            // Phải dùng đúng MOCK_USER_ID lấy từ AppUtil.userIdFormAuthentication()
            verify(mentionRepository).findByUserId(eq(MOCK_USER_ID), isNull(), eq(Limit.of(6)));
        }

        @Test
        @DisplayName("Thành công - Có cursor → truyền đúng cursor vào repository")
        void getMyMentions_WithCursor_PassesCursorToRepository() {
            UUID cursor = UUID.randomUUID();
            stubBuildCursorResponse();

            when(mentionRepository.findByUserId(MOCK_USER_ID, cursor, Limit.of(6)))
                    .thenReturn(new ArrayList<>());

            mentionService.getMyMentions(cursor, 5);

            verify(mentionRepository).findByUserId(MOCK_USER_ID, cursor, Limit.of(6));
        }
    }
}