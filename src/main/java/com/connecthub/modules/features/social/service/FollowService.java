package com.connecthub.modules.features.social.service;

import com.connecthub.modules.features.social.repository.FollowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@RequiredArgsConstructor
@Service
public class FollowService {
    private final FollowRepository followRepository;

    /**
     * Check if two users are mutual followers
     * @param userAId
     * @param userBId
     * @return <b>true</b> if both users follow each other, <b>false</b> otherwise
     */
    public boolean isMutualFollow(UUID userAId, UUID userBId) {
        return followRepository.existsByFollowerIdAndFollowingId(userAId, userBId)
                && followRepository.existsByFollowerIdAndFollowingId(userBId, userAId);
    }
}
