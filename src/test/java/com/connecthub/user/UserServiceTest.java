package com.connecthub.user;

import com.connecthub.common.dto.response.CursorResponse;
import com.connecthub.common.util.AppUtil;
import com.connecthub.modules.features.social.entity.Follow;
import com.connecthub.modules.features.social.repository.FollowRepository;
import com.connecthub.modules.features.user.dto.request.UserStatusRequest;
import com.connecthub.modules.features.user.dto.request.UserUpdateRequest;
import com.connecthub.modules.features.user.dto.response.UserResponse;
import com.connecthub.modules.features.user.dto.response.UserStatsResponse;
import com.connecthub.modules.features.user.dto.response.UserSummaryResponse;
import com.connecthub.modules.features.user.entity.User;
import com.connecthub.modules.features.user.enums.UserStatus;
import com.connecthub.modules.features.user.exception.DuplicatePhoneNumberException;
import com.connecthub.modules.features.user.exception.UserNotFoundException;
import com.connecthub.modules.features.user.mapper.UserMapper;
import com.connecthub.modules.features.user.repository.UserRepository;
import com.connecthub.modules.features.user.service.UserService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private FollowRepository followRepository;

    @InjectMocks
    private UserService userService;

    private MockedStatic<AppUtil> mockedAppUtil;
    private final String MOCK_USERNAME = "test_user";

    @BeforeEach
    void setUp() {
        mockedAppUtil = Mockito.mockStatic(AppUtil.class);
        mockedAppUtil.when(AppUtil::usernameFromAuthentication).thenReturn(MOCK_USERNAME);
        mockedAppUtil.when(AppUtil::generateUUID).thenReturn(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    }

    @AfterEach
    void tearDown() {
        mockedAppUtil.close();
    }

    @Nested
    @DisplayName("getUserById")
    class GetUserByIdTest {
        @Test
        void shouldReturnMappedUserResponse() {
            UUID userId = UUID.randomUUID();
            User user = User.builder()
                    .id(userId)
                    .username("user1")
                    .email("user1@gmail.com")
                    .fullName("User One")
                    .phoneNumber("+84900000001")
                    .build();
            UserResponse response = new UserResponse();

            Mockito.when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            Mockito.when(userMapper.toUserResponse(user)).thenReturn(response);

            UserResponse result = userService.getUserById(userId);

            Assertions.assertSame(response, result);
            Mockito.verify(userRepository).findById(userId);
            Mockito.verify(userMapper).toUserResponse(user);
        }

        @Test
        void shouldThrowWhenUserNotFound() {
            UUID userId = UUID.randomUUID();
            Mockito.when(userRepository.findById(userId)).thenReturn(Optional.empty());

            Assertions.assertThrows(UserNotFoundException.class, () -> userService.getUserById(userId));
            Mockito.verify(userRepository).findById(userId);
            Mockito.verifyNoInteractions(userMapper);
        }
    }

    @Nested
    @DisplayName("updateUser")
    class UpdateUserTest {
        @Test
        void shouldUpdateCurrentUserWithMapStructAndSave() {
            UUID userId = UUID.randomUUID();
            User currentUser = User.builder()
                    .id(userId)
                    .username(MOCK_USERNAME)
                    .email("old@gmail.com")
                    .fullName("Old Name")
                    .phoneNumber("+84900000001")
                    .avatarUrl("old-avatar")
                    .bio("old bio")
                    .status(UserStatus.ACTIVE)
                    .build();

            UserUpdateRequest request = UserUpdateRequest.builder()
                    .fullName("New Name")
                    .phoneNumber("+84900000002")
                    .avatarUrl("new-avatar")
                    .bio("new bio")
                    .build();

            User savedUser = User.builder()
                    .id(userId)
                    .username(MOCK_USERNAME)
                    .email("old@gmail.com")
                    .fullName("New Name")
                    .phoneNumber("+84900000002")
                    .avatarUrl("new-avatar")
                    .bio("new bio")
                    .status(UserStatus.ACTIVE)
                    .build();

            UserResponse response = new UserResponse();

            Mockito.when(userRepository.findByUsername(MOCK_USERNAME)).thenReturn(Optional.of(currentUser));
            Mockito.when(userRepository.existsByPhoneNumberAndIdNot("+84900000002", userId)).thenReturn(false);
            Mockito.doNothing().when(userMapper).updateUserFromRequest(request, currentUser);
            Mockito.when(userRepository.save(currentUser)).thenReturn(savedUser);
            Mockito.when(userMapper.toUserResponse(savedUser)).thenReturn(response);

            UserResponse result = userService.updateUser(request);

            Assertions.assertSame(response, result);
            Mockito.verify(userRepository).findByUsername(MOCK_USERNAME);
            Mockito.verify(userRepository).existsByPhoneNumberAndIdNot("+84900000002", userId);
            Mockito.verify(userMapper).updateUserFromRequest(request, currentUser);
            Mockito.verify(userRepository).save(currentUser);
            Mockito.verify(userMapper).toUserResponse(savedUser);
        }

        @Test
        void shouldThrowDuplicatePhoneWhenPhoneExists() {
            UUID userId = UUID.randomUUID();
            User currentUser = User.builder()
                    .id(userId)
                    .username(MOCK_USERNAME)
                    .phoneNumber("+84900000001")
                    .build();
            UserUpdateRequest request = UserUpdateRequest.builder()
                    .phoneNumber("+84900000002")
                    .build();

            Mockito.when(userRepository.findByUsername(MOCK_USERNAME)).thenReturn(Optional.of(currentUser));
            Mockito.when(userRepository.existsByPhoneNumberAndIdNot("+84900000002", userId)).thenReturn(true);

            Assertions.assertThrows(DuplicatePhoneNumberException.class, () -> userService.updateUser(request));

            Mockito.verify(userRepository).findByUsername(MOCK_USERNAME);
            Mockito.verify(userRepository).existsByPhoneNumberAndIdNot("+84900000002", userId);
            Mockito.verify(userMapper, Mockito.never()).updateUserFromRequest(ArgumentMatchers.any(), ArgumentMatchers.any());
            Mockito.verify(userRepository, Mockito.never()).save(ArgumentMatchers.any());
        }
    }

    @Nested
    @DisplayName("changeStatus")
    class ChangeStatusTest {
        @Test
        void shouldUpdateCurrentUserStatus() {
            UUID userId = UUID.randomUUID();
            User currentUser = User.builder()
                    .id(userId)
                    .username(MOCK_USERNAME)
                    .status(UserStatus.ACTIVE)
                    .isActive(true)
                    .isLocked(false)
                    .build();
            UserStatusRequest request = UserStatusRequest.builder()
                    .status(UserStatus.BANNED)
                    .build();

            Mockito.when(userRepository.findByUsername(MOCK_USERNAME)).thenReturn(Optional.of(currentUser));
            Mockito.when(userRepository.save(currentUser)).thenReturn(currentUser);

            userService.changeStatus(request);

            Assertions.assertEquals(UserStatus.BANNED, currentUser.getStatus());
            Assertions.assertFalse(currentUser.isActive());
            Assertions.assertTrue(currentUser.isLocked());
            Mockito.verify(userRepository).findByUsername(MOCK_USERNAME);
            Mockito.verify(userRepository).save(currentUser);
        }
    }

    @Nested
    @DisplayName("followUser / unfollowUser")
    class FollowActionsTest {
        @Test
        void followUser_shouldCreateFollowWhenNotExists() {
            UUID currentUserId = UUID.randomUUID();
            UUID targetUserId = UUID.randomUUID();

            User currentUser = User.builder().id(currentUserId).username(MOCK_USERNAME).build();
            User targetUser = User.builder().id(targetUserId).username("target_user").build();

            Mockito.when(userRepository.findByUsername(MOCK_USERNAME)).thenReturn(Optional.of(currentUser));
            Mockito.when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
            Mockito.when(followRepository.existsByFollowerIdAndFollowingId(currentUserId, targetUserId)).thenReturn(false);

            userService.followUser(targetUserId);

            Mockito.verify(followRepository).save(ArgumentMatchers.any(Follow.class));
        }

        @Test
        void unfollowUser_shouldDeleteFollow() {
            UUID currentUserId = UUID.randomUUID();
            UUID targetUserId = UUID.randomUUID();

            User currentUser = User.builder().id(currentUserId).username(MOCK_USERNAME).build();
            User targetUser = User.builder().id(targetUserId).username("target_user").build();

            Mockito.when(userRepository.findByUsername(MOCK_USERNAME)).thenReturn(Optional.of(currentUser));
            Mockito.when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));

            userService.unfollowUser(targetUserId);

            Mockito.verify(followRepository).deleteByFollowerIdAndFollowingId(currentUserId, targetUserId);
        }
    }

    @Nested
    @DisplayName("getStats")
    class GetStatsTest {
        @Test
        void shouldReturnStats() {
            UUID userId = UUID.randomUUID();
            User user = User.builder().id(userId).username("user1").build();

            Mockito.when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            Mockito.when(followRepository.countByFollowingId(userId)).thenReturn(7L);
            Mockito.when(followRepository.countByFollowerId(userId)).thenReturn(4L);

            UserStatsResponse response = userService.getStats(userId);

            Assertions.assertEquals(7L, response.getFollowersCount());
            Assertions.assertEquals(4L, response.getFollowingCount());
            Mockito.verify(userRepository).findById(userId);
        }
    }

    @Nested
    @DisplayName("getFollowers / getFollowing")
    class CursorPagingTest {
        @Test
        void getFollowers_shouldReturnPagedContent() {
            UUID userId = UUID.randomUUID();
            UUID cursor = UUID.randomUUID();
            User user = User.builder().id(userId).username("user1").build();
            User follower = User.builder().id(UUID.randomUUID()).username("follower").build();
            Follow follow = Follow.builder().id(UUID.randomUUID()).follower(follower).following(user).build();
            UserSummaryResponse summary = UserSummaryResponse.builder().build();

            Mockito.when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            Mockito.when(followRepository.findFollowers(ArgumentMatchers.eq(userId), ArgumentMatchers.eq(cursor), ArgumentMatchers.any(Limit.class))).thenReturn(List.of(follow));
            Mockito.when(userMapper.toUserSummaryResponse(follower)).thenReturn(summary);

            CursorResponse<UserSummaryResponse> response = userService.getFollowers(userId, cursor, 10);

            Assertions.assertEquals(1, response.getContent().size());
            Assertions.assertFalse(response.isHasNext());
            Assertions.assertNotNull(response.getNextCursor());
            Mockito.verify(userMapper).toUserSummaryResponse(follower);
        }

        @Test
        void getFollowing_shouldReturnPagedContent() {
            UUID userId = UUID.randomUUID();
            UUID cursor = UUID.randomUUID();
            User user = User.builder().id(userId).username("user1").build();
            User following = User.builder().id(UUID.randomUUID()).username("following").build();
            Follow follow = Follow.builder().id(UUID.randomUUID()).follower(user).following(following).build();
            UserSummaryResponse summary = UserSummaryResponse.builder().build();

            Mockito.when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            Mockito.when(followRepository.findFollowing(ArgumentMatchers.eq(userId), ArgumentMatchers.eq(cursor), ArgumentMatchers.any(Limit.class))).thenReturn(List.of(follow));
            Mockito.when(userMapper.toUserSummaryResponse(following)).thenReturn(summary);

            CursorResponse<UserSummaryResponse> response = userService.getFollowing(userId, cursor, 10);

            Assertions.assertEquals(1, response.getContent().size());
            Assertions.assertFalse(response.isHasNext());
            Assertions.assertNotNull(response.getNextCursor());
            Mockito.verify(userMapper).toUserSummaryResponse(following);
        }
    }
}

