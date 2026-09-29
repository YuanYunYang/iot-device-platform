-- 默认账号密码与文档一致：admin / tenant_admin 的密码均为 admin123。
-- V1 中的 BCrypt 哈希无法通过校验，这里替换为 Hutool BCrypt 生成的有效哈希。
UPDATE `sys_user`
SET `password` = '$2a$10$NzKJFONiWXRsY06DbI00.ORjxqjr3uyCggd9rtRIZcLb5U9XPRmb.'
WHERE `username` IN ('admin', 'tenant_admin');
