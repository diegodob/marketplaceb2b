package com.marketplaceb2b.repository;

import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.marketplaceb2b.model.PaymentMethod;

@Repository
public class PaymentMethodRepository {

    private static final RowMapper<PaymentMethod> MAPPER = (rs, rowNum) -> new PaymentMethod(
            rs.getLong("id"),
            rs.getLong("tenant_id"),
            rs.getString("code"),
            rs.getString("name"),
            rs.getBoolean("uses_credit"));

    private final PersistenceAccess access;

    public PaymentMethodRepository(PersistenceAccess access) {
        this.access = access;
    }

    public Optional<PaymentMethod> find(long paymentMethodId) {
        return access.queryList(
                "SELECT id, tenant_id, code, name, uses_credit FROM payment_method WHERE id = :id",
                Map.of("id", paymentMethodId), MAPPER).stream().findFirst();
    }
}
