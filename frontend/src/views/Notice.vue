<template>
  <div class="notice-shell">
    <AppSideNav active-key="notice" />

    <div class="notice-page">
      <div class="notice-top">
        <div class="notice-top-inner">
          <div class="page-title-cluster">
            <el-button class="page-back-btn" :icon="Back" @click="router.push('/chat')">返回消息</el-button>
            <div class="page-title-block">
              <div class="page-section-label">协同应用</div>
              <div class="page-heading-row">
                <span class="ptitle">通知公告</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="notice-body">
      <div v-if="!list.length && !loading" class="empty-tip">
        <el-empty description="暂无通知公告" />
      </div>

      <div v-loading="loading" class="card-list">
        <div
          v-for="item in list"
          :key="item.id"
          class="notice-card"
          :class="{ expanded: expandedId === item.id }"
          @click="toggle(item)"
        >
          <div class="card-header">
            <span v-if="!item.read" class="unread-dot"></span>
            <span class="card-title">{{ item.title }}</span>
            <el-tag size="small" :type="item.type === 1 ? 'primary' : 'warning'" class="tag-gap">
              {{ item.type === 1 ? '通知' : '公告' }}
            </el-tag>
            <el-tag size="small" :type="item.priority === 1 ? 'danger' : 'info'" class="tag-gap">
              {{ item.priority === 1 ? '紧急' : '普通' }}
            </el-tag>
          </div>
          <div class="card-meta">
            <span>{{ item.publisherName || '系统' }}</span>
            <span>{{ item.createTime }}</span>
            <span class="read-info">已读 {{ item.readCount ?? 0 }}/{{ item.totalCount ?? 0 }}</span>
          </div>
          <transition name="fade">
            <div v-if="expandedId === item.id" class="card-detail">
              <el-divider style="margin: 10px 0" />
              <div class="detail-content" v-html="detailContent"></div>
            </div>
          </transition>
        </div>
      </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Back } from '@element-plus/icons-vue'
import AppSideNav from '../components/AppSideNav.vue'
import { apiNoticeList, apiNoticeDetail } from '../api'

const router = useRouter()
const list = ref([])
const loading = ref(false)
const expandedId = ref(null)
const detailContent = ref('')

onMounted(() => {
  load()
})

async function load() {
  loading.value = true
  try {
    const data = await apiNoticeList({ status: 1 })
    list.value = Array.isArray(data) ? data : (data.records || data.list || [])
  } finally {
    loading.value = false
  }
}

async function toggle(item) {
  if (expandedId.value === item.id) {
    expandedId.value = null
    detailContent.value = ''
    return
  }
  expandedId.value = item.id
  detailContent.value = '加载中...'
  try {
    const detail = await apiNoticeDetail(item.id)
    detailContent.value = detail.content || '暂无详细内容'
    if (!item.read) {
      item.read = true
      item.readCount = (item.readCount || 0) + 1
    }
  } catch {
    detailContent.value = '加载失败'
  }
}
</script>

<style scoped>
.notice-shell {
  height: 100%;
  display: flex;
  background: var(--dt-bg);
}
.notice-page {
  flex: 1;
  min-width: 0;
  height: 100%;
  background: #f3f5f8;
  display: flex;
  flex-direction: column;
}
.notice-top {
  padding: 16px 24px 14px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96) 0%, rgba(247, 249, 252, 0.92) 100%);
  border-bottom: 1px solid rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(18px);
  flex-shrink: 0;
}
.notice-top-inner {
  width: 100%;
  max-width: 1260px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  gap: 18px;
}
.page-title-cluster {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 14px;
}
.page-title-block { min-width: 0; }
.page-section-label {
  margin-bottom: 4px;
  color: #98a2b3;
  font-size: 11px;
  font-weight: 600;
}
.page-heading-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.page-back-btn {
  height: 36px;
  padding: 0 14px;
  border-radius: 10px;
  border-color: rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.82);
  color: #344054;
  flex-shrink: 0;
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.04);
}
.page-back-btn:hover {
  color: #1677ff;
  border-color: rgba(22, 119, 255, 0.22);
  background: #fff;
}
.ptitle {
  font-size: 20px;
  line-height: 1.15;
  font-weight: 700;
  color: #1d2129;
}
.notice-body {
  flex: 1;
  overflow-y: auto;
  padding: 18px 24px 24px;
}
.card-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-width: 800px;
  margin: 0 auto;
}
.notice-card {
  background: #fff;
  border-radius: 10px;
  padding: 16px 20px;
  cursor: pointer;
  transition: box-shadow 0.2s;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  position: relative;
}
.notice-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}
.notice-card.expanded {
  box-shadow: 0 4px 12px rgba(22, 119, 255, 0.12);
}
.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #1d2129;
}
.tag-gap { margin-left: 2px; }
.unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #1677ff;
  flex-shrink: 0;
}
.card-meta {
  display: flex;
  gap: 16px;
  margin-top: 10px;
  font-size: 13px;
  color: #8c8c8c;
}
.read-info { margin-left: auto; }
.card-detail { padding-top: 4px; }
.detail-content {
  font-size: 14px;
  color: #333;
  line-height: 1.8;
  white-space: pre-wrap;
}
.empty-tip { padding: 80px 0; }
.fade-enter-active, .fade-leave-active { transition: opacity 0.25s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
@media (max-width: 960px) {
  .notice-top {
    padding: 14px 16px 12px;
  }
  .notice-body {
    padding: 16px;
  }
}
</style>
