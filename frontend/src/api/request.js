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
        'Accept': 'text/event-stream',
        'Cache-Control': 'no-cache'
      },
      body: JSON.stringify(params)
    })
        .then(async response => {
          let isClosed = false;
          // 缓存跨分片残留半行
          let leftover = '';

          if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
          }

          const reader = response.body.getReader();
          const decoder = new TextDecoder();

          const eventSource = {
            onmessage: null,
            onerror: null,
            onclose: null,
            close: () => {
              if (isClosed) return;
              console.log("执行关闭reader");
              reader.cancel();
              isClosed = true;
              if (eventSource.onclose) eventSource.onclose();
            }
          };

          const readStream = () => {
            if (isClosed) return;
            reader.read().then(({ done, value }) => {
              if (done) {
                if (!isClosed && eventSource.onclose) eventSource.onclose();
                isClosed = true;
                return;
              }

              // 拼接残留 + 当前分片
              const chunk = leftover + decoder.decode(value, { stream: true });
              const rawLines = chunk.split('\n');
              leftover = rawLines.pop() || '';

              rawLines.forEach(line => {
                const trimLine = line.trim();
                // 过滤空行、仅data:的无效行
                if (!trimLine || trimLine === 'data:') return;

                // 标准data:xxx 格式行
                if (trimLine.startsWith('data:')) {
                  try {
                    const jsonStr = trimLine.substring(5).trim();
                    const data = JSON.parse(jsonStr);
                    // 命中结束标记，关闭连接
                    if (data.content === '[DONE]') {
                      console.log("DONE 连接关闭");
                      eventSource.close();
                      return;
                    }
                    if (eventSource.onmessage) {
                      eventSource.onmessage({ data });
                    }
                  } catch (e) {
                    console.error('解析SSE行失败', e, trimLine);
                  }
                }
              });

              readStream();
            }).catch(error => {
              if (isClosed) return;
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
