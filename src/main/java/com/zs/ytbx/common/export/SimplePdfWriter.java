package com.zs.ytbx.common.export;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DeflaterOutputStream;

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
            List<String> wrapped = wrapText(note, noteFont, contentWidth - 28);
            wrappedNotes.add(wrapped);
            notesHeight += Math.max(noteLineHeight, wrapped.size() * noteLineHeight) + 6;
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

        g.setFont(sectionFont);
        g.setColor(new Color(0x0F, 0x1F, 0x33));
        g.drawString("保单信息", LEFT_MARGIN, y);
        y += sectionHeight + 12;
        y = drawTwoColumnFields(g, summaryList, wrappedSummaryValues, y, columnWidth, halfGap, labelFont, valueFont, labelHeight, valueLineHeight);

        y += 10;
        g.setFont(sectionFont);
        g.drawString("保险责任/份:", LEFT_MARGIN, y);
        y += sectionHeight + 8;
        y = drawCoverageTable(g, y, contentWidth, tableLabelWidth, tableValueWidth, coverageList, wrappedCoverageLabels, wrappedCoverageValues, tableHeadFont, tableBodyFont, tableHeadHeight, tableBodyLineHeight);

        if (!noteList.isEmpty()) {
            y += 16;
            g.setFont(sectionFont);
            g.drawString("特别约定：", LEFT_MARGIN, y);
            y += sectionHeight + 8;
            g.setFont(noteFont);
            for (int i = 0; i < wrappedNotes.size(); i++) {
                List<String> noteLines = wrappedNotes.get(i);
                int blockHeight = Math.max(noteLineHeight, noteLines.size() * noteLineHeight);
                g.drawString((i + 1) + "、", LEFT_MARGIN, y);
                int noteX = LEFT_MARGIN + 24;
                for (String line : noteLines) {
                    g.drawString(line, noteX, y);
                    y += noteLineHeight;
                }
                y += 6;
                if (blockHeight > noteLineHeight) {
                    y += (blockHeight - noteLineHeight);
                }
            }
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
        g.drawString("保险金额/人（人民币）", rowValueX + 14, y + headHeight - 8);
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
            return index + "、-";
        }
        if (value.startsWith(index + "、") || value.startsWith(index + ".") || value.startsWith(index + ")")) {
            return value;
        }
        return index + "、" + value;
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
        drawCenteredString(g, "电子专用章", centerX, centerY + 48);
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
