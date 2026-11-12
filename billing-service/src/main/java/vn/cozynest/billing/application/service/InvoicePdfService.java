package vn.cozynest.billing.application.service;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import vn.cozynest.billing.domain.model.Invoice;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

@Service
public class InvoicePdfService {

    private static final List<String> UNICODE_FONT_CANDIDATES = List.of(
            "C:\\Windows\\Fonts\\arial.ttf",
            "C:\\Windows\\Fonts\\tahoma.ttf",
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
            "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf"
    );

    public byte[] generateReceiptPdf(Invoice invoice) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 48, 48, 48, 48);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            Font titleFont = resolveFont(16, Font.BOLD);
            Font headingFont = resolveFont(11, Font.BOLD);
            Font bodyFont = resolveFont(10, Font.NORMAL);
            NumberFormat vnd = NumberFormat.getIntegerInstance(Locale.US);

            doc.add(new Paragraph("TỔ ẤM / COZYNEST - BIÊN LAI HÓA ĐƠN TIỀN PHÒNG", titleFont));
            doc.add(new Paragraph("Mã hóa đơn: " + safe(invoice.getInvoiceCode()), headingFont));
            doc.add(new Paragraph("Trạng thái: " + safe(invoice.getStatus()) + " | Loại: " + safe(invoice.getInvoiceType()), bodyFont));
            doc.add(new Paragraph("Kỳ thanh toán: " + invoice.getPeriodMonth() + " | Hạn nộp: " + invoice.getDueDate(), bodyFont));
            doc.add(new Paragraph("Hợp đồng: " + safe(invoice.getContractId()), bodyFont));
            doc.add(new Paragraph("Bên thuê: " + safe(invoice.getTenantId()) + " | Chủ trọ: " + safe(invoice.getLandlordId()), bodyFont));
            doc.add(new Paragraph(" "));

            doc.add(new Paragraph("CHI TIẾT KHOẢN THU", headingFont));
            doc.add(new Paragraph("Tiền phòng cơ bản: " + vnd.format(invoice.getBaseRent()) + " VND", bodyFont));
            doc.add(new Paragraph("Tiền điện: " + vnd.format(invoice.getElectricityAmount()) + " VND", bodyFont));
            doc.add(new Paragraph("Tiền nước: " + vnd.format(invoice.getWaterAmount()) + " VND", bodyFont));
            doc.add(new Paragraph("Phí dịch vụ: " + vnd.format(invoice.getServiceAmount()) + " VND", bodyFont));
            doc.add(new Paragraph("Khoản khác: " + vnd.format(invoice.getOtherAmount()) + " VND", bodyFont));
            doc.add(new Paragraph("Phí trễ hạn: " + vnd.format(invoice.getLateFeeAmount()) + " VND", bodyFont));
            doc.add(new Paragraph("Khấu trừ số dư thừa kỳ trước: -" + vnd.format(invoice.getCreditAppliedAmount()) + " VND", bodyFont));
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("TỔNG CỘNG PHẢI THU: " + vnd.format(invoice.getTotalAmount()) + " VND", headingFont));
            doc.add(new Paragraph("ĐÃ THANH TOÁN: " + vnd.format(invoice.getPaidAmount()) + " VND", bodyFont));
            doc.add(new Paragraph("CÒN LẠI: " + vnd.format(Math.max(0, invoice.getTotalAmount() - invoice.getPaidAmount())) + " VND", bodyFont));
            if (invoice.getOverpaymentCreditAmount() > 0) {
                doc.add(new Paragraph("SỐ DƯ TRẢ THỪA CHUYỂN KỲ SAU: "
                        + vnd.format(invoice.getOverpaymentCreditAmount()) + " VND", bodyFont));
            }

            doc.close();
            return baos.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Khong the tao PDF bien lai hoa don: " + ex.getMessage(), ex);
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
                // Next font candidate
            }
        }
        return FontFactory.getFont(FontFactory.HELVETICA, size, style);
    }

    private String safe(String s) {
        return s == null || s.isBlank() ? "-" : s;
    }
}

