package com.connecthub.common.websocket.handler;

import com.connecthub.common.websocket.event.DomainEvent;

public interface EventHandler<T extends DomainEvent> {

    Class<T> support();

    void handle(T event);
}