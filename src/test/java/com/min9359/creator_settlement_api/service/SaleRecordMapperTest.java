package com.min9359.creator_settlement_api.service;

import com.min9359.creator_settlement_api.domain.SaleAggregation;

import com.min9359.creator_settlement_api.mapper.CancelRecordMapper;
import com.min9359.creator_settlement_api.mapper.CreatorMapper;
import com.min9359.creator_settlement_api.mapper.SaleRecordMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.jdbc.Sql;


import java.time.LocalDateTime;


import static org.assertj.core.api.Assertions.assertThat;


@MybatisTest // MyBatis 전용 테스트 환경
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // 실제 H2 사용
class SaleRecordMapperTest {

    @Autowired
    private SaleRecordMapper saleRecordMapper;

    @Autowired
    private CreatorMapper creatorMapper;

    @Autowired
    private CancelRecordMapper cancelRecordMapper;

    @Autowired
    private SettlementService settlementService;

    @Test
    @DisplayName("경계값 테스트: 3월 31일 23:59:59 데이터가 3월 정산에 포함되어야 한다")
    @Sql(statements = "INSERT INTO sale_record (id, course_id, student_id, amount, paid_at) VALUES ('edge-sale', 'course-1', 'student-1', 10000, '2025-03-31 23:59:59')")
    void aggregate_BoundaryCheck() {
        // given
        LocalDateTime startAt = LocalDateTime.of(2025, 3, 1, 0, 0);
        LocalDateTime endAtExclusive = LocalDateTime.of(2025, 4, 1, 0, 0);

        // when
        SaleAggregation result = saleRecordMapper.aggregateByCreatorAndPeriod("creator-1", startAt, endAtExclusive);

        // then: 기존 샘플 데이터 26만 원 + 신규 1만 원 = 27만 원 확인
        assertThat(result.getTotalAmount()).isEqualByComparingTo("270000");
    }

    @Test
    @DisplayName("SQL 기간 필터링 검증: 3월 조회 시 2월말/4월초 데이터는 집계에서 제외되어야 한다")
    @Sql(statements = {
            "DELETE FROM sale_record", // 기존 데이터 청소
            // 2월 28일 데이터 (제외 대상)
            "INSERT INTO sale_record (id, course_id, student_id, amount, paid_at) VALUES ('S1', 'C1', 'ST1', 50000, '2025-02-28 23:59:59')",
            // 3월 1일 데이터 (포함 대상)
            "INSERT INTO sale_record (id, course_id, student_id, amount, paid_at) VALUES ('S2', 'C1', 'ST1', 100000, '2025-03-01 00:00:00')",
            // 3월 31일 데이터 (포함 대상)
            "INSERT INTO sale_record (id, course_id, student_id, amount, paid_at) VALUES ('S3', 'C1', 'ST1', 20000, '2025-03-31 23:59:59')",
            // 4월 1일 데이터 (제외 대상)
            "INSERT INTO sale_record (id, course_id, student_id, amount, paid_at) VALUES ('S4', 'C1', 'ST1', 99000, '2025-04-01 00:00:00')"
    })
    void aggregateByPeriod_SQLCheck() {
        // 1. Given: 3월 한 달간의 조회 범위 설정
        // 서비스 레이어의 calculateMonthly 로직과 동일한 시간 기준 적용
        LocalDateTime startAt = LocalDateTime.of(2025, 3, 1, 0, 0);
        LocalDateTime endAtExclusive = LocalDateTime.of(2025, 4, 1, 0, 0);

        // 2. When: 매퍼 호출 (전체 크리에이터 기준 조회를 위해 creatorId는 고정값 사용)
        SaleAggregation result = saleRecordMapper.aggregateByCreatorAndPeriod("creator-1", startAt, endAtExclusive);

        // 3. Then: 3월 데이터인 100,000 + 20,000 = 120,000원만 나와야 함
        assertThat(result.getTotalAmount()).isEqualByComparingTo("120000");
        assertThat(result.getCount()).isEqualTo(2);
    }

}
