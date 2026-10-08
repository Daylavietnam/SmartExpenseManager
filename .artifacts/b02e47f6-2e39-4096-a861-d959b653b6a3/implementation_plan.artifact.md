# Sửa chức năng quét QR từ Thư viện sang Camera

Hiện tại, chức năng "Quét QR" trong `AddActivity` đang mở thư viện ảnh để chọn ảnh QR. Người dùng muốn thay đổi hành vi này để sử dụng Camera quét mã QR trực tiếp.

## Các thay đổi đề xuất

Tôi sẽ cập nhật `AddActivity.java` để hỗ trợ quét QR qua Camera bằng cách tái sử dụng luồng chụp ảnh hiện có và thêm cờ hiệu để phân biệt giữa quét văn bản (OCR) và quét mã QR.

### [Component] UI Activity

#### [MODIFY] [AddActivity.java](file:///D:/Projects/Android/SmartExpenseManager/app/src/main/java/com/group14/expensemanager/ui/activity/AddActivity.java)

- Thêm biến `isQrScanning` để theo dõi trạng thái quét.
- Cập nhật `onSaveInstanceState` và `onCreate` để lưu trữ trạng thái này khi xoay màn hình.
- Cập nhật `cameraLauncher` để xử lý kết quả dựa trên biến `isQrScanning`.
- Cập nhật sự kiện click của `mcv_scan` để yêu cầu quyền Camera và mở Camera thay vì mở Thư viện.
- Cập nhật sự kiện click của `mcv_camera` để đặt `isQrScanning = false` trước khi mở Camera.

## Kế hoạch xác minh

### Kiểm tra thủ công
1. Mở màn hình Thêm giao dịch.
2. Nhấn vào nút "Quét QR".
3. Xác nhận Camera được mở lên.
4. Chụp một mã QR chứa thông tin hóa đơn.
5. Xác nhận ứng dụng nhận diện được mã QR và tự động điền thông tin vào form.
6. Thử nghiệm tương tự với nút "Chụp ảnh" để đảm bảo chức năng OCR vẫn hoạt động bình thường.
