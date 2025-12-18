package com.ruoyi.system.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.api.domain.SysUser;
import com.ruoyi.system.mapper.SysUserMapper;

/**
 * 飞书用户服务
 */
@Service
public class FeishuUserService {
    
    @Autowired
    private SysUserMapper userMapper;
    
    /**
     * 根据飞书 UnionID 查找用户
     */
    public SysUser selectUserByFeishuUnionId(String unionId) {
        return userMapper.selectUserByFeishuUnionId(unionId);
    }
    
    /**
     * 根据飞书 OpenID 查找用户
     */
    public SysUser selectUserByFeishuOpenId(String openId) {
        return userMapper.selectUserByFeishuOpenId(openId);
    }
    
    /**
     * 绑定飞书账号
     */
    public int bindFeishuAccount(Long userId, String openId, String unionId) {
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setFeishuOpenId(openId);
        user.setFeishuUnionId(unionId);
        return userMapper.updateUser(user);
    }
    
    /**
     * 解绑飞书账号
     */
    public int unbindFeishuAccount(Long userId) {
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setFeishuOpenId("");
        user.setFeishuUnionId("");
        return userMapper.updateUser(user);
    }
}
