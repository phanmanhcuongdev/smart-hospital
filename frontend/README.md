# Frontend

React, TypeScript and Vite status UI. After installing Node 24 and dependencies,
run `npm ci`, then `npm run lint`, `npm run typecheck`, `npm run test`, and
`npm run build`. The production image is served by Nginx and proxies `/api/` to
the backend. `VITE_API_BASE_URL` optionally overrides the same-origin base URL.

V.1 renders only system-health state; EMR/HR UI workflows are deliberately
deferred to their owning work packages.
