package com.connecthub.modules.features.post.mapper;

import com.connecthub.modules.features.post.dto.response.ReactionResponse;
import com.connecthub.modules.features.post.entity.Reaction;
import com.connecthub.modules.features.user.dto.response.UserSummaryResponse;
import com.connecthub.modules.features.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReactionMapper {

    ReactionResponse toReactionResponse(Reaction reaction);
}