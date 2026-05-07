package com.min9359.creator_settlement_api.mapper;

import com.min9359.creator_settlement_api.domain.CancelRecord;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;
import java.util.Optional;

@Mapper
public interface CancelRecordMapper {

    void insert(CancelRecord cancelRecord);

    Optional<CancelRecord> findById(Long id);

    BigDecimal sumRefundAmountBySaleRecordId(String saleRecordId);
}