import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const service = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截: 注入 token
service.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// 响应拦截: 统一处理 code
service.interceptors.response.use(
  (res) => {
    const { code, message, data } = res.data
    if (code === 200) return data
    ElMessage.error(message || '请求失败')
    return Promise.reject(new Error(message))
  },
  (err) => {
    const status = err.response?.status
    if (status === 401 || status === 403) {
      ElMessage.error('登录已过期, 请重新登录')
      localStorage.removeItem('token')
      router.push('/login')
    } else {
      ElMessage.error(err.message || '网络错误')
    }
    return Promise.reject(err)
  }
)

export default service

// ===== SSE 流式 API =====
/**
 * 创建 SSE 流式连接的 API 函数
 * 使用 fetch API 而不是 axios，避免拦截器干扰
 */
export const apiAiReplyStream = (params) => {
  return new Promise((resolve, reject) => {
    fetch('/api/chat/ai-reply', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${localStorage.getItem('token')}`,
        'Accept': 'text/event-stream',              // SSE 专用头
        'Cache-Control': 'no-cache'                 // 防止缓存
      },
      body: JSON.stringify(params)
    })
    .then(async response => {
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const reader = response.body.getReader();
      const decoder = new TextDecoder();

      // 包装成 EventSource 接口
      const eventSource = {
        onmessage: null,
        onerror: null,
        onclose: null,
        close: () => reader.cancel()
      };

      const readStream = () => {
        reader.read().then(({ done, value }) => {
          if (done) {
            if (eventSource.onclose) eventSource.onclose();
            return;
          }

          const chunk = decoder.decode(value, { stream: true });
          const lines = chunk.split('\n');
          lines.forEach(line => {
            if (line.startsWith('data: ')) {
              try {
                const jsonStr = line.substring(6).trim();
                if (jsonStr === '[DONE]') return;
                const data = JSON.parse(jsonStr);
                if (eventSource.onmessage) {
                  eventSource.onmessage({ data });
                }
              } catch (e) {
                console.error('Failed to parse SSE data:', e);
              }
            }
          });

          readStream();
        }).catch(error => {
          if (eventSource.onerror) eventSource.onerror(error);
          reject(error);
        });
      };

      readStream();
      resolve(eventSource);
    })
    .catch(error => {
      if (error.message.includes('Failed to fetch')) {
        console.error('SSE connection failed:', error);
      }
      reject(error);
    });
  });
}

