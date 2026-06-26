
package com.connecthub.modules.features.user.service;

import com.connecthub.common.util.AppUtil;
import com.connecthub.modules.features.moderation.enums.BanReason;
import com.connecthub.modules.features.moderation.service.BanService;
import com.connecthub.modules.features.user.dto.request.*;
import com.connecthub.modules.features.user.dto.response.AuthenticateResponse;
import com.connecthub.modules.features.user.dto.response.IntrospectResponse;
import com.connecthub.modules.features.user.dto.response.UserChangePasswordResponse;
import com.connecthub.modules.features.user.dto.response.UserResponse;
import com.connecthub.modules.features.user.entity.Role;
import com.connecthub.modules.features.user.entity.User;
import com.connecthub.modules.features.user.enums.RoleName;
import com.connecthub.modules.features.user.exception.*;
import com.connecthub.modules.features.user.exception.AccountLockedException;
import com.connecthub.modules.features.user.exception.BadCredentialsException;
import com.connecthub.modules.features.user.exception.DuplicateEmailException;
import com.connecthub.modules.features.user.exception.TokenExpiredException;
import com.connecthub.modules.features.user.mapper.UserMapper;
import com.connecthub.modules.features.user.repository.RoleRepository;
import com.connecthub.modules.features.user.repository.UserRepository;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class AuthenticationService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final JwtService jwtService;

    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final ChangePasswordService changePasswordService;
    private final TokenBlackListService tokenBlackListService;
    private final UserClockerService userClockerService;
    private final RoleRepository roleRepository;
    private final BanService banService;

    @Transactional
    public UserResponse register(UserCreateRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateUsernameException("username", request.getUsername());
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException("email", request.getEmail());
        }


        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new DuplicatePhoneNumberException("phone", request.getPhoneNumber());
        }

        User user = userMapper.toUser(request);
        user.setId(AppUtil.generateUUID());

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        Role roleUser = roleRepository.getReferenceById(RoleName.ROLE_USER.toString());

        user.setRoles(Set.of(roleUser));

        return userMapper.toUserResponse(userRepository.save(user));

    }

    @Transactional
    public AuthenticateResponse authenticate(AuthenticateRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(UserNotFoundException::new);

        // Check ban đang active (thay cho user.isLocked())
        banService.findActiveBanForUser(user.getId())
                .ifPresent(ban -> {
                    throw new AccountLockedException(ban.getEndDate());
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {

            int failedAttempts = userClockerService.incrementFailedAttempts(request.getUsername());

            if (failedAttempts >= 5) {
                log.warn("User {} bị khóa tạm thời do đăng nhập sai quá nhiều lần", request.getUsername());

                LocalDateTime endDate = LocalDateTime.now().plusMinutes(20);

                banService.createSystemBan(
                        user,
                        BanReason.TOO_MANY_FAILED_LOGIN_ATTEMPTS, // thêm reason này vào enum
                        endDate
                );

                userClockerService.resetFailedAttempts(request.getUsername());
                throw new AccountLockedException(endDate);
            }

            throw new BadCredentialsException();
        }

        String token = jwtService.generateAccessToken(user);

        return AuthenticateResponse.builder()
                .accessToken(token)
                .user(userMapper.toUserResponse(user))
                .expiresIn(jwtService.getExpiration())
                .refreshToken(jwtService.generateRefreshToken(user))
                .build();
    }


    public IntrospectResponse introspect(IntrospectRequest request) {
        IntrospectResponse introspectResponse = jwtService.introspect(request.getAccessToken());
        banService.findActiveBanForUser(UUID.fromString(introspectResponse.getUserId()))
                .ifPresent(ban -> {
                    throw new AccountLockedException(ban.getEndDate());
                });
        return introspectResponse;
    }

    @Transactional(readOnly = true)
    public AuthenticateResponse refreshToken(RefreshTokenRequest refreshToken) {

        String refreshTokenStr = refreshToken.getRefreshToken();

        if (!refreshTokenService.isRefreshTokenExists(refreshTokenStr)) {
            throw new TokenExpiredException();
        }

        String username = refreshTokenService.getUser(refreshTokenStr);

        User user = userRepository.findByUsername(username)
                .orElseThrow(UserNotFoundException::new);

        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);

        refreshTokenService.deleteRefreshToken(refreshTokenStr);
        refreshTokenService.saveRefreshToken(newRefreshToken, username);

        return AuthenticateResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .user(userMapper.toUserResponse(user))
                .expiresIn(jwtService.getExpiration())
                .build();

    }


    @Transactional
    @PreAuthorize("#request.username == authentication.name")
    public UserChangePasswordResponse changePassword(UserChangePasswordRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(UserNotFoundException::new);

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BadCredentialsException();
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        long currentTime = new Date().toInstant().getEpochSecond();
        changePasswordService.saveTokenInvalidationTimestamp(request.getUsername(), String.valueOf(currentTime));

        return UserChangePasswordResponse.builder()
                .success(true)
                .build();
    }


    @PreAuthorize("hasRole('ROLE_USER')")
    public void logout(LogoutRequest request) {
        String token = request.getAccessToken();
        String refreshToken = request.getRefreshToken();
        SignedJWT signedJWT = jwtService.verifyToken(token);

        try {
            String jitToken = signedJWT.getJWTClaimsSet().getJWTID();
            long expirationTime = getSecondsUntilExpiration(signedJWT.getJWTClaimsSet().getExpirationTime());

            // store blacklist with time to live equals to the remaining time of the token
            tokenBlackListService.blacklistToken(jitToken, expirationTime);

            refreshTokenService.deleteRefreshToken(refreshToken);
        } catch (ParseException e) {
            throw new UnauthenticatedException();
        }

    }

    private long getSecondsUntilExpiration(Date expirationDate) {
        long expirationEpoch = expirationDate.toInstant().getEpochSecond();
        long nowEpoch = Instant.now().getEpochSecond();
        return expirationEpoch - nowEpoch;
    }
}
