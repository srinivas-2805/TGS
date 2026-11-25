package com.tgs.school.reports.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class PdfGeneratorService {

    private static final Color NAVY = new Color(15, 41, 74);
    private static final Color ROYAL = new Color(29, 78, 216);
    private static final Color GOLD = new Color(245, 158, 11);
    private static final Color LIGHT_BG = new Color(241, 245, 249);
    private static final Color BORDER = new Color(203, 213, 225);

    public record ReportCardRow(
            String subjectName,
            boolean activityBased,
            String maxMarks,
            String marksObtained,
            String activityRating,
            String grade,
            String remarks
    ) {}

    public record ReportCardData(
            String schoolName,
            String schoolLocation,
            String examName,
            String academicYear,
            String studentName,
            String admissionNumber,
            String className,
            String sectionName,
            String rollNumber,
            boolean isActivityGradedClass,
            List<ReportCardRow> rows,
            String totalMarksText,
            String percentageText,
            String overallGrade,
            String teacherRemarks
    ) {}

    public record FeeReceiptData(
            String receiptNumber,
            String paidAt,
            String studentName,
            String admissionNumber,
            String className,
            String feeName,
            String paymentMode,
            String transactionRef,
            String amountPaid,
            String remainingBalance
    ) {}

    public record IdCardData(
            String admissionNumber,
            String studentName,
            String className,
            String sectionName,
            String dob,
            String bloodGroup,
            String parentName,
            String parentPhone,
            String academicYear
    ) {}

    public byte[] generateReportCardPdf(ReportCardData data) {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontReg = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float w = page.getMediaBox().getWidth();
                float y = page.getMediaBox().getHeight() - 40;

                // Header banner
                cs.setNonStrokingColor(NAVY);
                cs.addRect(36, y - 64, w - 72, 68);
                cs.fill();

                cs.setNonStrokingColor(GOLD);
                cs.addRect(36, y - 68, w - 72, 4);
                cs.fill();

                // Crest icon emblem (geometric book + sun representation)
                cs.setNonStrokingColor(GOLD);
                cs.addRect(50, y - 48, 34, 34);
                cs.fill();
                cs.setNonStrokingColor(NAVY);
                drawText(cs, fontBold, 11, 55, y - 35, "TGS");

                cs.setNonStrokingColor(Color.WHITE);
                drawText(cs, fontBold, 16, 98, y - 24, safe(data.schoolName()));
                drawText(cs, fontReg, 10, 98, y - 40, safe(data.schoolLocation()));
                drawText(cs, fontBold, 11, 98, y - 56, "OFFICIAL PROGRESS REPORT CARD • " + safe(data.academicYear()));

                y -= 92;
                cs.setNonStrokingColor(NAVY);
                drawText(cs, fontBold, 13, 40, y, safe(data.examName()));

                // Student Info box
                y -= 16;
                cs.setNonStrokingColor(LIGHT_BG);
                cs.addRect(36, y - 58, w - 72, 58);
                cs.fill();
                cs.setStrokingColor(BORDER);
                cs.addRect(36, y - 58, w - 72, 58);
                cs.stroke();

                cs.setNonStrokingColor(Color.BLACK);
                drawText(cs, fontBold, 10, 48, y - 18, "Student Name: ");
                drawText(cs, fontReg, 10, 128, y - 18, safe(data.studentName()));
                drawText(cs, fontBold, 10, 330, y - 18, "Admission No: ");
                drawText(cs, fontReg, 10, 412, y - 18, safe(data.admissionNumber()));

                drawText(cs, fontBold, 10, 48, y - 38, "Class & Section: ");
                drawText(cs, fontReg, 10, 135, y - 38, safe(data.className()) + " - " + safe(data.sectionName()));
                drawText(cs, fontBold, 10, 330, y - 38, "Roll Number: ");
                drawText(cs, fontReg, 10, 412, y - 38, safe(data.rollNumber()));

                y -= 80;

                // Table header
                cs.setNonStrokingColor(ROYAL);
                cs.addRect(36, y - 22, w - 72, 22);
                cs.fill();
                cs.setNonStrokingColor(Color.WHITE);
                drawText(cs, fontBold, 9.5f, 44, y - 15, "Subject");
                if (data.isActivityGradedClass()) {
                    drawText(cs, fontBold, 9.5f, 220, y - 15, "Activity Rating");
                    drawText(cs, fontBold, 9.5f, 340, y - 15, "Descriptive Remarks");
                } else {
                    drawText(cs, fontBold, 9.5f, 220, y - 15, "Max Marks");
                    drawText(cs, fontBold, 9.5f, 295, y - 15, "Obtained");
                    drawText(cs, fontBold, 9.5f, 365, y - 15, "Grade");
                    drawText(cs, fontBold, 9.5f, 420, y - 15, "Remarks");
                }

                y -= 22;
                boolean alt = false;
                for (ReportCardRow row : data.rows()) {
                    if (y < 140) break;
                    if (alt) {
                        cs.setNonStrokingColor(LIGHT_BG);
                        cs.addRect(36, y - 22, w - 72, 22);
                        cs.fill();
                    }
                    cs.setStrokingColor(BORDER);
                    cs.addRect(36, y - 22, w - 72, 22);
                    cs.stroke();

                    cs.setNonStrokingColor(Color.BLACK);
                    drawText(cs, fontReg, 9.5f, 44, y - 15, truncate(safe(row.subjectName()), 28));
                    if (data.isActivityGradedClass() || row.activityBased()) {
                        drawText(cs, fontBold, 9.5f, 220, y - 15, safe(row.activityRating()));
                        drawText(cs, fontReg, 9f, 340, y - 15, truncate(safe(row.remarks()), 38));
                    } else {
                        drawText(cs, fontReg, 9.5f, 225, y - 15, safe(row.maxMarks()));
                        drawText(cs, fontReg, 9.5f, 300, y - 15, safe(row.marksObtained()));
                        drawText(cs, fontBold, 9.5f, 370, y - 15, safe(row.grade()));
                        drawText(cs, fontReg, 9f, 420, y - 15, truncate(safe(row.remarks()), 26));
                    }
                    y -= 22;
                    alt = !alt;
                }

                // Summary box
                y -= 24;
                if (!data.isActivityGradedClass()) {
                    cs.setNonStrokingColor(LIGHT_BG);
                    cs.addRect(36, y - 34, w - 72, 34);
                    cs.fill();
                    cs.setNonStrokingColor(NAVY);
                    drawText(cs, fontBold, 10, 46, y - 21, "Total Marks: " + safe(data.totalMarksText()));
                    drawText(cs, fontBold, 10, 230, y - 21, "Percentage: " + safe(data.percentageText()));
                    drawText(cs, fontBold, 10, 400, y - 21, "Overall Grade: " + safe(data.overallGrade()));
                    y -= 50;
                }

                cs.setNonStrokingColor(Color.BLACK);
                drawText(cs, fontBold, 10, 40, y, "Class Teacher Remarks:");
                drawText(cs, fontReg, 9.5f, 40, y - 16, truncate(safe(data.teacherRemarks()), 95));

                // Signatures
                float sigY = 85;
                cs.setStrokingColor(Color.DARK_GRAY);
                cs.moveTo(46, sigY + 18); cs.lineTo(186, sigY + 18); cs.stroke();
                cs.moveTo(230, sigY + 18); cs.lineTo(370, sigY + 18); cs.stroke();
                cs.moveTo(410, sigY + 18); cs.lineTo(550, sigY + 18); cs.stroke();

                drawText(cs, fontBold, 9, 68, sigY, "Class Teacher");
                drawText(cs, fontBold, 9, 255, sigY, "Parent / Guardian");
                drawText(cs, fontBold, 9, 430, sigY, "Principal Signature");
            }

            doc.save(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to generate Report Card PDF", ex);
        }
    }

    public byte[] generateFeeReceiptPdf(FeeReceiptData data) {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A5);
            doc.addPage(page);
            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontReg = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float w = page.getMediaBox().getWidth();
                float y = page.getMediaBox().getHeight() - 30;

                cs.setNonStrokingColor(NAVY);
                cs.addRect(24, y - 54, w - 48, 54);
                cs.fill();
                cs.setNonStrokingColor(Color.WHITE);
                drawText(cs, fontBold, 13, 36, y - 22, "TELANGANA GRAMMAR SCHOOL");
                drawText(cs, fontReg, 9, 36, y - 38, "Kalluru, Khammam, Telangana • FEE PAYMENT RECEIPT");

                y -= 78;
                cs.setNonStrokingColor(Color.BLACK);
                drawText(cs, fontBold, 10, 28, y, "Receipt No: " + safe(data.receiptNumber()));
                drawText(cs, fontReg, 9.5f, 240, y, "Date: " + safe(data.paidAt()));

                y -= 28;
                String[][] fields = {
                        {"Student Name", safe(data.studentName())},
                        {"Admission Number", safe(data.admissionNumber())},
                        {"Class", safe(data.className())},
                        {"Fee Particular", safe(data.feeName())},
                        {"Payment Mode", safe(data.paymentMode())},
                        {"Transaction Ref", safe(data.transactionRef())},
                        {"Amount Paid (INR)", "Rs. " + safe(data.amountPaid())},
                        {"Remaining Balance", "Rs. " + safe(data.remainingBalance())}
                };
                for (String[] f : fields) {
                    cs.setStrokingColor(BORDER);
                    cs.addRect(28, y - 18, w - 56, 22);
                    cs.stroke();
                    drawText(cs, fontBold, 9.5f, 36, y - 11, f[0]);
                    drawText(cs, fontReg, 9.5f, 180, y - 11, f[1]);
                    y -= 22;
                }

                drawText(cs, fontReg, 8.5f, 28, 42, "This is a system-generated fee receipt from Telangana Grammar School ERP.");
            }
            doc.save(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to generate Fee Receipt PDF", ex);
        }
    }

    public byte[] generateStudentIdCardPdf(IdCardData data) {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(new PDRectangle(260, 380));
            doc.addPage(page);
            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontReg = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.setNonStrokingColor(NAVY);
                cs.addRect(0, 310, 260, 70);
                cs.fill();
                cs.setNonStrokingColor(GOLD);
                cs.addRect(0, 306, 260, 4);
                cs.fill();

                cs.setNonStrokingColor(Color.WHITE);
                drawText(cs, fontBold, 11, 20, 352, "TELANGANA GRAMMAR SCHOOL");
                drawText(cs, fontReg, 8, 42, 336, "Kalluru, Khammam, Telangana");
                drawText(cs, fontBold, 8.5f, 65, 320, "STUDENT IDENTITY CARD (" + safe(data.academicYear()) + ")");

                cs.setStrokingColor(ROYAL);
                cs.addRect(90, 220, 80, 74);
                cs.stroke();
                cs.setNonStrokingColor(Color.GRAY);
                drawText(cs, fontReg, 8, 108, 254, "PHOTO");

                cs.setNonStrokingColor(NAVY);
                drawText(cs, fontBold, 12, 24, 195, safe(data.studentName()));
                cs.setNonStrokingColor(ROYAL);
                drawText(cs, fontBold, 9.5f, 24, 178, "Adm No: " + safe(data.admissionNumber()));

                cs.setNonStrokingColor(Color.BLACK);
                drawText(cs, fontReg, 9, 24, 154, "Class: " + safe(data.className()) + " - " + safe(data.sectionName()));
                drawText(cs, fontReg, 9, 24, 136, "DOB: " + safe(data.dob()));
                drawText(cs, fontReg, 9, 24, 118, "Blood Group: " + safe(data.bloodGroup()));
                drawText(cs, fontReg, 9, 24, 100, "Parent: " + truncate(safe(data.parentName()), 28));
                drawText(cs, fontReg, 9, 24, 82, "Contact: " + safe(data.parentPhone()));

                cs.setNonStrokingColor(NAVY);
                cs.addRect(0, 0, 260, 30);
                cs.fill();
                cs.setNonStrokingColor(Color.WHITE);
                drawText(cs, fontReg, 8, 60, 12, "Principal Signature • TGS Kalluru");
            }
            doc.save(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to generate Student ID Card PDF", ex);
        }
    }

    public byte[] generateSimpleTablePdf(String title, String subtitle, List<String> headers, List<List<String>> rows) {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontReg = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float w = page.getMediaBox().getWidth();
                float y = page.getMediaBox().getHeight() - 40;
                cs.setNonStrokingColor(NAVY);
                cs.addRect(30, y - 48, w - 60, 48);
                cs.fill();
                cs.setNonStrokingColor(Color.WHITE);
                drawText(cs, fontBold, 13, 40, y - 20, "TELANGANA GRAMMAR SCHOOL - " + safe(title));
                drawText(cs, fontReg, 9, 40, y - 36, safe(subtitle));

                y -= 70;
                float colWidth = (w - 60) / Math.max(1, headers.size());
                cs.setNonStrokingColor(ROYAL);
                cs.addRect(30, y - 20, w - 60, 20);
                cs.fill();
                cs.setNonStrokingColor(Color.WHITE);
                for (int i = 0; i < headers.size(); i++) {
                    drawText(cs, fontBold, 8.5f, 34 + i * colWidth, y - 13, truncate(headers.get(i), 18));
                }
                y -= 20;

                cs.setNonStrokingColor(Color.BLACK);
                for (List<String> row : rows) {
                    if (y < 50) break;
                    cs.setStrokingColor(BORDER);
                    cs.addRect(30, y - 18, w - 60, 18);
                    cs.stroke();
                    for (int i = 0; i < row.size() && i < headers.size(); i++) {
                        drawText(cs, fontReg, 8f, 34 + i * colWidth, y - 12, truncate(safe(row.get(i)), 20));
                    }
                    y -= 18;
                }
            }
            doc.save(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to generate PDF report", ex);
        }
    }

    private void drawText(PDPageContentStream cs, PDType1Font font, float size, float x, float y, String text) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(sanitizePdfText(text));
        cs.endText();
    }

    private static String sanitizePdfText(String text) {
        if (text == null) return "";
        return text.replaceAll("[^\\x20-\\x7E]", " ");
    }

    private static String safe(String val) {
        return val == null || val.isBlank() ? "-" : val;
    }

    private static String truncate(String val, int max) {
        return val.length() > max ? val.substring(0, max - 2) + ".." : val;
    }
}
