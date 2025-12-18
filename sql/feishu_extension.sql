-- 扩展用户表，添加飞书相关字段
ALTER TABLE sys_user 
ADD COLUMN feishu_open_id VARCHAR(64) COMMENT '飞书OpenID',
ADD COLUMN feishu_union_id VARCHAR(64) COMMENT '飞书UnionID';

-- 为飞书字段添加索引（union_id 为主要查找字段）
CREATE INDEX idx_sys_user_feishu_union_id ON sys_user(feishu_union_id);
CREATE INDEX idx_sys_user_feishu_open_id ON sys_user(feishu_open_id);
