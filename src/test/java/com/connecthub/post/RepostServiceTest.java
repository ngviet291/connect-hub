package com.connecthub.post;

import com.connecthub.common.util.AppUtil;
import com.connecthub.modules.features.post.entity.Post;
import com.connecthub.modules.features.post.entity.Repost;
import com.connecthub.modules.features.post.exception.PostNotFoundException;
import com.connecthub.modules.features.post.repository.PostRepository;
import com.connecthub.modules.features.post.repository.RepostRepository;
import com.connecthub.modules.features.post.service.RepostService;
import com.connecthub.modules.features.user.entity.User;
import com.connecthub.modules.features.user.exception.UserNotFoundException;
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
class RepostServiceTest {

    @Mock
    private RepostRepository repostRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RepostService repostService;

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
    @DisplayName("Test toggleRepost()")
    class ToggleRepostTest {

        @Test
        @DisplayName("Thành công - Repost bài viết")
        void toggleRepost_CreateRepost_Success() {
            UUID postId = UUID.randomUUID();
            User user = User.builder().id(MOCK_USER_ID).build();
            Post post = Post.builder().id(postId).build();

            when(userRepository.findById(MOCK_USER_ID)).thenReturn(Optional.of(user));
            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            when(repostRepository.findByPostIdAndUserId(postId, MOCK_USER_ID))
                    .thenReturn(Optional.empty());

            boolean result = repostService.toggleRepost(postId);

            assertTrue(result);
            verify(repostRepository).save(any(Repost.class));
            verify(postRepository).incrementRepostCount(postId);
            verify(repostRepository, never()).delete(any());
            verify(postRepository, never()).decrementRepostCount(any());
        }

        @Test
        @DisplayName("Thành công - Repost được lưu đúng user và post")
        void toggleRepost_SavedWithCorrectFields() {
            UUID postId = UUID.randomUUID();
            User user = User.builder().id(MOCK_USER_ID).build();
            Post post = Post.builder().id(postId).build();

            when(userRepository.findById(MOCK_USER_ID)).thenReturn(Optional.of(user));
            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            when(repostRepository.findByPostIdAndUserId(postId, MOCK_USER_ID))
                    .thenReturn(Optional.empty());

            repostService.toggleRepost(postId);

            ArgumentCaptor<Repost> captor = ArgumentCaptor.forClass(Repost.class);
            verify(repostRepository).save(captor.capture());

            Repost saved = captor.getValue();
            assertNotNull(saved.getId());
            assertEquals(user, saved.getUser());
            assertEquals(post, saved.getPost());
        }

        @Test
        @DisplayName("Thành công - Hủy repost")
        void toggleRepost_RemoveRepost_Success() {
            UUID postId = UUID.randomUUID();
            User user = User.builder().id(MOCK_USER_ID).build();
            Post post = Post.builder().id(postId).build();
            Repost existing = Repost.builder()
                    .id(UUID.randomUUID()).user(user).post(post).build();

            when(userRepository.findById(MOCK_USER_ID)).thenReturn(Optional.of(user));
            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            when(repostRepository.findByPostIdAndUserId(postId, MOCK_USER_ID))
                    .thenReturn(Optional.of(existing));

            boolean result = repostService.toggleRepost(postId);

            assertFalse(result);
            verify(repostRepository).delete(existing);
            verify(postRepository).decrementRepostCount(postId);
            verify(repostRepository, never()).save(any());
            verify(postRepository, never()).incrementRepostCount(any());
        }

        @Test
        @DisplayName("Thành công - Delete đúng repost object")
        void toggleRepost_DeleteCorrectRepost() {
            UUID postId = UUID.randomUUID();
            UUID repostId = UUID.randomUUID();
            User user = User.builder().id(MOCK_USER_ID).build();
            Post post = Post.builder().id(postId).build();
            Repost existing = Repost.builder().id(repostId).user(user).post(post).build();

            when(userRepository.findById(MOCK_USER_ID)).thenReturn(Optional.of(user));
            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            when(repostRepository.findByPostIdAndUserId(postId, MOCK_USER_ID))
                    .thenReturn(Optional.of(existing));

            repostService.toggleRepost(postId);

            ArgumentCaptor<Repost> captor = ArgumentCaptor.forClass(Repost.class);
            verify(repostRepository).delete(captor.capture());
            assertEquals(repostId, captor.getValue().getId());
        }

        @Test
        @DisplayName("Thất bại - Không tìm thấy user")
        void toggleRepost_UserNotFound() {
            UUID postId = UUID.randomUUID();

            when(userRepository.findById(MOCK_USER_ID)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class,
                    () -> repostService.toggleRepost(postId));

            verify(postRepository, never()).findById(any());
            verify(repostRepository, never()).findByPostIdAndUserId(any(), any());
            verify(repostRepository, never()).save(any());
        }

        @Test
        @DisplayName("Thất bại - Không tìm thấy post")
        void toggleRepost_PostNotFound() {
            UUID postId = UUID.randomUUID();
            User user = User.builder().id(MOCK_USER_ID).build();

            when(userRepository.findById(MOCK_USER_ID)).thenReturn(Optional.of(user));
            when(postRepository.findById(postId)).thenReturn(Optional.empty());

            assertThrows(PostNotFoundException.class,
                    () -> repostService.toggleRepost(postId));

            verify(repostRepository, never()).findByPostIdAndUserId(any(), any());
            verify(repostRepository, never()).save(any());
        }

        @Test
        @DisplayName("Thất bại - User không tìm thấy, không gọi postRepository")
        void toggleRepost_UserNotFound_NoPostQuery() {
            UUID postId = UUID.randomUUID();

            when(userRepository.findById(MOCK_USER_ID)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class,
                    () -> repostService.toggleRepost(postId));

            verifyNoInteractions(postRepository);
        }
    }

    @Nested
    @DisplayName("Test hasReposted()")
    class HasRepostedTest {

        @Test
        @DisplayName("Trả về true khi đã repost")
        void hasReposted_ReturnTrue() {
            UUID postId = UUID.randomUUID();

            when(repostRepository.existsByPostIdAndUserId(postId, MOCK_USER_ID))
                    .thenReturn(true);

            assertTrue(repostService.hasReposted(postId));
        }

        @Test
        @DisplayName("Trả về false khi chưa repost")
        void hasReposted_ReturnFalse() {
            UUID postId = UUID.randomUUID();

            when(repostRepository.existsByPostIdAndUserId(postId, MOCK_USER_ID))
                    .thenReturn(false);

            assertFalse(repostService.hasReposted(postId));
        }

        @Test
        @DisplayName("Truyền đúng postId và userId vào repository")
        void hasReposted_PassesCorrectArgs() {
            UUID postId = UUID.randomUUID();

            when(repostRepository.existsByPostIdAndUserId(postId, MOCK_USER_ID))
                    .thenReturn(false);

            repostService.hasReposted(postId);

            verify(repostRepository).existsByPostIdAndUserId(postId, MOCK_USER_ID);
            verify(repostRepository, never()).existsByPostIdAndUserId(
                    argThat(id -> !id.equals(postId)), any());
        }
    }
}