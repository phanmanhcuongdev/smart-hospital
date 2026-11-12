-- IV.2 v0.9-RC indexes & protective triggers for PostgreSQL (from SQL/IV2_02_indexes_triggers.sql)
CREATE INDEX IF NOT EXISTS idx_appointment_fixture_patient_id ON ref.appointment_fixture (patient_id);
CREATE INDEX IF NOT EXISTS idx_allergy_fixture_patient_id ON ref.allergy_fixture (patient_id);
CREATE INDEX IF NOT EXISTS idx_department_parent_department_id ON hr.department (parent_department_id);
CREATE INDEX IF NOT EXISTS idx_employment_employee_period ON hr.employment (employee_id, start_at);
CREATE INDEX IF NOT EXISTS idx_employment_employee_id ON hr.employment (employee_id);
CREATE INDEX IF NOT EXISTS idx_employment_department_id ON hr.employment (department_id);
CREATE INDEX IF NOT EXISTS idx_license_certification_employee_id ON hr.license_certification (employee_id);
CREATE INDEX IF NOT EXISTS idx_personnel_decision_employee_id ON hr.personnel_decision (employee_id);
CREATE INDEX IF NOT EXISTS idx_integration_outbox_employee_id ON hr.integration_outbox (employee_id);
CREATE INDEX IF NOT EXISTS idx_user_account_employee_id ON sys.user_account (employee_id);
CREATE INDEX IF NOT EXISTS idx_role_permission_role_id ON sys.role_permission (role_id);
CREATE INDEX IF NOT EXISTS idx_role_permission_permission_id ON sys.role_permission (permission_id);
CREATE INDEX IF NOT EXISTS idx_role_assignment_user_id ON sys.role_assignment (user_id);
CREATE INDEX IF NOT EXISTS idx_role_assignment_role_id ON sys.role_assignment (role_id);
CREATE INDEX IF NOT EXISTS idx_session_user_id ON sys.session (user_id);
CREATE INDEX IF NOT EXISTS idx_audit_event_actor_user_id ON sys.audit_event (actor_user_id);
CREATE UNIQUE INDEX IF NOT EXISTS uidx_encounter_appointment ON emr.medical_encounter (appointment_id) WHERE appointment_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_medical_encounter_patient_id ON emr.medical_encounter (patient_id);
CREATE INDEX IF NOT EXISTS idx_medical_encounter_appointment_id_patient_id ON emr.medical_encounter (appointment_id, patient_id);
CREATE INDEX IF NOT EXISTS idx_medical_encounter_attending_employee_id ON emr.medical_encounter (attending_employee_id);
CREATE INDEX IF NOT EXISTS idx_clinical_observation_encounter_id ON emr.clinical_observation (encounter_id);
CREATE UNIQUE INDEX IF NOT EXISTS uidx_primary_confirmed_diagnosis ON emr.diagnosis (encounter_id) WHERE diagnosis_type = 'PRIMARY' AND diagnosis_status = 'CONFIRMED';
CREATE INDEX IF NOT EXISTS idx_diagnosis_encounter_id ON emr.diagnosis (encounter_id);
CREATE INDEX IF NOT EXISTS idx_diagnosis_confirmed_by ON emr.diagnosis (confirmed_by);
CREATE INDEX IF NOT EXISTS idx_medical_record_encounter_id ON emr.medical_record (encounter_id);
CREATE INDEX IF NOT EXISTS idx_medical_record_locked_by ON emr.medical_record (locked_by);
CREATE INDEX IF NOT EXISTS idx_record_version_record_id ON emr.record_version (record_id);
CREATE INDEX IF NOT EXISTS idx_addendum_record_id ON emr.addendum (record_id);
CREATE INDEX IF NOT EXISTS idx_addendum_confirmed_by ON emr.addendum (confirmed_by);
CREATE INDEX IF NOT EXISTS idx_medical_order_encounter_id ON emr.medical_order (encounter_id);
CREATE INDEX IF NOT EXISTS idx_medical_order_ordered_by ON emr.medical_order (ordered_by);
CREATE INDEX IF NOT EXISTS idx_clinical_result_order_id ON emr.clinical_result (order_id);
CREATE INDEX IF NOT EXISTS idx_prescription_encounter_id ON emr.prescription (encounter_id);
CREATE INDEX IF NOT EXISTS idx_prescription_prescriber_id ON emr.prescription (prescriber_id);
CREATE INDEX IF NOT EXISTS idx_prescription_item_prescription_id ON emr.prescription_item (prescription_id);
CREATE INDEX IF NOT EXISTS idx_prescription_item_medication_id ON emr.prescription_item (medication_id);

