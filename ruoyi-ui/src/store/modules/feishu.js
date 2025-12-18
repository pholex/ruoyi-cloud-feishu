/**
 * 飞书登录扩展
 * 
 * 使用方式：在 store/index.js 中注册此模块
 * 
 * import feishu from './modules/feishu'
 * 
 * const store = new Vuex.Store({
 *   modules: {
 *     ...
 *     feishu
 *   }
 * })
 * 
 * 或者直接将 FeishuLogin action 添加到现有的 user.js 模块中
 */

import { feishuLogin } from '@/api/feishu'
import { setToken, setExpiresIn } from '@/utils/auth'

const feishu = {
  actions: {
    // 飞书登录
    FeishuLogin({ commit }, { code, state }) {
      return new Promise((resolve, reject) => {
        feishuLogin(code, state).then(res => {
          const data = res.data
          setToken(data.access_token)
          setExpiresIn(data.expires_in)
          commit('SET_TOKEN', data.access_token, { root: true })
          resolve()
        }).catch(error => {
          reject(error)
        })
      })
    }
  }
}

export default feishu
