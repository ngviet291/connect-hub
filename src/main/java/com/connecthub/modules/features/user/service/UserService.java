package com.connecthub.modules.features.user.service;

import com.connecthub.modules.features.user.entity.User;
import com.connecthub.modules.features.user.exception.UserNotFoundException;
import com.connecthub.modules.features.user.repository.UserRepository;
import jakarta.persistence.Id;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class UserService {
    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void lockUser(String username) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(UserNotFoundException::new);
        user.setActive(false);
        user.setLocked(true);
        userRepository.save(user);

    }

}
