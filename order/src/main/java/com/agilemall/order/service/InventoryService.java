package com.agilemall.order.service;

import com.agilemall.order.events.OrderCreatedEvent;

public interface InventoryService {
    boolean isValidInventory(OrderCreatedEvent event);
}
