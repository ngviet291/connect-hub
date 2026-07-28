package com.connecthub.modules.features.post.service;

import com.connecthub.common.dto.response.CursorResponse;
import com.connecthub.common.util.AppUtil;
import com.connecthub.modules.features.post.dto.projection.MyReactionProjection;
import com.connecthub.modules.features.post.dto.response.PostResponse;
import com.connecthub.modules.features.post.entity.Bookmark;
import com.connecthub.modules.features.post.entity.Post;
import com.connecthub.modules.features.post.enums.ReactionType;
import com.connecthub.modules.features.post.exception.PostNotFoundException;
import com.connecthub.modules.features.post.mapper.PostMapper;
import com.connecthub.modules.features.post.repository.BookmarkRepository;
import com.connecthub.modules.features.post.repository.PostRepository;
import com.connecthub.modules.features.post.repository.ReactionRepository;
import com.connecthub.modules.features.post.repository.RepostRepository;
import com.connecthub.modules.features.user.entity.User;
import com.connecthub.modules.features.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostMapper postMapper;
    private final ReactionRepository reactionRepository;
    private final RepostRepository repostRepository;

    // Giống hệt helper bên PostService/MentionService — query 1 lần loại reaction của
    // user hiện tại cho cả batch postId. Trả Map rỗng nếu list rỗng.
    private Map<UUID, ReactionType> findMyReactionTypes(UUID userId, List<UUID> postIds) {
        if (postIds.isEmpty()) return Map.of();
        return reactionRepository.findMyReactionTypes(userId, postIds).stream()
                .collect(Collectors.toMap(
                        MyReactionProjection::getPostId,
                        MyReactionProjection::getType));
    }

    @Transactional
    @PreAuthorize("hasRole('ROLE_USER')")
    public boolean toggleBookmark(UUID postId) {
        UUID userId = AppUtil.userIdFromAuthentication();

        return bookmarkRepository.findByPostIdAndUserId(postId, userId)
                .map(existing -> {
                    bookmarkRepository.delete(existing);
                    postRepository.decrementBookmarkCount(postId);
                    log.info("User {} unbookmarked post {}", userId, postId);
                    return false;
                })
                .orElseGet(() -> {
                    if (!postRepository.existsById(postId))
                        throw new PostNotFoundException();
                    User user = userRepository.getReferenceById(userId);
                    Post post = postRepository.getReferenceById(postId);

                    bookmarkRepository.save(Bookmark.builder()
                            .id(AppUtil.generateUUID())
                            .user(user)
                            .post(post)
                            .build());
                    postRepository.incrementBookmarkCount(postId);
                    log.info("User {} bookmarked post {}", userId, postId);
                    return true;
                });
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ROLE_USER')")
    public CursorResponse<PostResponse> getBookmarkedPosts(UUID cursor, int size) {
        UUID userId = AppUtil.userIdFromAuthentication();

        List<Bookmark> bookmarks = bookmarkRepository
                .findByUserIdWithDetails(userId, cursor, Limit.of(size + 1));

        List<UUID> postIds = bookmarks.stream().map(b -> b.getPost().getId()).toList();

        Map<UUID, ReactionType> myReactionByPostId = findMyReactionTypes(userId, postIds);
        Set<UUID> repostedIds = postIds.isEmpty() ? Set.of() : repostRepository.findRepostedPostIds(userId, postIds);

        return AppUtil.buildCursorResponse(
                bookmarks,
                size,
                Bookmark::getId,
                b -> postMapper.mapToResponse(
                        b.getPost(),
                        myReactionByPostId.get(b.getPost().getId()), // null nếu chưa react
                        repostedIds.contains(b.getPost().getId()),
                        true // bookmarked luôn = true — đây là danh sách bookmark của chính user
                ));
    }
}