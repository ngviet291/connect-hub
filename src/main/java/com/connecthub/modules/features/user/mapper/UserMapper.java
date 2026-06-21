package com.connecthub.modules.features.user.mapper;

import com.connecthub.modules.features.user.dto.request.UserCreateRequest;
import com.connecthub.modules.features.user.dto.request.UserUpdateRequest;
import com.connecthub.modules.features.user.dto.response.UserResponse;
import com.connecthub.modules.features.user.dto.response.UserSummaryResponse;
import com.connecthub.modules.features.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {

    User toUser(UserCreateRequest request);

    UserResponse toUserResponse(User user);
   // User toUserUpdateRequest(UserUpdateRequest request);

    // Update existing User entity from UserUpdateRequest
    void updateUserFromRequest(UserUpdateRequest request, @MappingTarget User user);
   // @Mapping(target = "verified", constant = "false")
    UserSummaryResponse toUserSummaryResponse(User user);
}
