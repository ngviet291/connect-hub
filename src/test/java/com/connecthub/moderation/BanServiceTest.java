package com.connecthub.moderation;

import com.connecthub.common.dto.response.PagingResponse;
import com.connecthub.common.util.AppUtil;
import com.connecthub.modules.features.moderation.dto.request.ban.CreateBanRequest;
import com.connecthub.modules.features.moderation.dto.request.ban.UnbanRequest;
import com.connecthub.modules.features.moderation.dto.response.ban.BanResponse;
import com.connecthub.modules.features.moderation.entity.Ban;
import com.connecthub.modules.features.moderation.enums.BanReason;
import com.connecthub.modules.features.moderation.exception.ban.BanAlreadyInactiveException;
import com.connecthub.modules.features.moderation.exception.ban.BanNotFoundException;
import com.connecthub.modules.features.moderation.mapper.BanMapper;
import com.connecthub.modules.features.moderation.repository.BanRepository;
import com.connecthub.modules.features.moderation.service.BanService;
import com.connecthub.modules.features.moderation.exception.ban.UserAlreadyBannedException;
import com.connecthub.modules.features.user.entity.User;
import com.connecthub.modules.features.user.exception.UserNotFoundException;
import com.connecthub.modules.features.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BanService")
class BanServiceTest {

    @Mock
    private BanMapper banMapper;
    @Mock
    private BanRepository banRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BanService banService;

