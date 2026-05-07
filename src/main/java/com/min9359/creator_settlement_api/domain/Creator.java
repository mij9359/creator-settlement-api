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
}
