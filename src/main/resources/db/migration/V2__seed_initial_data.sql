INSERT INTO teams (id, name, description, created_at, updated_at)
VALUES (1, 'Core Team', '默认初始团队', NOW(), NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name), description = VALUES(description);

INSERT INTO users (
    id, username, password, email, role, team_id, status, created_at, updated_at
) VALUES (
    1,
    'admin',
    '$2a$10$uC7G7zyBbs6k8ZZrVSu2Ce/Cy5jHcps225y7sY9qsK0kGugHgd6EC',
    'admin@example.com',
    'admin',
    NULL,
    1,
    NOW(),
    NOW()
) ON DUPLICATE KEY UPDATE
    username = VALUES(username),
    email = VALUES(email),
    role = VALUES(role),
    status = VALUES(status);

INSERT INTO users (
    id, username, password, email, role, team_id, status, created_at, updated_at
) VALUES (
    2,
    'team_admin',
    '$2a$10$JH4s2Rpzdg3NofM8JrIo7OQ.Y7mDRbugzuDUFcPRl1BDpRP70dNDO',
    'team_admin@example.com',
    'team_admin',
    1,
    1,
    NOW(),
    NOW()
) ON DUPLICATE KEY UPDATE
    username = VALUES(username),
    email = VALUES(email),
    role = VALUES(role),
    team_id = VALUES(team_id),
    status = VALUES(status);
