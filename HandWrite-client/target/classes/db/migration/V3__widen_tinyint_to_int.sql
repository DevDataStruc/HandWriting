-- V3: 将状态/枚举字段从 TINYINT 调整为 INT，匹配 JPA Integer 默认映射
-- 原因：Hibernate 6 在 schema-validation 模式下忽略 @Column(columnDefinition)，
--      会按 Java 类型推断 JDBC Type Code。Integer -> Types#INTEGER -> 期望列类型为 INT。

ALTER TABLE t_user        MODIFY status   INT NOT NULL DEFAULT 0;
ALTER TABLE t_user        MODIFY deleted  INT NOT NULL DEFAULT 0;
ALTER TABLE t_char_dict   MODIFY difficulty INT NOT NULL DEFAULT 1;
ALTER TABLE t_char_dict   MODIFY enabled    INT NOT NULL DEFAULT 1;
ALTER TABLE t_sample      MODIFY status   INT NOT NULL DEFAULT 0;
ALTER TABLE t_sample      MODIFY deleted  INT NOT NULL DEFAULT 0;
