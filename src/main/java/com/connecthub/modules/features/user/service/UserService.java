package com.connecthub.modules.features.user.service;

import com.connecthub.common.dto.response.CursorResponse;
import com.connecthub.common.util.AppUtil;
import com.connecthub.modules.features.social.entity.Follow;
import com.connecthub.modules.features.social.repository.FollowRepository;
import com.connecthub.modules.features.user.dto.request.UserStatusRequest;
import com.connecthub.modules.features.user.dto.request.UserUpdateRequest;
import com.connecthub.modules.features.user.dto.response.UserResponse;
import com.connecthub.modules.features.user.dto.response.UserStatsResponse;
import com.connecthub.modules.features.user.dto.response.UserSummaryResponse;
import com.connecthub.modules.features.user.entity.User;
// ...existing code... (removed unused import)
import com.connecthub.modules.features.user.enums.UserStatus;
import com.connecthub.modules.features.user.exception.AccessDeniedException;
import com.connecthub.modules.features.user.exception.ConflictUserException;
import com.connecthub.modules.features.user.exception.DuplicatePhoneNumberException;
import com.connecthub.modules.features.user.exception.UserNotFoundException;
import com.connecthub.modules.features.user.mapper.UserMapper;
import com.connecthub.modules.features.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final FollowRepository followRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void lockUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(UserNotFoundException::new);
        user.setActive(false);
        user.setLocked(true);
        userRepository.save(user);
    }

    //    @Transactional(readOnly = true)
//    public CursorResponse<UserResponse>getAllUser(){
//        List<User> users = userRepository.findAll();
//        return CursorResponse.<UserResponse>builder()
//                .content(users.stream().map(userMapper::toUserResponse).toList())
//                .hasNext(false)
//                .nextCursor(null)
//                .build();
//    }
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        return userMapper.toUserResponse(getUserOrThrow(id));
    }

    // lấy danh sách followers của người dùng
    @Transactional(readOnly = true)
    public CursorResponse<UserSummaryResponse> getFollowers(UUID userId, UUID cursor, int size) {
        getUserOrThrow(userId);

        List<Follow> follows = new ArrayList<>(followRepository.findFollowers(userId, cursor, Limit.of(size + 1)));
        boolean hasNext = follows.size() > size;

        if (hasNext) {
            follows.removeLast();
        }

        String nextCursor = follows.isEmpty() ? null : follows.getLast().getId().toString();

        return CursorResponse.<UserSummaryResponse>builder()
                .content(
                        follows.stream()
                                .map(follow -> userMapper.toUserSummaryResponse(follow.getFollower()))
                                .toList()
                )
                .hasNext(hasNext)
                .nextCursor(nextCursor)
                .build();
    }

    @Transactional(readOnly = true)
    public CursorResponse<UserSummaryResponse> getFollowing(UUID userId, UUID cursor, int size) {
        getUserOrThrow(userId);

        List<Follow> follows = new ArrayList<>(followRepository.findFollowing(userId, cursor, Limit.of(size + 1)));
        boolean hasNext = follows.size() > size;

        if (hasNext) {
            follows.removeLast();
        }

        String nextCursor = follows.isEmpty() ? null : follows.getLast().getId().toString();

        return CursorResponse.<UserSummaryResponse>builder()
                .content(
                        follows.stream()
                                .map(follow -> userMapper.toUserSummaryResponse(follow.getFollowing()))
                                .toList()
                )
                .hasNext(hasNext)
                .nextCursor(nextCursor)
                .build();
    }

    @Transactional
    public void followUser(UUID targetUserId)  {
        String username = AppUtil.usernameFromAuthentication();
        User currentUser = getCurrentUser();
        User targetUser = getUserOrThrow(targetUserId);
        if (currentUser.getId().equals(targetUser.getId())) {
            throw new ConflictUserException();
        }
        if (followRepository.existsByFollowerIdAndFollowingId(
                currentUser.getId(),
                targetUser.getId())) {
            return;
        }
        followRepository.save(Follow.builder()
                .id(AppUtil.generateUUID())
                .follower(currentUser)
                .following(targetUser)
                .build());
    }

    @Transactional
    public void unfollowUser(UUID targetUserId) {
        User currentUser = getCurrentUser();
        User targetUser = getUserOrThrow(targetUserId);

        if (currentUser.getId().equals(targetUser.getId())) {
            throw new AccessDeniedException();
        }

        followRepository.deleteByFollowerIdAndFollowingId(currentUser.getId(), targetUser.getId());
    }
    @PreAuthorize("hasRole('ROLE_USER')")
    @Transactional
    public UserResponse updateUser(UserUpdateRequest request) {

        // Update the currently authenticated user's profile (no id passed)
        User currentUser = getCurrentUser();

        // validate phone uniqueness if provided
        if (request.getPhoneNumber() != null) {
            if (userRepository.existsByPhoneNumberAndIdNot(request.getPhoneNumber(), currentUser.getId())) {
                throw new DuplicatePhoneNumberException("phone", request.getPhoneNumber());
            }
        }

        // map values from request into existing entity using MapStruct
        userMapper.updateUserFromRequest(request, currentUser);

        User saved = userRepository.save(currentUser);

        return userMapper.toUserResponse(saved);
    }


    @Transactional
    public void changeStatus(UserStatusRequest request) {
        User currentUser = getCurrentUser();

        UserStatus status = request.getStatus();
        currentUser.setStatus(status);

        switch (status) {
            case ACTIVE -> {
                currentUser.setActive(true);
                currentUser.setLocked(false);
            }
            case INACTIVE -> {
                currentUser.setActive(false);
                currentUser.setLocked(false);
            }
            case BANNED -> {
                currentUser.setActive(false);
                currentUser.setLocked(true);
            }
        }

        userRepository.save(currentUser);
    }

    @Transactional(readOnly = true)
    public UserStatsResponse getStats(UUID id) {
        getUserOrThrow(id);

        return UserStatsResponse.builder()
                .followersCount(followRepository.countByFollowingId(id))
                .followingCount(followRepository.countByFollowerId(id))
                .build();
    }

    private User getCurrentUser() {
        String username = AppUtil.usernameFromAuthentication();

        System.out.println("Finding current user: " + username);

        return userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    System.out.println("CURRENT USER NOT FOUND");
                    return new UserNotFoundException();
                });
    }

    private User getUserOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(UserNotFoundException::new);
    }

//    private void assertCanModifyUser(User currentUser, User targetUser) {
//        boolean isAdmin = currentUser.getRoles() != null && currentUser.getRoles().stream()
//                .anyMatch(role -> role.getName() == RoleName.ROLE_ADMIN);
//
//        if (!currentUser.getId().equals(targetUser.getId()) && !isAdmin) {
//            throw new AccessDeniedException();
//        }
//    }
}
