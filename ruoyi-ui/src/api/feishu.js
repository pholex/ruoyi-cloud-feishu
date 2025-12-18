import request from '@/utils/request'

// 获取飞书授权URL
export function getFeishuAuthUrl(redirectUri) {
  return request({
    url: '/auth/feishu/auth-url',
    method: 'get',
    params: { redirectUri },
    headers: {
      isToken: false
    }
  })
}

// 飞书登录
export function feishuLogin(code, state) {
  return request({
    url: '/auth/feishu/login',
    method: 'post',
    data: { code, state },
    headers: {
      isToken: false,
      repeatSubmit: false
    }
  })
}
