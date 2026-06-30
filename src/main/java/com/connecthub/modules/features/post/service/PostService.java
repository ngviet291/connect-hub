package com.connecthub.modules.features.post.service;

import com.connecthub.common.dto.response.CursorResponse;
import com.connecthub.common.util.AppUtil;
import com.connecthub.modules.features.post.dto.request.PostRequest;
import com.connecthub.modules.features.post.dto.request.UpdatePostRequest;
import com.connecthub.modules.features.post.dto.response.PostResponse;
import com.connecthub.modules.features.post.dto.response.UploadedMedia;
import com.connecthub.modules.features.post.entity.*;
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

    @PreAuthorize("hasRole('ROLE_USER')")
    public PostResponse createPost(PostRequest request) {
        UUID userId = AppUtil.userIdFromAuthentication();

        // I/O thuần (upload file lên storage), KHÔNG nằm trong transaction
        List<UploadedMedia> uploadedMedia =
                (request.getFiles() != null && !request.getFiles().isEmpty())
                        ? mediaService.uploadFiles(request.getFiles())
                        : List.of();

        // Transaction ngắn, chỉ làm DB - nằm ở bean khác (PostWriteService) để @Transactional chạy được
        return postWriteService.createPostTx(request, userId, uploadedMedia);
    }

    @Transactional(readOnly = true)
    public PostResponse getPost(UUID postId) {
        return postMapper.mapToResponse(
                postRepository.findByIdWithDetails(postId)
                        .orElseThrow(PostNotFoundException::new));
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
        return postMapper.mapToResponse(updated);
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
        List<UUID> ids = postRepository.findRepliesIds(postId, cursor, Limit.of(size + 1));
        return fetchPagedPosts(ids, size);
    }
    private CursorResponse<PostResponse> fetchPagedPosts(List<UUID> ids, int size) {
        if (ids.isEmpty()) {
            return AppUtil.buildCursorResponse(Collections.emptyList(), size, Post::getId, postMapper::mapToResponse);
        }

        // Lấy chi tiết các Post từ DB và đưa vào Map O(1)
        // function (existing, replacement) -> existing để phòng lỗi trùng key
        Map<UUID, Post> postMap = postRepository.findAllWithDetailsByIds(ids)
                .stream()
                .collect(Collectors.toMap(
                        Post::getId,
                        p -> p,
                        (existing, replacement) -> existing));

        // Tái cấu trúc List Post đi theo đúng thứ tự chuẩn xác của mảng ids gốc
        List<Post> posts = ids.stream()
                .map(postMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return AppUtil.buildCursorResponse(posts, size, Post::getId, postMapper::mapToResponse);
    }

    private void checkPostExistsOrThrow(UUID postId) {
        if (!postRepository.existsByIdAndIsDeletedFalse(postId))
            throw new PostNotFoundException();
    }
}