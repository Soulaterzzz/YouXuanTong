package com.zs.ytbx.controller;

import com.zs.ytbx.common.auth.AuthContext;
import com.zs.ytbx.common.auth.SessionUser;
import com.zs.ytbx.common.enums.ResultCode;
import com.zs.ytbx.service.ImageService;
import com.zs.ytbx.support.ControllerTestSupport;
import com.zs.ytbx.vo.TemplateFileVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ImageControllerTest extends ControllerTestSupport {

    @Mock
    private ImageService imageService;

    @Mock
    private AuthContext authContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc(new ImageController(imageService, authContext));
    }

    @Test
    void shouldReturnImageWithResolvedContentType() throws Exception {
        byte[] imageBytes = new byte[] {1, 2, 3, 4};
        given(imageService.getProductImage(11L)).willReturn(imageBytes);
        given(imageService.getProductImageContentType(11L)).willReturn("image/webp");

        mockMvc.perform(get("/api/images/product/11"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "image/webp"))
                .andExpect(content().bytes(imageBytes));
    }

    @Test
    void shouldDownloadTemplateAsAttachmentForLoggedInUser() throws Exception {
        byte[] templateBytes = new byte[] {9, 8, 7};
        given(authContext.requireCurrentUser()).willReturn(SessionUser.builder()
                .userId(3L)
                .username("user3")
                .userType("USER")
                .build());
        given(imageService.getProductTemplate(22L)).willReturn(new TemplateFileVO(templateBytes, "template-22.xlsx"));

        mockMvc.perform(get("/api/images/template/22"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/octet-stream"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("template-22.xlsx")))
                .andExpect(content().bytes(templateBytes));
    }

    @Test
    void shouldRejectNonAdminUpload() throws Exception {
        given(authContext.requireCurrentUser()).willReturn(SessionUser.builder()
                .userId(4L)
                .username("user4")
                .userType("USER")
                .build());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "banner.webp",
                "image/webp",
                new byte[] {1, 2, 3});

        mockMvc.perform(multipart("/api/images/product/33/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.FORBIDDEN.getCode()))
                .andExpect(jsonPath("$.message").value(ResultCode.FORBIDDEN.getMessage()));

        verifyNoInteractions(imageService);
    }
}
