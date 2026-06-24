# WebSocket Destinations

## 1. `/topic/conversations/{conversationId}/messages`

**Type:** Broadcast  
**Scope:** Conversation

### Mô tả
Client subscribe để nhận tin nhắn realtime khi đang mở một conversation.  
Được broadcast tới **tất cả** subscriber của conversation — không phân biệt private hay group.

### Events nhận được

| Event | Khi nào |
|---|---|
| `MessageNotificationEvent` | Private chat |
| `GroupMessageEvent` | Group chat |

### Payload

**`MessageNotificationEvent`**
```json
{
  "recipientId": "uuid",
  "conversationType": "PRIVATE",
  "message": {
    "messageId": "uuid",
    "content": "string",
    "sentAt": "LocalDateTime",
    "status": "SENT | DELIVERED | READ | DELETED",
    "conversationId": "uuid",
    "conversationStatus": "ACCEPTED | PENDING | BLOCKED",
    "senderId": "uuid",
    "senderUsername": "string",
    "senderAvatarUrl": "string",
    "media": []
  }
}
```

**`GroupMessageEvent`**
```json
{
  "conversationId": "uuid",
  "message": {
    "messageId": "uuid",
    "content": "string",
    "sentAt": "LocalDateTime",
    "status": "SENT | DELIVERED | READ | DELETED",
    "conversationId": "uuid",
    "conversationStatus": "ACCEPTED | PENDING | BLOCKED",
    "senderId": "uuid",
    "senderUsername": "string",
    "senderAvatarUrl": "string",
    "media": []
  }
}
```

### Mục đích
- Nhận tin nhắn mới khi đang mở phòng chat.
- Cập nhật realtime danh sách tin nhắn trong conversation.

### Subscribe (client)
```javascript
stompClient.subscribe(`/topic/conversations/${conversationId}/messages`, callback);
```

---

## 2. `/topic/conversations/{conversationId}/messages/deleted`

**Type:** Broadcast  
**Scope:** Conversation

### Mô tả
Client subscribe khi đang mở conversation để nhận thông báo tin nhắn bị xóa/thu hồi.  
Được broadcast tới **tất cả** subscriber của conversation.

### Event nhận được

| Event | Khi nào |
|---|---|
| `MessageDeletedNotificationEvent` | Khi một tin nhắn bị xóa hoặc thu hồi |

### Payload

**`MessageDeletedNotificationEvent`**
```json
{
  "conversationId": "uuid",
  "messageId": "uuid"
}
```

### Mục đích
- Đồng bộ việc thu hồi / xóa tin nhắn trên tất cả client đang mở conversation.
- Xóa hoặc cập nhật trạng thái tin nhắn trên UI realtime.

### Subscribe (client)
```javascript
stompClient.subscribe(`/topic/conversations/${conversationId}/messages/deleted`, callback);
```

---

## 3. `/user/queue/messages`

**Type:** Personal  
**Scope:** User

### Mô tả
Queue cá nhân của từng user — chỉ gửi đúng recipient, hoạt động ngay cả khi user **không mở** conversation tương ứng.  
Áp dụng cho **private chat** (`PRIVATE`).

### Event nhận được

| Event | Khi nào |
|---|---|
| `MessageNotificationEvent` | Khi có tin nhắn private mới gửi đến user |

### Payload

```json
{
  "recipientId": "uuid",
  "conversationType": "PRIVATE",
  "message": {
    "messageId": "uuid",
    "content": "string",
    "sentAt": "LocalDateTime",
    "status": "SENT | DELIVERED | READ | DELETED",
    "conversationId": "uuid",
    "conversationStatus": "ACCEPTED | PENDING | BLOCKED",
    "senderId": "uuid",
    "senderUsername": "string",
    "senderAvatarUrl": "string",
    "media": []
  }
}
```

### Mục đích
- Hiển thị badge tin nhắn chưa đọc.
- Hiển thị preview tin nhắn mới.
- Cập nhật danh sách conversation.
- Trigger mark `DELIVERED` cho tin nhắn.

### Subscribe (client)
```javascript
stompClient.subscribe("/user/queue/messages", callback);
```

---

## 4. `/user/queue/notifications`

**Type:** Personal  
**Scope:** User

### Mô tả
Queue thông báo cá nhân — nhận mọi loại notification hệ thống và tương tác xã hội, chỉ gửi đúng recipient.

### Event nhận được

| Event | Khi nào |
|---|---|
| `NotificationEvent` | Khi có bất kỳ notification nào gửi đến user |

### Payload

```json
{
  "recipientId": "uuid",
  "type": "FOLLOW | REACTION | MESSAGE | COMMENT | LIKE | MENTION | SYSTEM | REPOST | MESSAGE_PENDING",
  "content": "string",
  "actor": {
    "id": "uuid",
    "username": "string",
    "avatarUrl": "string"
  },
  "entityId": "uuid",
  "createdAt": "LocalDateTime"
}
```

