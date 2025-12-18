<template>
  <div class="feishu-callback" v-loading="loading" element-loading-text="飞书登录处理中...">
    <div v-if="!loading && error" class="error-container">
      <el-alert
        :title="error"
        type="error"
        show-icon
        :closable="false"
      />
      <el-button 
        type="primary" 
        style="margin-top: 20px;"
        @click="backToLogin"
      >
        返回登录
      </el-button>
    </div>
  </div>
</template>

<script>
import { feishuLogin, getFeishuAuthUrl } from '@/api/feishu'
import { setToken, setExpiresIn } from '@/utils/auth'

export default {
  name: 'FeishuCallback',
  data() {
    return {
      loading: true,
      error: null
    }
  },
  mounted() {
    this.handleCallback()
  },
  methods: {
    handleCallback() {
      const code = this.$route.query.code
      const state = this.$route.query.state
      const auto = this.$route.query.auto
      
      // 如果没有 code 但有 auto 参数，自动跳转飞书授权
      if (!code && auto) {
        this.redirectToFeishu()
        return
      }
      
      if (!code || !state) {
        this.error = '飞书授权参数缺失'
        this.loading = false
        return
      }
      
      feishuLogin(code, state).then(res => {
        const data = res.data
        setToken(data.access_token)
        setExpiresIn(data.expires_in)
        this.$store.commit('SET_TOKEN', data.access_token)
        this.$message.success('飞书登录成功')
        this.$router.push('/')
      }).catch(err => {
        this.error = err.message || '飞书登录失败'
        this.loading = false
      })
    },
    redirectToFeishu() {
      const redirectUri = encodeURIComponent(window.location.origin + '/feishu/callback')
      getFeishuAuthUrl(redirectUri).then(res => {
        window.location.href = res.data
      }).catch(err => {
        this.error = '获取飞书授权链接失败'
        this.loading = false
      })
    },
    backToLogin() {
      this.$router.push('/login')
    }
  }
}
</script>

<style scoped>
.feishu-callback {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #f5f5f5;
}

.error-container {
  text-align: center;
  padding: 40px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  max-width: 400px;
}
</style>
