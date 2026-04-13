package com.zs.ytbx.common.export;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DeflaterOutputStream;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

public final class SimplePdfWriter {

    private static final int PAGE_WIDTH = 1240;
    private static final int LEFT_MARGIN = 72;
    private static final int TOP_MARGIN = 76;
    private static final int BOTTOM_MARGIN = 72;
    private static final int LABEL_WIDTH = 190;
    private static final int BODY_WIDTH = PAGE_WIDTH - LEFT_MARGIN * 2 - LABEL_WIDTH - 24;

    private SimplePdfWriter() {
    }

    public static byte[] writePages(List<BufferedImage> images) {
        if (images == null || images.isEmpty()) {
            throw new IllegalArgumentException("PDF页不能为空");
        }

        try {
            List<PdfPage> pages = new ArrayList<>();
            for (int i = 0; i < images.size(); i++) {
                pages.add(PdfPage.fromImage(3 + i * 3, images.get(i)));
            }
            return buildPdf(pages);
        } catch (IOException e) {
            throw new IllegalStateException("生成PDF失败", e);
        }
    }

    public static byte[] writePolicyPages(List<PolicyPage> pages) {
        if (pages == null || pages.isEmpty()) {
            throw new IllegalArgumentException("PDF页不能为空");
        }

        try (PDDocument document = new PDDocument()) {
            PDType0Font font = loadChineseFont(document);
            for (PolicyPage pageData : pages) {
                BufferedImage image = pageData.image();
                PDPage page = new PDPage(new PDRectangle(image.getWidth(), image.getHeight()));
                document.addPage(page);
                try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                    drawPolicyTextPage(contentStream, font, pageData.hiddenText(), image.getWidth(), image.getHeight());
                }
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("生成PDF失败", e);
        }
    }

    private static void drawPolicyTextPage(PDPageContentStream cs,
                                           PDType0Font font,
                                           List<String> lines,
                                           int pageWidth,
                                           int pageHeight) throws IOException {
        final float left = 72f;
        final float right = 72f;
        final float top = 76f;

        List<String> safeLines = lines == null ? List.of() : lines;
        List<String> introLines = new ArrayList<>();
        List<String> summaryLines = new ArrayList<>();
        List<String> coverageLines = new ArrayList<>();
        List<String> noteLines = new ArrayList<>();
        String title = "";
        String subtitle = "";
        String introLabel = "尊敬的客户：";
        int mode = 0;

        for (String raw : safeLines) {
            if (raw == null) continue;
            String line = raw.trim();
            if (line.isEmpty()) continue;
            if (line.startsWith("__TITLE__:")) { title = line.substring(10); continue; }
            if (line.startsWith("__SUBTITLE__:")) { subtitle = line.substring(13); continue; }
            if (line.startsWith("__INTRO_LABEL__:")) { introLabel = line.substring(16); continue; }
            if (line.equals("__SECTION_SUMMARY__")) { mode = 1; continue; }
            if (line.equals("__SECTION_COVERAGE__")) { mode = 2; continue; }
            if (line.equals("__SECTION_NOTES__")) { mode = 3; continue; }
            if (line.equals("__SECTION_FOOTER__")) { mode = 4; continue; }
            switch (mode) {
                case 0 -> introLines.add(line);
                case 1 -> summaryLines.add(line);
                case 2 -> coverageLines.add(line);
                case 3, 4 -> noteLines.add(line);
                default -> noteLines.add(line);
            }
        }

        cs.setNonStrokingColor(Color.WHITE);
        cs.addRect(0, 0, pageWidth, pageHeight);
        cs.fill();

        float y = pageHeight - top;
        drawCenteredText(cs, font, 20, title, pageWidth / 2f, y, new Color(0x0F, 0x1F, 0x33));
        y -= 26f;
        cs.setStrokingColor(new Color(0xE6, 0x10, 0x21));
        cs.setLineWidth(3f);
        cs.moveTo(left, y);
        cs.lineTo(pageWidth - right, y);
        cs.stroke();
        y -= 22f;
        drawCenteredText(cs, font, 11.5f, subtitle, pageWidth / 2f, y, new Color(0x33, 0x33, 0x33));
        y -= 26f;

        y = drawWrappedParagraph(cs, font, 12, introLabel, left, y, pageWidth - left - right, true, new Color(0, 0, 0));
        y = drawWrappedParagraph(cs, font, 11, String.join("\n", introLines), left + 28f, y - 4f, pageWidth - left - right - 28f, false, new Color(0, 0, 0));

        y -= 10f;
        cs.setStrokingColor(new Color(0x22, 0x22, 0x22));
        cs.setLineWidth(1f);
        cs.moveTo(left, y);
        cs.lineTo(pageWidth - right, y);
        cs.stroke();
        y -= 22f;

        y = drawSectionTitle(cs, font, "保单信息", left, y, 13, new Color(0x0F, 0x1F, 0x33));
        y = drawSummaryBlock(cs, font, summaryLines, left, y, pageWidth - left - right);

        y -= 8f;
        y = drawSectionTitle(cs, font, "保障说明", left, y, 13, new Color(0x0F, 0x1F, 0x33));
        y = drawCoverageBlock(cs, font, coverageLines, left, y, pageWidth - left - right);

        y -= 10f;
        y = drawNotesBlock(cs, font, noteLines, left, y, pageWidth - left - right);
    }

