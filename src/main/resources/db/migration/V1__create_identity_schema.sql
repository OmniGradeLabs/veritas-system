CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE university
(
    id         UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    code       VARCHAR(50) NOT NULL,
    name       VARCHAR(150) NOT NULL,
    domain     VARCHAR(100),
    created_at TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at TIMESTAMP,
    CONSTRAINT uq_university_code UNIQUE (code)
);


CREATE TABLE account
(
    id             UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    university_id  UUID        NOT NULL,
    username       VARCHAR(100) NOT NULL,
    password       TEXT        NOT NULL,
    email          VARCHAR(100),
    status         VARCHAR(20) NOT NULL,
    is_first_login BOOLEAN     NOT NULL DEFAULT TRUE,
    deleted_at     TIMESTAMP,
    created_at     TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at     TIMESTAMP,
    CONSTRAINT uq_account_username_univ UNIQUE (university_id, username),
    CONSTRAINT fk_account_university FOREIGN KEY (university_id)
        REFERENCES university (id) ON DELETE RESTRICT
);

CREATE INDEX idx_account_university ON account (university_id);
CREATE INDEX idx_account_status ON account (status) WHERE deleted_at IS NULL;


CREATE TABLE session
(
    id          UUID      NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id     UUID      NOT NULL,
    refresh_jti UUID      NOT NULL,
    revoked_at  TIMESTAMP,
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP,
    CONSTRAINT fk_session_account FOREIGN KEY (user_id)
        REFERENCES account (id) ON DELETE CASCADE,
    CONSTRAINT uq_session_refresh_jti UNIQUE (refresh_jti)
);

CREATE INDEX idx_session_user_id ON session (user_id);
CREATE INDEX idx_session_active ON session (user_id, expires_at)
    WHERE revoked_at IS NULL;


CREATE TABLE role
(
    id          UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    created_at  TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP,
    CONSTRAINT uq_role_name UNIQUE (name)
);


CREATE TABLE user_role
(
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    CONSTRAINT pk_user_role PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_account FOREIGN KEY (user_id)
        REFERENCES account (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id)
        REFERENCES role (id) ON DELETE CASCADE
);


CREATE TABLE permission
(
    id         UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    scope      VARCHAR(50)  NOT NULL,
    module     VARCHAR(80),
    resource   VARCHAR(120) NOT NULL,
    label      VARCHAR(160) NOT NULL,
    action     VARCHAR(30)  NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at TIMESTAMP,
    CONSTRAINT uq_permission_name UNIQUE (name)
);


CREATE TABLE permission_group
(
    id            UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    university_id UUID         NOT NULL,
    name          VARCHAR(150) NOT NULL,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP,
    CONSTRAINT fk_group_university FOREIGN KEY (university_id)
        REFERENCES university (id) ON DELETE CASCADE
);

CREATE INDEX idx_group_university ON permission_group (university_id);


CREATE TABLE account_group
(
    id         UUID      NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    account_id UUID      NOT NULL,
    group_id   UUID      NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP,
    CONSTRAINT uq_account_group UNIQUE (account_id, group_id),
    CONSTRAINT fk_account_group_account FOREIGN KEY (account_id)
        REFERENCES account (id) ON DELETE CASCADE,
    CONSTRAINT fk_account_group_group FOREIGN KEY (group_id)
        REFERENCES permission_group (id) ON DELETE CASCADE
);


CREATE TABLE group_permission
(
    group_id      UUID NOT NULL,
    permission_id UUID NOT NULL,
    CONSTRAINT pk_group_permission PRIMARY KEY (group_id, permission_id),
    CONSTRAINT fk_group_permission_group FOREIGN KEY (group_id)
        REFERENCES permission_group (id) ON DELETE CASCADE,
    CONSTRAINT fk_group_permission_perm FOREIGN KEY (permission_id)
        REFERENCES permission (id) ON DELETE CASCADE
);
