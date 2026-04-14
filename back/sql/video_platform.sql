CREATE DATABASE IF NOT EXISTS video_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE video_platform;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL,
    email VARCHAR(128) NOT NULL,
    phone VARCHAR(32) DEFAULT NULL,
    password_hash VARCHAR(255) NOT NULL,
    avatar_url VARCHAR(255) DEFAULT NULL,
    bio VARCHAR(512) DEFAULT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    fans_count BIGINT NOT NULL DEFAULT 0,
    following_count BIGINT NOT NULL DEFAULT 0,
    liked_count BIGINT NOT NULL DEFAULT 0,
    last_login_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_users_email (email),
    UNIQUE KEY uk_users_phone (phone),
    UNIQUE KEY uk_users_username (username)
);

CREATE TABLE IF NOT EXISTS user_refresh_token (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    refresh_token VARCHAR(255) NOT NULL,
    expire_at DATETIME NOT NULL,
    revoked TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_refresh_token (refresh_token),
    KEY idx_refresh_user (user_id)
);

CREATE TABLE IF NOT EXISTS user_follow (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    follow_user_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'FOLLOWING',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_follow_pair (user_id, follow_user_id),
    KEY idx_follow_target (follow_user_id)
);

CREATE TABLE IF NOT EXISTS friend_relation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    friend_user_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACCEPTED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_friend_pair (user_id, friend_user_id)
);

DROP TABLE IF EXISTS video_entity;

CREATE TABLE IF NOT EXISTS video_entity (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    file_hash VARCHAR(512) NOT NULL,
    video_url VARCHAR(512) NOT NULL,
    duration_seconds INT NOT NULL DEFAULT 0,
    file_size BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_file_hash (file_hash)
);

DROP TABLE IF EXISTS video;

CREATE TABLE IF NOT EXISTS video (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    author_id BIGINT NOT NULL,
    file_hash VARCHAR(512) NOT NULL,
    title VARCHAR(128) NOT NULL,
    description TEXT,
    cover_url VARCHAR(512) DEFAULT NULL,
    video_url VARCHAR(512) NOT NULL,
    duration_seconds INT NOT NULL DEFAULT 0,
    category VARCHAR(64) DEFAULT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PROCESSING',
    review_status VARCHAR(32) NOT NULL DEFAULT 'APPROVED',
    visibility VARCHAR(32) NOT NULL DEFAULT 'PUBLIC',
    view_count BIGINT NOT NULL DEFAULT 0,
    like_count BIGINT NOT NULL DEFAULT 0,
    favorite_count BIGINT NOT NULL DEFAULT 0,
    comment_count BIGINT NOT NULL DEFAULT 0,
    danmaku_count BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_video_author (author_id),
    KEY idx_video_hash (file_hash),
    KEY idx_video_status_time (status, created_at)
);

CREATE TABLE IF NOT EXISTS tag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tag_name (name)
);

CREATE TABLE IF NOT EXISTS video_tag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    video_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_video_tag (video_id, tag_id),
    KEY idx_video_tag_tag (tag_id)
);

CREATE TABLE IF NOT EXISTS video_interaction (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    video_id BIGINT NOT NULL,
    interaction_type VARCHAR(32) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_video_interaction (user_id, video_id, interaction_type),
    KEY idx_video_interaction_video (video_id)
);

CREATE TABLE IF NOT EXISTS video_comment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    video_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content VARCHAR(1000) NOT NULL,
    parent_id BIGINT DEFAULT NULL,
    like_count BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'VISIBLE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_comment_video_time (video_id, created_at),
    KEY idx_comment_parent (parent_id)
);

CREATE TABLE IF NOT EXISTS video_danmaku (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    video_id BIGINT NOT NULL,
    user_id BIGINT DEFAULT NULL,
    content VARCHAR(255) NOT NULL,
    play_time_seconds INT NOT NULL DEFAULT 0,
    color VARCHAR(16) NOT NULL DEFAULT '#FFFFFF',
    type VARCHAR(32) NOT NULL DEFAULT 'ROLLING',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_danmaku_video_time (video_id, play_time_seconds)
);

CREATE TABLE IF NOT EXISTS chat_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_key VARCHAR(128) NOT NULL,
    type VARCHAR(32) NOT NULL DEFAULT 'PRIVATE',
    last_message VARCHAR(500) DEFAULT NULL,
    last_message_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_chat_session_key (session_key)
);

CREATE TABLE IF NOT EXISTS chat_session_member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    unread_count INT NOT NULL DEFAULT 0,
    joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_chat_member (session_id, user_id),
    KEY idx_chat_member_user (user_id)
);

CREATE TABLE IF NOT EXISTS chat_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT DEFAULT NULL,
    content VARCHAR(2000) NOT NULL,
    message_type VARCHAR(32) NOT NULL DEFAULT 'TEXT',
    send_status VARCHAR(32) NOT NULL DEFAULT 'SENT',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_chat_message_session (session_id, created_at),
    KEY idx_chat_message_receiver (receiver_id)
);

