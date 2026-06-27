package com.connecthub.modules.features.moderation.service;

import com.connecthub.common.dto.response.PagingResponse;
import com.connecthub.common.util.AppUtil;
import com.connecthub.modules.features.moderation.dto.request.ban.CreateBanRequest;
import com.connecthub.modules.features.moderation.dto.request.ban.UnbanRequest;
import com.connecthub.modules.features.moderation.dto.response.ban.BanResponse;
import com.connecthub.modules.features.moderation.entity.Ban;
import com.connecthub.modules.features.moderation.enums.BanReason;
import com.connecthub.modules.features.moderation.exception.ban.BanAlreadyInactiveException;
import com.connecthub.modules.features.moderation.exception.ban.BanNotFoundException;
import com.connecthub.modules.features.moderation.exception.ban.UserAlreadyBannedException;
import com.connecthub.modules.features.moderation.mapper.BanMapper;
import com.connecthub.modules.features.moderation.repository.BanRepository;
import com.connecthub.modules.features.notification.dto.request.NotificationRequest;
import com.connecthub.modules.features.notification.enums.NotificationType;
import com.connecthub.modules.features.notification.service.NotificationService;
import com.connecthub.modules.features.user.entity.User;
import com.connecthub.modules.features.user.exception.UserNotFoundException;
import com.connecthub.modules.features.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class BanService {

    private static final String ACTIVE_BAN_CACHE = "activeBan";

    private final BanMapper banMapper;
    private final BanRepository banRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;


    @Transactional
    @PreAuthorize("hasRole('MODERATOR')")
    @CacheEvict(value = ACTIVE_BAN_CACHE, key = "#request.userId")
    public BanResponse createBan(CreateBanRequest request) {
        Ban banEntity = banMapper.toBan(request);
        UUID issuerId = AppUtil.userIdFormAuthentication();

        // nếu người dùng đã bị cấm, ném ra ngoại lệ
        if (banRepository.existsActiveBanByUserId(request.getUserId(), LocalDateTime.now())) {
            throw new UserAlreadyBannedException();
        }

        User bannedUser = userRepository.findById(request.getUserId()).orElseThrow(UserNotFoundException::new);
        User issuer = userRepository.getReferenceById(issuerId);

        banEntity.setId(AppUtil.generateUUID());
        banEntity.setBannedBy(issuer);
        banEntity.setUser(bannedUser);

        String reason = String.format("You have been banned for reason: %s. The ban will last until %s.", request.getReason(), request.getEndDate());
        pushBanNotification(bannedUser.getId(), reason, issuerId);
        return banMapper.toBanResponse(banRepository.save(banEntity));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('MODERATOR')")
    public PagingResponse<BanResponse> getAllBans(Pageable pageable, boolean active) {
        LocalDateTime now = LocalDateTime.now();
        Page<Ban> bansPage;
        if (active) {
            bansPage = banRepository.findAllActive(now, pageable);
        } else {
            bansPage = banRepository.findBansAll(pageable);
        }
        return AppUtil.buildPagingResponse(bansPage, banMapper::toBanResponse);
    }

    @Transactional
    @PreAuthorize("hasRole('MODERATOR')")
    @CacheEvict(value = ACTIVE_BAN_CACHE, key = "#result.userId")
    public BanResponse unbanUser(UUID banId, UnbanRequest request) {
        Ban ban = banRepository.findById(banId).orElseThrow(() -> new BanNotFoundException(banId.toString()));

        if (!ban.isActive()) {
            throw new BanAlreadyInactiveException();
        }

        User unbannedBy = userRepository.getReferenceById(AppUtil.userIdFormAuthentication());
        ban.setUnbannedBy(unbannedBy);
        ban.setUnbannedAt(LocalDateTime.now());
        ban.setUnbanReason(request.getUnbanReason());

        return banMapper.toBanResponse(banRepository.save(ban));
    }


    @PreAuthorize("hasRole('USER')")
    @Transactional(readOnly = true)
    public boolean isUserBanned(UUID userId) {
        LocalDateTime now = LocalDateTime.now();
        return banRepository.existsActiveBanByUserId(userId, now);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @CacheEvict(value = ACTIVE_BAN_CACHE, key = "#user.id")
    public Ban createSystemBan(User user, BanReason reason, LocalDateTime endDate) {
        log.info("Creating system ban for user {} with reason {} until {}", user.getId(), reason, endDate);
        Ban ban = Ban.builder()
                .id(AppUtil.generateUUID())
                .user(user)
                .bannedBy(null) // hệ thống tự ban, không phải admin
                .reason(reason)
                .description("Automatic system ban for reason: " + reason + ". The lock time is 20 minutes.")
                .startDate(LocalDateTime.now())
                .endDate(endDate)
                .build();
        pushBanNotification(user.getId(), "You have been banned for reason: " + reason + ". The lock time is 20 minutes.", null);
        return banRepository.save(ban);
    }

    @Cacheable(value = ACTIVE_BAN_CACHE, key = "#userId")
    public Optional<Ban> findActiveBanForUser(UUID userId) {
        return banRepository.findActiveBanForUser(userId, LocalDateTime.now());
    }

    private void pushBanNotification(UUID bannedId, String reason, UUID actorId) {
        notificationService.createNotification(NotificationRequest.builder()
                .recipient(bannedId)
                .content(reason)
                .actor(actorId)
                .type(NotificationType.SYSTEM)
                .build());
    }
}