CREATE TABLE IF NOT EXISTS teams (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    role ENUM('admin','team_admin','user') NOT NULL DEFAULT 'user',
    team_id BIGINT,
    status TINYINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_users_team FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS notification_tasks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id VARCHAR(100) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    user_id BIGINT,
    team_id BIGINT,
    trigger_type ENUM('api','cron','webhook') NOT NULL,
    cron_expression VARCHAR(100),
    channels JSON NOT NULL,
    message_template TEXT NOT NULL,
    custom_data JSON,
    rate_limit_enabled TINYINT NOT NULL DEFAULT 1,
    max_per_hour INT NOT NULL DEFAULT 10,
    max_per_day INT NOT NULL DEFAULT 100,
    silent_start TIME NULL,
    silent_end TIME NULL,
    merge_window_minutes INT NOT NULL DEFAULT 5,
    priority ENUM('low','normal','high','urgent') NOT NULL DEFAULT 'normal',
    status TINYINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_tasks_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_tasks_team FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE SET NULL,
    CONSTRAINT uk_task_id UNIQUE (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_tasks_user ON notification_tasks (user_id);
CREATE INDEX idx_tasks_team ON notification_tasks (team_id);

CREATE TABLE IF NOT EXISTS notification_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    notification_id VARCHAR(64) NOT NULL,
    task_id VARCHAR(100) NOT NULL,
    user_id BIGINT,
    channel VARCHAR(50) NOT NULL,
    recipient VARCHAR(200),
    subject VARCHAR(500),
    content TEXT,
    custom_data JSON,
    attachments JSON,
    status ENUM('pending','success','failed') NOT NULL DEFAULT 'pending',
    error_message TEXT,
    retry_count INT NOT NULL DEFAULT 0,
    sent_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_logs_task FOREIGN KEY (task_id) REFERENCES notification_tasks(task_id) ON DELETE CASCADE,
    CONSTRAINT fk_logs_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT uk_notification_id UNIQUE (notification_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_logs_task ON notification_logs (task_id);
CREATE INDEX idx_logs_user ON notification_logs (user_id);
CREATE INDEX idx_logs_status ON notification_logs (status);
CREATE INDEX idx_logs_created ON notification_logs (created_at);

CREATE TABLE IF NOT EXISTS notification_logs_archive (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    notification_id VARCHAR(64) NOT NULL,
    task_id VARCHAR(100) NOT NULL,
    user_id BIGINT,
    channel VARCHAR(50) NOT NULL,
    recipient VARCHAR(200),
    subject VARCHAR(500),
    content TEXT,
    custom_data JSON,
    attachments JSON,
    status ENUM('pending','success','failed') NOT NULL DEFAULT 'pending',
    error_message TEXT,
    retry_count INT NOT NULL DEFAULT 0,
    sent_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_notification_id_archive UNIQUE (notification_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_logs_archive_task ON notification_logs_archive (task_id);
CREATE INDEX idx_logs_archive_user ON notification_logs_archive (user_id);
CREATE INDEX idx_logs_archive_status ON notification_logs_archive (status);
CREATE INDEX idx_logs_archive_created ON notification_logs_archive (created_at);

CREATE TABLE IF NOT EXISTS api_tokens (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token VARCHAR(64) NOT NULL,
    name VARCHAR(100),
    scopes JSON,
    last_used_at TIMESTAMP NULL,
    expires_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_api_token UNIQUE (token),
    CONSTRAINT fk_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS system_configs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_key VARCHAR(100) NOT NULL,
    config_value TEXT,
    description VARCHAR(500),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_config_key UNIQUE (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
