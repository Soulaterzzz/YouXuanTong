package com.zs.ytbx.controller;

import com.zs.ytbx.common.api.ApiResponse;
import com.zs.ytbx.service.PortalService;
import com.zs.ytbx.vo.portal.PortalHomeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/portal")
public class PortalController {

    private final PortalService portalService;

    @GetMapping("/home")
    public ApiResponse<PortalHomeVO> getHome() {
        return ApiResponse.success(portalService.getHome());
    }
}
