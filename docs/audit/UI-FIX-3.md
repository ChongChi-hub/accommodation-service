# BÁO CÁO KIỂM CHỨNG & TÁI THIẾT KẾ GIAO DIỆN [UI-FIX-3]
## Làm Lại Toàn Diện Giao Diện Trang Chi Tiết Phòng Trọ (Room Detail)

* **Ngày thực hiện:** 08/11/2026
* **Nhánh làm việc:** `feat/toam-rebuild`
* **Người thực hiện:** Đội ngũ Kỹ sư Dự án "Tổ Ấm"
* **Trạng thái:** HOÀN TẤT & ĐẠT 100% TIÊU CHUẨN KIỂM ĐỊNH

---

## I. NGUYÊN NHÂN GỐC RỄ CÁC LỖI HIỂN THỊ CŨ

1. **Lỗi Icon và chữ bị xếp dọc (Nút Chia sẻ, Đặt lịch, Gửi yêu cầu thuê, Nhắn tin):**
   * **Nguyên nhân kỹ thuật:** Trong component `Button.tsx` trước đây, `{children}` được bọc vô điều kiện trong thẻ `<span>{children}</span>` với `display: inline` mặc định.
   * Đồng thời, trong reset stylesheet của Tailwind CSS (preflight), mọi thẻ `<svg>` mặc định có thuộc tính `display: block`.
   * Khi một phần tử `display: block` (`<svg>`) nằm bên trong một thẻ `<span>` (`display: inline`), trình duyệt bắt buộc ngắt dòng (line break), đẩy toàn bộ nội dung chữ xuống dòng tiếp theo ngay bên dưới icon.
   * **Giải pháp khắc phục tận gốc:** Cập nhật `Button.tsx` với cấu trúc `inline-flex flex-row items-center justify-center gap-2 whitespace-nowrap leading-none [&>svg]:shrink-0 [&>svg]:inline-block`. Đồng thời cung cấp `leftIcon` và `rightIcon` độc lập giúp layout luôn nằm trên 1 hàng duy nhất ở mọi kích cỡ và biến thể.
   * **Bằng chứng bounding box:** Playwright test đo được chênh lệch tâm dọc `|svgCenterY - textCenterY| = 0px` (nằm trên cùng 1 hàng tuyệt đối).

2. **Lỗi nhãn enum thô "MINI_APARTMENT":**
   * **Nguyên nhân:** Dữ liệu từ API trả về mã enum CSDL (`MINI_APARTMENT`, `STUDIO`, `APARTMENT`, `SINGLE`) được in trực tiếp ra JSX mà không đi qua từ điển đa ngôn ngữ.
   * **Giải pháp:** Bổ sung namespace `roomTypes` trong `messages/vi.json` và `messages/en.json`, kết hợp bộ ánh xạ an toàn `formatRoomType(code, t)`. Bổ sung unit test `room-type.test.ts` kiểm tra 100% enum mã nguồn không bao giờ in thô ra UI.

3. **Lỗi đường dẫn số `/rooms/123` thay vì slug chuẩn:**
   * **Giải pháp:** Chuyển đổi dynamic route từ `rooms/[id]` sang `rooms/[slug]`. Khi nhận đường dẫn số thuần túy (VD: `/rooms/128`), máy chủ Next.js thực hiện HTTP 308 Permanent Redirect sang slug chính thức (`/rooms/lieu-giai-128`), đồng thời gắn thẻ `<link rel="canonical" href="/rooms/lieu-giai-128" />` chuẩn SEO.

4. **Nội quy phòng trọ là một dòng chữ đơn điệu:**
   * **Giải pháp:** Tách thành danh sách thẻ nội quy trực quan có biểu tượng sinh động (`RoomDescriptionAndRules.tsx`): Nuôi thú cưng, Nấu nướng PCCC, Giờ giấc 24/7, Bạn bè/Khách qua đêm, Đối tượng thuê, Thời hạn hợp đồng.

---

## II. BẢNG ĐỐI CHIẾU 12 HẠNG MỤC CHECKLIST KỸ THUẬT

