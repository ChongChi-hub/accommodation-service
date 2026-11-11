package vn.cozynest.booking.application.service;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import vn.cozynest.booking.domain.model.Contract;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.NumberFormat;
import java.util.HexFormat;
import java.util.Locale;

@Service
public class ContractPdfService {

    public GeneratedPdf generateSignedContractPdf(Contract contract) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 48, 48, 48, 48);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font headingFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            NumberFormat vnd = NumberFormat.getIntegerInstance(Locale.US);

            doc.add(new Paragraph("TO AM / COZYNEST - HOP DONG THUE PHONG DIEN TU", titleFont));
            doc.add(new Paragraph("Ma hop dong: " + safe(contract.getContractCode()), headingFont));
            doc.add(new Paragraph("Trang thai: " + safe(contract.getStatus()), bodyFont));
            doc.add(new Paragraph(" "));

            doc.add(new Paragraph("1. THONG TIN CAC BEN & PHONG THUE", headingFont));
            doc.add(new Paragraph("Ben cho thue (Landlord ID): " + safe(contract.getLandlordId()), bodyFont));
            doc.add(new Paragraph("Ben thue (Tenant ID): " + safe(contract.getTenantId()), bodyFont));
            doc.add(new Paragraph("Ma phong (Room ID): " + safe(contract.getRoomId()), bodyFont));
            doc.add(new Paragraph("Thoi han thue: " + contract.getStartDate() + " den " + contract.getEndDate(), bodyFont));
            doc.add(new Paragraph(" "));

            doc.add(new Paragraph("2. GIA THUE VA TIEN COC", headingFont));
            doc.add(new Paragraph("Gia thue hang thang: " + vnd.format(contract.getMonthlyRent()) + " VND", bodyFont));
            doc.add(new Paragraph("Tien dat coc: " + vnd.format(contract.getDepositAmount()) + " VND", bodyFont));
            doc.add(new Paragraph("Chu ky thanh toan: " + contract.getPaymentCycleMonths() + " thang/lan, han ngay "
                    + contract.getPaymentDueDay(), bodyFont));
            doc.add(new Paragraph(" "));

            doc.add(new Paragraph("3. DIEU KHOAN HOP DONG", headingFont));
            doc.add(new Paragraph(safe(contract.getTerms()), bodyFont));
            doc.add(new Paragraph(" "));

            doc.add(new Paragraph("4. CHUNG THUC KY SO OTP", headingFont));
            doc.add(new Paragraph("Thoi gian nguoi thue ky: " + safeTime(contract.getTenantSignedAt()), bodyFont));
            doc.add(new Paragraph("Thoi gian chu tro ky: " + safeTime(contract.getLandlordSignedAt()), bodyFont));
            doc.add(new Paragraph("Ma bam noi dung (SHA-256): " + safe(contract.getDocumentHash()), bodyFont));
            doc.add(new Paragraph("Verification Code: CN-" + safe(contract.getContractCode()), bodyFont));

            doc.close();
            byte[] pdfBytes = baos.toByteArray();
            return new GeneratedPdf(pdfBytes, sha256Hex(pdfBytes));
        } catch (Exception ex) {
            throw new IllegalStateException("Khong the tao file PDF hop dong: " + ex.getMessage(), ex);
        }
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

