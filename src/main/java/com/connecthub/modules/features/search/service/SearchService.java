package com.connecthub.modules.features.search.service;

import com.connecthub.common.dto.response.CursorResponse;
import com.connecthub.common.util.AppUtil;
import com.connecthub.modules.features.post.dto.projection.HashtagPostCountProjection;
import com.connecthub.modules.features.post.dto.projection.MyReactionProjection;
import com.connecthub.modules.features.post.dto.response.PostResponse;
import com.connecthub.modules.features.post.entity.Hashtag;
import com.connecthub.modules.features.post.entity.Post;
import com.connecthub.modules.features.post.enums.ReactionType;
import com.connecthub.modules.features.post.mapper.PostMapper;
import com.connecthub.modules.features.post.repository.BookmarkRepository;
import com.connecthub.modules.features.post.repository.HashtagRepository;
import com.connecthub.modules.features.post.repository.PostRepository;
import com.connecthub.modules.features.post.repository.ReactionRepository;
import com.connecthub.modules.features.post.repository.RepostRepository;
import com.connecthub.modules.features.search.dto.response.HashtagSearchResponse;
import com.connecthub.modules.features.user.dto.response.UserSummaryResponse;
import com.connecthub.modules.features.user.entity.User;
import com.connecthub.modules.features.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final HashtagRepository hashtagRepository;
    private final PostMapper postMapper;
    // Cần cho batch-check reacted/reposted/bookmarked trong searchPosts —
    // xem giải thích trong PostService (cùng pattern, tránh N+1).
    private final ReactionRepository reactionRepository;
    private final RepostRepository repostRepository;
    private final BookmarkRepository bookmarkRepository;

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ROLE_USER')")
    public CursorResponse<UserSummaryResponse> searchUsers(String keyword, UUID cursor, int size) {
        List<User> users = userRepository.searchByNameOrUsername(
                keyword.trim(), cursor, Limit.of(size + 1));

        return AppUtil.buildCursorResponse(
                users, size,
                User::getId,
                u -> UserSummaryResponse.builder()
                        .id(u.getId())
                        .username(u.getUsername())
                        .fullName(u.getFullName())
                        .avatarUrl(u.getAvatarUrl())
                        .build());
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ROLE_USER')")
    public CursorResponse<PostResponse> searchPosts(
            String keyword,
            UUID cursor,
            int size) {
        List<UUID> ids = postRepository.searchIdsByKeyword(
                keyword.trim(),
                cursor,
                Limit.of(size + 1));

        if (ids.isEmpty()) {
            return emptyCursor();
        }

        Map<UUID, Post> postMap = postRepository.findAllWithDetailsByIds(ids)
                .stream()
                .collect(Collectors.toMap(Post::getId, p -> p));

        List<Post> posts = ids.stream()
                .map(postMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // Endpoint này có @PreAuthorize -> luôn có user đăng nhập, không cần
        // check null như getPost(). Batch 1 lần cho cả trang kết quả tìm kiếm.
        UUID userId = AppUtil.userIdFromAuthentication();
        List<UUID> pageIds = posts.stream()
                .map(Post::getId)
                .toList();

        Map<UUID, ReactionType> myReactionByPostId =
                reactionRepository.findMyReactionTypes(userId, pageIds)
                        .stream()
                        .collect(Collectors.toMap(
                                MyReactionProjection::getPostId,
                                MyReactionProjection::getType
                        ));

        Set<UUID> repostedIds =
                repostRepository.findRepostedPostIds(userId, pageIds);

        Set<UUID> bookmarkedIds =
                bookmarkRepository.findBookmarkedPostIds(userId, pageIds);
        return AppUtil.buildCursorResponse(
                posts,
                size,
                Post::getId,
                p -> postMapper.mapToResponse(
                        p,
                        myReactionByPostId.get(p.getId()),
                        repostedIds.contains(p.getId()),
                        bookmarkedIds.contains(p.getId())
                )
        );
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ROLE_USER')")
    public CursorResponse<HashtagSearchResponse> searchHashtags(
            String keyword,
            UUID cursor,
            int size) {
        List<Hashtag> hashtags = hashtagRepository.searchByName(
                keyword.trim(),
                cursor,
                Limit.of(size + 1));

        if (hashtags.isEmpty()) {
            return emptyCursorHashtag();
        }

        List<UUID> ids = hashtags.stream()
                .map(Hashtag::getId)
                .toList();

        Map<UUID, Long> countMap = hashtagRepository.countPostsByHashtagIds(ids)
                .stream()
                .collect(Collectors.toMap(
                        HashtagPostCountProjection::getHashtagId,
                        HashtagPostCountProjection::getPostCount));

        List<HashtagSearchResponse> content = hashtags.stream()
                .map(h -> HashtagSearchResponse.builder()
                        .id(h.getId())
                        .name(h.getName())
                        .postCount(
                                countMap.getOrDefault(
                                        h.getId(),
                                        0L))
                        .createdAt(h.getCreatedAt())
                        .build())
                .collect(Collectors.toCollection(ArrayList::new));

        return AppUtil.buildCursorResponse(
                content,
                size,
                HashtagSearchResponse::getId,
                Function.identity());
    }

    private CursorResponse<PostResponse> emptyCursor() {
        return CursorResponse.<PostResponse>builder()
                .content(List.of()).hasNext(false).nextCursor(null).build();
    }

    private CursorResponse<HashtagSearchResponse> emptyCursorHashtag() {
        return CursorResponse.<HashtagSearchResponse>builder()
                .content(List.of()).hasNext(false).nextCursor(null).build();
    }
}