package com.connecthub.search;

import com.connecthub.common.dto.response.CursorResponse;
import com.connecthub.common.util.AppUtil;
import com.connecthub.modules.features.post.dto.response.PostResponse;
import com.connecthub.modules.features.post.entity.Hashtag;
import com.connecthub.modules.features.post.entity.Post;
import com.connecthub.modules.features.post.entity.PostHashtag;
import com.connecthub.modules.features.post.mapper.PostMapper;
import com.connecthub.modules.features.post.repository.HashtagRepository;
import com.connecthub.modules.features.post.repository.PostRepository;
import com.connecthub.modules.features.search.dto.response.HashtagSearchResponse;
import com.connecthub.modules.features.search.service.SearchService;
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
class SearchServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PostRepository postRepository;
    @Mock private HashtagRepository hashtagRepository;
    @Mock private PostMapper postMapper;

    @InjectMocks
    private SearchService searchService;

    private MockedStatic<AppUtil> mockedAppUtil;

    @BeforeEach
    void setUp() {
        mockedAppUtil = Mockito.mockStatic(AppUtil.class);
        // Cho phép buildCursorResponse chạy thực tế (logic cursor cần verify)
        mockedAppUtil.when(() -> AppUtil.buildCursorResponse(anyList(), anyInt(), any(), any()))
                .thenCallRealMethod();
    }

    @AfterEach
    void tearDown() {
        mockedAppUtil.close();
    }

    // Helper: tạo User active với fullName và username
    private User buildUser(String username, String fullName) {
        return User.builder()
                .id(UUID.randomUUID())
                .username(username)
                .fullName(fullName)
                .avatarUrl("https://cdn.com/avatar/" + username + ".jpg")
                .build();
    }

    // Helper: tạo Hashtag với số lượng PostHashtag nhất định
    private Hashtag buildHashtag(String name, int postCount) {
        Hashtag h = Hashtag.builder().id(UUID.randomUUID()).name(name).build();
        if (postCount > 0) {
            Set<PostHashtag> postHashtags = new HashSet<>();
            for (int i = 0; i < postCount; i++) {
                postHashtags.add(PostHashtag.builder()
                        .hashtag(h)
                        .post(Post.builder().id(UUID.randomUUID()).build())
                        .build());
            }
            h.setPostHashtags(postHashtags);
        }
        return h;
    }

    // =====================================================================
    // searchUsers()
    // =====================================================================
    @Nested
    @DisplayName("searchUsers()")
    class SearchUsersTest {

        @Test
        @DisplayName("Thành công - Trả về danh sách rỗng khi không có user khớp keyword")
        void searchUsers_NoMatch_ReturnsEmptyCursor() {
            when(userRepository.searchByNameOrUsername("ghost", null, Limit.of(6)))
                    .thenReturn(new ArrayList<>());

            CursorResponse<UserSummaryResponse> response =
                    searchService.searchUsers("ghost", null, 5);

            assertFalse(response.isHasNext());
            assertNull(response.getNextCursor());
            assertTrue(response.getContent().isEmpty());
        }

        @Test
        @DisplayName("Thành công - Trang cuối, map đúng fields UserSummaryResponse")
        void searchUsers_LastPage_MapsFieldsCorrectly() {
            User alice = buildUser("alice", "Alice Nguyen");
            User bob = buildUser("bob", "Bob Tran");

            when(userRepository.searchByNameOrUsername("ali", null, Limit.of(6)))
                    .thenReturn(new ArrayList<>(List.of(alice, bob)));

            CursorResponse<UserSummaryResponse> response =
                    searchService.searchUsers("ali", null, 5);

            assertFalse(response.isHasNext());
            assertNull(response.getNextCursor());
            assertEquals(2, response.getContent().size());

            UserSummaryResponse first = response.getContent().get(0);
            assertEquals(alice.getId(), first.getId());
            assertEquals("alice", first.getUsername());
            assertEquals("Alice Nguyen", first.getFullName());
            assertEquals("https://cdn.com/avatar/alice.jpg", first.getAvatarUrl());
        }

        @Test
        @DisplayName("Thành công - Có trang kế tiếp (DB trả về size+1 users)")
        void searchUsers_HasNextPage_ReturnsCorrectCursor() {
            int size = 2;
            User u1 = buildUser("user1", "User One");
            User u2 = buildUser("user2", "User Two");
            User u3 = buildUser("user3", "User Three"); // phần tử thừa

            when(userRepository.searchByNameOrUsername("user", null, Limit.of(size + 1)))
                    .thenReturn(new ArrayList<>(List.of(u1, u2, u3)));

            CursorResponse<UserSummaryResponse> response =
                    searchService.searchUsers("user", null, size);

            assertTrue(response.isHasNext());
            assertEquals(2, response.getContent().size());
            // nextCursor = id của phần tử cuối trong page (u2)
            assertEquals(u2.getId().toString(), response.getNextCursor());
        }

        @Test
        @DisplayName("Thành công - Có cursor, truyền đúng cursor vào repository")
        void searchUsers_WithCursor_PassesCursorToRepository() {
            UUID cursor = UUID.randomUUID();
            when(userRepository.searchByNameOrUsername("john", cursor, Limit.of(6)))
                    .thenReturn(new ArrayList<>());

            searchService.searchUsers("john", cursor, 5);

            verify(userRepository).searchByNameOrUsername("john", cursor, Limit.of(6));
        }

        @Test
        @DisplayName("Thành công - Keyword có khoảng trắng thừa được trim trước khi tìm")
        void searchUsers_KeywordWithWhitespace_IsTrimmed() {
            when(userRepository.searchByNameOrUsername("john", null, Limit.of(6)))
                    .thenReturn(new ArrayList<>());

            searchService.searchUsers("  john  ", null, 5);

            // Phải gọi với keyword đã được trim
            verify(userRepository).searchByNameOrUsername("john", null, Limit.of(6));
        }

        @Test
        @DisplayName("Thành công - Keyword chỉ có khoảng trắng, trim thành chuỗi rỗng")
        void searchUsers_WhitespaceOnlyKeyword_TrimsToEmpty() {
            when(userRepository.searchByNameOrUsername("", null, Limit.of(6)))
                    .thenReturn(new ArrayList<>());

            searchService.searchUsers("   ", null, 5);

            verify(userRepository).searchByNameOrUsername("", null, Limit.of(6));
        }

        @Test
        @DisplayName("Thành công - Size = 1, nextCursor trỏ đúng vào phần tử duy nhất trong page")
        void searchUsers_SizeOne_HasNextPage_CorrectCursor() {
            int size = 1;
            User u1 = buildUser("alice", "Alice");
            User u2 = buildUser("alex", "Alex"); // phần tử thừa

            when(userRepository.searchByNameOrUsername("al", null, Limit.of(size + 1)))
                    .thenReturn(new ArrayList<>(List.of(u1, u2)));

            CursorResponse<UserSummaryResponse> response =
                    searchService.searchUsers("al", null, size);

            assertTrue(response.isHasNext());
            assertEquals(1, response.getContent().size());
            assertEquals(u1.getId().toString(), response.getNextCursor());
        }
    }

    // =====================================================================
    // searchPosts()
    // =====================================================================
    @Nested
    @DisplayName("searchPosts()")
    class SearchPostsTest {

        @Test
        @DisplayName("Thành công - Không có bài viết khớp keyword, trả về cursor rỗng")
        void searchPosts_NoMatch_ReturnsEmptyCursor() {
            when(postRepository.searchIdsByKeyword("xyz404", null, Limit.of(6)))
                    .thenReturn(List.of());

            CursorResponse<PostResponse> response =
                    searchService.searchPosts("xyz404", null, 5);

            assertFalse(response.isHasNext());
            assertNull(response.getNextCursor());
            assertTrue(response.getContent().isEmpty());
            verify(postRepository, never()).findAllWithDetailsByIds(any());
            verify(postMapper, never()).mapToResponse(any());
        }

        @Test
        @DisplayName("Thành công - Trang cuối, map đúng Post → PostResponse")
        void searchPosts_LastPage_MapsPostsCorrectly() {
            int size = 5;
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();
            Post p1 = Post.builder().id(id1).build();
            Post p2 = Post.builder().id(id2).build();

            when(postRepository.searchIdsByKeyword("spring", null, Limit.of(size + 1)))
                    .thenReturn(List.of(id1, id2));
            when(postRepository.findAllWithDetailsByIds(List.of(id1, id2)))
                    .thenReturn(List.of(p1, p2));
            when(postMapper.mapToResponse(any(Post.class))).thenReturn(new PostResponse());

            CursorResponse<PostResponse> response =
                    searchService.searchPosts("spring", null, size);

            assertFalse(response.isHasNext());
            assertNull(response.getNextCursor());
            assertEquals(2, response.getContent().size());
            verify(postMapper, times(2)).mapToResponse(any(Post.class));
        }

        @Test
        @DisplayName("Thành công - Có trang kế tiếp (DB trả về size+1 IDs)")
        void searchPosts_HasNextPage_ReturnsCorrectCursor() {
            int size = 2;
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();
            UUID id3 = UUID.randomUUID(); // phần tử thừa → hasNext = true
            Post p1 = Post.builder().id(id1).build();
            Post p2 = Post.builder().id(id2).build();

            when(postRepository.searchIdsByKeyword("java", null, Limit.of(size + 1)))
                    .thenReturn(List.of(id1, id2, id3));
            when(postRepository.findAllWithDetailsByIds(List.of(id1, id2)))
                    .thenReturn(List.of(p1, p2));
            when(postMapper.mapToResponse(any(Post.class))).thenReturn(new PostResponse());

            CursorResponse<PostResponse> response =
                    searchService.searchPosts("java", null, size);

            assertTrue(response.isHasNext());
            assertEquals(2, response.getContent().size());
            // nextCursor = pageIds.getLast() = id2
            assertEquals(id2.toString(), response.getNextCursor());
        }

        @Test
        @DisplayName("Thành công - Có cursor, truyền đúng cursor vào repository")
        void searchPosts_WithCursor_PassesCursorToRepository() {
            UUID cursor = UUID.randomUUID();
            when(postRepository.searchIdsByKeyword("boot", cursor, Limit.of(6)))
                    .thenReturn(List.of());

            searchService.searchPosts("boot", cursor, 5);

            verify(postRepository).searchIdsByKeyword("boot", cursor, Limit.of(6));
            verify(postRepository, never()).findAllWithDetailsByIds(any());
        }

        @Test
        @DisplayName("Thành công - Keyword có khoảng trắng thừa được trim")
        void searchPosts_KeywordWithWhitespace_IsTrimmed() {
            when(postRepository.searchIdsByKeyword("spring", null, Limit.of(6)))
                    .thenReturn(List.of());

            searchService.searchPosts("  spring  ", null, 5);

            verify(postRepository).searchIdsByKeyword("spring", null, Limit.of(6));
        }

        @Test
        @DisplayName("Thành công - Posts được sort giảm dần theo ID trước khi map")
        void searchPosts_ResultsSortedByIdDescending() {
            int size = 5;
            // id2 > id1 theo UUID ordering
            UUID id1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
            UUID id2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
            Post p1 = Post.builder().id(id1).build();
            Post p2 = Post.builder().id(id2).build();

            // Repository trả về thứ tự id1, id2 (tăng dần)
            when(postRepository.searchIdsByKeyword("hello", null, Limit.of(size + 1)))
                    .thenReturn(List.of(id1, id2));
            // findAllWithDetailsByIds có thể trả về bất kỳ thứ tự nào
            when(postRepository.findAllWithDetailsByIds(anyList()))
                    .thenReturn(new ArrayList<>(List.of(p1, p2)));

            // Capture thứ tự map được gọi
            List<UUID> mappedOrder = new ArrayList<>();
            when(postMapper.mapToResponse(any(Post.class))).thenAnswer(inv -> {
                mappedOrder.add(((Post) inv.getArgument(0)).getId());
                return new PostResponse();
            });

            searchService.searchPosts("hello", null, size);

            // Sau sort giảm dần: id2 trước, id1 sau
            assertEquals(id2, mappedOrder.get(0));
            assertEquals(id1, mappedOrder.get(1));
        }

        @Test
        @DisplayName("Thành công - findAllWithDetailsByIds chỉ được gọi với pageIds (không có phần tử thừa)")
        void searchPosts_HasNextPage_FindDetailsCalledWithPageIdsOnly() {
            int size = 2;
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();
            UUID id3 = UUID.randomUUID();

            when(postRepository.searchIdsByKeyword("test", null, Limit.of(size + 1)))
                    .thenReturn(List.of(id1, id2, id3));
            when(postRepository.findAllWithDetailsByIds(List.of(id1, id2)))
                    .thenReturn(new ArrayList<>());

            searchService.searchPosts("test", null, size);

            // Phải gọi với pageIds = [id1, id2], KHÔNG có id3
            verify(postRepository).findAllWithDetailsByIds(List.of(id1, id2));
        }
    }

    // =====================================================================
    // searchHashtags()
    // =====================================================================
    @Nested
    @DisplayName("searchHashtags()")
    class SearchHashtagsTest {

        @Test
        @DisplayName("Thành công - Không có hashtag khớp keyword, trả về cursor rỗng")
        void searchHashtags_NoMatch_ReturnsEmptyCursor() {
            when(hashtagRepository.searchByName("zzz", null, Limit.of(6)))
                    .thenReturn(new ArrayList<>());

            CursorResponse<HashtagSearchResponse> response =
                    searchService.searchHashtags("zzz", null, 5);

            assertFalse(response.isHasNext());
            assertNull(response.getNextCursor());
            assertTrue(response.getContent().isEmpty());
        }

        @Test
        @DisplayName("Thành công - Trang cuối, map đúng fields HashtagSearchResponse")
        void searchHashtags_LastPage_MapsFieldsCorrectly() {
            Hashtag h1 = buildHashtag("java", 3);
            Hashtag h2 = buildHashtag("javascript", 7);

            when(hashtagRepository.searchByName("java", null, Limit.of(6)))
                    .thenReturn(new ArrayList<>(List.of(h1, h2)));

            CursorResponse<HashtagSearchResponse> response =
                    searchService.searchHashtags("java", null, 5);

            assertFalse(response.isHasNext());
            assertEquals(2, response.getContent().size());

            HashtagSearchResponse first = response.getContent().get(0);
            assertEquals(h1.getId(), first.getId());
            assertEquals("java", first.getName());
            assertEquals(3, first.getPostCount());
        }

        @Test
        @DisplayName("Thành công - Có trang kế tiếp (DB trả về size+1 hashtags)")
        void searchHashtags_HasNextPage_ReturnsCorrectCursor() {
            int size = 2;
            Hashtag h1 = buildHashtag("fun", 1);
            Hashtag h2 = buildHashtag("funny", 2);
            Hashtag h3 = buildHashtag("funky", 0); // phần tử thừa

            when(hashtagRepository.searchByName("fun", null, Limit.of(size + 1)))
                    .thenReturn(new ArrayList<>(List.of(h1, h2, h3)));

            CursorResponse<HashtagSearchResponse> response =
                    searchService.searchHashtags("fun", null, size);

            assertTrue(response.isHasNext());
            assertEquals(2, response.getContent().size());
            assertEquals(h2.getId().toString(), response.getNextCursor());
        }

        @Test
        @DisplayName("Thành công - Có cursor, truyền đúng cursor vào repository")
        void searchHashtags_WithCursor_PassesCursorToRepository() {
            UUID cursor = UUID.randomUUID();
            when(hashtagRepository.searchByName("spring", cursor, Limit.of(6)))
                    .thenReturn(new ArrayList<>());

            searchService.searchHashtags("spring", cursor, 5);

            verify(hashtagRepository).searchByName("spring", cursor, Limit.of(6));
        }

        @Test
        @DisplayName("Thành công - Keyword có khoảng trắng thừa được trim")
        void searchHashtags_KeywordWithWhitespace_IsTrimmed() {
            when(hashtagRepository.searchByName("dev", null, Limit.of(6)))
                    .thenReturn(new ArrayList<>());

            searchService.searchHashtags("  dev  ", null, 5);

            verify(hashtagRepository).searchByName("dev", null, Limit.of(6));
        }

        @Test
        @DisplayName("Thành công - postCount = 0 khi postHashtags = null")
        void searchHashtags_NullPostHashtags_PostCountIsZero() {
            Hashtag h = Hashtag.builder().id(UUID.randomUUID()).name("empty").postHashtags(null).build();

            when(hashtagRepository.searchByName("empty", null, Limit.of(6)))
                    .thenReturn(new ArrayList<>(List.of(h)));

            CursorResponse<HashtagSearchResponse> response =
                    searchService.searchHashtags("empty", null, 5);

            assertEquals(0, response.getContent().get(0).getPostCount());
        }

        @Test
        @DisplayName("Thành công - postCount = 0 khi postHashtags là Set rỗng")
        void searchHashtags_EmptyPostHashtags_PostCountIsZero() {
            Hashtag h = Hashtag.builder().id(UUID.randomUUID()).name("new").postHashtags(new HashSet<>()).build();

            when(hashtagRepository.searchByName("new", null, Limit.of(6)))
                    .thenReturn(new ArrayList<>(List.of(h)));

            CursorResponse<HashtagSearchResponse> response =
                    searchService.searchHashtags("new", null, 5);

            assertEquals(0, response.getContent().get(0).getPostCount());
        }

        @Test
        @DisplayName("Thành công - postCount đúng với số lượng PostHashtag thực tế")
        void searchHashtags_PostCountMatchesPostHashtagsSize() {
            Hashtag h = buildHashtag("backend", 5);

            when(hashtagRepository.searchByName("backend", null, Limit.of(6)))
                    .thenReturn(new ArrayList<>(List.of(h)));

            CursorResponse<HashtagSearchResponse> response =
                    searchService.searchHashtags("backend", null, 5);

            assertEquals(5, response.getContent().get(0).getPostCount());
        }

        @Test
        @DisplayName("Thành công - createdAt được map đúng từ entity")
        void searchHashtags_CreatedAtIsMapped() {
            java.time.LocalDateTime now = java.time.LocalDateTime.of(2024, 6, 1, 12, 0, 0);
            Hashtag h = Hashtag.builder().id(UUID.randomUUID()).name("test").postHashtags(new HashSet<>()).build();
            h.setCreatedAt(now);

            when(hashtagRepository.searchByName("test", null, Limit.of(6)))
                    .thenReturn(new ArrayList<>(List.of(h)));

            CursorResponse<HashtagSearchResponse> response =
                    searchService.searchHashtags("test", null, 5);

            assertEquals(now, response.getContent().get(0).getCreatedAt());
        }
    }
}