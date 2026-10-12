# SmartStock – Quản lý kho

Ứng dụng Android Java/XML, phát triển từ SmartExpenseManager. Tái sử dụng Room, MVVM, biểu đồ, trợ lý AI, chế độ tối và PIN/vân tay.

## Dùng thử

1. Mở **Sản phẩm & tồn kho** trên trang chủ. Thêm mã hàng duy nhất, tên, đơn vị, nhóm hàng, giá tham chiếu và mức tồn tối thiểu.
2. Mở **Khách hàng & nhà cung cấp** trên trang chủ để thêm đối tác, số điện thoại và địa chỉ. Có thể tìm tên/số điện thoại, sửa thông tin và xem riêng phiếu của mỗi đối tác. Đối tác đã có phiếu không thể xóa hoặc đổi loại.
3. Mở **Nhập–xuất**, chọn **Nhập kho** và nhà cung cấp. Chọn sản phẩm, số lượng và đơn giá; bấm **+ Thêm dòng sản phẩm** để thêm hàng khác. Ghi chú và tổng tiền dùng chung cho cả phiếu. Chọn đối tác không bắt buộc, phù hợp với dữ liệu cũ và phiếu từ chat.
4. Tạo phiếu **Xuất kho**, chọn khách hàng và nhập đơn giá cho từng dòng. Phiếu chỉ được lưu khi mọi dòng hợp lệ; nếu một dòng vượt tồn thì toàn bộ phiếu bị từ chối.
5. Xem số lượng tồn theo từng sản phẩm và cảnh báo khi tồn nhỏ hơn hoặc bằng mức tối thiểu. Có thể tìm kiếm theo mã hoặc tên sản phẩm.
6. Xem **Lịch sử** để lọc theo ngày/tuần/tháng hoặc tìm theo mã phiếu, đối tác, số điện thoại, ghi chú, mã/tên hàng (hỗ trợ tìm không dấu). Mỗi thẻ là một phiếu với tất cả dòng hàng. Sửa hoặc xóa tác động toàn bộ phiếu; các thao tác làm âm kho bị từ chối. Sản phẩm có lịch sử không thể xóa hoặc đổi đơn vị tính.
7. **Báo cáo** hiển thị giá trị nhập–xuất theo nhóm và thời gian. **Cài đặt → Xuất dữ liệu** xuất mỗi dòng hàng kèm mã phiếu, đối tác, điện thoại, ghi chú, số lượng, đơn giá và thành tiền dưới dạng bảng HTML `.xls`.

## Quy ước

- Tồn kho tính từ toàn bộ phiếu, không phụ thuộc bộ lọc báo cáo. Các phiếu cùng ngày xử lý nhập trước xuất, theo múi giờ thiết bị.
- Giá trị tồn trên trang chủ = số lượng tồn × giá tham chiếu hiện tại của từng sản phẩm. Đây là giá trị ước tính, không áp dụng FIFO/bình quân gia quyền.
- Chênh lệch giá trị nhập–xuất là chênh lệch thành tiền của phiếu, không phải lợi nhuận hay giá trị tồn.
- Nhập–xuất bằng cách chọn sản phẩm và nhập số lượng, đơn giá. Các tính năng máy ảnh, thư viện ảnh và quét mã đã được bỏ do phụ thuộc bộ nhận dạng từ Google Play Services không hoạt động trên môi trường thử nghiệm.
- Nhận xét báo cáo chạy trên máy. Trợ lý chat dùng kết nối AI đã có và gửi ngữ cảnh mã/tên/tồn/mức tối thiểu khi người dùng đặt câu hỏi.
- Chat có thể tạo phiếu ngay với câu rõ ràng như `Tôi đã nhập 20 chai nước suối`, `xuất 5 NS001` hoặc `nhập 20 NS001 đơn giá 5000`. Các lệnh này xử lý trong app, không cần gọi Gemini. Tên phải khớp tên đầy đủ hoặc mã hàng trong danh mục; chưa hỗ trợ mọi cách diễn đạt.
- Thiếu tên/số lượng thì chat hỏi lại; trả lời để hoàn tất, hoặc gõ `Hủy`. Mỗi yêu cầu một sản phẩm, ngày phiếu là hôm nay. Nhập hàng không ghi giá thì dùng giá tham chiếu hiện tại. Xuất hàng không ghi giá thì chat hỏi đơn giá; trả lời `5000`, `5.000đ` hoặc `giá 5000` rồi mới tạo phiếu/trừ kho. Ghi đơn giá ngay trong lệnh xuất thì không cần hỏi lại. Câu hỏi, phủ định, giả định, đơn vị không khớp và xuất quá tồn không tự thay đổi kho.
- Với mã hàng chỉ gồm số, ghi rõ `nhập 5 mã 123` để tránh nhầm mã với số lượng. Chat không tự quy đổi thùng/chai hay kg/gram.
- Chat bắt buộc chọn nhà cung cấp khi nhập và khách hàng khi xuất. Thiếu đối tác thì chat hỏi tên đầy đủ, số điện thoại hoặc `mã đối tác ID`; đối tác phải có trong danh bạ và đúng loại. Tên trùng sẽ được hỏi lại, không tự chọn. Có thể ghi ngay `nhập 20 NS001 nhà cung cấp TÊN` hoặc `xuất 5 NS001 khách hàng TÊN giá 5000`. Tồn kho chỉ đổi sau khi có đủ sản phẩm, số lượng, giá xuất và đối tác. Thiếu giá xuất thì hỏi giá trước, rồi hỏi khách hàng nếu còn thiếu.
- Phiếu tạo từ chat xuất hiện trong Lịch sử và cập nhật tồn/cảnh báo/báo cáo. Phản hồi thành công được lưu cùng giao dịch cơ sở dữ liệu; lỗi lưu không tạo phản hồi thành công.
- Dữ liệu kho nằm trong `smart_stock_db`; cơ sở dữ liệu chi tiêu cũ được giữ lại, không tự chuyển thành hàng hóa. Lịch sử chat kho bắt đầu mới; cài đặt giao diện và bảo mật hiện có vẫn dùng được.
- Bản này quản lý một kho và nhiều sản phẩm trên mỗi phiếu. Chat tạo phiếu một sản phẩm và yêu cầu đối tác; có thể mở phiếu đó trong Lịch sử để thêm ghi chú và dòng hàng khác. Chưa có nhiều kho hoặc lô/hạn sử dụng.
- Room nâng `smart_stock_db` từ phiên bản 1 lên 2 bằng migration, không xóa dữ liệu. Phiếu cũ giữ số lượng/giá và được cấp mã `legacy-ID`, mỗi phiếu cũ vẫn là một dòng. Tên và số điện thoại đối tác được lưu tại thời điểm lập phiếu, nên sửa danh bạ không đổi thông tin lịch sử. Khi cập nhật phiếu, thông tin đối tác được lấy lại từ danh bạ.

## Kiểm tra

Chạy `gradlew.bat assembleDebug testDebugUnitTest` với JDK của Android Studio. Các bài kiểm tra StockCalculator bao gồm tồn theo sản phẩm, xuất quá tồn, xuất trước nhập, sửa/xóa phiếu nhập, số lượng lẻ và dữ liệu không hợp lệ.

APK: `app/build/outputs/apk/debug/app-debug.apk`.
