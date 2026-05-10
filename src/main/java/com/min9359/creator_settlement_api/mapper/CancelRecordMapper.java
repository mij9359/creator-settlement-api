package com.min9359.creator_settlement_api.mapper;

import com.min9359.creator_settlement_api.domain.CancelAggregation;
import com.min9359.creator_settlement_api.domain.CancelRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Mapper
public interface CancelRecordMapper {

    void insert(CancelRecord cancelRecord);

    Optional<CancelRecord> findById(Long id);

    BigDecimal sumRefundAmountBySaleRecordId(String saleRecordId);

    CancelAggregation aggregateByCreatorAndPeriod(
            @Param("creatorId") String creatorId,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAtExclusive") LocalDateTime endAtExclusive);
}