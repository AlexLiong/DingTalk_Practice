<template>
  <div class="favorites-shell">
    <AppSideNav active-key="favorites" />

    <div class="favorites-page">
      <div class="favorites-top">
        <div class="favorites-top-inner">
          <div class="page-title-cluster">
            <el-button class="page-back-btn" :icon="Back" @click="$router.push('/chat')">返回消息</el-button>
            <div class="page-title-block">
              <div class="page-section-label">协同应用</div>
              <div class="page-heading-row">
                <span class="ptitle">我的收藏</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="favorites-body">
      <!-- Empty state -->
      <div v-if="!loading && list.length === 0" class="empty-wrap">
        <el-empty description="暂无收藏内容" :image-size="120" />
      </div>

      <!-- Card list -->
      <div v-else class="fav-list">
        <div v-for="item in list" :key="item.id" class="fav-card">
          <div class="fav-header">
            <span class="sender-name">{{ item.senderName || '未知用户' }}</span>
            <span class="fav-time">{{ formatTime(item.collectTime || item.createTime || item.createdAt) }}</span>
          </div>

          <div class="fav-content">
            <!-- Text message -->
            <div v-if="item.msgType === 'text' || !item.msgType" class="fav-text">
              {{ item.content }}
            </div>

            <!-- Image message -->
            <div v-else-if="item.msgType === 'image'" class="fav-image">
              <el-image
                :src="item.fileUrl || item.content"
                fit="cover"
                style="max-width: 240px; max-height: 180px; border-radius: 6px;"
                :preview-src-list="[item.fileUrl || item.content]"
                preview-teleported
              />
            </div>

            <!-- File message -->
            <div v-else-if="item.msgType === 'file'" class="fav-file">
              <div class="file-preview">
                <el-icon :size="28" color="#909399"><Document /></el-icon>
                <div class="file-info">
                  <span class="file-name">{{ item.fileName || '文件' }}</span>
                  <span class="file-size">{{ formatSize(item.fileSize) }}</span>
                </div>
              </div>
            </div>

            <!-- Fallback -->
            <div v-else class="fav-text">{{ item.content || '[不支持的消息类型]' }}</div>
          </div>

          <div class="fav-footer">
            <el-button
              text
              type="danger"
              size="small"
              :icon="Delete"
              @click="handleDelete(item)"
            >取消收藏</el-button>
          </div>
        </div>
      </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Back, Delete } from '@element-plus/icons-vue'
import AppSideNav from '../components/AppSideNav.vue'
import { apiFavoriteList, apiFavoriteDelete } from '../api'
import { Document } from '@element-plus/icons-vue'

const router = useRouter()
const list = ref([])
const loading = ref(false)

async function loadList() {
  loading.value = true
  try {
    const res = await apiFavoriteList()
    list.value = Array.isArray(res) ? res : (res.list || res.data || [])
  } catch {
    list.value = []
  } finally {
    loading.value = false
  }
}

async function handleDelete(item) {
  try {
    await ElMessageBox.confirm('确定取消收藏该条消息？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await apiFavoriteDelete(item.id)
    ElMessage.success('已取消收藏')
    list.value = list.value.filter(f => f.id !== item.id)
  } catch {
    /* user cancelled */
  }
}

function formatTime(t) {
  if (!t) return ''
  const d = new Date(t)
  if (isNaN(d.getTime())) return t
  const now = new Date()
  const isToday =
    d.getFullYear() === now.getFullYear() &&
    d.getMonth() === now.getMonth() &&
    d.getDate() === now.getDate()
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  const hh = String(d.getHours()).padStart(2, '0')
  const mi = String(d.getMinutes()).padStart(2, '0')
  if (isToday) return `今天 ${hh}:${mi}`
  if (d.getFullYear() === now.getFullYear()) return `${mm}-${dd} ${hh}:${mi}`
  return `${d.getFullYear()}-${mm}-${dd} ${hh}:${mi}`
}

function formatSize(bytes) {
  if (!bytes) return ''
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(1) + ' MB'
}

onMounted(() => {
  loadList()
})
</script>

<style scoped>
.favorites-shell {
  height: 100%;
  display: flex;
  background: var(--dt-bg);
}
.favorites-page {
  flex: 1;
  min-width: 0;
  height: 100%;
  background: #f3f5f8;
  display: flex;
  flex-direction: column;
}

.favorites-top {
  padding: 16px 24px 14px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96) 0%, rgba(247, 249, 252, 0.92) 100%);
  border-bottom: 1px solid rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(18px);
}
.favorites-top-inner {
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

/* ---------- body ---------- */
.favorites-body {
  flex: 1;
  overflow-y: auto;
  padding: 18px 24px 24px;
}

.empty-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 360px;
}

/* ---------- card list ---------- */
.fav-list {
  max-width: 720px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.fav-card {
  background: #fff;
  border-radius: 8px;
  padding: 18px 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  transition: box-shadow 0.2s;
}
.fav-card:hover { box-shadow: 0 3px 12px rgba(0, 0, 0, 0.1); }

/* ---------- header ---------- */
.fav-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.sender-name { font-size: 14px; font-weight: 600; color: #303133; }
.fav-time { font-size: 12px; color: #999; }

/* ---------- content ---------- */
.fav-content { margin-bottom: 12px; }
.fav-text {
  font-size: 14px;
  color: #444;
  line-height: 1.65;
  white-space: pre-wrap;
  word-break: break-all;
}

.file-preview {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #f5f7fa;
  border-radius: 6px;
  padding: 12px 14px;
  max-width: 320px;
}
.file-info { display: flex; flex-direction: column; }
.file-name {
  font-size: 14px;
  color: #303133;
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.file-size { font-size: 12px; color: #999; margin-top: 2px; }

/* ---------- footer ---------- */
.fav-footer {
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid #f0f0f0;
  padding-top: 10px;
}
@media (max-width: 960px) {
  .favorites-top {
    padding: 14px 16px 12px;
  }
  .favorites-body {
    padding: 16px;
  }
}
@media (max-width: 768px) {
  .favorites-top {
    padding: 10px 12px 8px;
  }
  .page-back-btn {
    height: 32px;
    padding: 0 10px;
    font-size: 13px;
  }
  .ptitle {
    font-size: 17px;
  }
  .page-section-label {
    display: none;
  }
  .favorites-body {
    padding: 10px 12px;
    padding-bottom: calc(12px + var(--dt-bottom-tab-height));
  }
  .fav-card {
    padding: 14px 16px;
  }
  .fav-text {
    font-size: 13px;
  }
}
</style>
