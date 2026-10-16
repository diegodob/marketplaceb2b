# Proposal

## Why

The `data-model` spec already requires the access layer to scope by tenant, but it says nothing about **tests**. Product listings and filters will be added later (a catalog capability) and a filter that forgets the `tenant_id` silently mixes tenants — exactly the failure we want to make impossible.

## What Changes

- Sharpen the `data-model` "Multi-tenant column isolation" requirement: every query that lists or filters products — or any other tenant-owned entity — SHALL be scoped by tenant in the access layer and SHALL be covered by a multi-tenant test proving another tenant's rows are never returned.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `data-model`: the isolation requirement now also demands a tenant-scoped query and a multi-tenant test whenever product filters are added.

## Impact

- `openspec/specs/data-model/spec.md` (when applied). No code.
