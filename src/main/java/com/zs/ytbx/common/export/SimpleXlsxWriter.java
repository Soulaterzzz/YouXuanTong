package com.zs.ytbx.common.export;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class SimpleXlsxWriter {

    private SimpleXlsxWriter() {
    }

    public static byte[] write(String sheetName, List<String> headers, List<List<String>> rows) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream, StandardCharsets.UTF_8)) {

            putEntry(zipOutputStream, "[Content_Types].xml", buildContentTypesXml());
            putEntry(zipOutputStream, "_rels/.rels", buildRelsXml());
            putEntry(zipOutputStream, "xl/workbook.xml", buildWorkbookXml(sheetName));
            putEntry(zipOutputStream, "xl/_rels/workbook.xml.rels", buildWorkbookRelsXml());
            putEntry(zipOutputStream, "xl/styles.xml", buildStylesXml());
            putEntry(zipOutputStream, "xl/worksheets/sheet1.xml", buildSheetXml(headers, rows));

            zipOutputStream.finish();
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("生成Excel失败", e);
        }
    }

    private static void putEntry(ZipOutputStream zipOutputStream, String name, String content) throws IOException {
        zipOutputStream.putNextEntry(new ZipEntry(name));
        zipOutputStream.write(content.getBytes(StandardCharsets.UTF_8));
        zipOutputStream.closeEntry();
    }

    private static String buildContentTypesXml() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                  <Default Extension="xml" ContentType="application/xml"/>
                  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
                </Types>
                """;
    }

    private static String buildRelsXml() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                </Relationships>
                """;
    }

    private static String buildWorkbookXml(String sheetName) {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
                          xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                  <sheets>
                    <sheet name="%s" sheetId="1" r:id="rId1"/>
                  </sheets>
                </workbook>
                """.formatted(escapeXml(sheetName));
    }

    private static String buildWorkbookRelsXml() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
                  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
                </Relationships>
                """;
    }

    private static String buildStylesXml() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                  <fonts count="1">
                    <font>
                      <sz val="11"/>
                      <color theme="1"/>
                      <name val="Calibri"/>
                      <family val="2"/>
                    </font>
                  </fonts>
                  <fills count="1">
                    <fill>
                      <patternFill patternType="none"/>
                    </fill>
                  </fills>
                  <borders count="1">
                    <border>
                      <left/>
                      <right/>
                      <top/>
                      <bottom/>
                      <diagonal/>
                    </border>
                  </borders>
                  <cellStyleXfs count="1">
                    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
                  </cellStyleXfs>
                  <cellXfs count="1">
                    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
                  </cellXfs>
                </styleSheet>
                """;
    }

    private static String buildSheetXml(List<String> headers, List<List<String>> rows) {
        StringBuilder builder = new StringBuilder();
        builder.append("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                  <sheetData>
                """);

        appendRow(builder, 1, headers);
        for (int i = 0; i < rows.size(); i++) {
            appendRow(builder, i + 2, rows.get(i));
        }

        builder.append("""
                  </sheetData>
                </worksheet>
                """);
        return builder.toString();
    }

    private static void appendRow(StringBuilder builder, int rowIndex, List<String> cells) {
        builder.append("    <row r=\"").append(rowIndex).append("\">");
        for (int i = 0; i < cells.size(); i++) {
            String value = cells.get(i) == null ? "" : cells.get(i);
            builder.append("<c r=\"")
                    .append(columnName(i + 1))
                    .append(rowIndex)
                    .append("\" t=\"inlineStr\"><is><t>")
                    .append(escapeXml(value))
                    .append("</t></is></c>");
        }
        builder.append("</row>\n");
    }

    private static String columnName(int index) {
        StringBuilder builder = new StringBuilder();
        int current = index;
        while (current > 0) {
            int remainder = (current - 1) % 26;
            builder.insert(0, (char) ('A' + remainder));
            current = (current - 1) / 26;
        }
        return builder.toString();
    }

    private static String escapeXml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
