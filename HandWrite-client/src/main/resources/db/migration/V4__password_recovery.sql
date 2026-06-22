-- V4: 密码找回相关表（2FA / TOTP + 密保问题）
-- ==========================================

-- 用户 2FA (TOTP) 绑定表
CREATE TABLE IF NOT EXISTS t_user_totp (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    secret VARCHAR(64) NOT NULL COMMENT 'Base32 编码的 TOTP 密钥（密文存储）',
    issuer VARCHAR(64) NOT NULL DEFAULT 'HandWrite' COMMENT '认证器中显示的发行者',
    account_label VARCHAR(128) NOT NULL COMMENT 'otpauth:// 中的账户名（一般为用户名）',
    recovery_codes TEXT NOT NULL COMMENT 'JSON 数组：每项为恢复码的 BCrypt 密文',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0 启用 / 1 已停用',
    confirmed_at DATETIME COMMENT '首次绑定并验证通过的时间',
    last_used_step BIGINT NOT NULL DEFAULT 0 COMMENT '防重放：上次成功验证的 TOTP step',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_totp_user (user_id),
    KEY idx_totp_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户 2FA 绑定';

-- 用户密保问题（每个用户 3 题）
CREATE TABLE IF NOT EXISTS t_user_security_question (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    question_index INT NOT NULL COMMENT '问题序号 1/2/3',
    question_key VARCHAR(64) NOT NULL COMMENT '预设问题 key（如 favoriteTeacher）',
    question_text VARCHAR(255) NOT NULL COMMENT '问题正文（可自定义）',
    answer_hash VARCHAR(255) NOT NULL COMMENT 'BCrypt 答案密文（小写归一化后哈希）',
    salt VARCHAR(64) NOT NULL COMMENT '每题独立 salt',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sq_user_idx (user_id, question_index),
    KEY idx_sq_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户密保问题';

-- 密码恢复短期 token（一次性，5 分钟内有效）
CREATE TABLE IF NOT EXISTS t_password_reset_token (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(128) NOT NULL,
    user_id BIGINT NOT NULL,
    method VARCHAR(32) NOT NULL COMMENT 'TOTP / SECURITY_QUESTION',
    challenge_payload TEXT COMMENT 'JSON：方法特有上下文（如已验证的密保序号集合）',
    used TINYINT NOT NULL DEFAULT 0,
    expired_at DATETIME NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_pr_token (token),
    KEY idx_pr_user (user_id),
    KEY idx_pr_expire (expired_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='密码恢复一次性 token';
