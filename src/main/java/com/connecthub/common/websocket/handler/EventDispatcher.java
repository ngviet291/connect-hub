package com.connecthub.common.websocket.handler;

import com.connecthub.common.websocket.event.DomainEvent;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class EventDispatcher {

    private final Map<Class<?>, EventHandler<?>> handlers;

    public EventDispatcher(List<EventHandler<?>> handlers) {
        this.handlers = handlers.stream()
                .collect(Collectors.toMap(
                        EventHandler::support,
                        Function.identity()
                ));
    }

    @SuppressWarnings("unchecked")
    public <T extends DomainEvent> void dispatch(T event) {
        EventHandler<T> handler =
                (EventHandler<T>) handlers.get(event.getClass());

        if (handler != null) {
            handler.handle(event);
        }
    }
}