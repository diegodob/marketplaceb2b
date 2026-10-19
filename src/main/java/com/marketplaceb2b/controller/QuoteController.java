package com.marketplaceb2b.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.marketplaceb2b.dto.Cart;
import com.marketplaceb2b.dto.CreatedQuote;
import com.marketplaceb2b.service.QuoteWriter;

import jakarta.validation.Valid;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/api/v1")
public class QuoteController {

    private final QuoteWriter writer;

    public QuoteController(QuoteWriter writer) {
        this.writer = writer;
    }

    @PostMapping("/quotes")
    public Mono<ResponseEntity<CreatedQuote>> create(@Valid @RequestBody Cart cart) {
        return Mono.fromCallable(() -> writer.create(cart))
                .subscribeOn(Schedulers.boundedElastic())
                .map(created -> ResponseEntity.status(HttpStatus.CREATED).body(created));
    }
}
