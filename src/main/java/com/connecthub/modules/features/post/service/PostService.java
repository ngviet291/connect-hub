package com.connecthub.modules.features.post.service;

import com.connecthub.common.util.AppUtil;
import com.connecthub.modules.features.post.dto.request.PostRequest;
import com.connecthub.modules.features.post.dto.response.PostResponse;
import com.connecthub.modules.features.post.entity.Hashtag;
import com.connecthub.modules.features.post.entity.Mention;
import com.connecthub.modules.features.post.entity.Post;
import com.connecthub.modules.features.post.entity.PostHashtag;
import com.connecthub.modules.features.post.mapper.PostMapper;
import com.connecthub.modules.features.post.repository.HashtagRepository;
import com.connecthub.modules.features.post.repository.MentionRepository;
import com.connecthub.modules.features.post.repository.PostHashtagRepository;
import com.connecthub.modules.features.post.repository.PostRepository;
import com.connecthub.modules.features.user.entity.User;
import com.connecthub.modules.features.user.exception.UserNotFoundException;
import com.connecthub.modules.features.user.repository.UserRepository;
import com.github.f4b6a3.uuid.UuidCreator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.UnknownServiceException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final HashtagRepository hashtagRepository;
    private final PostHashtagRepository postHashtagRepository;
    private final MentionRepository mentionRepository;
    private final PostMapper postMapper;

    @Transactional
    public PostResponse createPost(PostRequest request) {
        String username = AppUtil.usernameFromAuthentication();
        User user = getUserOrThrow(username);

        Post post = Post.builder()
                .id(UuidCreator.getTimeOrderedEpoch())
                .user(user)
                .content(request.getContent())
                .visibility(request.getVisibility())
                .isDeleted(false)
                .build();

        if (request.getParentPostId() != null) {
            Post parentPost = getPostOrThrow(request.getParentPostId(), "Parent post not found");
            post.setParentPost(parentPost);
        }
        if (request.getQuotePostId() != null) {
            Post quotePost = getPostOrThrow(request.getQuotePostId(), "Quote post not found");
            post.setQuotePost(quotePost);
        }

        Post savedPost = postRepository.save(post);
        if (request.getHashtags() != null && !request.getHashtags().isEmpty()) {
            addHashtagsToPost(savedPost, request.getHashtags());
        }

        if (request.getMentionUserIds() != null && !request.getMentionUserIds().isEmpty()) {
            addMentionsToPost(savedPost, request.getMentionUserIds());
        }

        log.info("Post created: {} by user: {}", savedPost.getId(), username);
        return mapToResponse(savedPost);
    }

    public PostResponse getPost(UUID postId) {
        Post post = getPostOrThrow(postId, "Post not found");

        if (post.isDeleted()) {
            throw new RuntimeException("Post has been deleted");
        }

        return mapToResponse(post);
    }

    @Transactional
    public PostResponse updatePost(UUID postId, PostRequest request) {
        String username = AppUtil.usernameFromAuthentication();
        Post post = getOwnedPostOrThrow(postId, username, "update");

        if (post.isDeleted()) {
            throw new RuntimeException("Cannot update a deleted post");
        }

        post.setContent(request.getContent());
        post.setVisibility(request.getVisibility());

        Post updatedPost = postRepository.save(post);
        log.info("Post updated: {} by user: {}", postId, username);
        return mapToResponse(updatedPost);
    }

    @Transactional
    public void deletePost(UUID postId) {
        String username = AppUtil.usernameFromAuthentication();
        Post post = getOwnedPostOrThrow(postId, username, "delete");

        post.setDeleted(true);
        postRepository.save(post);
        log.info("Post deleted: {} by user: {}", postId, username);
    }

    public List<PostResponse> getUserFeed(UUID cursor, int limit) {
        List<Post> feed = postRepository.findPublicFeed(cursor, Limit.of(limit));
        return feed.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public List<PostResponse> getPostsByHashtag(String hashtag, UUID cursor, int limit) {
        Hashtag hashtagEntity = hashtagRepository.findByName(hashtag).orElse(null);
        if (hashtagEntity == null) {
            throw new RuntimeException("Hashtag not found");
        }
        List<PostHashtag> postHashtags = postHashtagRepository.findPostsByHashtagId(hashtagEntity.getId(), cursor, Limit.of(limit));
        return postHashtags.stream().map(ph -> mapToResponse(ph.getPost())).collect(Collectors.toList());
    }
    @Transactional
    public void addHashtagToPost(UUID postId, String hashtagName) {
        Post post = getPostOrThrow(postId, "Post not found");

        Hashtag hashtag = hashtagRepository.findByName(hashtagName)
                .orElseGet(() -> {
                    Hashtag newHashtag = Hashtag.builder()
                            .id(UuidCreator.getTimeOrderedEpoch())
                            .name(hashtagName)
                            .build();
                    return hashtagRepository.save(newHashtag);
                });

        boolean exists = post.getPostHashtags().stream()
                .anyMatch(ph -> ph.getHashtag().getId().equals(hashtag.getId()));

        if (!exists) {
            PostHashtag postHashtag = PostHashtag.builder()
                    .post(post)
                    .hashtag(hashtag)
                    .build();
            postHashtagRepository.save(postHashtag);
            log.info("Hashtag {} added to post {}", hashtagName, postId);
        }
    }

    @Transactional
    public PostResponse createReply(UUID parentPostId, PostRequest request) {
        getPostOrThrow(parentPostId, "Parent post not found");

        request.setParentPostId(parentPostId);
        return createPost(request);
    }


    @Transactional(readOnly = true)
    public List<PostResponse> getReplies(UUID postId, UUID cursor, int limit) {
        getPostOrThrow(postId, "Post not found");

        List<Post> replies = postRepository.findRepliesByParentPostId(postId, cursor, Limit.of(limit));
        return replies.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    private PostResponse mapToResponse(Post post) {
        long commentCount = postRepository.countByParentPostIdAndIsDeletedFalse(post.getId());

        return PostResponse.builder()
                .id(post.getId())
                .author(postMapper.toUserSummaryResponse(post.getUser()))
                .content(post.getContent())
                .visibility(post.getVisibility())
                .parentPostId(post.getParentPost() != null ? post.getParentPost().getId() : null)
                .quotePostId(post.getQuotePost() != null ? post.getQuotePost().getId() : null)
                .media(post.getMedia() != null ?
                        post.getMedia().stream().map(postMapper::toMediaResponse).collect(Collectors.toList()) :
                        null)
                .reactionCount(post.getReactions() != null ? post.getReactions().size() : 0)
                .commentCount((int) commentCount)
                .repostCount(post.getReposts() != null ? post.getReposts().size() : 0)
                .bookmarkCount(post.getBookmarks() != null ? post.getBookmarks().size() : 0)
                .viewCount(post.getPostViews() != null ? post.getPostViews().size() : 0)
                .reacted(false) // TODO: Check if current user reacted
                .bookmarked(false) // TODO: Check if current user bookmarked
                .reposted(false) // TODO: Check if current user reposted
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    private void addHashtagsToPost(Post post, List<String> hashtags) {
        for (String hashtagName : hashtags) {
            addHashtagToPost(post.getId(), hashtagName);
        }
    }

    private void addMentionsToPost(Post post, List<UUID> mentionUserIds) {
        for (UUID userId : mentionUserIds) {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                throw new RuntimeException("Mentioned user not found: " + userId);
            }

            Mention mention = Mention.builder()
                    .id(UuidCreator.getTimeOrderedEpoch())
                    .post(post)
                    .user(user)
                    .build();
            mentionRepository.save(mention);
        }
    }

    private User getUserOrThrow(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(UserNotFoundException::new);
    }

    private Post getPostOrThrow(UUID postId, String errorMessage) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException(errorMessage));
    }

    private Post getOwnedPostOrThrow(UUID postId, String username, String action) {
        User user = getUserOrThrow(username);
        Post post = getPostOrThrow(postId, "Post not found");

        if (!post.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You don't have permission to " + action + " this post");
        }

        return post;
    }
}