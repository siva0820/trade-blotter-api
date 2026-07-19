package com.tradeblotter.service;

import com.tradeblotter.dto.TradeUpdateRequest;
import com.tradeblotter.model.Trade;
import com.tradeblotter.model.TradeStatus;
import com.tradeblotter.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketSimulator {

    private final TradeRepository tradeRepository;
    private final TradeService tradeService;

    @Scheduled(fixedDelay = 5000)
    public void simulatePartialFill() {
        List<Trade> pendingTrades = tradeRepository.findByStatus(TradeStatus.PENDING);
        if (pendingTrades.isEmpty()) {
            log.debug("No PENDING trades to simulate a fill for");
            return;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        Trade trade = pendingTrades.get(random.nextInt(pendingTrades.size()));

        if (trade.getQty() < 2) {
            return;
        }

        // Capped below qty so the fill is always partial, not a full execution.
        int fillQty = 1 + random.nextInt(trade.getQty() - 1);

        TradeUpdateRequest request = TradeUpdateRequest.builder()
                .filledQty(fillQty)
                .build();

        tradeService.updateTrade(trade.getId(), request);

        log.info("Market simulator partially filled trade {} ({}): {}/{} shares",
                trade.getId(), trade.getSymbol(), fillQty, trade.getQty());
    }
}
