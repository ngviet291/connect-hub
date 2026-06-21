package com.connecthub.modules.features.user.service;

import com.connecthub.common.dto.response.CursorResponse;
import com.connecthub.common.util.AppUtil;
import com.connecthub.modules.features.notification.entity.Notification;
import com.connecthub.modules.features.notification.enums.NotificationType;
import com.connecthub.modules.features.notification.repository.NotificationRepository;
import com.connecthub.modules.features.social.dto.FollowStats;
import com.connecthub.modules.features.social.entity.Follow;
import com.connecthub.modules.features.social.repository.FollowRepository;
import com.connecthub.modules.features.user.dto.request.UserStatusRequest;
import com.connecthub.modules.features.user.dto.request.UserUpdateRequest;
import com.connecthub.modules.features.user.dto.response.FollowResponse;
import com.connecthub.modules.features.user.dto.response.UserResponse;

import com.connecthub.modules.features.user.dto.response.UserSummaryResponse;
import com.connecthub.modules.features.user.entity.User;
import com.connecthub.modules.features.user.enums.UserStatus;
import com.connecthub.modules.features.user.exception.*;
import com.connecthub.modules.features.user.mapper.UserMapper;
import com.connecthub.modules.features.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
import com.connecthub.common.exception.AppException;
import com.connecthub.common.exception.ErrorCode;

