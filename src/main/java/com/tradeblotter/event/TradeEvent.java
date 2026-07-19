package com.tradeblotter.event;

import com.tradeblotter.dto.TradeDTO;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.Instant;

@Getter
public class TradeEvent extends ApplicationEvent {

    private final String eventType;
    private final TradeDTO trade;
    private final Instant occurredAt;

    public TradeEvent(Object source, String eventType, TradeDTO trade) {
        super(source);
        this.eventType = eventType;
        this.trade = trade;
        this.occurredAt = Instant.now();
    }
}
