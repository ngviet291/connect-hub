package com.connecthub.modules.features.search.service;

import com.connecthub.common.dto.response.CursorResponse;
import com.connecthub.common.util.AppUtil;
import com.connecthub.modules.features.post.dto.response.PostResponse;
import com.connecthub.modules.features.post.entity.Hashtag;
import com.connecthub.modules.features.post.entity.Post;
import com.connecthub.modules.features.post.mapper.PostMapper;
import com.connecthub.modules.features.post.repository.HashtagRepository;
import com.connecthub.modules.features.post.repository.PostRepository;
import com.connecthub.modules.features.search.dto.response.HashtagSearchResponse;
import com.connecthub.modules.features.user.dto.response.UserSummaryResponse;
import com.connecthub.modules.features.user.entity.User;
import com.connecthub.modules.features.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final HashtagRepository hashtagRepository;
    private final PostMapper postMapper;

    /**
     * GET /v1/search/users?keyword=john
     * Tìm kiếm người dùng theo tên hoặc username
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ROLE_USER')")
    public CursorResponse<UserSummaryResponse> searchUsers(String keyword, UUID cursor, int size) {
        List<User> users = new ArrayList<>(
                userRepository.searchByNameOrUsername(keyword.trim(), cursor, Limit.of(size + 1))
        );

        return AppUtil.buildCursorResponse(
                users, size,
                User::getId,
                u -> UserSummaryResponse.builder()
                        .id(u.getId())
                        .username(u.getUsername())
                        .fullName(u.getFullName())
                        .avatarUrl(u.getAvatarUrl())
                        .build()
        );
    }

    /**
     * GET /v1/search/posts?keyword=spring
     * Tìm kiếm bài đăng theo từ khoá trong nội dung
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ROLE_USER')")
    public CursorResponse<PostResponse> searchPosts(String keyword, UUID cursor, int size) {
        List<UUID> ids = postRepository.searchIdsByKeyword(keyword.trim(), cursor, Limit.of(size + 1));

        boolean hasNext = ids.size() > size;
        List<UUID> pageIds = hasNext ? ids.subList(0, size) : ids;

        if (pageIds.isEmpty()) {
            return CursorResponse.<PostResponse>builder()
                    .content(List.of()).hasNext(false).nextCursor(null).build();
        }

        List<Post> posts = new ArrayList<>(postRepository.findAllWithDetailsByIds(pageIds));
        posts.sort((a, b) -> b.getId().compareTo(a.getId()));

        return CursorResponse.<PostResponse>builder()
                .content(posts.stream().map(postMapper::mapToResponse).toList())
                .hasNext(hasNext)
                .nextCursor(hasNext ? pageIds.getLast().toString() : null)
                .build();
    }

    /**
     * GET /v1/search/hashtags?keyword=fun
     * Tìm kiếm hashtag theo tên
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ROLE_USER')")
    public CursorResponse<HashtagSearchResponse> searchHashtags(String keyword, UUID cursor, int size) {
        List<Hashtag> hashtags = new ArrayList<>(
                hashtagRepository.searchByName(keyword.trim(), cursor, Limit.of(size + 1))
        );

        return AppUtil.buildCursorResponse(
                hashtags, size,
                Hashtag::getId,
                h -> HashtagSearchResponse.builder()
                        .id(h.getId())
                        .name(h.getName())
                        .postCount(h.getPostHashtags() != null ? h.getPostHashtags().size() : 0)
                        .createdAt(h.getCreatedAt())
                        .build()
        );
    }
}
