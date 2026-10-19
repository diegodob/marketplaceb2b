package com.marketplaceb2b.repository;

import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.marketplaceb2b.model.Customer;

@Repository
public class CustomerRepository {

    private static final RowMapper<Customer> MAPPER = (rs, rowNum) -> new Customer(
            rs.getLong("id"),
            rs.getLong("tenant_id"),
            rs.getString("name"),
            rs.getString("fiscal_id"),
            rs.getLong("fiscal_id_type_id"));

    private final PersistenceAccess access;

    public CustomerRepository(PersistenceAccess access) {
        this.access = access;
    }

    /** Looks the customer up scoped by tenant, so another tenant's id never resolves. */
    public Optional<Customer> find(long tenantId, long customerId) {
        return access.queryList(
                "SELECT id, tenant_id, name, fiscal_id, fiscal_id_type_id FROM customer "
                        + "WHERE tenant_id = :tenantId AND id = :id",
                Map.of("tenantId", tenantId, "id", customerId), MAPPER).stream().findFirst();
    }
}
