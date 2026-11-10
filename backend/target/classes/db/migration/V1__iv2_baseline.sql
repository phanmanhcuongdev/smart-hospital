-- IV.2 Database Design Specification v0.9-RC / 2026-11-06
-- PostgreSQL PROPOSED (ADR-DB-01), NOT an approved production baseline.
-- Scope: EMR+HR + limited SYS common + REF synthetic fixtures. No other module implemented.
-- Run on a NEW EMPTY database. Never use real patient records.
BEGIN;
CREATE SCHEMA IF NOT EXISTS ref;
CREATE SCHEMA IF NOT EXISTS hr;
CREATE SCHEMA IF NOT EXISTS sys;
CREATE SCHEMA IF NOT EXISTS emr;

-- ref.patient_fixture | PAT | MOCK | FR-PAT-01; BR-001,010; III.7 SC-01
CREATE TABLE ref.patient_fixture (
  patient_id uuid NOT NULL,
  display_name varchar(200) NOT NULL,
  identity_status varchar(16) NOT NULL,
  source_version varchar(40) NOT NULL,
  created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by varchar(80) NOT NULL,
  updated_by varchar(80) NOT NULL,
  CONSTRAINT pk_ref_patient_fixture_1 PRIMARY KEY (patient_id),
  CONSTRAINT ck_ref_patient_fixture_1 CHECK (identity_status IN ('UNVERIFIED','VERIFIED','FLAGGED'))
);

