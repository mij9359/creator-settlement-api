package com.min9359.creator_settlement_api.controller;

import com.min9359.creator_settlement_api.domain.Creator;
import com.min9359.creator_settlement_api.service.CreatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "개인 판매 내역", description = "개인 판매 조회 API")
@RestController
@RequestMapping("/api/creators")
@RequiredArgsConstructor
public class CreatorController {

    private final CreatorService creatorService;

    @Operation(summary = "전체 크리에이터 목록 조회",
            description = "시스템에 등록된 모든 크리에이터의 정보를 조회합니다.")
    @GetMapping
    public List<Creator> getAllCreators() {
        return creatorService.getAllCreators();
    }

    @Operation(summary = "특정 크리에이터 상세 조회",
            description = "ID를 통해 특정 크리에이터의 이름 및 가입일 등의 상세 정보를 조회합니다.")
    @GetMapping("/{id}")
    public Creator getCreator(
            @Parameter(description = "조회할 크리에이터의 고유 ID", example = "creator-1")
            @PathVariable String id) {
        return creatorService.getCreator(id);
    }

}
