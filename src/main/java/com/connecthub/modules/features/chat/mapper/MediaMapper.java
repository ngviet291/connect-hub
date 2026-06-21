package com.connecthub.modules.features.chat.mapper;

import com.connecthub.modules.features.chat.entity.MessageMedia;
import com.connecthub.modules.features.post.dto.response.MediaResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MediaMapper {

    @Mapping(target = "mediaId", source = "id")
    MediaResponse toResponse(MessageMedia media);
}