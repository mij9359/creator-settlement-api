package com.min9359.creator_settlement_api.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancelRecord {
    private Long id;
    private String saleRecordId;
    private BigDecimal refundAmount;
    private LocalDateTime cancelledAt;
    private String reason;
    private LocalDateTime createdAt;
}
