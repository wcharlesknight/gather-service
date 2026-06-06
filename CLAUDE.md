### Related Repositories & Clients
- **Frontend App (LoopIn):** [LoopIn](~/Projects/LoopIn)
  - This is the primary user-facing frontend repository that consumes this service's APIs.

## Cross-Repo Development Guardrails
1. **Breaking API Changes:** Before modifying, renaming, or deleting any controller routes, query parameters, or response payloads in this repository, Claude **MUST** search (`grep`) the [LoopIn](~/Projects/LoopIn) workspace to assess the downstream impact on frontend fetch requests and state management.
2. **Data Type Synchronization:** When updating backend response schemas, cross-reference the corresponding TypeScript interfaces or network types in the client app to ensure type safety remains intact.