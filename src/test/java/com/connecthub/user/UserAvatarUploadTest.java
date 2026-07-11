package com.connecthub.user;

import com.connecthub.common.dto.response.UploadMediaResponse;
import com.connecthub.common.enums.MediaType;
import com.connecthub.common.util.AppUtil;
import com.connecthub.modules.features.user.entity.User;
import com.connecthub.modules.features.user.mapper.UserMapper;
import com.connecthub.modules.features.user.repository.UserRepository;
import com.connecthub.modules.features.user.service.UserService;
import com.connecthub.common.service.MediaStorageService;
import com.connecthub.modules.features.social.repository.FollowRepository;
import com.connecthub.modules.features.notification.repository.NotificationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@ExtendWith(MockitoExtension.class)
class UserAvatarUploadTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private FollowRepository followRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private MediaStorageService mediaStorageService;

    @InjectMocks
    private UserService userService;

    private org.mockito.MockedStatic<AppUtil> mockedAppUtil;

    @BeforeEach
    void setUp() {
        mockedAppUtil = Mockito.mockStatic(AppUtil.class);
    }

    @AfterEach
    void tearDown() {
        mockedAppUtil.close();
    }

    @Test
    void uploadAvatar_success_deletesOldAvatar() throws Exception {
        UUID userId = UUID.randomUUID();
        mockedAppUtil.when(AppUtil::userIdFromAuthentication).thenReturn(userId);

        User currentUser = User.builder()
                .id(userId)
                .publicAvtId("old-public-id")
                .build();

        Mockito.when(userRepository.findById(userId)).thenReturn(Optional.of(currentUser));

        byte[] content = "imagecontent".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", content);

        UploadMediaResponse resp = UploadMediaResponse.builder()
                .url("https://cdn.example.com/new.png")
                .publicId("new-public-id")
                .mediaType(MediaType.IMAGE)
                .build();

        Mockito.when(mediaStorageService.uploadImage(ArgumentMatchers.any(), ArgumentMatchers.eq("avatars")))
                .thenReturn(CompletableFuture.completedFuture(resp));

        User savedUser = User.builder().id(userId).publicAvtId("new-public-id").avatarUrl(resp.getUrl()).build();
        Mockito.when(userRepository.save(ArgumentMatchers.any())).thenReturn(savedUser);
        Mockito.when(userMapper.toUserResponse(savedUser)).thenReturn(null);

        userService.uploadAvatar(file);

        Mockito.verify(mediaStorageService).uploadImage(ArgumentMatchers.any(), ArgumentMatchers.eq("avatars"));
        Mockito.verify(mediaStorageService).delete("old-public-id");
        Mockito.verify(userRepository).save(ArgumentMatchers.any());
    }

    @Test
    void uploadAvatar_success_noOldAvatar_noDelete() throws Exception {
        UUID userId = UUID.randomUUID();
        mockedAppUtil.when(AppUtil::userIdFromAuthentication).thenReturn(userId);

        User currentUser = User.builder()
                .id(userId)
                .publicAvtId(null)
                .build();

        Mockito.when(userRepository.findById(userId)).thenReturn(Optional.of(currentUser));

        byte[] content = "imagecontent".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", content);

        UploadMediaResponse resp = UploadMediaResponse.builder()
                .url("https://cdn.example.com/new.png")
                .publicId("new-public-id")
                .mediaType(MediaType.IMAGE)
                .build();

        Mockito.when(mediaStorageService.uploadImage(ArgumentMatchers.any(), ArgumentMatchers.eq("avatars")))
                .thenReturn(CompletableFuture.completedFuture(resp));

        User savedUser = User.builder().id(userId).publicAvtId("new-public-id").avatarUrl(resp.getUrl()).build();
        Mockito.when(userRepository.save(ArgumentMatchers.any())).thenReturn(savedUser);
        Mockito.when(userMapper.toUserResponse(savedUser)).thenReturn(null);

        userService.uploadAvatar(file);

        Mockito.verify(mediaStorageService).uploadImage(ArgumentMatchers.any(), ArgumentMatchers.eq("avatars"));
        Mockito.verify(mediaStorageService, Mockito.never()).delete(ArgumentMatchers.anyString());
        Mockito.verify(userRepository).save(ArgumentMatchers.any());
    }

    @Test
    void uploadAvatar_invalidType_throws() {
        UUID userId = UUID.randomUUID();
        mockedAppUtil.when(AppUtil::userIdFromAuthentication).thenReturn(userId);

        User currentUser = User.builder().id(userId).build();
        Mockito.when(userRepository.findById(userId)).thenReturn(Optional.of(currentUser));

        MockMultipartFile file = new MockMultipartFile("file", "data.txt", "text/plain", "hello".getBytes());

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> userService.uploadAvatar(file));
    }

    @Test
    void uploadAvatar_sizeExceeded_throws() {
        UUID userId = UUID.randomUUID();
        mockedAppUtil.when(AppUtil::userIdFromAuthentication).thenReturn(userId);

        User currentUser = User.builder().id(userId).build();
        Mockito.when(userRepository.findById(userId)).thenReturn(Optional.of(currentUser));

        // create large payload > 5MB
        byte[] large = new byte[6 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", large);

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> userService.uploadAvatar(file));
    }
}

