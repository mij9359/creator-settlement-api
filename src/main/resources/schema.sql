-- 기존 테이블 삭제 (재시작 시 깨끗하게)
DROP TABLE IF EXISTS cancel_record;
DROP TABLE IF EXISTS sale_record;
DROP TABLE IF EXISTS course;
DROP TABLE IF EXISTS creator;

-- 크리에이터
CREATE TABLE creator (
                         id          VARCHAR(50)  PRIMARY KEY,
                         name        VARCHAR(100) NOT NULL,
                         created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 강의
CREATE TABLE course (
                        id          VARCHAR(50)  PRIMARY KEY,
                        creator_id  VARCHAR(50)  NOT NULL,
                        title       VARCHAR(255) NOT NULL,
                        created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        CONSTRAINT fk_course_creator FOREIGN KEY (creator_id) REFERENCES creator(id)
);

-- 판매 내역
CREATE TABLE sale_record (
                             id          VARCHAR(50)    PRIMARY KEY,
                             course_id   VARCHAR(50)    NOT NULL,
                             student_id  VARCHAR(50)    NOT NULL,
                             amount      DECIMAL(15, 2) NOT NULL,
                             paid_at     DATETIME       NOT NULL,
                             created_at  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             CONSTRAINT fk_sale_course FOREIGN KEY (course_id) REFERENCES course(id)
);

CREATE INDEX idx_sale_paid_at ON sale_record(paid_at);
CREATE INDEX idx_sale_course  ON sale_record(course_id);

-- 취소/환불 내역
CREATE TABLE cancel_record (
                               id              BIGINT         AUTO_INCREMENT PRIMARY KEY,
                               sale_record_id  VARCHAR(50)    NOT NULL,
                               refund_amount   DECIMAL(15, 2) NOT NULL,
                               cancelled_at    DATETIME       NOT NULL,
                               reason          VARCHAR(255),
                               created_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               CONSTRAINT fk_cancel_sale FOREIGN KEY (sale_record_id) REFERENCES sale_record(id)
);

CREATE INDEX idx_cancel_cancelled_at ON cancel_record(cancelled_at);
CREATE INDEX idx_cancel_sale          ON cancel_record(sale_record_id);