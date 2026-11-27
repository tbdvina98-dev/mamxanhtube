# Mầm Xanh Tube — APK Độc Lập cho Android TV & Điện Thoại Android

Dự án này đóng gói trọn bộ ứng dụng **Mầm Xanh Tube** (giao diện, danh sách kênh/playlist đã duyệt, bộ lọc an toàn, hẹn giờ 20-20-20 và kho ngoại tuyến) vào trực tiếp bên trong tệp `.apk`, chạy hoàn toàn độc lập mà **không cần máy chủ web bên ngoài**.

## Tính năng chính
- **Chạy độc lập 100%**: Tự nạp giao diện đóng gói sẵn qua `WebViewAssetLoader` (HTTPS ảo nội bộ), không còn lỗi màn hình trắng hay lỗi đường link web.
- **Tương thích cả Android TV & Điện thoại Android**: Tự động bật giao diện TV 10-foot + điều khiển remote D-Pad khi chạy trên Android TV/Google TV, và giao diện cảm ứng khi chạy trên điện thoại/máy tính bảng.
- **Thêm kênh & playlist YouTube trực tiếp trên thiết bị**: Phụ huynh có thể thêm kênh hoặc playlist YouTube ngay trong Góc Phụ huynh trên TV/điện thoại.
- **Khóa an toàn trẻ em**: Chỉ cho phép nguồn nội bộ của ứng dụng và trình phát nhúng `youtube-nocookie.com/embed/`, chặn toàn bộ liên kết ra ngoài.

## Cách xuất tệp `.apk` trên GitHub (không cần cài phần mềm)
1. Giải nén tệp `MamXanhTube-AndroidTV-Project.zip` tải từ **Góc Phụ huynh → Android TV (APK)**.
2. Đẩy toàn bộ thư mục `android-tv` và `.github` lên kho GitHub của bạn (lưu ý ghi đè toàn bộ thư mục `android-tv` nếu cập nhật từ bản cũ).
3. Mở tab **Actions** → chọn **Build Android TV APK** → bấm **Run workflow** (để trống ô `app_url` để đóng gói bản chạy độc lập).
4. Sau ~2–3 phút, bấm vào lượt chạy có dấu tích xanh → tải mục **Artifacts → MamXanhTube-AndroidTV-APK** → giải nén lấy tệp `app-debug.apk` và cài lên Android TV hoặc điện thoại Android.
