# Tiến độ thực hiện [CONTRACT-HANDOVER-LIVEPAY]

- **Nhánh:** `feat/fix-payment-flow`
- **Thời gian bắt đầu:** 2026-11-19
- **Trạng thái tổng quan:** PHA 0 HOÀN THÀNH (DONE-VERIFIED) -> Sẵn sàng triển khai PHA 1

---

## 1. Bảng theo dõi tiến độ từng Pha

| Pha | Mục tiêu | Trạng thái | Ghi chú / Bằng chứng |
|---|---|---|---|
| **PHA 0** | Chẩn đoán hợp đồng mẫu, trích văn bản, lập bảng lỗi chữ, rà soát enum/bảng/API, đo baseline | DONE-VERIFIED | Đã trích xuất văn bản từ Web và PDF, chụp ảnh, ghi nhận baseline test BE/FE |
| **PHA 1** | Sửa mẫu hợp đồng và xuất file PDF (Unicode tiếng Việt, không đổi mã băm hợp đồng đã ký, template versioning) | NOT-STARTED | Tiếp tục thực hiện |
| **PHA 2** | Luồng cọc, dọn vào, bàn giao phòng (RESERVED, job nhắc việc bàn giao, ẩn tin đăng khi ACTIVE) | NOT-STARTED | Chờ xong Pha 1 |
| **PHA 3** | Chế độ giao dịch tiền thật giới hạn cho demo (LIVE_DEMO_LIMITED, VietQR động, webhook, sổ cái) | NOT-STARTED | Chờ xong Pha 2 |
| **PHA 4** | Nghiệm thu toàn diện, so sánh baseline, cập nhật tài liệu kiến trúc DECISIONS.md | NOT-STARTED | Chờ xong Pha 3 |

---

## 2. Bảng lỗi chữ và định dạng hợp đồng (Phát hiện từ Hợp đồng `d8082a0f-c5c9-4d62-9016-1e493f66057c`)

| Vị trí | Chữ / Giá trị hiện tại (Sai) | Chữ / Giá trị đề xuất (Đúng) | Loại lỗi | Nguồn gốc / File |
|---|---|---|---|---|
| PDF - Quốc hiệu | `CNG HÒA XÃ HI CH NGHA VIT NAM` | `CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM` | Mất dấu tiếng Việt (Font fallback HELVETICA) | `ContractPdfService.java` (đường dẫn font Linux thiếu thư mục dejavu đúng) |
| PDF - Tiêu ngữ | `c lp - T do - Hnh phúc` | `Độc lập - Tự do - Hạnh phúc` | Mất dấu tiếng Việt (Font HELVETICA) | `ContractPdfService.java` |
| PDF - Tiêu đề | `HP NG THUÊ NHÀ` | `HỢP ĐỒNG THUÊ NHÀ` | Mất dấu tiếng Việt | `ContractPdfService.java` |
| PDF - Tiêu đề các điều | `IU 1`, `IU 2`, `IU 3`... | `ĐIỀU 1`, `ĐIỀU 2`, `ĐIỀU 3`... | Mất ký tự `Đ` | `ContractPdfService.java` |
| PDF - Tiền tệ | `10,000 VN/tháng` | `10.000 đ/tháng` (hoặc `10.000 VNĐ/tháng`) | Mất chữ `Đ` và định dạng dấu phẩy kiểu Mỹ | `ContractPdfService.java` (dùng NumberFormat Locale.US) |
| PDF - Số tiền bằng chữ | `Bng ch: Mi nghìn ng` | `Bằng chữ: Mười nghìn đồng` | Rụng dấu tiếng Việt trong PDF | `ContractPdfService.java` |
| PDF - Đại diện Bên A | `• Đại diện / Mã chủ trọ: ff919669-1143-...` | `• Đại diện / Chủ trọ: Hào khách` | Hiện UUID thô thay vì họ tên người dùng | `ContractPdfService.java` |
| PDF - Ký tên Bên A | `H và tên: ff919669-1143-4cec-af6b-06c8a18df44d` | `Họ và tên: Hào khách` | Hiện UUID thô thay vì họ tên người dùng | `ContractPdfService.java` |
| PDF - Đại diện Bên B | `• Đại diện / Mã khách thuê: 3d21d512-ee1d-...` | `• Đại diện / Khách thuê: Cường Lê` | Hiện UUID thô thay vì họ tên người dùng | `ContractPdfService.java` |
| PDF - Ký tên Bên B | `H và tên: 3d21d512-ee1d-407c-8b92-a1d627b66e66` | `Họ và tên: Cường Lê` | Hiện UUID thô thay vì họ tên người dùng | `ContractPdfService.java` |
| Web - Tên Bên B (chủ trọ xem) | `Ông/Bà: Khách thuê (Mã #3d21d512-ee1d-...)` | `Ông/Bà: Cường Lê` | Dùng currentUser khi xem khác role dẫn đến fallback UUID | `ContractDetailWorkspace.tsx` |
| Web - Ký tên Bên B (chủ trọ xem) | `Họ và tên: Khách thuê (Mã #3d21d512-ee1d-...)` | `Họ và tên: Cường Lê` | Lỗi tương tự bên trên | `ContractDetailWorkspace.tsx` |
| Web - Liên hệ Bên B | `Theo hồ sơ khách thuê CozyNest` | Hiển thị SĐT / Email thực tế từ snapshot | Thiếu truyền dữ liệu liên hệ từ API backend | `ContractDto.java` & `ContractDetailWorkspace.tsx` |
| Web - Định dạng ngày | `20/5/2027` | `20/05/2027` | Thiếu số 0 đệm (`MM` thay vì `M`) | `format-vnd.ts` / `formatIsoDate` |
| PDF - Mã băm văn bản | Chỉ đưa vào mã QR ẩn góc dưới | In rõ dòng `Mã băm SHA-256 tài liệu: <hash>` và số trang `Trang x/y` | Thiếu in chuỗi băm để đối soát mắt thường | `ContractPdfService.java` |

