package com.marketplaceb2b.persistence.repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;

import org.springframework.stereotype.Repository;

import com.marketplaceb2b.model.LedgerEntryType;
import com.marketplaceb2b.persistence.PersistenceAccess;

@Repository
public class CreditLedgerRepository {

    private final PersistenceAccess access;

    public CreditLedgerRepository(PersistenceAccess access) {
        this.access = access;
    }

    public void insert(long creditAccountId, long salesOrderId, BigDecimal amount, LedgerEntryType entryType,
                       Instant createdAt) {
        access.insert("INSERT INTO credit_ledger (credit_account_id, sales_order_id, amount, entry_type, created_at) "
                        + "VALUES (:accountId, :orderId, :amount, :entryType, :createdAt)",
                Map.of("accountId", creditAccountId, "orderId", salesOrderId, "amount", amount,
                        "entryType", entryType.name(), "createdAt", Timestamp.from(createdAt)));
    }
}
