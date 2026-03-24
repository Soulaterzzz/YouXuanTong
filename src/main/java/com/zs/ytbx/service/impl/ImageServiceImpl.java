package com.zs.ytbx.service.impl;

import com.zs.ytbx.config.FileStorageConfig;
import com.zs.ytbx.entity.AxxProductEntity;
import com.zs.ytbx.common.exception.BusinessException;
import com.zs.ytbx.common.enums.ResultCode;
import com.zs.ytbx.mapper.AxxProductMapper;
import com.zs.ytbx.service.ImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageServiceImpl implements ImageService {

    private final AxxProductMapper productMapper;
    private final FileStorageConfig fileStorageConfig;

    @Override
    public byte[] getProductImage(Long productId) {
        AxxProductEntity product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "产品不存在");
        }

        String imagePath = product.getImagePath();
        if (imagePath == null || imagePath.isEmpty()) {
            return getDefaultImage();
        }

        try {
            Path path = Paths.get(imagePath);
            if (Files.exists(path)) {
                return Files.readAllBytes(path);
            }
        } catch (IOException e) {
            log.error("读取产品图片失败: productId={}, path={}", productId, imagePath, e);
        }

        return getDefaultImage();
    }

    @Override
    public String getProductImageUrl(Long productId) {
        AxxProductEntity product = productMapper.selectById(productId);
        if (product != null && product.getImageUrl() != null) {
            return product.getImageUrl();
        }
        return "/api/images/product/" + productId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uploadProductImage(Long productId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.INVALID_PARAM, "图片文件不能为空");
        }

        validateFile(file);

        AxxProductEntity product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "产品不存在");
        }

        try {
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String newFilename = UUID.randomUUID().toString() + "." + extension;
            
            Path uploadPath = Paths.get(fileStorageConfig.getUploadPath(), 
                                       fileStorageConfig.getProductImagePath());
            
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            Path filePath = uploadPath.resolve(newFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            String imageUrl = "/api/images/product/" + productId;
            
            product.setImageUrl(imageUrl);
            product.setImagePath(filePath.toString());
            product.setImageContentType(file.getContentType());
            product.setImageSize(file.getSize());
            product.setUpdateTime(LocalDateTime.now());
            
            productMapper.updateById(product);
            
            log.info("产品图片上传成功: productId={}, path={}", productId, filePath);
            
        } catch (IOException e) {
            log.error("上传产品图片失败: productId={}", productId, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "上传图片失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProductImage(Long productId) {
        AxxProductEntity product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "产品不存在");
        }

        String imagePath = product.getImagePath();
        if (imagePath != null && !imagePath.isEmpty()) {
            try {
                Path path = Paths.get(imagePath);
                Files.deleteIfExists(path);
                log.info("删除产品图片文件: {}", imagePath);
            } catch (IOException e) {
                log.error("删除产品图片文件失败: {}", imagePath, e);
            }
        }

        product.setImageUrl(null);
        product.setImagePath(null);
        product.setImageContentType(null);
        product.setImageSize(null);
        product.setUpdateTime(LocalDateTime.now());
        
        productMapper.updateById(product);
    }

    private void validateFile(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isEmpty()) {
            throw new BusinessException(ResultCode.INVALID_PARAM, "文件名不能为空");
        }

        String extension = getFileExtension(filename);
        if (extension == null || !Arrays.asList(fileStorageConfig.getAllowedExtensions())
                .contains(extension.toLowerCase())) {
            throw new BusinessException(ResultCode.INVALID_PARAM, 
                "不支持的文件类型，仅支持: " + String.join(", ", fileStorageConfig.getAllowedExtensions()));
        }

        if (file.getSize() > fileStorageConfig.getMaxFileSize()) {
            throw new BusinessException(ResultCode.INVALID_PARAM, 
                "文件大小超过限制，最大允许: " + (fileStorageConfig.getMaxFileSize() / 1024 / 1024) + "MB");
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null) {
            return null;
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1);
        }
        return null;
    }

    private byte[] getDefaultImage() {
        return new byte[0];
    }
}
