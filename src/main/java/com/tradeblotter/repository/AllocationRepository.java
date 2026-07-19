package com.tradeblotter.repository;

import com.tradeblotter.model.Allocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AllocationRepository extends JpaRepository<Allocation, Long> {

    List<Allocation> findByTradeId(Long tradeId);
}