---

## 3. Bảng chẩn đoán Baseline kỹ thuật (PHA 0)

| Hạng mục | Hiện trạng thật | Nguyên nhân | File liên quan | Cách sửa dự kiến | Rủi ro phá code |
|---|---|---|---|---|---|
| **Font Unicode PDF** | PDF sinh từ Docker bị rụng toàn bộ dấu tiếng Việt | `UNICODE_FONT_CANDIDATES` khai báo `/usr/share/fonts/truetype/dejavu/...` nhưng đường dẫn thật trong container là `/usr/share/fonts/dejavu/DejaVuSans.ttf` | `ContractPdfService.java` | Thêm đúng đường dẫn font Debian/Alpine và nhúng font dự phòng `Roboto/NotoSans` vào resources jar | Thấp, chỉ đổi cách vẽ file PDF, không đổi nội dung đã băm |
| **Dữ liệu 2 bên trên Hợp đồng** | Hiện UUID thô ở cả bản Web (khi đối tác xem) và bản PDF | `ContractDto` không chứa thông tin snapshot tên/SĐT khách thuê; FE fallback sang currentUser; PDF chỉ gọi `getLandlordId()` / `getTenantId()` | `ContractDto.java`, `ContractPdfService.java`, `ContractDetailWorkspace.tsx` | Bổ sung trường snapshot `tenantName`, `tenantPhone`, `landlordName`, `landlordPhone` vào `ContractDto` từ dữ liệu snapshot đã lưu | Không phá code (chỉ thêm trường additive trong DTO) |
| **Ký hợp đồng & Kích hoạt** | Bản scan ký tay lập tức chuyển `ACTIVE` bỏ qua cọc & bàn giao | Trong `Contract.java`, `tenantConfirmPhysicalContract` gán `this.status = "ACTIVE"` ngay lập tức | `Contract.java`, `ContractService.java` | Điều chỉnh luồng ký tay chuyển sang `AWAITING_DEPOSIT` giống ký điện tử OTP | Rất thấp, tuân thủ đúng yêu cầu Pha 2 |
| **Trạng thái phòng** | Kích hoạt hợp đồng đồng bộ thẳng sang `RENTED` | `ContractActivationService` gọi `markRoomRented` ngay khi cọc xong | `ContractActivationService.java` | Đổi thành: Đủ cọc -> phòng chuyển `RESERVED` (giữ chỗ); Chủ trọ xác nhận bàn giao -> phòng mới chuyển `RENTED` | Rất thấp, đảm bảo đúng yêu cầu mới thay thế Pha 2 |
| **Bộ kiểm thử BE** | 100% tests đạt (booking, communication, listing, billing, identity) | Cần cấu hình `maxHeapSize = '192m'`, SerialGC để tránh lỗi giới hạn pagefile của Windows | `build.gradle`, `gradle.properties` | Đã cấu hình và kiểm chứng thành công | Không phá code |
| **Bộ kiểm thử FE** | ESLint 0 warning, TypeScript 0 error, Vitest 101/101 pass, Next.js build 202 routes pass | Build worker cần `cpus: 1` để tránh nghẽn bộ nhớ Windows | `next.config.ts` | Đã cấu hình và kiểm chứng thành công | Không phá code |

---

## 4. Nhật ký thực thi
- [CHẠY LẠI HÔM NAY] Hoàn tất chụp ảnh màn hình Web, tải file PDF thật của hợp đồng `d8082a0f-c5c9-4d62-9016-1e493f66057c`, trích xuất toàn bộ văn bản và đối chiếu cơ sở dữ liệu.
- [CHẠY LẠI HÔM NAY] Đã sao lưu toàn bộ cơ sở dữ liệu vào `backup_dev_phase0.sql` (28 MB) bằng `mysqldump`.
