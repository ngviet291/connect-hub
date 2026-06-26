package com.connecthub.modules.features.moderation.enums;

public enum BanReason {
    SPAM, // gửi tin nhắn rác, spam
    HARASSMENT, // quấy rối, bắt nạt
    HATE_SPEECH,// phát ngôn thù ghét
    INAPPROPRIATE_CONTENT, // nội dung không phù hợp
    IMPERSONATION, //  giả mạo
    SCAM_FRAUD,// lừa đảo, gian lận
    VIOLENCE_THREAT, // bạo lực, đe dọa
    NUDITY, // khỏa thân, khiêu dâm
    TOO_MANY_FAILED_LOGIN_ATTEMPTS, // quá nhiều lần đăng nhập thất bại
    OTHER
}