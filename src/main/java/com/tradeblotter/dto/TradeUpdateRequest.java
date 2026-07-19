package com.tradeblotter.dto;

import com.tradeblotter.model.TradeStatus;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

// All fields optional: doubles as both the create body (POST) and partial-update body (PATCH).
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeUpdateRequest {

    private String symbol;

    private String side;

    @Min(1)
    private Integer qty;

    @Min(0)
    private Integer filledQty;

    private BigDecimal price;

    private TradeStatus status;

    private String trader;

    private String orderType;
}
