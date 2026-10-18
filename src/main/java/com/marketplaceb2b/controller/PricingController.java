package com.marketplaceb2b.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.marketplaceb2b.dto.Cart;
import com.marketplaceb2b.dto.PriceBreakdown;
import com.marketplaceb2b.service.OrderPricingService;

import jakarta.validation.Valid;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/api/v1")
public class PricingController {

    private final OrderPricingService pricing;

    public PricingController(OrderPricingService pricing) {
        this.pricing = pricing;
    }

    @PostMapping("/pricing")
    public Mono<PriceBreakdown> price(@Valid @RequestBody Cart cart) {
        return Mono.fromCallable(() -> pricing.price(cart))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
