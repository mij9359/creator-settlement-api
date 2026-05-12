package com.min9359.creator_settlement_api.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatorAggregation {
    private String creatorId;
    private String creatorName;
    private BigDecimal saleTotalAmount;
    private long saleCount;
    private BigDecimal cancelTotalAmount;
    private long cancelCount;
}
