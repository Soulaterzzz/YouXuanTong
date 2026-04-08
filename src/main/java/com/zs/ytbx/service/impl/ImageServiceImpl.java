package com.zs.ytbx.service.impl;

import com.zs.ytbx.config.FileStorageConfig;
import com.zs.ytbx.entity.AxxProductEntity;
import com.zs.ytbx.common.exception.BusinessException;
import com.zs.ytbx.common.enums.ResultCode;
import com.zs.ytbx.mapper.AxxProductMapper;
import com.zs.ytbx.service.ImageService;
import com.zs.ytbx.vo.TemplateFileVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

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
    public String getProductImageContentType(Long productId) {
        AxxProductEntity product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "产品不存在");
        }

        if (product.getImageContentType() != null && !product.getImageContentType().isEmpty()) {
            return product.getImageContentType();
        }

        String imagePath = product.getImagePath();
        if (imagePath != null && !imagePath.isEmpty()) {
            try {
                Path path = Paths.get(imagePath);
                if (Files.exists(path)) {
                    String contentType = Files.probeContentType(path);
                    if (contentType != null && !contentType.isEmpty()) {
                        return contentType;
                    }
                }
            } catch (IOException e) {
                log.warn("探测产品图片内容类型失败: productId={}, path={}", productId, imagePath, e);
            }
        }

        return "image/jpeg";
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
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            BufferedImage image = new BufferedImage(800, 500, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            try {
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics.setPaint(new GradientPaint(0, 0, new Color(247, 250, 252), 800, 500, new Color(226, 232, 240)));
                graphics.fillRect(0, 0, 800, 500);

                graphics.setColor(new Color(230, 0, 18));
                graphics.fillRoundRect(282, 132, 236, 236, 42, 42);

                graphics.setColor(Color.WHITE);
                graphics.setFont(new Font("SansSerif", Font.BOLD, 58));
                String title = "暂无图片";
                int titleWidth = graphics.getFontMetrics().stringWidth(title);
                graphics.drawString(title, (800 - titleWidth) / 2, 272);

                graphics.setFont(new Font("SansSerif", Font.PLAIN, 24));
                String subtitle = "点击上传产品图片";
                int subtitleWidth = graphics.getFontMetrics().stringWidth(subtitle);
                graphics.drawString(subtitle, (800 - subtitleWidth) / 2, 320);
            } finally {
                graphics.dispose();
            }

            javax.imageio.ImageIO.write(image, "jpg", outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            log.warn("生成默认产品图片失败", e);
            return new byte[0];
        }
    }

    @Override
    public TemplateFileVO getProductTemplate(Long productId) {
        AxxProductEntity product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "产品不存在");
        }

        String templatePath = product.getTemplateFilePath();
        if (templatePath != null && !templatePath.isEmpty()) {
            try {
                Path filePath = Paths.get(templatePath);
                if (Files.exists(filePath)) {
                    byte[] data = Files.readAllBytes(filePath);
                    String fileName = product.getTemplateFileName();
                    return new TemplateFileVO(data, fileName);
                }
            } catch (IOException e) {
                log.error("读取模板文件失败: productId={}, path={}", productId, templatePath, e);
            }
        }

        // 如果没有模板文件，生成默认的Excel模板
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("批量导入模板");

            // 创建表头
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("序号");
            headerRow.createCell(1).setCellValue("方案名称");
            headerRow.createCell(2).setCellValue("被保人姓名");
            headerRow.createCell(3).setCellValue("被保人证件号");
            headerRow.createCell(4).setCellValue("被保人职业");
            headerRow.createCell(5).setCellValue("数量");
            headerRow.createCell(6).setCellValue("地址");
            headerRow.createCell(7).setCellValue("代理人");

            // 创建示例数据
            Row dataRow = sheet.createRow(1);
            dataRow.createCell(0).setCellValue(1);
            dataRow.createCell(1).setCellValue(product.getProductName());
            dataRow.createCell(2).setCellValue("张三");
            dataRow.createCell(3).setCellValue("110101199001011234");
            dataRow.createCell(4).setCellValue("办公室职员");
            dataRow.createCell(5).setCellValue(1);
            dataRow.createCell(6).setCellValue("北京市朝阳区");
            dataRow.createCell(7).setCellValue("李四");

            // 调整列宽
            for (int i = 0; i < 8; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
            workbook.close();

            byte[] data = outputStream.toByteArray();
            outputStream.close();

            String fileName = "template_" + productId + ".xlsx";
            return new TemplateFileVO(data, fileName);
        } catch (Exception e) {
            log.error("生成默认模板失败: productId={}", productId, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "生成模板失败");
        }
    }

    @Override
    public String uploadProductTemplate(Long productId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.INVALID_PARAM, "模板文件不能为空");
        }

        validateTemplateFile(file);

        AxxProductEntity product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "产品不存在");
        }

        try {
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String newFilename = productId + "_template_" + UUID.randomUUID().toString() + "." + extension;

            Path uploadPath = Paths.get(fileStorageConfig.getTemplateFilePath());

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path filePath = uploadPath.resolve(newFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            product.setTemplateFileName(originalFilename);
            product.setTemplateFilePath(filePath.toString());
            productMapper.updateById(product);

            String templateUrl = "/api/images/template/" + productId;

            log.info("产品模板上传成功: productId={}, path={}", productId, filePath);

            return templateUrl;

        } catch (IOException e) {
            log.error("上传产品模板失败: productId={}", productId, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "上传模板失败: " + e.getMessage());
        }
    }

    @Override
    public void deleteProductTemplate(Long productId) {
        AxxProductEntity product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "产品不存在");
        }

        String templatePath = product.getTemplateFilePath();
        if (templatePath != null && !templatePath.isEmpty()) {
            try {
                Path path = Paths.get(templatePath);
                Files.deleteIfExists(path);
                log.info("删除产品模板文件: {}", templatePath);
            } catch (IOException e) {
                log.error("删除产品模板文件失败: {}", templatePath, e);
            }
        }

        product.setTemplateFileName(null);
        product.setTemplateFilePath(null);
        productMapper.updateById(product);
    }

    private void validateTemplateFile(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isEmpty()) {
            throw new BusinessException(ResultCode.INVALID_PARAM, "文件名不能为空");
        }

        String extension = getFileExtension(filename);
        if (extension == null || !Arrays.asList(fileStorageConfig.getAllowedTemplateExtensions())
                .contains(extension.toLowerCase())) {
            throw new BusinessException(ResultCode.INVALID_PARAM,
                "不支持的文件类型，仅支持: " + String.join(", ", fileStorageConfig.getAllowedTemplateExtensions()));
        }

        if (file.getSize() > fileStorageConfig.getMaxFileSize()) {
            throw new BusinessException(ResultCode.INVALID_PARAM,
                "文件大小超过限制，最大允许: " + (fileStorageConfig.getMaxFileSize() / 1024 / 1024) + "MB");
        }
    }
}
