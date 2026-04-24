CREATE TABLE student
(
    account_id   UUID         NOT NULL PRIMARY KEY,
    student_code VARCHAR(20)  NOT NULL,
    full_name    VARCHAR(100) NOT NULL,
    date_of_birth DATE,
    major        VARCHAR(150),
    citizen_id   VARCHAR(20),
    avatar_url   TEXT,
    created_at   TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP,
    CONSTRAINT fk_student_account FOREIGN KEY (account_id)
        REFERENCES account (id) ON DELETE CASCADE,
    CONSTRAINT uq_student_code UNIQUE (student_code)
);


CREATE TABLE examiner
(
    account_id    UUID        NOT NULL PRIMARY KEY,
    examiner_code VARCHAR(20) NOT NULL,
    full_name     VARCHAR(100) NOT NULL,
    department    VARCHAR(150),
    avatar_url    TEXT,
    created_at    TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP,
    CONSTRAINT fk_examiner_account FOREIGN KEY (account_id)
        REFERENCES account (id) ON DELETE CASCADE,
    CONSTRAINT uq_examiner_code UNIQUE (examiner_code)
);
