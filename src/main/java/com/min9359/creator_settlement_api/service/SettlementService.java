package com.min9359.creator_settlement_api.service;


import com.min9359.creator_settlement_api.domain.*;
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
        // 1. 크리에이터 존재 검증
        creatorMapper.findById(creatorId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "크리에이터를 찾을 수 없습니다: " + creatorId));

        // 2. 저장된 정산이 있는지 확인
        //    - CONFIRMED / PAID 는 변경 불가 스냅샷으로 간주 → 그대로 반환
        //    - PENDING 또는 미존재 → 최신 데이터로 재계산
        Optional<SettlementResponse> saved =
                settlementMapper.findSavedSettlement(creatorId, yearMonth.toString());

        if (saved.isPresent() && !"PENDING".equals(saved.get().getStatus())) {
            return saved.get();
        }

        // 3. 월 경계 계산
        // 시작: 해당 월 1일 00:00:00, 끝: 다음 달 1일 00:00:00 (배타적)
        // "말일 23:59:59" 대신 exclusive end 로 처리해 밀리초 단위 데이터도 안전하게 포함.
        LocalDateTime startAt = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endAtExclusive = yearMonth.plusMonths(1).atDay(1).atStartOfDay();

        // 4. 판매/취소 집계
        SaleAggregation saleAgg = saleRecordMapper.aggregateByCreatorAndPeriod(
                creatorId, startAt, endAtExclusive);
        CancelAggregation cancelAgg = cancelRecordMapper.aggregateByCreatorAndPeriod(
                creatorId, startAt, endAtExclusive);

        // 5. 금액 계산
        BigDecimal totalSales = saleAgg.getTotalAmount();
        BigDecimal totalRefunds = cancelAgg.getTotalAmount();
        BigDecimal netSales = totalSales.subtract(totalRefunds);

        // TODO: 수수료율 이력 도입 시, 정산 대상 월 시점에 유효했던 fee_rate 를 조회해 적용
        BigDecimal feeAmount = netSales.multiply(feeRate)
                .setScale(0, RoundingMode.DOWN);
        BigDecimal settlementAmount = netSales.subtract(feeAmount);

        SettlementResponse response = SettlementResponse.builder()
                .id(saved.map(SettlementResponse::getId).orElse(null))
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

        // 6. PENDING 재계산이면 update, 신규면 insert
        if (saved.isPresent()) {
            settlementMapper.updateSettlement(response);
        } else {
            settlementMapper.insertSettlement(response);
        }

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