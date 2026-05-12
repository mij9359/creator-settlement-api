package com.min9359.creator_settlement_api.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "월별 정산 응답")
public class SettlementResponse {

    private Long id;

    @Schema(description = "크리에이터 ID", example = "creator-1")
    private String creatorId;

    @Schema(description = "조회 연월", example = "2025-03")
    private String yearMonth;

    @Schema(description = "총 판매 금액", example = "260000")
    private BigDecimal totalSales;

    @Schema(description = "환불 금액 합계", example = "110000")
    private BigDecimal totalRefunds;

    @Schema(description = "순 판매 금액 (총 판매 - 환불)", example = "150000")
    private BigDecimal netSales;

    @Schema(description = "수수료율 (현재 20%)", example = "0.20")
    private BigDecimal feeRate;

    @Schema(description = "플랫폼 수수료 (순 판매 × 수수료율, 원 단위 절사)", example = "30000")
    private BigDecimal feeAmount;

    @Schema(description = "정산 예정 금액 (순 판매 - 수수료)", example = "120000")
    private BigDecimal settlementAmount;

    @Schema(description = "판매 건수", example = "4")
    private long saleCount;

    @Schema(description = "취소 건수", example = "2")
    private long cancelCount;

    @Schema(description = "정산 상태 (PENDING, CONFIRMED, PAID)", example = "PENDING")
    private String status;
}
