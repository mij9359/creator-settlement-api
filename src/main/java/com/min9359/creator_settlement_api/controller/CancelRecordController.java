package com.min9359.creator_settlement_api.controller;

import com.min9359.creator_settlement_api.domain.CancelRecord;
import com.min9359.creator_settlement_api.dto.CancelRecordCreateRequest;
import com.min9359.creator_settlement_api.service.CancelRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "취소 내역", description = "취소조회 API")
@RestController
@RequestMapping("/api/cancels")
@RequiredArgsConstructor
public class CancelRecordController {

    private final CancelRecordService cancelRecordService;

    @Operation(summary = "취소/환불 내역 등록",
            description = "수강생의 환불 요청 시 호출합니다. 원본 판매 내역을 참조하며, 부분 환불이 가능합니다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CancelRecord registerCancel(@Valid @RequestBody CancelRecordCreateRequest request) {
        return cancelRecordService.registerCancel(request);
    }
}

