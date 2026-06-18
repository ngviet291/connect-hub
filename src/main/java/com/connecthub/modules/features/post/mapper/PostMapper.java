package com.connecthub.modules.features.post.mapper;

import com.connecthub.modules.features.post.dto.request.PostRequest;
import com.connecthub.modules.features.post.dto.response.MediaResponse;
import com.connecthub.modules.features.post.dto.response.PostResponse;
import com.connecthub.modules.features.post.entity.Media;
import com.connecthub.modules.features.post.entity.Post;
import com.connecthub.modules.features.user.dto.response.UserSummaryResponse;
import com.connecthub.modules.features.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.stream.Collectors;

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
    @Mapping(target = "isDeleted", ignore = true)
    Post toPost(PostRequest request);

    UserSummaryResponse toUserSummaryResponse(User user);

    MediaResponse toMediaResponse(Media media);

    default PostResponse mapToResponse(
            Post post,
            List<Media> media,
            long commentCount,
            long reactionCount,
            long repostCount,
            long bookmarkCount,
            long viewCount
    ) {
        return PostResponse.builder()
                .id(post.getId())
                .author(toUserSummaryResponse(post.getUser()))
                .content(post.getContent())
                .visibility(post.getVisibility())
                .parentPostId(post.getParentPost() != null ? post.getParentPost().getId() : null)
                .quotePostId(post.getQuotePost() != null ? post.getQuotePost().getId() : null)
                .media(media != null ?
                        media.stream().map(this::toMediaResponse).collect(Collectors.toList()) :
                        null)
                .reactionCount((int) reactionCount)
                .commentCount((int) commentCount)
                .repostCount((int) repostCount)
                .bookmarkCount((int) bookmarkCount)
                .viewCount((int) viewCount)
                .reacted(false) // TODO: Check if current user reacted
                .bookmarked(false) // TODO: Check if current user bookmarked
                .reposted(false) // TODO: Check if current user reposted
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}