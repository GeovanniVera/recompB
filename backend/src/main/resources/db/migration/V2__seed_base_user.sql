-- Base user for the first phase. The Telegram chat id stays empty until it is linked.
INSERT INTO app_users (email, display_name, telegram_chat_id, active, created_at, updated_at)
VALUES ('geovanni.vera23@gmail.com', 'Geovanni Vera', NULL, 1, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE email = email;
