package com.marketplaceb2b.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    private static Set<String> violations(Object bean) {
        return validator.validate(bean).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    private static ScaleTier tier(int fromQty, String percentage) {
        return new ScaleTier(fromQty, new BigDecimal(percentage));
    }

    private static ScaleDiscount scale(Long rulesetId, DiscountTargetType type, Long productId, Long familyId,
                                       List<ScaleTier> tiers) {
        return new ScaleDiscount(1L, rulesetId, type, productId, familyId, tiers, 0);
    }

    private static ScaleDiscount skuScale(Long productId, List<ScaleTier> tiers) {
        return scale(1L, DiscountTargetType.SKU, productId, null, tiers);
    }

    private static ComboItem item(long productId, int minQty) {
        return new ComboItem(productId, minQty);
    }

    private static Combo combo(Long rulesetId, String name, List<ComboItem> items, String price) {
        return new Combo(1L, rulesetId, name, items, price == null ? null : new BigDecimal(price), 0);
    }

    private static GiftCondition condition(Long productId, Long familyId, Integer minQty, String minAmount) {
        return new GiftCondition(productId, familyId, minQty, minAmount == null ? null : new BigDecimal(minAmount));
    }

    // --- Scale discount -------------------------------------------------------

    @Test
    void validScaleForSkuHasNoViolations() {
        assertThat(validator.validate(skuScale(10L,
                List.of(tier(1, "0"), tier(10, "5"), tier(50, "10"))))).isEmpty();
    }

    @Test
    void validScaleForFamilyHasNoViolations() {
        assertThat(validator.validate(scale(1L, DiscountTargetType.FAMILY, null, 7L,
                List.of(tier(1, "5"))))).isEmpty();
    }

    @Test
    void scaleRejectsMissingRequiredFields() {
        assertThat(violations(scale(null, DiscountTargetType.SKU, 10L, null, List.of(tier(1, "5")))))
                .contains("rulesetId");
        assertThat(violations(skuScale(10L, List.of(tier(1, "5"))))).isEmpty();
        assertThat(violations(scale(1L, null, 10L, null, List.of(tier(1, "5"))))).contains("targetType");
        assertThat(violations(scale(1L, DiscountTargetType.SKU, 10L, null, null))).contains("tiers");
    }

    @Test
    void scaleRejectsEmptyTiers() {
        assertThat(violations(skuScale(10L, List.of()))).contains("tiers");
    }

    @Test
    void scaleRejectsNullTier() {
        assertThat(violations(skuScale(10L, java.util.Arrays.asList(tier(1, "5"), null)))).isNotEmpty();
    }

    @Test
    void scaleRejectsNonPositiveTierStart() {
        assertThat(violations(tier(0, "5"))).contains("fromQty");
        assertThat(violations(tier(-1, "5"))).contains("fromQty");
        assertThat(violations(skuScale(10L, List.of(tier(0, "5"))))).contains("tiers[0].fromQty");
    }

    @Test
    void scaleRejectsPercentageOutOfRange() {
        assertThat(violations(tier(1, "150"))).contains("percentage");
        assertThat(violations(tier(1, "-1"))).contains("percentage");
    }

    @Test
    void scaleRejectsNullTierPercentage() {
        assertThat(violations(new ScaleTier(1, null))).contains("percentage");
    }

    @Test
    void scaleRejectsSkuTargetWithoutProduct() {
        assertThat(violations(scale(1L, DiscountTargetType.SKU, null, null, List.of(tier(1, "5")))))
                .contains("targetConsistent");
    }

    @Test
    void scaleRejectsSkuTargetWithFamily() {
        assertThat(violations(scale(1L, DiscountTargetType.SKU, 10L, 7L, List.of(tier(1, "5")))))
                .contains("targetConsistent");
    }

    @Test
    void scaleRejectsFamilyTargetWithoutFamily() {
        assertThat(violations(scale(1L, DiscountTargetType.FAMILY, null, null, List.of(tier(1, "5")))))
                .contains("targetConsistent");
    }

    @Test
    void scaleRejectsFamilyTargetWithProduct() {
        assertThat(violations(scale(1L, DiscountTargetType.FAMILY, 10L, 7L, List.of(tier(1, "5")))))
                .contains("targetConsistent");
    }

    @Test
    void scaleRejectsEqualTierStarts() {
        assertThat(violations(skuScale(10L, List.of(tier(10, "5"), tier(10, "10")))))
                .contains("tiersIncreasing");
    }

    @Test
    void scaleRejectsDecreasingTierStarts() {
        assertThat(violations(skuScale(10L, List.of(tier(50, "5"), tier(10, "10")))))
                .contains("tiersIncreasing");
    }

    // --- Combo ----------------------------------------------------------------

    @Test
    void validComboHasNoViolations() {
        assertThat(validator.validate(combo(1L, "pack",
                List.of(item(1L, 2), item(2L, 1)), "100.00"))).isEmpty();
    }

    @Test
    void validComboWithSingleItemHasNoViolations() {
        assertThat(validator.validate(combo(1L, "pack", List.of(item(1L, 6)), "20000.00"))).isEmpty();
    }

    @Test
    void comboRejectsMissingRequiredFields() {
        assertThat(violations(combo(null, "pack", List.of(item(1L, 1)), "10.00"))).contains("rulesetId");
        assertThat(violations(combo(1L, null, List.of(item(1L, 1)), "10.00"))).contains("name");
        assertThat(violations(combo(1L, "pack", null, "10.00"))).contains("items");
        assertThat(violations(combo(1L, "pack", List.of(item(1L, 1)), null))).contains("comboPrice");
    }

    @Test
    void comboRejectsEmptyItems() {
        assertThat(violations(combo(1L, "pack", List.of(), "10.00"))).contains("items");
    }

    @Test
    void comboRejectsNullItem() {
        assertThat(violations(combo(1L, "pack", java.util.Arrays.asList(item(1L, 1), null), "10.00")))
                .isNotEmpty();
    }

    @Test
    void comboRejectsItemWithoutProduct() {
        assertThat(violations(combo(1L, "pack", List.of(new ComboItem(null, 1)), "10.00")))
                .contains("items[0].productId");
    }

    @Test
    void comboRejectsNonPositiveMinQty() {
        assertThat(violations(combo(1L, "pack", List.of(item(1L, 0)), "10.00"))).contains("items[0].minQty");
        assertThat(violations(new ComboItem(1L, -1))).contains("minQty");
    }

    @Test
    void comboRejectsNegativePrice() {
        assertThat(violations(combo(1L, "pack", List.of(item(1L, 1)), "-1"))).contains("comboPrice");
    }

    // --- Gift -----------------------------------------------------------------

    @Test
    void validGiftByQuantityOfProductHasNoViolations() {
        assertThat(validator.validate(new Gift(1L, 1L, condition(5L, null, 3, null), 9L, 1, false, 0))).isEmpty();
    }

    @Test
    void validGiftByQuantityOfFamilyHasNoViolations() {
        assertThat(validator.validate(new Gift(1L, 1L, condition(null, 7L, 3, null), 9L, 1, false, 0))).isEmpty();
    }

    @Test
    void validGiftByAmountOnlyHasNoViolations() {
        assertThat(validator.validate(new Gift(1L, 1L, condition(null, null, null, "1000.00"), 9L, 1, false, 0))).isEmpty();
    }

    @Test
    void validGiftByQuantityAndAmountHasNoViolations() {
        assertThat(validator.validate(new Gift(1L, 1L, condition(5L, null, 3, "1000.00"), 9L, 1, false, 0))).isEmpty();
    }

    @Test
    void giftConditionRejectsMissingThreshold() {
        assertThat(violations(condition(null, null, null, null))).contains("wellFormed");
    }

    @Test
    void giftConditionRejectsQuantityWithoutTarget() {
        assertThat(violations(condition(null, null, 3, null))).contains("wellFormed");
    }

    @Test
    void giftConditionRejectsBothTargets() {
        assertThat(violations(condition(5L, 7L, 3, null))).contains("wellFormed");
    }

    @Test
    void giftConditionRejectsNonPositiveQuantity() {
        assertThat(violations(condition(5L, null, 0, null))).contains("minQty");
        assertThat(violations(condition(5L, null, -1, null))).contains("minQty");
    }

    @Test
    void giftConditionRejectsNegativeAmount() {
        assertThat(violations(condition(null, null, null, "-1"))).contains("minAmount");
    }

    @Test
    void giftRejectsMissingRequiredFields() {
        assertThat(violations(new Gift(1L, null, condition(5L, null, 3, null), 9L, 1, false, 0))).contains("rulesetId");
        assertThat(violations(new Gift(1L, 1L, null, 9L, 1, false, 0))).contains("condition");
        assertThat(violations(new Gift(1L, 1L, condition(5L, null, 3, null), null, 1, false, 0))).contains("giftProductId");
    }

    @Test
    void giftRejectsNonPositiveQuantity() {
        assertThat(violations(new Gift(1L, 1L, condition(5L, null, 3, null), 9L, 0, false, 0))).contains("giftQty");
        assertThat(violations(new Gift(1L, 1L, condition(5L, null, 3, null), 9L, -2, false, 0))).contains("giftQty");
    }

    // --- Types ----------------------------------------------------------------

    @Test
    void moneyAndPercentageAreBigDecimal() {
        assertThat(componentType(Combo.class, "comboPrice")).isEqualTo(BigDecimal.class);
        assertThat(componentType(ScaleTier.class, "percentage")).isEqualTo(BigDecimal.class);
        assertThat(componentType(GiftCondition.class, "minAmount")).isEqualTo(BigDecimal.class);
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
