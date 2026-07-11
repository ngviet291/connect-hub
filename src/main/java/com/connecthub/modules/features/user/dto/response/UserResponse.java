package com.connecthub.modules.features.user.dto.response;

import com.connecthub.common.util.AppUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {

    private UUID id;
    private String username;
    private String email;
    private String fullName;
    private String phoneNumber;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String avatarUrl;
    private String bio;
    private Set<String> roles;
}