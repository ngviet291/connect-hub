package com.connecthub.post;

import com.connecthub.common.util.AppUtil;
import com.connecthub.modules.features.post.entity.Post;
import com.connecthub.modules.features.post.entity.Reaction;
import com.connecthub.modules.features.post.enums.ReactionType;
import com.connecthub.modules.features.post.repository.PostRepository;
import com.connecthub.modules.features.post.repository.ReactionRepository;
import com.connecthub.modules.features.post.service.ReactionService;
import com.connecthub.modules.features.user.entity.User;
import com.connecthub.modules.features.user.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReactionServiceTest {

    @Mock
    private ReactionRepository reactionRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReactionService reactionService;

    private MockedStatic<AppUtil> mockedAppUtil;

    private final UUID MOCK_USER_ID =
            UUID.fromString("019ed9d6-65e9-7267-b396-7ac0ad80ded8");

    @BeforeEach
    void setUp() {
        mockedAppUtil = Mockito.mockStatic(AppUtil.class);
        mockedAppUtil.when(AppUtil::userIdFormAuthentication)
                .thenReturn(MOCK_USER_ID);
    }

    @AfterEach
    void tearDown() {
        mockedAppUtil.close();
    }

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
        @DisplayName("Thành công - Unreact không gọi getReferenceById (không cần load)")
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

    @Nested
    @DisplayName("Test hasReacted()")
    class HasReactedTest {

        @Test
        @DisplayName("Trả về true khi đã react")
        void hasReacted_ReturnTrue() {
            UUID postId = UUID.randomUUID();

            when(reactionRepository.existsByPostIdAndUserId(postId, MOCK_USER_ID))
                    .thenReturn(true);

            assertTrue(reactionService.hasReacted(postId));
        }

        @Test
        @DisplayName("Trả về false khi chưa react")
        void hasReacted_ReturnFalse() {
            UUID postId = UUID.randomUUID();

            when(reactionRepository.existsByPostIdAndUserId(postId, MOCK_USER_ID))
                    .thenReturn(false);

            assertFalse(reactionService.hasReacted(postId));
        }

        @Test
        @DisplayName("Truyền đúng postId và userId vào repository")
        void hasReacted_PassesCorrectArgs() {
            UUID postId = UUID.randomUUID();

            when(reactionRepository.existsByPostIdAndUserId(postId, MOCK_USER_ID))
                    .thenReturn(true);

            reactionService.hasReacted(postId);

            verify(reactionRepository).existsByPostIdAndUserId(postId, MOCK_USER_ID);
            verify(reactionRepository, never()).existsByPostIdAndUserId(
                    argThat(id -> !id.equals(postId)), any());
        }
    }
}