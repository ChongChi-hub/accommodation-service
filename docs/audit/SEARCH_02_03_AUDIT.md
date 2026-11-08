# AUDIT CHECKLIST: [SEARCH-02] [SEARCH-03]

## BƯỚC KHỞI ĐẦU: ĐỐI CHIẾU
- [ ] 0. Đối chiếu /rooms hiện đang dùng repository và API nào.

## BACKEND (listing-service)
- [ ] 1. GET /api/v1/rooms với tham số: q, provinceId (hoặc provinceSlug), wardIds (nhiều), priceMin, priceMax, areaMin, areaMax, roomTypeIds, amenityIds (nhiều; amenityMode=all|any, mặc định all), allowPets, allowCooking, genderPreference, furnishing, includeUnavailable, sort (newest|priceAsc|priceDesc|areaAsc|areaDesc|relevance), page, size (tối đa 50). Áp dụng hợp đồng hiển thị công khai.
- [ ] 2. Từ khóa không phân biệt dấu và hoa thường trên tiêu đề, mô tả, địa chỉ, tên phường/tỉnh (FULLTEXT hoặc LIKE với collation ai_ci); test "phong tro cau giay", "Cầu Giấy", "CAU GIAY" cho cùng kết quả; escape %, _, ', --; chuỗi > 100 ký tự bị cắt hoặc 400; test chống SQL injection với payload thật.
- [ ] 3. Validate: priceMin<=priceMax, giá/diện tích không âm, size<=50, page>=0, id không tồn tại không gây 500; lỗi 400 theo RFC 9457 liệt kê từng trường.
- [ ] 4. Phản hồi: items (id, slug, title, giá, diện tích, ảnh bìa có width/height, tỉnh/phường, loại, vài tiện nghi nổi bật, huy hiệu chủ trọ đã xác minh, rating), meta (page, size, totalElements, totalPages) và facets (số phòng theo tỉnh, khoảng giá min/max thực của kết quả, đếm theo tiện nghi/loại phòng).
- [ ] 5. Hiệu năng: không N+1 (test đếm câu SQL); chỉ mục cho các kết hợp lọc phổ biến; dán EXPLAIN của 4 truy vấn tiêu biểu; p95 <= 300 ms (đo và dán). Nếu seed < 200 phòng, tạo bộ seed >= 2000 phòng CHỈ ở profile dev-perf để đo.
- [ ] 6. Testcontainers: từng bộ lọc riêng và kết hợp; AND của tiện nghi; phân trang không trùng/mất phòng khi sắp xếp theo giá trị trùng nhau (sắp phụ theo id); phòng HIDDEN/OCCUPIED/chưa duyệt không lọt vào.

## FRONTEND (SEARCH-02)
- [ ] 7. Bộ lọc: tỉnh/thành -> phường/xã (2 cấp, đổi tỉnh thì xóa phường đã chọn), khoảng giá (slider hai đầu + ô nhập + gợi ý nhanh, VND theo locale), diện tích, loại phòng, tiện nghi (nhóm theo group, kèm số lượng), thú cưng/nấu ăn/giới tính, sắp xếp.
- [ ] 8. Chip bộ lọc đang áp dụng (xóa từng chip), nút "Xóa tất cả", tổng số kết quả, trạng thái rỗng có gợi ý nới lỏng bộ lọc, skeleton, lỗi + thử lại (lỗi kết nối không hiện thành 0 kết quả); mobile: bộ lọc trong ngăn kéo có nút "Xem N kết quả".
- [ ] 9. Debounce 300 ms cho ô tìm và slider; hủy request cũ (AbortController), kết quả cũ không ghi đè kết quả mới (test race); giữ dữ liệu cũ mờ khi đang tải trang mới.

## FRONTEND (SEARCH-03)
- [ ] 10. URL là nguồn sự thật duy nhất: q, province, wards, priceMin, priceMax, areaMin, areaMax, type, amenities, pets, cooking, gender, sort, page, size. Dùng nuqs hoặc hook tự viết (chọn và ghi DECISIONS.md); Zod phân tích ở tầng application (RoomSearchCriteria); tham số sai/lạ -> về mặc định, KHÔNG crash, KHÔNG trang trắng.
- [ ] 11. Đổi bộ lọc -> về trang 1; Back/Forward đúng; sao chép URL mở ở tab mới/ẩn danh cho đúng kết quả và đúng trạng thái điều khiển; làm mới trang giữ nguyên; quy ước push/replace nhất quán (ghi lại).
- [ ] 12. Render lần đầu ở server từ URL (Server Component); metadata theo bộ lọc (title có khu vực), canonical; trang page>1 hoặc bộ lọc phức tạp dùng noindex; chuẩn hóa URL (sắp tham số, bỏ giá trị mặc định).
- [ ] 13. Phân trang: số trang, trước/sau, cuộn lên đầu danh sách khi đổi trang, "x–y trong tổng z", vượt trang cuối -> về trang cuối hợp lệ.

## KIỂM THỬ
- [ ] 14. E2E (backend thật): áp dụng nhiều bộ lọc -> URL đổi -> reload giữ -> số thẻ = số API trả về và tiêu đề đầu tiên khớp; Back/Forward; URL sai tham số; "phong tro" và "Phòng trọ" cùng kết quả; đổi tỉnh xóa phường.
- [ ] 15. Unit: parse/serialize tham số URL (vòng tròn parse(serialize(x)) = x), các giá trị biên.
- [ ] 16. Ảnh chụp: có kết quả, rỗng, lỗi, đang tải, ngăn bộ lọc mobile; 1440 và 390, light/dark, vi/en; tự phê bình 3 điểm CHƯA TỐT mỗi ảnh và sửa.

