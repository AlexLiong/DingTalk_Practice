<template>
  <div class="approval-shell">
    <AppSideNav active-key="work" />

    <div class="approval-page">
      <div class="approval-top">
        <div class="approval-top-inner">
          <div class="page-title-cluster">
            <el-button
              class="page-back-btn"
              :icon="Back"
              @click="router.push('/chat')"
              >返回消息</el-button
            >
            <div class="page-title-block">
              <div class="page-section-label">协同应用</div>
              <div class="page-heading-row">
                <span class="ptitle">审批中心</span>
              </div>
            </div>
          </div>
          <div class="page-top-actions">
            <el-button type="primary" :icon="Plus" @click="openApply"
              >发起审批</el-button
            >
          </div>
        </div>
      </div>

      <div class="approval-body">
        <el-tabs
          v-model="activeTab"
          @tab-change="onTabChange"
          class="approval-tabs"
        >
          <el-tab-pane label="我发起的" name="applied" />
          <el-tab-pane label="待我审批" name="pending" />
        </el-tabs>

        <div v-loading="loading" class="card-list">
          <el-empty v-if="!list.length && !loading" description="暂无记录" />
          <div v-for="item in list" :key="item.id" class="approval-card">
            <div class="card-header">
              <span class="type-badge">{{
                item.type === "leave" ? "🏖 请假" : "💰 报销"
              }}</span>
              <span class="card-title">{{ item.title }}</span>
              <el-tag
                size="small"
                :type="statusType(item.status)"
                class="status-tag"
              >
                {{ statusLabel(item.status) }}
              </el-tag>
            </div>
            <div class="card-meta">
              <span>申请人: {{ item.applicantName || "-" }}</span>
              <span>审批人: {{ item.approverName || "-" }}</span>
              <span>{{ item.createTime }}</span>
            </div>
            <div v-if="item.content" class="card-content">
              <table
                cellpadding="6"
                cellspacing="0"
                style="border-collapse: collapse"
              >
                <tr>
                  <th width="120">说明</th>
                  <th>内容</th>
                </tr>
                <tr v-for="(val, key) in parseJson(item.content)" :key="key">
                  <td>{{ key }}</td>
                  <td>{{ val }}</td>
                </tr>
              </table>
            </div>
            <div v-if="item.remark" class="card-remark">
              审批备注: {{ item.remark }}
            </div>

            <!-- 待审批操作 -->
            <div
              v-if="activeTab === 'pending' && item.status === 0"
              class="card-actions"
            >
              <el-button
                type="success"
                size="small"
                @click="handleApprove(item, 1)"
                >通过</el-button
              >
              <el-button
                type="danger"
                size="small"
                @click="handleApprove(item, 2)"
                >驳回</el-button
              >
            </div>
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="applyDialog" title="发起审批" width="520px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="类型">
          <el-select v-model="form.type" style="width: 100%">
            <el-option label="请假" value="leave" />
            <el-option label="报销" value="expense" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="审批标题" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="3"
            placeholder="详细说明"
          />
        </el-form-item>
        <el-form-item label="审批人">
          <el-select
            v-model="form.approverId"
            filterable
            placeholder="选择审批人"
            style="width: 100%"
          >
            <el-option
              v-for="u in users"
              :key="u.id"
              :label="u.nickname || u.username"
              :value="u.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyDialog = false">取消</el-button>
        <el-button type="primary" @click="submitApply">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, watch } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { Back, Plus } from "@element-plus/icons-vue";
import AppSideNav from "../components/AppSideNav.vue";
import {
  apiApprovalPending,
  apiApprovalApplied,
  apiApprovalApply,
  apiApprovalApprove,
  apiUserList,
} from "../api";
import { useUserPreferenceStore } from "../store/userPreference";

const router = useRouter();
const preferenceStore = useUserPreferenceStore();
const loading = ref(false);
const list = ref([]);
const users = ref([]);
const activeTab = ref("applied");
const approvalStateReady = ref(false);
const applyDialog = ref(false);
const form = reactive({
  type: "leave",
  title: "",
  content: "",
  approverId: null,
});

watch(
  () => preferenceStore.loaded,
  (loaded) => {
    if (!loaded || approvalStateReady.value) return;
    const state = preferenceStore.getPageState("approval");
    if (typeof state.activeTab === "string") activeTab.value = state.activeTab;
    approvalStateReady.value = true;
  },
  { immediate: true },
);

watch(activeTab, () => {
  if (!approvalStateReady.value || !preferenceStore.loaded) return;
  preferenceStore.setPageState("approval", { activeTab: activeTab.value });
});

onMounted(async () => {
  users.value = await apiUserList();
  await load();
});
// 解析JSON，捕获报错防止页面崩溃
const parseJson = (str) => {
  try {
    return JSON.parse(str);
  } catch (e) {
    return {};
  }
};

