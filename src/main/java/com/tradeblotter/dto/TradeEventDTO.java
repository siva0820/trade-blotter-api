package com.tradeblotter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeEventDTO {
    private String eventType;
    private TradeDTO trade;
    private Instant timestamp;
}
