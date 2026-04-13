package com.zs.ytbx.controller;

import com.zs.ytbx.common.api.ApiResponse;
import com.zs.ytbx.common.auth.AuthContext;
import com.zs.ytbx.common.exception.BusinessException;
import com.zs.ytbx.common.enums.ResultCode;
import com.zs.ytbx.common.auth.SessionUser;
import com.zs.ytbx.service.ImageService;
import com.zs.ytbx.vo.TemplateFileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/images")
public class ImageController {

    private final ImageService imageService;
    private final AuthContext authContext;

    @GetMapping("/product/{productId}")
    public ResponseEntity<byte[]> getProductImage(@PathVariable Long productId) {
        byte[] imageData = imageService.getProductImage(productId);
        String contentType = imageService.getProductImageContentType(productId);

        if (imageData == null || imageData.length == 0) {
            return ResponseEntity.notFound().build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(resolveMediaType(contentType));
        headers.setContentLength(imageData.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(imageData);
    }

    @GetMapping("/product/{productId}/url")
    public ApiResponse<Map<String, String>> getProductImageUrl(@PathVariable Long productId) {
        String imageUrl = imageService.getProductImageUrl(productId);
        return ApiResponse.success(Map.of("url", imageUrl));
    }

    @GetMapping("/template/{productId}")
    public ResponseEntity<byte[]> downloadProductTemplate(@PathVariable Long productId) {
        authContext.requireCurrentUser();
        TemplateFileVO templateFile = imageService.getProductTemplate(productId);

        if (templateFile == null || templateFile.getData() == null || templateFile.getData().length == 0) {
            return ResponseEntity.notFound().build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(templateFile.getFileName(), StandardCharsets.UTF_8)
                .build());
        headers.setContentLength(templateFile.getData().length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(templateFile.getData());
    }

    @PostMapping("/product/{productId}/upload")
    public ApiResponse<Map<String, String>> uploadProductImage(
            @PathVariable Long productId,
            @RequestParam("file") MultipartFile file) {
        requireAdmin();
        imageService.uploadProductImage(productId, file);
        String imageUrl = imageService.getProductImageUrl(productId);
        return ApiResponse.success(Map.of("url", imageUrl, "message", "图片上传成功"));
    }

    @DeleteMapping("/product/{productId}")
    public ApiResponse<Map<String, String>> deleteProductImage(@PathVariable Long productId) {
        requireAdmin();
        imageService.deleteProductImage(productId);
        return ApiResponse.success(Map.of("message", "图片删除成功"));
    }

    @PostMapping("/product/{productId}/template")
    public ApiResponse<Map<String, String>> uploadProductTemplate(
            @PathVariable Long productId,
            @RequestParam("file") MultipartFile file) {
        requireAdmin();
        String templateUrl = imageService.uploadProductTemplate(productId, file);
        return ApiResponse.success(Map.of("url", templateUrl, "message", "模板上传成功"));
    }

    @PostMapping("/profession/{companyCode}")
    public ApiResponse<Map<String, String>> uploadProfessionTable(
            @PathVariable String companyCode,
            @RequestParam("file") MultipartFile file) {
        requireAdmin();
        String fileName = imageService.uploadProfessionTable(companyCode, file);
        return ApiResponse.success(Map.of(
                "fileName", fileName,
                "url", "/files/uploads/profession/" + fileName,
                "message", "职业表上传成功"
        ));
    }

    @DeleteMapping("/product/{productId}/template")
    public ApiResponse<Map<String, String>> deleteProductTemplate(@PathVariable Long productId) {
        requireAdmin();
        imageService.deleteProductTemplate(productId);
        return ApiResponse.success(Map.of("message", "模板删除成功"));
    }

    private void requireAdmin() {
        SessionUser currentUser = authContext.requireCurrentUser();
        if (!"ADMIN".equals(currentUser.getUserType())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }

    private MediaType resolveMediaType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.IMAGE_JPEG;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (IllegalArgumentException exception) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
