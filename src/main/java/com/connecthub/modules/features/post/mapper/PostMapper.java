package com.connecthub.modules.features.post.mapper;

import com.connecthub.modules.features.post.dto.request.PostRequest;
import com.connecthub.modules.features.post.dto.response.MediaResponse;
import com.connecthub.modules.features.post.dto.response.PostResponse;
import com.connecthub.modules.features.post.dto.response.QuotePostResponse;
import com.connecthub.modules.features.post.entity.Media;
import com.connecthub.modules.features.post.entity.Post;
import com.connecthub.modules.features.user.dto.response.UserSummaryResponse;
import com.connecthub.modules.features.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface PostMapper {

    @Mapping(target = "id",            ignore = true)
    @Mapping(target = "user",          ignore = true)
    @Mapping(target = "parentPost",    ignore = true)
    @Mapping(target = "quotePost",     ignore = true)
    @Mapping(target = "media",         ignore = true)
    @Mapping(target = "reactions",     ignore = true)
    @Mapping(target = "bookmarks",     ignore = true)
    @Mapping(target = "reposts",       ignore = true)
    @Mapping(target = "postViews",     ignore = true)
    @Mapping(target = "feedItems",     ignore = true)
    @Mapping(target = "mentions",      ignore = true)
    @Mapping(target = "postHashtags",  ignore = true)
    @Mapping(target = "notifications", ignore = true)
    @Mapping(target = "reports",       ignore = true)
    @Mapping(target = "deletedAt",     ignore = true)
    @Mapping(target = "isDeleted",     ignore = true)
    @Mapping(target = "reactionCount", ignore = true)
    @Mapping(target = "commentCount",  ignore = true)
    @Mapping(target = "repostCount",   ignore = true)
    @Mapping(target = "bookmarkCount", ignore = true)
    @Mapping(target = "viewCount",     ignore = true)
    Post toPost(PostRequest request);
    UserSummaryResponse toUserSummaryResponse(User user);
    MediaResponse toMediaResponse(Media media);

    @Mapping(target = "author", source = "user")
    @Mapping(target = "media",  expression = "java(mapMedia(post.getMedia()))")
    QuotePostResponse toQuotePostResponse(Post post);

    default PostResponse mapToResponse(Post post) {
        return PostResponse.builder()
                .id(post.getId())
                .author(toUserSummaryResponse(post.getUser()))
                .content(post.getContent())
                .visibility(post.getVisibility())
                .parentPostId(post.getParentPost() != null
                        ? post.getParentPost().getId() : null)
                .quotePost(post.getQuotePost() != null
                        ? toQuotePostResponse(post.getQuotePost()) : null)
                .media(mapMedia(post.getMedia()))
                .reactionCount((int) post.getReactionCount())
                .commentCount((int) post.getCommentCount())
                .repostCount((int) post.getRepostCount())
                .bookmarkCount((int) post.getBookmarkCount())
                .viewCount((int) post.getViewCount())
                .reacted(false)
                .bookmarked(false)
                .reposted(false)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    default List<MediaResponse> mapMedia(Set<Media> media) {
        if (media == null) return List.of();
        return media.stream().map(this::toMediaResponse).toList();
    }
}