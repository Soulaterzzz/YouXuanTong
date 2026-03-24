package com.zs.ytbx.controller;

import com.zs.ytbx.common.api.ApiResponse;
import com.zs.ytbx.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/images")
public class ImageController {

    private final ImageService imageService;

    @GetMapping("/product/{productId}")
    public ResponseEntity<byte[]> getProductImage(@PathVariable Long productId) {
        byte[] imageData = imageService.getProductImage(productId);
        
        if (imageData == null || imageData.length == 0) {
            return ResponseEntity.notFound().build();
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG);
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

    @PostMapping("/product/{productId}/upload")
    public ApiResponse<Map<String, String>> uploadProductImage(
            @PathVariable Long productId,
            @RequestParam("file") MultipartFile file) {
        imageService.uploadProductImage(productId, file);
        String imageUrl = imageService.getProductImageUrl(productId);
        return ApiResponse.success(Map.of("url", imageUrl, "message", "图片上传成功"));
    }

    @DeleteMapping("/product/{productId}")
    public ApiResponse<Map<String, String>> deleteProductImage(@PathVariable Long productId) {
        imageService.deleteProductImage(productId);
        return ApiResponse.success(Map.of("message", "图片删除成功"));
    }
}