### Notification types

| Type | Trường hợp |
|---|---|
| `FOLLOW` | Có người follow |
| `REACTION` | Có người thả cảm xúc |
| `MESSAGE` | Có tin nhắn mới (group chat) |
| `COMMENT` | Có người bình luận |
| `LIKE` | Có người like |
| `MENTION` | Có người nhắc tới |
| `SYSTEM` | Thông báo hệ thống |
| `REPOST` | Có người repost |
| `MESSAGE_PENDING` | Tin nhắn đang chờ duyệt |

### Mục đích
- Hiển thị notification bell / badge toàn cục.
- Trigger UI cho từng loại tương tác xã hội.
- Nhận thông báo tin nhắn group khi không mở conversation.

### Subscribe (client)
```javascript
stompClient.subscribe("/user/queue/notifications", callback);
```

---

## Tổng quan

| Destination | Type | Scope | Event |
|---|---|---|---|
| `/topic/conversations/{conversationId}/messages` | Broadcast | Conversation | `MessageNotificationEvent`, `GroupMessageEvent` |
| `/topic/conversations/{conversationId}/messages/deleted` | Broadcast | Conversation | `MessageDeletedNotificationEvent` |
| `/user/queue/messages` | Personal | User | `MessageNotificationEvent` |
| `/user/queue/notifications` | Personal | User | `NotificationEvent` |

---

## Luồng hoạt động

### Private message
Sender

└─► MessageNotificationEvent

├─► Broadcast ──► /topic/conversations/{conversationId}/messages

├─► SendToUser ──► /user/queue/messages        (recipient)

└─► createNotification + markDelivered

### Group message
Sender

└─► GroupMessageEvent

├─► Tạo NotificationEvent cho mọi member (trừ sender)

│     └─► SendToUser ──► /user/queue/notifications  (từng member)

├─► Broadcast ──► /topic/conversations/{conversationId}/messages

└─► markDelivered

### Delete message
Delete trigger

└─► MessageDeletedNotificationEvent

└─► Broadcast ──► /topic/conversations/{conversationId}/messages/deleted

### Notification
System / interaction trigger

└─► NotificationEvent

├─► createNotification (lưu DB)

└─► SendToUser ──► /user/queue/notifications

### 📑 Danh Sách REST API Endpoints - ConnectHub Chat Module

| Chức năng | Phương thức | Endpoint | Mô tả chi tiết nghiệp vụ |
|:---|:---:|:---|:---|
| **Gửi tin nhắn** *(Private + Group)* | `POST` | `/v1/chat/messages` | Gửi nội dung tin nhắn hoặc media vào một cuộc trò chuyện. Kích hoạt Event Socket gửi về phòng chat. |
| **Đánh dấu đã đọc** | `PUT` | `/v1/chat/{conversationId}/read` | Cập nhật trạng thái tất cả tin nhắn chưa đọc trong hội thoại thành `READ`. |
| **Xoá tin nhắn** *(Soft Delete)* | `DELETE` | `/v1/messages/{messageId}` | Ẩn/thu hồi tin nhắn phía Client. Kích hoạt `MessageDeletedNotificationEvent` qua Socket. |
| **Lấy danh sách conversation** | `GET` | `/v1/conversations` | Tải danh sách các cuộc trò chuyện hiện tại của user (gồm cả Private và Group). |
| **Lấy chi tiết conversation** | `GET` | `/v1/conversations/{id}` | Lấy thông tin chi tiết của một phòng chat cụ thể (thành viên, trạng thái,...). |
| **Tạo group conversation** | `POST` | `/v1/conversations/group` | Tạo một phòng chat nhóm mới với danh sách thành viên ban đầu. |
| **Chấp nhận yêu cầu trò chuyện** | `PATCH` | `/v1/conversations/accept` | Chuyển trạng thái cuộc trò chuyện từ `PENDING` sang `ACCEPTED` (áp dụng cho lời mời chat 1-1). |
| **Rời nhóm** | `PATCH` | `/v1/conversations/{id}/leave` | Thành viên tự chủ động rời khỏi cuộc trò chuyện nhóm. |
| **Xóa thành viên khỏi nhóm** | `PATCH` | `/v1/conversations/{id}/members/{memberId}/remove` | Quyền Quản trị viên (Admin) bắt buộc, dùng để kick một thành viên ra khỏi nhóm. |
| **Cập nhật thông tin nhóm** | `PATCH` | `/v1/conversations/{id}` | Thay đổi các thông tin cơ bản của nhóm như Tên nhóm (Group Name) hoặc Ảnh đại diện (Avatar). |
| **Lấy lịch sử tin nhắn** | `GET` | `/v1/messages/{conversationId}/messages` | Tải danh sách tin nhắn trong một cuộc trò chuyện cụ thể. |