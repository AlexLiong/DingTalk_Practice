# 仿钉钉企业协作管理系统 —— 环境搭建与运行指南

> 本文档面向**零环境基础**的开发者，详细说明从安装环境到成功运行项目的每一步操作。
> 同时覆盖 **Windows** 与 **macOS** 两大操作系统。
>
> 项目地址：`E:\practice\dingtalk-admin-demo`（Windows）/ 请将项目放置于你本地合适的路径（macOS）

---

## 目录

1. [环境要求总览](#一环境要求总览)
2. [安装 JDK 17+](#二安装-jdk-17)
3. [安装 Maven 3.6+](#三安装-maven-36)
4. [安装 IntelliJ IDEA（可选）](#四安装-intellij-idea可选)
5. [安装 Node.js 18+](#五安装-nodejs-18)
6. [安装 DevEco Studio（鸿蒙开发，可选）](#六安装-deveco-studio鸿蒙开发可选)
7. [后端配置与运行](#七后端配置与运行)
8. [前端配置与运行](#八前端配置与运行)
9. [鸿蒙端配置与运行](#九鸿蒙端配置与运行)
10. [验证与测试](#十验证与测试)
11. [常见问题排查](#十一常见问题排查)

---

## 一、环境要求总览

| 组件 | 最低版本 | 推荐版本 | 用途 | 是否必须 |
|------|---------|---------|------|---------|
| JDK | 17 | 17 / 21 LTS | 后端编译与运行 | ✅ 必须 |
| Maven | 3.6 | 3.9+ | 后端依赖管理与构建 | ✅ 必须 |
| Node.js | 18 | 20 / 22 LTS | 前端依赖安装与开发服务器 | ✅ 必须 |
| npm | 9+ | 随 Node.js 自带 | 前端包管理器 | ✅ 必须 |
| IntelliJ IDEA | 2023+ | 2024+ | IDE（也可用 VS Code） | 推荐 |
| DevEco Studio | 5.0+ | 6.x | 鸿蒙 App 开发与调试 | ❌ 可选 |

> **提示**：如果你只想运行前后端 Web 版本，只需安装 JDK + Maven + Node.js 即可，鸿蒙部分可以跳过。

### 系统差异速览

| 操作 | Windows | macOS |
|------|---------|-------|
| 包管理器 | 手动下载 / winget / choco | **Homebrew**（推荐） |
| Shell | PowerShell / CMD | Terminal（Bash / Zsh） |
| 环境变量 | 系统属性 → 环境变量 | `~/.zshrc` 或 `~/.bash_profile` |
| 路径分隔符 | `\` | `/` |
| JDK 默认路径 | `C:\Program Files\...` | `/Library/Java/...` 或 `/opt/homebrew/...` |

---

## 二、安装 JDK 17+

### Windows

#### 2.1 下载

推荐使用 Adoptium (Eclipse Temurin) 开源版本：

- 官网：https://adoptium.net/download/
- 选择 **JDK 17** (LTS)，操作系统选 **Windows x64**，下载 `.msi` 安装包

> 也可以使用 Oracle JDK 17：https://www.oracle.com/java/technologies/downloads/#java17

#### 2.2 安装

1. 双击 `.msi` 安装包，按默认选项完成安装
2. **重要**：在安装过程中勾选 **"Set JAVA_HOME variable"** 选项
3. 记录安装路径，通常为：`C:\Program Files\Eclipse Adoptium\jdk-17.0.x.x-hotspot\`

#### 2.3 验证

打开 **PowerShell**，输入：

```powershell
java -version
echo $env:JAVA_HOME
```

预期输出：

```
openjdk version "17.0.11" 2024-04-16
OpenJDK Runtime Environment Temurin-17.0.11+9 (build 17.0.11+9)
C:\Program Files\Eclipse Adoptium\jdk-17.0.x.x-hotspot\
```

#### 2.4 手动配置环境变量

如果 `java -version` 失败或 `JAVA_HOME` 为空：

1. 右键「此电脑」→「属性」→「高级系统设置」→「环境变量」
2. **系统变量** → 新建：
   - 变量名：`JAVA_HOME`
   - 变量值：`C:\Program Files\Eclipse Adoptium\jdk-17.0.x.x-hotspot\`（替换为实际路径）
3. 编辑 `Path` 变量，新增一条：`%JAVA_HOME%\bin`
4. 点击确定，**重新打开**命令行验证

---

### macOS

#### 2.1 安装（推荐 Homebrew）

```bash
# 安装 Homebrew（如果尚未安装）
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# 安装 JDK 17
brew install openjdk@17
```

#### 2.2 配置环境变量

```bash
# 编辑 ~/.zshrc（如果使用 Bash 则编辑 ~/.bash_profile）
echo 'export JAVA_HOME=$(/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home)' >> ~/.zshrc
echo 'export PATH="$JAVA_HOME/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
```

> **Intel Mac** 用户：将路径中的 `/opt/homebrew` 替换为 `/usr/local`

#### 2.3 验证

```bash
java -version
echo $JAVA_HOME
```

预期输出：

```
openjdk version "17.0.11" 2024-04-16
OpenJDK Runtime Environment Homebrew (build 17.0.11+0)
/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
```

---

## 三、安装 Maven 3.6+

### Windows

#### 3.1 下载

- 官网：https://maven.apache.org/download.cgi
- 下载 **Binary zip archive**（如 `apache-maven-3.9.x-bin.zip`）

#### 3.2 安装

1. 将 zip 解压到指定目录，例如 `C:\tools\apache-maven-3.9.x`（路径不要包含空格和中文）
2. 配置环境变量：
   - 新建系统变量 `MAVEN_HOME`，值为解压路径（如 `C:\tools\apache-maven-3.9.x`）
   - 编辑 `Path`，新增 `%MAVEN_HOME%\bin`

#### 3.3 验证

```powershell
mvn -version
```

预期输出：

```
Apache Maven 3.9.x
Java version: 17.0.x, vendor: Eclipse Adoptium
```

---

### macOS

#### 3.1 安装（推荐 Homebrew）

```bash
brew install maven
```

#### 3.2 验证

```bash
mvn -version
```

预期输出：

```
Apache Maven 3.9.x
Java version: 17.0.x, vendor: Homebrew
```

> Homebrew 会自动处理 `PATH` 配置，无需手动设置环境变量。

---

### 3.3 配置阿里云镜像（Windows & macOS 通用）

编辑 Maven 配置文件 `~/.m2/settings.xml`（如果不存在则创建），在 `<mirrors>` 标签内添加：

```xml
<mirror>
    <id>aliyunmaven</id>
    <mirrorOf>*</mirrorOf>
    <name>阿里云公共仓库</name>
    <url>https://maven.aliyun.com/repository/public</url>
</mirror>
```

> **Windows**：`~/.m2` 即 `C:\Users\你的用户名\.m2`
> **macOS**：`~/.m2` 即 `/Users/你的用户名/.m2`

---

## 四、安装 IntelliJ IDEA（可选）

> 可以使用任何编辑器（VS Code 等），但 IDEA 对 Spring Boot 项目支持最好。

### Windows & macOS 通用

1. 下载：https://www.jetbrains.com/idea/download/
2. 选择 **Community Edition**（免费）或 **Ultimate**（付费，有学生优惠）
3. **Windows**：双击 `.exe` 安装，勾选「Add "Open Folder as Project"」等选项
4. **macOS**：双击 `.dmg` 拖入 Applications 文件夹

### 配置 Maven（IDEA 内）

启动 IDEA → `Settings`（macOS: `Preferences`）→ `Build, Execution, Deployment` → `Build Tools` → `Maven`：

- **Maven home path**：选择你的 Maven 安装目录
  - Windows：`C:/tools/apache-maven-3.9.x`
  - macOS（Homebrew）：`/opt/homebrew/opt/maven/libexec`

---

## 五、安装 Node.js 18+

### Windows

#### 5.1 下载安装

- 官网：https://nodejs.org/
- 推荐下载 **LTS 版本**（20.x 或 22.x），选择 Windows `.msi` 安装包
- 双击安装，勾选「Automatically install the necessary tools」，保持默认路径

#### 5.2 验证

```powershell
node -v
npm -v
```

---

### macOS

#### 5.1 安装（推荐 Homebrew）

```bash
brew install node@20
```

或使用 nvm（Node 版本管理器，便于切换版本）：

```bash
# 安装 nvm
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.0/install.sh | bash
source ~/.zshrc

# 安装并使用 Node.js 20 LTS
nvm install 20
nvm use 20
```

#### 5.2 验证

```bash
node -v   # v20.x.x
npm -v    # 10.x.x
```

---

### 5.3 配置 npm 镜像（Windows & macOS 通用）

```bash
npm config set registry https://registry.npmmirror.com
```

---

## 六、安装 DevEco Studio（鸿蒙开发，可选）

> ⚠️ 仅在需要编译和调试鸿蒙 App 时才需要安装。Web 端开发不需要。
> ⚠️ DevEco Studio **仅支持 Windows 和 macOS**。

### 6.1 下载

1. 访问华为开发者联盟：https://developer.huawei.com/
2. 注册华为开发者账号并完成实名认证
3. 下载 DevEco Studio：https://developer.huawei.com/consumer/cn/deveco-studio/
4. 根据你的操作系统选择 **Windows** 或 **macOS** 版本

### 6.2 安装

**Windows**：
- 双击安装包，按默认选项安装

**macOS**：
- 双击 `.dmg` 文件，拖入 Applications 文件夹
- 首次打开时如果提示「无法验证开发者」，进入「系统设置 → 隐私与安全性」点击「仍要打开」

### 6.3 首次启动配置

1. 首次启动时，根据向导下载 **HarmonyOS SDK**（含 API 6.x / toolchains / previewer）
2. 登录华为开发者账号

### 6.4 版本兼容说明

| 项目配置项 | 当前值 | 说明 |
|-----------|--------|------|
| `build-profile.json5` 中的 `targetSdkVersion` | `6.1.1(24)` | 对应 HarmonyOS API 6.1 |
| `build-profile.json5` 中的 `compatibleSdkVersion` | `6.1.1(24)` | 最低兼容 API 版本 |
| `hvigor-config.json5` 中的 `modelVersion` | `6.0.0` | Hvigor 构建工具版本 |
| 推荐 DevEco Studio 版本 | **5.0.5+** 或 **6.x** | 需匹配 SDK API 6.1 |

> **常见问题**：如果遇到 `hvigor 版本不匹配` 的报错，请检查你的 DevEco Studio 版本是否支持 modelVersion `6.0.0`。通常 DevEco Studio 5.0.5 及以上版本可以兼容。如果不行，可将 `hvigor-config.json5` 和 `oh-package.json5` 中的 `modelVersion` 降为 `5.0.0`，但建议优先升级 DevEco Studio。

---

## 七、后端配置与运行

### 7.1 项目结构

```
backend/
├── pom.xml                  # Maven 项目配置（依赖、插件）
├── data/                    # H2 数据库文件目录（自动创建）
├── uploads/                 # 文件上传目录（自动创建）
└── src/main/
    ├── java/com/example/dingtalk/
    │   ├── DingtalkAdminApplication.java   # Spring Boot 启动类
    │   ├── common/          # 统一返回结果、异常处理、工具类
    │   ├── config/          # Security/WebSocket/MyBatis/AI 配置 + 种子数据
    │   ├── security/        # JWT 工具与认证过滤器
    │   ├── entity/          # 数据库实体类
    │   ├── mapper/          # MyBatis-Plus Mapper 接口
    │   ├── dto/ vo/         # 数据传输/视图对象
    │   ├── service/         # 业务逻辑层（含 AI Function Calling 工具）
    │   └── controller/      # REST API 控制器
    └── resources/
        ├── application.yml          # 主配置文件
        ├── application-h2.yml       # H2 数据库配置（默认激活）
        ├── application-mysql.yml    # MySQL 数据库配置（可选）
        ├── schema.sql               # H2 建表脚本
        └── schema-mysql.sql         # MySQL 建表脚本
```

### 7.2 数据库说明

项目默认使用 **H2 文件数据库**，无需安装 MySQL。数据文件保存在 `backend/data/dingtalk.mv.db`。

- **优点**：零配置、开箱即用、数据持久化（重启不丢失）
- **种子数据**：首次启动时自动创建表结构并插入演示账号、菜单权限等数据
- **H2 控制台**：启动后可访问 http://localhost:8080/h2-console 查看数据库
  - JDBC URL: `jdbc:h2:file:./data/dingtalk`
  - 用户名: `sa`，密码: （空）

> 如需使用 MySQL，参考 [附录：切换 MySQL](#附录切换-mysql可选)。

### 7.3 AI 配置说明

AI 功能依赖外部大模型 API，配置文件中的相关环境变量：

| 环境变量 | 默认值 | 说明 |
|---------|--------|------|
| `SPRING_AI_OPENAI_API_KEY` | `disabled` | API 密钥（如阿里云百炼的 Key） |
| `SPRING_AI_OPENAI_BASE_URL` | `https://api.openai.com` | API 端点地址 |
| `SPRING_AI_OPENAI_CHAT_MODEL` | `gpt-4o-mini` | 对话模型名称 |
| `SPRING_AI_OPENAI_EMBEDDING_MODEL` | `text-embedding-3-small` | 嵌入模型名称 |
| `APP_AI_ENABLED` | `true` | 是否启用 AI 功能 |

> **阿里云百炼配置示例**（使用通义千问模型）：
>
> **Windows PowerShell**：
> ```powershell
> $env:SPRING_AI_OPENAI_API_KEY="你的百炼API-Key"
> $env:SPRING_AI_OPENAI_BASE_URL="https://dashscope.aliyuncs.com/compatible-mode/v1"
> $env:SPRING_AI_OPENAI_CHAT_MODEL="qwen-plus"
> $env:SPRING_AI_OPENAI_EMBEDDING_MODEL="text-embedding-v3"
> ```
>
> **macOS Terminal**：
> ```bash
> export SPRING_AI_OPENAI_API_KEY="你的百炼API-Key"
> export SPRING_AI_OPENAI_BASE_URL="https://dashscope.aliyuncs.com/compatible-mode/v1"
> export SPRING_AI_OPENAI_CHAT_MODEL="qwen-plus"
> export SPRING_AI_OPENAI_EMBEDDING_MODEL="text-embedding-v3"
> ```
>
> 或者直接在 `application.yml` 中修改对应的配置值（不推荐提交到 Git）。
>
> 即使不配置 AI，后端仍可正常启动，AI 聊天功能会返回提示信息。

### 7.4 启动后端

> 以下命令假设你已 `cd` 到项目 `backend` 目录。
> **Windows**：`cd E:\practice\dingtalk-admin-demo\backend`
> **macOS**：`cd /你的路径/dingtalk-admin-demo/backend`

#### 方式一：命令行启动（推荐）

```bash
mvn spring-boot:run
```

首次运行会下载大量 Maven 依赖（约 3-10 分钟），请耐心等待。

启动成功后会看到：

```
Started DingtalkAdminApplication in x.xxx seconds
```

后端运行在 **http://localhost:8080**。

#### 方式二：IDEA 启动（Windows & macOS 通用）

1. 用 IDEA 打开 `backend` 目录（或整个项目根目录）
2. 等待 Maven 依赖自动下载完成
3. 找到 `DingtalkAdminApplication.java`
4. 右键 → Run 'DingtalkAdminApplication'

#### 方式三：打包运行

```bash
mvn clean package -DskipTests
java -jar target/dingtalk-admin.jar
```

### 7.5 重置数据库

**Windows**：
```powershell
# 停止后端服务
# 删除 H2 数据库文件
Remove-Item -Force backend\data\dingtalk.mv.db -ErrorAction SilentlyContinue
# 重新启动后端，种子数据会自动重新插入
```

**macOS**：
```bash
# 停止后端服务
# 删除 H2 数据库文件
rm -f backend/data/dingtalk.mv.db
# 重新启动后端，种子数据会自动重新插入
```

---

## 八、前端配置与运行

### 8.1 项目结构

```
frontend/
├── package.json             # 项目依赖与脚本
├── vite.config.js           # Vite 构建配置（代理 /api → 8080）
├── index.html               # 入口 HTML
└── src/
    ├── main.js              # Vue 应用入口
    ├── App.vue              # 根组件
    ├── api/                 # Axios 封装 + 各模块 API 接口
    ├── store/               # Pinia 状态管理
    ├── router/              # Vue Router + 登录守卫
    ├── utils/ws.js          # STOMP WebSocket 客户端
    ├── layout/              # 布局组件
    ├── components/          # 公共组件
    ├── assets/              # 静态资源
    └── views/               # 页面视图
        ├── Login.vue         # 登录/注册
        ├── Workbench.vue     # 钉钉三栏主界面
        ├── Documents.vue     # 文件管理
        ├── Mailbox.vue       # 企业邮箱
        ├── Ding.vue          # DING消息
        ├── Todo.vue          # 待办任务
        ├── Approval.vue      # 审批中心
        ├── Calendar.vue      # 日程管理
        ├── Notice.vue        # 通知公告
        ├── Profile.vue       # 个人中心
        ├── Favorites.vue     # 消息收藏
        └── admin/            # 管理后台
            ├── UserAdmin.vue       # 用户管理
            ├── RoleAdmin.vue       # 角色管理
            ├── MenuAdmin.vue       # 菜单管理
            ├── DeptAdmin.vue       # 部门管理
            ├── Dashboard.vue       # 数据看板
            └── NoticeManage.vue    # 公告管理
```

### 8.2 安装依赖

> 以下命令假设你已 `cd` 到项目 `frontend` 目录。

```bash
npm install
```

> 首次运行会下载约 200MB 的 node_modules，约需 2-5 分钟。

### 8.3 前端代理说明

`vite.config.js` 中配置了代理规则：

- `/api` 开头的请求 → 转发到 `http://localhost:8080`
- `/ws` 开头的请求 → 转发到 `http://localhost:8080`（含 WebSocket 升级）

因此前端开发时无需处理跨域问题，**但必须确保后端已在 8080 端口运行**。

### 8.4 启动前端

```bash
npm run dev
```

启动成功后会显示：

```
VITE v5.x.x  ready in xxx ms
➜  Local:   http://localhost:5173/
➜  Network: http://192.168.x.x:5173/
```

浏览器访问 **http://localhost:5173** 即可。

### 8.5 构建生产版本

```bash
npm run build
```

构建产物在 `frontend/dist/` 目录，可直接部署到 Nginx 等 Web 服务器。

---

## 九、鸿蒙端配置与运行

> ⚠️ 此部分为可选内容，仅在需要编译调试鸿蒙 App 时需要。

### 9.1 项目结构

```
harmony/
├── build-profile.json5      # 项目构建配置（SDK版本等）
├── hvigor-config.json5      # Hvigor 构建工具配置
├── hvigorfile.ts            # Hvigor 构建入口
├── oh-package.json5         # 项目依赖配置
├── AppScope/                # 应用全局配置（图标、名称等）
└── entry/                   # 主模块
    └── src/main/
        ├── module.json5     # 模块配置（权限、Ability 等）
        ├── ets/
        │   ├── entryability/EntryAbility.ets    # 应用入口
        │   └── pages/Index.ets                  # 主页面（WebView）
        └── resources/       # 资源文件
```

### 9.2 用 DevEco Studio 打开

1. 启动 DevEco Studio
2. 点击 **Open** → 选择 `harmony` 目录
   - Windows：`E:\practice\dingtalk-admin-demo\harmony`
   - macOS：`/你的路径/dingtalk-admin-demo/harmony`
3. 等待 SDK 索引和依赖下载完成

### 9.3 修改后端地址

鸿蒙端通过 WebView 加载前端页面，需要将 URL 指向你的服务地址。

编辑 `harmony/entry/src/main/ets/pages/Index.ets`，找到 WebView 的 URL 配置：

```typescript
// 找到类似以下代码，将 localhost 改为实际地址
Web({ src: 'http://localhost:5173', controller: this.webviewController })
```

如果鸿蒙模拟器/真机与后端在同一局域网，改为局域网 IP：

```typescript
Web({ src: 'http://192.168.x.x:5173', controller: this.webviewController })
```

> **提示**：macOS 上可通过 `ifconfig | grep inet` 查看本机局域网 IP。

### 9.4 运行

1. 在 DevEco Studio 顶部工具栏选择设备（模拟器或真机）
2. 点击 **Run** 按钮（绿色三角形）
3. 首次运行会编译并部署到设备

> **注意**：鸿蒙模拟器的运行对硬件有一定要求，macOS Apple Silicon（M 系列芯片）支持较好。

### 9.5 版本兼容性修复

如果遇到 `hvigor 版本不匹配` 错误：

1. 确认 DevEco Studio 版本 ≥ 5.0.5
2. 检查 `harmony/hvigor-config.json5` 中 `modelVersion` 是否为 `"6.0.0"`
3. 检查 `harmony/oh-package.json5` 中 `modelVersion` 是否为 `"6.0.0"`
4. 如果 DevEco Studio 版本较旧，尝试将两个文件中的 `modelVersion` 改为 `"5.0.0"`

---

## 十、验证与测试

### 10.1 验证后端

浏览器访问 http://localhost:8080/h2-console 确认 H2 控制台可打开。

或用命令行快速测试（Windows & macOS 通用）：

```bash
curl -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d "{\"username\":\"admin\",\"password\":\"Boz@2026\"}"
```

> **Windows 注意**：如果 `curl` 不可用，用浏览器直接访问 http://localhost:8080/api/auth/login 查看是否有响应；或者安装 curl（`winget install curl.curl`）。

### 10.2 验证前端

1. 打开 http://localhost:5173
2. 使用演示账号登录：

| 账号 | 密码 | 角色 |
|------|------|------|
| `admin` | `Boz@2026` | 超级管理员（可见管理后台） |
| `chenyuqi` | `Boz@2026` | 普通员工 |
| `liuyulin` | `Boz@2026` | 普通员工 |
| `zhouleheng` | `Boz@2026` | 普通员工 |
| `liangqinwei` | `Boz@2026` | 普通员工 |
| `jiangzezhi` | `Boz@2026` | 普通员工 |

### 10.3 测试即时通讯

1. 浏览器 A 用 `admin` 登录
2. 浏览器 B（或无痕窗口）用 `chenyuqi` 登录
3. 在任一端发消息，另一端应实时收到（WebSocket 推送）

### 10.4 测试 AI 助手

1. 确认已配置 AI 相关环境变量（见 7.3 节）
2. 登录后在左侧导航点击「AI助手」或在消息列表中找到 AI 助手会话
3. 发送消息，应收到 SSE 流式回复

---

## 十一、常见问题排查

### Q1：`mvn` 命令找不到

| 系统 | 解决 |
|------|------|
| **Windows** | 检查 `MAVEN_HOME` 环境变量是否指向 Maven 安装目录；检查 `Path` 中是否包含 `%MAVEN_HOME%\bin`；**重启命令行**后重试 |
| **macOS** | 如果用 Homebrew 安装则自动配置；手动安装需在 `~/.zshrc` 中添加 `export PATH="/path/to/maven/bin:$PATH"` |

### Q2：Maven 依赖下载失败或卡住

**原因**：网络问题或 Maven 中央仓库访问慢。

**解决**（Windows & macOS 通用）：
- 配置阿里云镜像（见 3.3 节）
- 删除失败缓存后重试：

**Windows**：
```powershell
Remove-Item -Recurse -Force $env:USERPROFILE\.m2\repository\org\springframework -ErrorAction SilentlyContinue
```

**macOS**：
```bash
rm -rf ~/.m2/repository/org/springframework
```

- 使用 IDE（IDEA）下载依赖通常更稳定

### Q3：`npm install` 失败

**原因**：网络问题或 Node.js 版本不兼容。

**解决**（Windows & macOS 通用）：
- 配置 npm 镜像（见 5.3 节）
- 确认 Node.js 版本 ≥ 18：`node -v`
- 清除缓存后重试：
  ```bash
  npm cache clean --force
  rm -rf node_modules        # macOS
  # 或 Windows:
  # Remove-Item -Recurse -Force node_modules
  npm install
  ```

### Q4：前端代理报错 502 / ECONNREFUSED

**原因**：后端未启动或端口被占用。

**解决**：
- 确认后端已在 8080 端口运行

**Windows**：
```powershell
netstat -ano | findstr :8080
```

**macOS**：
```bash
lsof -i :8080
```

### Q5：前端页面空白 / 路由不跳转

**原因**：可能是浏览器缓存或 Vite 热更新异常。

**解决**（通用）：
- 浏览器硬刷新：`Ctrl + Shift + R`（Windows）/ `Cmd + Shift + R`（macOS）
- 清除浏览器缓存
- 重启前端开发服务器

### Q6：后端启动报 `java: 非法字符: '\ufeff'`

**原因**：Java 文件编码为 UTF-8 with BOM，JDK 编译器不识别。

**解决**（通用）：
- **IDEA**：右键文件 → File Encoding → 选择 UTF-8（不带 BOM）
- **VS Code**：右下角点击编码 → Save with Encoding → UTF-8

### Q7：WebSocket 连接失败

**原因**：WebSocket 握手认证失败或代理未正确转发。

**解决**（通用）：
- 确认已登录（Token 存储在 localStorage）
- 检查 `vite.config.js` 中 `/ws` 代理配置
- 浏览器控制台查看 WebSocket 错误详情

### Q8：AI 聊天无响应 / 返回空

**原因**：AI API 未配置或 Key 无效。

**解决**（通用）：
- 检查环境变量 `SPRING_AI_OPENAI_API_KEY` 是否设置正确
- 可在 `application.yml` 中直接修改进行测试
- 测试 API 连通性：
  ```bash
  curl -X POST "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions" \
    -H "Authorization: Bearer 你的Key" \
    -H "Content-Type: application/json" \
    -d '{"model":"qwen-plus","messages":[{"role":"user","content":"hello"}]}'
  ```

### Q9：H2 数据库文件损坏

**原因**：后端异常关闭可能导致数据文件损坏。

**解决**：删除数据库文件后重启（见 7.5 节）。

### Q10：端口 8080 或 5173 被占用

**解决**：
- 修改后端端口：编辑 `application.yml`，将 `server.port` 改为其他端口，同时更新 `vite.config.js` 中的代理目标端口
- 修改前端端口：编辑 `vite.config.js`，将 `server.port` 改为其他端口

**Windows** — 结束占用进程：
```powershell
netstat -ano | findstr :8080
taskkill /PID xxxx /F      # 替换 xxxx 为实际 PID
```

**macOS** — 结束占用进程：
```bash
lsof -ti :8080 | xargs kill -9
```

### Q11：macOS 提示「"xxx" 已损坏，无法打开」

**原因**：macOS Gatekeeper 安全限制。

**解决**：
```bash
# 允许从任何来源打开（适用于 DevEco Studio 等）
sudo spctl --master-disable
# 或者仅允许特定应用
xattr -cr /Applications/DevEco\ Studio.app
```

### Q12：macOS 前端启动时提示 `Error: EMFILE: too many open files`

**原因**：macOS 默认文件描述符限制较低。

**解决**：
```bash
# 临时提高限制
ulimit -n 65536
# 或在 ~/.zshrc 中永久设置
echo 'ulimit -n 65536' >> ~/.zshrc
```

---

## 附录 A：快速启动脚本

### Windows（PowerShell）

在项目根目录新建 `start.ps1`：

```powershell
# 启动后端
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd backend; mvn spring-boot:run"
# 等待后端就绪
Start-Sleep -Seconds 15
# 启动前端
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd frontend; npm install; npm run dev"
Write-Host "后端 http://localhost:8080"
Write-Host "前端 http://localhost:5173"
```

### macOS / Linux（Bash）

项目已自带 `start.sh`：

```bash
chmod +x start.sh
./start.sh
```

---

## 附录 B：切换 MySQL（可选）

### B.1 安装 MySQL

**Windows**：
- 下载：https://dev.mysql.com/downloads/mysql/
- 安装时设置 root 密码

**macOS**：
```bash
brew install mysql
brew services start mysql
mysql_secure_installation  # 设置 root 密码
```

### B.2 创建数据库

```sql
CREATE DATABASE dingtalk CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### B.3 修改配置

1. 编辑 `backend/src/main/resources/application.yml`：
   ```yaml
   spring:
     profiles:
       active: mysql   # 从 h2 改为 mysql
   ```

2. 编辑 `backend/src/main/resources/application-mysql.yml`，填入 MySQL 连接信息

3. 执行建表脚本：

**Windows**：
```powershell
Get-Content backend\src\main\resources\schema-mysql.sql | mysql -u root -p dingtalk
```

**macOS**：
```bash
mysql -u root -p dingtalk < backend/src/main/resources/schema-mysql.sql
```

### B.4 重启后端

```bash
cd backend
mvn spring-boot:run
```

---

> **最后更新：2026-07-02**
> 如有问题，请查看 `document/` 目录下的模块设计文档或 `AGENTS.md`。