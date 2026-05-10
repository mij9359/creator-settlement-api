package com.min9359.creator_settlement_api.mapper;

import com.min9359.creator_settlement_api.domain.SaleRecord;
import com.min9359.creator_settlement_api.domain.SaleAggregation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Mapper
public interface SaleRecordMapper {

    void insert(SaleRecord saleRecord);

    Optional<SaleRecord> findById(String id);

    List<SaleRecord> findByCreatorAndPeriod(
            @Param("creatorId") String creatorId,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAtExclusive") LocalDateTime endAtExclusive);

    SaleAggregation aggregateByCreatorAndPeriod(
            @Param("creatorId") String creatorId,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAtExclusive") LocalDateTime endAtExclusive);


}
