package com.connecthub.modules.features.post.controller;

import com.connecthub.common.dto.response.ApiResponse;
import com.connecthub.modules.features.post.dto.response.HashtagResponse;
import com.connecthub.modules.features.post.enums.HashTagResponseCode;
import com.connecthub.modules.features.post.service.HashtagService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/hashtags")
@RequiredArgsConstructor
public class HashtagController {

    private final HashtagService hashtagService;

    @GetMapping("/trending/{limit}")
    public ApiResponse<List<HashtagResponse>> getTrendingHashtags(@PathVariable Integer limit) {
        return ApiResponse.<List<HashtagResponse>>builder()
                .code(HashTagResponseCode.TRENDING_HASHTAG.getCode())
                .message(HashTagResponseCode.TRENDING_HASHTAG.getMessage())
                .data(hashtagService.getTrendingHashtags(limit))
                .build();
    }
}
