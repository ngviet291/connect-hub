package com.connecthub.modules.features.post.service;

import com.connecthub.common.dto.response.CursorResponse;
import com.connecthub.common.util.AppUtil;
import com.connecthub.modules.features.post.dto.request.PostRequest;
import com.connecthub.modules.features.post.dto.request.UpdatePostRequest;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final HashtagRepository hashtagRepository;
    private final PostHashtagRepository postHashtagRepository;
    private final MentionRepository mentionRepository;
    private final MediaService mediaService;
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
        post.setMedia(new HashSet<>());
        post.setPostHashtags(new HashSet<>());
        post.setMentions(new HashSet<>());

        if (request.getParentPostId() != null) {
            post.setParentPost(getActivePostOrThrow(request.getParentPostId()));
        }
        if (request.getQuotePostId() != null) {
            post.setQuotePost(getActivePostOrThrow(request.getQuotePostId()));
        }

        Post savedPost = postRepository.save(post);

        if (request.getFiles() != null && !request.getFiles().isEmpty()) {
            List<Media> uploaded = mediaService.uploadAndAttachToPost(request.getFiles(), savedPost);
            savedPost.getMedia().addAll(uploaded);
        }
        if (request.getHashtags() != null && !request.getHashtags().isEmpty()) {
            List<PostHashtag> postHashtags = addHashtagsToPost(savedPost, request.getHashtags());
            savedPost.getPostHashtags().addAll(postHashtags);
        }
        if (request.getMentionUsernames() != null && !request.getMentionUsernames().isEmpty()) {
            List<Mention> mentions = addMentionsByUsername(savedPost, request.getMentionUsernames());
            savedPost.getMentions().addAll(mentions);
        }

        log.info("Post created: {} by user: {}", savedPost.getId(), userId);
        return postMapper.mapToResponse(savedPost);
    }

    @Transactional(readOnly = true)
    public PostResponse getPost(UUID postId) {
        Post post = postRepository.findByIdWithDetails(postId)
                .orElseThrow(PostNotFoundException::new);
        if (post.isDeleted()) throw new PostNotFoundException();
        return postMapper.mapToResponse(post);
    }

    @Transactional
    @PreAuthorize("hasRole('ROLE_USER')")
    public PostResponse updatePost(UUID postId, UpdatePostRequest request) {
        UUID userId = AppUtil.userIdFormAuthentication();

        Post post = postRepository.findByIdWithDetails(postId)
                .orElseThrow(PostNotFoundException::new);

        if (post.isDeleted()) throw new PostNotFoundException();
        if (!post.getUser().getId().equals(userId)) throw new PostAccessDeniedException();
        // Update content & visibility
        if (request.getContent() != null) {
            post.setContent(request.getContent());
        }
        if (request.getVisibility() != null) {
            post.setVisibility(request.getVisibility());
        }
        // Update hashtags: xóa cũ, thêm mới
        if (request.getHashtags() != null) {
            postHashtagRepository.deleteByPostId(post.getId());
            post.getPostHashtags().clear();
            if (!request.getHashtags().isEmpty()) {
                List<PostHashtag> newHashtags = addHashtagsToPost(post, request.getHashtags());
                post.getPostHashtags().addAll(newHashtags);
            }
        }
        // Update mentions: xóa cũ, thêm mới
        if (request.getMentionUsernames() != null) {
            mentionRepository.deleteByPostId(post.getId());
            post.getMentions().clear();
            if (!request.getMentionUsernames().isEmpty()) {
                List<Mention> newMentions = addMentionsByUsername(post, request.getMentionUsernames());
                post.getMentions().addAll(newMentions);
            }
        }

        postRepository.save(post);
        log.info("Post updated: {} by user: {}", postId, userId);

        return postMapper.mapToResponse(
                postRepository.findByIdWithDetails(postId).orElseThrow(PostNotFoundException::new)
        );
    }

    @Transactional
    @PreAuthorize("hasRole('ROLE_USER')")
    public void deletePost(UUID postId) {
        UUID userId = AppUtil.userIdFormAuthentication();
        Post post = getPostOrThrow(postId);

        if (post.isDeleted()) throw new PostNotFoundException();
        if (!post.getUser().getId().equals(userId)) throw new PostAccessDeniedException();

        post.setDeleted(true);
        postRepository.save(post);

        if (post.getParentPost() != null) {
            postRepository.decrementCommentCount(post.getParentPost().getId());
        }
        log.info("Post deleted: {} by user: {}", postId, userId);
    }

    @Transactional(readOnly = true)
    public CursorResponse<PostResponse> getUserFeed(UUID cursor, int size) {
        List<UUID> ids = postRepository.findPublicFeedIds(cursor, Limit.of(size + 1));
        boolean hasNext = ids.size() > size;
        List<UUID> pageIds = hasNext ? ids.subList(0, size) : ids;

        if (pageIds.isEmpty()) return emptyCursor();

        List<Post> posts = new ArrayList<>(postRepository.findAllWithDetailsByIds(pageIds));
        posts.sort((a, b) -> b.getId().compareTo(a.getId()));

        return CursorResponse.<PostResponse>builder()
                .content(posts.stream().map(postMapper::mapToResponse).toList())
                .hasNext(hasNext)
                .nextCursor(hasNext ? pageIds.getLast().toString() : null)
                .build();
    }

    @Transactional(readOnly = true)
    public CursorResponse<PostResponse> getPostsByHashtag(String hashtag, UUID cursor, int size) {
        String normalized = hashtag.toLowerCase();
        Hashtag hashtagEntity = hashtagRepository.findByName(normalized)
                .orElseThrow(() -> new HashtagNotFoundException(normalized));

        List<PostHashtag> postHashtags = new ArrayList<>(
                postHashtagRepository.findPostsByHashtagId(hashtagEntity.getId(), cursor, Limit.of(size + 1))
        );

        return AppUtil.buildCursorResponse(
                postHashtags, size,
                ph -> ph.getPost().getId(),
                ph -> postMapper.mapToResponse(ph.getPost())
        );
    }

    @Transactional
    @PreAuthorize("hasRole('ROLE_USER')")
    public void addHashtagToPost(UUID postId, String hashtagName) {
        attachHashtag(getActivePostOrThrow(postId), hashtagName);
    }

    @Transactional
    @PreAuthorize("hasRole('ROLE_USER')")
    public PostResponse createReply(UUID parentPostId, PostRequest request) {
        getActivePostOrThrow(parentPostId);
        request.setParentPostId(parentPostId);
        PostResponse reply = createPost(request);
        postRepository.incrementCommentCount(parentPostId);
        return reply;
    }

    @Transactional(readOnly = true)
    public CursorResponse<PostResponse> getReplies(UUID postId, UUID cursor, int size) {
        getActivePostOrThrow(postId);

        List<UUID> ids = postRepository.findRepliesIds(postId, cursor, Limit.of(size + 1));
        boolean hasNext = ids.size() > size;
        List<UUID> pageIds = hasNext ? ids.subList(0, size) : ids;

        if (pageIds.isEmpty()) return emptyCursor();

        List<Post> posts = new ArrayList<>(postRepository.findAllWithDetailsByIds(pageIds));
        posts.sort((a, b) -> b.getId().compareTo(a.getId()));

        return CursorResponse.<PostResponse>builder()
                .content(posts.stream().map(postMapper::mapToResponse).toList())
                .hasNext(hasNext)
                .nextCursor(hasNext ? pageIds.getLast().toString() : null)
                .build();
    }

    private List<PostHashtag> addHashtagsToPost(Post post, List<String> hashtags) {
        List<PostHashtag> saved = hashtags.stream()
                .map(name -> {
                    String n = name.toLowerCase();
                    Hashtag h = hashtagRepository.findByName(n)
                            .orElseGet(() -> hashtagRepository.save(
                                    Hashtag.builder().id(UuidCreator.getTimeOrderedEpoch()).name(n).build()
                            ));
                    if (postHashtagRepository.existsByPostIdAndHashtagId(post.getId(), h.getId())) return null;
                    return postHashtagRepository.save(PostHashtag.builder().post(post).hashtag(h).build());
                })
                .filter(ph -> ph != null)
                .toList();
        return saved;
    }

    private void attachHashtag(Post post, String hashtagName) {
        String n = hashtagName.toLowerCase();
        Hashtag h = hashtagRepository.findByName(n)
                .orElseGet(() -> hashtagRepository.save(
                        Hashtag.builder().id(UuidCreator.getTimeOrderedEpoch()).name(n).build()
                ));
        if (!postHashtagRepository.existsByPostIdAndHashtagId(post.getId(), h.getId())) {
            postHashtagRepository.save(PostHashtag.builder().post(post).hashtag(h).build());
        }
    }

    private List<Mention> addMentionsByUsername(Post post, List<String> usernames) {
        List<Mention> mentions = usernames.stream()
                .map(username -> {
                    User user = userRepository.findExactByUsername(username)
                            .orElseThrow(() -> new MentionedUserNotFoundException(username));
                    return Mention.builder()
                            .id(UuidCreator.getTimeOrderedEpoch())
                            .post(post)
                            .user(user)
                            .build();
                })
                .toList();
        return mentionRepository.saveAll(mentions);
    }

    private User getUserOrThrow(UUID uuid) {
        return userRepository.findById(uuid).orElseThrow(UserNotFoundException::new);
    }

    private Post getPostOrThrow(UUID postId) {
        return postRepository.findById(postId).orElseThrow(PostNotFoundException::new);
    }

    private Post getActivePostOrThrow(UUID postId) {
        Post post = getPostOrThrow(postId);
        if (post.isDeleted()) throw new PostNotFoundException();
        return post;
    }

    private CursorResponse<PostResponse> emptyCursor() {
        return CursorResponse.<PostResponse>builder()
                .content(List.of()).hasNext(false).nextCursor(null).build();
    }
}
