# Mầm Xanh Tube — Phiên bản Android TV (APK)

Thư mục này chứa trọn bộ dự án Android TV (Kotlin + Leanback) bọc ứng dụng **Mầm Xanh Tube** với:
- Biểu ngữ (Banner) hiển thị chuẩn trên màn hình chính Android TV / Google TV (`LEANBACK_LAUNCHER`).
- Điều khiển hoàn toàn bằng remote TV (phím điều hướng D-Pad Lên/Xuống/Trái/Phải, OK/Enter, Quay lại).
- Phát video YouTube toàn màn hình tăng tốc phần cứng (`youtube-nocookie.com`) và khóa chặt tên miền để bé không thoát ra trình duyệt ngoài.

## Cách 1: Build file `.apk` bằng Android Studio (nhanh nhất trên máy tính)
1. Mở **Android Studio** -> chọn **Open** -> chọn thư mục `android-tv`.
2. Đợi Gradle tự tải thư viện.
3. (Tùy chọn) Đổi đường dẫn web app nếu bạn tự triển khai máy chủ riêng trong `app/build.gradle.kts` (`appUrl`).
4. Chọn menu **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
5. File cài đặt nằm tại: `android-tv/app/build/outputs/apk/debug/app-debug.apk`.

## Cách 2: Build file `.apk` tự động trên GitHub (không cần cài Android Studio)
1. Đẩy mã nguồn lên kho GitHub của bạn (đã có sẵn file `.github/workflows/build-android-tv-apk.yml`).
2. Vào tab **Actions** → chọn **Build Android TV APK** → bấm **Run workflow**.
3. Sau ~2 phút, tải trực tiếp file `MamXanhTube-AndroidTV-APK` ở mục Artifacts.

## Cài đặt file `.apk` lên Android TV / Google TV
- **Qua USB**: Chép file `.apk` vào USB, cắm vào TV và cài qua ứng dụng quản lý tệp (bật "Cho phép cài ứng dụng không rõ nguồn gốc").
- **Qua ADB (Wi-Fi)**:
  ```bash
  adb connect <IP_CUA_TV>:5555
  adb install -r app-debug.apk
  ```
