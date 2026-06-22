package com.connecthub.common.websocket;

import com.connecthub.common.websocket.event.DomainEvent;
import com.connecthub.common.websocket.handler.EventDispatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class GlobalEventListener {

    private final EventDispatcher dispatcher;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(DomainEvent event) {
        dispatcher.dispatch(event);
    }
}