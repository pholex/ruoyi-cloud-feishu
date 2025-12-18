package com.ruoyi.auth.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.auth.service.FeishuAuthService;
import com.ruoyi.auth.form.FeishuLoginBody;

/**
 * 飞书认证控制器
 */
@RestController
@RequestMapping("/feishu")
public class FeishuAuthController {
    
    @Autowired
    private FeishuAuthService feishuAuthService;
    
    /**
     * 飞书登录
     */
    @PostMapping("/login")
    public R<?> feishuLogin(@RequestBody FeishuLoginBody form) {
        return feishuAuthService.login(form.getCode(), form.getState());
    }
    
    /**
     * 获取飞书授权URL
     */
    @GetMapping("/auth-url")
    public R<?> getAuthUrl(@RequestParam String redirectUri) {
        return R.ok(feishuAuthService.buildAuthUrl(redirectUri));
    }
}
