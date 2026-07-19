package com.tradeblotter.repository;

import com.tradeblotter.model.Trade;
import com.tradeblotter.model.TradeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TradeRepository extends JpaRepository<Trade, Long> {

    List<Trade> findByTrader(String trader);

    List<Trade> findByStatus(TradeStatus status);
}
