# RuoYi-Cloud-Feishu 飞书集成方案

## 概述
本项目为 RuoYi-Cloud 微服务架构集成飞书工作台登录的扩展方案，保持原有架构不变，通过最小化改动实现飞书 OAuth2.0 登录。

## 改动汇总

### 新增 4 个 Java 文件
| 文件 | 模块 | 说明 |
|------|------|------|
| FeishuAuthController.java | ruoyi-auth | 飞书认证控制器 |
| FeishuAuthService.java | ruoyi-auth | 飞书认证服务 |
| FeishuLoginBody.java | ruoyi-auth | 登录请求体 |
| FeishuUserService.java | ruoyi-system | 飞书用户服务 |

### 修改 6 个若依源文件
| 文件 | 修改内容 |
|------|----------|
| SysUser.java | 添加 `feishuOpenId`、`feishuUnionId` 字段及 getter/setter |
| SysUserMapper.java | 添加 `selectUserByFeishuUnionId`、`selectUserByFeishuOpenId` 方法 |
| SysUserMapper.xml | 添加对应的 SQL 查询语句 |
| RemoteUserService.java | 添加 `getUserInfoByFeishuUnionId` 方法 |
| RemoteUserFallbackFactory.java | 添加对应的降级方法 |
| SysUserController.java | 添加根据飞书 UnionID 查询用户的接口 |

### 新增 5 个前端文件
| 文件 | 说明 |
|------|------|
| views/feishu/login.vue | 飞书登录页面（扩展原登录页） |
| views/feishu/callback.vue | 飞书授权回调页面 |
| api/feishu.js | 飞书 API 接口 |
| store/modules/feishu.js | Vuex 飞书模块 |
| router/feishu.js | 飞书路由配置 |

## 目录结构
```
ruoyi-cloud-feishu/
├── ruoyi-auth/                    # 认证中心扩展
│   └── src/main/java/com/ruoyi/auth/
│       ├── controller/FeishuAuthController.java
│       ├── form/FeishuLoginBody.java
│       └── service/FeishuAuthService.java
├── ruoyi-modules/ruoyi-system/    # 系统模块扩展
│   └── src/main/
│       ├── java/.../service/FeishuUserService.java
│       └── resources/mapper/system/SysUserMapperExt.xml
├── ruoyi-ui/                      # 前端扩展
│   └── src/
│       ├── views/feishu/login.vue
│       ├── views/feishu/callback.vue
│       ├── api/feishu.js
│       ├── store/modules/feishu.js
│       └── router/feishu.js
├── sql/
│   ├── feishu_extension.sql       # 数据库扩展
│   └── feishu_config_init.sql     # 配置初始化
└── README.md
```

## 集成步骤

### 1. 数据库扩展
```bash
# 执行 SQL 脚本
mysql -u用户名 -p密码 数据库名 < sql/feishu_extension.sql
mysql -u用户名 -p密码 数据库名 < sql/feishu_config_init.sql
```

### 2. 后端集成

#### 2.1 复制 4 个新增文件

**ruoyi-auth 模块（3 个文件）**
```
ruoyi-auth/src/main/java/com/ruoyi/auth/controller/FeishuAuthController.java
ruoyi-auth/src/main/java/com/ruoyi/auth/form/FeishuLoginBody.java
ruoyi-auth/src/main/java/com/ruoyi/auth/service/FeishuAuthService.java
```

**ruoyi-system 模块（1 个文件）**
```
ruoyi-modules/ruoyi-system/src/main/java/com/ruoyi/system/service/FeishuUserService.java
```

#### 2.2 修改 6 个若依源文件

**① SysUser.java**

位置: `ruoyi-api/ruoyi-api-system/src/main/java/com/ruoyi/system/api/domain/SysUser.java`

添加字段：
```java
/** 飞书OpenID */
private String feishuOpenId;

/** 飞书UnionID */
private String feishuUnionId;

public String getFeishuOpenId() {
    return feishuOpenId;
}

public void setFeishuOpenId(String feishuOpenId) {
    this.feishuOpenId = feishuOpenId;
}

public String getFeishuUnionId() {
    return feishuUnionId;
}

public void setFeishuUnionId(String feishuUnionId) {
    this.feishuUnionId = feishuUnionId;
}
```

**② SysUserMapper.java**

位置: `ruoyi-modules/ruoyi-system/src/main/java/com/ruoyi/system/mapper/SysUserMapper.java`

添加方法：
```java
/**
 * 根据飞书UnionID查询用户
 */
SysUser selectUserByFeishuUnionId(String unionId);

/**
 * 根据飞书OpenID查询用户
 */
SysUser selectUserByFeishuOpenId(String openId);
```

**③ SysUserMapper.xml**

位置: `ruoyi-modules/ruoyi-system/src/main/resources/mapper/system/SysUserMapper.xml`

