/**
 * 飞书路由配置
 */

const feishuRouter = {
  path: '/feishu/callback',
  component: () => import('@/views/feishu/callback'),
  hidden: true,
  meta: { title: '飞书登录' }
}

export default feishuRouter