    private MockedStatic<AppUtil> appUtilMock;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ISSUER_ID = UUID.randomUUID();
    private static final UUID BAN_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        appUtilMock = mockStatic(AppUtil.class, Mockito.CALLS_REAL_METHODS);
    }

    @AfterEach
    void tearDown() {
        appUtilMock.close();
    }

    @Nested
    @DisplayName("createBan")
    class CreateBanTest {

        @Test
        @DisplayName("Tạo ban thành công khi user chưa bị ban")
        void shouldCreateBanSuccessfully_whenUserNotAlreadyBanned() {
            CreateBanRequest request = mock(CreateBanRequest.class);
            when(request.getUserId()).thenReturn(USER_ID);

            Ban mappedBan = new Ban();
            User bannedUser = User.builder().id(USER_ID).build();
            User issuer = User.builder().id(ISSUER_ID).build();
            Ban savedBan = Ban.builder().id(UUID.randomUUID()).user(bannedUser).bannedBy(issuer).build();
            BanResponse expectedResponse = mock(BanResponse.class);

            appUtilMock.when(AppUtil::userIdFromAuthentication).thenReturn(ISSUER_ID);
            appUtilMock.when(AppUtil::generateUUID).thenReturn(UUID.randomUUID());

            when(banMapper.toBan(request)).thenReturn(mappedBan);
            when(banRepository.existsActiveBanByUserId(eq(USER_ID), any(LocalDateTime.class)))
                    .thenReturn(false);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(bannedUser));
            when(userRepository.getReferenceById(ISSUER_ID)).thenReturn(issuer);
            when(banRepository.save(any(Ban.class))).thenReturn(savedBan);
            when(banMapper.toBanResponse(savedBan)).thenReturn(expectedResponse);

            BanResponse result = banService.createBan(request);

            assertThat(result).isEqualTo(expectedResponse);
            assertThat(mappedBan.getUser()).isEqualTo(bannedUser);
            assertThat(mappedBan.getBannedBy()).isEqualTo(issuer);
            verify(banRepository).save(mappedBan);
        }

        @Test
        @DisplayName("Ném exception khi user đã có ban active")
        void shouldThrowException_whenUserAlreadyBanned() {
            CreateBanRequest request = mock(CreateBanRequest.class);
            when(request.getUserId()).thenReturn(USER_ID);

            appUtilMock.when(AppUtil::userIdFromAuthentication).thenReturn(ISSUER_ID);
            when(banMapper.toBan(request)).thenReturn(new Ban());
            when(banRepository.existsActiveBanByUserId(eq(USER_ID), any(LocalDateTime.class)))
                    .thenReturn(true);

            assertThatThrownBy(() -> banService.createBan(request))
                    .isInstanceOf(UserAlreadyBannedException.class);

            verify(banRepository, never()).save(any());
            verify(userRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Ném exception khi user cần ban không tồn tại")
        void shouldThrowException_whenBannedUserNotFound() {
            CreateBanRequest request = mock(CreateBanRequest.class);
            when(request.getUserId()).thenReturn(USER_ID);

            appUtilMock.when(AppUtil::userIdFromAuthentication).thenReturn(ISSUER_ID);
            when(banMapper.toBan(request)).thenReturn(new Ban());
            when(banRepository.existsActiveBanByUserId(eq(USER_ID), any(LocalDateTime.class)))
                    .thenReturn(false);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> banService.createBan(request))
                    .isInstanceOf(UserNotFoundException.class);

            verify(banRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getAllBans")
    class GetAllBansTest {

        @Test
        @DisplayName("Gọi findAllActive khi active=true")
        void shouldCallFindAllActive_whenActiveTrue() {
            Pageable pageable = Pageable.ofSize(10);
            Page<Ban> page = new PageImpl<>(List.of(new Ban()));
            BanResponse response = mock(BanResponse.class);
            PagingResponse<BanResponse> expectedPagingResponse = mock(PagingResponse.class);

            when(banRepository.findAllActive(any(LocalDateTime.class), eq(pageable))).thenReturn(page);
            appUtilMock.when(() -> AppUtil.buildPagingResponse(eq(page), any()))
                    .thenReturn(expectedPagingResponse);

            PagingResponse<BanResponse> result = banService.getAllBans(pageable, true);

            assertThat(result).isEqualTo(expectedPagingResponse);
            verify(banRepository).findAllActive(any(LocalDateTime.class), eq(pageable));
            verify(banRepository, never()).findBansAll(any());
        }

        @Test
        @DisplayName("Gọi findBansAll khi active=false")
        void shouldCallFindBansAll_whenActiveFalse() {
            Pageable pageable = Pageable.ofSize(10);
            Page<Ban> page = new PageImpl<>(List.of(new Ban()));
            PagingResponse<BanResponse> expectedPagingResponse = mock(PagingResponse.class);

            when(banRepository.findBansAll(eq(pageable))).thenReturn(page);
            appUtilMock.when(() -> AppUtil.buildPagingResponse(eq(page), any()))
                    .thenReturn(expectedPagingResponse);
            // KHÔNG cần stub banMapper.toBanResponse vì buildPagingResponse đã bị mock hoàn toàn,
            // banMapper không bao giờ thực sự được gọi qua đường này

            PagingResponse<BanResponse> result = banService.getAllBans(pageable, false);

            assertThat(result).isEqualTo(expectedPagingResponse);
            verify(banRepository).findBansAll(pageable);
            verify(banRepository, never()).findAllActive(any(), any());
        }
    }

    @Nested
    @DisplayName("unbanUser")
    class UnbanUserTest {

        @Test
        @DisplayName("Unban thành công khi ban đang active")
        void shouldUnbanSuccessfully_whenBanIsActive() {
            UnbanRequest request = mock(UnbanRequest.class);
            when(request.getUnbanReason()).thenReturn("Đã xác minh khắc phục vi phạm");

            Ban activeBan = Ban.builder()
                    .id(BAN_ID)
                    .startDate(LocalDateTime.now().minusDays(1))
                    .endDate(LocalDateTime.now().plusDays(1))
                    .build();

            User admin = User.builder().id(ISSUER_ID).build();
            BanResponse expectedResponse = mock(BanResponse.class);

            when(banRepository.findById(BAN_ID)).thenReturn(Optional.of(activeBan));
            appUtilMock.when(AppUtil::userIdFromAuthentication).thenReturn(ISSUER_ID);
            when(userRepository.getReferenceById(ISSUER_ID)).thenReturn(admin);
            when(banRepository.save(activeBan)).thenReturn(activeBan);
            when(banMapper.toBanResponse(activeBan)).thenReturn(expectedResponse);

            BanResponse result = banService.unbanUser(BAN_ID, request);

            assertThat(result).isEqualTo(expectedResponse);
            assertThat(activeBan.getUnbannedBy()).isEqualTo(admin);
            assertThat(activeBan.getUnbannedAt()).isNotNull();
            assertThat(activeBan.getUnbanReason()).isEqualTo("Đã xác minh khắc phục vi phạm");
        }

        @Test
        @DisplayName("Ném exception khi ban không tồn tại")
        void shouldThrowException_whenBanNotFound() {
            UnbanRequest request = mock(UnbanRequest.class);
            when(banRepository.findById(BAN_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> banService.unbanUser(BAN_ID, request))
                    .isInstanceOf(BanNotFoundException.class);

            verify(banRepository, never()).save(any());
        }

        @Test
        @DisplayName("Ném exception khi ban đã hết hạn (không active)")
        void shouldThrowException_whenBanAlreadyExpired() {
            UnbanRequest request = mock(UnbanRequest.class);
            Ban expiredBan = Ban.builder()
                    .id(BAN_ID)
                    .startDate(LocalDateTime.now().minusDays(10))
                    .endDate(LocalDateTime.now().minusDays(1)) // đã hết hạn
                    .build();

            when(banRepository.findById(BAN_ID)).thenReturn(Optional.of(expiredBan));

            assertThatThrownBy(() -> banService.unbanUser(BAN_ID, request))
                    .isInstanceOf(BanAlreadyInactiveException.class);

            verify(banRepository, never()).save(any());
        }

        @Test
        @DisplayName("Ném exception khi ban đã được unban trước đó (REVOKED)")
        void shouldThrowException_whenBanAlreadyRevoked() {
            UnbanRequest request = mock(UnbanRequest.class);
            Ban revokedBan = Ban.builder()
                    .id(BAN_ID)
                    .startDate(LocalDateTime.now().minusDays(2))
                    .endDate(LocalDateTime.now().plusDays(5))
                    .unbannedAt(LocalDateTime.now().minusHours(1)) // đã từng unban
                    .build();

            when(banRepository.findById(BAN_ID)).thenReturn(Optional.of(revokedBan));

            assertThatThrownBy(() -> banService.unbanUser(BAN_ID, request))
                    .isInstanceOf(BanAlreadyInactiveException.class);

            verify(banRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("isUserBanned")
    class IsUserBannedTest {

        @Test
        @DisplayName("Trả về true khi user có ban active")
        void shouldReturnTrue_whenUserHasActiveBan() {
            when(banRepository.existsActiveBanByUserId(eq(USER_ID), any(LocalDateTime.class)))
                    .thenReturn(true);

            boolean result = banService.isUserBanned(USER_ID);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Trả về false khi user không có ban active")
        void shouldReturnFalse_whenUserHasNoActiveBan() {
            when(banRepository.existsActiveBanByUserId(eq(USER_ID), any(LocalDateTime.class)))
                    .thenReturn(false);

            boolean result = banService.isUserBanned(USER_ID);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("createSystemBan")
    class CreateSystemBanTest {

        @Test
        @DisplayName("Tạo system ban với bannedBy là null")
        void shouldCreateSystemBan_withNullBannedBy() {
            User user = User.builder().id(USER_ID).build();
            LocalDateTime endDate = LocalDateTime.now().plusMinutes(20);

            appUtilMock.when(AppUtil::generateUUID).thenReturn(UUID.randomUUID());
            when(banRepository.save(any(Ban.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Ban result = banService.createSystemBan(user, BanReason.TOO_MANY_FAILED_LOGIN_ATTEMPTS, endDate);

            assertThat(result.getBannedBy()).isNull();
            assertThat(result.getUser()).isEqualTo(user);
            assertThat(result.getReason()).isEqualTo(BanReason.TOO_MANY_FAILED_LOGIN_ATTEMPTS);
            assertThat(result.getEndDate()).isEqualTo(endDate);
            assertThat(result.getDescription()).contains("TOO_MANY_FAILED_LOGIN_ATTEMPTS");

            verify(banRepository).save(any(Ban.class));
        }
    }

    @Nested
    @DisplayName("findActiveBanForUser")
    class FindActiveBanForUserTest {

        @Test
        @DisplayName("Trả về Optional có giá trị khi tồn tại ban active")
        void shouldReturnBan_whenActiveBanExists() {
            Ban activeBan = Ban.builder().id(BAN_ID).build();
            when(banRepository.findActiveBanForUser(eq(USER_ID), any(LocalDateTime.class)))
                    .thenReturn(Optional.of(activeBan));

            Optional<Ban> result = banService.findActiveBanForUser(USER_ID);

            assertThat(result).isPresent().contains(activeBan);
        }

        @Test
        @DisplayName("Trả về Optional.empty khi không có ban active")
        void shouldReturnEmpty_whenNoActiveBan() {
            when(banRepository.findActiveBanForUser(eq(USER_ID), any(LocalDateTime.class)))
                    .thenReturn(Optional.empty());

            Optional<Ban> result = banService.findActiveBanForUser(USER_ID);

            assertThat(result).isEmpty();
        }
    }
}