package io.store.ua.models.data;

import io.store.ua.entity.StockItem;

import java.util.List;

public record StockItemVersionGroup(StockItem baseVersion, List<StockItem> otherVersions) {
}
