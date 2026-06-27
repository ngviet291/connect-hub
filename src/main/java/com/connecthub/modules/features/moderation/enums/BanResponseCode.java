package com.connecthub.modules.features.moderation.enums;

import lombok.Getter;

@Getter
public enum BanResponseCode {

    CREATE_BAN_SUCCESS(200, "success.ban.create"),
    GET_BANS_SUCCESS(200, "success.ban.get_bans"),
    UNBAN_SUCCESS(200, "success.ban.unban");

    private final int code;
    private final String message;

    BanResponseCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

}
