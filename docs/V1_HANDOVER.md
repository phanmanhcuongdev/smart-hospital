# V.1 implementation handover

## Structure and ownership

- `backend/src/common`: cross-cutting extension points only; no module business
  rules. SYS/common owns accounts, roles, permissions, sessions, and audit.
- `backend/src/modules/emr`: owns encounter/clinical-record behavior only.
- `backend/src/modules/hr`: owns employment and professional evidence only.
- `frontend`: reserved until a framework is accepted.

EMR and HR communicate through approved typed, in-process ports. They must not
reach into one another's persistence, introduce circular dependencies, or
become internal HTTP/microservices. Patient, appointment, allergy, medication,
CLS, and other out-of-scope data are fixtures/adapters only when approved and
needed by EMR.

## Work handoff

| Work package | Start here | Preconditions |
| --- | --- | --- |
| V.2 common/security | `backend/src/common` | Accept backend stack; resolve IV.6 policy. Implement auth, RBAC, audit, error handling with default deny. |
| V.3 EMR | `backend/src/modules/emr` | Accept EMR API/schema decisions and approved fixture/port contracts. |
| V.4 HR | `backend/src/modules/hr` | Accept HR API/schema decisions; publish practitioner facts through an approved port. |
| V.5 integration | module boundaries above | Use typed in-process ports; freeze contract ownership before wiring. |
| V.6 demo/test | approved database/test setup | Add synthetic demo data/account seed and end-to-end tests only after migration strategy exists. |

## Implementation rules

- Add the chosen manifest and its lockfile together after approval.
- Put non-secret examples in an approved environment example; retain `.env`
  ignore protection and validate required variables at startup.
- Convert IV.2 DDL into executable migrations only after DBMS/tooling approval;
  keep SCP-IV4-001 out until unblocked.
- Record actual commands, exit codes, and environment assumptions in
  `V1_VERIFICATION.md`; never mark a static review as runtime PASS.
