package com.connecthub.modules.features.moderation.mapper;

import com.connecthub.modules.features.moderation.dto.request.ban.CreateBanRequest;
import com.connecthub.modules.features.moderation.dto.response.ban.BanResponse;
import com.connecthub.modules.features.moderation.entity.Ban;
import io.swagger.v3.oas.annotations.media.Schema;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BanMapper {

    @Mapping(target = "bannedId", source = "bannedBy.id")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "unbannedById", source = "unbannedBy.id")
    @Mapping(target = "unbannedByUsername", source = "unbannedBy.username")
    @Mapping(target = "active", expression = "java(ban.isActive())")
    BanResponse toBanResponse(Ban ban);


    Ban toBan(CreateBanRequest request);
}
