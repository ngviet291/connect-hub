package com.connecthub.common.websocket;

import com.connecthub.common.websocket.event.DomainEvent;
import com.connecthub.common.websocket.handler.EventDispatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GlobalEventListener {

    private final EventDispatcher dispatcher;

    @EventListener
    public void handle(DomainEvent event) {
        dispatcher.dispatch(event);
    }
}