    private static float drawSummaryBlock(PDPageContentStream cs, PDType0Font font, List<String> lines, float left, float y, float width) throws IOException {
        if (lines.isEmpty()) return y;
        List<String> rendered = new ArrayList<>();
        for (String line : lines) {
            rendered.add(line);
        }
        float rowHeight = 23f;
        float colGap = 24f;
        float colWidth = (width - colGap) / 2f;
        float labelWidth = 108f;
        for (int i = 0; i < rendered.size(); i += 2) {
            String leftLine = rendered.get(i);
            String rightLine = i + 1 < rendered.size() ? rendered.get(i + 1) : null;
            float blockHeight = rowHeight;
            if (leftLine.contains("：")) {
                String[] kv = leftLine.split("：", 2);
                blockHeight = Math.max(blockHeight, measureWrappedHeight(font, 11, kv[1], colWidth - labelWidth));
            }
            if (rightLine != null && rightLine.contains("：")) {
                String[] kv = rightLine.split("：", 2);
                blockHeight = Math.max(blockHeight, measureWrappedHeight(font, 11, kv[1], colWidth - labelWidth));
            }
            drawKeyValue(cs, font, 11, leftLine, left, y, labelWidth, colWidth - labelWidth);
            if (rightLine != null) {
                drawKeyValue(cs, font, 11, rightLine, left + colWidth + colGap, y, labelWidth, colWidth - labelWidth);
            }
            y -= blockHeight + 4f;
        }
        return y;
    }

    private static float drawCoverageBlock(PDPageContentStream cs, PDType0Font font, List<String> lines, float left, float y, float width) throws IOException {
        if (lines.isEmpty()) return y;
        float tableLabelWidth = width * 0.60f;
        float tableValueWidth = width - tableLabelWidth;
        float headerHeight = 24f;
        cs.setNonStrokingColor(new Color(0xF5, 0xF6, 0xF8));
        cs.addRect(left, y - headerHeight, width, headerHeight);
        cs.fill();
        cs.setStrokingColor(new Color(0x55, 0x55, 0x55));
        cs.addRect(left, y - headerHeight, width, headerHeight);
        cs.stroke();
        cs.moveTo(left + tableLabelWidth, y - headerHeight);
        cs.lineTo(left + tableLabelWidth, y);
        cs.stroke();
        drawText(cs, font, 11.5f, "保障内容", left + 12, y - 16, new Color(0x0F, 0x1F, 0x33));
        drawText(cs, font, 11.5f, "保险金额", left + tableLabelWidth + 12, y - 16, new Color(0x0F, 0x1F, 0x33));
        y -= headerHeight;

        for (String line : lines) {
            if (line.startsWith("##")) break;
            String cleaned = line.replaceAll("^[0-9]+[、.．)]?\\s*", "");
            String[] kv = cleaned.split("：", 2);
            String label = kv.length > 0 ? kv[0].trim() : cleaned;
            String value = kv.length > 1 ? kv[1].trim() : "";
            float rowHeight = Math.max(measureWrappedHeight(font, 10.5f, label, tableLabelWidth - 18), measureWrappedHeight(font, 10.5f, value, tableValueWidth - 18)) + 6f;
            cs.setNonStrokingColor(Color.WHITE);
            cs.addRect(left, y - rowHeight, width, rowHeight);
            cs.fill();
            cs.setStrokingColor(new Color(0x55, 0x55, 0x55));
            cs.addRect(left, y - rowHeight, width, rowHeight);
            cs.stroke();
            cs.moveTo(left + tableLabelWidth, y - rowHeight);
            cs.lineTo(left + tableLabelWidth, y);
            cs.stroke();
            drawWrappedParagraph(cs, font, 10.5f, label, left + 12, y - 13, tableLabelWidth - 18, false, new Color(0, 0, 0));
            drawWrappedParagraph(cs, font, 10.5f, value, left + tableLabelWidth + 12, y - 13, tableValueWidth - 18, false, new Color(0, 0, 0));
            y -= rowHeight;
        }
        return y;
    }

