package com.min9359.creator_settlement_api.mapper;

import com.min9359.creator_settlement_api.domain.SaleRecord;
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
            @Param("endAt") LocalDateTime endAt);

}
