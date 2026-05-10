package com.min9359.creator_settlement_api.service;

import com.min9359.creator_settlement_api.domain.*;
import com.min9359.creator_settlement_api.mapper.CancelRecordMapper;
import com.min9359.creator_settlement_api.mapper.CreatorMapper;
import com.min9359.creator_settlement_api.mapper.SaleRecordMapper;
import com.min9359.creator_settlement_api.mapper.SettlementMapper;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class SettlementServiceTest {

    @Mock
    private SaleRecordMapper saleRecordMapper;
    @Mock
    private CancelRecordMapper cancelRecordMapper;
    @Mock
    private CreatorMapper creatorMapper;
    @Mock
    private SettlementMapper settlementMapper;
    @InjectMocks
    private SettlementService settlementService;

    @BeforeEach
    void setUp() {
        // application.yml에 있는 수수료율 설정을 수동으로 주입합니다
        ReflectionTestUtils.setField(settlementService, "feeRate", new BigDecimal("0.20"));
    }

    @Test
    @DisplayName("ST-1: 명세서 기준 정산 계산 검증 (26만 판매, 11만 환불 → 12만 정산)")
    void calculateMonthly_Success() {
        // given
        String creatorId = "creator-1";
        given(creatorMapper.findById(creatorId)).willReturn(Optional.of(new Creator()));
        given(saleRecordMapper.aggregateByCreatorAndPeriod(any(), any(), any()))
                .willReturn(new SaleAggregation(new BigDecimal("260000"), 4));
        given(cancelRecordMapper.aggregateByCreatorAndPeriod(any(), any(), any()))
                .willReturn(new CancelAggregation(new BigDecimal("110000"), 2));

        // when
        SettlementResponse response = settlementService.calculateMonthly(creatorId, YearMonth.of(2025, 3));

        // then
        assertThat(response.getNetSales()).isEqualByComparingTo("150000");
        assertThat(response.getSettlementAmount()).isEqualByComparingTo("120000");
        assertThat(response.getFeeAmount()).isEqualByComparingTo("30000");
    }

    @Test
    @DisplayName("음수 정산 검증: 환불이 판매보다 많을 경우 정산 예정 금액은 음수가 나와야 한다")
    void calculateMonthly_NegativeSettlement() {
        // given: 판매 0원, 환불 6만원 설정
        given(creatorMapper.findById(any())).willReturn(Optional.of(new Creator()));
        given(saleRecordMapper.aggregateByCreatorAndPeriod(any(), any(), any()))
                .willReturn(new SaleAggregation(BigDecimal.ZERO, 0));
        given(cancelRecordMapper.aggregateByCreatorAndPeriod(any(), any(), any()))
                .willReturn(new CancelAggregation(new BigDecimal("60000"), 1));

        // when
        SettlementResponse response = settlementService.calculateMonthly("creator-2", YearMonth.of(2025, 2));

        // then: 순판매 -6만, 수수료 -1.2만, 정산금 -4.8만
        assertThat(response.getNetSales()).isEqualByComparingTo("-60000");
        assertThat(response.getSettlementAmount()).isEqualByComparingTo("-48000");
    }

    @Test
    @DisplayName("예외 테스트: 존재하지 않는 크리에이터 요청 시 예외가 발생해야 한다")
    void calculateMonthly_CreatorNotFound() {
        // given: 존재하지 않는 크리에이터 설정
        given(creatorMapper.findById("unknown")).willReturn(java.util.Optional.empty());

        // when & then: 예외가 발생하는지 검증
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> {
            settlementService.calculateMonthly("unknown", YearMonth.of(2025, 3));
        });
    }

    @Test
    @DisplayName("운영자 집계 검증: 여러 크리에이터의 정산 합계가 요약 정보에 정확히 반영되어야 한다")
    void calculateForPeriod_SummaryCheck() {
        // given: 두 명의 크리에이터 집계 데이터 세팅
        CreatorAggregation agg1 = CreatorAggregation.builder()
                .creatorId("c1").saleTotalAmount(new BigDecimal("100000")).saleCount(1)
                .cancelTotalAmount(BigDecimal.ZERO).cancelCount(0).build();
        CreatorAggregation agg2 = CreatorAggregation.builder()
                .creatorId("c2").saleTotalAmount(new BigDecimal("200000")).saleCount(1)
                .cancelTotalAmount(new BigDecimal("50000")).cancelCount(1).build();

        given(settlementMapper.aggregateAllByPeriod(any(), any())).willReturn(java.util.List.of(agg1, agg2));

        // when
        PeriodSettlementResponse response = settlementService.calculateForPeriod(LocalDate.of(2025, 3, 1), LocalDate.of(2025, 3, 31));

        // then: 전체 매출 30만 - 전체 취소 5만 = 순매출 25만 확인
        assertThat(response.summary().totalSales()).isEqualByComparingTo("300000");
        assertThat(response.summary().totalNetSales()).isEqualByComparingTo("250000");
        assertThat(response.summary().totalCreators()).isEqualTo(2);
    }

    @Test
    @DisplayName("운영자 집계 검증: Swagger 입력값이 다음날 00:00(Exclusive)으로 정확히 변환되는지 확인")
    void calculateForPeriod_VerifyDateConversion() {
        // 1. Given: 사용자가 Swagger에서 선택한 날짜
        LocalDate startDate = LocalDate.of(2025, 2, 1);
        LocalDate endDate = LocalDate.of(2025, 2, 28);

        // 서비스 로직 및 SQL 파라미터와 동일한 기준 설정
        LocalDateTime expectedStart = startDate.atStartOfDay();
        LocalDateTime expectedEnd = endDate.plusDays(1).atStartOfDay(); // SQL의 < #{endAtExclusive}와 매칭

        given(settlementMapper.aggregateAllByPeriod(any(), any())).willReturn(java.util.List.of());

        // 2. When
        settlementService.calculateForPeriod(startDate, endDate);

        // 3. Then: 매퍼가 '다음날 00시'를 인자로 받았는지 검증
        org.mockito.Mockito.verify(settlementMapper).aggregateAllByPeriod(
                expectedStart,
                expectedEnd
        );
    }
}
