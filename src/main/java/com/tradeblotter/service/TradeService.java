package com.tradeblotter.service;

import com.tradeblotter.dto.AllocationDTO;
import com.tradeblotter.dto.TradeDTO;
import com.tradeblotter.dto.TradeUpdateRequest;
import com.tradeblotter.event.TradeEvent;
import com.tradeblotter.exception.TradeNotFoundException;
import com.tradeblotter.model.Allocation;
import com.tradeblotter.model.Trade;
import com.tradeblotter.model.TradeStatus;
import com.tradeblotter.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TradeService {

    private final TradeRepository tradeRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<TradeDTO> getAllTrades(String trader) {
        List<Trade> trades = trader == null
                ? tradeRepository.findAll()
                : tradeRepository.findByTrader(trader);
        return trades.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public TradeDTO getTradeById(Long id) {
        return toDto(findTradeOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<AllocationDTO> getAllocationsByTradeId(Long tradeId) {
        Trade trade = findTradeOrThrow(tradeId);
        return trade.getAllocations().stream()
                .map(this::mapToAllocationDTO)
                .toList();
    }

    public TradeDTO createTrade(TradeUpdateRequest req) {
        Trade trade = Trade.builder()
                .symbol(req.getSymbol())
                .side(req.getSide())
                .qty(req.getQty())
                .filledQty(req.getFilledQty() != null ? req.getFilledQty() : 0)
                .price(req.getPrice())
                .status(req.getStatus() != null ? req.getStatus() : TradeStatus.PENDING)
                .trader(req.getTrader())
                .orderType(req.getOrderType())
                .build();

        Trade saved = tradeRepository.save(trade);
        TradeDTO dto = toDto(saved);
        eventPublisher.publishEvent(new TradeEvent(this, "TRADE_NEW", dto));
        return dto;
    }

    public TradeDTO updateTrade(Long id, TradeUpdateRequest req) {
        Trade trade = findTradeOrThrow(id);

        if (req.getSymbol() != null) {
            trade.setSymbol(req.getSymbol());
        }
        if (req.getSide() != null) {
            trade.setSide(req.getSide());
        }
        if (req.getQty() != null) {
            trade.setQty(req.getQty());
        }
        if (req.getTrader() != null) {
            trade.setTrader(req.getTrader());
        }
        if (req.getOrderType() != null) {
            trade.setOrderType(req.getOrderType());
        }
        if (req.getPrice() != null) {
            trade.setPrice(req.getPrice());
        }
        if (req.getFilledQty() != null) {
            trade.setFilledQty(req.getFilledQty());
        }

        if (req.getStatus() != null) {
            trade.setStatus(req.getStatus());
        } else if (req.getFilledQty() != null) {
            trade.setStatus(deriveStatus(trade.getQty(), trade.getFilledQty()));
        }

        Trade saved = tradeRepository.save(trade);
        TradeDTO dto = toDto(saved);
        eventPublisher.publishEvent(new TradeEvent(this, eventTypeFor(saved.getStatus()), dto));
        return dto;
    }

    public TradeDTO executeTrade(Long id) {
        Trade trade = findTradeOrThrow(id);
        trade.setStatus(TradeStatus.EXECUTED);
        trade.setFilledQty(trade.getQty());

        Trade saved = tradeRepository.save(trade);
        TradeDTO dto = toDto(saved);
        eventPublisher.publishEvent(new TradeEvent(this, "TRADE_EXECUTED", dto));
        return dto;
    }

    public TradeDTO cancelTrade(Long id) {
        Trade trade = findTradeOrThrow(id);
        trade.setStatus(TradeStatus.CANCELLED);

        Trade saved = tradeRepository.save(trade);
        TradeDTO dto = toDto(saved);
        eventPublisher.publishEvent(new TradeEvent(this, "TRADE_CANCELLED", dto));
        return dto;
    }

    // Lets updateTrade's SSE event reflect the trade's resulting status (e.g. a fill that
    // lands on PARTIAL announces itself as TRADE_PARTIAL_FILL, not a generic TRADE_UPDATED),
    // matching the event names executeTrade/cancelTrade use for the same status transitions.
    private String eventTypeFor(TradeStatus status) {
        return switch (status) {
            case PARTIAL -> "TRADE_PARTIAL_FILL";
            case EXECUTED -> "TRADE_EXECUTED";
            case CANCELLED -> "TRADE_CANCELLED";
            case PENDING -> "TRADE_UPDATED";
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

    private Trade findTradeOrThrow(Long id) {
        return tradeRepository.findById(id)
                .orElseThrow(() -> new TradeNotFoundException(id));
    }

    private TradeDTO toDto(Trade trade) {
        List<AllocationDTO> allocations = trade.getAllocations().stream()
                .map(this::mapToAllocationDTO)
                .toList();

        return TradeDTO.builder()
                .id(trade.getId())
                .symbol(trade.getSymbol())
                .side(trade.getSide())
                .qty(trade.getQty())
                .filledQty(trade.getFilledQty())
                .price(trade.getPrice())
                .status(trade.getStatus())
                .trader(trade.getTrader())
                .orderType(trade.getOrderType())
                .createdAt(trade.getCreatedAt())
                .updatedAt(trade.getUpdatedAt())
                .allocations(allocations)
                .build();
    }

    private AllocationDTO mapToAllocationDTO(Allocation allocation) {
        return AllocationDTO.builder()
                .id(allocation.getId())
                .tradeId(allocation.getTrade().getId())
                .accountId(allocation.getAccountId())
                .fundName(allocation.getFundName())
                .shares(allocation.getShares())
                .filledShares(allocation.getFilledShares())
                .status(allocation.getStatus())
                .build();
    }
}