-- ref.appointment_fixture | APT | MOCK | FR-APT-01,02; BR-029; III.7 SC-04
CREATE TABLE ref.appointment_fixture (
  appointment_id uuid NOT NULL,
  patient_id uuid NOT NULL,
  appointment_status varchar(20) NOT NULL,
  check_in_at timestamptz,
  source_version varchar(40) NOT NULL,
  created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by varchar(80) NOT NULL,
  updated_by varchar(80) NOT NULL,
  CONSTRAINT pk_ref_appointment_fixture_1 PRIMARY KEY (appointment_id),
  CONSTRAINT uq_ref_appointment_fixture_1 UNIQUE (appointment_id, patient_id),
  CONSTRAINT fk_ref_appointment_fixture_1 FOREIGN KEY (patient_id) REFERENCES ref.patient_fixture (patient_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_ref_appointment_fixture_1 CHECK (appointment_status IN ('BOOKED','CHECKED_IN','CANCELLED','NO_SHOW'))
);

-- ref.medication_fixture | PHA | MOCK | FR-PHA-01; BR-053,055; III.7 SC-06
CREATE TABLE ref.medication_fixture (
  medication_id uuid NOT NULL,
  display_name varchar(240) NOT NULL,
  is_active boolean NOT NULL DEFAULT true,
  source_version varchar(40) NOT NULL,
  created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by varchar(80) NOT NULL,
  updated_by varchar(80) NOT NULL,
  CONSTRAINT pk_ref_medication_fixture_1 PRIMARY KEY (medication_id)
);

-- ref.allergy_fixture | PAT | MOCK | FR-PAT-02; BR-011,055; OPEN-08
CREATE TABLE ref.allergy_fixture (
  allergy_id uuid NOT NULL,
  patient_id uuid NOT NULL,
  allergen varchar(240),
  allergy_status varchar(24) NOT NULL,
  verified_at timestamptz,
  verified_by varchar(80),
  source_version varchar(40) NOT NULL,
  created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by varchar(80) NOT NULL,
  updated_by varchar(80) NOT NULL,
  CONSTRAINT pk_ref_allergy_fixture_1 PRIMARY KEY (allergy_id),
  CONSTRAINT fk_ref_allergy_fixture_1 FOREIGN KEY (patient_id) REFERENCES ref.patient_fixture (patient_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_ref_allergy_fixture_1 CHECK (allergy_status IN ('UNKNOWN','NONE_CONFIRMED','HAS_ALLERGY'))
);

-- hr.department | HR | CORE | FR-HR-01; BR-015; III.7 SC-02
CREATE TABLE hr.department (
  department_id uuid NOT NULL,
  department_code varchar(40) NOT NULL,
  display_name varchar(180) NOT NULL,
  parent_department_id uuid,
  active boolean NOT NULL DEFAULT true,
  created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by varchar(80) NOT NULL,
  updated_by varchar(80) NOT NULL,
  CONSTRAINT pk_hr_department_1 PRIMARY KEY (department_id),
  CONSTRAINT uq_hr_department_1 UNIQUE (department_code),
  CONSTRAINT fk_hr_department_1 FOREIGN KEY (parent_department_id) REFERENCES hr.department (department_id) ON DELETE RESTRICT ON UPDATE RESTRICT
);

-- hr.employee | HR | CORE | FR-HR-01,03; BR-015..020
CREATE TABLE hr.employee (
  employee_id uuid NOT NULL,
  employee_code varchar(40) NOT NULL,
  full_name varchar(200) NOT NULL,
  birth_date date,
  status varchar(20) NOT NULL,
  status_effective_at timestamptz NOT NULL,
  row_version integer NOT NULL DEFAULT 1,
  created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by varchar(80) NOT NULL,
  updated_by varchar(80) NOT NULL,
  CONSTRAINT pk_hr_employee_1 PRIMARY KEY (employee_id),
  CONSTRAINT uq_hr_employee_1 UNIQUE (employee_code),
  CONSTRAINT ck_hr_employee_1 CHECK (status IN ('ONBOARDING','ACTIVE','SUSPENDED','TERMINATED')),
  CONSTRAINT ck_hr_employee_2 CHECK (row_version > 0)
);

-- hr.employment | HR | CORE | FR-HR-01,03,04; BR-015,019,020
CREATE TABLE hr.employment (
  employment_id uuid NOT NULL,
  employee_id uuid NOT NULL,
  department_id uuid NOT NULL,
  job_title varchar(120) NOT NULL,
  start_at timestamptz NOT NULL,
  end_at timestamptz,
  decision_ref varchar(120),
  created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by varchar(80) NOT NULL,
  updated_by varchar(80) NOT NULL,
  CONSTRAINT pk_hr_employment_1 PRIMARY KEY (employment_id),
  CONSTRAINT fk_hr_employment_1 FOREIGN KEY (employee_id) REFERENCES hr.employee (employee_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_hr_employment_2 FOREIGN KEY (department_id) REFERENCES hr.department (department_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_hr_employment_1 CHECK (end_at IS NULL OR end_at > start_at)
);

-- hr.license_certification | HR | CORE | FR-HR-02; BR-017,018; OPEN-03
CREATE TABLE hr.license_certification (
  credential_id uuid NOT NULL,
  employee_id uuid NOT NULL,
  credential_type varchar(80) NOT NULL,
  credential_no varchar(120),
  issuer varchar(160),
  scope_note text,
  valid_from date,
  valid_until date,
  verification_status varchar(16) NOT NULL,
  source_ref varchar(160) NOT NULL,
  created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by varchar(80) NOT NULL,
  updated_by varchar(80) NOT NULL,
  CONSTRAINT pk_hr_license_certification_1 PRIMARY KEY (credential_id),
  CONSTRAINT fk_hr_license_certification_1 FOREIGN KEY (employee_id) REFERENCES hr.employee (employee_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_hr_license_certification_1 CHECK (verification_status IN ('UNVERIFIED','VERIFIED','REVOKED')),
  CONSTRAINT ck_hr_license_certification_2 CHECK (valid_until IS NULL OR valid_from IS NULL OR valid_until >= valid_from)
);

-- hr.personnel_decision | HR | CORE | FR-HR-03,04; BR-017,019,020
CREATE TABLE hr.personnel_decision (
  decision_id uuid NOT NULL,
  employee_id uuid NOT NULL,
  decision_ref varchar(120) NOT NULL,
  decision_type varchar(20) NOT NULL,
  decision_status varchar(16) NOT NULL,
  effective_at timestamptz NOT NULL,
  approved_by varchar(80),
  approved_at timestamptz,
  reason text,
  created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by varchar(80) NOT NULL,
  updated_by varchar(80) NOT NULL,
  CONSTRAINT pk_hr_personnel_decision_1 PRIMARY KEY (decision_id),
  CONSTRAINT uq_hr_personnel_decision_1 UNIQUE (decision_ref),
  CONSTRAINT fk_hr_personnel_decision_1 FOREIGN KEY (employee_id) REFERENCES hr.employee (employee_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_hr_personnel_decision_1 CHECK (decision_type IN ('ACTIVATE','SUSPEND','RETURN','TERMINATE','TRANSFER')),
  CONSTRAINT ck_hr_personnel_decision_2 CHECK (decision_status IN ('DRAFT','APPROVED','REVOKED')),
  CONSTRAINT ck_hr_personnel_decision_3 CHECK (decision_status <> 'APPROVED' OR (approved_by IS NOT NULL AND approved_at IS NOT NULL))
);

-- hr.integration_outbox | HR | CORE | FR-HR-03,04; BR-020,090; III.4 DH-01
CREATE TABLE hr.integration_outbox (
  event_id uuid NOT NULL,
  employee_id uuid NOT NULL,
  event_key varchar(140) NOT NULL,
  event_type varchar(64) NOT NULL,
  payload jsonb NOT NULL,
  delivery_state varchar(16) NOT NULL DEFAULT 'PENDING',
  attempts integer NOT NULL DEFAULT 0,
  last_attempt_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by varchar(80) NOT NULL,
  updated_by varchar(80) NOT NULL,
  CONSTRAINT pk_hr_integration_outbox_1 PRIMARY KEY (event_id),
  CONSTRAINT uq_hr_integration_outbox_1 UNIQUE (event_key),
  CONSTRAINT fk_hr_integration_outbox_1 FOREIGN KEY (employee_id) REFERENCES hr.employee (employee_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_hr_integration_outbox_1 CHECK (delivery_state IN ('PENDING','SENT','FAILED')),
  CONSTRAINT ck_hr_integration_outbox_2 CHECK (attempts >= 0)
);

-- sys.user_account | SYS | CORE | FR-SYS-01; BR-086..090,094
CREATE TABLE sys.user_account (
  user_id uuid NOT NULL,
  employee_id uuid,
  username varchar(100) NOT NULL,
  password_hash text NOT NULL,
  account_status varchar(16) NOT NULL,
  revocation_epoch integer NOT NULL DEFAULT 0,
  created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by varchar(80) NOT NULL,
  updated_by varchar(80) NOT NULL,
  CONSTRAINT pk_sys_user_account_1 PRIMARY KEY (user_id),
  CONSTRAINT uq_sys_user_account_1 UNIQUE (username),
  CONSTRAINT uq_sys_user_account_2 UNIQUE (employee_id),
  CONSTRAINT fk_sys_user_account_1 FOREIGN KEY (employee_id) REFERENCES hr.employee (employee_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_sys_user_account_1 CHECK (account_status IN ('PENDING','ACTIVE','DISABLED')),
  CONSTRAINT ck_sys_user_account_2 CHECK (revocation_epoch >= 0)
);

-- sys.role | SYS | CORE | FR-SYS-02; BR-003,091
CREATE TABLE sys.role (
  role_id uuid NOT NULL,
  role_code varchar(80) NOT NULL,
  display_name varchar(150) NOT NULL,
  created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by varchar(80) NOT NULL,
  updated_by varchar(80) NOT NULL,
  CONSTRAINT pk_sys_role_1 PRIMARY KEY (role_id),
  CONSTRAINT uq_sys_role_1 UNIQUE (role_code)
);

-- sys.permission | SYS | CORE | FR-SYS-02; BR-003,091
CREATE TABLE sys.permission (
  permission_id uuid NOT NULL,
  resource_code varchar(60) NOT NULL,
  action_code varchar(60) NOT NULL,
  scope_code varchar(60) NOT NULL,
  created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by varchar(80) NOT NULL,
  updated_by varchar(80) NOT NULL,
  CONSTRAINT pk_sys_permission_1 PRIMARY KEY (permission_id),
  CONSTRAINT uq_sys_permission_1 UNIQUE (resource_code, action_code, scope_code)
);

-- sys.role_permission | SYS | CORE | FR-SYS-02; BR-091
CREATE TABLE sys.role_permission (
  role_id uuid NOT NULL,
  permission_id uuid NOT NULL,
  created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by varchar(80) NOT NULL,
  updated_by varchar(80) NOT NULL,
  CONSTRAINT pk_sys_role_permission_1 PRIMARY KEY (role_id, permission_id),
  CONSTRAINT fk_sys_role_permission_1 FOREIGN KEY (role_id) REFERENCES sys.role (role_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_sys_role_permission_2 FOREIGN KEY (permission_id) REFERENCES sys.permission (permission_id) ON DELETE RESTRICT ON UPDATE RESTRICT
);

-- sys.role_assignment | SYS | CORE | FR-SYS-02; BR-090,091; OPEN-01
CREATE TABLE sys.role_assignment (
  assignment_id uuid NOT NULL,
  user_id uuid NOT NULL,
  role_id uuid NOT NULL,
  start_at timestamptz NOT NULL,
  end_at timestamptz,
  approval_ref varchar(120) NOT NULL,
  approved_by varchar(80) NOT NULL,
  created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by varchar(80) NOT NULL,
  updated_by varchar(80) NOT NULL,
  CONSTRAINT pk_sys_role_assignment_1 PRIMARY KEY (assignment_id),
  CONSTRAINT fk_sys_role_assignment_1 FOREIGN KEY (user_id) REFERENCES sys.user_account (user_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_sys_role_assignment_2 FOREIGN KEY (role_id) REFERENCES sys.role (role_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_sys_role_assignment_1 CHECK (end_at IS NULL OR end_at > start_at)
);

-- sys.session | SYS | CORE | FR-SYS-01; BR-090,094; NFR-SEC-01
CREATE TABLE sys.session (
  session_id uuid NOT NULL,
  user_id uuid NOT NULL,
  token_digest varchar(128) NOT NULL,
  issued_at timestamptz NOT NULL,
  expires_at timestamptz NOT NULL,
  revoked_at timestamptz,
  issued_epoch integer NOT NULL,
  created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by varchar(80) NOT NULL,
  updated_by varchar(80) NOT NULL,
  CONSTRAINT pk_sys_session_1 PRIMARY KEY (session_id),
  CONSTRAINT uq_sys_session_1 UNIQUE (token_digest),
  CONSTRAINT fk_sys_session_1 FOREIGN KEY (user_id) REFERENCES sys.user_account (user_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_sys_session_1 CHECK (expires_at > issued_at),
  CONSTRAINT ck_sys_session_2 CHECK (issued_epoch >= 0)
);

-- sys.audit_event | SYS | CORE | FR-SYS-03; BR-002,007,045,092; NFR-LOG-01
CREATE TABLE sys.audit_event (
  audit_id uuid NOT NULL,
  actor_user_id uuid,
  actor_ref varchar(80) NOT NULL,
  target_type varchar(80) NOT NULL,
  target_id varchar(120) NOT NULL,
  action varchar(80) NOT NULL,
  outcome varchar(16) NOT NULL,
  change_ref varchar(120),
  happened_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  safe_metadata jsonb,
  created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by varchar(80) NOT NULL,
  updated_by varchar(80) NOT NULL,
  CONSTRAINT pk_sys_audit_event_1 PRIMARY KEY (audit_id),
  CONSTRAINT fk_sys_audit_event_1 FOREIGN KEY (actor_user_id) REFERENCES sys.user_account (user_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_sys_audit_event_1 CHECK (outcome IN ('SUCCESS','DENIED','FAILURE'))
);

-- emr.medical_encounter | EMR | CORE | FR-EMR-01; BR-029,032..038
CREATE TABLE emr.medical_encounter (
  encounter_id uuid NOT NULL,
  patient_id uuid NOT NULL,
  appointment_id uuid,
  attending_employee_id uuid NOT NULL,
  status varchar(20) NOT NULL,
  started_at timestamptz,
  completed_at timestamptz,
  row_version integer NOT NULL DEFAULT 1,
  created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by varchar(80) NOT NULL,
  updated_by varchar(80) NOT NULL,
  CONSTRAINT pk_emr_medical_encounter_1 PRIMARY KEY (encounter_id),
  CONSTRAINT fk_emr_medical_encounter_1 FOREIGN KEY (patient_id) REFERENCES ref.patient_fixture (patient_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_emr_medical_encounter_2 FOREIGN KEY (appointment_id, patient_id) REFERENCES ref.appointment_fixture (appointment_id, patient_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_emr_medical_encounter_3 FOREIGN KEY (attending_employee_id) REFERENCES hr.employee (employee_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_emr_medical_encounter_1 CHECK (status IN ('WAITING','IN_PROGRESS','PENDING_LAB_RAD','DIAGNOSED','COMPLETED','CANCELLED','NO_SHOW')),
  CONSTRAINT ck_emr_medical_encounter_2 CHECK (row_version > 0)
);

-- emr.clinical_observation | EMR | CORE | FR-EMR-01; BR-035,076; NFR-DATA-01
CREATE TABLE emr.clinical_observation (
  observation_id uuid NOT NULL,
  encounter_id uuid NOT NULL,
  metric_code varchar(60) NOT NULL,
  numeric_value numeric(14,4),
  text_value text,
  unit_code varchar(40),
  observed_at timestamptz NOT NULL,
  source_type varchar(20) NOT NULL,
  source_ref varchar(160) NOT NULL,
  quality varchar(16) NOT NULL,
  created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by varchar(80) NOT NULL,
  updated_by varchar(80) NOT NULL,
  CONSTRAINT pk_emr_clinical_observation_1 PRIMARY KEY (observation_id),
  CONSTRAINT fk_emr_clinical_observation_1 FOREIGN KEY (encounter_id) REFERENCES emr.medical_encounter (encounter_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_emr_clinical_observation_1 CHECK ((numeric_value IS NOT NULL) <> (text_value IS NOT NULL)),
  CONSTRAINT ck_emr_clinical_observation_2 CHECK (source_type IN ('MANUAL','FIXTURE','EXTERNAL')),
  CONSTRAINT ck_emr_clinical_observation_3 CHECK (quality IN ('VALID','INVALID','UNKNOWN'))
);

-- emr.diagnosis | EMR | CORE | FR-EMR-01; BR-036,037
CREATE TABLE emr.diagnosis (
  diagnosis_id uuid NOT NULL,
  encounter_id uuid NOT NULL,
  diagnosis_type varchar(16) NOT NULL,
  icd10_code varchar(20),
  narrative text NOT NULL,
  diagnosis_status varchar(16) NOT NULL,
  confirmed_by uuid,
  created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by varchar(80) NOT NULL,
  updated_by varchar(80) NOT NULL,
  CONSTRAINT pk_emr_diagnosis_1 PRIMARY KEY (diagnosis_id),
  CONSTRAINT fk_emr_diagnosis_1 FOREIGN KEY (encounter_id) REFERENCES emr.medical_encounter (encounter_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_emr_diagnosis_2 FOREIGN KEY (confirmed_by) REFERENCES hr.employee (employee_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_emr_diagnosis_1 CHECK (diagnosis_type IN ('PRIMARY','SECONDARY')),
  CONSTRAINT ck_emr_diagnosis_2 CHECK (diagnosis_status IN ('PROVISIONAL','CONFIRMED'))
);

-- emr.medical_record | EMR | CORE | FR-EMR-04; BR-039..046
CREATE TABLE emr.medical_record (
  record_id uuid NOT NULL,
  encounter_id uuid NOT NULL,
  record_status varchar(22) NOT NULL,
  current_version integer NOT NULL DEFAULT 1,
  locked_at timestamptz,
  locked_by uuid,
  row_version integer NOT NULL DEFAULT 1,
  created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by varchar(80) NOT NULL,
  updated_by varchar(80) NOT NULL,
  CONSTRAINT pk_emr_medical_record_1 PRIMARY KEY (record_id),
  CONSTRAINT uq_emr_medical_record_1 UNIQUE (encounter_id),
  CONSTRAINT fk_emr_medical_record_1 FOREIGN KEY (encounter_id) REFERENCES emr.medical_encounter (encounter_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_emr_medical_record_2 FOREIGN KEY (locked_by) REFERENCES sys.user_account (user_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_emr_medical_record_1 CHECK (record_status IN ('DRAFT','PENDING_SIGNATURE','LOCKED','ARCHIVED')),
  CONSTRAINT ck_emr_medical_record_2 CHECK (current_version > 0),
  CONSTRAINT ck_emr_medical_record_3 CHECK (row_version > 0),
  CONSTRAINT ck_emr_medical_record_4 CHECK ((record_status NOT IN ('LOCKED','ARCHIVED')) OR locked_at IS NOT NULL)
);

-- emr.record_version | EMR | CORE | FR-EMR-04; BR-041..044; NFR-DATA-02
CREATE TABLE emr.record_version (
  record_id uuid NOT NULL,
  version_no integer NOT NULL,
  payload jsonb NOT NULL,
  sha256_hex varchar(64),
  version_created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by varchar(80) NOT NULL,
  updated_by varchar(80) NOT NULL,
  CONSTRAINT pk_emr_record_version_1 PRIMARY KEY (record_id, version_no),
  CONSTRAINT fk_emr_record_version_1 FOREIGN KEY (record_id) REFERENCES emr.medical_record (record_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_emr_record_version_1 CHECK (version_no > 0)
);

-- emr.addendum | EMR | CORE | FR-EMR-04; BR-043,044
CREATE TABLE emr.addendum (
  addendum_id uuid NOT NULL,
  record_id uuid NOT NULL,
  reason text NOT NULL,
  content text NOT NULL,
  confirmation_status varchar(16) NOT NULL,
  confirmed_at timestamptz,
  confirmed_by uuid,
  created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by varchar(80) NOT NULL,
  updated_by varchar(80) NOT NULL,
  CONSTRAINT pk_emr_addendum_1 PRIMARY KEY (addendum_id),
  CONSTRAINT fk_emr_addendum_1 FOREIGN KEY (record_id) REFERENCES emr.medical_record (record_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_emr_addendum_2 FOREIGN KEY (confirmed_by) REFERENCES sys.user_account (user_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_emr_addendum_1 CHECK (confirmation_status IN ('DRAFT','CONFIRMED')),
  CONSTRAINT ck_emr_addendum_2 CHECK (confirmation_status <> 'CONFIRMED' OR (confirmed_at IS NOT NULL AND confirmed_by IS NOT NULL))
);

-- emr.medical_order | EMR | CORE | FR-EMR-02; BR-035,047..052
CREATE TABLE emr.medical_order (
  order_id uuid NOT NULL,
  encounter_id uuid NOT NULL,
  ordered_by uuid NOT NULL,
  order_type varchar(16) NOT NULL,
  order_status varchar(18) NOT NULL,
  external_order_ref varchar(160),
  ordered_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by varchar(80) NOT NULL,
  updated_by varchar(80) NOT NULL,
  CONSTRAINT pk_emr_medical_order_1 PRIMARY KEY (order_id),
  CONSTRAINT fk_emr_medical_order_1 FOREIGN KEY (encounter_id) REFERENCES emr.medical_encounter (encounter_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_emr_medical_order_2 FOREIGN KEY (ordered_by) REFERENCES hr.employee (employee_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_emr_medical_order_1 CHECK (order_type IN ('LAB','IMAGING','OTHER')),
  CONSTRAINT ck_emr_medical_order_2 CHECK (order_status IN ('DRAFT','ORDERED','ACKNOWLEDGED','IN_PROGRESS','COMPLETED','CANCELLED'))
);

-- emr.clinical_result | EMR | CORE | FR-EMR-02; BR-035,051,052,100; III.4 DH-05
CREATE TABLE emr.clinical_result (
  result_id uuid NOT NULL,
  order_id uuid NOT NULL,
  source_system varchar(100) NOT NULL,
  source_result_id varchar(160) NOT NULL,
  source_version varchar(60) NOT NULL,
  received_at timestamptz NOT NULL,
  result_at timestamptz,
  result_payload jsonb NOT NULL,
  abnormal_flag boolean,
  quality varchar(16) NOT NULL,
  created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by varchar(80) NOT NULL,
  updated_by varchar(80) NOT NULL,
  CONSTRAINT pk_emr_clinical_result_1 PRIMARY KEY (result_id),
  CONSTRAINT uq_emr_clinical_result_1 UNIQUE (source_system, source_result_id, source_version),
  CONSTRAINT fk_emr_clinical_result_1 FOREIGN KEY (order_id) REFERENCES emr.medical_order (order_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_emr_clinical_result_1 CHECK (quality IN ('VALID','QUARANTINED'))
);

-- emr.prescription | EMR | CORE | FR-EMR-03; BR-053..055
CREATE TABLE emr.prescription (
  prescription_id uuid NOT NULL,
  encounter_id uuid NOT NULL,
  prescriber_id uuid NOT NULL,
  prescription_status varchar(16) NOT NULL,
  issued_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by varchar(80) NOT NULL,
  updated_by varchar(80) NOT NULL,
  CONSTRAINT pk_emr_prescription_1 PRIMARY KEY (prescription_id),
  CONSTRAINT fk_emr_prescription_1 FOREIGN KEY (encounter_id) REFERENCES emr.medical_encounter (encounter_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_emr_prescription_2 FOREIGN KEY (prescriber_id) REFERENCES hr.employee (employee_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_emr_prescription_1 CHECK (prescription_status IN ('DRAFT','ORDERED','CANCELLED'))
);

-- emr.prescription_item | EMR | CORE | FR-EMR-03; BR-053,054,055
CREATE TABLE emr.prescription_item (
  prescription_id uuid NOT NULL,
  line_no integer NOT NULL,
  medication_id uuid NOT NULL,
  medication_label varchar(240) NOT NULL,
  dose varchar(100) NOT NULL,
  route varchar(80) NOT NULL,
  frequency varchar(120) NOT NULL,
  days_count integer NOT NULL,
  quantity numeric(14,3) NOT NULL,
  unit_code varchar(40) NOT NULL,
  created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by varchar(80) NOT NULL,
  updated_by varchar(80) NOT NULL,
  CONSTRAINT pk_emr_prescription_item_1 PRIMARY KEY (prescription_id, line_no),
  CONSTRAINT fk_emr_prescription_item_1 FOREIGN KEY (prescription_id) REFERENCES emr.prescription (prescription_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_emr_prescription_item_2 FOREIGN KEY (medication_id) REFERENCES ref.medication_fixture (medication_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_emr_prescription_item_1 CHECK (line_no > 0),
  CONSTRAINT ck_emr_prescription_item_2 CHECK (days_count > 0),
  CONSTRAINT ck_emr_prescription_item_3 CHECK (quantity > 0)
);

COMMIT;

