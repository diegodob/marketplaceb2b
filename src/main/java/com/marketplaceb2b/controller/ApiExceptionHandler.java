package com.marketplaceb2b.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebInputException;

import com.marketplaceb2b.dto.ApiError;
import com.marketplaceb2b.service.exception.MissingPaymentMethodException;
import com.marketplaceb2b.service.exception.MissingPriceException;
import com.marketplaceb2b.service.exception.MissingTaxRateException;
import com.marketplaceb2b.service.exception.NoCurrentRulesetException;

/**
 * Maps failures to explicit, non-leaking HTTP errors. The response never carries the exception
 * message (it holds internal ids) or a stack trace: only a stable code, a clear message and, for
 * validation, the offending fields.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MissingPriceException.class)
    public ResponseEntity<ApiError> productNotSellable(MissingPriceException exception) {
        log.info("A cart line has no price: {}", exception.getMessage());
        return error(HttpStatus.UNPROCESSABLE_ENTITY, "PRODUCT_NOT_SELLABLE",
                "Uno de los productos del carrito no está disponible para la venta.");
    }

    @ExceptionHandler(NoCurrentRulesetException.class)
    public ResponseEntity<ApiError> storeWithoutPricing(NoCurrentRulesetException exception) {
        log.warn("No pricing in force: {}", exception.getMessage());
        return error(HttpStatus.SERVICE_UNAVAILABLE, "STORE_PRICING_UNAVAILABLE",
                "La tienda no tiene precios disponibles en este momento.");
    }

    @ExceptionHandler(MissingPaymentMethodException.class)
    public ResponseEntity<ApiError> paymentMethodNotFound(MissingPaymentMethodException exception) {
        log.info("Unknown payment method: {}", exception.getMessage());
        return error(HttpStatus.UNPROCESSABLE_ENTITY, "PAYMENT_METHOD_NOT_FOUND",
                "El medio de pago no es válido.");
    }

    @ExceptionHandler(MissingTaxRateException.class)
    public ResponseEntity<ApiError> pricingMisconfigured(MissingTaxRateException exception) {
        log.error("Missing tax rate: {}", exception.getMessage());
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "PRICING_MISCONFIGURED",
                "No pudimos calcular los impuestos de este pedido. Intentá más tarde.");
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ApiError> invalidCart(WebExchangeBindException exception) {
        Map<String, String> details = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getFieldErrors()) {
            details.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return new ResponseEntity<>(new ApiError("INVALID_REQUEST", "El carrito no es válido.", details),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ServerWebInputException.class)
    public ResponseEntity<ApiError> invalidBody(ServerWebInputException exception) {
        log.info("Unreadable request body: {}", exception.getMessage());
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "El carrito no es válido.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpected(Exception exception) {
        log.error("Unexpected error while pricing", exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "No pudimos calcular el precio. Intentá más tarde.");
    }

    private static ResponseEntity<ApiError> error(HttpStatus status, String code, String message) {
        return new ResponseEntity<>(new ApiError(code, message, Map.of()), status);
    }
}
