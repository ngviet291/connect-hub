package com.connecthub.modules.features.post.service;

import com.connecthub.common.util.AppUtil;
import com.connecthub.modules.features.post.entity.Post;
import com.connecthub.modules.features.post.entity.PostView;
import com.connecthub.modules.features.post.exception.PostNotFoundException;
import com.connecthub.modules.features.post.repository.PostRepository;
import com.connecthub.modules.features.post.repository.PostViewRepository;
import com.connecthub.modules.features.user.entity.User;
import com.connecthub.modules.features.user.exception.UserNotFoundException;
import com.connecthub.modules.features.user.repository.UserRepository;
import com.github.f4b6a3.uuid.UuidCreator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostViewService {

    private final PostViewRepository postViewRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Transactional
    @PreAuthorize("hasRole('ROLE_USER')")
    public void recordView(UUID postId) {
        UUID userId = AppUtil.userIdFormAuthentication();


        Post post = postRepository.getReferenceById(postId);
        User user = userRepository.getReferenceById(userId);

        postViewRepository.save(PostView.builder()
                .id(UuidCreator.getTimeOrderedEpoch())
                .post(post)
                .user(user)
                .viewedAt(LocalDateTime.now())
                .build());

        postRepository.incrementViewCount(postId);
        log.info("User {} viewed post {}", userId, postId);
    }
    @Transactional(readOnly = true)
    public long getViewCount(UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(PostNotFoundException::new);

        return post.getViewCount();
    }
}