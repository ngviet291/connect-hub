package com.connecthub.modules.features.user.mapper;

import com.connecthub.modules.features.notification.dto.response.NotificationUserSummaryResponse;
import com.connecthub.modules.features.social.projection.FollowingRowProjection;
import com.connecthub.modules.features.user.dto.request.UserCreateRequest;
import com.connecthub.modules.features.user.dto.request.UserUpdateRequest;
import com.connecthub.modules.features.user.dto.response.UserResponse;
import com.connecthub.modules.features.user.dto.response.UserSummaryResponse;
import com.connecthub.modules.features.user.entity.Role;
import com.connecthub.modules.features.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserMapper {

    User toUser(UserCreateRequest request);

    @Mapping(target = "isFollowing", ignore = true)
    @Mapping(target = "roles", source = "roles", qualifiedByName = "rolesToNames")
    UserResponse toUserResponse(User user);

    @Named("rolesToNames")
    default Set<String> rolesToNames(Set<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return Set.of();
        }
        return roles.stream()
                .map(role -> role.getName().toString())
                .collect(Collectors.toSet());
    }

    // User toUserUpdateRequest(UserUpdateRequest request);
    default UserResponse toUserResponse(User user, boolean isFollowing) {
        UserResponse res = toUserResponse(user);
        res.setFollowing(isFollowing); // Lombok @Data sinh setFollowing(), KHÔNG phải setIsFollowing()
        return res;
    }

    // Update existing User entity from UserUpdateRequest
    void updateUserFromRequest(UserUpdateRequest request, @MappingTarget User user);

    UserSummaryResponse toUserSummaryResponse(User user);

    NotificationUserSummaryResponse toNotificationUserSummaryResponse(UUID id, String username, String avatarUrl);

    UserSummaryResponse fromFollowingRowProjection(FollowingRowProjection followingRowProjection);
}