    private static float drawNotesBlock(PDPageContentStream cs, PDType0Font font, List<String> lines, float left, float y, float width) throws IOException {
        if (lines.isEmpty()) return y;
        float currentY = y;
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            if (line.startsWith("##")) {
                String heading = line.replaceFirst("^#{2,3}\\s*", "");
                currentY = drawSectionTitle(cs, font, heading, left, currentY, 12.2f, new Color(0x0F, 0x1F, 0x33));
                continue;
            }
            if (line.matches("^[0-9]+[、.．)].*")) {
                currentY = drawNumberedParagraph(cs, font, 10.5f, line, left, currentY, width, new Color(0, 0, 0));
                continue;
            }
            if (line.startsWith("（")) {
                currentY = drawWrappedParagraph(cs, font, 10.2f, line, left, currentY, width, false, new Color(0, 0, 0));
                continue;
            }
            currentY = drawWrappedParagraph(cs, font, 10.2f, line, left, currentY, width, false, new Color(0, 0, 0));
        }
        return currentY;
    }

    private static void drawKeyValue(PDPageContentStream cs, PDType0Font font, float fontSize, String line, float x, float y, float labelWidth, float valueWidth) throws IOException {
        String[] kv = line.split("：", 2);
        String label = kv[0];
        String value = kv.length > 1 ? kv[1] : "";
        drawText(cs, font, fontSize, label + "：", x, y, new Color(0x0F, 0x1F, 0x33));
        drawWrappedParagraph(cs, font, fontSize, value, x + labelWidth, y, valueWidth, false, new Color(0, 0, 0));
    }

    private static float drawSectionTitle(PDPageContentStream cs, PDType0Font font, String title, float left, float y, float fontSize, Color color) throws IOException {
        drawText(cs, font, fontSize, title, left, y, color);
        return y - (fontSize + 10f);
    }

    private static float drawWrappedParagraph(PDPageContentStream cs,
                                              PDType0Font font,
                                              float fontSize,
                                              String text,
                                              float x,
                                              float y,
                                              float width,
                                              boolean numbered,
                                              Color color) throws IOException {
        List<String> wrapped = wrapPdfText(font, fontSize, text, width);
        float lineHeight = fontSize * 1.45f;
        float currentY = y;
        for (int i = 0; i < wrapped.size(); i++) {
            drawText(cs, font, fontSize, wrapped.get(i), x, currentY, color);
            currentY -= lineHeight;
        }
        return currentY - 4f;
    }

    private static float drawNumberedParagraph(PDPageContentStream cs,
                                                PDType0Font font,
                                                float fontSize,
                                                String text,
                                                float x,
                                                float y,
                                                float width,
                                                Color color) throws IOException {
        String trimmed = text == null ? "" : text.trim();
        int separatorIndex = trimmed.indexOf('、');
        if (separatorIndex < 0) {
            separatorIndex = trimmed.indexOf('.');
        }
        String prefix = separatorIndex >= 0 ? trimmed.substring(0, separatorIndex + 1) : "1、";
        String body = separatorIndex >= 0 ? trimmed.substring(separatorIndex + 1).trim() : trimmed;
        float prefixWidth = measureTextWidth(font, fontSize, prefix) + 6f;
        drawText(cs, font, fontSize, prefix, x, y, color);
        return drawWrappedParagraph(cs, font, fontSize, body, x + prefixWidth, y, width - prefixWidth, false, color);
    }

    private static List<String> wrapPdfText(PDType0Font font, float fontSize, String text, float maxWidth) throws IOException {
        String value = text == null ? "" : text.trim();
        if (value.isEmpty()) {
            return List.of();
        }
        List<String> lines = new ArrayList<>();
        for (String paragraph : value.split("\\r?\\n")) {
            String p = paragraph.trim();
            if (p.isEmpty()) continue;
            StringBuilder current = new StringBuilder();
            for (int i = 0; i < p.length(); i++) {
                char ch = p.charAt(i);
                String candidate = current.toString() + ch;
                if (measureTextWidth(font, fontSize, candidate) > maxWidth && current.length() > 0) {
                    lines.add(current.toString());
                    current.setLength(0);
                }
                current.append(ch);
            }
            if (current.length() > 0) {
                lines.add(current.toString());
            }
        }
        return lines;
    }

    private static float measureWrappedHeight(PDType0Font font, float fontSize, String text, float width) throws IOException {
        List<String> lines = wrapPdfText(font, fontSize, text, width);
        return Math.max(fontSize * 1.2f, lines.size() * fontSize * 1.45f);
    }

    private static float measureTextWidth(PDType0Font font, float fontSize, String text) throws IOException {
        return font.getStringWidth(text) / 1000f * fontSize;
    }

    private static float measureGraphicsTextWidth(Graphics2D g, Font font, String text) {
        FontMetrics metrics = g.getFontMetrics(font);
        return metrics.stringWidth(text == null ? "" : text);
    }

    private static void drawText(PDPageContentStream cs, PDType0Font font, float fontSize, String text, float x, float y, Color color) throws IOException {
        cs.beginText();
        cs.setNonStrokingColor(color);
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
    }

    private static void drawCenteredText(PDPageContentStream cs, PDType0Font font, float fontSize, String text, float centerX, float y, Color color) throws IOException {
        float width = measureTextWidth(font, fontSize, text);
        drawText(cs, font, fontSize, text, centerX - width / 2f, y, color);
    }

    public static BufferedImage renderDetailPage(String title, List<FieldLine> fieldLines, String subtitle) {
        Font titleFont = new Font("SansSerif", Font.BOLD, 28);
        Font subtitleFont = new Font("SansSerif", Font.PLAIN, 15);
        Font labelFont = new Font("SansSerif", Font.BOLD, 16);
        Font valueFont = new Font("SansSerif", Font.PLAIN, 16);

        BufferedImage probeImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        Graphics2D probe = probeImage.createGraphics();
        applyQualityHints(probe);
        int titleHeight = probe.getFontMetrics(titleFont).getHeight();
        int subtitleHeight = probe.getFontMetrics(subtitleFont).getHeight();
        int lineHeight = probe.getFontMetrics(valueFont).getHeight();
        int labelHeight = probe.getFontMetrics(labelFont).getHeight();
        probe.dispose();

        List<List<String>> wrappedValues = new ArrayList<>();
        int height = TOP_MARGIN + titleHeight + 14 + subtitleHeight + 28;
        for (FieldLine fieldLine : fieldLines) {
            List<String> wrapped = wrapText(fieldLine.value(), valueFont, BODY_WIDTH);
            wrappedValues.add(wrapped);
            height += Math.max(labelHeight, lineHeight * wrapped.size()) + 14;
        }
        height += BOTTOM_MARGIN;

        BufferedImage image = new BufferedImage(PAGE_WIDTH, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        applyQualityHints(g);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, PAGE_WIDTH, height);

        g.setColor(new Color(0x00, 0x3B, 0x72));
        g.fillRoundRect(LEFT_MARGIN, 28, 92, 26, 14, 14);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.drawString("YTBX PDF", LEFT_MARGIN + 16, 46);

        g.setColor(new Color(0x0F, 0x1F, 0x33));
        g.setFont(titleFont);
        g.drawString(title, LEFT_MARGIN, TOP_MARGIN);

        g.setColor(new Color(0x63, 0x6B, 0x74));
        g.setFont(subtitleFont);
        g.drawString(subtitle, LEFT_MARGIN, TOP_MARGIN + titleHeight + 8);

        int y = TOP_MARGIN + titleHeight + 28 + subtitleHeight;
        g.setColor(new Color(0xEA, 0xEC, 0xF0));
        g.fillRect(LEFT_MARGIN, y, PAGE_WIDTH - LEFT_MARGIN * 2, 1);
        y += 30;

        g.setFont(labelFont);
        for (int i = 0; i < fieldLines.size(); i++) {
            FieldLine fieldLine = fieldLines.get(i);
            List<String> wrapped = wrappedValues.get(i);
            int blockHeight = Math.max(labelHeight, lineHeight * wrapped.size());

            g.setColor(new Color(0x0F, 0x1F, 0x33));
            g.drawString(fieldLine.label(), LEFT_MARGIN, y + labelHeight - 6);

            g.setFont(valueFont);
            int valueY = y + lineHeight - 6;
            int valueX = LEFT_MARGIN + LABEL_WIDTH;
            for (String line : wrapped) {
                g.drawString(line, valueX, valueY);
                valueY += lineHeight;
            }

            g.setFont(labelFont);
            y += blockHeight + 14;
        }

        g.dispose();
        return image;
    }

    public static BufferedImage renderInsurancePolicyPage(String title,
                                                          String subtitle,
                                                          String introLabel,
                                                          String introText,
                                                          List<FieldLine> summaryFields,
                                                          List<FieldLine> coverageRows,
                                                          List<String> specialNotes,
                                                          List<String> footerLines) {
        Font titleFont = new Font("SansSerif", Font.BOLD, 30);
        Font subtitleFont = new Font("SansSerif", Font.PLAIN, 15);
        Font introLabelFont = new Font("SansSerif", Font.BOLD, 16);
        Font introBodyFont = new Font("SansSerif", Font.PLAIN, 15);
        Font sectionFont = new Font("SansSerif", Font.BOLD, 17);
        Font labelFont = new Font("SansSerif", Font.BOLD, 15);
        Font valueFont = new Font("SansSerif", Font.PLAIN, 15);
        Font tableHeadFont = new Font("SansSerif", Font.BOLD, 15);
        Font tableBodyFont = new Font("SansSerif", Font.PLAIN, 15);
        Font noteFont = new Font("SansSerif", Font.PLAIN, 14);
        Font footerFont = new Font("SansSerif", Font.PLAIN, 14);

        BufferedImage probeImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        Graphics2D probe = probeImage.createGraphics();
        applyQualityHints(probe);
        int titleHeight = probe.getFontMetrics(titleFont).getHeight();
        int subtitleHeight = probe.getFontMetrics(subtitleFont).getHeight();
        int introLabelHeight = probe.getFontMetrics(introLabelFont).getHeight();
        int introBodyLineHeight = probe.getFontMetrics(introBodyFont).getHeight();
        int sectionHeight = probe.getFontMetrics(sectionFont).getHeight();
        int labelHeight = probe.getFontMetrics(labelFont).getHeight();
        int valueLineHeight = probe.getFontMetrics(valueFont).getHeight();
        int tableHeadHeight = probe.getFontMetrics(tableHeadFont).getHeight();
        int tableBodyLineHeight = probe.getFontMetrics(tableBodyFont).getHeight();
        int noteLineHeight = probe.getFontMetrics(noteFont).getHeight();
        int footerLineHeight = probe.getFontMetrics(footerFont).getHeight();
        probe.dispose();

        int contentWidth = PAGE_WIDTH - LEFT_MARGIN * 2;
        int halfGap = 24;
        int columnWidth = (contentWidth - halfGap) / 2;
        int valueWidth = columnWidth - LABEL_WIDTH;
        int introWidth = contentWidth - 36;
        int tableLabelWidth = (int) (contentWidth * 0.56);
        int tableValueWidth = contentWidth - tableLabelWidth;

        List<String> introLines = wrapParagraphText(introText, introBodyFont, introWidth);
        List<List<String>> wrappedSummaryValues = new ArrayList<>();
        int summaryHeight = 0;
        List<FieldLine> summaryList = summaryFields == null ? Collections.emptyList() : summaryFields;
        for (FieldLine fieldLine : summaryList) {
            List<String> wrapped = wrapText(fieldLine.value(), valueFont, valueWidth);
            wrappedSummaryValues.add(wrapped);
        }
        for (int i = 0; i < summaryList.size(); i += 2) {
            List<String> left = wrappedSummaryValues.get(i);
            List<String> right = i + 1 < wrappedSummaryValues.size() ? wrappedSummaryValues.get(i + 1) : List.of("-");
            int leftHeight = Math.max(labelHeight, valueLineHeight * left.size());
            int rightHeight = Math.max(labelHeight, valueLineHeight * right.size());
            summaryHeight += Math.max(leftHeight, rightHeight) + 12;
        }

        List<FieldLine> coverageList = coverageRows == null ? Collections.emptyList() : coverageRows;
        List<List<String>> wrappedCoverageLabels = new ArrayList<>();
        List<List<String>> wrappedCoverageValues = new ArrayList<>();
        int coverageHeight = tableHeadHeight + 16;
        for (FieldLine row : coverageList) {
            List<String> left = wrapText(row.label(), tableBodyFont, tableLabelWidth - 24);
            List<String> right = wrapText(row.value(), tableBodyFont, tableValueWidth - 24);
            wrappedCoverageLabels.add(left);
            wrappedCoverageValues.add(right);
            int rowHeight = Math.max(left.size(), right.size()) * tableBodyLineHeight + 16;
            coverageHeight += rowHeight;
        }

        List<String> noteList = specialNotes == null ? Collections.emptyList() : specialNotes;
        List<List<String>> wrappedNotes = new ArrayList<>();
        int notesHeight = 0;
        for (int i = 0; i < noteList.size(); i++) {
            String note = normalizeNoteText(noteList.get(i), i + 1);
            Font activeFont = isHeadingLine(note) ? sectionFont : noteFont;
            int noteWidth = isHeadingLine(note) ? contentWidth : contentWidth - 28;
            List<String> wrapped = wrapText(stripHeadingPrefix(note), activeFont, noteWidth);
            wrappedNotes.add(wrapped);
            notesHeight += isHeadingLine(note)
                    ? sectionHeight + 8
                    : Math.max(noteLineHeight, wrapped.size() * noteLineHeight) + 6;
        }

        List<String> footerList = footerLines == null ? Collections.emptyList() : footerLines;
        List<List<String>> wrappedFooters = new ArrayList<>();
        int footerHeight = 0;
        for (String footer : footerList) {
            List<String> wrapped = wrapText(footer, footerFont, contentWidth);
            wrappedFooters.add(wrapped);
            footerHeight += wrapped.size() * footerLineHeight + 4;
        }

        int introHeight = introLabelHeight + introLines.size() * introBodyLineHeight + 12;
        int totalHeight = TOP_MARGIN + titleHeight + subtitleHeight + introHeight + 28
                + sectionHeight + summaryHeight + 28
                + sectionHeight + coverageHeight + 30
                + (noteList.isEmpty() ? 0 : (sectionHeight + notesHeight + 18))
                + footerHeight + BOTTOM_MARGIN + 120;

        BufferedImage image = new BufferedImage(PAGE_WIDTH, totalHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        applyQualityHints(g);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, PAGE_WIDTH, totalHeight);

        g.setColor(new Color(0x0F, 0x1F, 0x33));
        g.setFont(titleFont);
        drawCenteredString(g, title, PAGE_WIDTH / 2, TOP_MARGIN + titleHeight - 4);

        g.setColor(new Color(0xE6, 0x10, 0x21));
        g.fillRect(LEFT_MARGIN, TOP_MARGIN + titleHeight + 14, contentWidth, 4);

        g.setColor(new Color(0x0F, 0x1F, 0x33));
        g.setFont(subtitleFont);
        int subtitleY = TOP_MARGIN + titleHeight + 48;
        drawCenteredString(g, subtitle, PAGE_WIDTH / 2, subtitleY);

        int y = subtitleY + 26;
        g.setColor(Color.BLACK);
        g.setFont(introLabelFont);
        g.drawString(introLabel == null || introLabel.isBlank() ? "尊敬的客户：" : introLabel, LEFT_MARGIN, y);
        y += introLabelHeight + 8;
        g.setFont(introBodyFont);
        for (String line : introLines) {
            g.drawString(line, LEFT_MARGIN + 28, y);
            y += introBodyLineHeight;
        }

        y += 14;
        g.setColor(new Color(0x22, 0x22, 0x22));
        g.fillRect(LEFT_MARGIN, y, contentWidth, 1);
        y += 24;

        y += 2;
        y = drawTwoColumnFields(g, summaryList, wrappedSummaryValues, y, columnWidth, halfGap, labelFont, valueFont, labelHeight, valueLineHeight);

        y += 8;
        g.setFont(sectionFont);
        g.setColor(new Color(0x0F, 0x1F, 0x33));
        g.drawString("保险责任/份：", LEFT_MARGIN, y);
        y += sectionHeight + 8;
        y = drawCoverageTable(g, y, contentWidth, tableLabelWidth, tableValueWidth, coverageList, wrappedCoverageLabels, wrappedCoverageValues, tableHeadFont, tableBodyFont, tableHeadHeight, tableBodyLineHeight);

        if (!noteList.isEmpty()) {
            y += 28;
            y = (int) drawNotesBlock(g, sectionFont, noteFont, sectionHeight, noteLineHeight, noteList, LEFT_MARGIN, y, contentWidth);
        }

        if (!footerList.isEmpty()) {
            y += 10;
            g.setFont(footerFont);
            for (List<String> footerLinesWrap : wrappedFooters) {
                for (String line : footerLinesWrap) {
                    g.drawString(line, LEFT_MARGIN, y);
                    y += footerLineHeight;
                }
                y += 2;
            }
        }

        drawInsuranceSeal(g, PAGE_WIDTH - 190, Math.min(totalHeight - 200, y + 40), 78);

        g.dispose();
        return image;
    }

    private static float drawNotesBlock(Graphics2D g,
                                        Font sectionFont,
                                        Font noteFont,
                                        int sectionHeight,
                                        int noteLineHeight,
                                        List<String> lines,
                                        float left,
                                        float y,
                                        float width) {
        if (lines == null || lines.isEmpty()) {
            return y;
        }

        float currentY = y;
        g.setColor(Color.BLACK);
        for (String raw : lines) {
            String line = raw == null ? "" : raw.trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith("##")) {
                String heading = line.replaceFirst("^#{2,3}\\s*", "");
                g.setFont(sectionFont);
                g.drawString(heading, (int) left, (int) currentY);
                currentY += sectionHeight + 6;
                continue;
            }

            g.setFont(noteFont);
            if (line.matches("^[0-9]+[、.．)].*")) {
                int separatorIndex = line.indexOf('、');
                if (separatorIndex < 0) {
                    separatorIndex = line.indexOf('.');
                }
                String prefix = separatorIndex >= 0 ? line.substring(0, separatorIndex + 1) : "1、";
                String body = separatorIndex >= 0 ? line.substring(separatorIndex + 1).trim() : line;
                g.drawString(prefix, (int) left, (int) currentY);
                int prefixWidth = (int) Math.ceil(measureGraphicsTextWidth(g, noteFont, prefix)) + 6;
                List<String> wrapped = wrapText(body, noteFont, Math.max(40, (int) width - prefixWidth));
                for (String wrappedLine : wrapped) {
                    g.drawString(wrappedLine, (int) left + prefixWidth, (int) currentY);
                    currentY += noteLineHeight;
                }
                currentY += 4;
                continue;
            }

            List<String> wrapped = wrapText(line, noteFont, (int) width);
            for (String wrappedLine : wrapped) {
                g.drawString(wrappedLine, (int) left, (int) currentY);
                currentY += noteLineHeight;
            }
            currentY += 4;
        }
        return currentY;
    }

    private static byte[] buildPdf(List<PdfPage> pages) throws IOException {
        List<byte[]> objects = new ArrayList<>();
        objects.add(objectBytes("<< /Type /Catalog /Pages 2 0 R >>"));

        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < pages.size(); i++) {
            int pageObjectNumber = 3 + i * 3 + 2;
            kids.append(pageObjectNumber).append(" 0 R ");
        }
        objects.add(objectBytes("<< /Type /Pages /Count " + pages.size() + " /Kids [" + kids + "] >>"));

        for (PdfPage page : pages) {
            objects.add(page.contentObject());
            objects.add(page.imageObject());
            objects.add(page.pageObject());
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write("%PDF-1.4\n".getBytes(StandardCharsets.US_ASCII));

        List<Integer> offsets = new ArrayList<>();
        offsets.add(0);
        for (int i = 0; i < objects.size(); i++) {
            offsets.add(outputStream.size());
            outputStream.write((i + 1 + " 0 obj\n").getBytes(StandardCharsets.US_ASCII));
            outputStream.write(objects.get(i));
            outputStream.write("\nendobj\n".getBytes(StandardCharsets.US_ASCII));
        }

        int xrefOffset = outputStream.size();
        outputStream.write(("xref\n0 " + (objects.size() + 1) + "\n").getBytes(StandardCharsets.US_ASCII));
        outputStream.write("0000000000 65535 f \n".getBytes(StandardCharsets.US_ASCII));
        for (int i = 1; i < offsets.size(); i++) {
            outputStream.write(String.format("%010d 00000 n \n", offsets.get(i)).getBytes(StandardCharsets.US_ASCII));
        }
        outputStream.write(("trailer\n<< /Size " + (objects.size() + 1) + " /Root 1 0 R >>\n").getBytes(StandardCharsets.US_ASCII));
        outputStream.write(("startxref\n" + xrefOffset + "\n%%EOF").getBytes(StandardCharsets.US_ASCII));
        return outputStream.toByteArray();
    }

    private static byte[] objectBytes(String body) {
        return body.getBytes(StandardCharsets.US_ASCII);
    }

    private static void applyQualityHints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    private static List<String> wrapText(String text, Font font, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String value = text == null ? "" : text.trim();
        if (value.isEmpty()) {
            lines.add("-");
            return lines;
        }

        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setFont(font);
        FontMetrics metrics = g.getFontMetrics();
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            current.append(ch);
            if (metrics.stringWidth(current.toString()) > maxWidth) {
                current.deleteCharAt(current.length() - 1);
                if (!current.isEmpty()) {
                    lines.add(current.toString());
                }
                current = new StringBuilder().append(ch);
            }
        }

        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
        g.dispose();
        return lines;
    }

    private static List<String> wrapParagraphText(String text, Font font, int maxWidth) {
        List<String> result = new ArrayList<>();
        String value = text == null ? "" : text.trim();
        if (value.isEmpty()) {
            return result;
        }
        for (String paragraph : value.split("\\r?\\n")) {
            String trimmed = paragraph.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            result.addAll(wrapText(trimmed, font, maxWidth));
        }
        return result;
    }

    private static int drawTwoColumnFields(Graphics2D g,
                                           List<FieldLine> fields,
                                           List<List<String>> wrappedValues,
                                           int startY,
                                           int columnWidth,
                                           int gap,
                                           Font labelFont,
                                           Font valueFont,
                                           int labelHeight,
                                           int valueLineHeight) {
        int y = startY;
        int leftX = LEFT_MARGIN;
        int rightX = LEFT_MARGIN + columnWidth + gap;
        int valueOffset = LABEL_WIDTH;
        g.setFont(labelFont);
        for (int i = 0; i < fields.size(); i += 2) {
            FieldLine left = fields.get(i);
            List<String> leftValues = wrappedValues.get(i);
            FieldLine right = i + 1 < fields.size() ? fields.get(i + 1) : null;
            List<String> rightValues = i + 1 < wrappedValues.size() ? wrappedValues.get(i + 1) : List.of("-");
            int leftBlockHeight = Math.max(labelHeight, leftValues.size() * valueLineHeight);
            int rightBlockHeight = right == null ? leftBlockHeight : Math.max(labelHeight, rightValues.size() * valueLineHeight);
            int rowHeight = Math.max(leftBlockHeight, rightBlockHeight) + 12;

            g.setColor(new Color(0x0F, 0x1F, 0x33));
            g.drawString(left.label(), leftX, y + labelHeight - 6);
            g.setFont(valueFont);
            int valueY = y + valueLineHeight - 6;
            for (String line : leftValues) {
                g.drawString(line, leftX + valueOffset, valueY);
                valueY += valueLineHeight;
            }

            if (right != null) {
                g.setFont(labelFont);
                g.drawString(right.label(), rightX, y + labelHeight - 6);
                g.setFont(valueFont);
                int rightValueY = y + valueLineHeight - 6;
                for (String line : rightValues) {
                    g.drawString(line, rightX + valueOffset, rightValueY);
                    rightValueY += valueLineHeight;
                }
            }

            y += rowHeight;
            g.setFont(labelFont);
        }
        return y;
    }

    private static int drawCoverageTable(Graphics2D g,
                                         int startY,
                                         int tableWidth,
                                         int labelWidth,
                                         int valueWidth,
                                         List<FieldLine> rows,
                                         List<List<String>> wrappedLabels,
                                         List<List<String>> wrappedValues,
                                         Font headFont,
                                         Font bodyFont,
                                         int headHeight,
                                         int bodyLineHeight) {
        int y = startY;
        int tableX = LEFT_MARGIN;
        int headerHeight = 38;
        int rowValueX = tableX + labelWidth;

        g.setColor(new Color(0xF5, 0xF6, 0xF8));
        g.fillRect(tableX, y, tableWidth, headerHeight);
        g.setColor(new Color(0x55, 0x55, 0x55));
        g.drawRect(tableX, y, tableWidth, headerHeight);
        g.drawLine(rowValueX, y, rowValueX, y + headerHeight);
        g.setFont(headFont);
        g.setColor(new Color(0x0F, 0x1F, 0x33));
        g.drawString("保障内容", tableX + 14, y + headHeight - 8);
        g.drawString("保险金额", rowValueX + 14, y + headHeight - 8);
        y += headerHeight;

        g.setFont(bodyFont);
        for (int i = 0; i < rows.size(); i++) {
            List<String> leftLines = wrappedLabels.get(i);
            List<String> rightLines = wrappedValues.get(i);
            int rowHeight = Math.max(leftLines.size(), rightLines.size()) * bodyLineHeight + 16;
            g.setColor(Color.WHITE);
            g.fillRect(tableX, y, tableWidth, rowHeight);
            g.setColor(new Color(0x55, 0x55, 0x55));
            g.drawRect(tableX, y, tableWidth, rowHeight);
            g.drawLine(rowValueX, y, rowValueX, y + rowHeight);

            int leftTextY = y + bodyLineHeight - 6;
            for (String line : leftLines) {
                g.drawString(line, tableX + 14, leftTextY);
                leftTextY += bodyLineHeight;
            }

            int rightTextY = y + bodyLineHeight - 6;
            for (String line : rightLines) {
                g.drawString(line, rowValueX + 14, rightTextY);
                rightTextY += bodyLineHeight;
            }

            y += rowHeight;
        }
        return y;
    }

    private static void drawCenteredString(Graphics2D g, String text, int centerX, int baselineY) {
        String value = text == null ? "" : text;
        FontMetrics metrics = g.getFontMetrics();
        int textWidth = metrics.stringWidth(value);
        g.drawString(value, centerX - textWidth / 2, baselineY);
    }

    private static String normalizeNoteText(String text, int index) {
        String value = text == null ? "" : text.trim();
        if (value.isEmpty()) {
            return "-";
        }
        return value;
    }

    private static boolean isHeadingLine(String text) {
        return text != null && (text.startsWith("##") || text.startsWith("###"));
    }

    private static String stripHeadingPrefix(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceFirst("^#{2,3}\\s*", "").trim();
    }

    private static void drawInsuranceSeal(Graphics2D g, int centerX, int centerY, int radius) {
        Color sealColor = new Color(0xE5, 0x16, 0x1B, 185);
        g.setColor(sealColor);
        g.setStroke(new BasicStroke(3.5f));
        g.drawOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
        g.setStroke(new BasicStroke(2.4f));
        g.drawOval(centerX - radius + 10, centerY - radius + 10, (radius - 10) * 2, (radius - 10) * 2);

        Polygon star = createStar(centerX, centerY - 2, 30, 14);
        g.fillPolygon(star);

        Font sealFont = new Font("SansSerif", Font.BOLD, 22);
        g.setFont(sealFont);
        drawCenteredString(g, "保单凭证电子专用章", centerX, centerY + 48);
    }

    private static Polygon createStar(int centerX, int centerY, int outerRadius, int innerRadius) {
        Polygon polygon = new Polygon();
        for (int i = 0; i < 10; i++) {
            double angle = Math.PI / 2 + i * Math.PI / 5;
            int radius = i % 2 == 0 ? outerRadius : innerRadius;
            int x = centerX + (int) Math.round(Math.cos(angle) * radius);
            int y = centerY - (int) Math.round(Math.sin(angle) * radius);
            polygon.addPoint(x, y);
        }
        return polygon;
    }

    private static PDType0Font loadChineseFont(PDDocument document) throws IOException {
        String[] candidates = {
                "C:/Windows/Fonts/simhei.ttf",
                "C:/Windows/Fonts/msyh.ttc",
                "C:/Windows/Fonts/simsun.ttc"
        };
        for (String candidate : candidates) {
            File file = new File(candidate);
            if (file.exists()) {
                return PDType0Font.load(document, file);
            }
        }
        throw new IOException("未找到可用中文字体文件");
    }

    public record PolicyPage(BufferedImage image, List<String> hiddenText) {
    }

    public record FieldLine(String label, String value) {
    }

    private static final class PdfPage {
        private final int width;
        private final int height;
        private final int contentObjectNumber;
        private final int imageObjectNumber;
        private final int pageObjectNumber;
        private final byte[] contentBytes;
        private final byte[] imageBytes;

        private PdfPage(int width, int height, int contentObjectNumber, int imageObjectNumber, int pageObjectNumber, byte[] contentBytes, byte[] imageBytes) {
            this.width = width;
            this.height = height;
            this.contentObjectNumber = contentObjectNumber;
            this.imageObjectNumber = imageObjectNumber;
            this.pageObjectNumber = pageObjectNumber;
            this.contentBytes = contentBytes;
            this.imageBytes = imageBytes;
        }

        static PdfPage fromImage(int baseObjectNumber, BufferedImage image) throws IOException {
            int width = image.getWidth();
            int height = image.getHeight();
            byte[] rgb = toRgbBytes(image);
            byte[] compressedImage = compress(rgb);
            String content = "q\n" + width + " 0 0 " + height + " 0 0 cm\n/Im1 Do\nQ\n";
            return new PdfPage(width, height, baseObjectNumber, baseObjectNumber + 1, baseObjectNumber + 2,
                    content.getBytes(StandardCharsets.US_ASCII), compressedImage);
        }

        byte[] contentObject() {
            return streamObject(contentBytes, false, null, null);
        }

        byte[] imageObject() {
            return streamObject(imageBytes, true, width, height);
        }

        byte[] pageObject() {
            return ("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + width + " " + height + "] "
                    + "/Resources << /XObject << /Im1 " + imageObjectNumber + " 0 R >> >> "
                    + "/Contents " + contentObjectNumber + " 0 R >>").getBytes(StandardCharsets.US_ASCII);
        }

        private byte[] streamObject(byte[] data, boolean isImage, Integer width, Integer height) {
            StringBuilder builder = new StringBuilder();
            if (isImage) {
                builder.append("<< /Type /XObject /Subtype /Image /Width ")
                        .append(width)
                        .append(" /Height ")
                        .append(height)
                        .append(" /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /FlateDecode /Length ")
                        .append(data.length)
                        .append(" >>\nstream\n");
            } else {
                builder.append("<< /Length ").append(data.length).append(" >>\nstream\n");
            }
            builder.append(new String(data, StandardCharsets.ISO_8859_1));
            builder.append("\nendstream");
            return builder.toString().getBytes(StandardCharsets.ISO_8859_1);
        }

        private static byte[] toRgbBytes(BufferedImage image) {
            byte[] rgb = new byte[image.getWidth() * image.getHeight() * 3];
            int index = 0;
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int argb = image.getRGB(x, y);
                    rgb[index++] = (byte) ((argb >> 16) & 0xFF);
                    rgb[index++] = (byte) ((argb >> 8) & 0xFF);
                    rgb[index++] = (byte) (argb & 0xFF);
                }
            }
            return rgb;
        }

        private static byte[] compress(byte[] data) throws IOException {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try (DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(outputStream)) {
                deflaterOutputStream.write(data);
            }
            return outputStream.toByteArray();
        }
    }
}
