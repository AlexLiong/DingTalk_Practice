import request from './request'

// ===== 认证 =====
export const apiLogin = (data) => request.post('/auth/login', data)
export const apiRegister = (data) => request.post('/auth/register', data)
export const apiUserInfo = () => request.get('/auth/info')

// ===== 通讯录 =====
export const apiUserList = () => request.get('/users')

// ===== 聊天 =====
export const apiSessions = () => request.get('/chat/sessions')
export const apiMessages = (sessionId) => request.get('/chat/messages', { params: { sessionId } })
export const apiSendMessage = (data) => request.post('/chat/messages', data)
export const apiRecall = (messageId) => request.post('/chat/recall', null, { params: { messageId } })
export const apiMarkRead = (sessionId) => request.post('/chat/read', null, { params: { sessionId } })
export const apiSingleChat = (targetUserId) => request.post('/chat/single', null, { params: { targetUserId } })

// ===== 群聊 =====
export const apiCreateGroup = (data) => request.post('/chat/group', data)
export const apiGroupInfo = (sessionId) => request.get(`/chat/group/${sessionId}`)
export const apiGroupMembers = (sessionId) => request.get(`/chat/group/${sessionId}/members`)
export const apiAddGroupMembers = (sessionId, userIds) => request.post(`/chat/group/${sessionId}/members`, { userIds })
export const apiRemoveGroupMember = (sessionId, userId) => request.delete(`/chat/group/${sessionId}/members/${userId}`)
export const apiQuitGroup = (sessionId) => request.post(`/chat/group/${sessionId}/quit`)
export const apiUpdateNotice = (sessionId, notice) => request.put(`/chat/group/${sessionId}/notice`, { notice })

// ===== 文件 =====
export const apiUpload = (formData, config = {}) => request.post('/file/upload', formData, config)
export const apiFileList = (params = {}) => request.get('/file/list', { params })
export const apiFileUpdate = (id, data) => request.put(`/file/${id}`, data)
export const apiFileDelete = (id) => request.delete(`/file/${id}`)
export const apiFileShare = (id, data = {}) => request.post(`/file/${id}/share`, data)
export const apiFileLock = (id) => request.post(`/file/${id}/lock`)
export const apiFileRecycle = (id) => request.post(`/file/${id}/recycle`)

// ===== 添加面板 =====
export const apiAddPanelLinks = () => request.get('/add-panel/links')
export const apiAddPanelSaveLink = (data) => request.post('/add-panel/links', data)
export const apiAddPanelDeleteLink = (id) => request.delete(`/add-panel/links/${id}`)
export const apiAddPanelRecentUsage = () => request.get('/add-panel/recent-usage')
export const apiAddPanelRememberUsage = (itemKey) => request.post('/add-panel/recent-usage', { itemKey })
export const apiAddPanelClearUsage = () => request.delete('/add-panel/recent-usage')

// ===== 邮箱 =====
export const apiMailboxList = () => request.get('/mailbox/list')
export const apiMailboxReset = () => request.post('/mailbox/reset')
export const apiMailboxSend = (data) => request.post('/mailbox/send', data)
export const apiMailboxOpen = (id) => request.post(`/mailbox/${id}/open`)
export const apiMailboxUpdateRead = (id, unread) => request.put(`/mailbox/${id}/read`, { unread })
export const apiMailboxUpdateStar = (id, starred) => request.put(`/mailbox/${id}/star`, { starred })
export const apiMailboxArchive = (id) => request.put(`/mailbox/${id}/archive`)
export const apiMailboxSaveDraft = (id, draft) => request.put(`/mailbox/${id}/draft`, { draft })
export const apiMailboxReply = (id, content) => request.post(`/mailbox/${id}/reply`, { content })

// ===== DING =====
export const apiDingList = () => request.get('/ding/list')
export const apiDingReset = () => request.post('/ding/reset')
export const apiDingSend = (data) => request.post('/ding/send', data)
export const apiDingConfirm = (id) => request.post(`/ding/${id}/confirm`)
export const apiDingDone = (id) => request.post(`/ding/${id}/done`)
export const apiDingRemind = (id) => request.post(`/ding/${id}/remind`)
export const apiDingSaveNote = (id, note) => request.put(`/ding/${id}/note`, { note })

// ===== 个人中心 =====
export const apiUpdateProfile = (data) => request.put('/profile', data)
export const apiChangePassword = (data) => request.put('/profile/password', data)
export const apiUserPreference = () => request.get('/user-preference')
export const apiUpdateUserPreference = (data) => request.put('/user-preference', data)

