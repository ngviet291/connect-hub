package com.connecthub.modules.features.social.service;

import com.connecthub.modules.features.social.repository.FollowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@RequiredArgsConstructor
@Service
@Slf4j
public class FollowService {
    private final FollowRepository followRepository;

    /**
     * Check if two users are mutual followers
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
}
