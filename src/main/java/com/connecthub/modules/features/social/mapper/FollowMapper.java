package com.connecthub.modules.features.social.mapper;

import com.connecthub.modules.features.social.dto.projection.SuggestedUserProjection;
import com.connecthub.modules.features.social.dto.response.SuggestedUserResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FollowMapper {

    SuggestedUserResponse fromSuggestedUserProjection(SuggestedUserProjection suggestedUserProjection);
}
