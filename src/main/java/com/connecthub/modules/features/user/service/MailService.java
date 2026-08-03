package com.connecthub.modules.features.user.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {
    private final JavaMailSender mailSender;

    @Async
    public void sendResetPasswordOtpMail(String to, String otp) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setTo(to);
            helper.setSubject("[ConnectHub] Reset Your Password");

            String htmlContent = String.format(
                    "<h3>Reset Your Password</h3>" +
                    "<p>We received a request to reset your password. Use the following 6-digit OTP code to verify and reset your password:</p>" +
                    "<p style=\"font-size:24px; font-weight:bold; letter-spacing: 5px; color:#4CAF50;\">%s</p>" +
                    "<p>If you didn't request this, you can safely ignore this email.</p>" +
                    "<p>This OTP is valid for 15 minutes.</p>" +
                    "<br/>" +
                    "<p>Best regards,<br/>ConnectHub Team</p>",
                    otp
            );

            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
            log.info("Reset password OTP email successfully sent to {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send reset password OTP email to {}", to, e);
        }
    }
}
