package com.tradeblotter.controller;

import com.tradeblotter.dto.TradeDTO;
import com.tradeblotter.dto.TradeEventDTO;
import com.tradeblotter.model.TradeStatus;
import com.tradeblotter.service.SseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/api/trades/stream")
@RequiredArgsConstructor
@CrossOrigin(originPatterns = {"http://localhost:5173", "https://*.vercel.app"}, allowCredentials = "true")
public class SseController {

    private static final List<String> EVENT_TYPES = List.of(
            "TRADE_PARTIAL_FILL", "TRADE_EXECUTED", "TRADE_CANCELLED", "TRADE_NEW"
    );
    private static final String[] SYMBOLS = {"AAPL", "MSFT", "GOOGL", "AMZN", "TSLA", "NVDA"};
    private static final String[] TRADERS = {"jsmith", "kwong", "mrivera", "achen", "tokafor"};
    private static final String[] ORDER_TYPES = {"MARKET", "LIMIT"};

    private final SseService sseService;

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam String clientId) {
        return sseService.subscribe(clientId);
    }

    @GetMapping("/simulate")
    public TradeEventDTO simulate() {
        TradeEventDTO event = randomTradeEvent();
        sseService.publishTradeEvent(event);
        return event;
    }

    private TradeEventDTO randomTradeEvent() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        String eventType = EVENT_TYPES.get(random.nextInt(EVENT_TYPES.size()));
        int qty = 100 + random.nextInt(20) * 100;

        int filledQty = switch (eventType) {
            case "TRADE_NEW" -> 0;
            case "TRADE_PARTIAL_FILL" -> qty / 3 + random.nextInt(qty / 3);
            case "TRADE_EXECUTED" -> qty;
            case "TRADE_CANCELLED" -> random.nextInt(qty / 2);
            default -> 0;
        };

        TradeStatus status = switch (eventType) {
            case "TRADE_NEW" -> TradeStatus.PENDING;
            case "TRADE_PARTIAL_FILL" -> TradeStatus.PARTIAL;
            case "TRADE_EXECUTED" -> TradeStatus.EXECUTED;
            case "TRADE_CANCELLED" -> TradeStatus.CANCELLED;
            default -> TradeStatus.PENDING;
        };

        Instant now = Instant.now();
        TradeDTO trade = TradeDTO.builder()
                .id(random.nextLong(1, 100_000))
                .symbol(SYMBOLS[random.nextInt(SYMBOLS.length)])
                .side(random.nextBoolean() ? "BUY" : "SELL")
                .qty(qty)
                .filledQty(filledQty)
                .price(BigDecimal.valueOf(50 + random.nextDouble(0, 450)).setScale(2, RoundingMode.HALF_UP))
                .status(status)
                .trader(TRADERS[random.nextInt(TRADERS.length)])
                .orderType(ORDER_TYPES[random.nextInt(ORDER_TYPES.length)])
                .createdAt(now)
                .updatedAt(now)
                .allocations(List.of())
                .build();

        return TradeEventDTO.builder()
                .eventType(eventType)
                .trade(trade)
                .timestamp(now)
                .build();
    }
}
