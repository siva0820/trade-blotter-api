package com.tradeblotter.service;

import com.tradeblotter.dto.TradeEventDTO;
import com.tradeblotter.event.TradeEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class SseService {

    private static final long EMITTER_TIMEOUT_MS = 5 * 60 * 1000L;

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final SseService self;

    public SseService(@Lazy SseService self) {
        this.self = self;
    }

    public SseEmitter subscribe(String clientId) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        emitters.put(clientId, emitter);

        emitter.onCompletion(() -> {
            emitters.remove(clientId, emitter);
            log.debug("SSE completed for client {}", clientId);
        });
        emitter.onTimeout(() -> {
            emitters.remove(clientId, emitter);
            emitter.complete();
            log.debug("SSE timed out for client {}", clientId);
        });
        emitter.onError(ex -> {
            emitters.remove(clientId, emitter);
            log.debug("SSE error for client {}: {}", clientId, ex.getMessage());
        });

        try {
            emitter.send(SseEmitter.event().name("CONNECTED").data("connected", MediaType.TEXT_PLAIN));
        } catch (IOException e) {
            emitters.remove(clientId, emitter);
        }

        log.info("Client {} subscribed, {} clients connected", clientId, emitters.size());
        return emitter;
    }

    // AFTER_COMMIT (the default phase) so subscribers only see trades that
    // actually persisted - a plain @EventListener fires before the enclosing
    // @Transactional method commits, which could broadcast a trade that then rolls back.
    @TransactionalEventListener
    public void onTradeEvent(TradeEvent event) {
        TradeEventDTO payload = TradeEventDTO.builder()
                .eventType(event.getEventType())
                .trade(event.getTrade())
                .timestamp(event.getOccurredAt())
                .build();
        // Route through the proxy (self) so @Async actually applies -
        // a direct call here would be a self-invocation that bypasses the proxy.
        self.publishTradeEvent(payload);
    }

    @Async
    public void publishTradeEvent(TradeEventDTO event) {
        if (emitters.isEmpty()) {
            return;
        }

        List<String> deadClients = new CopyOnWriteArrayList<>();

        emitters.forEach((clientId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name(event.getEventType())
                        .data(event, MediaType.APPLICATION_JSON));
            } catch (IOException | IllegalStateException e) {
                deadClients.add(clientId);
            }
        });

        deadClients.forEach(emitters::remove);

        log.debug("Published {} to {} clients ({} dead removed)",
                event.getEventType(), emitters.size(), deadClients.size());
    }
}
