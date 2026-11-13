package vn.cozynest.booking.application.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import vn.cozynest.booking.domain.model.Contract;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

@Service
public class ContractPdfService {

    private static final List<String> UNICODE_FONT_CANDIDATES = List.of(
            "C:\\Windows\\Fonts\\arial.ttf",
            "C:\\Windows\\Fonts\\tahoma.ttf",
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
            "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf"
    );

    private static final DateTimeFormatter VI_DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public GeneratedPdf generateSignedContractPdf(Contract contract) {
        return generateContractPdf(contract);
    }

    public GeneratedPdf generateContractPdf(Contract contract) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 32, 32);
            PdfWriter writer = PdfWriter.getInstance(doc, baos);
            doc.open();

            Font headerTitleFont = resolveFont(11, Font.BOLD);
            Font headerSubFont = resolveFont(9, Font.BOLD);
            Font mainTitleFont = resolveFont(13, Font.BOLD);
            Font sectionHeadingFont = resolveFont(9.5f, Font.BOLD);
            Font bodyFont = resolveFont(8.5f, Font.NORMAL);
            Font italicSmallFont = resolveFont(7.5f, Font.ITALIC);

            NumberFormat vnd = NumberFormat.getIntegerInstance(Locale.US);

            LocalDate now = contract.getCreatedAt() != null
                    ? LocalDate.ofInstant(contract.getCreatedAt(), java.time.ZoneId.systemDefault())
                    : LocalDate.now();

            // 1. Quốc hiệu - Tiêu ngữ
            Paragraph header1 = new Paragraph("CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM", headerTitleFont);
            header1.setAlignment(Element.ALIGN_CENTER);
            doc.add(header1);

            Paragraph header2 = new Paragraph("Độc lập - Tự do - Hạnh phúc", headerSubFont);
            header2.setAlignment(Element.ALIGN_CENTER);
            doc.add(header2);

            Paragraph headerDivider = new Paragraph("---------o0o---------", italicSmallFont);
            headerDivider.setAlignment(Element.ALIGN_CENTER);
            doc.add(headerDivider);
            doc.add(new Paragraph(" "));

            // 2. Tiêu đề Hợp đồng
            Paragraph mainTitle = new Paragraph("HỢP ĐỒNG THUÊ NHÀ", mainTitleFont);
            mainTitle.setAlignment(Element.ALIGN_CENTER);
            doc.add(mainTitle);

            Paragraph contractSub = new Paragraph("Mã hợp đồng: " + safe(contract.getContractCode())
                    + "  |  Ngày lập: " + now.format(VI_DATE_FMT)
                    + "  |  Trạng thái: " + safe(contract.getStatus()), italicSmallFont);
            contractSub.setAlignment(Element.ALIGN_CENTER);
            doc.add(contractSub);
            doc.add(new Paragraph(" "));

            // 3. Căn cứ pháp lý
            doc.add(new Paragraph("- Căn cứ Bộ Luật Dân sự số 91/2015/QH13 của Quốc Hội nước CHXHCN Việt Nam;", italicSmallFont));
            doc.add(new Paragraph("- Căn cứ Luật Nhà ở số 27/2023/QH15 của Quốc Hội nước CHXHCN Việt Nam;", italicSmallFont));
            doc.add(new Paragraph("- Căn cứ nhu cầu và khả năng thực tế của các Bên tham gia giao dịch.", italicSmallFont));
            doc.add(new Paragraph(" "));

            String intro = String.format("Hôm nay, ngày %d tháng %d năm %d, tại địa chỉ phòng thuê thuộc hệ thống CozyNest, chúng tôi gồm có:",
                    now.getDayOfMonth(), now.getMonthValue(), now.getYear());
            doc.add(new Paragraph(intro, bodyFont));
            doc.add(new Paragraph(" "));

            // 4. Thông tin các bên
            PdfPTable partiesTable = new PdfPTable(2);
            partiesTable.setWidthPercentage(100);
            partiesTable.setSpacingBefore(2);
            partiesTable.setSpacingAfter(4);

            PdfPCell cellA = new PdfPCell();
            cellA.setBorder(Rectangle.BOX);
            cellA.setPadding(6);
            cellA.addElement(new Paragraph("BÊN CHO THUÊ (Sau đây gọi tắt là Bên A):", sectionHeadingFont));
            cellA.addElement(new Paragraph("• Đại diện / Mã chủ trọ: " + safe(contract.getLandlordId()), bodyFont));
            cellA.addElement(new Paragraph("• Tư cách: Bên quản lý & cho thuê tài sản trên hệ thống CozyNest", bodyFont));
            partiesTable.addCell(cellA);

            PdfPCell cellB = new PdfPCell();
            cellB.setBorder(Rectangle.BOX);
            cellB.setPadding(6);
            cellB.addElement(new Paragraph("BÊN THUÊ (Sau đây gọi tắt là Bên B):", sectionHeadingFont));
            cellB.addElement(new Paragraph("• Đại diện / Mã khách thuê: " + safe(contract.getTenantId()), bodyFont));
            cellB.addElement(new Paragraph("• Mã yêu cầu thuê liên kết: " + safe(contract.getRequestId()), bodyFont));
            cellB.addElement(new Paragraph("• Tư cách: Bên thuê sử dụng cư trú", bodyFont));
            partiesTable.addCell(cellB);

            doc.add(partiesTable);
            doc.add(new Paragraph("Hai Bên tự nguyện cùng nhau thống nhất lập và ký kết Hợp đồng này để thực hiện việc thuê và cho thuê tài sản theo các thỏa thuận chi tiết sau đây:", italicSmallFont));
            doc.add(new Paragraph(" "));

            // ĐIỀU 1
            doc.add(new Paragraph("ĐIỀU 1: DIỆN TÍCH CHO THUÊ VÀ MỤC ĐÍCH THUÊ", sectionHeadingFont));
            doc.add(new Paragraph("1.1. Bên A đồng ý cho Bên B thuê, Bên B đồng ý thuê của Bên A toàn bộ phòng/nhà ở: Mã số #"
                    + safe(contract.getRoomId()) + " trên nền tảng CozyNest. Tình trạng kỹ thuật và vệ sinh bảo đảm tốt, trang bị hệ thống công tơ điện và đồng hồ nước độc lập. (Sau đây gọi tắt là \"Tài sản\" hoặc \"Tài sản thuê\").", bodyFont));
            doc.add(new Paragraph("1.2. Mục đích thuê: Dùng để ở sinh hoạt và cư trú hợp pháp. Bên B cam kết không sử dụng tài sản thuê vào bất kỳ mục đích kinh doanh trái phép hoặc các mục đích khác trái pháp luật.", bodyFont));
            doc.add(new Paragraph(" "));

            // ĐIỀU 2
            doc.add(new Paragraph("ĐIỀU 2: THỜI HẠN THUÊ, GIA HẠN VÀ CHẤM DỨT HỢP ĐỒNG", sectionHeadingFont));
            String startStr = contract.getStartDate() != null ? contract.getStartDate().format(VI_DATE_FMT) : "-";
            String endStr = contract.getEndDate() != null ? contract.getEndDate().format(VI_DATE_FMT) : "-";
            doc.add(new Paragraph("2.1. Thời hạn thuê là từ ngày " + startStr + " đến hết ngày " + endStr + ".", bodyFont));
            doc.add(new Paragraph("2.2. Bên A bàn giao tài sản thuê cho Bên B vào ngày " + startStr + ".", bodyFont));
            doc.add(new Paragraph("2.3. Hợp đồng này có hiệu lực thi hành kể từ ngày các bên ký kết hoặc bản chụp/scan hợp đồng có chữ ký hợp lệ được tải lên và xác nhận trên hệ thống CozyNest.", bodyFont));
            doc.add(new Paragraph("2.4. Hợp đồng đương nhiên chấm dứt khi hết thời hạn thuê hoặc hai bên đạt được thỏa thuận bằng văn bản.", bodyFont));
            doc.add(new Paragraph("2.5. Nếu Bên B không thanh toán tiền nhà sau 15 (mười lăm) ngày kể từ ngày đến hạn thanh toán của mỗi đợt thì Bên A có quyền đơn phương chấm dứt Hợp đồng mà Bên B không được bồi thường.", bodyFont));
            doc.add(new Paragraph("2.6. Trường hợp chấm dứt Hợp đồng trước thời hạn: Bên muốn chấm dứt phải thông báo bằng văn bản cho bên kia biết trước ít nhất 30 ngày. Hoàn trả tiền thuê đã trả trước chưa sử dụng (nếu có) và xử lý tiền đặt cọc theo Điều 3.", bodyFont));
            doc.add(new Paragraph("2.7. Việc chậm trễ thực hiện nghĩa vụ do thiên tai, dịch bệnh, hỏa hoạn, chiến tranh hoặc sự kiện bất khả kháng theo quy định pháp luật không bị coi là vi phạm hợp đồng.", bodyFont));
            doc.add(new Paragraph(" "));

            // ĐIỀU 3
            doc.add(new Paragraph("ĐIỀU 3: GIÁ THUÊ, ĐẶT CỌC VÀ PHƯƠNG THỨC THANH TOÁN", sectionHeadingFont));
            long rent = contract.getMonthlyRent();
            long deposit = contract.getDepositAmount();
            doc.add(new Paragraph("3.1. Giá cho thuê: " + vnd.format(rent) + " VNĐ/tháng (Bằng chữ: " + toVietnameseWords(rent) + "). Giá thuê áp dụng ổn định trong suốt thời hạn hợp đồng.", bodyFont));
            doc.add(new Paragraph("3.2. Tiền đặt cọc: Bên B bàn giao cho Bên A số tiền: " + vnd.format(deposit) + " VNĐ (Bằng chữ: " + toVietnameseWords(deposit) + ") ngay sau khi ký hợp đồng để bảo đảm thực hiện hợp đồng. Hoàn trả lại cho Bên B khi kết thúc hợp đồng sau khi khấu trừ chi phí hư hỏng hoặc công nợ tiện ích (nếu có). Bên đơn phương chấm dứt vi phạm thời hạn báo trước sẽ mất cọc hoặc bồi thường tương đương tiền cọc.", bodyFont));
            doc.add(new Paragraph("3.3. Chu kỳ và hạn thanh toán: " + contract.getPaymentCycleMonths() + " tháng/lần, hoàn tất trước ngày " + contract.getPaymentDueDay() + " của kỳ tiếp theo.", bodyFont));
            doc.add(new Paragraph("3.4. Phương thức thanh toán: Thực hiện thanh toán trực tuyến qua cổng bảo đảm của hệ thống CozyNest (hoặc chuyển khoản ngân hàng do Bên A chỉ định). Hệ thống tự động xuất hóa đơn và biên nhận điện tử cho mỗi lần thanh toán.", bodyFont));
            doc.add(new Paragraph(" "));

            // ĐIỀU 4
            doc.add(new Paragraph("ĐIỀU 4: PHÍ DỊCH VỤ VÀ BÀN GIAO CHỈ SỐ CÔNG TƠ TIỆN ÍCH BAN ĐẦU", sectionHeadingFont));
            String elecInitial = contract.getInitialElectricityReading() != null ? contract.getInitialElectricityReading().toPlainString() : "0";
            String waterInitial = contract.getInitialWaterReading() != null ? contract.getInitialWaterReading().toPlainString() : "0";
            String elecRate = contract.getElectricityRate() != null ? vnd.format(contract.getElectricityRate().longValue()) : "3,500";
            String waterRate = contract.getWaterRate() != null ? vnd.format(contract.getWaterRate().longValue()) : "25,000";

            doc.add(new Paragraph("4.1. Bàn giao chỉ số công tơ tiện ích ban đầu tại thời điểm Bên B nhận phòng:", bodyFont));
            doc.add(new Paragraph("   • Chỉ số công tơ ĐIỆN ban đầu: " + elecInitial + " kWh (Đơn giá: " + elecRate + " VNĐ/kWh).", bodyFont));
            doc.add(new Paragraph("   • Chỉ số đồng hồ NƯỚC ban đầu: " + waterInitial + " m³ (Đơn giá: " + waterRate + " VNĐ/m³).", bodyFont));
            doc.add(new Paragraph("4.2. Hàng tháng, Bên A có trách nhiệm ghi nhận chỉ số công tơ điện nước tiêu thụ thực tế và tải ảnh minh chứng mặt đồng hồ lên hệ thống CozyNest. Bên B có quyền đối chiếu hình ảnh công tơ trước khi thực hiện thanh toán hóa đơn.", bodyFont));
            doc.add(new Paragraph("4.3. Các khoản thuế, phí và lệ phí riêng biệt (nếu có) phát sinh do bên nào thì bên đó tự chịu trách nhiệm thanh toán theo quy định của pháp luật.", bodyFont));
            doc.add(new Paragraph(" "));

            // ĐIỀU 5
            doc.add(new Paragraph("ĐIỀU 5: QUYỀN VÀ NGHĨA VỤ CỦA BÊN A", sectionHeadingFont));
            doc.add(new Paragraph("5.1. Bàn giao tài sản sạch sẽ, bảo đảm quyền sử dụng ổn định và độc lập; tạo điều kiện điện nước sinh hoạt thuận lợi.", bodyFont));
            doc.add(new Paragraph("5.2. Có trách nhiệm phối hợp thực hiện thủ tục đăng ký tạm trú cho Bên B tại cơ quan Công an địa phương theo quy định.", bodyFont));
            doc.add(new Paragraph("5.3. Nhận đủ tiền thuê đúng hạn; yêu cầu bồi thường nếu Bên B làm hư hỏng tài sản do lỗi chủ quan.", bodyFont));
            doc.add(new Paragraph(" "));

            // ĐIỀU 6
            doc.add(new Paragraph("ĐIỀU 6: QUYỀN VÀ NGHĨA VỤ CỦA BÊN B", sectionHeadingFont));
            doc.add(new Paragraph("6.1. Sử dụng tài sản đúng mục đích cư trú; thanh toán đầy đủ tiền thuê và tiện ích đúng hạn.", bodyFont));
            doc.add(new Paragraph("6.2. Giữ gìn an ninh trật tự, chấp hành nghiêm quy định an toàn PCCC; không tự ý thay đổi kết cấu sửa chữa khi chưa được Bên A đồng ý.", bodyFont));
            doc.add(new Paragraph("6.3. Tuyệt đối không sử dụng tài sản để tàng trữ hàng cấm, vũ khí, chất cháy nổ hoặc vi phạm pháp luật.", bodyFont));
            doc.add(new Paragraph("6.4. Bàn giao lại tài sản trong tình trạng sạch sẽ, nguyên vẹn khi chấm dứt hợp đồng.", bodyFont));
            doc.add(new Paragraph(" "));

            // ĐIỀU 7 & 8
            doc.add(new Paragraph("ĐIỀU 7: GIẢI QUYẾT TRANH CHẤP", sectionHeadingFont));
            doc.add(new Paragraph("Trong quá trình thực hiện Hợp đồng này, nếu phát sinh tranh chấp, các bên cùng nhau thương lượng giải quyết trên tinh thần tôn trọng quyền lợi của nhau. Nếu không tự thương lượng được thì một trong hai bên có quyền yêu cầu Tòa án nhân dân có thẩm quyền giải quyết theo quy định của pháp luật.", bodyFont));
            doc.add(new Paragraph(" "));

            doc.add(new Paragraph("ĐIỀU 8: CÁC ĐIỀU KHOẢN CHUNG", sectionHeadingFont));
            doc.add(new Paragraph("8.1. Hợp đồng này thay thế cho toàn bộ các thỏa thuận miệng hoặc văn bản trước đó giữa hai Bên.", bodyFont));
            doc.add(new Paragraph("8.2. Mọi sửa đổi, bổ sung phải lập thành văn bản (Phụ lục hợp đồng) và được ký bởi đại diện hai Bên.", bodyFont));
            doc.add(new Paragraph("8.3. Hợp đồng được lập thành 02 (hai) bản có giá trị pháp lý như nhau, mỗi bên giữ 01 bản làm căn cứ thi hành. Bản điện tử có giá trị đối soát toàn vẹn trên hệ thống CozyNest.", bodyFont));
            if (contract.getTerms() != null && !contract.getTerms().isBlank()) {
                doc.add(new Paragraph("8.4. Thỏa thuận riêng bổ sung: " + contract.getTerms(), bodyFont));
            }
            doc.add(new Paragraph(" "));

            // 9. Bảng ký tên 2 cột
            PdfPTable signTable = new PdfPTable(2);
            signTable.setWidthPercentage(100);
            signTable.setSpacingBefore(6);

            PdfPCell cellSignA = new PdfPCell();
            cellSignA.setBorder(Rectangle.NO_BORDER);
            cellSignA.setHorizontalAlignment(Element.ALIGN_CENTER);
            cellSignA.addElement(new Paragraph("BÊN CHO THUÊ TÀI SẢN (BÊN A)", sectionHeadingFont));
            cellSignA.addElement(new Paragraph("(Ký/ điểm chỉ, ghi rõ họ tên)", italicSmallFont));
            if (contract.getLandlordSignedAt() != null) {
                cellSignA.addElement(new Paragraph("\n[Đã ký số điện tử: " + contract.getLandlordSignedAt() + "]", italicSmallFont));
            } else if ("PHYSICAL_SIGNED".equals(contract.getPhysicalContractStatus()) || contract.getPhysicalSignedAt() != null) {
                cellSignA.addElement(new Paragraph("\n[Đã ký tay trên bản scan]", italicSmallFont));
            } else {
                cellSignA.addElement(new Paragraph("\n\n\n", bodyFont));
            }
            cellSignA.addElement(new Paragraph("Họ và tên: " + safe(contract.getLandlordId()), bodyFont));
            signTable.addCell(cellSignA);

            PdfPCell cellSignB = new PdfPCell();
            cellSignB.setBorder(Rectangle.NO_BORDER);
            cellSignB.setHorizontalAlignment(Element.ALIGN_CENTER);
            cellSignB.addElement(new Paragraph("BÊN THUÊ TÀI SẢN (BÊN B)", sectionHeadingFont));
            cellSignB.addElement(new Paragraph("(Ký/ điểm chỉ, ghi rõ họ tên)", italicSmallFont));
            if (contract.getTenantSignedAt() != null) {
                cellSignB.addElement(new Paragraph("\n[Đã ký số điện tử: " + contract.getTenantSignedAt() + "]", italicSmallFont));
            } else if ("PHYSICAL_SIGNED".equals(contract.getPhysicalContractStatus()) || contract.getTenantConfirmedAt() != null) {
                cellSignB.addElement(new Paragraph("\n[Đã xác nhận bản scan ký tay]", italicSmallFont));
            } else {
                cellSignB.addElement(new Paragraph("\n\n\n", bodyFont));
            }
            cellSignB.addElement(new Paragraph("Họ và tên: " + safe(contract.getTenantId()), bodyFont));
            signTable.addCell(cellSignB);

            doc.add(signTable);

            // 10. Mã QR xác thực hợp đồng ở chân trang
            String verCode = contract.getVerificationCode() != null && !contract.getVerificationCode().isBlank()
                    ? contract.getVerificationCode()
                    : "CN-" + safe(contract.getContractCode());
            String docHash = contract.getDocumentHash() != null ? contract.getDocumentHash() : verCode;
            try {
                renderVerificationQrBlock(writer.getDirectContent(), verCode, docHash);
            } catch (Exception ignored) {
                // non-critical QR block
            }

            doc.close();
            byte[] pdfBytes = baos.toByteArray();
            return new GeneratedPdf(pdfBytes, sha256Hex(pdfBytes));
        } catch (Exception ex) {
            throw new IllegalStateException("Khong the tao file PDF hop dong: " + ex.getMessage(), ex);
        }
    }

    public static String toVietnameseWords(long number) {
        if (number <= 0) return "Không đồng";
        String[] units = {"", "một", "hai", "ba", "bốn", "năm", "sáu", "bảy", "tám", "chín"};
        String[] scales = {"", "nghìn", "triệu", "tỷ", "nghìn tỷ"};

        long temp = number;
        List<Integer> groups = new ArrayList<>();
        while (temp > 0) {
            groups.add((int) (temp % 1000));
            temp /= 1000;
        }

        StringBuilder result = new StringBuilder();
        for (int i = groups.size() - 1; i >= 0; i--) {
            int g = groups.get(i);
            if (g > 0) {
                String gStr = readThreeDigits(g, i < groups.size() - 1, units);
                result.append(gStr).append(" ").append(scales[i]).append(" ");
            }
        }
        String res = result.toString().trim().replaceAll("\\s+", " ");
        if (res.isEmpty()) return "Không đồng";
        return Character.toUpperCase(res.charAt(0)) + res.substring(1) + " đồng";
    }

    private static String readThreeDigits(int group, boolean showZeroHundred, String[] units) {
        int h = group / 100;
        int t = (group % 100) / 10;
        int u = group % 10;
        StringBuilder sb = new StringBuilder();
        if (h > 0 || showZeroHundred) {
            sb.append(units[h]).append(" trăm ");
        }
        if (t > 1) {
            sb.append(units[t]).append(" mươi ");
            if (u == 1) sb.append("mốt ");
            else if (u == 5) sb.append("lăm ");
            else if (u > 0) sb.append(units[u]).append(" ");
        } else if (t == 1) {
            sb.append("mười ");
            if (u == 5) sb.append("lăm ");
            else if (u > 0) sb.append(units[u]).append(" ");
        } else if (h > 0 && u > 0) {
            sb.append("lẻ ").append(units[u]).append(" ");
        } else if (u > 0) {
            sb.append(units[u]).append(" ");
        }
        return sb.toString().trim();
    }

    private Font resolveFont(float size, int style) {
        for (String candidate : UNICODE_FONT_CANDIDATES) {
            try {
                if (Files.isReadable(Path.of(candidate))) {
                    BaseFont base = BaseFont.createFont(candidate, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                    return new Font(base, size, style);
                }
            } catch (Exception ignored) {
                // Fall back to next candidate
            }
        }
        return FontFactory.getFont(FontFactory.HELVETICA, size, style);
    }

    /**
     * Renders a deterministic 21x21 verification QR-style matrix with standard 7x7 corner finders
     * bound to SHA-256(verificationCode + documentHash) in the bottom-right signature stamp box.
     */
    private void renderVerificationQrBlock(PdfContentByte cb, String verCode, String docHash) throws Exception {
        byte[] seed = MessageDigest.getInstance("SHA-256")
                .digest((verCode + "|" + docHash).getBytes(StandardCharsets.UTF_8));
        float startX = 455f;
        float startY = 40f;
        float moduleSize = 3.2f;
        int grid = 21;

        cb.saveState();
        cb.setColorStroke(Color.DARK_GRAY);
        cb.rectangle(startX - 3f, startY - 3f, grid * moduleSize + 6f, grid * moduleSize + 6f);
        cb.stroke();
        cb.setColorFill(Color.BLACK);

        for (int row = 0; row < grid; row++) {
            for (int col = 0; col < grid; col++) {
                boolean fill = isFinderFilled(row, col, grid);
                if (!fill && !inFinderZone(row, col, grid)) {
                    int bitIndex = (row * grid + col) % (seed.length * 8);
                    fill = ((seed[bitIndex / 8] >> (bitIndex % 8)) & 1) == 1;
                }
                if (fill) {
                    float x = startX + col * moduleSize;
                    float y = startY + (grid - 1 - row) * moduleSize;
                    cb.rectangle(x, y, moduleSize, moduleSize);
                    cb.fill();
                }
            }
        }
        cb.restoreState();
    }

    private boolean inFinderZone(int r, int c, int grid) {
        return (r < 7 && c < 7) || (r < 7 && c >= grid - 7) || (r >= grid - 7 && c < 7);
    }

    private boolean isFinderFilled(int r, int c, int grid) {
        if (r < 7 && c < 7) return finderSquare(r, c);
        if (r < 7 && c >= grid - 7) return finderSquare(r, c - (grid - 7));
        if (r >= grid - 7 && c < 7) return finderSquare(r - (grid - 7), c);
        return false;
    }

    private boolean finderSquare(int r, int c) {
        if (r == 0 || r == 6 || c == 0 || c == 6) return true;
        return r >= 2 && r <= 4 && c >= 2 && c <= 4;
    }

    private String safe(String v) {
        return v == null || v.isBlank() ? "-" : v;
    }

    private String sha256Hex(byte[] bytes) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(bytes));
    }

    public record GeneratedPdf(byte[] bytes, String sha256Hex) {}
}
