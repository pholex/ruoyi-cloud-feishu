-- 飞书配置参数初始化
-- 插入飞书相关系统配置参数

-- 飞书应用ID
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark) 
VALUES ('飞书应用ID', 'feishu.app.id', 'your_app_id_here', 'N', 'admin', NOW(), '飞书开放平台应用ID，用于OAuth2.0认证');

-- 飞书应用密钥
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark) 
VALUES ('飞书应用密钥', 'feishu.app.secret', 'your_app_secret_here', 'N', 'admin', NOW(), '飞书开放平台应用密钥，用于获取访问令牌');

-- 飞书回调地址
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark) 
VALUES ('飞书回调地址', 'feishu.redirect.uri', 'http://localhost/feishu/callback', 'N', 'admin', NOW(), '飞书OAuth2.0授权回调地址');

-- 飞书登录开关
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark) 
VALUES ('飞书登录开关', 'feishu.login.enabled', 'true', 'N', 'admin', NOW(), '是否启用飞书登录功能（true启用，false禁用）');

-- 飞书用户自动创建开关
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark) 
VALUES ('飞书用户自动创建', 'feishu.user.auto.create', 'true', 'N', 'admin', NOW(), '飞书用户首次登录时是否自动创建本地账号（true自动创建，false需要预先创建）');
