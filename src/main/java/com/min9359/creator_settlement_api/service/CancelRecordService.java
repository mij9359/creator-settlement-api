package com.min9359.creator_settlement_api.service;


import com.min9359.creator_settlement_api.domain.CancelRecord;
import com.min9359.creator_settlement_api.domain.SaleRecord;
import com.min9359.creator_settlement_api.dto.CancelRecordCreateRequest;
import com.min9359.creator_settlement_api.mapper.CancelRecordMapper;
import com.min9359.creator_settlement_api.mapper.SaleRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CancelRecordService {

    private final CancelRecordMapper cancelRecordMapper;
    private final SaleRecordMapper saleRecordMapper;

    @Transactional
    public CancelRecord registerCancel(CancelRecordCreateRequest request) {
        // 1. 원본 판매가 존재하는지 확인
        SaleRecord sale = saleRecordMapper.findById(request.getSaleRecordId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "원본 판매 내역을 찾을 수 없습니다: " + request.getSaleRecordId()));

        // 2. 취소 일시는 결제 일시 이후여야 함
        if (request.getCancelledAt().isBefore(sale.getPaidAt())) {
            throw new IllegalArgumentException(
                    "취소 일시는 결제 일시 이후여야 합니다");
        }

        // 3. 누적 환불 금액이 원결제 금액을 넘지 않는지 확인
        BigDecimal alreadyRefunded = cancelRecordMapper.sumRefundAmountBySaleRecordId(
                request.getSaleRecordId());
        BigDecimal totalAfterThis = alreadyRefunded.add(request.getRefundAmount());

        if (totalAfterThis.compareTo(sale.getAmount()) > 0) {
            throw new IllegalArgumentException(String.format(
                    "환불 금액 합계가 원 결제 금액을 초과합니다. " +
                            "원결제: %s, 기존 환불: %s, 요청 환불: %s",
                    sale.getAmount(), alreadyRefunded, request.getRefundAmount()));
        }

        // 4. 등록
        CancelRecord cancelRecord = CancelRecord.builder()
                .saleRecordId(request.getSaleRecordId())
                .refundAmount(request.getRefundAmount())
                .cancelledAt(request.getCancelledAt())
                .reason(request.getReason())
                .build();

        cancelRecordMapper.insert(cancelRecord);

        return cancelRecordMapper.findById(cancelRecord.getId())
                .orElseThrow(() -> new IllegalStateException("등록 직후 조회 실패"));
    }
}