CREATE TABLE mail_log (
    id BIGINT PRIMARY KEY,
    sentAt DATETIME NOT NULL,
    receiver VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NULL,
    template_type VARCHAR(255) NULL
)