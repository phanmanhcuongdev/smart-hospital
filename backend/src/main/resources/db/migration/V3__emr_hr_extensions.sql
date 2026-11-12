-- V3: EMR/HR module extensions (2026-11-07).
-- All changes are additive; no V1 table is dropped or renamed.
-- Fills the spec-to-schema gaps for FR-HR-01 to FR-HR-04:
-- role profiles and status history did not exist, and the baseline
-- license check lacked the REJECTED verification status.

BEGIN;

-- hr.role_profiles (new, spec IV.8): title and qualification catalogue.
CREATE TABLE hr.role_profiles (
    role_profile_id uuid NOT NULL,
    title_name varchar(120) NOT NULL,
    professional_group varchar(80),
    qualification varchar(160),
    description text,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by varchar(80) NOT NULL,
    updated_by varchar(80) NOT NULL,
    CONSTRAINT pk_hr_role_profiles_1 PRIMARY KEY (role_profile_id),
    CONSTRAINT uq_hr_role_profiles_1 UNIQUE (title_name)
);

-- hr.employee_status_history (new, FR-HR-03): status change audit trail.
-- History is never overwritten; old_status is null for the initial record.
CREATE TABLE hr.employee_status_history (
    history_id uuid NOT NULL,
    employee_id uuid NOT NULL,
    old_status varchar(20),
    new_status varchar(20) NOT NULL,
    effective_at timestamptz NOT NULL,
    decision_id uuid,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by varchar(80) NOT NULL,
    updated_by varchar(80) NOT NULL,
    CONSTRAINT pk_hr_employee_status_history_1 PRIMARY KEY (history_id),
    CONSTRAINT fk_hr_employee_status_history_1 FOREIGN KEY (employee_id)
        REFERENCES hr.employee (employee_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_hr_employee_status_history_2 FOREIGN KEY (decision_id)
        REFERENCES hr.personnel_decision (decision_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_hr_employee_status_history_1
        CHECK (new_status IN ('ONBOARDING', 'ACTIVE', 'SUSPENDED', 'TERMINATED')),
    CONSTRAINT ck_hr_employee_status_history_2
        CHECK (old_status IS NULL OR old_status IN ('ONBOARDING', 'ACTIVE', 'SUSPENDED', 'TERMINATED'))
);

-- hr.employment gains the role profile link (FR-HR-04).
ALTER TABLE hr.employment ADD COLUMN role_profile_id uuid;
ALTER TABLE hr.employment ADD CONSTRAINT fk_hr_employment_role_profile
    FOREIGN KEY (role_profile_id) REFERENCES hr.role_profiles (role_profile_id)
    ON DELETE RESTRICT ON UPDATE RESTRICT;

-- hr.personnel_decision gains execution tracking (FR-HR-03):
-- an APPROVED decision with a future effective date stays PENDING.
ALTER TABLE hr.personnel_decision ADD COLUMN execution_status varchar(16) NOT NULL DEFAULT 'PENDING';
ALTER TABLE hr.personnel_decision ADD CONSTRAINT ck_hr_personnel_decision_execution_status
    CHECK (execution_status IN ('PENDING', 'APPLIED', 'SKIPPED', 'FAILED'));

-- hr.employee gains the contact fields named by IV.8 (FR-HR-01).
ALTER TABLE hr.employee ADD COLUMN gender varchar(20);
ALTER TABLE hr.employee ADD COLUMN address text;
ALTER TABLE hr.employee ADD COLUMN phone varchar(40);
ALTER TABLE hr.employee ADD COLUMN email varchar(160);
ALTER TABLE hr.employee ADD COLUMN hire_date date;

-- hr.department gains its type (FR-HR-01).
ALTER TABLE hr.department ADD COLUMN department_type varchar(60);

-- hr.license_certification: the spec adds REJECTED to verification status.
-- The V1 check is named ck_hr_license_certification_1.
ALTER TABLE hr.license_certification DROP CONSTRAINT ck_hr_license_certification_1;
ALTER TABLE hr.license_certification ADD CONSTRAINT ck_hr_license_certification_1
    CHECK (verification_status IN ('UNVERIFIED', 'VERIFIED', 'REVOKED', 'REJECTED'));

COMMIT;
