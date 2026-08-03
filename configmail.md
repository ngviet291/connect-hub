## Cấu hình Gmail SMTP

Ứng dụng sử dụng Gmail SMTP để gửi email (OTP, xác thực tài khoản, quên mật khẩu,...).

### 1. Bật xác minh 2 bước (2-Step Verification)

Đăng nhập tài khoản Gmail muốn dùng để gửi email.

Vào **Google Account** → **Security** → **2-Step Verification** và bật tính năng này.

### 2. Tạo App Password

Sau khi bật xác minh 2 bước:

1. Vào **Google Account** → **Security**.
2. Chọn **App passwords**.
3. Chọn:

   * **App:** Mail
   * **Device:** Other (Custom name)
4. Đặt tên (ví dụ: `ConnectHub Backend`) và nhấn **Generate**.
5. Google sẽ tạo một **App Password** gồm 16 ký tự. Sao chép mật khẩu này.

> **Lưu ý:** Không sử dụng mật khẩu đăng nhập Gmail. Chỉ sử dụng **App Password**.

### 3. Cấu hình file `.env`

Tạo file `.env` tại thư mục gốc của dự án và thêm:

```env
# Gmail SMTP
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_16_character_app_password

# Frontend URL
FRONTEND_URL=http://localhost:5173
```

Ví dụ:

```env
MAIL_USERNAME=example@gmail.com
MAIL_PASSWORD=abcd efgh ijkl mnop
FRONTEND_URL=http://localhost:5173
```

> Khi dán vào file `.env`, có thể giữ nguyên hoặc xóa khoảng trắng trong App Password, Gmail đều chấp nhận.

### 4. Khởi động ứng dụng

Sau khi cấu hình xong, chạy ứng dụng. Backend sẽ sử dụng Gmail SMTP để gửi email thông qua tài khoản đã cấu hình.
