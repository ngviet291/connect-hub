package com.connecthub.modules.features.post.service;

import com.connecthub.common.util.AppUtil;
import com.connecthub.modules.features.post.entity.Hashtag;
import com.connecthub.modules.features.post.entity.Post;
import com.connecthub.modules.features.post.entity.PostHashtag;
import com.connecthub.modules.features.post.repository.HashtagRepository;
import com.connecthub.modules.features.post.repository.PostHashtagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HashtagService {

    private final HashtagRepository hashtagRepository;
    private final PostHashtagRepository postHashtagRepository;

    public List<PostHashtag> addHashtagsToPost(Post post, List<String> hashtags) {
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
}