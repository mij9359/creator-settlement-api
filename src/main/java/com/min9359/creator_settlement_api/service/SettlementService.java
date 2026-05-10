package com.min9359.creator_settlement_api.service;


import com.min9359.creator_settlement_api.domain.*;
import com.min9359.creator_settlement_api.dto.CancelRecordCreateRequest;
import com.min9359.creator_settlement_api.mapper.CancelRecordMapper;
import com.min9359.creator_settlement_api.mapper.CreatorMapper;
import com.min9359.creator_settlement_api.mapper.SaleRecordMapper;
import com.min9359.creator_settlement_api.mapper.SettlementMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SettlementService {
    private final SaleRecordMapper saleRecordMapper;
    private final CancelRecordMapper cancelRecordMapper;
    private final CreatorMapper creatorMapper;
    private final SettlementMapper settlementMapper;

    // TODO: 향후 DB 기반의 정책 관리 서비스(PolicyService)를 통한 동적 수수료율 조회 방식으로 확장 가능
    @Value("${settlement.fee-rate:0.20}")
    private BigDecimal feeRate;

    @Transactional
    public SettlementResponse calculateMonthly(String creatorId, YearMonth yearMonth) {
        // 1. 이미 저장된 데이터가 있는지 먼저 확인 (중복 방지)
        Optional<SettlementResponse> saved = settlementMapper.findSavedSettlement(creatorId, yearMonth.toString());
        if (saved.isPresent()) {
            return saved.get();
        }

        // 2. 크리에이터 존재 검증 (기존 로직)
        creatorMapper.findById(creatorId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "크리에이터를 찾을 수 없습니다: " + creatorId));

        // 3. 월 경계 계산
        // 시작: 해당 월 1일 00:00:00
        // 끝: 다음 달 1일 00:00:00 (배타적, exclusive)
        // 명세는 "말일 23:59:59"이지만, 밀리초 단위 데이터까지 안전하게 포함하려면
        // < 다음달_1일_00:00:00 방식이 표준. 결과는 동일하면서 더 안전.
        LocalDateTime startAt = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endAtExclusive = yearMonth.plusMonths(1).atDay(1).atStartOfDay();

        // 4. 판매 집계
        SaleAggregation saleAgg = saleRecordMapper.aggregateByCreatorAndPeriod(
                creatorId, startAt, endAtExclusive);

        // 6. 환불 집계 (cancelled_at 기준)
        CancelAggregation cancelAgg = cancelRecordMapper.aggregateByCreatorAndPeriod(
                creatorId, startAt, endAtExclusive);

        // 금액 계산
        BigDecimal totalSales = saleAgg.getTotalAmount();
        BigDecimal totalRefunds = cancelAgg.getTotalAmount();
        BigDecimal netSales = totalSales.subtract(totalRefunds);

        // TODO: 과거 정산 내역 조회 시, 당시 적용되었던 수수료율 이력(History)을 조회하여 적용하도록 수정 필요
        // 수수료는 원 단위 절사 (0.5원 같은 거 안 만들기 위해)
        BigDecimal feeAmount = netSales.multiply(feeRate)
                .setScale(0, RoundingMode.DOWN);
        BigDecimal settlementAmount = netSales.subtract(feeAmount);

        SettlementResponse response = SettlementResponse.builder()
                .creatorId(creatorId)
                .yearMonth(yearMonth.toString())
                .totalSales(totalSales)
                .totalRefunds(totalRefunds)
                .netSales(netSales)
                .feeRate(feeRate)
                .feeAmount(feeAmount)
                .settlementAmount(settlementAmount)
                .saleCount(saleAgg.getCount())
                .cancelCount(cancelAgg.getCount())
                .status("PENDING")
                .build();

        // 7. DB에 저장
        settlementMapper.insertSettlement(response);

        return response;
    }

    public PeriodSettlementResponse calculateForPeriod(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startAt = startDate.atStartOfDay();
        LocalDateTime endAtExclusive = endDate.plusDays(1).atStartOfDay();

        List<CreatorAggregation> aggregations = settlementMapper.aggregateAllByPeriod(startAt, endAtExclusive);

        List<PeriodSettlementResponse.CreatorSettlement> creators = aggregations.stream().map(this::mapToCreatorSettlementDto).toList();

        //전체 합계 데이터 계산
        PeriodSettlementResponse.Summary summary = buildSummary(creators);

        return new PeriodSettlementResponse(
                new PeriodSettlementResponse.Period(startDate, endDate),
                feeRate,
                creators,
                summary
        );
    }

    private PeriodSettlementResponse.Summary buildSummary(List<PeriodSettlementResponse.CreatorSettlement> creators) {
        // 1. 활성 크리에이터 수 (판매나 취소가 1건이라도 있는 경우)
        int activeCreators = (int) creators.stream()
                .filter(c -> c.saleCount() > 0 || c.cancelCount() > 0)
                .count();

        // 2. 전체 금액 합산 (BigDecimal 특성상 reduce 사용)
        BigDecimal totalSales = creators.stream()
                .map(PeriodSettlementResponse.CreatorSettlement::totalSales)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRefunds = creators.stream()
                .map(PeriodSettlementResponse.CreatorSettlement::totalRefunds)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalNetSales = totalSales.subtract(totalRefunds);

        BigDecimal totalFeeAmount = creators.stream()
                .map(PeriodSettlementResponse.CreatorSettlement::feeAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSettlementAmount = creators.stream()
                .map(PeriodSettlementResponse.CreatorSettlement::settlementAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new PeriodSettlementResponse.Summary(
                creators.size(),
                activeCreators,
                totalSales,
                totalRefunds,
                totalNetSales,
                totalFeeAmount,
                totalSettlementAmount
        );
    }


    private PeriodSettlementResponse.CreatorSettlement mapToCreatorSettlementDto(CreatorAggregation agg) {
        BigDecimal totalSales = agg.getSaleTotalAmount();
        BigDecimal totalRefunds = agg.getCancelTotalAmount();
        BigDecimal netSales = totalSales.subtract(totalRefunds);

        // 수수료 계산: 순판매액 * 수수료율 (원 단위 절사)
        BigDecimal feeAmount = netSales.multiply(feeRate)
                .setScale(0, RoundingMode.DOWN);

        BigDecimal settlementAmount = netSales.subtract(feeAmount);

        return new PeriodSettlementResponse.CreatorSettlement(
                agg.getCreatorId(),   // 이제 빨간 줄이 사라질 겁니다!
                agg.getCreatorName(),
                totalSales,
                totalRefunds,
                netSales,
                feeAmount,
                settlementAmount,
                agg.getSaleCount(),
                agg.getCancelCount()
        );
    }

    @Transactional
    public void confirmSettlement(Long settlementId) {
        SettlementResponse settlement = settlementMapper.findById(settlementId)
                .orElseThrow(() -> new IllegalArgumentException("정산 내역을 찾을 수 없습니다."));

        if (!"PENDING".equals(settlement.getStatus())) {
            throw new IllegalStateException("PENDING 상태인 정산만 확정할 수 있습니다.");
        }

        settlementMapper.updateStatus(settlementId, "CONFIRMED");
    }

    @Transactional
    public void paySettlement(Long settlementId) {
        SettlementResponse settlement = settlementMapper.findById(settlementId)
                .orElseThrow(() -> new IllegalArgumentException("정산 내역을 찾을 수 없습니다."));

        // 비즈니스 규칙: CONFIRMED 상태인 것만 지급 완료 처리 가능
        if (!"CONFIRMED".equals(settlement.getStatus())) {
            throw new IllegalStateException("CONFIRMED 상태인 정산만 '지급 완료' 처리가 가능합니다. " +
                    "현재 상태: " + settlement.getStatus());
        }

        settlementMapper.updateStatus(settlementId, "PAID");
    }
}