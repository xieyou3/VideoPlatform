# V-Stream 视频平台 - 后端功能与 API 文档

本项目前端已使用 Vue 3 + Tailwind CSS 实现。本文件详细描述了后端需要实现的功能以及与前端交互的 API 规范。

## 1. 后端核心功能需求

### 1.1 用户系统 (Authentication)
*   **注册**: 支持邮箱、用户名、密码注册。
*   **登录**: 基于 JWT (JSON Web Token) 的身份验证。
*   **个人资料**: 支持头像、昵称、简介的修改。

### 1.2 视频系统 (Video Management)
*   **视频上传**: 处理大文件上传，生成不同分辨率的流媒体文件。
*   **封面提取**: 自动从视频中提取或允许用户上传封面图。
*   **视频流服务**: 支持 HLS 或 DASH 协议的视频分发。
*   **搜索与推荐**: 基于标题、标签的全文搜索，以及基于用户行为的简单推荐算法。

### 1.3 互动系统 (Interactions)
*   **弹幕系统**: 实时推送弹幕（建议使用 WebSocket 或 Server-Sent Events）。
*   **评论系统**: 支持多级评论（评论与回复）。
*   **点赞/收藏**: 记录用户对视频的交互状态。

### 1.4 社交系统 (Social & Messaging)
*   **好友关系**: 好友申请、同意/拒绝、好友列表。
*   **私信**: 实时一对一聊天。
*   **群聊**: 创建群组、邀请成员、群消息推送。
*   **实时通知**: 好友申请、新消息、视频被点赞/评论的通知。

---

## 2. API 接口文档

**基础路径**: `https://api.v-stream.com/v1`
**内容类型**: `application/json`
**认证方式**: `Authorization: Bearer <JWT_TOKEN>`

### 2.1 用户认证 (Auth)

#### 注册
*   **方式**: `POST`
*   **路径**: `/auth/register`
*   **参数**:
    ```json
    {
      "username": "string",
      "email": "string",
      "password": "string(min:8)"
    }
    ```
*   **返回**: `201 Created`
    ```json
    {
      "token": "jwt_token_string",
      "user": { "id": "uuid", "username": "string", "email": "string" }
    }
    ```

#### 登录
*   **方式**: `POST`
*   **路径**: `/auth/login`
*   **参数**:
    ```json
    {
      "email": "string",
      "password": "string"
    }
    ```
*   **返回**: `200 OK` (同注册返回格式)

---

### 2.2 视频接口 (Videos)

#### 获取视频列表
*   **方式**: `GET`
*   **路径**: `/videos`
*   **参数**: `page` (int), `limit` (int), `category` (string), `search` (string)
*   **返回**:
    ```json
    {
      "items": [
        {
          "id": "uuid",
          "title": "string",
          "thumbnail": "url",
          "author": { "id": "uuid", "name": "string" },
          "views": 1200,
          "duration": "05:30",
          "createdAt": "iso_date"
        }
      ],
      "total": 100
    }
    ```

#### 上传视频
*   **方式**: `POST`
*   **路径**: `/videos/upload`
*   **参数**: `multipart/form-data` (file: video, file: thumbnail, title: string, description: string, tags: string[])
*   **返回**: `201 Created` (返回视频对象)

#### 收藏视频
*   **方式**: `POST`
*   **路径**: `/videos/:id/favorite`
*   **返回**: `200 OK` (`{ "isFavorited": true }`)

---

### 2.3 互动接口 (Interactions)

#### 发送弹幕
*   **方式**: `POST`
*   **路径**: `/videos/:id/danmaku`
*   **参数**:
    ```json
    {
      "text": "string",
      "time": 12.5, // 视频播放时间点（秒）
      "color": "hex_code"
    }
    ```
*   **返回**: `201 Created`

#### 获取弹幕
*   **方式**: `GET`
*   **路径**: `/videos/:id/danmaku`
*   **返回**: `Danmaku[]`

---

### 2.4 社交与消息接口 (Social)

#### 获取好友列表
*   **方式**: `GET`
*   **路径**: `/social/friends`
*   **返回**: `User[]`

#### 发送私信 (建议使用 WebSocket)
*   **方式**: `POST`
*   **路径**: `/social/messages/send`
*   **参数**:
    ```json
    {
      "receiverId": "uuid",
      "text": "string",
      "chatId": "uuid" // 如果是已有会话
    }
    ```
*   **返回**: `201 Created` (返回消息对象)

#### 创建群聊
*   **方式**: `POST`
*   **路径**: `/social/groups/create`
*   **参数**:
    ```json
    {
      "name": "string",
      "members": ["uuid1", "uuid2"]
    }
    ```
*   **返回**: `201 Created` (返回群组对象)

---

## 3. 推荐后端技术栈
*   **语言**: Node.js (Express/NestJS) 或 Go (Gin)
*   **数据库**: PostgreSQL (用户、视频、社交关系) + Redis (弹幕缓存、实时消息)
*   **文件存储**: AWS S3 或 阿里云 OSS
*   **实时通讯**: Socket.io 或 原生 WebSocket
*   **视频处理**: FFmpeg (转码、切片、封面提取)
