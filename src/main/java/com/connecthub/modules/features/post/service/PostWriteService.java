package com.connecthub.modules.features.post.service;

import com.connecthub.modules.features.post.dto.request.PostRequest;
import com.connecthub.modules.features.post.dto.response.PostResponse;
import com.connecthub.modules.features.post.dto.response.UploadedMedia;
import com.connecthub.modules.features.post.entity.Post;
import com.connecthub.modules.features.post.exception.PostNotFoundException;
import com.connecthub.modules.features.post.mapper.PostMapper;
import com.connecthub.modules.features.post.repository.PostRepository;
import com.connecthub.modules.features.user.entity.User;
import com.connecthub.modules.features.user.exception.UserNotFoundException;
import com.connecthub.modules.features.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

// Chỉ chứa phần thuần DB của việc tạo post (save post + gắn media/hashtag/mention đã có sẵn).
// Tách riêng bean này để @Transactional chạy được qua proxy khi PostService gọi sang
// (gọi nội bộ cùng class thì proxy bị bỏ qua, transaction sẽ không hoạt động).
@Service
@RequiredArgsConstructor
@Slf4j
public class PostWriteService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final MediaService mediaService;
    private final HashtagService hashtagService;
    private final MentionService mentionService;
    private final PostMapper postMapper;

    @Transactional
    public PostResponse createPostTx(PostRequest request, UUID userId, List<UploadedMedia> uploadedMedia) {
        Post post = postMapper.toPost(request);
        postMapper.initNewPost(post, getUserOrThrow(userId));
        if (request.getParentPostId() != null)
            post.setParentPost(getPostOrThrow(request.getParentPostId()));
        if (request.getQuotePostId() != null)
            post.setQuotePost(getPostOrThrow(request.getQuotePostId()));

        Post savedPost = postRepository.save(post);

        if (!uploadedMedia.isEmpty())
            //addAll giữ object collection cũ, chỉ thêm phần tử vào
            savedPost.getMedia().addAll(
                    mediaService.attachToPost(uploadedMedia, savedPost));
        if (request.getHashtags() != null && !request.getHashtags().isEmpty())
            //addAll giữ object collection cũ, chỉ thêm phần tử vào
            savedPost.getPostHashtags().addAll(
                    hashtagService.addHashtagsToPost(savedPost, request.getHashtags()));
        if (request.getMentionUsernames() != null && !request.getMentionUsernames().isEmpty())
            //addAll giữ object collection cũ, chỉ thêm phần tử vào
            savedPost.getMentions().addAll(
                    mentionService.addMentionsByUsername(savedPost, request.getMentionUsernames()));

        log.info("Post created: {} by user: {}", savedPost.getId(), userId);
        return postMapper.mapToResponse(savedPost);
    }

    private User getUserOrThrow(UUID userId) {
        return userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
    }

    private Post getPostOrThrow(UUID postId) {
        return postRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(PostNotFoundException::new);
    }
}