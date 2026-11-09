# EMR module boundary

EMR owns examination and clinical-record behavior within the approved EMR
scope. It must not own HR, patient identity, appointment, allergy, medication
catalogue, or diagnostic-service master data.

When those facts are required, EMR consumes only approved typed in-process
ports or synthetic test fixtures. It must not query another module's internal
repositories/tables or create an internal REST service as a substitute.

V.1 contains no EMR business behavior, schema, routes, or real integrations.
