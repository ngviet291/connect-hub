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
public class RepostService {

    private final RepostRepository repostRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;


    @Transactional
    @PreAuthorize("hasRole('ROLE_USER')")
    public boolean toggleRepost(UUID postId) {
        UUID userId = AppUtil.userIdFormAuthentication();
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        Post post = postRepository.findById(postId).orElseThrow(PostNotFoundException::new);

        return repostRepository.findByPostIdAndUserId(postId, userId)
                .map(existing -> {
                    //  Đã repost  xóa
                    repostRepository.delete(existing);
                    postRepository.decrementRepostCount(postId); //  giảm
                    log.info("User {} un-reposted post {}", userId, postId);
                    return false;
                })
                .orElseGet(() -> {
                    //  Chưa repost  thêm
                    Repost repost = Repost.builder()
                            .id(UuidCreator.getTimeOrderedEpoch())
                            .user(user)
                            .post(post)
                            .build();
                    repostRepository.save(repost);
                    postRepository.incrementRepostCount(postId); // tăng
                    log.info("User {} reposted post {}", userId, postId);
                    return true;
                });
    }

    @Transactional(readOnly = true)
    public boolean hasReposted(UUID postId) {
        UUID userId = AppUtil.userIdFormAuthentication();
        return repostRepository.existsByPostIdAndUserId(postId, userId);
    }
}