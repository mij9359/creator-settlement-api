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
public class Course {
    private String id;
    private String creatorId;
    private String title;
    private LocalDateTime createdAt;
}
