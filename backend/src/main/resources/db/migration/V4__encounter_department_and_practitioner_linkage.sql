-- V4: Encounter department and practitioner data linkage (Task 3)
-- Connects emr.medical_encounter with hr.department and ensures doctor licensing data is available

BEGIN;

-- Add department_id to emr.medical_encounter
ALTER TABLE emr.medical_encounter
    ADD COLUMN department_id uuid;

ALTER TABLE emr.medical_encounter
    ADD CONSTRAINT fk_emr_medical_encounter_department
    FOREIGN KEY (department_id) REFERENCES hr.department (department_id)
    ON DELETE RESTRICT ON UPDATE RESTRICT;

CREATE INDEX idx_emr_medical_encounter_department ON emr.medical_encounter (department_id);

-- Seed clinical practice license (CCHN) for synthetic clinician EMP-DEMO-001
INSERT INTO hr.license_certification (
    credential_id,
    employee_id,
    credential_type,
    credential_no,
    issuer,
    scope_note,
    valid_from,
    valid_until,
    verification_status,
    source_ref,
    created_by,
    updated_by
) VALUES (
    '00000000-0000-0000-0000-000000000105',
    '00000000-0000-0000-0000-000000000102',
    'CCHN_BAC_SI',
    'CCHN-00123/HN',
    'Sở Y Tế Hà Nội',
    'Khám chữa bệnh đa khoa và nội khoa',
    CURRENT_DATE - INTERVAL '1 year',
    CURRENT_DATE + INTERVAL '5 years',
    'VERIFIED',
    'SYT-HN-PORTAL',
    'flyway',
    'flyway'
) ON CONFLICT (credential_id) DO NOTHING;

-- Seed employment assignment for EMP-DEMO-001 in DEMO department
INSERT INTO hr.employment (
    employment_id,
    employee_id,
    department_id,
    job_title,
    start_at,
    created_by,
    updated_by
) VALUES (
    '00000000-0000-0000-0000-000000000106',
    '00000000-0000-0000-0000-000000000102',
    '00000000-0000-0000-0000-000000000101',
    'Bác sĩ điều trị',
    CURRENT_TIMESTAMP - INTERVAL '1 year',
    'flyway',
    'flyway'
) ON CONFLICT (employment_id) DO NOTHING;

-- Seed a second department (Khoa Nội tổng hợp) for richer integration testing & selection
INSERT INTO hr.department (
    department_id,
    department_code,
    display_name,
    department_type,
    active,
    created_by,
    updated_by
) VALUES (
    '00000000-0000-0000-0000-000000000107',
    'INTERNAL_MED',
    'Khoa Nội Tổng Hợp',
    'CLINICAL',
    true,
    'flyway',
    'flyway'
) ON CONFLICT (department_code) DO NOTHING;

-- Backfill any existing encounters with default DEMO department
UPDATE emr.medical_encounter
SET department_id = '00000000-0000-0000-0000-000000000101'
WHERE department_id IS NULL;

COMMIT;
