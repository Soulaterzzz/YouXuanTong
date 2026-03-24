package com.zs.ytbx.controller;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
public class FileController {

    @GetMapping("/files/{*filePath}")
    public ResponseEntity<Resource> download(@PathVariable String filePath) {
        String normalized = StringUtils.hasText(filePath) ? filePath : "document.txt";
        String fileName = normalized.contains("/") ? normalized.substring(normalized.lastIndexOf('/') + 1) : normalized;
        byte[] content = ("YTBX 文件下载示例\n\n文件标识: " + normalized + "\n\n"
                + "当前版本用于 MVP 一体化交付，可替换为 OSS / MinIO / 本地文件系统实现。")
                .getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(fileName).build().toString())
                .body(new ByteArrayResource(content));
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
}
