package com.connecthub.post;

import com.connecthub.common.dto.response.CursorResponse;
import com.connecthub.common.util.AppUtil;
import com.connecthub.modules.features.post.dto.projection.ReactionTypeCountProjection;
import com.connecthub.modules.features.post.dto.response.ReactionCountResponse;
import com.connecthub.modules.features.post.dto.response.ReactionResponse;
import com.connecthub.modules.features.post.entity.Post;
import com.connecthub.modules.features.post.entity.Reaction;
import com.connecthub.modules.features.post.enums.ReactionType;
import com.connecthub.modules.features.post.exception.PostNotFoundException;
import com.connecthub.modules.features.post.repository.PostRepository;
import com.connecthub.modules.features.post.repository.ReactionRepository;
import com.connecthub.modules.features.post.service.ReactionService;
import com.connecthub.modules.features.user.entity.User;
import com.connecthub.modules.features.user.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReactionServiceTest {

    @Mock private ReactionRepository reactionRepository;
    @Mock private PostRepository postRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private ReactionService reactionService;

    private MockedStatic<AppUtil> mockedAppUtil;

    private final UUID MOCK_USER_ID =
            UUID.fromString("019ed9d6-65e9-7267-b396-7ac0ad80ded8");

    @BeforeEach
    void setUp() {
        mockedAppUtil = Mockito.mockStatic(AppUtil.class);
        mockedAppUtil.when(AppUtil::userIdFromAuthentication).thenReturn(MOCK_USER_ID);
    }

    @AfterEach
    void tearDown() {
        mockedAppUtil.close();
    }

    // ── toggleReaction (existing tests kept) ─────────────────────────────────

    @Nested
    @DisplayName("Test toggleReaction()")
    class ToggleReactionTest {

        @Test
        @DisplayName("Thành công - React LIKE bài viết")
        void toggleReaction_CreateLike_Success() {
            UUID postId = UUID.randomUUID();
            when(reactionRepository.findByPostIdAndUserId(postId, MOCK_USER_ID))
                    .thenReturn(Optional.empty());
            when(userRepository.getReferenceById(MOCK_USER_ID))
                    .thenReturn(User.builder().id(MOCK_USER_ID).build());
            when(postRepository.getReferenceById(postId))
                    .thenReturn(Post.builder().id(postId).build());

            boolean result = reactionService.toggleReaction(postId, ReactionType.LIKE);

            assertTrue(result);
            verify(reactionRepository).save(any(Reaction.class));
            verify(postRepository).incrementReactionCount(postId);
            verify(reactionRepository, never()).delete(any());
            verify(postRepository, never()).decrementReactionCount(any());
        }

        @Test
        @DisplayName("Thành công - React với các type khác nhau")
        void toggleReaction_CreateWithDifferentTypes() {
            for (ReactionType type : ReactionType.values()) {
                UUID postId = UUID.randomUUID();
                when(reactionRepository.findByPostIdAndUserId(postId, MOCK_USER_ID))
                        .thenReturn(Optional.empty());
                when(userRepository.getReferenceById(MOCK_USER_ID))
                        .thenReturn(User.builder().id(MOCK_USER_ID).build());
                when(postRepository.getReferenceById(postId))
                        .thenReturn(Post.builder().id(postId).build());

                boolean result = reactionService.toggleReaction(postId, type);
                assertTrue(result, "Should return true for type: " + type);
            }
        }

        @Test
        @DisplayName("Thành công - Reaction được lưu đúng type và id")
        void toggleReaction_SavedWithCorrectFields() {
            UUID postId = UUID.randomUUID();
            User userProxy = User.builder().id(MOCK_USER_ID).build();
            Post postProxy = Post.builder().id(postId).build();

            when(reactionRepository.findByPostIdAndUserId(postId, MOCK_USER_ID))
                    .thenReturn(Optional.empty());
            when(userRepository.getReferenceById(MOCK_USER_ID)).thenReturn(userProxy);
            when(postRepository.getReferenceById(postId)).thenReturn(postProxy);

            reactionService.toggleReaction(postId, ReactionType.LIKE);

            ArgumentCaptor<Reaction> captor = ArgumentCaptor.forClass(Reaction.class);
            verify(reactionRepository).save(captor.capture());
            Reaction saved = captor.getValue();
            assertNotNull(saved.getId());
            assertEquals(ReactionType.LIKE, saved.getType());
            assertEquals(userProxy, saved.getUser());
            assertEquals(postProxy, saved.getPost());
        }

        @Test
        @DisplayName("Thành công - Bỏ reaction khi đã react")
        void toggleReaction_RemoveReaction_Success() {
            UUID postId = UUID.randomUUID();
            Reaction existing = Reaction.builder().id(UUID.randomUUID()).build();
            when(reactionRepository.findByPostIdAndUserId(postId, MOCK_USER_ID))
                    .thenReturn(Optional.of(existing));

            boolean result = reactionService.toggleReaction(postId, ReactionType.LIKE);

            assertFalse(result);
            verify(reactionRepository).delete(existing);
            verify(postRepository).decrementReactionCount(postId);
            verify(reactionRepository, never()).save(any());
            verify(postRepository, never()).incrementReactionCount(any());
        }

        @Test
        @DisplayName("Thành công - delete đúng reaction object")
        void toggleReaction_DeleteCorrectReaction() {
            UUID postId = UUID.randomUUID();
            UUID reactionId = UUID.randomUUID();
            Reaction existing = Reaction.builder().id(reactionId).build();
            when(reactionRepository.findByPostIdAndUserId(postId, MOCK_USER_ID))
                    .thenReturn(Optional.of(existing));

            reactionService.toggleReaction(postId, ReactionType.LIKE);

            ArgumentCaptor<Reaction> captor = ArgumentCaptor.forClass(Reaction.class);
            verify(reactionRepository).delete(captor.capture());
            assertEquals(reactionId, captor.getValue().getId());
        }

        @Test
        @DisplayName("Thành công - Dùng getReferenceById không load full entity")
        void toggleReaction_UsesGetReferenceById() {
            UUID postId = UUID.randomUUID();
            when(reactionRepository.findByPostIdAndUserId(postId, MOCK_USER_ID))
                    .thenReturn(Optional.empty());
            when(userRepository.getReferenceById(MOCK_USER_ID))
                    .thenReturn(User.builder().id(MOCK_USER_ID).build());
            when(postRepository.getReferenceById(postId))
                    .thenReturn(Post.builder().id(postId).build());

            reactionService.toggleReaction(postId, ReactionType.LIKE);

            verify(userRepository).getReferenceById(MOCK_USER_ID);
            verify(postRepository).getReferenceById(postId);
            verify(userRepository, never()).findById(any());
            verify(postRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Thành công - Unreact không gọi getReferenceById")
        void toggleReaction_Remove_DoesNotCallGetReference() {
            UUID postId = UUID.randomUUID();
            Reaction existing = Reaction.builder().id(UUID.randomUUID()).build();
            when(reactionRepository.findByPostIdAndUserId(postId, MOCK_USER_ID))
                    .thenReturn(Optional.of(existing));

            reactionService.toggleReaction(postId, ReactionType.LIKE);

            verify(userRepository, never()).getReferenceById(any());
            verify(postRepository, never()).getReferenceById(any());
        }
    }

    // ── hasReacted (existing tests kept) ─────────────────────────────────────

    @Nested
    @DisplayName("Test hasReacted()")
    class HasReactedTest {

        @Test
        @DisplayName("Trả về true khi đã react")
        void hasReacted_ReturnTrue() {
            UUID postId = UUID.randomUUID();
            when(reactionRepository.existsByPostIdAndUserId(postId, MOCK_USER_ID)).thenReturn(true);
            assertTrue(reactionService.hasReacted(postId));
        }

        @Test
        @DisplayName("Trả về false khi chưa react")
        void hasReacted_ReturnFalse() {
            UUID postId = UUID.randomUUID();
            when(reactionRepository.existsByPostIdAndUserId(postId, MOCK_USER_ID)).thenReturn(false);
            assertFalse(reactionService.hasReacted(postId));
        }

        @Test
        @DisplayName("Truyền đúng postId và userId vào repository")
        void hasReacted_PassesCorrectArgs() {
            UUID postId = UUID.randomUUID();
            when(reactionRepository.existsByPostIdAndUserId(postId, MOCK_USER_ID)).thenReturn(true);

            reactionService.hasReacted(postId);

            verify(reactionRepository).existsByPostIdAndUserId(postId, MOCK_USER_ID);
            verify(reactionRepository, never())
                    .existsByPostIdAndUserId(argThat(id -> !id.equals(postId)), any());
        }
    }

    // ── getReactionsByPost (NEW) ──────────────────────────────────────────────

    @Nested
    @DisplayName("Test getReactionsByPost()")
    class GetReactionsByPostTest {

        private User buildUser(UUID id) {
            return User.builder()
                    .id(id)
                    .username("user_" + id.toString().substring(0, 8))
                    .fullName("Full Name")
                    .avatarUrl("https://example.com/avatar.jpg")
                    .build();
        }

        private Reaction buildReaction(User user, UUID postId, ReactionType type) {
            Reaction r = Reaction.builder()
                    .id(UUID.randomUUID())
                    .user(user)
                    .post(Post.builder().id(postId).build())
                    .type(type)
                    .build();
            // BaseEntity createdAt
            r.setCreatedAt(LocalDateTime.now());
            return r;
        }

        @Test
        @DisplayName("Ném PostNotFoundException khi bài đăng không tồn tại")
        void getReactions_PostNotFound_ThrowsException() {
            UUID postId = UUID.randomUUID();
            when(postRepository.existsById(postId)).thenReturn(false);

            assertThrows(PostNotFoundException.class,
                    () -> reactionService.getReactionsByPost(postId, null, 20));
            verify(reactionRepository, never())
                    .findByPostIdWithUser(any(), any(), any());
        }

        @Test
        @DisplayName("Trả về danh sách reaction đúng với cursor null (trang đầu)")
        void getReactions_FirstPage_ReturnsList() {
            UUID postId = UUID.randomUUID();
            User user1 = buildUser(UUID.randomUUID());
            User user2 = buildUser(UUID.randomUUID());
            List<Reaction> mockReactions = List.of(
                    buildReaction(user1, postId, ReactionType.LIKE),
                    buildReaction(user2, postId, ReactionType.LOVE)
            );

            when(postRepository.existsById(postId)).thenReturn(true);
            when(reactionRepository.findByPostIdWithUser(eq(postId), isNull(), any(Pageable.class)))
                    .thenReturn(mockReactions);

            CursorResponse<ReactionResponse> result =
                    reactionService.getReactionsByPost(postId, null, 20);

            assertEquals(2, result.getContent().size());
            assertFalse(result.isHasNext());
            assertNull(result.getNextCursor());
            assertEquals(ReactionType.LIKE, result.getContent().get(0).getType());
            assertEquals(ReactionType.LOVE, result.getContent().get(1).getType());
        }

        @Test
        @DisplayName("Trả về hasNext=true khi còn dữ liệu")
        void getReactions_HasNextPage() {
            UUID postId = UUID.randomUUID();
            int limit = 2;
            User user = buildUser(UUID.randomUUID());
            // repository trả về limit+1 phần tử → còn trang sau
            List<Reaction> mockReactions = List.of(
                    buildReaction(user, postId, ReactionType.LIKE),
                    buildReaction(user, postId, ReactionType.LOVE),
                    buildReaction(user, postId, ReactionType.HAHA)  // extra
            );

            when(postRepository.existsById(postId)).thenReturn(true);
            when(reactionRepository.findByPostIdWithUser(eq(postId), isNull(), any(Pageable.class)))
                    .thenReturn(mockReactions);

            CursorResponse<ReactionResponse> result =
                    reactionService.getReactionsByPost(postId, null, limit);

            assertTrue(result.isHasNext());
            assertEquals(2, result.getContent().size());
            assertNotNull(result.getNextCursor());
        }

        @Test
        @DisplayName("Truyền đúng cursor vào repository")
        void getReactions_PassesCursorToRepository() {
            UUID postId = UUID.randomUUID();
            UUID cursor = UUID.randomUUID();

            when(postRepository.existsById(postId)).thenReturn(true);
            when(reactionRepository.findByPostIdWithUser(eq(postId), eq(cursor), any(Pageable.class)))
                    .thenReturn(List.of());

            reactionService.getReactionsByPost(postId, cursor, 20);

            verify(reactionRepository).findByPostIdWithUser(eq(postId), eq(cursor), any(Pageable.class));
        }

        @Test
        @DisplayName("Map đúng thông tin user vào ReactionResponse")
        void getReactions_MapsUserCorrectly() {
            UUID postId = UUID.randomUUID();
            User user = buildUser(UUID.randomUUID());
            Reaction reaction = buildReaction(user, postId, ReactionType.WOW);

            when(postRepository.existsById(postId)).thenReturn(true);
            when(reactionRepository.findByPostIdWithUser(any(), any(), any()))
                    .thenReturn(List.of(reaction));

            CursorResponse<ReactionResponse> result =
                    reactionService.getReactionsByPost(postId, null, 20);

            ReactionResponse response = result.getContent().get(0);
            assertEquals(user.getId(), response.getUser().getId());
            assertEquals(user.getUsername(), response.getUser().getUsername());
            assertEquals(user.getFullName(), response.getUser().getFullName());
            assertEquals(user.getAvatarUrl(), response.getUser().getAvatarUrl());
            assertEquals(ReactionType.WOW, response.getType());
        }

        @Test
        @DisplayName("Trả về danh sách rỗng khi bài đăng chưa có reaction")
        void getReactions_EmptyList() {
            UUID postId = UUID.randomUUID();
            when(postRepository.existsById(postId)).thenReturn(true);
            when(reactionRepository.findByPostIdWithUser(any(), any(), any()))
                    .thenReturn(List.of());

            CursorResponse<ReactionResponse> result =
                    reactionService.getReactionsByPost(postId, null, 20);

            assertTrue(result.getContent().isEmpty());
            assertFalse(result.isHasNext());
            assertNull(result.getNextCursor());
        }
    }

    // ── countReactionsByType (NEW) ────────────────────────────────────────────

    @Nested
    @DisplayName("Test countReactionsByType()")
    class CountReactionsByTypeTest {

        private ReactionTypeCountProjection mockCount(ReactionType type, long count) {
            return new ReactionTypeCountProjection() {
                public ReactionType getType() { return type; }
                public long getCount() { return count; }
            };
        }

        @Test
        @DisplayName("Ném PostNotFoundException khi bài đăng không tồn tại")
        void countReactions_PostNotFound_ThrowsException() {
            UUID postId = UUID.randomUUID();
            when(postRepository.existsById(postId)).thenReturn(false);

            assertThrows(PostNotFoundException.class,
                    () -> reactionService.countReactionsByType(postId));
            verify(reactionRepository, never()).countByPostIdGroupByType(any());
        }

        @Test
        @DisplayName("Trả về đúng số lượng theo từng loại")
        void countReactions_ReturnsCounts() {
            UUID postId = UUID.randomUUID();
            when(postRepository.existsById(postId)).thenReturn(true);
            when(reactionRepository.countByPostIdGroupByType(postId)).thenReturn(List.of(
                    mockCount(ReactionType.LIKE, 10),
                    mockCount(ReactionType.LOVE, 5),
                    mockCount(ReactionType.HAHA, 2)
            ));

            List<ReactionCountResponse> result = reactionService.countReactionsByType(postId);

            assertEquals(3, result.size());
            assertEquals(ReactionType.LIKE, result.get(0).getType());
            assertEquals(10, result.get(0).getCount());
            assertEquals(ReactionType.LOVE, result.get(1).getType());
            assertEquals(5, result.get(1).getCount());
        }

        @Test
        @DisplayName("Trả về danh sách rỗng khi bài đăng chưa có reaction")
        void countReactions_EmptyWhenNoReactions() {
            UUID postId = UUID.randomUUID();
            when(postRepository.existsById(postId)).thenReturn(true);
            when(reactionRepository.countByPostIdGroupByType(postId)).thenReturn(List.of());

            List<ReactionCountResponse> result = reactionService.countReactionsByType(postId);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Truyền đúng postId vào repository")
        void countReactions_PassesCorrectPostId() {
            UUID postId = UUID.randomUUID();
            when(postRepository.existsById(postId)).thenReturn(true);
            when(reactionRepository.countByPostIdGroupByType(postId)).thenReturn(List.of());

            reactionService.countReactionsByType(postId);

            verify(reactionRepository).countByPostIdGroupByType(postId);
            verify(reactionRepository, never())
                    .countByPostIdGroupByType(argThat(id -> !id.equals(postId)));
        }

        @Test
        @DisplayName("Hỗ trợ tất cả 6 loại ReactionType")
        void countReactions_SupportsAllTypes() {
            UUID postId = UUID.randomUUID();
            List<ReactionTypeCountProjection> allTypes = List.of(
                    mockCount(ReactionType.LIKE, 1),
                    mockCount(ReactionType.LOVE, 2),
                    mockCount(ReactionType.HAHA, 3),
                    mockCount(ReactionType.WOW,  4),
                    mockCount(ReactionType.SAD,  5),
                    mockCount(ReactionType.ANGRY, 6)
            );
            when(postRepository.existsById(postId)).thenReturn(true);
            when(reactionRepository.countByPostIdGroupByType(postId)).thenReturn(allTypes);

            List<ReactionCountResponse> result = reactionService.countReactionsByType(postId);

            assertEquals(6, result.size());
            long totalCount = result.stream().mapToLong(ReactionCountResponse::getCount).sum();
            assertEquals(21, totalCount);
        }
    }
}