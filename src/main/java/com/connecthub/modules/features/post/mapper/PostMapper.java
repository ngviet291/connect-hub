package com.connecthub.modules.features.post.mapper;

import com.connecthub.modules.features.post.dto.request.PostRequest;
import com.connecthub.modules.features.post.dto.response.PostResponse;
import com.connecthub.modules.features.post.entity.Post;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PostMapper {

    PostResponse toPostResponse(Post post);
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "parentPost", ignore = true)
    @Mapping(target = "quotePost", ignore = true)
    @Mapping(target = "media", ignore = true)
    @Mapping(target = "reactions", ignore = true)
    @Mapping(target = "bookmarks", ignore = true)
    @Mapping(target = "reposts", ignore = true)
    @Mapping(target = "postViews", ignore = true)
    @Mapping(target = "feedItems", ignore = true)
    @Mapping(target = "mentions", ignore = true)
    @Mapping(target = "postHashtags", ignore = true)
    @Mapping(target = "notifications", ignore = true)
    @Mapping(target = "reports", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)

    Post toPost(PostRequest request);
}
