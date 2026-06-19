package com.connecthub.modules.features.post.service;

import com.connecthub.common.dto.response.CursorResponse;
import com.connecthub.common.util.AppUtil;
import com.connecthub.modules.features.post.dto.request.PostRequest;
import com.connecthub.modules.features.post.dto.response.PostResponse;
import com.connecthub.modules.features.post.entity.Hashtag;
import com.connecthub.modules.features.post.entity.Mention;
import com.connecthub.modules.features.post.entity.Post;
import com.connecthub.modules.features.post.entity.PostHashtag;
import com.connecthub.modules.features.post.exception.HashtagNotFoundException;
import com.connecthub.modules.features.post.exception.MentionedUserNotFoundException;
import com.connecthub.modules.features.post.exception.PostAccessDeniedException;
import com.connecthub.modules.features.post.exception.PostNotFoundException;
import com.connecthub.modules.features.post.mapper.PostMapper;
import com.connecthub.modules.features.post.repository.HashtagRepository;
import com.connecthub.modules.features.post.repository.MediaRepository;
import com.connecthub.modules.features.post.repository.MentionRepository;
import com.connecthub.modules.features.post.repository.BookmarkRepository;
import com.connecthub.modules.features.post.repository.PostHashtagRepository;
import com.connecthub.modules.features.post.repository.PostRepository;
import com.connecthub.modules.features.post.repository.PostViewRepository;
import com.connecthub.modules.features.post.repository.ReactionRepository;
import com.connecthub.modules.features.post.repository.RepostRepository;
import com.connecthub.modules.features.user.entity.User;
import com.connecthub.modules.features.user.exception.UserNotFoundException;
import com.connecthub.modules.features.user.repository.UserRepository;
import com.github.f4b6a3.uuid.UuidCreator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final HashtagRepository hashtagRepository;
    private final PostHashtagRepository postHashtagRepository;
    private final MentionRepository mentionRepository;
    private final MediaRepository mediaRepository;
    private final ReactionRepository reactionRepository;
    private final BookmarkRepository bookmarkRepository;
    private final RepostRepository repostRepository;
    private final PostViewRepository postViewRepository;
    private final PostMapper postMapper;

    @Transactional
    @PreAuthorize("hasRole('ROLE_USER')")
    public PostResponse createPost(PostRequest request) {
        UUID userId = AppUtil.userIdFormAuthentication();
        User user = getUserOrThrow(userId);

        Post post = postMapper.toPost(request);
        post.setId(AppUtil.generateUUID());
        post.setUser(user);
        post.setDeleted(false);

        if (request.getParentPostId() != null) {
            Post parentPost = getPostOrThrow(request.getParentPostId());
            post.setParentPost(parentPost);
        }
        if (request.getQuotePostId() != null) {
            Post quotePost = getPostOrThrow(request.getQuotePostId());
            post.setQuotePost(quotePost);
        }

        Post savedPost = postRepository.save(post);
        if (request.getHashtags() != null && !request.getHashtags().isEmpty()) {
            addHashtagsToPost(savedPost, request.getHashtags());
        }

        if (request.getMentionUserIds() != null && !request.getMentionUserIds().isEmpty()) {
            addMentionsToPost(savedPost, request.getMentionUserIds());
        }

        log.info("Post created: {} by user: {}", savedPost.getId(), userId);
        return mapToResponse(savedPost);
    }

    @Transactional(readOnly = true)
    public PostResponse getPost(UUID postId) {
        Post post = getPostOrThrow(postId);

        if (post.isDeleted()) {
            throw new PostNotFoundException("Post has been deleted");
        }

        return mapToResponse(post);
    }

    @Transactional
    @PreAuthorize("hasRole('ROLE_USER')")
    public PostResponse updatePost(UUID postId, PostRequest request) {
        java.util.UUID userId = AppUtil.userIdFormAuthentication();
        Post post = getOwnedPostOrThrow(postId, userId, "update");

        if (post.isDeleted()) {
            throw new PostNotFoundException("Cannot update a deleted post");
        }

        post.setContent(request.getContent());
        post.setVisibility(request.getVisibility());

        Post updatedPost = postRepository.save(post);
        log.info("Post updated: {} by user: {}", postId, userId);
        return mapToResponse(updatedPost);
    }

    @Transactional
    @PreAuthorize("hasRole('ROLE_USER')")
    public void deletePost(UUID postId) {
        java.util.UUID userId = AppUtil.userIdFormAuthentication();
        Post post = getOwnedPostOrThrow(postId, userId, "delete");

        post.setDeleted(true);
        postRepository.save(post);
        log.info("Post deleted: {} by user: {}", postId, userId);
    }


    @Transactional(readOnly = true)
    public CursorResponse<PostResponse> getUserFeed(UUID cursor, int size) {
        List<Post> feed = new ArrayList<>(
                postRepository.findPublicFeed(cursor, Limit.of(size + 1))
        );

        return AppUtil.buildCursorResponse(
                feed,
                size,
                Post::getId,
                this::mapToResponse
        );
    }

    @Transactional(readOnly = true)
    public CursorResponse<PostResponse> getPostsByHashtag(String hashtag, UUID cursor, int size) {
        Hashtag hashtagEntity = hashtagRepository.findByName(hashtag)
                .orElseThrow(() -> new HashtagNotFoundException(hashtag));

        List<PostHashtag> postHashtags = new ArrayList<>(
                postHashtagRepository.findPostsByHashtagId(
                        hashtagEntity.getId(),
                        cursor,
                        Limit.of(size + 1)
                )
        );

        return AppUtil.buildCursorResponse(
                postHashtags,
                size,
                ph -> ph.getPost().getId(),
                ph -> mapToResponse(ph.getPost())
        );
    }

    @Transactional
    @PreAuthorize("hasRole('ROLE_USER')")
    public void addHashtagToPost(UUID postId, String hashtagName) {
        Post post = getPostOrThrow(postId);
        attachHashtag(post, hashtagName);
    }

    @Transactional
    @PreAuthorize("hasRole('ROLE_USER')")
    public PostResponse createReply(UUID parentPostId, PostRequest request) {
        getPostOrThrow(parentPostId);

        request.setParentPostId(parentPostId);
        return createPost(request);
    }
    @Transactional(readOnly = true)
    public CursorResponse<PostResponse> getReplies(UUID postId, UUID cursor, int size) {
        getPostOrThrow(postId);

        List<Post> replies = new ArrayList<>(
                postRepository.findRepliesByParentPostId(postId, cursor, Limit.of(size + 1))
        );

        return AppUtil.buildCursorResponse(
                replies,
                size,
                Post::getId,
                this::mapToResponse
        );
    }

    private PostResponse mapToResponse(Post post) {
        UUID postId = post.getId();
        long commentCount = postRepository.countByParentPostIdAndIsDeletedFalse(postId);
        long reactionCount = reactionRepository.countByPostId(postId);
        long repostCount = repostRepository.countByPostId(postId);
        long bookmarkCount = bookmarkRepository.countByPostId(postId);
        long viewCount = postViewRepository.countByPostId(postId);
        var media = mediaRepository.findByPostId(postId);

        return postMapper.mapToResponse(post, media, commentCount, reactionCount, repostCount, bookmarkCount, viewCount);
    }

    private void addHashtagsToPost(Post post, List<String> hashtags) {
        for (String hashtagName : hashtags) {
            attachHashtag(post, hashtagName);
        }
    }

    private void attachHashtag(Post post, String hashtagName) {
        Hashtag hashtag = hashtagRepository.findByName(hashtagName)
                .orElseGet(() -> {
                    Hashtag newHashtag = Hashtag.builder()
                            .id(UuidCreator.getTimeOrderedEpoch())
                            .name(hashtagName)
                            .build();
                    return hashtagRepository.save(newHashtag);
                });

        if (postHashtagRepository.existsByPostIdAndHashtagId(post.getId(), hashtag.getId())) {
            return;
        }

        PostHashtag postHashtag = PostHashtag.builder()
                .post(post)
                .hashtag(hashtag)
                .build();

        postHashtagRepository.save(postHashtag);
        log.info("Hashtag {} added to post {}", hashtagName, post.getId());
    }

    private void addMentionsToPost(Post post, List<UUID> mentionUserIds) {
        for (UUID userId : mentionUserIds) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new MentionedUserNotFoundException(userId));

            Mention mention = Mention.builder()
                    .id(UuidCreator.getTimeOrderedEpoch())
                    .post(post)
                    .user(user)
                    .build();
            mentionRepository.save(mention);
        }
    }

    private User getUserOrThrow(UUID uuid) {
        return userRepository.findById(uuid)
                .orElseThrow(UserNotFoundException::new);
    }

    private Post getPostOrThrow(UUID postId) {
        return postRepository.findById(postId)
                .orElseThrow(PostNotFoundException::new);
    }

    private Post getOwnedPostOrThrow(UUID postId, UUID uuid, String action) {
        User user = getUserOrThrow(uuid);
        Post post = getPostOrThrow(postId);

        if (!post.getUser().getId().equals(user.getId())) {
            throw new PostAccessDeniedException(action);
        }

        return post;
    }
}