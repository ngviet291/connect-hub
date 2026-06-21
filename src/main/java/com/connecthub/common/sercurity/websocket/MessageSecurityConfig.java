package com.connecthub.common.sercurity.websocket;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;

@Configuration
@EnableWebSocketSecurity
public class MessageSecurityConfig {

    @Bean
    AuthorizationManager<Message<?>> messageAuthorizationManager(
            MessageMatcherDelegatingAuthorizationManager.Builder messages) {

        messages
                // CONNECT / DISCONNECT / HEARTBEAT — không cần auth
                .simpTypeMatchers(SimpMessageType.CONNECT,
                        SimpMessageType.DISCONNECT,
                        SimpMessageType.HEARTBEAT).permitAll()

                // Subscribe các topic chat — cần authenticated
                .simpSubscribeDestMatchers("/topic/conversations/**").authenticated()  // ← THÊM
                .simpSubscribeDestMatchers("/user/queue/**").authenticated()
                .simpSubscribeDestMatchers("/topic/orders/**").authenticated()

                // Admin
                .simpSubscribeDestMatchers("/topic/admin/**").hasRole("ADMIN")

                // Send đến /app/**
                .simpDestMatchers("/app/**").authenticated()  // ← THÊM (thay vì liệt kê từng cái)

                .anyMessage().denyAll();

        return messages.build();
    }

    @Bean("csrfChannelInterceptor")
    public ChannelInterceptor csrfChannelInterceptor() {
        return new ChannelInterceptor() {
            // Override rỗng — bỏ qua CSRF check hoàn toàn
        };
    }
}