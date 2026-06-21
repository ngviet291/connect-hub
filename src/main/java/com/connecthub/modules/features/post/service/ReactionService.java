package com.connecthub.modules.features.post.service;

import com.connecthub.common.util.AppUtil;
import com.connecthub.modules.features.post.entity.Post;
import com.connecthub.modules.features.post.entity.Reaction;
import com.connecthub.modules.features.post.enums.ReactionType;
import com.connecthub.modules.features.post.exception.PostNotFoundException;
import com.connecthub.modules.features.post.repository.PostRepository;
import com.connecthub.modules.features.post.repository.ReactionRepository;
import com.connecthub.modules.features.user.entity.User;
import com.connecthub.modules.features.user.exception.UserNotFoundException;
import com.connecthub.modules.features.user.repository.UserRepository;
import com.github.f4b6a3.uuid.UuidCreator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
@Service
@RequiredArgsConstructor
@Slf4j
public class ReactionService {

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
}