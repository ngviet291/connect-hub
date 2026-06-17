package com.connecthub.notification;

import com.connecthub.common.dto.response.CursorResponse;
import com.connecthub.common.util.AppUtil;
import com.connecthub.modules.features.notification.dto.response.NotificationResponse;
import com.connecthub.modules.features.notification.dto.response.NotificationUnreadResponse;
import com.connecthub.modules.features.notification.entity.Notification;
import com.connecthub.modules.features.notification.exception.NotificationNotFoundException;
import com.connecthub.modules.features.notification.mapper.NotificationMapper;
import com.connecthub.modules.features.notification.repository.NotificationRepository;
import com.connecthub.modules.features.notification.service.NotificationService;
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
import org.springframework.data.domain.Limit;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private NotificationService notificationService;

    private MockedStatic<AppUtil> mockedAppUtil;
    private final String MOCK_USERNAME = "test_user";

    @BeforeEach
    void setUp() {
        // Mock static AppUtil để luôn trả về MOCK_USERNAME khi test chạy
        mockedAppUtil = Mockito.mockStatic(AppUtil.class);
        mockedAppUtil.when(AppUtil::usernameFromAuthentication).thenReturn(MOCK_USERNAME);
    }

    @AfterEach
    void tearDown() {
        // Cần đóng mock static sau mỗi test case để tránh ảnh hưởng test khác
        mockedAppUtil.close();
    }

    // ==========================================
    // TEST CASES FOR read(UUID id)
    // ==========================================
    @Nested
    @DisplayName("Test hàm read(id)")
    class ReadTest {

        @Test
        @DisplayName("Thành công - Đánh dấu đã đọc khi tìm thấy thông báo")
        void read_Success() {
            UUID notificationId = UUID.randomUUID();
            Notification notification = new Notification();
            notification.setId(notificationId);
            notification.setRead(false);

            when(notificationRepository.findByIdAndRecipientUsername(notificationId, MOCK_USERNAME))
                    .thenReturn(Optional.of(notification));

            // Call
            notificationService.read(notificationId);

            // Assert & Verify
            assertTrue(notification.isRead());
            verify(notificationRepository, times(1))
                    .findByIdAndRecipientUsername(notificationId, MOCK_USERNAME);
        }

        @Test
        @DisplayName("Thất bại - Ném NotificationNotFoundException khi không tìm thấy thông báo")
        void read_NotFound_ThrowsException() {
            UUID notificationId = UUID.randomUUID();

            when(notificationRepository.findByIdAndRecipientUsername(notificationId, MOCK_USERNAME))
                    .thenReturn(Optional.empty());

            // Call & Assert
            assertThrows(NotificationNotFoundException.class, () -> notificationService.read(notificationId));
            verify(notificationRepository, times(1))
                    .findByIdAndRecipientUsername(notificationId, MOCK_USERNAME);
        }
    }

    // ==========================================
    // TEST CASES FOR readAll()
    // ==========================================
    @Nested
    @DisplayName("Test hàm readAll()")
    class ReadAllTest {

        @Test
        @DisplayName("Thành công - Gọi xuống repository để cập nhật tất cả")
        void readAll_Success() {
            // Call
            notificationService.readAll();

            // Verify
            verify(notificationRepository, times(1))
                    .markAsReadAllByIdAndRecipientUsername(MOCK_USERNAME);
        }
    }

    // ==========================================
    // TEST CASES FOR countUnread()
    // ==========================================
    @Nested
    @DisplayName("Test hàm countUnread()")
    class CountUnreadTest {

        @Test
        @DisplayName("Thành công - Trả về số lượng thông báo chưa đọc đúng")
        void countUnread_Success() {
            long expectedCount = 5L;
            when(notificationRepository.countUnreadByRecipientUsername(MOCK_USERNAME)).thenReturn(expectedCount);

            // Call
            NotificationUnreadResponse response = notificationService.countUnread();

            // Assert
            assertNotNull(response);
            assertEquals(expectedCount, response.getUnreadCount());
            verify(notificationRepository, times(1)).countUnreadByRecipientUsername(MOCK_USERNAME);
        }
    }

    // ==========================================
    // TEST CASES FOR getNotification(cursor, size)
    // ==========================================
    @Nested
    @DisplayName("Test hàm getNotification(cursor, size) - Cursor-based Pagination")
    class GetNotificationTest {

        @Test
        @DisplayName("Trường hợp 1 - Danh sách trống (Empty List)")
        void getNotification_EmptyList() {
            UUID cursor = UUID.randomUUID();
            int size = 5;

            // Mock DB trả về rỗng
            when(notificationRepository.findByRecipientUsername(eq(MOCK_USERNAME), eq(cursor), any(Limit.class)))
                    .thenReturn(Collections.emptyList());

            // Call
            CursorResponse<NotificationResponse> response = notificationService.getNotification(cursor, size);

            // Assert
            assertNotNull(response);
            assertTrue(response.getContent().isEmpty());
            assertNull(response.getNextCursor());
            assertFalse(response.isHasNext());
        }

        @Test
        @DisplayName("Trường hợp 2 - Số lượng phần tử ít hơn hoặc bằng size (hasNext = false)")
        void getNotification_HasNextFalse() {
            UUID cursor = UUID.randomUUID();
            int size = 2;

            Notification n1 = Notification.builder().id(UUID.randomUUID()).build();
            Notification n2 = Notification.builder().id(UUID.randomUUID()).build();
            List<Notification> mockDbResult = List.of(n1, n2); // đúng bằng size = 2

            NotificationResponse res1 = new NotificationResponse();
            NotificationResponse res2 = new NotificationResponse();

            when(notificationRepository.findByRecipientUsername(eq(MOCK_USERNAME), eq(cursor), any(Limit.class)))
                    .thenReturn(mockDbResult);
            when(notificationMapper.toNotificationResponse(n1)).thenReturn(res1);
            when(notificationMapper.toNotificationResponse(n2)).thenReturn(res2);

            // Call
            CursorResponse<NotificationResponse> response = notificationService.getNotification(cursor, size);

            // Assert
            assertNotNull(response);
            assertEquals(2, response.getContent().size());
            assertFalse(response.isHasNext());
            assertEquals(n2.getId().toString(), response.getNextCursor()); // Phần tử cuối làm cursor
            verify(notificationRepository).findByRecipientUsername(MOCK_USERNAME, cursor, Limit.of(size + 1));
        }

        @Test
        @DisplayName("Trường hợp 3 - Số lượng phần tử lớn hơn size (hasNext = true)")
        void getNotification_HasNextTrue() {
            UUID cursor = UUID.randomUUID();
            int size = 2;

            Notification n1 = Notification.builder().id(UUID.randomUUID()).build();
            Notification n2 = Notification.builder().id(UUID.randomUUID()).build();
            Notification n3 = Notification.builder().id(UUID.randomUUID()).build(); // Phần tử dư thừa (thứ 3)
            
            // DB trả về 3 phần tử (vì Limit = size + 1 = 3)
            List<Notification> mockDbResult = List.of(n1, n2, n3); 

            NotificationResponse res1 = new NotificationResponse();
            NotificationResponse res2 = new NotificationResponse();

            when(notificationRepository.findByRecipientUsername(eq(MOCK_USERNAME), eq(cursor), any(Limit.class)))
                    .thenReturn(mockDbResult);
            when(notificationMapper.toNotificationResponse(n1)).thenReturn(res1);
            when(notificationMapper.toNotificationResponse(n2)).thenReturn(res2);

            // Call
            CursorResponse<NotificationResponse> response = notificationService.getNotification(cursor, size);

            // Assert
            assertNotNull(response);
            assertEquals(2, response.getContent().size()); // Chỉ lấy 2 sau khi removeLast
            assertTrue(response.isHasNext()); // Đã xác nhận có trang tiếp theo
            assertEquals(n2.getId().toString(), response.getNextCursor()); // Cursor phải là của n2 (không phải n3)
            
            // Đảm bảo không map n3 sang Response
            verify(notificationMapper, never()).toNotificationResponse(n3); 
        }
    }
}