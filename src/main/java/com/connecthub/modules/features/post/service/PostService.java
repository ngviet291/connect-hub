package com.connecthub.modules.features.post.service;

import com.connecthub.common.dto.response.CursorResponse;
import com.connecthub.common.util.AppUtil;
import com.connecthub.modules.features.post.dto.projection.MyReactionProjection;
import com.connecthub.modules.features.post.dto.projection.RepostedPostIdProjection;
import com.connecthub.modules.features.post.dto.request.PostRequest;
import com.connecthub.modules.features.post.dto.request.UpdatePostRequest;
import com.connecthub.modules.features.post.dto.response.PostResponse;
import com.connecthub.modules.features.post.dto.response.UploadedMedia;
import com.connecthub.modules.features.post.entity.*;
import com.connecthub.modules.features.post.enums.ReactionType;
import com.connecthub.modules.features.post.exception.HashtagNotFoundException;
import com.connecthub.modules.features.post.exception.PostAccessDeniedException;
import com.connecthub.modules.features.post.exception.PostNotFoundException;
import com.connecthub.modules.features.post.mapper.PostMapper;
import com.connecthub.modules.features.post.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {

    private final PostRepository postRepository;
    private final HashtagRepository hashtagRepository;
    private final PostHashtagRepository postHashtagRepository;
    private final MentionRepository mentionRepository;
    private final MediaService mediaService;
    private final PostMapper postMapper;
    private final HashtagService hashtagService;
    private final MentionService mentionService;
    private final PostWriteService postWriteService;
    private final ReactionRepository reactionRepository;
    private final RepostRepository repostRepository;
    private final BookmarkRepository bookmarkRepository;
    private final MediaRepository mediaRepository;

    private UUID currentUserIdOrNull() {
        try {
            return AppUtil.userIdFromAuthentication();
        } catch (Exception e) {
            return null;
        }
    }

    private Map<UUID, ReactionType> findMyReactionTypes(UUID userId, List<UUID> postIds) {
        if (userId == null || postIds.isEmpty()) return Map.of();
        return reactionRepository.findMyReactionTypes(userId, postIds).stream()
                .collect(Collectors.toMap(
                        MyReactionProjection::getPostId,
                        MyReactionProjection::getType));
    }

    @PreAuthorize("hasRole('ROLE_USER')")
    public PostResponse createPost(PostRequest request) {
        UUID userId = AppUtil.userIdFromAuthentication();

        List<UploadedMedia> uploadedMedia =
                (request.getFiles() != null && !request.getFiles().isEmpty())
                        ? mediaService.uploadFiles(request.getFiles())
                        : List.of();

        // Transaction ngắn, chỉ làm DB - nằm ở bean khác (PostWriteService) để @Transactional chạy được
        return postWriteService.createPostTx(request, userId, uploadedMedia);
    }

    @Transactional(readOnly = true)
    public PostResponse getPost(UUID postId) {
        Post post = postRepository.findByIdWithDetails(postId)
                .orElseThrow(PostNotFoundException::new);

        UUID userId = currentUserIdOrNull();
        List<UUID> ids = List.of(postId);

        ReactionType myReactionType = findMyReactionTypes(userId, ids).get(postId);
        boolean bookmarked = userId != null && !bookmarkRepository.findBookmarkedPostIds(userId, ids).isEmpty();
        boolean reposted   = userId != null && !repostRepository.findRepostedPostIds(userId, ids).isEmpty();

        return postMapper.mapToResponse(post, myReactionType, reposted, bookmarked);
    }

    @Transactional
    @PreAuthorize("hasRole('ROLE_USER')")
    public PostResponse updatePost(UUID postId, UpdatePostRequest request) {
        UUID userId = AppUtil.userIdFromAuthentication();
        Post post = postRepository.findByIdAndUserIdWithDetails(postId, userId)
                .orElseThrow(PostAccessDeniedException::new);

        if (request.getContent() != null)
            post.setContent(request.getContent());
        if (request.getVisibility() != null)
            post.setVisibility(request.getVisibility());
        if (request.getMediaIdsToDelete() != null && !request.getMediaIdsToDelete().isEmpty()) {
            mediaRepository.deleteByIdsAndPostId(request.getMediaIdsToDelete(), postId);
            // Sync lại collection trong memory để mapper không map media cũ
            post.getMedia().removeIf(m -> request.getMediaIdsToDelete().contains(m.getId()));
        }
        if (request.getHashtags() != null) {
            postHashtagRepository.deleteByPostId(post.getId());
            post.setPostHashtags(request.getHashtags().isEmpty()
                    ? new HashSet<>()
                    : new HashSet<>(hashtagService.addHashtagsToPost(post, request.getHashtags())));
        }

        if (request.getMentionUsernames() != null) {
            mentionRepository.deleteByPostId(post.getId());
            post.setMentions(request.getMentionUsernames().isEmpty()
                    ? new HashSet<>()
                    : new HashSet<>(mentionService.addMentionsByUsername(post, request.getMentionUsernames())));
        }

        Post updated = postRepository.save(post);
        log.info("Post updated: {} by user: {}", postId, userId);

        List<UUID> ids = List.of(postId);
        ReactionType myReactionType = findMyReactionTypes(userId, ids).get(postId);
        boolean reposted   = repostRepository.existsByPostIdAndUserId(postId, userId);
        boolean bookmarked = bookmarkRepository.existsByPostIdAndUserId(postId, userId);

        return postMapper.mapToResponse(updated, myReactionType, reposted, bookmarked);
    }

    @Transactional
    @PreAuthorize("hasRole('ROLE_USER')")
    public void deletePost(UUID postId) {
        UUID userId = AppUtil.userIdFromAuthentication();
        Post post = postRepository.findByIdAndUserIdAndIsDeletedFalse(postId, userId)
                .orElseThrow(PostAccessDeniedException::new);

        post.setDeleted(true);
        postRepository.save(post);

        if (post.getParentPost() != null)
            postRepository.decrementCommentCount(post.getParentPost().getId());

        log.info("Post deleted: {} by user: {}", postId, userId);
    }

    @Transactional(readOnly = true)
    public CursorResponse<PostResponse> getUserFeed(UUID cursor, int size) {
        List<UUID> ids = postRepository.findPublicFeedIds(cursor, Limit.of(size + 1));
        return fetchPagedPosts(ids, size);
    }

    @Transactional(readOnly = true)
    public CursorResponse<PostResponse> getPostsByHashtag(String hashtag, UUID cursor, int size) {
        String normalized = hashtag.toLowerCase();
        UUID hashtagId = hashtagRepository.findIdByName(normalized)
                .orElseThrow(() -> new HashtagNotFoundException(normalized));
        List<UUID> ids = postHashtagRepository.findPostIdsByHashtagId(hashtagId, cursor, Limit.of(size + 1));
        return fetchPagedPosts(ids, size);
    }

    @Transactional
    @PreAuthorize("hasRole('ROLE_USER')")
    public PostResponse createReply(UUID parentPostId, PostRequest request) {
        checkPostExistsOrThrow(parentPostId);
        request.setParentPostId(parentPostId);
        PostResponse reply = createPost(request);
        postRepository.incrementCommentCount(parentPostId);
        return reply;
    }

    @Transactional(readOnly = true)
    public CursorResponse<PostResponse> getReplies(UUID postId, UUID cursor, int size) {
        checkPostExistsOrThrow(postId);
        List<UUID> ids = postRepository.findRepliesByPostIdWithMedia(postId, cursor, Limit.of(size + 1));
        return fetchPagedPosts(ids, size);
    }

    private CursorResponse<PostResponse> fetchPagedPosts(List<UUID> ids, int size) {
        if (ids.isEmpty()) {
            return AppUtil.buildCursorResponse(
                    Collections.emptyList(), size, Post::getId,
                    p -> postMapper.mapToResponse(p, null, false, false));
        }

        Map<UUID, Post> postMap = postRepository.findAllWithDetailsByIds(ids)
                .stream()
                .collect(Collectors.toMap(Post::getId, p -> p, (existing, replacement) -> existing));

        List<Post> posts = ids.stream()
                .map(postMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 3 query batch — CHỈ 1 LẦN cho cả trang, không phải N+1 theo từng post.
        UUID userId = currentUserIdOrNull();
        Map<UUID, ReactionType> myReactionByPostId = findMyReactionTypes(userId, ids);
        Set<UUID> bookmarkedIds = userId != null ? bookmarkRepository.findBookmarkedPostIds(userId, ids) : Set.of();
        Set<UUID> repostedIds   = userId != null ? repostRepository.findRepostedPostIds(userId, ids)     : Set.of();

        return AppUtil.buildCursorResponse(posts, size, Post::getId, p ->
                postMapper.mapToResponse(
                        p,
                        myReactionByPostId.get(p.getId()), // null nếu chưa react
                        repostedIds.contains(p.getId()),
                        bookmarkedIds.contains(p.getId())));
    }

    private void checkPostExistsOrThrow(UUID postId) {
        if (!postRepository.existsByIdAndIsDeletedFalse(postId))
            throw new PostNotFoundException();
    }
    @Transactional(readOnly = true)
    public CursorResponse<PostResponse> getUserPosts(String username, UUID cursor, int size) {

        UUID currentUserId = AppUtil.currentUserIdOrNull();
        List<UUID> ids = postRepository.findUserPostIds(username, currentUserId, cursor, Limit.of(size + 1));
        return fetchPagedPosts(ids, size);
    }
    @Transactional(readOnly = true)
    public CursorResponse<PostResponse> getUserReplies(String username, UUID cursor, int size) {
        UUID currentUserId = AppUtil.currentUserIdOrNull();
        List<UUID> ids = postRepository.findUserReplyIds(username, currentUserId, cursor, Limit.of(size + 1));
        return fetchPagedPosts(ids, size);
    }

    @Transactional(readOnly = true)
    public CursorResponse<PostResponse> getUserMedia(String username, UUID cursor, int size) {
        UUID currentUserId = AppUtil.currentUserIdOrNull();
        List<UUID> ids = postRepository.findUserMediaPostIds(username, currentUserId, cursor, Limit.of(size + 1));
        return fetchPagedPosts(ids, size);
    }

    @Transactional(readOnly = true)
    public CursorResponse<PostResponse> getUserReposts(String username, UUID cursor, int size) {
        UUID currentUserId = AppUtil.currentUserIdOrNull();

        List<RepostedPostIdProjection> rows =
                repostRepository.findUserRepostedPostIds(username, currentUserId, cursor, Limit.of(size + 1));

        boolean hasNext = rows.size() > size;
        List<RepostedPostIdProjection> page = hasNext ? rows.subList(0, size) : rows;

        if (page.isEmpty()) {
            return CursorResponse.<PostResponse>builder()
                    .content(Collections.emptyList()).hasNext(false).nextCursor(null).build();
        }

        List<UUID> postIds = page.stream().map(RepostedPostIdProjection::getPostId).toList();

        Map<UUID, Post> postMap = postRepository.findAllWithDetailsByIds(postIds).stream()
                .collect(Collectors.toMap(Post::getId, p -> p, (existing, replacement) -> existing));

        Map<UUID, ReactionType> myReactionByPostId = findMyReactionTypes(currentUserId, postIds);
        Set<UUID> bookmarkedIds = currentUserId == null ? Set.of() : bookmarkRepository.findBookmarkedPostIds(currentUserId, postIds);

        List<PostResponse> content = page.stream()
                .map(row -> postMap.get(row.getPostId()))
                .filter(Objects::nonNull)
                .map(p -> postMapper.mapToResponse(
                        p,
                        myReactionByPostId.get(p.getId()),
                        true,
                        bookmarkedIds.contains(p.getId())
                ))
                .toList();

        UUID nextCursor = hasNext ? page.getLast().getRepostId() : null;

        return CursorResponse.<PostResponse>builder()
                .content(content)
                .hasNext(hasNext)
                .nextCursor(nextCursor == null ? null : nextCursor.toString())
                .build();
    }
}