package com.connecthub.post;

import com.connecthub.common.dto.response.CursorResponse;
import com.connecthub.common.util.AppUtil;
import com.connecthub.modules.features.post.dto.response.PostResponse;
import com.connecthub.modules.features.post.entity.Bookmark;
import com.connecthub.modules.features.post.entity.Post;
import com.connecthub.modules.features.post.exception.PostNotFoundException;
import com.connecthub.modules.features.post.mapper.PostMapper;
import com.connecthub.modules.features.post.repository.BookmarkRepository;
import com.connecthub.modules.features.post.repository.PostRepository;
import com.connecthub.modules.features.post.service.BookmarkService;
import com.connecthub.modules.features.user.entity.User;
import com.connecthub.modules.features.user.exception.UserNotFoundException;
import com.connecthub.modules.features.user.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookmarkServiceTest {

    @Mock
    private BookmarkRepository bookmarkRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PostMapper postMapper;

    @InjectMocks
    private BookmarkService bookmarkService;

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
    @DisplayName("Test toggleBookmark()")
    class ToggleBookmarkTest {

        @Test
        @DisplayName("Thành công - Bookmark bài viết")
        void toggleBookmark_CreateBookmark_Success() {
            UUID postId = UUID.randomUUID();
            User user = User.builder().id(MOCK_USER_ID).build();
            Post post = Post.builder().id(postId).build();

            when(userRepository.findById(MOCK_USER_ID)).thenReturn(Optional.of(user));
            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            when(bookmarkRepository.findByPostIdAndUserId(postId, MOCK_USER_ID))
                    .thenReturn(Optional.empty());

            boolean result = bookmarkService.toggleBookmark(postId);

            assertTrue(result);
            verify(bookmarkRepository).save(any(Bookmark.class));
            verify(postRepository).incrementBookmarkCount(postId);
            verify(bookmarkRepository, never()).delete(any());
            verify(postRepository, never()).decrementBookmarkCount(any());
        }

        @Test
        @DisplayName("Thành công - Bookmark được lưu đúng user và post")
        void toggleBookmark_SavedWithCorrectFields() {
            UUID postId = UUID.randomUUID();
            User user = User.builder().id(MOCK_USER_ID).build();
            Post post = Post.builder().id(postId).build();

            when(userRepository.findById(MOCK_USER_ID)).thenReturn(Optional.of(user));
            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            when(bookmarkRepository.findByPostIdAndUserId(postId, MOCK_USER_ID))
                    .thenReturn(Optional.empty());

            bookmarkService.toggleBookmark(postId);

            ArgumentCaptor<Bookmark> captor = ArgumentCaptor.forClass(Bookmark.class);
            verify(bookmarkRepository).save(captor.capture());

            Bookmark saved = captor.getValue();
            assertNotNull(saved.getId());
            assertEquals(user, saved.getUser());
            assertEquals(post, saved.getPost());
        }

        @Test
        @DisplayName("Thành công - Bỏ bookmark bài viết")
        void toggleBookmark_RemoveBookmark_Success() {
            UUID postId = UUID.randomUUID();
            User user = User.builder().id(MOCK_USER_ID).build();
            Post post = Post.builder().id(postId).build();
            Bookmark existing = Bookmark.builder()
                    .id(UUID.randomUUID()).user(user).post(post).build();

            when(userRepository.findById(MOCK_USER_ID)).thenReturn(Optional.of(user));
            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            when(bookmarkRepository.findByPostIdAndUserId(postId, MOCK_USER_ID))
                    .thenReturn(Optional.of(existing));

            boolean result = bookmarkService.toggleBookmark(postId);

            assertFalse(result);
            verify(bookmarkRepository).delete(existing);
            verify(postRepository).decrementBookmarkCount(postId);
            verify(bookmarkRepository, never()).save(any());
            verify(postRepository, never()).incrementBookmarkCount(any());
        }

        @Test
        @DisplayName("Thành công - Delete đúng bookmark object")
        void toggleBookmark_DeleteCorrectBookmark() {
            UUID postId = UUID.randomUUID();
            UUID bookmarkId = UUID.randomUUID();
            User user = User.builder().id(MOCK_USER_ID).build();
            Post post = Post.builder().id(postId).build();
            Bookmark existing = Bookmark.builder().id(bookmarkId).user(user).post(post).build();

            when(userRepository.findById(MOCK_USER_ID)).thenReturn(Optional.of(user));
            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            when(bookmarkRepository.findByPostIdAndUserId(postId, MOCK_USER_ID))
                    .thenReturn(Optional.of(existing));

            bookmarkService.toggleBookmark(postId);

            ArgumentCaptor<Bookmark> captor = ArgumentCaptor.forClass(Bookmark.class);
            verify(bookmarkRepository).delete(captor.capture());
            assertEquals(bookmarkId, captor.getValue().getId());
        }

        @Test
        @DisplayName("Thất bại - Không tìm thấy user")
        void toggleBookmark_UserNotFound() {
            UUID postId = UUID.randomUUID();

            when(userRepository.findById(MOCK_USER_ID)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class,
                    () -> bookmarkService.toggleBookmark(postId));

            verify(postRepository, never()).findById(any());
            verify(bookmarkRepository, never()).findByPostIdAndUserId(any(), any());
            verify(bookmarkRepository, never()).save(any());
        }

        @Test
        @DisplayName("Thất bại - Không tìm thấy post")
        void toggleBookmark_PostNotFound() {
            UUID postId = UUID.randomUUID();
            User user = User.builder().id(MOCK_USER_ID).build();

            when(userRepository.findById(MOCK_USER_ID)).thenReturn(Optional.of(user));
            when(postRepository.findById(postId)).thenReturn(Optional.empty());

            assertThrows(PostNotFoundException.class,
                    () -> bookmarkService.toggleBookmark(postId));

            verify(bookmarkRepository, never()).findByPostIdAndUserId(any(), any());
            verify(bookmarkRepository, never()).save(any());
        }

        @Test
        @DisplayName("Thất bại - User không tìm thấy, không gọi postRepository")
        void toggleBookmark_UserNotFound_NoPostQuery() {
            UUID postId = UUID.randomUUID();

            when(userRepository.findById(MOCK_USER_ID)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class,
                    () -> bookmarkService.toggleBookmark(postId));

            verifyNoInteractions(postRepository);
        }
    }

    @Nested
    @DisplayName("Test getBookmarkedPosts()")
    class GetBookmarkedPostsTest {

        @Test
        @DisplayName("Thành công - Trả về danh sách bookmark")
        void getBookmarkedPosts_Success() {
            UUID postId = UUID.randomUUID();
            Post post = Post.builder().id(postId).build();
            Bookmark bookmark = Bookmark.builder()
                    .id(UUID.randomUUID()).post(post).build();
            PostResponse response = new PostResponse();

            when(bookmarkRepository.findByUserIdWithDetails(
                    eq(MOCK_USER_ID), isNull(), any(Limit.class)))
                    .thenReturn(List.of(bookmark));
            when(postMapper.mapToResponse(post)).thenReturn(response);

            CursorResponse<PostResponse> result =
                    bookmarkService.getBookmarkedPosts(null, 20);

            assertNotNull(result);
            assertFalse(result.getContent().isEmpty());
            assertEquals(1, result.getContent().size());
            verify(postMapper).mapToResponse(post);
        }

        @Test
        @DisplayName("Thành công - Không có bookmark trả về list rỗng")
        void getBookmarkedPosts_EmptyResult() {
            when(bookmarkRepository.findByUserIdWithDetails(
                    eq(MOCK_USER_ID), isNull(), any(Limit.class)))
                    .thenReturn(List.of());

            CursorResponse<PostResponse> result =
                    bookmarkService.getBookmarkedPosts(null, 20);

            assertNotNull(result);
            assertTrue(result.getContent().isEmpty());
            assertNull(result.getNextCursor());
            verifyNoInteractions(postMapper);
        }

        @Test
        @DisplayName("Thành công - Lấy bookmark với cursor")
        void getBookmarkedPosts_WithCursor() {
            UUID cursor = UUID.randomUUID();

            when(bookmarkRepository.findByUserIdWithDetails(
                    eq(MOCK_USER_ID), eq(cursor), any(Limit.class)))
                    .thenReturn(List.of());

            CursorResponse<PostResponse> result =
                    bookmarkService.getBookmarkedPosts(cursor, 10);

            assertNotNull(result);
            verify(bookmarkRepository).findByUserIdWithDetails(
                    eq(MOCK_USER_ID), eq(cursor), any(Limit.class));
        }

        @Test
        @DisplayName("Thành công - Có nextCursor khi còn trang tiếp theo")
        void getBookmarkedPosts_HasNextCursor() {
            int size = 2;
            // Trả về size+1 item để signal còn trang tiếp
            Post post1 = Post.builder().id(UUID.randomUUID()).build();
            Post post2 = Post.builder().id(UUID.randomUUID()).build();
            Post post3 = Post.builder().id(UUID.randomUUID()).build();

            UUID b1Id = UUID.randomUUID();
            UUID b2Id = UUID.randomUUID();
            UUID b3Id = UUID.randomUUID();

            Bookmark b1 = Bookmark.builder().id(b1Id).post(post1).build();
            Bookmark b2 = Bookmark.builder().id(b2Id).post(post2).build();
            Bookmark b3 = Bookmark.builder().id(b3Id).post(post3).build();

            when(bookmarkRepository.findByUserIdWithDetails(
                    eq(MOCK_USER_ID), isNull(), any(Limit.class)))
                    .thenReturn(List.of(b1, b2, b3));
            when(postMapper.mapToResponse(any())).thenReturn(new PostResponse());

            CursorResponse<PostResponse> result =
                    bookmarkService.getBookmarkedPosts(null, size);

            assertNotNull(result.getNextCursor());
            assertEquals(size, result.getContent().size());
        }

        @Test
        @DisplayName("Thành công - Không có nextCursor khi hết data")
        void getBookmarkedPosts_NoNextCursor_WhenLastPage() {
            Post post = Post.builder().id(UUID.randomUUID()).build();
            Bookmark bookmark = Bookmark.builder().id(UUID.randomUUID()).post(post).build();

            when(bookmarkRepository.findByUserIdWithDetails(
                    eq(MOCK_USER_ID), isNull(), any(Limit.class)))
                    .thenReturn(List.of(bookmark));
            when(postMapper.mapToResponse(post)).thenReturn(new PostResponse());

            CursorResponse<PostResponse> result =
                    bookmarkService.getBookmarkedPosts(null, 20);

            assertNull(result.getNextCursor());
        }

        @Test
        @DisplayName("Thành công - Limit được truyền là size + 1")
        void getBookmarkedPosts_LimitIsSizePlusOne() {
            int size = 10;

            when(bookmarkRepository.findByUserIdWithDetails(
                    eq(MOCK_USER_ID), isNull(), any(Limit.class)))
                    .thenReturn(List.of());

            bookmarkService.getBookmarkedPosts(null, size);

            ArgumentCaptor<Limit> limitCaptor = ArgumentCaptor.forClass(Limit.class);
            verify(bookmarkRepository).findByUserIdWithDetails(
                    eq(MOCK_USER_ID), isNull(), limitCaptor.capture());

            assertEquals(size + 1, limitCaptor.getValue().max());
        }
    }
}