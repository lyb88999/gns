-- Fix historical log status for Silent Mode
-- Only update if status is 'blocked' or 'failed' AND error message contains "Silent Mode"

UPDATE notification_logs
SET status = 'skipped'
WHERE (status = 'blocked' OR status = 'failed')
  AND error_message LIKE '%Silent Mode%';
