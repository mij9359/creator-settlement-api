package com.min9359.creator_settlement_api.mapper;

import com.min9359.creator_settlement_api.domain.Creator;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

@Mapper
public interface CreatorMapper {

    List<Creator> findAll();

    Optional<Creator> findById(String id);
}