CREATE OR REPLACE FUNCTION sys.reject_audit_mutation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN RAISE EXCEPTION 'audit_event is append-only for application writes'; END $$;
DROP TRIGGER IF EXISTS trg_audit_immutable ON sys.audit_event;
CREATE TRIGGER trg_audit_immutable BEFORE UPDATE OR DELETE ON sys.audit_event
FOR EACH ROW EXECUTE FUNCTION sys.reject_audit_mutation();

CREATE OR REPLACE FUNCTION emr.protect_locked_record() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  IF OLD.record_status IN ('LOCKED','ARCHIVED') THEN
    IF TG_OP = 'DELETE' THEN RAISE EXCEPTION 'locked medical_record cannot be deleted'; END IF;
    IF OLD.record_status='LOCKED' AND NEW.record_status='ARCHIVED'
       AND NEW.record_id=OLD.record_id AND NEW.encounter_id=OLD.encounter_id
       AND NEW.current_version=OLD.current_version AND NEW.locked_at IS NOT DISTINCT FROM OLD.locked_at
       AND NEW.locked_by IS NOT DISTINCT FROM OLD.locked_by THEN RETURN NEW; END IF;
    RAISE EXCEPTION 'locked/archived medical_record cannot be modified';
  END IF;
  RETURN CASE WHEN TG_OP='DELETE' THEN OLD ELSE NEW END;
END $$;
DROP TRIGGER IF EXISTS trg_record_locked ON emr.medical_record;
CREATE TRIGGER trg_record_locked BEFORE UPDATE OR DELETE ON emr.medical_record
FOR EACH ROW EXECUTE FUNCTION emr.protect_locked_record();

CREATE OR REPLACE FUNCTION emr.protect_locked_version() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE parent_status varchar;
BEGIN
 SELECT record_status INTO parent_status FROM emr.medical_record WHERE record_id=OLD.record_id;
 IF parent_status IN ('LOCKED','ARCHIVED') THEN
  RAISE EXCEPTION 'locked record versions are immutable';
 END IF;
 RETURN CASE WHEN TG_OP='DELETE' THEN OLD ELSE NEW END;
END $$;
DROP TRIGGER IF EXISTS trg_version_immutable ON emr.record_version;
CREATE TRIGGER trg_version_immutable BEFORE UPDATE OR DELETE ON emr.record_version
FOR EACH ROW EXECUTE FUNCTION emr.protect_locked_version();

CREATE OR REPLACE FUNCTION emr.check_addendum_locked() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE parent_status varchar;
BEGIN
 SELECT record_status INTO parent_status FROM emr.medical_record WHERE record_id=NEW.record_id FOR SHARE;
 IF parent_status IS NULL OR parent_status NOT IN ('LOCKED','ARCHIVED') THEN
    RAISE EXCEPTION 'addendum requires locked/archived record';
 END IF;
 RETURN NEW;
END $$;
DROP TRIGGER IF EXISTS trg_addendum_parent ON emr.addendum;
CREATE TRIGGER trg_addendum_parent BEFORE INSERT ON emr.addendum
FOR EACH ROW EXECUTE FUNCTION emr.check_addendum_locked();
