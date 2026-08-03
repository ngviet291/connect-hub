package com.connecthub.modules.features.user.service;

import com.connecthub.modules.features.user.dto.request.ForgotPasswordRequest;
import com.connecthub.modules.features.user.dto.request.ResetPasswordRequest;
import com.connecthub.modules.features.user.entity.User;
import com.connecthub.modules.features.user.exception.UserNotFoundException;
import com.connecthub.modules.features.user.exception.InvalidResetTokenException;
import com.connecthub.modules.features.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ForgotPasswordService {
    private final UserRepository userRepository;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, String> redis;

    private static final String RESET_OTP_PREFIX = "RESET_PASSWORD_OTP:";
    private static final long OTP_TTL_MINUTES = 15;

    public ForgotPasswordService(
            UserRepository userRepository,
            MailService mailService,
            PasswordEncoder passwordEncoder,
            @Qualifier("redisTemplate") RedisTemplate<String, String> redis
    ) {
        this.userRepository = userRepository;
        this.mailService = mailService;
        this.passwordEncoder = passwordEncoder;
        this.redis = redis;
    }

    public void sendForgotPasswordEmail(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(UserNotFoundException::new);

        String otp = generateOtp();

        // Save to Redis (Email -> OTP)
        redis.opsForValue().set(
                RESET_OTP_PREFIX + user.getEmail(),
                otp,
                OTP_TTL_MINUTES,
                TimeUnit.MINUTES
        );

        mailService.sendResetPasswordOtpMail(user.getEmail(), otp);
        log.info("Saved 6-digit reset OTP for user email: {}", user.getEmail());
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String tokenKey = RESET_OTP_PREFIX + request.getEmail();
        String savedOtp = redis.opsForValue().get(tokenKey);

        if (savedOtp == null || !savedOtp.equals(request.getOtp())) {
            throw new InvalidResetTokenException();
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(UserNotFoundException::new);

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Invalidate OTP code
        redis.delete(tokenKey);
        log.info("Successfully reset password using OTP for user: {}", request.getEmail());
    }

    private String generateOtp() {
        Random random = new Random();
        int otpNum = 100000 + random.nextInt(900000);
        return String.valueOf(otpNum);
    }
}
