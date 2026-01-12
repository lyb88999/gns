ALTER TABLE notification_logs MODIFY COLUMN status ENUM('pending','success','failed','blocked') NOT NULL DEFAULT 'pending';
ALTER TABLE notification_logs_archive MODIFY COLUMN status ENUM('pending','success','failed','blocked') NOT NULL DEFAULT 'pending';
