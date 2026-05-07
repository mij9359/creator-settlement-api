package com.min9359.creator_settlement_api.controller;

import com.min9359.creator_settlement_api.domain.CancelRecord;
import com.min9359.creator_settlement_api.dto.CancelRecordCreateRequest;
import com.min9359.creator_settlement_api.service.CancelRecordService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cancels")
@RequiredArgsConstructor
public class CancelRecordController {

    private final CancelRecordService cancelRecordService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CancelRecord registerCancel(@Valid @RequestBody CancelRecordCreateRequest request) {
        return cancelRecordService.registerCancel(request);
    }
}