// 判断是否为合法JSON字符串
const isJsonStr = (str) => {
  if (typeof str !== "string") return false;
  try {
    JSON.parse(str);
    return true;
  } catch {
    return false;
  }
};
async function load() {
  loading.value = true;
  try {
    let data;
    if (activeTab.value === "pending") {
      data = await apiApprovalPending();
    } else {
      data = await apiApprovalApplied();
    }
    list.value = Array.isArray(data) ? data : data.records || data.list || [];
  } finally {
    loading.value = false;
  }
}

function onTabChange() {
  load();
}

function openApply() {
  Object.assign(form, {
    type: "leave",
    title: "",
    content: "",
    approverId: null,
  });
  applyDialog.value = true;
}

async function submitApply() {
  if (!form.title) {
    ElMessage.warning("请输入标题");
    return;
  }
  if (!form.approverId) {
    ElMessage.warning("请选择审批人");
    return;
  }
  await apiApprovalApply({ ...form });
  ElMessage.success("审批已提交");
  applyDialog.value = false;
  await load();
}

async function handleApprove(item, status) {
  const action = status === 1 ? "通过" : "驳回";
  const { value: remark } = await ElMessageBox.prompt(
    `请输入${action}备注（可选）`,
    `确认${action}`,
    {
      inputPlaceholder: "备注",
      inputValue: "",
      confirmButtonText: action,
      cancelButtonText: "取消",
      type: status === 1 ? "success" : "warning",
      inputRequired: false,
      beforeClose: (act, instance, done) => {
        done();
      },
    },
  ).catch(() => ({ value: null }));

  if (remark === null) return;

  await apiApprovalApprove(item.id, { status, remark: remark || "" });
  ElMessage.success(`已${action}`);
  await load();
}

function statusLabel(s) {
  return { 0: "待审批", 1: "已通过", 2: "已驳回" }[s] ?? "待审批";
}
function statusType(s) {
  return { 0: "warning", 1: "success", 2: "danger" }[s] ?? "info";
}
</script>

<style scoped>
.approval-shell {
  height: 100%;
  display: flex;
  background: var(--dt-bg);
}
.approval-page {
  flex: 1;
  min-width: 0;
  height: 100%;
  background: #f3f5f8;
  display: flex;
  flex-direction: column;
}
.approval-top {
  padding: 16px 24px 14px;
  background: linear-gradient(
    180deg,
    rgba(255, 255, 255, 0.96) 0%,
    rgba(247, 249, 252, 0.92) 100%
  );
  border-bottom: 1px solid rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(18px);
  flex-shrink: 0;
}
.approval-top-inner {
  width: 100%;
  max-width: 1260px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}
.page-title-cluster {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 14px;
}
.page-title-block {
  min-width: 0;
}
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
.page-top-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}
.ptitle {
  font-size: 20px;
  line-height: 1.15;
  font-weight: 700;
  color: #1d2129;
}
.approval-body {
  flex: 1;
  overflow-y: auto;
  padding: 18px 24px 24px;
  max-width: 860px;
  margin: 0 auto;
  width: 100%;
  box-sizing: border-box;
}
.approval-tabs {
  background: #fff;
  border-radius: 10px;
  padding: 4px 16px 0;
  margin-bottom: 12px;
}
.card-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.approval-card {
  background: #fff;
  border-radius: 10px;
  padding: 16px 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}
.card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.type-badge {
  font-size: 14px;
  padding: 2px 8px;
  background: #f5f5f5;
  border-radius: 4px;
}
.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #1d2129;
}
.status-tag {
  margin-left: auto;
}
.card-meta {
  display: flex;
  gap: 20px;
  margin-top: 10px;
  font-size: 13px;
  color: #8c8c8c;
  flex-wrap: wrap;
}
.card-content {
  margin-top: 10px;
  font-size: 14px;
  color: #555;
  line-height: 1.7;
  white-space: pre-wrap;
  padding: 10px 12px;
  background: #fafafa;
  border-radius: 6px;
}
.card-remark {
  margin-top: 8px;
  font-size: 13px;
  color: #fa8c16;
}
.card-actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
  justify-content: flex-end;
}
@media (max-width: 960px) {
  .approval-top {
    padding: 14px 16px 12px;
  }
  .approval-top-inner {
    align-items: flex-start;
    flex-direction: column;
  }
  .page-title-cluster,
  .page-top-actions {
    width: 100%;
  }
  .page-top-actions {
    flex-wrap: wrap;
  }
  .approval-body {
    padding: 16px;
  }
}
@media (max-width: 768px) {
  .approval-top {
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
  .approval-body {
    padding: 10px 12px;
    padding-bottom: calc(12px + var(--dt-bottom-tab-height));
  }
  .approval-card {
    padding: 12px 14px;
  }
  .card-meta {
    flex-wrap: wrap;
    gap: 8px;
    font-size: 12px;
  }
}
</style>