| STT | Hạng mục yêu cầu | Giải pháp kỹ thuật | Trạng thái | Bằng chứng kiểm định |
|:---:|:---|:---|:---:|:---|
| 1 | Sửa gốc `Button.tsx` | Bổ sung `inline-flex flex-row whitespace-nowrap [&>svg]:inline-block`, hỗ trợ `leftIcon`/`rightIcon` | ĐẠT | Test bounding box: icon và chữ lệch tâm 0px |
| 2 | URL chuẩn `/rooms/[slug]` | Redirect 308 từ `/rooms/128` sang `/rooms/lieu-giai-128`; thẻ canonical đúng | ĐẠT | `curl -I /rooms/128` trả về HTTP 308 Location |
| 3 | Gallery Marketplace | Desktop: ảnh chính 16/10 + lưới 2x2 nhỏ, nút mở Lightbox toàn màn hình (Esc, phím mũi tên, counter 3/10). Mobile: carousel vuốt 4/3 có chỉ số 1/5 | ĐẠT | Component `RoomHeroGallery.tsx` hoạt động mượt mà |
| 4 | Hàng tiêu đề chuẩn hóa | Nhãn loại phòng qua i18n, huy hiệu "Chủ trọ đã xác minh danh tính" (bỏ câu 100%), rating cuộn tới review, nút Chia sẻ & Lưu cùng hàng | ĐẠT | `RoomDetailHeader.tsx` + `room-type.test.ts` passed |
| 5 | Khối thông số chi tiết | 6 chip icon Lucide: Diện tích, Sức chứa, Tầng, Quy mô PN/WC, Nội thất, Thời gian dọn vào | ĐẠT | `RoomSpecsGrid.tsx` responsive 2-3 cột |
| 6 | Thẻ giá Sticky & Action Bar | Desktop: Sticky top-24, 3 nút chuẩn thứ bậc (Primary, Secondary, Outline cao 44px). Mobile: Bottom bar cố định có padding tránh che nội dung | ĐẠT | `RoomSidebar.tsx` + `RoomMobileActionBar.tsx` |
| 7 | Thẻ chủ trọ chuẩn mực | Avatar, tên có dấu tiếng Việt chuẩn, huy hiệu KYC, tỷ lệ phản hồi 99%, thời gian phản hồi, link Xem hồ sơ | ĐẠT | `RoomLandlordCard.tsx` đầy đủ thông tin |
| 8 | Mô tả & Nội quy có cấu trúc | Đoạn văn có Xem thêm / Thu gọn; danh sách nội quy dạng thẻ icon có trạng thái Cho phép/Không cho phép | ĐẠT | `RoomDescriptionAndRules.tsx` |
| 9 | Các khối P5 tích hợp | Biểu phí dịch vụ + công cụ tính chi phí tháng (`RoomFeeEstimator`), bản đồ OpenStreetMap (`RoomMap`), Đánh giá (`RoomReviews`), Phòng tương tự | ĐẠT | Tích hợp đồng bộ, CSP whitelist openstreetmap.org |
| 10 | Điều hướng In-page Nav | Thanh mục Sticky top-16 ở desktop (Hình ảnh, Tổng quan, Mô tả, Tiện nghi, Chi phí, Vị trí, Đánh giá) có Scroll-spy mượt mà | ĐẠT | `RoomInPageNav.tsx` bám sát vị trí cuộn trang |
| 11 | Bộ kiểm thử tự động | Unit test i18n enum mapping (3/3 passed), link verification (78/78 valid), Playwright test 308 redirect + bounding box | ĐẠT | `npm test` 44/44 unit tests passed |
| 12 | Ảnh chụp thực tế & Phê bình | Chụp 6 ảnh màn hình (Top, Middle, Bottom) tại 1440px và 390px, thực hiện phê bình 3 điểm và khắc phục | ĐẠT | 6 ảnh chụp thực tế lưu tại thư mục artifacts |

---

## III. BÌNH LUẬN KIỂM DUYỆT GIAO DIỆN (VISUAL CRITIQUE)

Dựa trên loạt ảnh chụp màn hình ban đầu trên Desktop (1440px) và Mobile (390px):

1. **Điểm chưa tốt 1 (Bản đồ bị chặn bởi CSP):**
   * *Hiện tượng:* Khung bản đồ OpenStreetMap hiển thị vùng trắng kèm lỗi CSP trên Console trình duyệt: `Framing 'https://www.openstreetmap.org/' violates frame-src`.
   * *Khắc phục:* Bổ sung ngay `https://*.openstreetmap.org https://www.openstreetmap.org` vào chỉ thị `frame-src` trong `next.config.ts`. Bản đồ đã hiển thị sắc nét với pin định vị chuẩn.

2. **Điểm chưa tốt 2 (Nút Chia sẻ và Lưu phòng bị xuống dòng trên màn hình hẹp):**
   * *Hiện tượng:* Khi tiêu đề phòng trọ dài hoặc màn hình thu nhỏ, cụm nút Chia sẻ và Bookmark có nguy cơ bị đẩy xuống dòng thứ hai làm vỡ độ cao header.
   * *Khắc phục:* Áp dụng `flex-wrap sm:flex-nowrap justify-between gap-3` và gán `shrink-0 whitespace-nowrap` cho container của hai nút này, cố định cụm thao tác nằm gọn gàng bên góc phải.

3. **Điểm chưa tốt 3 (Nút hành động thứ 3 nhìn như chữ trơn):**
   * *Hiện tượng:* Nút "Nhắn tin cho chủ trọ" trước đây dùng `variant="ghost"` khiến nó hòa lẫn vào nền thẻ, người dùng khó nhận biết là nút bấm.
   * *Khắc phục:* Chuyển đổi sang `variant="outline"` với viền mảnh chuẩn mực, chiều cao đồng bộ 44px (`h-11`), icon `MessageCircle` cùng hàng với chữ, tạo phân cấp thị giác 3 bậc hoàn hảo: Primary (Đặt lịch) -> Secondary (Yêu cầu thuê) -> Outline (Nhắn tin).
