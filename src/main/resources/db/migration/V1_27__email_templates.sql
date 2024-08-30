CREATE TABLE email_templates (
  id BIGINT AUTO_INCREMENT NOT NULL,
   title VARCHAR(255) NOT NULL,
   message TEXT NOT NULL,
   template_type VARCHAR(64) NOT NULL,
   enabled BIT(1) NULL,
   CONSTRAINT pk_email_templates PRIMARY KEY (id)
);