添加 SQL：
```xml
<select id="selectUserByFeishuUnionId" parameterType="String" resultMap="SysUserResult">
    <include refid="selectUserVo"/>
    where u.feishu_union_id = #{unionId} and u.del_flag = '0'
</select>

<select id="selectUserByFeishuOpenId" parameterType="String" resultMap="SysUserResult">
    <include refid="selectUserVo"/>
    where u.feishu_open_id = #{openId} and u.del_flag = '0'
</select>
```

**④ RemoteUserService.java**

位置: `ruoyi-api/ruoyi-api-system/src/main/java/com/ruoyi/system/api/RemoteUserService.java`

添加方法：
```java
/**
 * 根据飞书 UnionID 查询用户信息
 */
@GetMapping("/user/feishu/{unionId}")
public R<LoginUser> getUserInfoByFeishuUnionId(@PathVariable("unionId") String unionId, 
    @RequestHeader(SecurityConstants.FROM_SOURCE) String source);
```

**⑤ RemoteUserFallbackFactory.java**

位置: `ruoyi-api/ruoyi-api-system/src/main/java/com/ruoyi/system/api/factory/RemoteUserFallbackFactory.java`

在 `create()` 方法返回的匿名类中添加：
```java
@Override
public R<LoginUser> getUserInfoByFeishuUnionId(String unionId, String source) {
    return R.fail("获取飞书用户失败:" + throwable.getMessage());
}
```

**⑥ SysUserController.java**

位置: `ruoyi-modules/ruoyi-system/src/main/java/com/ruoyi/system/controller/SysUserController.java`

添加依赖注入：
```java
@Autowired
private com.ruoyi.system.service.FeishuUserService feishuUserService;
```

添加接口：
```java
/**
 * 根据飞书UnionID获取用户信息
 */
@InnerAuth
@GetMapping("/feishu/{unionId}")
public R<LoginUser> getUserInfoByFeishuUnionId(@PathVariable String unionId) {
    SysUser sysUser = feishuUserService.selectUserByFeishuUnionId(unionId);
    if (StringUtils.isNull(sysUser)) {
        return R.fail("飞书用户未绑定本地账号");
    }
    // 角色集合
    Set<String> roles = permissionService.getRolePermission(sysUser);
    // 权限集合
    Set<String> permissions = permissionService.getMenuPermission(sysUser);
    LoginUser sysUserVo = new LoginUser();
    sysUserVo.setSysUser(sysUser);
    sysUserVo.setRoles(roles);
    sysUserVo.setPermissions(permissions);
    return R.ok(sysUserVo);
}
```

### 3. 前端集成

#### 3.1 复制 5 个前端文件
```
ruoyi-ui/src/views/feishu/login.vue
ruoyi-ui/src/views/feishu/callback.vue
ruoyi-ui/src/api/feishu.js
ruoyi-ui/src/store/modules/feishu.js
ruoyi-ui/src/router/feishu.js
```

#### 3.2 修改前端配置

**router/index.js** - 引入飞书路由：
```javascript
import feishuRouter from './feishu'

export const constantRoutes = [
  // ... 其他路由
  feishuRouter
]
```

**store/index.js** - 注册飞书模块（可选）：
```javascript
import feishu from './modules/feishu'

const store = new Vuex.Store({
  modules: {
    // ... 其他模块
    feishu
  }
})
```

**permission.js** - 将飞书回调加入白名单：
```javascript
const whiteList = ['/login', '/register', '/feishu/callback']
```

### 4. 网关配置

在 Nacos 配置中心修改 `ruoyi-gateway-dev.yml`，添加飞书路由白名单：
```yaml
security:
  ignore:
    whites:
      # ... 其他白名单
      - /auth/feishu/**
```

### 5. 飞书应用配置

1. 登录 [飞书开放平台](https://open.feishu.cn/)
2. 创建企业自建应用
3. 配置重定向 URL：`https://your-domain/feishu/callback`
4. 配置工作台应用首页 URL：`https://your-domain/feishu/callback?auto=1`（支持自动登录）
5. 获取 App ID 和 App Secret
6. 在系统管理 → 参数设置中配置相关信息

## 配置说明

配置存储在 `sys_config` 表中，会被缓存到 Redis（key 格式：`sys_config:配置键`）

| 配置键 | 说明 |
|--------|------|
| feishu.app.id | 飞书应用 ID |
| feishu.app.secret | 飞书应用密钥 |
| feishu.redirect.uri | OAuth 回调地址 |
| feishu.login.enabled | 是否启用飞书登录 |
| feishu.user.auto.create | 是否自动创建用户 |

## 登录流程

```
用户点击"飞书登录"
       ↓
前端调用 /feishu/auth-url 获取授权 URL
       ↓
跳转飞书授权页面，用户授权
       ↓
飞书回调 /feishu/callback，携带 code 和 state
       ↓
前端调用 /feishu/login 完成登录
       ↓
后端验证 state，用 code 换取 token，获取用户信息
       ↓
根据 union_id 查找本地用户，生成 JWT token
       ↓
登录成功，跳转首页
```

## 安全说明

- 使用 state 参数防止 CSRF 攻击
- state 存储在 Redis，有效期 5 分钟
- 飞书配置存储在数据库，通过 Redis 缓存读取
