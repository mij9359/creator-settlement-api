package com.min9359.creator_settlement_api.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Creator {
    private String id;
    private String name;
    private LocalDateTime createdAt;

    // TODO: 크리에이터별 차등 수수료 적용이 필요할 경우 해당 필드 활용 (NULL일 경우 기본 수수료율 적용)
    // private BigDecimal customFeeRate;
}
