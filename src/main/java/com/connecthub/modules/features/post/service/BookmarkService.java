package com.connecthub.modules.features.post.service;

import com.connecthub.common.dto.response.CursorResponse;
import com.connecthub.common.util.AppUtil;
import com.connecthub.modules.features.post.dto.response.PostResponse;
import com.connecthub.modules.features.post.entity.Bookmark;
import com.connecthub.modules.features.post.entity.Post;
import com.connecthub.modules.features.post.exception.PostNotFoundException;
import com.connecthub.modules.features.post.mapper.PostMapper;
import com.connecthub.modules.features.post.repository.BookmarkRepository;
import com.connecthub.modules.features.post.repository.PostRepository;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostMapper postMapper;

    @Transactional
    @PreAuthorize("hasRole('ROLE_USER')")
    public boolean toggleBookmark(UUID postId) {
        UUID userId = AppUtil.userIdFormAuthentication();
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        Post post = postRepository.findById(postId).orElseThrow(PostNotFoundException::new);

        return bookmarkRepository.findByPostIdAndUserId(postId, userId)
                .map(existing -> {
                    // Đã bookmark  xóa
                    bookmarkRepository.delete(existing);
                    postRepository.decrementBookmarkCount(postId); //  giảm counter
                    log.info("User {} unbookmarked post {}", userId, postId);
                    return false;
                })
                .orElseGet(() -> {
                    // Chưa bookmark  thêm
                    Bookmark bookmark = Bookmark.builder()
                            .id(UuidCreator.getTimeOrderedEpoch())
                            .user(user)
                            .post(post)
                            .build();
                    bookmarkRepository.save(bookmark);
                    postRepository.incrementBookmarkCount(postId); // tăng counter
                    log.info("User {} bookmarked post {}", userId, postId);
                    return true;
                });
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ROLE_USER')")
    public CursorResponse<PostResponse> getBookmarkedPosts(UUID cursor, int size) {
        UUID userId = AppUtil.userIdFormAuthentication();

        List<Bookmark> bookmarks = new ArrayList<>(
                bookmarkRepository.findByUserIdWithDetails(userId, cursor, Limit.of(size + 1))
        );

        return AppUtil.buildCursorResponse(
                bookmarks,
                size,
                Bookmark::getId,
                b -> postMapper.mapToResponse(b.getPost())
        );
    }
}