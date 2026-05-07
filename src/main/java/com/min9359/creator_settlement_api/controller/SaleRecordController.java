package com.min9359.creator_settlement_api.controller;

import com.min9359.creator_settlement_api.domain.SaleRecord;
import com.min9359.creator_settlement_api.dto.SaleRecordCreateRequest;
import com.min9359.creator_settlement_api.service.SaleRecordService;
import io.swagger.v3.oas.annotations.Operation;
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
            description = "새로운 판매를 등록합니다. ID는 서버에서 자동 생성됩니다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SaleRecord registerSale(@Valid @RequestBody SaleRecordCreateRequest request) {
        return saleRecordService.registerSale(request);
    }


    @Operation(summary = "판매 내역 목록 조회",
            description = "크리에이터별 판매 목록 조회. 기간 필터는 선택입니다.")
    @GetMapping
    public List<SaleRecord> getSales(
            @RequestParam String creatorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startAt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endAt) {
        return saleRecordService.getSalesByCreatorAndPeriod(creatorId, startAt, endAt);
    }

}
