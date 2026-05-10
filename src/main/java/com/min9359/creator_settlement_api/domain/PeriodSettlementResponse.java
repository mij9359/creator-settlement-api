package com.min9359.creator_settlement_api.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PeriodSettlementResponse(
        Period period,
        BigDecimal feeRate,
        List<CreatorSettlement> creators,
        Summary summary
) {
    public record Period(LocalDate startDate, LocalDate endDate) {}

    public record CreatorSettlement(
            String creatorId, String creatorName,
            BigDecimal totalSales, BigDecimal totalRefunds, BigDecimal netSales,
            BigDecimal feeAmount, BigDecimal settlementAmount,
            long saleCount, long cancelCount
    ) {}

    public record Summary(
            int totalCreators, int activeCreators,
            BigDecimal totalSales, BigDecimal totalRefunds, BigDecimal totalNetSales,
            BigDecimal totalFeeAmount, BigDecimal totalSettlementAmount
    ) {}
}