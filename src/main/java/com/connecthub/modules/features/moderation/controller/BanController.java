package com.connecthub.modules.features.moderation.controller;

import com.connecthub.common.dto.response.ApiResponse;
import com.connecthub.common.dto.response.PagingResponse;
import com.connecthub.modules.features.moderation.dto.request.ban.CreateBanRequest;
import com.connecthub.modules.features.moderation.dto.request.ban.UnbanRequest;
import com.connecthub.modules.features.moderation.dto.response.ban.BanResponse;
import com.connecthub.modules.features.moderation.enums.BanResponseCode;
import com.connecthub.modules.features.moderation.service.BanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/bans")
public class BanController {

    private final BanService banService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ApiResponse<BanResponse> createBan(@Valid @RequestBody CreateBanRequest request) {
        return ApiResponse.<BanResponse>builder()
                .code(BanResponseCode.CREATE_BAN_SUCCESS.getCode())
                .message(BanResponseCode.CREATE_BAN_SUCCESS.getMessage())
                .data(banService.createBan(request))
                .build();
    }

    @GetMapping
    public ApiResponse<PagingResponse<BanResponse>> getBans(Pageable pageable, @RequestParam(required = false, defaultValue = "true") boolean active) {
        return ApiResponse.<PagingResponse<BanResponse>>builder()
                .code(BanResponseCode.GET_BANS_SUCCESS.getCode())
                .message(BanResponseCode.GET_BANS_SUCCESS.getMessage())
                .data(banService.getAllBans(pageable, active))
                .build();
    }

    @PatchMapping("/{banId}/unban")
    public ApiResponse<BanResponse> unbanUser(@PathVariable UUID banId, @RequestBody UnbanRequest request) {
        return ApiResponse.<BanResponse>builder()
                .code(BanResponseCode.UNBAN_SUCCESS.getCode())
                .message(BanResponseCode.UNBAN_SUCCESS.getMessage())
                .data(banService.unbanUser(banId, request))
                .build();
    }

}
