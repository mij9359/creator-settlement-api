package com.min9359.creator_settlement_api.domain;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "판매 내역")
public class SaleRecord {
    @Schema(description = "판매 ID", example = "sale-abc123")
    private String id;

    @Schema(description = "강의 ID", example = "course-1")
    private String courseId;

    @Schema(description = "수강생 ID", example = "student-99")
    private String studentId;

    @Schema(description = "결제 금액", example = "50000")
    private BigDecimal amount;

    @Schema(description = "결제 일시", example = "2025-04-01T10:00:00")
    private LocalDateTime paidAt;

    @Schema(description = "생성 일시", example = "2025-04-01T10:00:01")
    private LocalDateTime createdAt;
}
