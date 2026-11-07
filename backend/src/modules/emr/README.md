# EMR module boundary

EMR owns examination and clinical-record behavior within the approved EMR scope. It must not own HR, patient identity, appointment, allergy, medication catalogue, or diagnostic-service master data.

When those facts are required, EMR consumes only approved typed in-process ports or synthetic test fixtures. It must not query the internal repositories/tables of another module or create an internal REST service as a substitute.

## Implemented (branch tran-nam)

FR-EMR-01 to FR-EMR-04 are implemented under backend/src/main/java/vn/edu/ptit/smarthms/emr/ (services in emr/service, routes in emr/api, entities and repositories in emr/domain):

- FR-EMR-01 Encounters: open, start, complete and cancel an encounter for a verified patient with an ACTIVE attending employee; vitals recorded by nurses only; diagnosis recorded by the attending doctor only (a CONFIRMED diagnosis requires an ICD-10 code; one confirmed primary diagnosis per encounter). EncounterService.
- FR-EMR-02 Orders and results: doctor creates a CLS order (ACTIVE doctor with a valid CCHN); order states DRAFT, ORDERED, ACKNOWLEDGED, IN_PROGRESS, COMPLETED, CANCELLED. Results are reconciled by OrderID + PatientID: a wrong-patient result is stored as QUARANTINED and does not complete the order; a duplicate source tuple (source system, source result id, source version) returns the existing result without a new row; a missing result leaves the order pending. OrderService.
- FR-EMR-03 Prescriptions: linked to an encounter; only an ACTIVE doctor with a valid CCHN may prescribe; only active MedicationCatalog drugs; each item carries dose, route, frequency, days and quantity. The allergy check blocks a known allergy and warns (it never declares a drug safe) when allergy data is UNKNOWN or missing. A prescription is not a dispensation. PrescriptionService.
- FR-EMR-04 Records: lifecycle DRAFT -> PENDING_SIGNATURE -> LOCKED -> ARCHIVED. Submit and lock use the expected row_version compare-and-swap (a stale version returns 409). LOCKED records are immutable through the API; an addendum is a linked child record on a LOCKED parent only. Locking is a demo confirmation, NOT a legal or digital signature; locked_by stays empty in the demo because the seed has no user-account rows. MedicalRecordService.

Endpoints live under /api/v1/emr/ and require authentication (HTTP Basic demo users, see the backend README). Practitioner facts (active status, CCHN validity) come only through PractitionerPort, implemented by the HR module (HrPractitionerAdapter); EMR never queries HR tables.

## Spec-to-schema notes

The 10 emr tables come from the V.1 baseline migration (V1__iv2_baseline.sql); V3 adds no EMR tables. JPA is validate-only, so the entities map the baseline columns exactly (foreign keys are plain UUID columns, not associations).

Prescription has no version or row_version column in the baseline, so the spec prescription version is not persisted.

record_version payload is JSONB and clinical_result result_payload is JSONB.
