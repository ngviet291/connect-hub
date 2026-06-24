package com.connecthub.modules.features.chat.mapper;

import com.connecthub.modules.features.chat.dto.response.ConversationMemberResponse;
import com.connecthub.modules.features.chat.entity.ConversationMember;
import com.connecthub.modules.features.user.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ConversationMapper {
    default ConversationMemberResponse toMemberResponse(ConversationMember member) {
        User user = member.getUser();
        return ConversationMemberResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .avatarUrl(user.getAvatarUrl())
                .role(member.getRole())
                .status(member.getStatus())
                .build();
    }


}
