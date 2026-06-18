#!/usr/bin/env bash
# 一键启动脚本 (需已安装 JDK17+ / Maven / Node)
set -e
ROOT="$(cd "$(dirname "$0")" && pwd)"

echo ">>> [1/2] 启动后端 (Spring Boot, 端口 8080)..."
cd "$ROOT/backend"
mvn -q -DskipTests spring-boot:run &
BACKEND_PID=$!

echo ">>> 等待后端就绪..."
for i in $(seq 1 60); do
  if curl -s http://localhost:8080/api/auth/login -o /dev/null 2>/dev/null; then
    echo ">>> 后端已就绪"; break
  fi
  sleep 2
done

echo ">>> [2/2] 启动前端 (Vite, 端口 5173)..."
cd "$ROOT/frontend"
[ -d node_modules ] || npm install
npm run dev

# 退出时关闭后端
trap "kill $BACKEND_PID 2>/dev/null" EXIT
