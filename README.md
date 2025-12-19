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

### 新增 6 个前端文件
| 文件 | 说明 |
|------|------|
| views/feishu/callback.vue | 飞书授权回调页面 |
| views/login_feishu_patch.vue | 登录页面飞书集成补丁文件 |
| api/feishu.js | 飞书 API 接口 |
| store/modules/feishu.js | Vuex 飞书模块 |
| router/feishu.js | 飞书路由配置 |
| assets/icons/svg/feishu.svg | 飞书图标 |

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
│       ├── views/feishu/callback.vue
│       ├── views/login_feishu_patch.vue
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

#### 3.1 复制 6 个前端文件
```
ruoyi-ui/src/views/feishu/callback.vue
ruoyi-ui/src/views/login_feishu_patch.vue
ruoyi-ui/src/api/feishu.js
ruoyi-ui/src/store/modules/feishu.js
ruoyi-ui/src/router/feishu.js
ruoyi-ui/src/assets/icons/svg/feishu.svg
```

#### 3.2 修改登录页面

按照 `login_feishu_patch.vue` 文件中的说明，在若依源码的 `login.vue` 中添加飞书登录功能：

```bash
# 参考增量修改文件
cat ruoyi-ui/src/views/login_feishu_patch.vue
# 按照文件中的注释说明修改若依源码的 login.vue
```

#### 3.3 修改前端配置

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

在 Nacos 配置中心修改网关配置文件 `ruoyi-gateway.yml`，添加飞书路由白名单：
```yaml
security:
  ignore:
    whites:
      # ... 其他白名单
      - /auth/feishu/**
```

**注意：** 请在对应环境的 Nacos 中进行配置

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

## 登录页面集成飞书按钮

### 实现效果
在若依原登录页面的登录按钮下方，添加一个飞书图标，点击后跳转到飞书OAuth授权页面。

### 集成步骤

#### 1. 复制增量修改文件
将 `login_feishu_patch.vue` 复制到若依前端项目：
```bash
cp ruoyi-ui/src/views/login_feishu_patch.vue /path/to/RuoYi-Cloud/ruoyi-ui/src/views/
```

#### 2. 复制飞书图标
将飞书SVG图标复制到若依前端项目：
```bash
cp ruoyi-ui/src/assets/icons/svg/feishu.svg /path/to/RuoYi-Cloud/ruoyi-ui/src/assets/icons/svg/
```

#### 3. 修改登录页面
按照 `login_feishu_patch.vue` 文件中的注释说明，在若依源码的 `login.vue` 中添加相应内容

#### 4. 确保依赖完整
确保以下文件已正确复制到若依项目：
- `api/feishu.js` - 飞书API接口
- `store/modules/feishu.js` - Vuex飞书模块（可选）
- `router/feishu.js` - 飞书路由配置

### 登录按钮样式说明

飞书登录图标特点：
- 位置：在原登录按钮下方
- 样式：简洁的飞书官方图标，居中显示
- 图标：使用官方飞书SVG图标
- 交互：点击后调用 `getFeishuAuthUrl(redirectUri)` 获取授权链接并跳转

### 具体修改内容

按照以下步骤在若依源码的 `login.vue` 中添加飞书登录功能：

#### 1. 模板部分（template）
在登录按钮的 `el-form-item` 后面添加：
```vue
<!-- 飞书登录图标 -->
<el-form-item style="width:100%; margin-top: 20px;">
  <div style="text-align: center;">
    <svg-icon 
      icon-class="feishu" 
      style="font-size: 32px; cursor: pointer;" 
      @click="handleFeishuLogin"
    />
  </div>
</el-form-item>
```

#### 2. 脚本部分（script）
在 import 中添加：
```javascript
import { getFeishuAuthUrl } from "@/api/feishu";
```

在 methods 中添加：
```javascript
handleFeishuLogin() {
  const redirectUri = window.location.origin + '/feishu/callback';
  getFeishuAuthUrl(redirectUri).then(res => {
    if (res.code === 200) {
      window.location.href = res.data;
    } else {
      this.$modal.msgError(res.msg || '获取飞书授权链接失败');
    }
  }).catch(() => {
    this.$modal.msgError('飞书登录服务异常');
  });
}
```

#### 3. 样式部分（style）
由于移除了分隔线，不需要添加额外的CSS样式。

### 注意事项

1. **图标注册**：确保飞书SVG图标已正确放置在 `assets/icons/svg/` 目录下
2. **API依赖**：确保 `api/feishu.js` 中的 `getFeishuAuthUrl` 方法可用
3. **样式兼容**：登录页面样式与若依原版保持一致，只在底部添加飞书登录区域
4. **错误处理**：包含完整的错误提示和异常处理

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

## 联系方式

- Email: pholex@gmail.com

---
最后更新: 2025-12-19
