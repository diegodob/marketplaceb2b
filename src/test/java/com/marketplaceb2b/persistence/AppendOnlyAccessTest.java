package com.marketplaceb2b.persistence;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AppendOnlyAccessTest {

    @Test
    void guardRejectsUpdateAndDeleteOnAppendOnlyTables() {
        assertThatThrownBy(() -> PersistenceAccess.assertNotMutatingAppendOnly("UPDATE credit_ledger SET amount = 0"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> PersistenceAccess.assertNotMutatingAppendOnly("DELETE FROM quote WHERE id = 1"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> PersistenceAccess.assertNotMutatingAppendOnly("update ruleset set currency = 'USD'"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> PersistenceAccess.assertNotMutatingAppendOnly("delete from quote_line where id = 1"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void guardAllowsInsertAndSelect() {
        assertThatCode(() -> PersistenceAccess.assertNotMutatingAppendOnly(
                "INSERT INTO credit_ledger (credit_account_id) VALUES (1)")).doesNotThrowAnyException();
        assertThatCode(() -> PersistenceAccess.assertNotMutatingAppendOnly("SELECT * FROM quote"))
                .doesNotThrowAnyException();
    }
}
