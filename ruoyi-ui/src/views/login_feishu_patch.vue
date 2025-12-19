<!-- 
  在若依源码 login.vue 中需要添加的内容
  请按照以下步骤修改原 login.vue 文件
-->

<!-- 1. 在 template 中，找到登录按钮的 el-form-item，在其后面添加以下内容： -->
<template>
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
</template>

<script>
// 2. 在 script 的 import 部分添加：
import { getFeishuAuthUrl } from "@/api/feishu";

// 3. 在 methods 中添加以下方法：
export default {
  methods: {
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
  }
}
</script>

<style rel="stylesheet/scss" lang="scss">
/* 4. 在 style 中添加以下样式： */
.third-party-login {
  .divider {
    text-align: center;
    margin: 10px 0;
    position: relative;
    
    &::before {
      content: '';
      position: absolute;
      top: 50%;
      left: 0;
      right: 0;
      height: 1px;
      background: #e4e7ed;
    }
    
    span {
      background: white;
      padding: 0 15px;
      color: #909399;
      font-size: 14px;
    }
  }
}
</style>
