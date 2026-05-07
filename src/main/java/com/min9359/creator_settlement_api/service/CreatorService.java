package com.min9359.creator_settlement_api.service;

import com.min9359.creator_settlement_api.domain.Creator;
import com.min9359.creator_settlement_api.mapper.CreatorMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CreatorService {

    private final CreatorMapper creatorMapper;

    public List<Creator> getAllCreators() {
        return creatorMapper.findAll();
    }

    public Creator getCreator(String id) {
        return creatorMapper.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "크리에이터를 찾을 수 없습니다: " + id));
    }
}
