-- V5: Complete Fixtures & Least-Privilege Policy for TC-SYS-001 .. TC-SYS-014

-- 1. Additional Role: ROLE_HR
INSERT INTO sys.role (role_id, role_code, display_name, created_at, updated_at, created_by, updated_by)
VALUES ('00000000-0000-0000-0000-000000000006', 'ROLE_HR', 'HR Specialist', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system')
ON CONFLICT (role_code) DO NOTHING;

-- 2. Additional Permissions: EMR_RECORD (READ, LOCK) and SYS_ROLE (ASSIGN)
INSERT INTO sys.permission (permission_id, resource_code, action_code, scope_code, created_at, updated_at, created_by, updated_by)
VALUES
  ('00000000-0000-0000-0005-000000000001', 'EMR_RECORD', 'READ', 'OWN',  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system'),
  ('00000000-0000-0000-0005-000000000002', 'EMR_RECORD', 'READ', 'DEPT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system'),
  ('00000000-0000-0000-0005-000000000003', 'EMR_RECORD', 'LOCK', 'OWN',  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system'),
  ('00000000-0000-0000-0006-000000000001', 'SYS_ROLE',   'ASSIGN', 'ALL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system')
ON CONFLICT (resource_code, action_code, scope_code) DO NOTHING;

-- 3. Enforce Least Privilege (TC-SYS-005, TC-SYS-006, TC-SYS-010):
-- IT Admin (ROLE_ADMIN) must NOT have EMR_ENCOUNTER or EMR_RECORD permissions!
DELETE FROM sys.role_permission
WHERE role_id = '00000000-0000-0000-0000-000000000001'
  AND permission_id IN (
    SELECT permission_id FROM sys.permission WHERE resource_code IN ('EMR_ENCOUNTER', 'EMR_RECORD', 'HR_EMPLOYEE')
  );

-- Assign SYS_ROLE:ASSIGN:ALL to ROLE_ADMIN
INSERT INTO sys.role_permission (role_id, permission_id, created_at, updated_at, created_by, updated_by)
VALUES ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0006-000000000001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system')
ON CONFLICT DO NOTHING;

-- Assign EMR_RECORD:READ:OWN and EMR_RECORD:LOCK:OWN to ROLE_DOCTOR
INSERT INTO sys.role_permission (role_id, permission_id, created_at, updated_at, created_by, updated_by)
VALUES
  ('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0005-000000000001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system'),
  ('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0005-000000000003', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system')
ON CONFLICT DO NOTHING;

-- Assign EMR_RECORD:READ:DEPT to ROLE_NURSE (Nurse cannot LOCK record -> TC-SYS-006)
INSERT INTO sys.role_permission (role_id, permission_id, created_at, updated_at, created_by, updated_by)
VALUES
  ('00000000-0000-0000-0000-000000000003', '00000000-0000-0000-0005-000000000002', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system')
ON CONFLICT DO NOTHING;

-- Assign HR_EMPLOYEE READ/WRITE to ROLE_HR
INSERT INTO sys.role_permission (role_id, permission_id, created_at, updated_at, created_by, updated_by)
VALUES
  ('00000000-0000-0000-0000-000000000006', '00000000-0000-0000-0002-000000000001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system'),
  ('00000000-0000-0000-0000-000000000006', '00000000-0000-0000-0002-000000000002', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system')
ON CONFLICT DO NOTHING;

-- 4. Synthetic HR Employees & Employment
INSERT INTO hr.employee (employee_id, employee_code, full_name, status, status_effective_at, created_by, updated_by)
VALUES
  ('00000000-0000-0000-0000-000000000201', 'EMP-NURSE-001', 'Synthetic Nurse', 'ACTIVE', CURRENT_TIMESTAMP, 'system', 'system'),
  ('00000000-0000-0000-0000-000000000202', 'EMP-HR-001', 'Synthetic HR Specialist', 'ACTIVE', CURRENT_TIMESTAMP, 'system', 'system'),
  ('00000000-0000-0000-0000-000000000203', 'EMP-UNAPPROVED-DOC', 'Clinician Without SYS Role', 'ACTIVE', CURRENT_TIMESTAMP, 'system', 'system')
ON CONFLICT (employee_code) DO NOTHING;

INSERT INTO hr.employment (employment_id, employee_id, department_id, job_title, start_at, decision_ref, created_by, updated_by)
VALUES
  ('00000000-0000-0000-0000-000000000301', '00000000-0000-0000-0000-000000000102', '00000000-0000-0000-0000-000000000101', 'Attending Doctor', CURRENT_TIMESTAMP - INTERVAL '30 days', 'DEC-EMP-001', 'system', 'system'),
  ('00000000-0000-0000-0000-000000000302', '00000000-0000-0000-0000-000000000203', '00000000-0000-0000-0000-000000000101', 'Senior Doctor (HR Title Only)', CURRENT_TIMESTAMP - INTERVAL '10 days', 'DEC-EMP-002', 'system', 'system')
ON CONFLICT (employment_id) DO NOTHING;

-- 5. Synthetic Test Accounts (DOC-TEST-001, IT-ADMIN-TEST-001, NURSE-TEST-001, HR-TEST-001, HR-UNAPPROVED-DOC)
INSERT INTO sys.user_account (user_id, employee_id, username, password_hash, account_status, revocation_epoch, created_at, updated_at, created_by, updated_by)
VALUES
  ('a0000000-0000-0000-0000-000000000011', NULL, 'DOC-TEST-001', '$2a$12$7x5JaVDcwkIm1eLx7VlIDuMxqR4k4rT8mI.CgP4UxLlPtnUm1NVXS', 'ACTIVE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system'),
  ('a0000000-0000-0000-0000-000000000012', NULL, 'IT-ADMIN-TEST-001', '$2a$12$7x5JaVDcwkIm1eLx7VlIDuMxqR4k4rT8mI.CgP4UxLlPtnUm1NVXS', 'ACTIVE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system'),
  ('a0000000-0000-0000-0000-000000000013', '00000000-0000-0000-0000-000000000201', 'NURSE-TEST-001', '$2a$12$7x5JaVDcwkIm1eLx7VlIDuMxqR4k4rT8mI.CgP4UxLlPtnUm1NVXS', 'ACTIVE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system'),
  ('a0000000-0000-0000-0000-000000000014', '00000000-0000-0000-0000-000000000202', 'HR-TEST-001', '$2a$12$7x5JaVDcwkIm1eLx7VlIDuMxqR4k4rT8mI.CgP4UxLlPtnUm1NVXS', 'ACTIVE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system'),
  ('a0000000-0000-0000-0000-000000000015', '00000000-0000-0000-0000-000000000203', 'HR-UNAPPROVED-DOC', '$2a$12$7x5JaVDcwkIm1eLx7VlIDuMxqR4k4rT8mI.CgP4UxLlPtnUm1NVXS', 'ACTIVE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system')
ON CONFLICT (username) DO NOTHING;

-- Assign roles to DOC-TEST-001, IT-ADMIN-TEST-001, NURSE-TEST-001, HR-TEST-001 (NOTE: HR-UNAPPROVED-DOC gets NO role assignment!)
INSERT INTO sys.role_assignment (assignment_id, user_id, role_id, start_at, end_at, approval_ref, approved_by, created_at, updated_at, created_by, updated_by)
VALUES
  ('b0000000-0000-0000-0000-000000000011', 'a0000000-0000-0000-0000-000000000011', '00000000-0000-0000-0000-000000000002', CURRENT_TIMESTAMP - INTERVAL '1 day', NULL, 'DEC-RBAC-BOOT-01', 'system', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system'),
  ('b0000000-0000-0000-0000-000000000012', 'a0000000-0000-0000-0000-000000000012', '00000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP - INTERVAL '1 day', NULL, 'DEC-RBAC-BOOT-02', 'system', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system'),
  ('b0000000-0000-0000-0000-000000000013', 'a0000000-0000-0000-0000-000000000013', '00000000-0000-0000-0000-000000000003', CURRENT_TIMESTAMP - INTERVAL '1 day', NULL, 'DEC-RBAC-BOOT-03', 'system', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system'),
  ('b0000000-0000-0000-0000-000000000014', 'a0000000-0000-0000-0000-000000000014', '00000000-0000-0000-0000-000000000006', CURRENT_TIMESTAMP - INTERVAL '1 day', NULL, 'DEC-RBAC-BOOT-04', 'system', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system')
ON CONFLICT (assignment_id) DO NOTHING;

-- 6. Synthetic EMR Encounters, Medical Records (EMR-TEST-001, EMR-TEST-002), and Medical Order
INSERT INTO emr.medical_encounter (encounter_id, patient_id, attending_employee_id, status, started_at, row_version, created_by, updated_by)
VALUES
  ('e0000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000103', '00000000-0000-0000-0000-000000000102', 'IN_PROGRESS', CURRENT_TIMESTAMP - INTERVAL '2 hours', 1, 'system', 'system'),
  ('e0000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000103', '00000000-0000-0000-0000-000000000102', 'IN_PROGRESS', CURRENT_TIMESTAMP - INTERVAL '1 hour', 1, 'system', 'system')
ON CONFLICT (encounter_id) DO NOTHING;

INSERT INTO emr.medical_record (record_id, encounter_id, record_status, current_version, row_version, created_by, updated_by)
VALUES
  ('e0000000-0000-0000-0000-000000000101', 'e0000000-0000-0000-0000-000000000001', 'DRAFT', 1, 1, 'system', 'system'),
  ('e0000000-0000-0000-0000-000000000102', 'e0000000-0000-0000-0000-000000000002', 'DRAFT', 1, 1, 'system', 'system')
ON CONFLICT (record_id) DO NOTHING;

INSERT INTO emr.medical_order (order_id, encounter_id, ordered_by, order_type, order_status, external_order_ref, ordered_at, created_by, updated_by)
VALUES
  ('e0000000-0000-0000-0000-000000000201', 'e0000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000102', 'LAB', 'ORDERED', 'ORD-CLS-001', CURRENT_TIMESTAMP, 'system', 'system')
ON CONFLICT (order_id) DO NOTHING;

-- 7. Seed Audit Event AUD-TEST-001 (for TC-SYS-011 & TC-SYS-012 immutability tests)
INSERT INTO sys.audit_event (audit_id, actor_user_id, actor_ref, target_type, target_id, action, outcome, change_ref, happened_at, safe_metadata, created_by, updated_by)
VALUES (
  'f0000000-0000-0000-0000-000000000001',
  'a0000000-0000-0000-0000-000000000011',
  'DOC-TEST-001',
  'EMR_RECORD',
  'EMR-TEST-001',
  'CREATE_DRAFT',
  'SUCCESS',
  'INIT-SEED',
  CURRENT_TIMESTAMP - INTERVAL '30 minutes',
  '{"fixture":"AUD-TEST-001"}'::jsonb,
  'system',
  'system'
)
ON CONFLICT (audit_id) DO NOTHING;
