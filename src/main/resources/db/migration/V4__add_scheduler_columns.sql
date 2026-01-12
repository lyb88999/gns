ALTER TABLE notification_tasks
ADD COLUMN last_run_at DATETIME NULL COMMENT 'Last time the task was executed',
ADD COLUMN next_run_at DATETIME NULL COMMENT 'Next scheduled execution time';
