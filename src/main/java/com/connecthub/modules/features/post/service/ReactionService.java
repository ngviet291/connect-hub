package com.connecthub.modules.features.post.service;

import com.connecthub.common.dto.response.CursorResponse;
import com.connecthub.common.util.AppUtil;
import com.connecthub.modules.features.post.dto.response.ReactionCountResponse;
import com.connecthub.modules.features.post.dto.response.ReactionResponse;
import com.connecthub.modules.features.post.entity.Post;
import com.connecthub.modules.features.post.entity.Reaction;
import com.connecthub.modules.features.post.enums.ReactionType;
import com.connecthub.modules.features.post.exception.PostNotFoundException;
import com.connecthub.modules.features.post.mapper.ReactionMapper;
import com.connecthub.modules.features.post.repository.PostRepository;
import com.connecthub.modules.features.post.repository.ReactionRepository;
import com.connecthub.modules.features.user.dto.response.UserSummaryResponse;
import com.connecthub.modules.features.user.entity.User;
import com.connecthub.modules.features.user.exception.UserNotFoundException;
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

    private final ReactionRepository reactionRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ReactionMapper reactionMapper;

    @Transactional
    @PreAuthorize("hasRole('ROLE_USER')")
    public boolean toggleReaction(UUID postId, ReactionType type) {
        UUID userId = AppUtil.userIdFromAuthentication();

        return reactionRepository.findByPostIdAndUserId(postId, userId)
                .map(existing -> {
                    reactionRepository.delete(existing);
                    postRepository.decrementReactionCount(postId);
                    log.info("User {} unreacted post {}", userId, postId);
                    return false;
                })
                .orElseGet(() -> {
                    reactionRepository.save(Reaction.builder()
                            .id(AppUtil.generateUUID())
                            .user(userRepository.findById(userId).orElseThrow(UserNotFoundException::new))
                            .post(postRepository.findById(postId).orElseThrow(PostNotFoundException::new))
                            .type(type)
                            .build());
                    postRepository.incrementReactionCount(postId);
                    log.info("User {} reacted post {} with {}", userId, postId, type);
                    return true;
                });
    }

    @Transactional(readOnly = true)
    public boolean hasReacted(UUID postId) {
        UUID userId = AppUtil.userIdFromAuthentication();
        return reactionRepository.existsByPostIdAndUserId(postId, userId);
    }

    @Transactional(readOnly = true)
    public CursorResponse<ReactionResponse> getReactionsByPost(UUID postId, UUID cursor, int size) {
        List<Reaction> reactions = reactionRepository.findByPostIdWithUser(
                postId, cursor, Pageable.ofSize(size + 1));

        boolean hasNext = reactions.size() > size;
        List<Reaction> page = hasNext ? reactions.subList(0, size) : reactions;

        return CursorResponse.<ReactionResponse>builder()
                .content(page.stream().map(reactionMapper::toReactionResponse).toList())
                .hasNext(hasNext)
                .nextCursor(hasNext ? page.getLast().getId().toString() : null)
                .build();
    }

    @Transactional(readOnly = true)
    public List<ReactionCountResponse> countReactionsByType(UUID postId) {
        return reactionRepository.countByPostIdGroupByType(postId)
                .stream()
                .map(row -> ReactionCountResponse.builder()
                        .type(row.getType())
                        .count(row.getCount())
                        .build())
                .toList();
    }

}