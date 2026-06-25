package com.connecthub.modules.features.post.service;

import com.connecthub.common.dto.response.CursorResponse;
import com.connecthub.common.util.AppUtil;
import com.connecthub.modules.features.post.dto.response.ReactionCountResponse;
import com.connecthub.modules.features.post.dto.response.ReactionResponse;
import com.connecthub.modules.features.post.entity.Post;
import com.connecthub.modules.features.post.entity.Reaction;
import com.connecthub.modules.features.post.enums.ReactionType;
import com.connecthub.modules.features.post.exception.PostNotFoundException;
import com.connecthub.modules.features.post.repository.PostRepository;
import com.connecthub.modules.features.post.repository.ReactionRepository;
import com.connecthub.modules.features.user.dto.response.UserSummaryResponse;
import com.connecthub.modules.features.user.entity.User;
import com.connecthub.modules.features.user.repository.UserRepository;
import com.github.f4b6a3.uuid.UuidCreator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReactionService {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final ReactionRepository reactionRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Transactional
    @PreAuthorize("hasRole('ROLE_USER')")
    public boolean toggleReaction(UUID postId, ReactionType type) {
        UUID userId = AppUtil.userIdFormAuthentication();

        return reactionRepository.findByPostIdAndUserId(postId, userId)
                .map(existing -> {
                    reactionRepository.delete(existing);
                    postRepository.decrementReactionCount(postId);
                    log.info("User {} unreacted post {}", userId, postId);
                    return false;
                })
                .orElseGet(() -> {
                    reactionRepository.save(Reaction.builder()
                            .id(UuidCreator.getTimeOrderedEpoch())
                            .user(userRepository.getReferenceById(userId))
                            .post(postRepository.getReferenceById(postId))
                            .type(type)
                            .build());
                    postRepository.incrementReactionCount(postId);
                    log.info("User {} reacted post {} with {}", userId, postId, type);
                    return true;
                });
    }

    @Transactional(readOnly = true)
    public boolean hasReacted(UUID postId) {
        UUID userId = AppUtil.userIdFormAuthentication();
        return reactionRepository.existsByPostIdAndUserId(postId, userId);
    }

    /**
     * Lấy danh sách React của bài đăng (cursor-based pagination).
     *
     * @param postId ID bài đăng
     * @param cursor UUID cursor từ lần trước (null = trang đầu)
     * @param size   số phần tử mỗi trang (mặc định 20)
     */
    @Transactional(readOnly = true)
    public CursorResponse<ReactionResponse> getReactionsByPost(UUID postId, UUID cursor, int size) {
        if (!postRepository.existsById(postId)) {
            throw new PostNotFoundException();
        }

        int limit = size > 0 ? size : DEFAULT_PAGE_SIZE;
        List<Reaction> reactions = reactionRepository.findByPostIdWithUser(
                postId, cursor, Pageable.ofSize(limit + 1));

        boolean hasNext = reactions.size() > limit;
        List<Reaction> page = hasNext ? reactions.subList(0, limit) : reactions;

        List<ReactionResponse> content = page.stream()
                .map(this::toReactionResponse)
                .toList();

        String nextCursor = hasNext
                ? page.get(page.size() - 1).getId().toString()
                : null;

        return CursorResponse.<ReactionResponse>builder()
                .content(content)
                .hasNext(hasNext)
                .nextCursor(nextCursor)
                .build();
    }

    /**
     * Đếm số lượng React theo từng loại của bài đăng.
     *
     * @param postId ID bài đăng
     */
    @Transactional(readOnly = true)
    public List<ReactionCountResponse> countReactionsByType(UUID postId) {
        if (!postRepository.existsById(postId)) {
            throw new PostNotFoundException();
        }

        return reactionRepository.countByPostIdGroupByType(postId)
                .stream()
                .map(row -> ReactionCountResponse.builder()
                        .type(row.getType())
                        .count(row.getCount())
                        .build())
                .toList();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private ReactionResponse toReactionResponse(Reaction reaction) {
        User user = reaction.getUser();
        return ReactionResponse.builder()
                .id(reaction.getId())
                .type(reaction.getType())
                .createdAt(reaction.getCreatedAt())
                .user(UserSummaryResponse.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .fullName(user.getFullName())
                        .avatarUrl(user.getAvatarUrl())
                        .build())
                .build();
    }
}