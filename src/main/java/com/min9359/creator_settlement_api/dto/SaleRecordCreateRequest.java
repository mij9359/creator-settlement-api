package com.min9359.creator_settlement_api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "판매 등록 요청")
public class SaleRecordCreateRequest {

    @Schema(hidden = true)
    private String id;

    @Schema(description = "강의 ID", example = "course-1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "강의 ID는 필수입니다")
    private String courseId;

    @Schema(description = "수강생 ID", example = "student-99", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "수강생 ID는 필수입니다")
    private String studentId;

    @Schema(description = "결제 금액 (원)", example = "50000", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "결제 금액은 필수입니다")
    @Positive(message = "결제 금액은 0보다 커야 합니다")
    private BigDecimal amount;

    @Schema(description = "결제 일시 (ISO-8601, KST)", example = "2025-04-01T10:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "결제 일시는 필수입니다")
    private LocalDateTime paidAt;

}
