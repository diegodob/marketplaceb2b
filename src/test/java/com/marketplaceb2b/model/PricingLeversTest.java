package com.marketplaceb2b.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PricingLeversTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private static ScaleTier tier(int fromQty, String percentage) {
        return new ScaleTier(fromQty, new BigDecimal(percentage));
    }

    private static ScaleDiscount scale(DiscountTargetType type, Long productId, Long familyId, List<ScaleTier> tiers) {
        return new ScaleDiscount(1L, 1L, type, productId, familyId, tiers);
    }

    private static ComboItem item(long productId, int minQty) {
        return new ComboItem(productId, minQty);
    }

    private static Combo combo(List<ComboItem> items, String price) {
        return new Combo(1L, 1L, "pack", items, new BigDecimal(price));
    }

    private static GiftCondition condition(Long productId, Long familyId, Integer minQty, String minAmount) {
        return new GiftCondition(productId, familyId, minQty, minAmount == null ? null : new BigDecimal(minAmount));
    }

    @Test
    void validScaleHasNoViolations() {
        assertThat(validator.validate(scale(DiscountTargetType.SKU, 10L, null,
                List.of(tier(1, "0"), tier(10, "10"))))).isEmpty();
        assertThat(validator.validate(scale(DiscountTargetType.FAMILY, null, 7L, List.of(tier(1, "5"))))).isEmpty();
    }

    @Test
    void scaleRejectsNonPositiveTierStart() {
        assertThat(validator.validate(tier(0, "10"))).isNotEmpty();
        assertThat(validator.validate(scale(DiscountTargetType.SKU, 10L, null, List.of(tier(0, "10"))))).isNotEmpty();
    }

    @Test
    void scaleRejectsOutOfRangePercentage() {
        assertThat(validator.validate(tier(1, "150"))).isNotEmpty();
    }

    @Test
    void scaleRejectsEmptyTiers() {
        assertThat(validator.validate(scale(DiscountTargetType.SKU, 10L, null, List.of()))).isNotEmpty();
    }

    @Test
    void scaleRejectsInconsistentTarget() {
        assertThat(validator.validate(scale(DiscountTargetType.SKU, null, null, List.of(tier(1, "5"))))).isNotEmpty();
        assertThat(validator.validate(scale(DiscountTargetType.FAMILY, 10L, 7L, List.of(tier(1, "5"))))).isNotEmpty();
    }

    @Test
    void scaleRejectsNonIncreasingTiers() {
        assertThat(validator.validate(scale(DiscountTargetType.SKU, 10L, null,
                List.of(tier(10, "5"), tier(10, "10"))))).isNotEmpty();
        assertThat(validator.validate(scale(DiscountTargetType.SKU, 10L, null,
                List.of(tier(10, "5"), tier(5, "10"))))).isNotEmpty();
    }

    @Test
    void validComboHasNoViolations() {
        assertThat(validator.validate(combo(List.of(item(1L, 2), item(2L, 1)), "100.00"))).isEmpty();
    }

    @Test
    void comboRejectsEmptyItems() {
        assertThat(validator.validate(combo(List.of(), "100.00"))).isNotEmpty();
    }

    @Test
    void comboRejectsNonPositiveMinQty() {
        assertThat(validator.validate(combo(List.of(item(1L, 0)), "100.00"))).isNotEmpty();
    }

    @Test
    void comboRejectsNegativePrice() {
        assertThat(validator.validate(combo(List.of(item(1L, 1)), "-1"))).isNotEmpty();
    }

    @Test
    void validGiftConditionsHaveNoViolations() {
        assertThat(validator.validate(new Gift(1L, 1L, condition(5L, null, 3, null), 9L, 1))).isEmpty();
        assertThat(validator.validate(new Gift(1L, 1L, condition(null, null, null, "1000.00"), 9L, 1))).isEmpty();
        assertThat(validator.validate(new Gift(1L, 1L, condition(5L, null, 3, "1000.00"), 9L, 1))).isEmpty();
    }

    @Test
    void giftConditionRejectsMissingThreshold() {
        assertThat(validator.validate(condition(null, null, null, null))).isNotEmpty();
    }

    @Test
    void giftConditionRejectsQuantityWithoutTarget() {
        assertThat(validator.validate(condition(null, null, 3, null))).isNotEmpty();
    }

    @Test
    void giftConditionRejectsBothTargets() {
        assertThat(validator.validate(condition(5L, 7L, 3, null))).isNotEmpty();
    }

    @Test
    void giftConditionRejectsNegativeAmount() {
        assertThat(validator.validate(condition(null, null, null, "-1"))).isNotEmpty();
    }

    @Test
    void giftRejectsNonPositiveQuantity() {
        assertThat(validator.validate(new Gift(1L, 1L, condition(5L, null, 3, null), 9L, 0))).isNotEmpty();
    }

    @Test
    void comboPriceAndTierPercentageAreBigDecimal() {
        assertThat(componentType(Combo.class, "comboPrice")).isEqualTo(BigDecimal.class);
        assertThat(componentType(ScaleTier.class, "percentage")).isEqualTo(BigDecimal.class);
    }

    private static Class<?> componentType(Class<? extends Record> type, String name) {
        for (RecordComponent component : type.getRecordComponents()) {
            if (component.getName().equals(name)) {
                return component.getType();
            }
        }
        throw new AssertionError("No component '" + name + "' on " + type.getSimpleName());
    }
}
