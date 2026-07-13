package com.connecthub.modules.features.social.service;

import com.connecthub.common.dto.response.CursorResponse;
import com.connecthub.common.util.AppUtil;
import com.connecthub.modules.features.chat.entity.Conversation;
import com.connecthub.modules.features.social.projection.FollowingRowProjection;
import com.connecthub.modules.features.social.repository.FollowRepository;
import com.connecthub.modules.features.user.dto.response.UserSummaryResponse;
import com.connecthub.modules.features.user.exception.UserNotFoundException;
import com.connecthub.modules.features.user.mapper.UserMapper;
import com.connecthub.modules.features.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Role;
import org.springframework.data.domain.Limit;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
@Slf4j
public class FollowService {
    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    /**
     * Check if two users are mutual followers
     *
     * @param userAId
     * @param userBId
     * @return <b>true</b> if both users follow each other, <b>false</b> otherwise
     */
    public boolean isMutualFollow(UUID userAId, UUID userBId) {
        boolean aFollowsB = followRepository.existsByFollowerIdAndFollowingId(userAId, userBId);
        boolean bFollowsA = followRepository.existsByFollowerIdAndFollowingId(userBId, userAId);

        log.info("isMutualFollow check: userA={} userB={} | aFollowsB={} (follower={}, following={}) | bFollowsA={} (follower={}, following={}) | mutual={}",
                userAId, userBId,
                aFollowsB, userAId, userBId,
                bFollowsA, userBId, userAId,
                aFollowsB && bFollowsA);

        return aFollowsB && bFollowsA;
    }

    @PreAuthorize("hasRole('USER')")
    @Transactional(readOnly = true)
    public CursorResponse<UserSummaryResponse> getFollowers(String username, UUID cursor, int size) {
        UUID currentUserId = AppUtil.userIdFromAuthentication();

        UUID userId = userRepository.findIdByUsername(username)
                .orElseThrow(UserNotFoundException::new);

        List<FollowingRowProjection> followingRowProjections = new ArrayList<>(
                followRepository.findFollowing(userId, currentUserId, cursor, Limit.of(size + 1))
        );

        return AppUtil.buildCursorResponse(followingRowProjections, size, FollowingRowProjection::getFollowId, userMapper::fromFollowingRowProjection);
    }

    @PreAuthorize("hasRole('USER')")
    @Transactional(readOnly = true)
    public CursorResponse<UserSummaryResponse> getFollowing(String username, UUID cursor, int size) {
        UUID currentUserId = AppUtil.userIdFromAuthentication();

        UUID userId = userRepository.findIdByUsername(username)
                .orElseThrow(UserNotFoundException::new);

        List<FollowingRowProjection> followersRowProjections = new ArrayList<>(
                followRepository.findFollowers(userId, currentUserId, cursor, Limit.of(size + 1))
        );

        return AppUtil.buildCursorResponse(followersRowProjections, size, FollowingRowProjection::getFollowId, userMapper::fromFollowingRowProjection);
    }
}
