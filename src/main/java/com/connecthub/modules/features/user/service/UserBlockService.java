package com.connecthub.modules.features.user.service;

import com.connecthub.modules.features.user.repository.UserBlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserBlockService {
    private final UserBlockRepository userBlockRepository;

    @Transactional(readOnly = true)
    public boolean isBlockedBy(UUID userId, UUID potentialBlockerId) {
        return userBlockRepository.existsByBlockedIdAndBlockerId(userId, potentialBlockerId);
    }
}
