package com.connecthub.post;

import com.connecthub.common.dto.response.CursorResponse;
import com.connecthub.common.util.AppUtil;
import com.connecthub.modules.features.post.dto.response.MentionResponse;
import com.connecthub.modules.features.post.dto.response.PostResponse;
import com.connecthub.modules.features.post.entity.Mention;
import com.connecthub.modules.features.post.entity.Post;
import com.connecthub.modules.features.post.exception.MentionedUserNotFoundException;
import com.connecthub.modules.features.post.mapper.PostMapper;
import com.connecthub.modules.features.post.repository.MentionRepository;
import com.connecthub.modules.features.post.service.MentionService;
import com.connecthub.modules.features.user.dto.response.UserSummaryResponse;
import com.connecthub.modules.features.user.entity.User;
import com.connecthub.modules.features.user.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MentionServiceTest {

    @Mock private MentionRepository mentionRepository;
    @Mock private PostMapper postMapper;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private MentionService mentionService;

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

    private Mention buildMention(UUID mentionId, UUID postId) {
        User user = User.builder().id(UUID.randomUUID()).username("user_" + mentionId).build();
        Post post = Post.builder().id(postId).build();
        return Mention.builder().id(mentionId).user(user).post(post).build();
    }

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
        @DisplayName("Thành công - Danh sách mentions rỗng")
        void getMentionsByPost_NoMentions_ReturnsEmptyCursor() {
            UUID postId = UUID.randomUUID();
            stubBuildCursorResponse();

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
            UUID m3Id = UUID.randomUUID();
            Mention m1 = buildMention(m1Id, postId);
            Mention m2 = buildMention(m2Id, postId);
            Mention m3 = buildMention(m3Id, postId);

            when(mentionRepository.findByPostId(postId, cursor, Limit.of(size + 1)))
                    .thenReturn(new ArrayList<>(List.of(m1, m2, m3)));
            when(postMapper.toUserSummaryResponse(any(User.class)))
                    .thenReturn(UserSummaryResponse.builder().build());

            CursorResponse<MentionResponse> response =
                    mentionService.getMentionsByPost(postId, cursor, size);

            assertTrue(response.isHasNext());
            assertEquals(2, response.getContent().size());
            assertEquals(m2Id.toString(), response.getNextCursor());
        }

        @Test
        @DisplayName("Thành công - Có cursor → truyền đúng cursor vào repository")
        void getMentionsByPost_WithCursor_PassesCursorToRepository() {
            UUID postId = UUID.randomUUID();
            UUID cursor = UUID.randomUUID();
            stubBuildCursorResponse();

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
            UUID m3Id = UUID.randomUUID();
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

    // =====================================================================
    // addMentionsByUsername()
    // =====================================================================
    @Nested
    @DisplayName("addMentionsByUsername()")
    class AddMentionsByUsernameTest {

        private Post post;

        @BeforeEach
        void setUp() {
            post = Post.builder().id(UUID.randomUUID()).build();
        }

        @Test
        @DisplayName("Thành công - Tất cả username tồn tại, tạo đúng các Mention")
        void addMentionsByUsername_AllUsersExist_CreatesMentions() {
            List<String> usernames = List.of("Alice", " bob ");

            User alice = User.builder().id(UUID.randomUUID()).username("alice").build();
            User bob = User.builder().id(UUID.randomUUID()).username("bob").build();

            when(userRepository.findAllByUsernameIn(List.of("alice", "bob")))
                    .thenReturn(List.of(alice, bob));

            mockedAppUtil.when(AppUtil::generateUUID)
                    .thenReturn(UUID.randomUUID(), UUID.randomUUID());

            when(mentionRepository.saveAll(anyList()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            List<Mention> result = mentionService.addMentionsByUsername(post, usernames);

            assertEquals(2, result.size());
            assertTrue(result.stream().allMatch(m -> m.getPost().equals(post)));
            assertTrue(result.stream().anyMatch(m -> m.getUser().equals(alice)));
            assertTrue(result.stream().anyMatch(m -> m.getUser().equals(bob)));

            verify(mentionRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("Thất bại - Một username không tồn tại → ném MentionedUserNotFoundException")
        void addMentionsByUsername_UsernameNotFound_ThrowsException() {
            List<String> usernames = List.of("alice", "ghost");

            User alice = User.builder().id(UUID.randomUUID()).username("alice").build();

            when(userRepository.findAllByUsernameIn(List.of("alice", "ghost")))
                    .thenReturn(List.of(alice));

            assertThrows(MentionedUserNotFoundException.class,
                    () -> mentionService.addMentionsByUsername(post, usernames));

            verify(mentionRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("Thành công - Username không phân biệt hoa thường và trim khoảng trắng")
        void addMentionsByUsername_NormalizesUsernames() {
            List<String> usernames = List.of("ALICE", "  Bob");

            User alice = User.builder().id(UUID.randomUUID()).username("alice").build();
            User bob = User.builder().id(UUID.randomUUID()).username("bob").build();

            when(userRepository.findAllByUsernameIn(List.of("alice", "bob")))
                    .thenReturn(List.of(alice, bob));
            mockedAppUtil.when(AppUtil::generateUUID)
                    .thenReturn(UUID.randomUUID(), UUID.randomUUID());
            when(mentionRepository.saveAll(anyList()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            mentionService.addMentionsByUsername(post, usernames);

            verify(userRepository).findAllByUsernameIn(List.of("alice", "bob"));
        }

        @Test
        @DisplayName("Thành công - Danh sách username rỗng → trả về danh sách rỗng, không gọi DB user")
        void addMentionsByUsername_EmptyUsernames_ReturnsEmptyList() {
            when(userRepository.findAllByUsernameIn(List.of())).thenReturn(List.of());
            when(mentionRepository.saveAll(List.of())).thenReturn(List.of());

            List<Mention> result = mentionService.addMentionsByUsername(post, List.of());

            assertTrue(result.isEmpty());
            verify(mentionRepository).saveAll(List.of());
        }
    }
}