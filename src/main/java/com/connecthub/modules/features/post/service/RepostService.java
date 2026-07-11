package com.connecthub.modules.features.post.service;

import com.connecthub.common.util.AppUtil;
import com.connecthub.modules.features.post.entity.Post;
import com.connecthub.modules.features.post.entity.Repost;
import com.connecthub.modules.features.post.exception.PostNotFoundException;
import com.connecthub.modules.features.post.repository.PostRepository;
import com.connecthub.modules.features.post.repository.RepostRepository;
import com.connecthub.modules.features.user.entity.User;
import com.connecthub.modules.features.user.exception.UserNotFoundException;
import com.connecthub.modules.features.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RepostService {

    private final RepostRepository repostRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Transactional
    @PreAuthorize("hasRole('ROLE_USER')")
    public boolean toggleRepost(UUID postId) {
        UUID userId = AppUtil.userIdFromAuthentication();

        return repostRepository.findByPostIdAndUserId(postId, userId)
                .map(existing -> {
                    repostRepository.delete(existing);
                    postRepository.decrementRepostCount(postId);
                    log.info("User {} un-reposted post {}", userId, postId);
                    return false;
                })
                .orElseGet(() -> {
                    // Chỉ load khi thực sự cần tạo mới
                    User user = userRepository.findById(userId)
                            .orElseThrow(UserNotFoundException::new);
                    Post post = postRepository.findById(postId)
                            .orElseThrow(PostNotFoundException::new);

                    repostRepository.save(Repost.builder()
                            .id(AppUtil.generateUUID())
                            .user(user)
                            .post(post)
                            .build());
                    postRepository.incrementRepostCount(postId);
                    log.info("User {} reposted post {}", userId, postId);
                    return true;
                });
    }

    @Transactional(readOnly = true)
    public boolean hasReposted(UUID postId) {
        UUID userId = AppUtil.userIdFromAuthentication();
        return repostRepository.existsByPostIdAndUserId(postId, userId);
    }
}