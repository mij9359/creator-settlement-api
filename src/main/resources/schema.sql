-- 기존 테이블 삭제 (외래 키 제약 조건 때문에 삭제 순서 중요)
DROP TABLE IF EXISTS settlement;
DROP TABLE IF EXISTS cancel_record;
DROP TABLE IF EXISTS sale_record;
DROP TABLE IF EXISTS course;
DROP TABLE IF EXISTS creator;

-- 1. 크리에이터
CREATE TABLE creator (
                         id          VARCHAR(50)  PRIMARY KEY,
                         name        VARCHAR(100) NOT NULL,
                         created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. 강의
CREATE TABLE course (
                        id          VARCHAR(50)  PRIMARY KEY,
                        creator_id  VARCHAR(50)  NOT NULL,
                        title       VARCHAR(255) NOT NULL,
                        created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        CONSTRAINT fk_course_creator FOREIGN KEY (creator_id) REFERENCES creator(id)
);

-- 3. 판매 내역
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

-- 4. 취소/환불 내역
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

-- 5. 정산 내역 (중복 방지 및 상태 관리용 추가)
CREATE TABLE settlement (
                            id                  BIGINT         AUTO_INCREMENT PRIMARY KEY,
                            creator_id          VARCHAR(50)    NOT NULL,
                            settlement_year_month VARCHAR(7)    NOT NULL, -- 예: '2025-03'
                            total_sales         DECIMAL(15, 2) NOT NULL,
                            total_refunds       DECIMAL(15, 2) NOT NULL,
                            net_sales           DECIMAL(15, 2) NOT NULL,

    -- [추가] 이력 관리를 위해 당시 수수료율을 저장 (가산점 포인트)
                            fee_rate            DECIMAL(5, 2)  NOT NULL,

                            fee_amount          DECIMAL(15, 2) NOT NULL,
                            settlement_amount   DECIMAL(15, 2) NOT NULL,
                            sale_count          INT            NOT NULL DEFAULT 0,
                            cancel_count        INT            NOT NULL DEFAULT 0,
                            status              VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
                            created_at          DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at          DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                            CONSTRAINT fk_settlement_creator FOREIGN KEY (creator_id) REFERENCES creator(id),
                            CONSTRAINT uk_creator_month UNIQUE (creator_id, settlement_year_month)
);

CREATE INDEX idx_settlement_month ON settlement(settlement_year_month);
CREATE INDEX idx_settlement_status ON settlement(status);