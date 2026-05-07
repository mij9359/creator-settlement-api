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
@Schema(description = "취소(환불) 등록 요청")
public class CancelRecordCreateRequest {

    @Schema(description = "원본 판매 ID", example = "sale-1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "원본 판매 ID는 필수입니다")
    private String saleRecordId;

    @Schema(description = "환불 금액 (원결제 금액 이하, 누적 환불도 원결제 이하)",
            example = "30000",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "환불 금액은 필수입니다")
    @Positive(message = "환불 금액은 0보다 커야 합니다")
    private BigDecimal refundAmount;

    @Schema(description = "취소 일시 (ISO-8601, 결제 일시 이후여야 함)",
            example = "2025-04-05T15:00:00",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "취소 일시는 필수입니다")
    private LocalDateTime cancelledAt;

    @Schema(description = "취소 사유 (선택)", example = "고객 요청")
    private String reason;
}
