CREATE TABLE mail_log (
    id BIGINT PRIMARY KEY,
    sent_at DATETIME NOT NULL,
    receiver VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NULL,
    template_type VARCHAR(255) NULL
);

CREATE INDEX idx_mail_log_sent_at ON mail_log (sent_at);