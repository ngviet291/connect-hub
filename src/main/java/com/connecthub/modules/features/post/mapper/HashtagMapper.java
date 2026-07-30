package com.connecthub.modules.features.post.mapper;


import com.connecthub.modules.features.post.dto.projection.TrendingHashtagProjection;
import com.connecthub.modules.features.post.dto.response.HashtagResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface HashtagMapper {

    HashtagResponse toHashtagResponse(TrendingHashtagProjection trendingHashtagProjection);

}
