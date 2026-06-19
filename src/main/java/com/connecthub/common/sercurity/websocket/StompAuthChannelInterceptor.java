package com.connecthub.common.sercurity.websocket;

import com.connecthub.modules.features.user.service.JwtService;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // Thêm Log để giám sát lỗi
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j // Khởi tạo logger tự động từ Lombok
public class StompAuthChannelInterceptor implements ChannelInterceptor {
    private final JwtService jwtService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authorizationHeader = accessor.getFirstNativeHeader("Authorization");

            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                String token = authorizationHeader.substring(7);

                try {
                    // Xác thực token và lấy dữ liệu một cách an toàn
                    if (jwtService.isTokenValid(token)) {
                        SignedJWT signedJWT = jwtService.verifyToken(token);
                        JWTClaimsSet jwtClaimsSet = signedJWT.getJWTClaimsSet();
                        String username = jwtClaimsSet.getSubject();

                        List<GrantedAuthority> authorities = parseRoles(jwtClaimsSet);

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(username, null, authorities);

                        // Gán quyền hạn trực tiếp vào phiên làm việc của STOMP
                        accessor.setUser(authentication);
                        log.info("Successfully authenticated WebSocket user: {}", username);
                    } else {
                        log.warn("WebSocket connection rejected: Token is invalid or expired.");
                        return null; // Trả về null chủ động để từ chối kết nối không hợp lệ
                    }
                } catch (Exception e) {
                    // BẮT BUỘC IN LOG RA ĐÂY để biết chính xác lý do bị sập kênh
                    log.error("Error occurred during WebSocket authentication process: ", e);
                    return null; // Ngắt kết nối một cách an toàn để tránh crash executor channel
                }
            } else {
                log.warn("WebSocket CONNECT frame missing Bearer token in Authorization header.");
                return null;
            }
        }
        return message;
    }

    private List<GrantedAuthority> parseRoles(JWTClaimsSet jwtClaimsSet) {
        try {
            // Lấy claim an toàn tránh NullPointerException
            Object scopeClaimObj = jwtClaimsSet.getClaim("scope");
            if (scopeClaimObj == null) {
                return List.of();
            }

            String scopeClaim = scopeClaimObj.toString();
            if (scopeClaim.isBlank()) {
                return List.of();
            }

            return Arrays.stream(scopeClaim.split(" "))
                    .map(SimpleGrantedAuthority::new)
                    .map(GrantedAuthority.class::cast)
                    .toList();
        } catch (Exception e) {
            log.error("Failed to parse roles from JWT claim set: ", e);
            return List.of();
        }
    }

}