import com.connecthub.common.service.MediaStorageService;
import com.connecthub.common.dto.response.UploadMediaResponse;
import com.connecthub.common.exception.UploadMediaException;
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
    private final NotificationRepository notificationRepository;
    private final MediaStorageService mediaStorageService;
    private static final long MAX_AVATAR_SIZE = 5L * 1024L * 1024L; // 5 MB

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void lockUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(UserNotFoundException::new);
        user.setActive(false);
        user.setLocked(true);
        userRepository.save(user);
    }

    // ADMIN
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        return userMapper.toUserResponse(getUserByIdOrThrow(id));
    }

    // USER
    @PreAuthorize("hasRole('ROLE_USER')")
    @Transactional(readOnly = true)
    public UserResponse getUserById() {
        return userMapper.toUserResponse(getCurrentUser());
    }

    // lấy danh sách followers của người dùng (ADMIN only)
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Transactional(readOnly = true)
    public CursorResponse<UserSummaryResponse> getFollowers(UUID userId, UUID cursor, int size) {
        return buildFollowersResponse(userId, cursor, size);
    }
    // lấy danh sách followers của người dùng hiện tại (USER)
    @PreAuthorize("hasRole('ROLE_USER')")
    @Transactional(readOnly = true)
    public CursorResponse<UserSummaryResponse> getFollowers(UUID cursor, int size) {
        UUID currentUserId = AppUtil.userIdFormAuthentication();
        return buildFollowersResponse(currentUserId, cursor, size);
    }

    // lấy danh sách following của người dùng nào đó (ADMIN only)
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Transactional(readOnly = true)
    public CursorResponse<UserSummaryResponse> getFollowing(UUID userId, UUID cursor, int size) {
        return buildFollowingResponse(userId, cursor, size);
    }

    // lấy danh sách following của người dùng hiện tại (USER)
    @PreAuthorize("hasRole('ROLE_USER')")
    @Transactional(readOnly = true)
    public CursorResponse<UserSummaryResponse> getFollowing(UUID cursor, int size) {
        UUID currentUserId = AppUtil.userIdFormAuthentication();
        return buildFollowingResponse(currentUserId, cursor, size);
    }
    @PreAuthorize("hasRole('ROLE_USER')")
    @Transactional
    public FollowResponse followUser(UUID targetUserId) {
        UUID currentUserId = AppUtil.userIdFormAuthentication();
        if (currentUserId.equals(targetUserId)) {
            throw new ConflictUserException();
        }
        if (followRepository.existsByFollowerIdAndFollowingId(currentUserId, targetUserId)) {
            return FollowResponse.builder()
                    .followerId(currentUserId)
                    .followingId(targetUserId)
                    .message("Already following this user")
                    .success(false)
                    .build();
        }

        User currentUser = getUserByIdOrThrow(currentUserId);
        User targetUser = getUserByIdOrThrow(targetUserId);
        followRepository.save(Follow.builder()
                .id(AppUtil.generateUUID())
                .follower(currentUser)
                .following(targetUser)
                .build());

        notificationRepository.save(Notification.builder()
                .id(AppUtil.generateUUID())
                .recipient(targetUser)
                .actor(currentUser)
                .content(currentUser.getFullName() + " started following you")
                .type(NotificationType.FOLLOW)
                .isRead(false)
                .build());

        return FollowResponse.builder()
                .followerId(currentUserId)
                .followingId(targetUserId)
                .message("Successfully followed user")
                .success(true)
                .build();
    }

    @PreAuthorize("hasRole('ROLE_USER')")
    @Transactional
    public FollowResponse unfollowUser(UUID targetUserId) {
        UUID currentUserId = AppUtil.userIdFormAuthentication();
        if (currentUserId.equals(targetUserId)) {
            throw new ConflictUserException();
        }
        if (!followRepository.existsByFollowerIdAndFollowingId(currentUserId, targetUserId)) {

             throw new UserNotFoundException();
        }

        followRepository.deleteByFollowerIdAndFollowingId(currentUserId, targetUserId);
        return FollowResponse.builder()
                .followerId(currentUserId)
                .followingId(targetUserId)
                .message("Successfully unfollowed user")
                .success(true)
                .build();
    }
    @PreAuthorize("hasRole('ROLE_USER')")
    @Transactional
    public UserResponse updateUser(UserUpdateRequest request) {

        // Update the currently authenticated user's profile (no id passed)
        UUID currentUserId = AppUtil.userIdFormAuthentication();
        User currentUser = getUserByIdOrThrow(currentUserId);

        // validate phone uniqueness if provided
        if (request.getPhoneNumber() != null) {
            if (userRepository.existsByPhoneNumberAndIdNot(request.getPhoneNumber(), currentUserId)) {
                throw new DuplicatePhoneNumberException("phone", request.getPhoneNumber());
            }
        }

        // map values from request into existing entity using MapStruct
        userMapper.updateUserFromRequest(request, currentUser);

        User saved = userRepository.save(currentUser);

        return userMapper.toUserResponse(saved);
    }

    @PreAuthorize("hasRole('ROLE_USER')")
    @Transactional
    public UserResponse uploadAvatar(MultipartFile file) {
        UUID currentUserId = AppUtil.userIdFormAuthentication();
        User currentUser = getUserByIdOrThrow(currentUserId);

        // basic validation
        if (file == null || file.isEmpty()) {
            throw new FileNotFoundException();
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw new InvalidFileTypeException();
        }

        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new FileSizeExceededException();
        }

        try {
            // upload new image first
            UploadMediaResponse uploadResponse = mediaStorageService
                    .uploadImage(file.getBytes(), "avatars")
                    .join();

            // delete old avatar in storage if exists (best-effort)
            if (currentUser.getPublicAvtId() != null) {
                try {
                    mediaStorageService.delete(currentUser.getPublicAvtId());
                } catch (Exception ex) {
                  throw new UploadMediaException();
                }
            }

            currentUser.setAvatarUrl(uploadResponse.getUrl());
            currentUser.setPublicAvtId(uploadResponse.getPublicId());

            User saved = userRepository.save(currentUser);
            return userMapper.toUserResponse(saved);
        } catch (Exception e) {
            // If the underlying cause is an IO problem, wrap and rethrow as UploadMediaException
            if (e instanceof UploadMediaException || (e.getCause() != null && e.getCause() instanceof UploadMediaException)) {
                throw new UploadMediaException();
            }
            throw new UploadMediaException();
        }
    }

    @PreAuthorize("hasRole('ROLE_USER')")
    @Transactional
    public void changeStatus(UserStatusRequest request) {
        UUID currentUserId = AppUtil.userIdFormAuthentication();
        User currentUser = getUserByIdOrThrow(currentUserId);

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
    }

    @PreAuthorize("hasRole('ROLE_USER')")
    @Transactional(readOnly = true)
    public FollowStats getStats() {
        UUID currentUserId = AppUtil.userIdFormAuthentication();
        return buildUserStats(currentUserId);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Transactional(readOnly = true)
    public FollowStats getStats(UUID userId) {
        return buildUserStats(userId);
    }
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    private User getUserByIdOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(UserNotFoundException::new);
    }

    private User getCurrentUser() {
        UUID currentUserId = AppUtil.userIdFormAuthentication();
        return userRepository.findById(currentUserId)
                .orElseThrow(UserNotFoundException::new);
    }

    private CursorResponse<UserSummaryResponse> buildFollowersResponse(UUID userId, UUID cursor, int size) {

        if(!userRepository.existsById(userId)){
            throw new UserNotFoundException();
        }
        List<Follow> follows = new ArrayList<>(followRepository.findFollowersOptimized(userId, cursor, Limit.of(size + 1)));
        boolean hasNext = follows.size() > size;
        if (hasNext) {
            follows.removeLast();
        }

        String nextCursor = follows.isEmpty() ? null : follows.getLast().getId().toString();
        return CursorResponse.<UserSummaryResponse>builder()
                .content(follows.stream().map(follow -> userMapper.toUserSummaryResponse(follow.getFollower())).toList())
                .hasNext(hasNext)
                .nextCursor(nextCursor)
                .build();
    }

    private CursorResponse<UserSummaryResponse> buildFollowingResponse(UUID userId, UUID cursor, int size) {
        if(!userRepository.existsById(userId)){
            throw new UserNotFoundException();
        }

        List<Follow> follows = new ArrayList<>(followRepository.findFollowingOptimized(userId, cursor, Limit.of(size + 1)));
        boolean hasNext = follows.size() > size;
        if (hasNext) {
            follows.removeLast();
        }

        String nextCursor = follows.isEmpty() ? null : follows.getLast().getId().toString();
        return CursorResponse.<UserSummaryResponse>builder()
                .content(follows.stream().map(follow -> userMapper.toUserSummaryResponse(follow.getFollowing())).toList())
                .hasNext(hasNext)
                .nextCursor(nextCursor)
                .build();
    }

    private FollowStats buildUserStats(UUID userId) {
        // kiểm tra user tồn tại trước khi đếm để tránh trả về 0 cho user không tồn tại
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException();
        }
        FollowStats stats =userRepository.countFollowStats(userId);
        return stats != null ? stats : new FollowStats(0L, 0L);


    }

}
