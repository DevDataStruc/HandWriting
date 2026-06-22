-- V5: 兼容旧 V4（状态/标记列从 TINYINT 改回 INT）
-- V4 早期版本使用了 TINYINT，与 Hibernate 实体（Integer）不匹配。
-- 实体统一映射 INT，本迁移只对已执行过旧版 V4 的环境生效。
-- ==========================================

ALTER TABLE t_user_totp          MODIFY COLUMN status INT NOT NULL DEFAULT 0 COMMENT '0 启用 / 1 已停用';
ALTER TABLE t_password_reset_token MODIFY COLUMN used   INT NOT NULL DEFAULT 0 COMMENT '0 未使用 / 1 已使用';
