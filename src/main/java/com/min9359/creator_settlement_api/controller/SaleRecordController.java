package com.min9359.creator_settlement_api.controller;

import com.min9359.creator_settlement_api.domain.SaleRecord;
import com.min9359.creator_settlement_api.dto.SaleRecordCreateRequest;
import com.min9359.creator_settlement_api.service.SaleRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "판매 내역", description = "판매 등록/조회 API")
@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SaleRecordController {

    private final SaleRecordService saleRecordService;

    @Operation(summary = "판매 내역 등록",
            description = "강의 판매가 발생했을 때 호출합니다. 외부 결제 시스템과의 연동을 가정하여 결제 정보를 저장합니다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SaleRecord registerSale(@Valid @RequestBody SaleRecordCreateRequest request) {
        return saleRecordService.registerSale(request);
    }


    @Operation(summary = "판매 내역 목록 조회",
            description = "특정 크리에이터의 판매 내역을 조회합니다. 시작일과 종료일 파라미터를 통해 기간 필터링이 가능합니다.")
    @GetMapping
    public List<SaleRecord> getSales(
            @Parameter(description = "크리에이터 ID", example = "creator-1")
            @RequestParam String creatorId,

            @Parameter(description = "조회 시작 일시 (ISO-8601 형식)", example = "2025-03-01T00:00:00")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startAt,

            @Parameter(description = "조회 종료 일시 (미만 기준, ISO-8601 형식)", example = "2025-04-01T00:00:00")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endAtExclusive) {
        return saleRecordService.getSalesByCreatorAndPeriod(creatorId, startAt, endAtExclusive);
    }

}
