package com.connecthub.modules.features.post.service;

import com.connecthub.common.dto.response.CursorResponse;
import com.connecthub.common.util.AppUtil;
import com.connecthub.modules.features.post.dto.request.PostRequest;
import com.connecthub.modules.features.post.dto.response.PostResponse;
import com.connecthub.modules.features.post.entity.*;
import com.connecthub.modules.features.post.exception.HashtagNotFoundException;
import com.connecthub.modules.features.post.exception.MentionedUserNotFoundException;
import com.connecthub.modules.features.post.exception.PostAccessDeniedException;
import com.connecthub.modules.features.post.exception.PostNotFoundException;
import com.connecthub.modules.features.post.mapper.PostMapper;
import com.connecthub.modules.features.post.repository.*;
import com.connecthub.modules.features.user.entity.User;
import com.connecthub.modules.features.user.exception.UserNotFoundException;
import com.connecthub.modules.features.user.repository.UserRepository;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
    private final PostMapper postMapper;
    private final EntityManager entityManager;

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

        if (request.getMediaIds() != null && !request.getMediaIds().isEmpty()) {
            addMediaToPost(savedPost, request.getMediaIds());
        }
        if (request.getHashtags() != null && !request.getHashtags().isEmpty()) {
            addHashtagsToPost(savedPost, request.getHashtags());
        }
        if (request.getMentionUserIds() != null && !request.getMentionUserIds().isEmpty()) {
            addMentionsToPost(savedPost, request.getMentionUserIds());
        }

        log.info("Post created: {} by user: {}", savedPost.getId(), userId);

        entityManager.flush();
        entityManager.clear();

        Post fullPost = postRepository.findByIdWithDetails(savedPost.getId())
                .orElseThrow(PostNotFoundException::new);
        return postMapper.mapToResponse(fullPost);
    }


    @Transactional(readOnly = true)
    public PostResponse getPost(UUID postId) {
        Post post = postRepository.findByIdWithDetails(postId)
                .orElseThrow(PostNotFoundException::new);

        if (post.isDeleted()) {
            throw new PostNotFoundException("Post has been deleted");
        }
        return postMapper.mapToResponse(post);
    }

    @Transactional
    @PreAuthorize("hasRole('ROLE_USER')")
    public PostResponse updatePost(UUID postId, PostRequest request) {
        UUID userId = AppUtil.userIdFormAuthentication();
        Post post = postRepository.findByIdWithDetails(postId)
                .orElseThrow(PostNotFoundException::new);

        if (!post.getUser().getId().equals(userId)) {
            throw new PostAccessDeniedException("update");
        }
        if (post.isDeleted()) {
            throw new PostNotFoundException("Cannot update a deleted post");
        }

        post.setContent(request.getContent());
        post.setVisibility(request.getVisibility());

        postRepository.save(post);
        log.info("Post updated: {} by user: {}", postId, userId);
        entityManager.flush();
        entityManager.clear();
        Post fullPost = postRepository.findByIdWithDetails(postId)
                .orElseThrow(PostNotFoundException::new);
        return postMapper.mapToResponse(fullPost);
    }

    @Transactional
    @PreAuthorize("hasRole('ROLE_USER')")
    public void deletePost(UUID postId) {
        UUID userId = AppUtil.userIdFormAuthentication();
        Post post = getOwnedPostOrThrow(postId, userId, "delete");

        post.setDeleted(true);
        postRepository.save(post);

        if (post.getParentPost() != null) {
            postRepository.decrementCommentCount(post.getParentPost().getId());
        }
        log.info("Post deleted: {} by user: {}", postId, userId);
    }

    @Transactional(readOnly = true)
    public CursorResponse<PostResponse> getUserFeed(UUID cursor, int size) {
        List<Post> feed = new ArrayList<>(
                postRepository.findPublicFeedWithDetails(cursor, Limit.of(size + 1))
        );
        return AppUtil.buildCursorResponse(feed, size, Post::getId, postMapper::mapToResponse);
    }


    @Transactional(readOnly = true)
    public CursorResponse<PostResponse> getPostsByHashtag(String hashtag, UUID cursor, int size) {
        Hashtag hashtagEntity = hashtagRepository.findByName(hashtag)
                .orElseThrow(() -> new HashtagNotFoundException(hashtag));

        List<PostHashtag> postHashtags = new ArrayList<>(
                postHashtagRepository.findPostsByHashtagId(hashtagEntity.getId(), cursor, Limit.of(size + 1))
        );

        return AppUtil.buildCursorResponse(
                postHashtags,
                size,
                ph -> ph.getPost().getId(),
                ph -> postMapper.mapToResponse(ph.getPost())
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
        PostResponse reply = createPost(request);
        postRepository.incrementCommentCount(parentPostId);
        return reply;
    }

    @Transactional(readOnly = true)
    public CursorResponse<PostResponse> getReplies(UUID postId, UUID cursor, int size) {
        getPostOrThrow(postId);
        List<Post> replies = new ArrayList<>(
                postRepository.findRepliesByParentPostIdWithDetails(postId, cursor, Limit.of(size + 1))
        );
        return AppUtil.buildCursorResponse(replies, size, Post::getId, postMapper::mapToResponse);
    }

    private void addHashtagsToPost(Post post, List<String> hashtags) {
        hashtags.forEach(name -> attachHashtag(post, name));
    }

    private void attachHashtag(Post post, String hashtagName) {
        Hashtag hashtag = hashtagRepository.findByName(hashtagName)
                .orElseGet(() -> hashtagRepository.save(
                        Hashtag.builder()
                                .id(UuidCreator.getTimeOrderedEpoch())
                                .name(hashtagName)
                                .build()
                ));

        if (postHashtagRepository.existsByPostIdAndHashtagId(post.getId(), hashtag.getId())) {
            return;
        }

        postHashtagRepository.save(PostHashtag.builder()
                .post(post)
                .hashtag(hashtag)
                .build());

        log.info("Hashtag {} added to post {}", hashtagName, post.getId());
    }

    private void addMentionsToPost(Post post, List<UUID> mentionUserIds) {
        for (UUID userId : mentionUserIds) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new MentionedUserNotFoundException(userId));

            mentionRepository.save(Mention.builder()
                    .id(UuidCreator.getTimeOrderedEpoch())
                    .post(post)
                    .user(user)
                    .build());
        }
    }
    private void addMediaToPost(Post post, List<UUID> mediaIds) {
        // Chỉ lấy media chưa thuộc post nào tránh claim media của người khác
//        List<Media> mediaList = mediaRepository.findAllByIdInAndPostIsNull(mediaIds);
        List<Media> mediaList = mediaRepository.findAllById(mediaIds);
        if (mediaList.size() != mediaIds.size()) {
            log.warn("Some media not found or already attached. Expected: {}, Found: {}",
                    mediaIds.size(), mediaList.size());
        }

        mediaList.forEach(media -> media.setPost(post));
        mediaRepository.saveAll(mediaList);
        log.info("Linked {} media to post: {}", mediaList.size(), post.getId());
    }
    private User getUserOrThrow(UUID uuid) {
        return userRepository.findById(uuid).orElseThrow(UserNotFoundException::new);
    }

    private Post getPostOrThrow(UUID postId) {
        return postRepository.findById(postId)
                .orElseThrow(PostNotFoundException::new);
    }

    private Post getOwnedPostOrThrow(UUID postId, UUID userId, String action) {
        User user = getUserOrThrow(userId);
        Post post = getPostOrThrow(postId);
        if (!post.getUser().getId().equals(user.getId())) {
            throw new PostAccessDeniedException(action);
        }
        return post;
    }
}