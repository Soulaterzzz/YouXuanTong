package com.zs.ytbx.service;

import com.zs.ytbx.vo.TemplateFileVO;
import org.springframework.web.multipart.MultipartFile;

public interface ImageService {
    byte[] getProductImage(Long productId);
    String getProductImageUrl(Long productId);
    void uploadProductImage(Long productId, MultipartFile file);
    void deleteProductImage(Long productId);

    TemplateFileVO getProductTemplate(Long productId);
    String uploadProductTemplate(Long productId, MultipartFile file);
    void deleteProductTemplate(Long productId);
}
