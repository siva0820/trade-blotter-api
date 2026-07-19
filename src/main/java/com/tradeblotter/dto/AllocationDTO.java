package com.tradeblotter.dto;

import com.tradeblotter.model.TradeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AllocationDTO {
    private Long id;
    private Long tradeId;
    private String accountId;
    private String fundName;
    private Integer shares;
    private Integer filledShares;
    private TradeStatus status;
}
