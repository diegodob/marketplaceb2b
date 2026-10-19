## 1. The endpoint

- [ ] 1.1 The DTOs (`CreateOrderRequest(quotePublicId)`, `CreatedOrder(number, total)`); the tenant-scoped `QuoteRepository.findByPublicId` and `SalesOrderRepository.findByQuote`; the quote's public id on the read model.
- [ ] 1.2 `OrderController`: the `POST /api/v1/orders` (the headers, the body, the 201, the conflict re-read) and the `GET /api/v1/orders/{number}` (tenant-scoped).
- [ ] 1.3 The error mapping in `ApiExceptionHandler` (404 / 409 / 422 / 400 / 401).

## 2. Tests

- [ ] 2.1 The E2E: the 201, the replay (the same 201/body), the two 409, the insufficient (422), the expired (422), the unknown/another tenant (404), the GET (200/404).
- [ ] 2.2 The **double tap**: two concurrent POSTs with the same key -> both 201 with the same order, one `sales_order`, one consumption, one outbox event.
- [ ] 2.3 The quote -> order: the confirmed order keeps its price after a price change (the snapshot).

## 3. Documentation

- [ ] 3.1 The README: the `/orders` endpoints, the errors and the pending list.
- [ ] 3.2 `gradlew clean test`.
