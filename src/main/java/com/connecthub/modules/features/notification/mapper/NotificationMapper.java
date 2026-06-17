package com.connecthub.modules.features.notification.mapper;

import com.connecthub.modules.features.notification.dto.response.NotificationResponse;
import com.connecthub.modules.features.notification.dto.response.PostSummaryResponse;
import com.connecthub.modules.features.notification.entity.Notification;
import com.connecthub.modules.features.post.entity.Post;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationMapper {
    @Mapping(target = "post", source = "post")
    @Mapping(target = "user.id", source = "recipient.id")
    @Mapping(target = "user.username", source = "recipient.username")
    @Mapping(target = "user.avatarUrl", source = "recipient.avatarUrl")
    NotificationResponse toNotificationResponse(Notification notification);

    PostSummaryResponse map(Post value);
}
