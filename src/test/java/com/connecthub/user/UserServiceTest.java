package com.connecthub.user;

import com.connecthub.common.dto.response.CursorResponse;
import com.connecthub.common.util.AppUtil;
import com.connecthub.modules.features.notification.entity.Notification;
import com.connecthub.modules.features.notification.enums.NotificationType;
import com.connecthub.modules.features.notification.repository.NotificationRepository;
import com.connecthub.modules.features.social.dto.FollowStats;
import com.connecthub.modules.features.social.entity.Follow;
import com.connecthub.modules.features.social.repository.FollowRepository;
import com.connecthub.modules.features.user.dto.request.UserStatusRequest;
import com.connecthub.modules.features.user.dto.request.UserUpdateRequest;
import com.connecthub.modules.features.user.dto.response.FollowResponse;
import com.connecthub.modules.features.user.dto.response.UserResponse;
import com.connecthub.modules.features.user.dto.response.UserSummaryResponse;
import com.connecthub.modules.features.user.entity.User;
import com.connecthub.modules.features.user.enums.UserStatus;
import com.connecthub.modules.features.user.exception.ConflictUserException;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private FollowRepository followRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private UserService userService;

    private MockedStatic<AppUtil> mockedAppUtil;
    
    private final UUID CURRENT_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID GENERATED_UUID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @BeforeEach
    void setUp() {
        mockedAppUtil = mockStatic(AppUtil.class);
        mockedAppUtil.when(AppUtil::userIdFormAuthentication).thenReturn(CURRENT_USER_ID);
        mockedAppUtil.when(AppUtil::generateUUID).thenReturn(GENERATED_UUID);
    }

    @AfterEach
    void tearDown() {
        mockedAppUtil.close();
    }

    @Nested
    @DisplayName("lockUser")
    class LockUserTest {
        @Test
        void lockUser_success() {
            String username = "testuser";
            User user = User.builder()
                    .id(UUID.randomUUID())
                    .username(username)
                    .build();

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
            when(userRepository.save(user)).thenReturn(user);

            userService.lockUser(username);

            assertFalse(user.isActive());
            assertTrue(user.isLocked());
            verify(userRepository).findByUsername(username);
            verify(userRepository).save(user);
        }

        @Test
        void lockUser_notFound_throwsException() {
            String username = "nonexistent";
            when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class, () -> userService.lockUser(username));
            verify(userRepository).findByUsername(username);
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getUserById")
    class GetUserByIdTest {
        @Test
        void getUserById_admin_success() {
            UUID id = UUID.randomUUID();
            User user = User.builder().id(id).build();
            UserResponse response = new UserResponse();

            when(userRepository.findById(id)).thenReturn(Optional.of(user));
            when(userMapper.toUserResponse(user)).thenReturn(response);

            UserResponse result = userService.getUserById(id);

            assertSame(response, result);
            verify(userRepository).findById(id);
            verify(userMapper).toUserResponse(user);
        }

        @Test
        void getUserById_admin_notFound_throwsException() {
            UUID id = UUID.randomUUID();
            when(userRepository.findById(id)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class, () -> userService.getUserById(id));
            verify(userRepository).findById(id);
            verify(userMapper, never()).toUserResponse(any());
        }

        @Test
        void getUserById_user_success() {
            User currentUser = User.builder().id(CURRENT_USER_ID).build();
            UserResponse response = new UserResponse();

            when(userRepository.findById(CURRENT_USER_ID)).thenReturn(Optional.of(currentUser));
            when(userMapper.toUserResponse(currentUser)).thenReturn(response);

            UserResponse result = userService.getUserById();

            assertSame(response, result);
            verify(userRepository).findById(CURRENT_USER_ID);
            verify(userMapper).toUserResponse(currentUser);
        }

        @Test
        void getUserById_user_notFound_throwsException() {
            when(userRepository.findById(CURRENT_USER_ID)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class, () -> userService.getUserById());
            verify(userRepository).findById(CURRENT_USER_ID);
        }
    }

    @Nested
    @DisplayName("getFollowers")
    class GetFollowersTest {
        @Test
        void getFollowers_admin_success_withNextPage() {
            UUID userId = UUID.randomUUID();
            UUID cursor = UUID.randomUUID();
            int size = 2;

            User user = User.builder().id(userId).build();
            User follower1 = User.builder().id(UUID.randomUUID()).build();
            User follower2 = User.builder().id(UUID.randomUUID()).build();
            User follower3 = User.builder().id(UUID.randomUUID()).build();

            Follow f1 = Follow.builder().id(UUID.randomUUID()).follower(follower1).build();
            Follow f2 = Follow.builder().id(UUID.randomUUID()).follower(follower2).build();
            Follow f3 = Follow.builder().id(UUID.randomUUID()).follower(follower3).build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(followRepository.findFollowersOptimized(userId, cursor, Limit.of(size + 1)))
                    .thenReturn(List.of(f1, f2, f3));

            UserSummaryResponse summary1 = new UserSummaryResponse();
            UserSummaryResponse summary2 = new UserSummaryResponse();
            when(userMapper.toUserSummaryResponse(follower1)).thenReturn(summary1);
            when(userMapper.toUserSummaryResponse(follower2)).thenReturn(summary2);

            CursorResponse<UserSummaryResponse> response = userService.getFollowers(userId, cursor, size);

            assertTrue(response.isHasNext());
            assertEquals(2, response.getContent().size());
            assertEquals(f2.getId().toString(), response.getNextCursor());
            verify(userRepository).findById(userId);
            verify(followRepository).findFollowersOptimized(userId, cursor, Limit.of(size + 1));
        }

        @Test
        void getFollowers_user_success_noNextPage() {
            UUID cursor = UUID.randomUUID();
            int size = 5;

            User user = User.builder().id(CURRENT_USER_ID).build();
            User follower = User.builder().id(UUID.randomUUID()).build();
            Follow f = Follow.builder().id(UUID.randomUUID()).follower(follower).build();

            when(userRepository.findById(CURRENT_USER_ID)).thenReturn(Optional.of(user));
            when(followRepository.findFollowersOptimized(CURRENT_USER_ID, cursor, Limit.of(size + 1)))
                    .thenReturn(List.of(f));

            UserSummaryResponse summary = new UserSummaryResponse();
            when(userMapper.toUserSummaryResponse(follower)).thenReturn(summary);

            CursorResponse<UserSummaryResponse> response = userService.getFollowers(cursor, size);

            assertFalse(response.isHasNext());
            assertEquals(1, response.getContent().size());
            assertEquals(f.getId().toString(), response.getNextCursor());
            verify(userRepository).findById(CURRENT_USER_ID);
        }
    }

    @Nested
    @DisplayName("getFollowing")
    class GetFollowingTest {
        @Test
        void getFollowing_admin_success() {
            UUID userId = UUID.randomUUID();
            UUID cursor = UUID.randomUUID();
            int size = 5;

            User user = User.builder().id(userId).build();
            User followingUser = User.builder().id(UUID.randomUUID()).build();
            Follow f = Follow.builder().id(UUID.randomUUID()).following(followingUser).build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(followRepository.findFollowingOptimized(userId, cursor, Limit.of(size + 1)))
                    .thenReturn(List.of(f));

            UserSummaryResponse summary = new UserSummaryResponse();
            when(userMapper.toUserSummaryResponse(followingUser)).thenReturn(summary);

            CursorResponse<UserSummaryResponse> response = userService.getFollowing(userId, cursor, size);

            assertFalse(response.isHasNext());
            assertEquals(1, response.getContent().size());
            assertEquals(f.getId().toString(), response.getNextCursor());
            verify(userRepository).findById(userId);
            verify(followRepository).findFollowingOptimized(userId, cursor, Limit.of(size + 1));
        }

        @Test
        void getFollowing_user_success() {
            UUID cursor = UUID.randomUUID();
            int size = 5;

            User user = User.builder().id(CURRENT_USER_ID).build();
            User followingUser = User.builder().id(UUID.randomUUID()).build();
            Follow f = Follow.builder().id(UUID.randomUUID()).following(followingUser).build();

            when(userRepository.findById(CURRENT_USER_ID)).thenReturn(Optional.of(user));
            when(followRepository.findFollowingOptimized(CURRENT_USER_ID, cursor, Limit.of(size + 1)))
                    .thenReturn(List.of(f));

            UserSummaryResponse summary = new UserSummaryResponse();
            when(userMapper.toUserSummaryResponse(followingUser)).thenReturn(summary);

            CursorResponse<UserSummaryResponse> response = userService.getFollowing(cursor, size);

            assertFalse(response.isHasNext());
            assertEquals(1, response.getContent().size());
            verify(userRepository).findById(CURRENT_USER_ID);
        }
    }

    @Nested
    @DisplayName("followUser")
    class FollowUserTest {
        @Test
        void followUser_self_throwsConflictException() {
            assertThrows(ConflictUserException.class, () -> userService.followUser(CURRENT_USER_ID));
        }

        @Test
        void followUser_alreadyFollowing_returnsFailureResponse() {
            UUID targetId = UUID.randomUUID();
            when(followRepository.existsByFollowerIdAndFollowingId(CURRENT_USER_ID, targetId)).thenReturn(true);

            FollowResponse response = userService.followUser(targetId);

            assertFalse(response.isSuccess());
            assertEquals("Already following this user", response.getMessage());
            assertEquals(CURRENT_USER_ID, response.getFollowerId());
            assertEquals(targetId, response.getFollowingId());
            verify(followRepository).existsByFollowerIdAndFollowingId(CURRENT_USER_ID, targetId);
            verifyNoInteractions(notificationRepository);
            verify(followRepository, never()).save(any());
        }

        @Test
        void followUser_success() {
            UUID targetId = UUID.randomUUID();
            User currentUser = User.builder().id(CURRENT_USER_ID).fullName("Current User").build();
            User targetUser = User.builder().id(targetId).fullName("Target User").build();

            when(followRepository.existsByFollowerIdAndFollowingId(CURRENT_USER_ID, targetId)).thenReturn(false);
            when(userRepository.findById(CURRENT_USER_ID)).thenReturn(Optional.of(currentUser));
            when(userRepository.findById(targetId)).thenReturn(Optional.of(targetUser));

            FollowResponse response = userService.followUser(targetId);

            assertTrue(response.isSuccess());
            assertEquals("Successfully followed user", response.getMessage());
            assertEquals(CURRENT_USER_ID, response.getFollowerId());
            assertEquals(targetId, response.getFollowingId());

            ArgumentCaptor<Follow> followCaptor = ArgumentCaptor.forClass(Follow.class);
            verify(followRepository).save(followCaptor.capture());
            Follow savedFollow = followCaptor.getValue();
            assertEquals(GENERATED_UUID, savedFollow.getId());
            assertEquals(currentUser, savedFollow.getFollower());
            assertEquals(targetUser, savedFollow.getFollowing());

            ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).save(notificationCaptor.capture());
            Notification savedNotification = notificationCaptor.getValue();
            assertEquals(GENERATED_UUID, savedNotification.getId());
            assertEquals(targetUser, savedNotification.getRecipient());
            assertEquals(currentUser, savedNotification.getActor());
            assertEquals(NotificationType.FOLLOW, savedNotification.getType());
            assertEquals("Current User started following you", savedNotification.getContent());
            assertFalse(savedNotification.isRead());
        }
    }

    @Nested
    @DisplayName("unfollowUser")
    class UnfollowUserTest {
        @Test
        void unfollowUser_self_throwsConflictException() {
            assertThrows(ConflictUserException.class, () -> userService.unfollowUser(CURRENT_USER_ID));
        }

        @Test
        void unfollowUser_notFollowing_throwsUserNotFoundException() {
            UUID targetId = UUID.randomUUID();
            when(followRepository.existsByFollowerIdAndFollowingId(CURRENT_USER_ID, targetId)).thenReturn(false);

            assertThrows(UserNotFoundException.class, () -> userService.unfollowUser(targetId));
            verify(followRepository).existsByFollowerIdAndFollowingId(CURRENT_USER_ID, targetId);
            verify(followRepository, never()).deleteByFollowerIdAndFollowingId(any(), any());
        }

        @Test
        void unfollowUser_success() {
            UUID targetId = UUID.randomUUID();
            when(followRepository.existsByFollowerIdAndFollowingId(CURRENT_USER_ID, targetId)).thenReturn(true);

            FollowResponse response = userService.unfollowUser(targetId);

            assertTrue(response.isSuccess());
            assertEquals("Successfully unfollowed user", response.getMessage());
            assertEquals(CURRENT_USER_ID, response.getFollowerId());
            assertEquals(targetId, response.getFollowingId());
            verify(followRepository).deleteByFollowerIdAndFollowingId(CURRENT_USER_ID, targetId);
        }
    }

    @Nested
    @DisplayName("updateUser")
    class UpdateUserTest {
        @Test
        void updateUser_success() {
            User currentUser = User.builder()
                    .id(CURRENT_USER_ID)
                    .fullName("Old Name")
                    .phoneNumber("+84900000001")
                    .build();
            UserUpdateRequest request = UserUpdateRequest.builder()
                    .fullName("New Name")
                    .phoneNumber("+84900000002")
                    .build();
            User savedUser = User.builder()
                    .id(CURRENT_USER_ID)
                    .fullName("New Name")
                    .phoneNumber("+84900000002")
                    .build();
            UserResponse response = new UserResponse();

            when(userRepository.findById(CURRENT_USER_ID)).thenReturn(Optional.of(currentUser));
            when(userRepository.existsByPhoneNumberAndIdNot("+84900000002", CURRENT_USER_ID)).thenReturn(false);
            doNothing().when(userMapper).updateUserFromRequest(request, currentUser);
            when(userRepository.save(currentUser)).thenReturn(savedUser);
            when(userMapper.toUserResponse(savedUser)).thenReturn(response);

            UserResponse result = userService.updateUser(request);

            assertSame(response, result);
            verify(userRepository).findById(CURRENT_USER_ID);
            verify(userRepository).existsByPhoneNumberAndIdNot("+84900000002", CURRENT_USER_ID);
            verify(userMapper).updateUserFromRequest(request, currentUser);
            verify(userRepository).save(currentUser);
            verify(userMapper).toUserResponse(savedUser);
        }

        @Test
        void updateUser_duplicatePhoneNumber_throwsException() {
            User currentUser = User.builder()
                    .id(CURRENT_USER_ID)
                    .phoneNumber("+84900000001")
                    .build();
            UserUpdateRequest request = UserUpdateRequest.builder()
                    .phoneNumber("+84900000002")
                    .build();

            when(userRepository.findById(CURRENT_USER_ID)).thenReturn(Optional.of(currentUser));
            when(userRepository.existsByPhoneNumberAndIdNot("+84900000002", CURRENT_USER_ID)).thenReturn(true);

            assertThrows(DuplicatePhoneNumberException.class, () -> userService.updateUser(request));
            verify(userRepository).findById(CURRENT_USER_ID);
            verify(userRepository).existsByPhoneNumberAndIdNot("+84900000002", CURRENT_USER_ID);
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("changeStatus")
    class ChangeStatusTest {
        @Test
        void changeStatus_active() {
            User currentUser = User.builder().id(CURRENT_USER_ID).status(UserStatus.INACTIVE).isActive(false).isLocked(false).build();
            UserStatusRequest request = UserStatusRequest.builder().status(UserStatus.ACTIVE).build();

            when(userRepository.findById(CURRENT_USER_ID)).thenReturn(Optional.of(currentUser));

            userService.changeStatus(request);

            assertEquals(UserStatus.ACTIVE, currentUser.getStatus());
            assertTrue(currentUser.isActive());
            assertFalse(currentUser.isLocked());
            verify(userRepository).findById(CURRENT_USER_ID);
        }

        @Test
        void changeStatus_inactive() {
            User currentUser = User.builder().id(CURRENT_USER_ID).status(UserStatus.ACTIVE).isActive(true).isLocked(false).build();
            UserStatusRequest request = UserStatusRequest.builder().status(UserStatus.INACTIVE).build();

            when(userRepository.findById(CURRENT_USER_ID)).thenReturn(Optional.of(currentUser));

            userService.changeStatus(request);

            assertEquals(UserStatus.INACTIVE, currentUser.getStatus());
            assertFalse(currentUser.isActive());
            assertFalse(currentUser.isLocked());
        }

        @Test
        void changeStatus_banned() {
            User currentUser = User.builder().id(CURRENT_USER_ID).status(UserStatus.ACTIVE).isActive(true).isLocked(false).build();
            UserStatusRequest request = UserStatusRequest.builder().status(UserStatus.BANNED).build();

            when(userRepository.findById(CURRENT_USER_ID)).thenReturn(Optional.of(currentUser));

            userService.changeStatus(request);

            assertEquals(UserStatus.BANNED, currentUser.getStatus());
            assertFalse(currentUser.isActive());
            assertTrue(currentUser.isLocked());
        }
    }

    @Nested
    @DisplayName("getStats")
    class GetStatsTest {
        @Test
        void getStats_admin_success() {
            UUID userId = UUID.randomUUID();
            FollowStats expectedStats = new FollowStats(10L, 5L);

            when(userRepository.existsById(userId)).thenReturn(true);
            when(userRepository.countFollowStats(userId)).thenReturn(expectedStats);

            FollowStats result = userService.getStats(userId);

            assertSame(expectedStats, result);
            verify(userRepository).existsById(userId);
            verify(userRepository).countFollowStats(userId);
        }

        @Test
        void getStats_admin_notFound_throwsException() {
            UUID userId = UUID.randomUUID();
            when(userRepository.existsById(userId)).thenReturn(false);

            assertThrows(UserNotFoundException.class, () -> userService.getStats(userId));
            verify(userRepository).existsById(userId);
            verify(userRepository, never()).countFollowStats(any());
        }

        @Test
        void getStats_admin_nullStats_returnsZeroStats() {
            UUID userId = UUID.randomUUID();
            when(userRepository.existsById(userId)).thenReturn(true);
            when(userRepository.countFollowStats(userId)).thenReturn(null);

            FollowStats result = userService.getStats(userId);

            assertNotNull(result);
            assertEquals(0L, result.getFollowersCount());
            assertEquals(0L, result.getFollowingCount());
        }

        @Test
        void getStats_user_success() {
            FollowStats expectedStats = new FollowStats(3L, 2L);

            when(userRepository.existsById(CURRENT_USER_ID)).thenReturn(true);
            when(userRepository.countFollowStats(CURRENT_USER_ID)).thenReturn(expectedStats);

            FollowStats result = userService.getStats();

            assertSame(expectedStats, result);
            verify(userRepository).existsById(CURRENT_USER_ID);
        }
    }
}