CREATE TABLE IF NOT EXISTS search_sync_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    biz_type VARCHAR(32) NOT NULL,
    biz_id BIGINT NOT NULL,
    table_name VARCHAR(64) NOT NULL,
    operation_type VARCHAR(32) NOT NULL,
    payload_json JSON NOT NULL,
    sync_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_sync_status_time (sync_status, created_at)
);

INSERT INTO users (id, username, email, phone, password_hash, avatar_url, bio, status, fans_count, following_count, liked_count)
VALUES
    (1, 'creator_lin', 'lin@example.com', '13800000001', '$2a$10$0uY1k7aovvIhEEm8AdummyHashForDemo1', 'https://picsum.photos/seed/user1/128', '专注技术分享的视频创作者', 'ACTIVE', 125000, 128, 680000),
    (2, 'design_cat', 'cat@example.com', '13800000002', '$2a$10$0uY1k7aovvIhEEm8AdummyHashForDemo2', 'https://picsum.photos/seed/user2/128', '热爱 UI 与动效', 'ACTIVE', 98000, 260, 450000),
    (3, 'travel_joe', 'joe@example.com', '13800000003', '$2a$10$0uY1k7aovvIhEEm8AdummyHashForDemo3', 'https://picsum.photos/seed/user3/128', '旅行记录者', 'ACTIVE', 42000, 310, 210000);

INSERT INTO video (id, author_id, title, description, cover_url, video_url, duration_seconds, category, status, review_status, visibility, view_count, like_count, favorite_count, comment_count, danmaku_count)
VALUES
    (1, 1, 'Vue 3 组合式实战：从零做一个视频站首页', '围绕组合式 API、Pinia、路由拆解视频站首页搭建过程。', 'https://picsum.photos/seed/video1/640/360', 'https://example.com/videos/video1.mp4', 615, 'TECH', 'PUBLISHED', 'APPROVED', 'PUBLIC', 245000, 32000, 5800, 128, 36),
    (2, 2, '高质感上传页设计拆解', '讲解上传流程、状态反馈与视觉层级。', 'https://picsum.photos/seed/video2/640/360', 'https://example.com/videos/video2.mp4', 412, 'DESIGN', 'PUBLISHED', 'APPROVED', 'PUBLIC', 89000, 12800, 2300, 46, 18),
    (3, 3, '三天两夜城市漫游 Vlog', '剪辑节奏、镜头语言与配乐选择分享。', 'https://picsum.photos/seed/video3/640/360', 'https://example.com/videos/video3.mp4', 760, 'VLOG', 'PUBLISHED', 'APPROVED', 'PUBLIC', 156000, 19400, 3500, 75, 24);

INSERT INTO tag (id, name) VALUES (1, 'Vue'), (2, '前端'), (3, '设计'), (4, 'Vlog');

INSERT INTO video_tag (video_id, tag_id) VALUES
    (1, 1), (1, 2), (2, 3), (3, 4);

INSERT INTO video_danmaku (video_id, user_id, content, play_time_seconds, color, type) VALUES
    (1, 2, '这个动画做得很顺滑', 8, '#FF9800', 'ROLLING'),
    (1, 3, 'Pinia 这段讲得很清楚', 26, '#FFFFFF', 'ROLLING'),
    (2, 1, '上传流程体验很完整', 15, '#00E5FF', 'ROLLING');

INSERT INTO video_comment (video_id, user_id, content, parent_id, like_count) VALUES
    (1, 2, '组合式 API 这部分刚好解决了我项目里的状态拆分问题。', NULL, 128),
    (1, 3, '希望后面也讲一下路由守卫和权限控制。', NULL, 45),
    (2, 1, '封面上传和进度反馈这部分的交互细节很不错。', NULL, 18);

INSERT INTO user_follow (user_id, follow_user_id, status) VALUES
    (2, 1, 'FOLLOWING'),
    (3, 1, 'FOLLOWING'),
    (1, 2, 'FOLLOWING');

INSERT INTO friend_relation (user_id, friend_user_id, status) VALUES
    (1, 2, 'ACCEPTED'),
    (2, 1, 'ACCEPTED'),
    (1, 3, 'ACCEPTED'),
    (3, 1, 'ACCEPTED');

INSERT INTO chat_session (id, session_key, type, last_message, last_message_at) VALUES
    (1, 'private_1_2', 'PRIVATE', '收到，谢谢！', NOW()),
    (2, 'private_1_3', 'PRIVATE', '周末一起拍片吗？', NOW());

INSERT INTO chat_session_member (session_id, user_id, unread_count) VALUES
    (1, 1, 0), (1, 2, 1),
    (2, 1, 0), (2, 3, 2);

INSERT INTO chat_message (session_id, sender_id, receiver_id, content, message_type, send_status) VALUES
    (1, 1, 2, '新的首页视频我已经发布了。', 'TEXT', 'READ'),
    (1, 2, 1, '收到，谢谢！', 'TEXT', 'DELIVERED'),
    (2, 3, 1, '周末一起拍片吗？', 'TEXT', 'SENT');
