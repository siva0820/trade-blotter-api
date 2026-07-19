package com.tradeblotter.config;

import com.tradeblotter.model.Allocation;
import com.tradeblotter.model.Trade;
import com.tradeblotter.model.TradeStatus;
import com.tradeblotter.repository.AllocationRepository;
import com.tradeblotter.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private static final String[] SYMBOLS = {
            "AAPL", "MSFT", "GOOGL", "AMZN", "TSLA",
            "NVDA", "META", "NFLX", "JPM", "BAC",
            "GS", "V", "MA", "DIS", "INTC"
    };

    private static final String[] TRADERS = {"jsmith", "kwong", "mrivera", "achen", "tokafor"};
    private static final String[] ORDER_TYPES = {"MARKET", "LIMIT"};
    private static final String[] FUNDS = {"Growth Fund", "Income Fund", "Global Equity Fund", "Small Cap Fund"};
    private static final TradeStatus[] STATUS_CYCLE = {
            TradeStatus.PENDING, TradeStatus.PARTIAL, TradeStatus.EXECUTED, TradeStatus.CANCELLED
    };

    private final TradeRepository tradeRepository;
    private final AllocationRepository allocationRepository;

    @Override
    public void run(String... args) {
        if (tradeRepository.count() > 0) {
            log.info("Trades already present, skipping seed");
            return;
        }

        for (int i = 0; i < SYMBOLS.length; i++) {
            int qty = 1000 + (i * 250);
            TradeStatus status = STATUS_CYCLE[i % STATUS_CYCLE.length];
            int filledQty = filledQtyFor(status, qty);

            Trade trade = Trade.builder()
                    .symbol(SYMBOLS[i])
                    .side(i % 2 == 0 ? "BUY" : "SELL")
                    .qty(qty)
                    .filledQty(filledQty)
                    .price(BigDecimal.valueOf(50 + (i * 12.37)).setScale(2, java.math.RoundingMode.HALF_UP))
                    .status(status)
                    .trader(TRADERS[i % TRADERS.length])
                    .orderType(ORDER_TYPES[i % ORDER_TYPES.length])
                    .build();

            Trade saved = tradeRepository.save(trade);
            seedAllocations(saved);
        }

        log.info("Seeded {} trades with allocations", SYMBOLS.length);
    }

    private void seedAllocations(Trade trade) {
        int qty = trade.getQty();
        int filledQty = trade.getFilledQty();

        int firstShares = qty / 2;
        int secondShares = qty - firstShares;
        int firstFilled = Math.min(filledQty, firstShares);
        int secondFilled = Math.min(filledQty - firstFilled, secondShares);

        TradeStatus firstStatus = trade.getStatus() == TradeStatus.CANCELLED
                ? TradeStatus.CANCELLED : deriveStatus(firstShares, firstFilled);
        TradeStatus secondStatus = trade.getStatus() == TradeStatus.CANCELLED
                ? TradeStatus.CANCELLED : deriveStatus(secondShares, secondFilled);

        List<Allocation> allocations = List.of(
                Allocation.builder()
                        .trade(trade)
                        .accountId("ACC-" + trade.getId() + "-1")
                        .fundName(FUNDS[trade.getId().intValue() % FUNDS.length])
                        .shares(firstShares)
                        .filledShares(firstFilled)
                        .status(firstStatus)
                        .build(),
                Allocation.builder()
                        .trade(trade)
                        .accountId("ACC-" + trade.getId() + "-2")
                        .fundName(FUNDS[(trade.getId().intValue() + 1) % FUNDS.length])
                        .shares(secondShares)
                        .filledShares(secondFilled)
                        .status(secondStatus)
                        .build()
        );

        allocationRepository.saveAll(allocations);
    }

    private int filledQtyFor(TradeStatus status, int qty) {
        return switch (status) {
            case PENDING -> 0;
            case PARTIAL -> qty / 3;
            case EXECUTED -> qty;
            case CANCELLED -> qty / 4;
        };
    }

    private TradeStatus deriveStatus(int qty, int filledQty) {
        if (filledQty <= 0) {
            return TradeStatus.PENDING;
        } else if (filledQty < qty) {
            return TradeStatus.PARTIAL;
        } else {
            return TradeStatus.EXECUTED;
        }
    }
}
