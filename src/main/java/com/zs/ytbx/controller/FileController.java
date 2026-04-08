package com.zs.ytbx.controller;

import com.zs.ytbx.common.export.SimplePdfWriter;
import com.zs.ytbx.config.FileStorageConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class FileController {

    private static final MediaType PDF_MEDIA_TYPE = MediaType.APPLICATION_PDF;

    private final FileStorageConfig fileStorageConfig;

    @GetMapping("/files/{*filePath}")
    public ResponseEntity<byte[]> download(@PathVariable String filePath) {
        if (!StringUtils.hasText(filePath)) {
            return ResponseEntity.notFound().build();
        }

        if (filePath.startsWith("mock/")) {
            return buildMockPdf(filePath);
        }

        Path resolved = resolvePath(filePath);
        if (resolved == null || !Files.exists(resolved) || !Files.isRegularFile(resolved)) {
            return ResponseEntity.notFound().build();
        }

        try {
            byte[] content = Files.readAllBytes(resolved);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(resolveMediaType(resolved));
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename(resolved.getFileName().toString(), StandardCharsets.UTF_8)
                    .build());
            headers.setContentLength(content.length);
            return ResponseEntity.ok().headers(headers).body(content);
        } catch (IOException exception) {
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/agreements/trust/{policyNo}")
    public ResponseEntity<String> agreement(@PathVariable String policyNo) {
        String html = """
                <!doctype html>
                <html lang="zh-CN">
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                  <title>数字信托协议</title>
                  <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, "PingFang SC", sans-serif; margin: 0; background: #f6f4f2; color: #1b1c1c; }
                    main { max-width: 860px; margin: 40px auto; padding: 32px; background: #fff; border-radius: 24px; box-shadow: 0 20px 50px rgba(0,59,114,.08); }
                    h1 { margin: 0 0 8px; color: #003b72; }
                    p, li { line-height: 1.8; }
                    .meta { color: #6d7481; font-size: 14px; }
                  </style>
                </head>
                <body>
                <main>
                  <h1>平安数字信托服务协议</h1>
                  <p class="meta">适用保单：%s</p>
                  <p>本页面为 MVP 交付内置协议展示页，用于承接保单详情中的“查看信托协议”链接。</p>
                  <ul>
                    <li>理赔金拨付按照约定受益人和信托分配比例执行。</li>
                    <li>保单状态、履约事件与理赔信息由平台统一留痕。</li>
                    <li>正式上线时建议替换为可配置 CMS 协议模板或电子签章文档服务。</li>
                  </ul>
                </main>
                </body>
                </html>
                """.formatted(policyNo);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }

    private ResponseEntity<byte[]> buildMockPdf(String filePath) {
        String fileName = getFileName(filePath);
        byte[] content = SimplePdfWriter.writePages(List.of(SimplePdfWriter.renderDetailPage(
                "电子保单示例",
                List.of(
                        new SimplePdfWriter.FieldLine("文件标识", filePath),
                        new SimplePdfWriter.FieldLine("说明", "当前环境未配置真实电子保单文件，返回示例 PDF 用于联调。"),
                        new SimplePdfWriter.FieldLine("提示", "上线时请将 biz_policy.e_policy_file_id 指向实际存储文件。")
                ),
                "YTBX 电子保单下载示例")));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(PDF_MEDIA_TYPE);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(fileName.endsWith(".pdf") ? fileName : fileName + ".pdf", StandardCharsets.UTF_8)
                .build());
        headers.setContentLength(content.length);
        return ResponseEntity.ok().headers(headers).body(content);
    }

    private Path resolvePath(String filePath) {
        Path candidate = Paths.get(filePath).toAbsolutePath().normalize();
        Path uploadRoot = Paths.get(fileStorageConfig.getUploadPath()).toAbsolutePath().normalize();
        Path templateRoot = Paths.get(fileStorageConfig.getTemplateFilePath()).toAbsolutePath().normalize();

        if (candidate.startsWith(uploadRoot) || candidate.startsWith(templateRoot)) {
            return candidate;
        }

        return null;
    }

    private MediaType resolveMediaType(Path path) {
        try {
            String contentType = Files.probeContentType(path);
            if (StringUtils.hasText(contentType)) {
                return MediaType.parseMediaType(contentType);
            }
        } catch (Exception ignored) {
            // fall through to octet-stream
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    private String getFileName(String filePath) {
        String normalized = filePath.replace('\\', '/');
        int lastSlash = normalized.lastIndexOf('/');
        return lastSlash >= 0 ? normalized.substring(lastSlash + 1) : normalized;
    }
}
