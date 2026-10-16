package com.marketplaceb2b.repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;

import org.springframework.stereotype.Repository;

import com.marketplaceb2b.model.OutboxDestination;
import com.marketplaceb2b.model.OutboxEventType;

@Repository
public class OutboxRepository {

    private final PersistenceAccess access;

    public OutboxRepository(PersistenceAccess access) {
        this.access = access;
    }

    public void insertPending(long salesOrderId, OutboxDestination destination, OutboxEventType eventType,
                              Instant createdAt) {
        access.insert("INSERT INTO outbox (sales_order_id, destination, event_type, payload, status, attempts, created_at) "
                        + "VALUES (:orderId, :destination, :eventType, '{}', 'PENDING', 0, :createdAt)",
                Map.of("orderId", salesOrderId, "destination", destination.name(), "eventType", eventType.name(),
                        "createdAt", Timestamp.from(createdAt)));
    }
}
