package com.connecthub.modules.features.search.service;

import com.connecthub.common.dto.response.CursorResponse;
import com.connecthub.common.util.AppUtil;
import com.connecthub.modules.features.post.dto.response.HashtagPostCount;
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

import java.util.*;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
public class SearchService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final HashtagRepository hashtagRepository;
    private final PostMapper postMapper;

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
                        .build()
        );
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ROLE_USER')")
    public CursorResponse<PostResponse> searchPosts(String keyword, UUID cursor, int size) {
        List<UUID> ids = postRepository.searchIdsByKeyword(
                keyword.trim(), cursor, Limit.of(size + 1));

        boolean hasNext = ids.size() > size;
        List<UUID> pageIds = hasNext ? ids.subList(0, size) : ids;

        if (pageIds.isEmpty()) return emptyCursor();

        Map<UUID, Post> postMap = postRepository.findAllWithDetailsByIds(pageIds)
                .stream().collect(Collectors.toMap(Post::getId, p -> p));

        List<PostResponse> content = pageIds.stream()
                .map(postMap::get)
                .filter(Objects::nonNull)
                .map(postMapper::mapToResponse)
                .toList();

        return CursorResponse.<PostResponse>builder()
                .content(content)
                .hasNext(hasNext)
                .nextCursor(hasNext ? pageIds.getLast().toString() : null)
                .build();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ROLE_USER')")
    public CursorResponse<HashtagSearchResponse> searchHashtags(String keyword, UUID cursor, int size) {
        List<Hashtag> hashtags = hashtagRepository.searchByName(
                keyword.trim(), cursor, Limit.of(size + 1));

        boolean hasNext = hashtags.size() > size;
        List<Hashtag> page = hasNext ? hashtags.subList(0, size) : hashtags;

        if (page.isEmpty()) return emptyCursorHashtag();

        List<UUID> ids = page.stream().map(Hashtag::getId).toList();

        // Dùng projection thay Object[] để tránh ClassCastException
        Map<UUID, Long> countMap = hashtagRepository.countPostsByHashtagIds(ids)
                .stream()
                .collect(Collectors.toMap(
                        HashtagPostCount::getHashtagId,
                        HashtagPostCount::getPostCount
                ));

        List<HashtagSearchResponse> content = page.stream()
                .map(h -> HashtagSearchResponse.builder()
                        .id(h.getId())
                        .name(h.getName())
                        .postCount(countMap.getOrDefault(h.getId(), 0L))
                        .createdAt(h.getCreatedAt())
                        .build())
                .toList();

        return CursorResponse.<HashtagSearchResponse>builder()
                .content(content)
                .hasNext(hasNext)
                .nextCursor(hasNext ? page.getLast().getId().toString() : null)
                .build();
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