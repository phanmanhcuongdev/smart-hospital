# Frontend

SmartHMS web UI: React, TypeScript and Vite. It serves the Examination and Medical Record (EMR) screens and the Human Resource Management (HR) screens, both wired to the real backend API (`/api/v1/emr/...` and `/api/v1/hr/...`).

## Run

After installing Node 24 and dependencies, run `npm ci`, then `npm run lint`, `npm run typecheck`, `npm run test`, and `npm run build`. The production image is served by Nginx and proxies `/api/` to the backend. `VITE_API_BASE_URL` optionally overrides the same-origin base URL. For local development, `npm run dev` serves the app on port 5173 and proxies `/api` to the backend on `http://localhost:8080`.

## Demo authentication

The backend uses HTTP Basic authentication with in-memory demo users; the password comes from the `SMARTHMS_DEMO_PASSWORD` environment variable (default `demo`). The login screen offers the seven demo users (doctor, nurse, CLS technician, records manager, reception, HR specialist, HR approver). Credentials are kept only in `sessionStorage` for the session. This is demo authentication only; never use real credentials here.

Clinical roles (doctor, nurse, CLS technician, records manager, reception) see the EMR screens; HR roles (HR specialist, HR approver) see the HR screens. HR users have no default clinical access.

## Screens

EMR: encounter list (SCR-EMR-01), encounter detail (SCR-EMR-02), vital signs and diagnosis (SCR-EMR-03), clinical orders (SCR-EMR-04), CLS results with pending and quarantined states (SCR-EMR-05), prescriptions with allergy warnings (SCR-EMR-06), medical record draft and signature (SCR-EMR-07), demo lock (SCR-EMR-08) and addenda (SCR-EMR-09). Record locking is a demo confirmation only; it is not a digital or legally binding signature.

HR: employee list and search, employee create and edit, departments, role profiles, practice certificates (CCHN) with validity states, status decisions with four-eyes approval, transfers and status history.

API errors (400, 401, 403, 404, 409, 5xx) are shown as plain-language messages. A 409 means a version or state conflict; the UI never overwrites data.
