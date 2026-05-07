package com.min9359.creator_settlement_api.controller;

import com.min9359.creator_settlement_api.domain.Creator;
import com.min9359.creator_settlement_api.service.CreatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/creators")
@RequiredArgsConstructor
public class CreatroController {

    private final CreatorService creatorService;

    @GetMapping
    public List<Creator> getAllCreators() {
        return creatorService.getAllCreators();
    }

    @GetMapping("/{id}")
    public Creator getCreator(@PathVariable String id) {
        return creatorService.getCreator(id);
    }

}
