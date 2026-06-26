package com.connecthub.modules.features.moderation.enums;

import com.connecthub.common.exception.BaseErrorCode;
import lombok.Getter;

@Getter
public enum BanResponseCode {

    CREATE_BAN_SUCCESS(200, "Create ban successfully"),
    GET_BANS_SUCCESS(200, "Get bans successfully"),
    UNBAN_SUCCESS(200, "Unban successfully");

    private final int code;
    private final String message;

    BanResponseCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

}
