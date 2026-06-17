package com.connecthub.modules.features.user.service;

import com.connecthub.common.exception.AppException;
import com.connecthub.common.exception.ErrorCode;
import com.connecthub.modules.features.user.dto.response.IntrospectResponse;
import com.connecthub.modules.features.user.entity.Role;
import com.connecthub.modules.features.user.entity.User;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtService {
    private final ChangePasswordService changePasswordService;
    private final TokenBlackListService tokenBlackListService;
    @Value("${jwt.secret-key}")
    private String SECRET_KEY;

    @Value("${jwt.expiration}")
    private long VALID_DURATION;

    @Value("${spring.application.name}")
    private String ISSUER;

    private static final String SCOPE_CLAIM = "scope";

    private final RefreshTokenService refreshTokenService;


    public String generateAccessToken(User user) {
        JWSHeader jwsHeader = new JWSHeader.Builder(JWSAlgorithm.HS512)
                .type(JOSEObjectType.JWT)
                .build();

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(user.getUsername())
                .issuer(ISSUER)
                .issueTime(new Date())
                .expirationTime(new Date(Instant.now().plus(VALID_DURATION, ChronoUnit.SECONDS).toEpochMilli()))
                .claim(SCOPE_CLAIM, buildScope(user))
                .jwtID(UUID.randomUUID().toString())
                .build();

        Payload payload = new Payload(claimsSet.toJSONObject());

        JWSObject jwsObject = new JWSObject(jwsHeader, payload);


        try {
            jwsObject.sign(new MACSigner(SECRET_KEY.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            throw new AppException(ErrorCode.GENERATE_TOKEN_FAILED);
        }
    }

    private String buildScope(User user) {
        if (user.getRoles() != null) {
            Role userRole = user.getRoles().iterator().next();
            return userRole.getName().name();
        }
        return "";
    }

    public long getExpiration() {
        return VALID_DURATION;
    }

    public String generateRefreshToken(User user) {
        String refreshToken = UUID.randomUUID().toString();
        refreshTokenService.saveRefreshToken(refreshToken, user.getUsername());
        return refreshToken;
    }

    public IntrospectResponse introspect(String token) {

        try {
            SignedJWT
                    signedJWT = verifyToken(token);
            JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
            return IntrospectResponse.builder()
                    .active(true)
                    .scope(claimsSet.getStringClaim(SCOPE_CLAIM))
                    .clientId(claimsSet.getIssuer())
                    .username(claimsSet.getSubject())
                    .exp(claimsSet.getExpirationTime().getTime() / 1000)
                    .iat(claimsSet.getIssueTime().getTime() / 1000)
                    .sub(claimsSet.getSubject())
                    .build();

        } catch (AppException e) {
            return IntrospectResponse.builder()
                    .active(false)
                    .build();
        } catch (ParseException e) {
            throw new AppException(ErrorCode.INTROSPECT_FAILED);
        }


    }

    public SignedJWT verifyToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWSVerifier verifier = new MACVerifier(SECRET_KEY.getBytes());

            boolean verified = signedJWT.verify(verifier);

            Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
            // Check if the token is valid and not expired
            if (!(verified && expirationTime.after(new Date()))) {
                throw new AppException(ErrorCode.INTROSPECT_FAILED);
            }
            // Check if blacklisted
            String jitToken = signedJWT.getJWTClaimsSet().getJWTID();
            if (tokenBlackListService.isBlacklisted(jitToken)) {
                throw new AppException(ErrorCode.UNAUTHENTICATED);
            }

            // Check if the token is invalidated due to password change
            Date iatTime = signedJWT.getJWTClaimsSet().getIssueTime();
            String email = signedJWT.getJWTClaimsSet().getSubject();
            if (changePasswordService.isTokenInvalidationTimestampExists(email)) {
                long changePasswordDate = Long.parseLong(changePasswordService.getTokenInvalidationTimestamp(email));
                if (changePasswordDate > iatTime.toInstant().toEpochMilli()) {
                    throw new AppException(ErrorCode.UNAUTHENTICATED);
                }
            }
            return signedJWT;


        } catch (ParseException | JOSEException e) {
            throw new AppException(ErrorCode.INTROSPECT_FAILED);
        }
    }


}