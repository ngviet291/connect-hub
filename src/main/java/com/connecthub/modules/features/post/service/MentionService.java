package com.connecthub.modules.features.post.service;

import com.connecthub.common.dto.response.CursorResponse;
import com.connecthub.common.util.AppUtil;
import com.connecthub.modules.features.post.dto.response.MentionResponse;
import com.connecthub.modules.features.post.dto.response.PostResponse;
import com.connecthub.modules.features.post.entity.Mention;
import com.connecthub.modules.features.post.entity.Post;
import com.connecthub.modules.features.post.exception.MentionedUserNotFoundException;
import com.connecthub.modules.features.post.mapper.PostMapper;
import com.connecthub.modules.features.post.repository.MentionRepository;
import com.connecthub.modules.features.user.entity.User;
import com.connecthub.modules.features.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MentionService {

        private final MentionRepository mentionRepository;
        private final PostMapper postMapper;
    private final UserRepository userRepository;

    // GET /posts/{id}/mentions
        // Trả về danh sách user được mention trong bài đăng
        @Transactional(readOnly = true)
        @PreAuthorize("hasRole('ROLE_USER')")
        public CursorResponse<MentionResponse> getMentionsByPost(UUID postId, UUID cursor, int size) {
                List<Mention> mentions = new ArrayList<>(
                                mentionRepository.findByPostId(postId, cursor, Limit.of(size + 1)));

                return AppUtil.buildCursorResponse(
                                mentions, size,
                                Mention::getId,
                                m -> MentionResponse.builder()
                                                .id(m.getId())
                                                .user(postMapper.toUserSummaryResponse(m.getUser()))
                                                .createdAt(m.getCreatedAt())
                                                .build());
        }

        // GET /users/me/mentions
        // Trả về danh sách bài đăng đang mention user hiện tại

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ROLE_USER')")
    public CursorResponse<PostResponse> getMyMentions(UUID cursor, int size) {
                UUID currentUserId = AppUtil.userIdFromAuthentication();

                List<Mention> mentions = new ArrayList<>(
                                mentionRepository.findByUserId(currentUserId, cursor, Limit.of(size + 1)));

                return AppUtil.buildCursorResponse(
                                mentions, size,
                                Mention::getId,
                                m -> postMapper.mapToResponse(m.getPost()));
        }
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ROLE_USER')")
    public List<Mention> addMentionsByUsername(Post post, List<String> usernames) {
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
}
