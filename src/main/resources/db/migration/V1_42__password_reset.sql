
alter table users
    add column password_reset_request_date datetime     NULL,
    add column password_reset_token        VARCHAR(255) NULL;
