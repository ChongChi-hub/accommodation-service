package vn.cozynest.booking.application.service;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
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

    public GeneratedPdf generateSignedContractPdf(Contract contract) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 48, 48, 48, 48);
            PdfWriter writer = PdfWriter.getInstance(doc, baos);
            doc.open();

            Font titleFont = resolveFont(16, Font.BOLD);
            Font headingFont = resolveFont(11, Font.BOLD);
            Font bodyFont = resolveFont(10, Font.NORMAL);

            NumberFormat vnd = NumberFormat.getIntegerInstance(Locale.US);

            doc.add(new Paragraph("TỔ ẤM / COZYNEST - HỢP ĐỒNG THUÊ PHÒNG ĐIỆN TỬ", titleFont));
            doc.add(new Paragraph("Mã hợp đồng: " + safe(contract.getContractCode()), headingFont));
            doc.add(new Paragraph("Trạng thái: " + safe(contract.getStatus()), bodyFont));
            doc.add(new Paragraph(" "));

            doc.add(new Paragraph("1. THÔNG TIN CÁC BÊN & PHÒNG THUÊ", headingFont));
            doc.add(new Paragraph("Bên cho thuê (Landlord ID): " + safe(contract.getLandlordId()), bodyFont));
            doc.add(new Paragraph("Bên thuê (Tenant ID): " + safe(contract.getTenantId()), bodyFont));
            doc.add(new Paragraph("Mã phòng (Room ID): " + safe(contract.getRoomId()), bodyFont));
            doc.add(new Paragraph("Thời hạn thuê: " + contract.getStartDate() + " đến " + contract.getEndDate(), bodyFont));
            doc.add(new Paragraph(" "));

            doc.add(new Paragraph("2. GIÁ THUÊ VÀ TIỀN CỌC", headingFont));
            doc.add(new Paragraph("Giá thuê hàng tháng: " + vnd.format(contract.getMonthlyRent()) + " VND", bodyFont));
            doc.add(new Paragraph("Tiền đặt cọc: " + vnd.format(contract.getDepositAmount()) + " VND", bodyFont));
            doc.add(new Paragraph("Chu kỳ thanh toán: " + contract.getPaymentCycleMonths() + " tháng/lần, hạn ngày "
                    + contract.getPaymentDueDay(), bodyFont));
            doc.add(new Paragraph(" "));

            doc.add(new Paragraph("3. ĐIỀU KHOẢN HỢP ĐỒNG", headingFont));
            doc.add(new Paragraph(safe(contract.getTerms()), bodyFont));
            doc.add(new Paragraph(" "));

            doc.add(new Paragraph("4. CHỨNG THỰC KÝ SỐ OTP & MÃ XÁC THỰC QR", headingFont));
            doc.add(new Paragraph("Thời gian người thuê ký: " + safeTime(contract.getTenantSignedAt()), bodyFont));
            doc.add(new Paragraph("Thời gian chủ trọ ký: " + safeTime(contract.getLandlordSignedAt()), bodyFont));
            doc.add(new Paragraph("Mã băm nội dung (SHA-256): " + safe(contract.getDocumentHash()), bodyFont));
            String verCode = contract.getVerificationCode() != null && !contract.getVerificationCode().isBlank()
                    ? contract.getVerificationCode()
                    : "CN-" + safe(contract.getContractCode());
            doc.add(new Paragraph("Verification Code: " + verCode, bodyFont));

            renderVerificationQrBlock(writer.getDirectContent(), verCode, safe(contract.getDocumentHash()));

            doc.close();
            byte[] pdfBytes = baos.toByteArray();
            return new GeneratedPdf(pdfBytes, sha256Hex(pdfBytes));
        } catch (Exception ex) {
            throw new IllegalStateException("Khong the tao file PDF hop dong: " + ex.getMessage(), ex);
        }
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
        float startX = 435f;
        float startY = 58f;
        float moduleSize = 3.8f;
        int grid = 21;

        cb.saveState();
        cb.setColorStroke(Color.DARK_GRAY);
        cb.rectangle(startX - 4f, startY - 4f, grid * moduleSize + 8f, grid * moduleSize + 8f);
        cb.stroke();
        cb.setColorFill(Color.BLACK);

        for (int row = 0; row < grid; row++) {
            for (int col = 0; col < grid; col++) {
                boolean fill = isFinderFilled(row, col, grid);
                if (fill == false && !inFinderZone(row, col, grid)) {
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

    private String safeTime(Object v) {
        return v == null ? "Chua ky" : v.toString();
    }

    private String sha256Hex(byte[] bytes) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(bytes));
    }

    public record GeneratedPdf(byte[] bytes, String sha256Hex) {}
}

