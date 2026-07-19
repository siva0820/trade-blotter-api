package com.tradeblotter.controller;

import com.tradeblotter.dto.AllocationDTO;
import com.tradeblotter.dto.TradeDTO;
import com.tradeblotter.dto.TradeUpdateRequest;
import com.tradeblotter.exception.TradeNotFoundException;
import com.tradeblotter.service.TradeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/trades")
@RequiredArgsConstructor
public class TradeController {

    private final TradeService tradeService;

    @GetMapping
    public ResponseEntity<List<TradeDTO>> getAllTrades(@RequestParam(required = false) String trader) {
        return ResponseEntity.ok(tradeService.getAllTrades(trader));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TradeDTO> getTradeById(@PathVariable Long id) {
        return ResponseEntity.ok(tradeService.getTradeById(id));
    }

    @GetMapping("/{id}/allocations")
    public ResponseEntity<List<AllocationDTO>> getAllocations(@PathVariable Long id) {
        return ResponseEntity.ok(tradeService.getAllocationsByTradeId(id));
    }

    @PostMapping
    public ResponseEntity<TradeDTO> createTrade(@Valid @RequestBody TradeUpdateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tradeService.createTrade(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TradeDTO> updateTrade(@PathVariable Long id, @Valid @RequestBody TradeUpdateRequest request) {
        return ResponseEntity.ok(tradeService.updateTrade(id, request));
    }

    @PostMapping("/{id}/execute")
    public ResponseEntity<TradeDTO> executeTrade(@PathVariable Long id) {
        return ResponseEntity.ok(tradeService.executeTrade(id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<TradeDTO> cancelTrade(@PathVariable Long id) {
        return ResponseEntity.ok(tradeService.cancelTrade(id));
    }

    @ExceptionHandler(TradeNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleTradeNotFound(TradeNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Trade data is missing required fields or violates a constraint"));
    }
}
