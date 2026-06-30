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
    private final UserRepository userRepository;
    private final HashtagRepository hashtagRepository;
    private final PostHashtagRepository postHashtagRepository;
    private final MentionRepository mentionRepository;
    private final MediaService mediaService;
    private final PostMapper postMapper;

    @Transactional
    @PreAuthorize("hasRole('ROLE_USER')")
    public PostResponse createPost(PostRequest request) {
        UUID userId = AppUtil.userIdFromAuthentication();

        Post post = postMapper.toPost(request);
        postMapper.initNewPost(post, getUserOrThrow(userId));
        if (request.getParentPostId() != null)
            post.setParentPost(getPostOrThrow(request.getParentPostId()));
        if (request.getQuotePostId() != null)
            post.setQuotePost(getPostOrThrow(request.getQuotePostId()));

        Post savedPost = postRepository.save(post);

        if (request.getFiles() != null && !request.getFiles().isEmpty())
            //addAll giữ object collection cũ, chỉ thêm phần tử vào
            savedPost.getMedia().addAll(
                    mediaService.uploadAndAttachToPost(request.getFiles(), savedPost));
        if (request.getHashtags() != null && !request.getHashtags().isEmpty())
            //addAll giữ object collection cũ, chỉ thêm phần tử vào
            savedPost.getPostHashtags().addAll(
                    addHashtagsToPost(savedPost, request.getHashtags()));
        if (request.getMentionUsernames() != null && !request.getMentionUsernames().isEmpty())
            //addAll giữ object collection cũ, chỉ thêm phần tử vào
            savedPost.getMentions().addAll(
                    addMentionsByUsername(savedPost, request.getMentionUsernames()));

        log.info("Post created: {} by user: {}", savedPost.getId(), userId);
        return postMapper.mapToResponse(savedPost);
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
            if (!request.getHashtags().isEmpty())
                post.setPostHashtags(new HashSet<>(addHashtagsToPost(post, request.getHashtags())));
        }

        if (request.getMentionUsernames() != null) {
            mentionRepository.deleteByPostId(post.getId());
            if (!request.getMentionUsernames().isEmpty())
                post.setMentions(new HashSet<>(addMentionsByUsername(post, request.getMentionUsernames())));
        }
        Post updated = postRepository.save(post);
        log.info("Post updated: {} by user: {}", postId, userId);
        return postMapper.mapToResponse(updated);
    }

    @Transactional
    @PreAuthorize("hasRole('ROLE_USER')")
    public void deletePost(UUID postId) {
        UUID userId = AppUtil.userIdFromAuthentication();
        Post post = postRepository.findByIdAndUserId(postId, userId)
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

        // Nếu bảng tin trống, trả về kết quả rỗng luôn, đỡ tốn 1 lần gọi DB phía dưới
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

    @Transactional(readOnly = true)
    public CursorResponse<PostResponse> getPostsByHashtag(String hashtag, UUID cursor, int size) {
        String normalized = hashtag.toLowerCase();
        UUID hashtagId = hashtagRepository.findIdByName(normalized)
                .orElseThrow(() -> new HashtagNotFoundException(normalized));
        List<UUID> ids = postHashtagRepository.findPostIdsByHashtagId(hashtagId, cursor, Limit.of(size + 1));

        // Nếu không có bài viết nào thì trả về kết quả rỗng luôn, né việc gọi SQL IN ()
        // vô nghĩa
        if (ids.isEmpty()) {
            return AppUtil.buildCursorResponse(Collections.emptyList(), size, Post::getId, postMapper::mapToResponse);
        }

        // Fetch chi tiết các Post và map vào cấu trúc Map O(1)
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
        List<UUID> ids = postRepository.findRepliesIds(postId, cursor, Limit.of(size + 1));

        // nếu bài viết chưa có bình luận nào, trả về rỗng luôn để né lỗi SQL IN ()
        if (ids.isEmpty()) {
            return AppUtil.buildCursorResponse(Collections.emptyList(), size, Post::getId, postMapper::mapToResponse);
        }

        // lấy dữ liệu chi tiết từ DB (thứ tự lộn xộn) và đưa vào Map O(1)
        // function (existing, replacement) -> existing để phòng lỗi trùng key
        Map<UUID, Post> postMap = postRepository.findAllWithDetailsByIds(ids)
                .stream()
                .collect(Collectors.toMap(
                        Post::getId,
                        p -> p,
                        (existing, replacement) -> existing));
        // duyệt theo mảng ids gốc để ép List Post đầu ra phải khớp 100% thứ tự chuẩn
        // của Cursor
        List<Post> posts = ids.stream()
                .map(postMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return AppUtil.buildCursorResponse(posts, size, Post::getId, postMapper::mapToResponse);
    }

    private List<PostHashtag> addHashtagsToPost(Post post, List<String> hashtags) {
        List<String> normalized = hashtags.stream().map(String::toLowerCase).toList();

        // 1 query lấy tất cả hashtag đã tồn tại
        Map<String, Hashtag> existing = hashtagRepository.findAllByNameIn(normalized)
                .stream().collect(Collectors.toMap(Hashtag::getName, h -> h));

        // Tạo mới những cái chưa có
        List<Hashtag> toCreate = normalized.stream()
                .filter(n -> !existing.containsKey(n))
                .map(n -> Hashtag.builder().id(AppUtil.generateUUID()).name(n).build())
                .toList();
        if (!toCreate.isEmpty()) {
            hashtagRepository.saveAll(toCreate) // 1 batch INSERT
                    .forEach(h -> existing.put(h.getName(), h));
        }

        // 1 query kiểm tra PostHashtag đã tồn tại
        Set<UUID> existingHashtagIds = postHashtagRepository
                .findHashtagIdsByPostId(post.getId());

        List<PostHashtag> toSave = existing.values().stream()
                .filter(h -> !existingHashtagIds.contains(h.getId()))
                .map(h -> PostHashtag.builder().post(post).hashtag(h).build())
                .toList();

        return postHashtagRepository.saveAll(toSave); // 1 batch INSERT
    }

    private List<Mention> addMentionsByUsername(Post post, List<String> usernames) {
        List<String> normalized = usernames.stream()
                .map(u -> u.toLowerCase().trim()).toList();

        // 1 query lấy tất cả user theo username
        Map<String, User> userMap = userRepository.findAllByUsernameIn(normalized)
                .stream().collect(Collectors.toMap(
                        u -> u.getUsername().toLowerCase(), u -> u));

        // Kiểm tra username nào không tồn tại
        normalized.forEach(u -> {
            if (!userMap.containsKey(u))
                throw new MentionedUserNotFoundException(u);
        });

        List<Mention> mentions = normalized.stream()
                .map(u -> Mention.builder()
                        .id(AppUtil.generateUUID())
                        .post(post)
                        .user(userMap.get(u))
                        .build())
                .toList();

        return mentionRepository.saveAll(mentions); // 1 batch INSERT
    }

    private User getUserOrThrow(UUID userId) {
        return userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
    }

    private Post getPostOrThrow(UUID postId) {
        return postRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(PostNotFoundException::new);
    }
}