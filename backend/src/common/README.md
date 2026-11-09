# Common boundary

`common` is reserved for cross-cutting abstractions that have an identified
owner and do not embed EMR or HR business rules.

Allowed after the relevant decisions are accepted:

- Environment configuration loading and startup validation.
- Diagnostic logging with correlation identifiers and no sensitive clinical,
  credential, session-verifier, or bearer-token content.
- Typed interfaces for authentication/session validation, authorization, and
  audit emission.

V.1 does not implement these facilities. Authentication, RBAC enforcement,
audit behavior, and finalized error handling belong to V.2. The default
security posture for future protected actions is deny-until-authorized; no
permit-all guard may be introduced as a placeholder.
