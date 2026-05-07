package com.min9359.creator_settlement_api.service;

import com.min9359.creator_settlement_api.domain.SaleRecord;
import com.min9359.creator_settlement_api.dto.SaleRecordCreateRequest;
import com.min9359.creator_settlement_api.mapper.SaleRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SaleRecordService {
    private final SaleRecordMapper saleRecordMapper;

    public SaleRecord registerSale(SaleRecordCreateRequest request) {
        String id = (request.getId() != null && !request.getId().isBlank())
                ? request.getId()
                : "sale-" + UUID.randomUUID();

        SaleRecord saleRecord = SaleRecord.builder()
                .id(id)
                .courseId(request.getCourseId())
                .studentId(request.getStudentId())
                .amount(request.getAmount())
                .paidAt(request.getPaidAt())
                .build();

        saleRecordMapper.insert(saleRecord);

        return saleRecordMapper.findById(id)
                .orElseThrow(() -> new IllegalStateException("등록 직후 조회 실패"));
    }

    public List<SaleRecord> getSalesByCreatorAndPeriod(
            String creatorId, LocalDateTime startAt, LocalDateTime endAt) {
        return saleRecordMapper.findByCreatorAndPeriod(creatorId, startAt, endAt);
    }
}
