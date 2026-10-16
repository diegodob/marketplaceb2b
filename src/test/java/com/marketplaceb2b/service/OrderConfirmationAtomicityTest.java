package com.marketplaceb2b.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.marketplaceb2b.repository.OutboxRepository;
import com.marketplaceb2b.support.AbstractIntegrationTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class OrderConfirmationAtomicityTest extends AbstractIntegrationTest {

    @Autowired
    private OrderConfirmationService service;

    @MockitoBean
    private OutboxRepository outbox;

    @BeforeEach
    void failOnOutbox() {
        doThrow(new IllegalStateException("outbox unavailable"))
                .when(outbox).insertPending(anyLong(), any(), any(), any());
    }

    @Test
    void partialFailureLeavesNothingBehind() {
        long tenant = gtTenantId();
        long customer = gtCustomerId();
        long quote = newQuote(tenant, customer, gtRulesetId(), paymentMethodId("CREDIT"), "1000.00",
                Instant.now().plusSeconds(3600));

        assertThatThrownBy(() -> service.confirm(tenant, quote, unique("KEY")))
                .isInstanceOf(IllegalStateException.class);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sales_order WHERE quote_id = ?", Integer.class, quote))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM credit_ledger WHERE sales_order_id IN "
                + "(SELECT id FROM sales_order WHERE quote_id = ?)", Integer.class, quote)).isZero();
        assertThat(jdbc.queryForObject("SELECT available FROM credit_account WHERE customer_id = ?", BigDecimal.class,
                customer)).isEqualByComparingTo("300000.00");
    }
}
