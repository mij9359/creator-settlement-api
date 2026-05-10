package com.min9359.creator_settlement_api.mapper;

import com.min9359.creator_settlement_api.domain.CancelAggregation;
import com.min9359.creator_settlement_api.domain.CancelRecord;
import com.min9359.creator_settlement_api.domain.CreatorAggregation;
import com.min9359.creator_settlement_api.domain.SettlementResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Mapper
public interface SettlementMapper {

    List<CreatorAggregation> aggregateAllByPeriod(
            @Param("startAt") LocalDateTime startAt,
            @Param("endAtExclusive") LocalDateTime endAtExclusive
    );

    // 이미 저장된 정산 내역이 있는지 확인
    Optional<SettlementResponse> findSavedSettlement(
            @Param("creatorId") String creatorId,
            @Param("yearMonth") String yearMonth);

    // 정산 결과 저장
    void insertSettlement(SettlementResponse settlement);

    // 특정 정산 건의 상태를 업데이트
    void updateStatus(@Param("id") Long id, @Param("status") String status);

    // ID로 정산 내역 조회 (상태 변경 전 검증용)
    Optional<SettlementResponse> findById(Long id);


}