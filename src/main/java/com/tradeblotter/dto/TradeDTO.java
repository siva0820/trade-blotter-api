package com.tradeblotter.dto;

import com.tradeblotter.model.TradeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeDTO {
    private Long id;
    private String symbol;
    private String side;
    private Integer qty;
    private Integer filledQty;
    private BigDecimal price;
    private TradeStatus status;
    private String trader;
    private String orderType;
    private Instant createdAt;
    private Instant updatedAt;
    private List<AllocationDTO> allocations;
}
