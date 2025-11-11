package io.store.ua.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum WebSocketTopic {
    STOCK_ITEM_OUT_OF_STOCK("/stock_items");

    @Getter
    private final String topic;
}
