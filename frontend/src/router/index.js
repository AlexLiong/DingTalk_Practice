import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', redirect: '/chat' },
  { path: '/login', name: 'login', component: () => import('../views/Login.vue') },
  { path: '/chat', name: 'chat', component: () => import('../views/Workbench.vue'), meta: { auth: true } },
  { path: '/documents', name: 'documents', component: () => import('../views/Documents.vue'), meta: { auth: true } },
  { path: '/mailbox', name: 'mailbox', component: () => import('../views/Mailbox.vue'), meta: { auth: true } },
  { path: '/ding', name: 'ding', component: () => import('../views/Ding.vue'), meta: { auth: true } },
  { path: '/profile', name: 'profile', component: () => import('../views/Profile.vue'), meta: { auth: true } },
  { path: '/notice', name: 'notice', component: () => import('../views/Notice.vue'), meta: { auth: true } },
  { path: '/todo', name: 'todo', component: () => import('../views/Todo.vue'), meta: { auth: true } },
  { path: '/approval', name: 'approval', component: () => import('../views/Approval.vue'), meta: { auth: true } },
  { path: '/calendar', name: 'calendar', component: () => import('../views/Calendar.vue'), meta: { auth: true } },
  { path: '/favorites', name: 'favorites', component: () => import('../views/Favorites.vue'), meta: { auth: true } },
  {
    path: '/admin',
    component: () => import('../layout/AdminLayout.vue'),
    meta: { auth: true },
    redirect: '/admin/user',
    children: [
      { path: 'user', name: 'admin-user', component: () => import('../views/admin/UserManage.vue') },
      { path: 'role', name: 'admin-role', component: () => import('../views/admin/RoleManage.vue') },
      { path: 'menu', name: 'admin-menu', component: () => import('../views/admin/MenuManage.vue') },
      { path: 'dept', name: 'admin-dept', component: () => import('../views/admin/DeptManage.vue') },
      { path: 'dashboard', name: 'admin-dashboard', component: () => import('../views/admin/Dashboard.vue') },
      { path: 'notice', name: 'admin-notice', component: () => import('../views/admin/NoticeManage.vue') }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.auth && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/chat')
  } else {
    next()
  }
})

export default router
