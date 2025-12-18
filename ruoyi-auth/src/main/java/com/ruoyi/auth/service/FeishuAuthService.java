package com.ruoyi.auth.service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.core.constant.CacheConstants;
import com.ruoyi.common.core.constant.Constants;
import com.ruoyi.common.core.constant.SecurityConstants;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.text.Convert;
import com.ruoyi.common.redis.service.RedisService;
import com.ruoyi.common.security.service.TokenService;
import com.ruoyi.system.api.RemoteUserService;
import com.ruoyi.system.api.model.LoginUser;

/**
 * 飞书认证服务
 */
@Service
public class FeishuAuthService {
    
    private static final Logger log = LoggerFactory.getLogger(FeishuAuthService.class);
    
    private static final String FEISHU_STATE_KEY = "feishu_state:";
    
    @Autowired
    private RedisService redisService;
    
    @Autowired
    private RemoteUserService remoteUserService;
    
    @Autowired
    private TokenService tokenService;
    
    @Autowired
    private SysRecordLogService recordLogService;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    /**
     * 从 Redis 获取系统配置
     */
    private String getConfigValue(String configKey) {
        String cacheKey = CacheConstants.SYS_CONFIG_KEY + configKey;
        return Convert.toStr(redisService.getCacheObject(cacheKey));
    }
    
    /**
     * 获取飞书应用ID
     */
    private String getAppId() {
        return getConfigValue("feishu.app.id");
    }
    
    /**
     * 获取飞书应用密钥
     */
    private String getAppSecret() {
        return getConfigValue("feishu.app.secret");
    }
    
    /**
     * 检查飞书登录是否启用
     */
    private boolean isFeishuLoginEnabled() {
        String enabled = getConfigValue("feishu.login.enabled");
        return "true".equalsIgnoreCase(enabled);
    }
    
    /**
     * 检查是否自动创建飞书用户
     */
    private boolean isAutoCreateUser() {
        String autoCreate = getConfigValue("feishu.user.auto.create");
        return "true".equalsIgnoreCase(autoCreate);
    }
    
    /**
     * 生成飞书授权URL
     */
    public String buildAuthUrl(String redirectUri) {
        if (!isFeishuLoginEnabled()) {
            throw new ServiceException("飞书登录功能已禁用");
        }
        
        String appId = getAppId();
        if (appId == null || appId.isEmpty() || "your_app_id_here".equals(appId)) {
            throw new ServiceException("飞书应用ID未配置");
        }
        
        String state = UUID.randomUUID().toString().replace("-", "");
        redisService.setCacheObject(FEISHU_STATE_KEY + state, redirectUri, 300L, TimeUnit.SECONDS);
        
        return String.format(
            "https://open.feishu.cn/open-apis/authen/v1/authorize?app_id=%s&redirect_uri=%s&state=%s",
            appId, redirectUri, state
        );
    }
    
    /**
     * 飞书登录处理
     */
    public R<?> login(String code, String state) {
        if (!isFeishuLoginEnabled()) {
            return R.fail("飞书登录功能已禁用");
        }
        
        // 验证 state
        String cachedRedirectUri = redisService.getCacheObject(FEISHU_STATE_KEY + state);
        if (cachedRedirectUri == null) {
            return R.fail("无效的登录状态，请重新登录");
        }
        redisService.deleteObject(FEISHU_STATE_KEY + state);
        
        try {
            // 获取 app_access_token
            String appAccessToken = getAppAccessToken();
            
            // 用 code 换取 user_access_token
            JSONObject tokenResult = getUserAccessToken(appAccessToken, code);
            String userAccessToken = tokenResult.getString("access_token");
            
            // 获取用户信息
            JSONObject userInfo = getUserInfo(userAccessToken);
            String unionId = userInfo.getString("union_id");
            String name = userInfo.getString("name");
            
            // 根据 union_id 查找本地用户
            R<LoginUser> userResult = remoteUserService.getUserInfoByFeishuUnionId(unionId, SecurityConstants.INNER);
            
            if (userResult == null || userResult.getData() == null) {
                if (!isAutoCreateUser()) {
                    recordLogService.recordLogininfor(name, "Error", "飞书用户未绑定本地账号");
                    return R.fail("飞书用户未绑定本地账号，请联系管理员");
                }
                return R.fail("飞书用户未绑定本地账号");
            }
            
            LoginUser loginUser = userResult.getData();
            recordLogService.recordLogininfor(loginUser.getSysUser().getUserName(), Constants.LOGIN_SUCCESS, "飞书登录成功");
            
            // 生成 token
            return R.ok(tokenService.createToken(loginUser));
            
        } catch (Exception e) {
            log.error("飞书登录失败", e);
            return R.fail("飞书登录失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取 app_access_token
     */
    private String getAppAccessToken() {
        String url = "https://open.feishu.cn/open-apis/auth/v3/app_access_token/internal";
        
        Map<String, String> body = new HashMap<>();
        body.put("app_id", getAppId());
        body.put("app_secret", getAppSecret());
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<String> entity = new HttpEntity<>(JSON.toJSONString(body), headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
        
        JSONObject result = JSON.parseObject(response.getBody());
        if (result.getIntValue("code") != 0) {
            throw new ServiceException("获取飞书 app_access_token 失败: " + result.getString("msg"));
        }
        
        return result.getString("app_access_token");
    }
    
    /**
     * 用授权码换取 user_access_token
     */
    private JSONObject getUserAccessToken(String appAccessToken, String code) {
        String url = "https://open.feishu.cn/open-apis/authen/v1/oidc/access_token";
        
        Map<String, String> body = new HashMap<>();
        body.put("grant_type", "authorization_code");
        body.put("code", code);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + appAccessToken);
        
        HttpEntity<String> entity = new HttpEntity<>(JSON.toJSONString(body), headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
        
        JSONObject result = JSON.parseObject(response.getBody());
        if (result.getIntValue("code") != 0) {
            throw new ServiceException("获取飞书用户令牌失败: " + result.getString("msg"));
        }
        
        return result.getJSONObject("data");
    }
    
    /**
     * 获取飞书用户信息
     */
    private JSONObject getUserInfo(String userAccessToken) {
        String url = "https://open.feishu.cn/open-apis/authen/v1/user_info";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + userAccessToken);
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        
        JSONObject result = JSON.parseObject(response.getBody());
        if (result.getIntValue("code") != 0) {
            throw new ServiceException("获取飞书用户信息失败: " + result.getString("msg"));
        }
        
        return result.getJSONObject("data");
    }
}
