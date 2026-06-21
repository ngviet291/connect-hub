package com.connecthub.post;

import com.connecthub.common.util.AppUtil;
import com.connecthub.modules.features.post.entity.Post;
import com.connecthub.modules.features.post.entity.PostView;
import com.connecthub.modules.features.post.exception.PostNotFoundException;
import com.connecthub.modules.features.post.repository.PostRepository;
import com.connecthub.modules.features.post.repository.PostViewRepository;
import com.connecthub.modules.features.post.service.PostViewService;
import com.connecthub.modules.features.user.entity.User;
import com.connecthub.modules.features.user.exception.UserNotFoundException;
import com.connecthub.modules.features.user.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostViewServiceTest {

    @Mock
    private PostViewRepository postViewRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PostViewService postViewService;

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
    @DisplayName("Test recordView()")
    class RecordViewTest {

        @Test
        @DisplayName("Thành công - Ghi nhận lượt xem")
        void recordView_Success() {
            UUID postId = UUID.randomUUID();

            Post post = Post.builder().id(postId).build();
            User user = User.builder().id(MOCK_USER_ID).build();

            when(postRepository.getReferenceById(postId)).thenReturn(post);
            when(userRepository.getReferenceById(MOCK_USER_ID)).thenReturn(user);

            postViewService.recordView(postId);

            verify(postViewRepository).save(any(PostView.class));
            verify(postRepository).incrementViewCount(postId);
        }

        @Test
        @DisplayName("Thành công - PostView được lưu với viewedAt không null")
        void recordView_SavedWithViewedAt() {
            UUID postId = UUID.randomUUID();

            Post post = Post.builder().id(postId).build();
            User user = User.builder().id(MOCK_USER_ID).build();

            when(postRepository.getReferenceById(postId)).thenReturn(post);
            when(userRepository.getReferenceById(MOCK_USER_ID)).thenReturn(user);

            postViewService.recordView(postId);

            ArgumentCaptor<PostView> captor = ArgumentCaptor.forClass(PostView.class);
            verify(postViewRepository).save(captor.capture());

            PostView saved = captor.getValue();
            assertNotNull(saved.getId());
            assertNotNull(saved.getViewedAt());
            assertFalse(saved.getViewedAt().isAfter(LocalDateTime.now()));
            assertEquals(post, saved.getPost());
            assertEquals(user, saved.getUser());
        }

        @Test
        @DisplayName("Thành công - incrementViewCount được gọi đúng postId")
        void recordView_IncrementCalledWithCorrectPostId() {
            UUID postId = UUID.randomUUID();

            when(postRepository.getReferenceById(postId))
                    .thenReturn(Post.builder().id(postId).build());
            when(userRepository.getReferenceById(MOCK_USER_ID))
                    .thenReturn(User.builder().id(MOCK_USER_ID).build());

            postViewService.recordView(postId);

            verify(postRepository).incrementViewCount(postId);
            verify(postRepository, never()).incrementViewCount(argThat(id -> !id.equals(postId)));
        }

        @Test
        @DisplayName("Thành công - Ghi nhiều lượt xem cho cùng 1 post (duplicate cho phép)")
        void recordView_AllowDuplicateViews() {
            UUID postId = UUID.randomUUID();

            when(postRepository.getReferenceById(postId))
                    .thenReturn(Post.builder().id(postId).build());
            when(userRepository.getReferenceById(MOCK_USER_ID))
                    .thenReturn(User.builder().id(MOCK_USER_ID).build());

            postViewService.recordView(postId);
            postViewService.recordView(postId);

            verify(postViewRepository, times(2)).save(any(PostView.class));
            verify(postRepository, times(2)).incrementViewCount(postId);
        }

        @Test
        @DisplayName("Thành công - getReferenceById được gọi đúng (không load full entity)")
        void recordView_UsesGetReferenceById() {
            UUID postId = UUID.randomUUID();

            when(postRepository.getReferenceById(postId))
                    .thenReturn(Post.builder().id(postId).build());
            when(userRepository.getReferenceById(MOCK_USER_ID))
                    .thenReturn(User.builder().id(MOCK_USER_ID).build());

            postViewService.recordView(postId);

            verify(postRepository).getReferenceById(postId);
            verify(userRepository).getReferenceById(MOCK_USER_ID);
            verify(postRepository, never()).findById(any());
            verify(userRepository, never()).findById(any());
        }
    }
}