// ===== 后台: 用户 =====
export const apiAdminUserPage = (params) => request.get('/admin/user/page', { params })
export const apiAdminUserRoles = (id) => request.get(`/admin/user/${id}/roles`)
export const apiAdminUserAdd = (data) => request.post('/admin/user', data)
export const apiAdminUserUpdate = (data) => request.put('/admin/user', data)
export const apiAdminUserDelete = (id) => request.delete(`/admin/user/${id}`)
export const apiAdminUserResetPwd = (id, password) => request.put(`/admin/user/${id}/password`, { password })

// ===== 后台: 角色 =====
export const apiRolePage = (params) => request.get('/role/page', { params })
export const apiRoleAll = () => request.get('/role/all')
export const apiRoleMenus = (id) => request.get(`/role/${id}/menus`)
export const apiRoleAdd = (data) => request.post('/role', data)
export const apiRoleUpdate = (data) => request.put('/role', data)
export const apiRoleDelete = (id) => request.delete(`/role/${id}`)

// ===== 后台: 菜单 =====
export const apiMenuTree = () => request.get('/menu/tree')
export const apiMenuAdd = (data) => request.post('/menu', data)
export const apiMenuUpdate = (data) => request.put('/menu', data)
export const apiMenuDelete = (id) => request.delete(`/menu/${id}`)

// ===== 后台: 部门 =====
export const apiDeptTree = () => request.get('/dept/tree')
export const apiDeptAdd = (data) => request.post('/dept', data)
export const apiDeptUpdate = (data) => request.put('/dept', data)
export const apiDeptDelete = (id) => request.delete(`/dept/${id}`)

// ===== 通知公告 =====
export const apiNoticeList = (params) => request.get('/notice/list', { params })
export const apiNoticeDetail = (id) => request.get(`/notice/${id}`)
export const apiNoticeAdminList = (params) => request.get('/notice/admin/list', { params })
export const apiNoticeAdd = (data) => request.post('/notice', data)
export const apiNoticeUpdate = (data) => request.put('/notice', data)
export const apiNoticeDelete = (id) => request.delete(`/notice/${id}`)

// ===== 待办 =====
export const apiTodoList = (params) => request.get('/todo/list', { params })
export const apiTodoStats = () => request.get('/todo/stats')
export const apiTodoAdd = (data) => request.post('/todo', data)
export const apiTodoUpdate = (data) => request.put('/todo', data)
export const apiTodoUpdateStatus = (id, status) => request.put(`/todo/${id}/status`, { status })
export const apiTodoDelete = (id) => request.delete(`/todo/${id}`)

// ===== 审批 =====
export const apiApprovalList = () => request.get('/approval/list')
export const apiApprovalPending = () => request.get('/approval/pending')
export const apiApprovalApplied = () => request.get('/approval/applied')
export const apiApprovalApply = (data) => request.post('/approval', data)
export const apiApprovalApprove = (id, data) => request.put(`/approval/${id}`, data)

// ===== 已读回执/搜索/置顶 =====
export const apiReadReceipt = (sessionId, lastMsgId) => request.post('/chat/read-receipt', null, { params: { sessionId, lastMsgId } })
export const apiMessageReaders = (messageId) => request.get('/chat/message-readers', { params: { messageId } })
export const apiSearchMessages = (keyword) => request.get('/chat/search', { params: { keyword } })
export const apiToggleTop = (sessionId) => request.post('/chat/toggle-top', null, { params: { sessionId } })

// ===== 在线状态 =====
export const apiOnlineUsers = () => request.get('/online/users')

// ===== 免打扰/特别关注 =====
export const apiToggleMute = (sessionId) => request.post('/chat/toggle-mute', null, { params: { sessionId } })
export const apiToggleStar = (sessionId) => request.post('/chat/toggle-star', null, { params: { sessionId } })

// ===== 消息反应 =====
export const apiGetReactions = (messageId) => request.get(`/reaction/${messageId}`)
export const apiToggleReaction = (data) => request.post('/reaction', data)

// ===== AI助手 =====
export const apiAiReply = (data) => request.post('/chat/ai-reply', data)

// ===== 收藏 =====
export const apiFavoriteList = () => request.get('/favorite/list')
export const apiFavoriteAdd = (data) => request.post('/favorite', data)
export const apiFavoriteDelete = (id) => request.delete(`/favorite/${id}`)

// ===== 日程 =====
export const apiScheduleList = (params) => request.get('/schedule/list', { params })
export const apiScheduleAdd = (data) => request.post('/schedule', data)
export const apiScheduleUpdate = (data) => request.put('/schedule', data)
export const apiScheduleDelete = (id) => request.delete(`/schedule/${id}`)

// ===== 看板 =====
export const apiDashOverview = () => request.get('/dashboard/overview')
export const apiDashDept = () => request.get('/dashboard/dept-distribution')
export const apiDashTrend = () => request.get('/dashboard/message-trend')
