# Tasks

## 1. Complete the README design notes

- [x] 1.1 Fill **Lever application order** (the strategy pipeline, the priority order combo → scale → gift → tax, exclusivity by consumed units, the combo/scale/gift rules); verify it matches the pricing design
- [x] 1.2 Fill **Rounding policy** (per-line rounding, `DECIMAL(19,4)` money, the `Money` value object) and **Consistency model** (immutable quote, single-transaction confirmation, transactional outbox); verify they match the code
- [x] 1.3 Update **Scope** (pricing engine baseline implemented; levers/quote/credit/relay/HTTP/ADR pending) and **Structure** (`controller`, `dto`, `model`, `repository`, `service`) and the migrations list; verify they match the code
- [x] 1.4 Update **Known simplifications & rules** (combos chosen by priority, not size) and review the README end to end

## 2. Align the spec

- [x] 2.1 Update the `api-foundation` README requirement (MODIFIED) so the design-notes sections reflect the current design instead of being "marked as not yet defined"; verify `openspec validate document-design-notes --strict` passes

## Workflow follow-up

- After the maintainer merges the PR, archive the change (`openspec archive document-design-notes`).
