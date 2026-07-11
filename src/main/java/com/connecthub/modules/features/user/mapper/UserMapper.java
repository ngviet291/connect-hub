package com.connecthub.modules.features.user.mapper;

import com.connecthub.modules.features.notification.dto.response.NotificationUserSummaryResponse;
import com.connecthub.modules.features.social.projection.FollowingRowProjection;
import com.connecthub.modules.features.user.dto.request.UserCreateRequest;
import com.connecthub.modules.features.user.dto.request.UserUpdateRequest;
import com.connecthub.modules.features.user.dto.response.UserResponse;
import com.connecthub.modules.features.user.dto.response.UserSummaryResponse;
import com.connecthub.modules.features.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface UserMapper {

    User toUser(UserCreateRequest request);
    @Mapping(
            target = "roles",
            expression = "java(user.getRoles().stream()"
                    + ".map(role -> role.getName().toString())"
                    + ".collect(java.util.stream.Collectors.toSet()))"
    )
    UserResponse toUserResponse(User user);
    // User toUserUpdateRequest(UserUpdateRequest request);

    // Update existing User entity from UserUpdateRequest
    void updateUserFromRequest(UserUpdateRequest request, @MappingTarget User user);

    UserSummaryResponse toUserSummaryResponse(User user);

    NotificationUserSummaryResponse toNotificationUserSummaryResponse(UUID id, String username, String avatarUrl);

    UserSummaryResponse fromFollowingRowProjection(FollowingRowProjection followingRowProjection);
}
