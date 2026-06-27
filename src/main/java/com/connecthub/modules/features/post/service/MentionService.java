package com.connecthub.modules.features.post.service;

import com.connecthub.common.dto.response.CursorResponse;
import com.connecthub.common.util.AppUtil;
import com.connecthub.modules.features.post.dto.response.MentionResponse;
import com.connecthub.modules.features.post.dto.response.PostResponse;
import com.connecthub.modules.features.post.entity.Mention;
import com.connecthub.modules.features.post.exception.PostNotFoundException;
import com.connecthub.modules.features.post.mapper.PostMapper;
import com.connecthub.modules.features.post.repository.MentionRepository;
import com.connecthub.modules.features.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MentionService {

    private final MentionRepository mentionRepository;
    private final PostRepository postRepository;
    private final PostMapper postMapper;

    //GET /posts/{id}/mentions
     //Trả về danh sách user được mention trong bài đăng
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ROLE_USER')")
    public CursorResponse<MentionResponse> getMentionsByPost(UUID postId, UUID cursor, int size) {
        postRepository.findById(postId).orElseThrow(PostNotFoundException::new);

        List<Mention> mentions = new ArrayList<>(
                mentionRepository.findByPostId(postId, cursor, Limit.of(size + 1))
        );

        return AppUtil.buildCursorResponse(
                mentions, size,
                Mention::getId,
                m -> MentionResponse.builder()
                        .id(m.getId())
                        .user(postMapper.toUserSummaryResponse(m.getUser()))
                        .createdAt(m.getCreatedAt())
                        .build()
        );
    }

    //GET /users/me/mentions
     //Trả về danh sách bài đăng đang mention user hiện tại

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ROLE_USER')")
    public CursorResponse<PostResponse> getMyMentions(UUID cursor, int size) {
        UUID currentUserId = AppUtil.userIdFormAuthentication();

        List<Mention> mentions = new ArrayList<>(
                mentionRepository.findByUserId(currentUserId, cursor, Limit.of(size + 1))
        );

        return AppUtil.buildCursorResponse(
                mentions, size,
                Mention::getId,
                m -> postMapper.mapToResponse(m.getPost())
        );
    }
}
