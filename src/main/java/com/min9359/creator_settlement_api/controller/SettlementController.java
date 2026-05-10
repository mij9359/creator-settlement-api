package com.min9359.creator_settlement_api.controller;

import com.min9359.creator_settlement_api.domain.PeriodSettlementResponse;
import com.min9359.creator_settlement_api.domain.SettlementResponse;
import com.min9359.creator_settlement_api.service.SettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.YearMonth;


@Tag(name = "정산", description = "정산 계산 API")
@RestController
@RequestMapping("/api/settlements")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    @Operation(summary = "크리에이터별 월별 정산 조회",
            description = "특정 크리에이터의 한 달 치 정산 데이터를 계산합니다. 판매는 결제일, 취소는 취소일 기준으로 집계됩니다.")
    @GetMapping("/creators/{creatorId}")
    public SettlementResponse getMonthlySettlement(
            @PathVariable String creatorId,
            @Parameter(description = "조회 연월 (YYYY-MM)", example = "2025-03")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth yearMonth) {
        return settlementService.calculateMonthly(creatorId, yearMonth);
    }


    @Operation(
            summary = "전체 크리에이터 기간 정산 집계 (운영자용)",
            description = """
        지정된 기간(startDate ~ endDate) 동안의 전체 크리에이터 정산 현황을 집계합니다.
        
        주요 설계 특징:
        - 활동 내역(판매/취소)이 없는 크리에이터를 포함하여 전체 현황을 제공합니다.
        - 대량 데이터 조회 성능을 고려하여 단일 쿼리(Join & Subquery)로 N+1 문제를 최적화했습니다.
        - 시작일 00:00:00부터 종료일 23:59:59까지의 데이터를 포함합니다.
        """
    )
    @GetMapping
    public PeriodSettlementResponse getPeriodSettlement(
            @Parameter(description = "조회 시작일 (yyyy-MM-dd)", example = "2025-03-01") //
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

            @Parameter(description = "조회 종료일 (yyyy-MM-dd)", example = "2025-03-31") //
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("종료일은 시작일 이후여야 합니다.");
        }

        return settlementService.calculateForPeriod(startDate, endDate);
    }

    @Operation(summary = "정산 확정", description = "PENDING 상태의 정산을 CONFIRMED 상태로 변경합니다.")
    @PatchMapping("/{id}/confirm")
    public void confirmSettlement(@PathVariable Long id) {
        settlementService.confirmSettlement(id);
    }

    @Operation(summary = "정산 지급 완료", description = "CONFIRMED 상태의 정산을 PAID 상태로 변경합니다.")
    @PatchMapping("/{id}/pay")
    public void paySettlement(@PathVariable Long id) {
        settlementService.paySettlement(id);
    }
}
