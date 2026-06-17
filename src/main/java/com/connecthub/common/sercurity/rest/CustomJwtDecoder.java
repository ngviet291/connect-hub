package com.connecthub.common.sercurity.rest;

import com.connecthub.modules.features.user.dto.request.IntrospectRequest;
import com.connecthub.modules.features.user.exception.AccountLockedException;
import com.connecthub.modules.features.user.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.util.Objects;

@RequiredArgsConstructor
@Component
public class CustomJwtDecoder implements JwtDecoder {


    private final AuthenticationService authenticationService;
    @Value("${jwt.secret-key}")
    private String SECRET_KEY;

    private NimbusJwtDecoder nimbusJwtDecoder = null;


    /*
     * This method will first call the introspect endpoint to check if the token is active.
     * If the token is active, it will decode the token using NimbusJwtDecoder.
     * If the token is not active, it will throw a JwtException.
     */
    @Override
    public Jwt decode(String token) throws JwtException {
        try {
            var response = authenticationService.introspect(IntrospectRequest.builder().accessToken(token).build());

            if (!response.isActive()) throw new JwtException("Invalid token");

            if (Objects.isNull(nimbusJwtDecoder)) {
                SecretKeySpec secretKeySpec = new SecretKeySpec(SECRET_KEY.getBytes(), "HS512");
                nimbusJwtDecoder = NimbusJwtDecoder.withSecretKey(secretKeySpec)
                        .macAlgorithm(MacAlgorithm.HS512)
                        .build();
            }

            return nimbusJwtDecoder.decode(token);
        } catch (AccountLockedException e) {
            // If the account is locked,
            throw new BadJwtException(e.getMessage(), e);
        }
    }
}
