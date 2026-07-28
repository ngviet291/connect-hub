package com.connecthub.modules.features.post.mapper;

import com.connecthub.common.util.AppUtil;
import com.connecthub.modules.features.post.dto.request.PostRequest;
import com.connecthub.modules.features.post.dto.response.MediaResponse;
import com.connecthub.modules.features.post.dto.response.PostResponse;
import com.connecthub.modules.features.post.dto.response.QuotePostResponse;
import com.connecthub.modules.features.post.entity.Media;
import com.connecthub.modules.features.post.entity.Mention;
import com.connecthub.modules.features.post.entity.Post;
import com.connecthub.modules.features.post.entity.PostHashtag;
import com.connecthub.modules.features.post.enums.ReactionType;
import com.connecthub.modules.features.user.dto.response.UserSummaryResponse;
import com.connecthub.modules.features.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.HashSet;
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
    default void initNewPost(Post post, User user) {
        post.setId(AppUtil.generateUUID());
        post.setUser(user);
        post.setDeleted(false);
        post.setMedia(new HashSet<>());
        post.setPostHashtags(new HashSet<>());
        post.setMentions(new HashSet<>());
    }
    UserSummaryResponse toUserSummaryResponse(User user);

    // Map id -> mediaId, size -> fileSize
    @Mapping(target = "mediaId",  source = "id")
    @Mapping(target = "fileSize", expression = "java(media.getSize() != null ? media.getSize().longValue() : null)")
    @Mapping(target = "fileName", ignore = true)
    @Mapping(target = "mimeType", ignore = true)
    MediaResponse toMediaResponse(Media media);

    @Mapping(target = "author", source = "user")
    @Mapping(target = "media",  expression = "java(mapMedia(post.getMedia()))")
    QuotePostResponse toQuotePostResponse(Post post);

    /**
     * Map post kèm trạng thái reacted/reposted/bookmarked của user hiện tại.
     * BẮT BUỘC phải truyền từ Service (không tự query trong mapper) — để:
     *  - Service có thể batch-query 1 lần cho cả trang feed (tránh N+1)
     *  - Mapper vẫn thuần/stateless, dễ test
     * Dùng hàm này ở MỌI nơi trả PostResponse — không dùng bản 3-arg=false phía dưới
     * trừ trường hợp chắc chắn là post vừa tạo (createPost).
     */
    default PostResponse mapToResponse(Post post, ReactionType myReactionType, boolean reposted, boolean bookmarked) {
        return PostResponse.builder()
                .id(post.getId())
                .author(toUserSummaryResponse(post.getUser()))
                .content(post.getContent())
                .visibility(post.getVisibility())
                .parentPostId(post.getParentPost() != null ? post.getParentPost().getId() : null)
                .quotePost(post.getQuotePost() != null ? toQuotePostResponse(post.getQuotePost()) : null)
                .media(mapMedia(post.getMedia()))
                .hashtags(mapHashtags(post.getPostHashtags()))
                .mentions(mapMentions(post.getMentions()))
                .reactionCount((int) post.getReactionCount())
                .commentCount((int) post.getCommentCount())
                .repostCount((int) post.getRepostCount())
                .bookmarkCount((int) post.getBookmarkCount())
                .viewCount((int) post.getViewCount())
                .myReactionType(myReactionType)
                .reacted(myReactionType != null)
                .bookmarked(bookmarked)
                .reposted(reposted)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    default PostResponse mapToResponseForNewPost(Post post) {
        return mapToResponse(post, null, false, false);
    }
    default List<MediaResponse> mapMedia(Set<Media> media) {
        if (media == null) return List.of();
        return media.stream().map(this::toMediaResponse).toList();
    }

    default List<String> mapHashtags(Set<PostHashtag> postHashtags) {
        if (postHashtags == null) return List.of();
        return postHashtags.stream()
                .map(ph -> ph.getHashtag().getName())
                .toList();
    }

    default List<UserSummaryResponse> mapMentions(Set<Mention> mentions) {
        if (mentions == null) return List.of();
        return mentions.stream()
                .map(m -> toUserSummaryResponse(m.getUser()))
                .toList();
    }
}