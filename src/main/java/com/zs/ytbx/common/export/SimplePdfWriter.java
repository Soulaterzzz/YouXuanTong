package com.zs.ytbx.common.export;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
