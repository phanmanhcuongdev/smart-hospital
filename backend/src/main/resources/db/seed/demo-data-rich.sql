-- demo-data-rich.sql
-- Purpose: a LARGE, realistic demo dataset for the Smart Hospital test database.
-- It adds departments, employees, CCHN licences, assignments, personnel
-- decisions, patients, allergies, a drug catalogue, encounters, vitals,
-- diagnoses, CLS orders/results, prescriptions and medical records on top
-- of the existing demo department (DEMO), demo doctor (EMP-DEMO-001) and
-- the single WAITING smoke-test encounter. It never touches Flyway
-- migrations and never modifies existing rows.
--
-- Run (inside this repo, with the smart-hospital-db-1 container up):
--   docker exec -i smart-hospital-db-1 psql -U smarthms -d smarthms \
--     -v ON_ERROR_STOP=1 -f backend/src/main/resources/db/seed/demo-data-rich.sql
-- Or from the host (PostgreSQL is on 127.0.0.1:5439):
--   psql "host=127.0.0.1 port=5439 dbname=smarthms user=smarthms" \
--     -v ON_ERROR_STOP=1 -f backend/src/main/resources/db/seed/demo-data-rich.sql
--
-- Re-run safely: the whole script runs in one transaction, every row uses
-- a deterministic UUID built from a fixed numeric base, and every INSERT
-- uses ON CONFLICT DO NOTHING on its primary/unique key, so re-running
-- adds nothing and errors on nothing.
--
-- Schema notes learned from V1-V3 and the running database:
-- * Patients live in ref.patient_fixture (display_name only; the schema has
--   no date-of-birth/gender column for patients) and must be VERIFIED so an
--   encounter can be opened for them.
-- * Allergy status is UNKNOWN / NONE_CONFIRMED / HAS_ALLERGY (not KNOWN).
-- * The drug catalogue is ref.medication_fixture; it held one row
--   ("Synthetic Medication"), so this script adds 12 real drugs and every
--   prescription item uses only a catalogue drug.
-- * CCHN validity is computed by the backend from verification_status and
--   valid_until: REJECTED/REVOKED -> REJECTED, UNVERIFIED -> UNVERIFIED,
--   valid_until < today -> EXPIRED, within 30 days -> EXPIRING_SOON,
--   otherwise VALID. Prescribers need VALID or EXPIRING_SOON.
-- * Encounters become visible to the demo doctor when
--   attending_employee_id = '00000000-0000-0000-0000-000000000102'
--   (employee EMP-DEMO-001); about 40 of the 120 encounters use him.
-- * sys.user_account is empty, so locked_by / confirmed_by stay NULL:
--   LOCKED/ARCHIVED records only need locked_at, and no addendum is seeded
--   because a CONFIRMED addendum would need a user_account row.
-- * appointment_id is left NULL on every encounter, so no
--   ref.appointment_fixture rows are needed.
-- * Clinical result rows carry no patient_id column; the patient identity
--   is stored in result_payload.patientId and always matches the order's
--   encounter patient, so no result is ever QUARANTINED.

BEGIN;

-- 1. Departments: 11 new ones in addition to the existing DEMO department.
INSERT INTO hr.department
  (department_id, department_code, display_name, department_type, active,
   created_at, created_by, updated_at, updated_by)
VALUES
  (('10000000-0000-0000-0000-' || lpad(to_hex(1001), 12, '0'))::uuid, 'NOI-TH', 'Nội tổng hợp', 'CLINICAL', true, CURRENT_TIMESTAMP, 'demo-hr', CURRENT_TIMESTAMP, 'demo-hr'),
  (('10000000-0000-0000-0000-' || lpad(to_hex(1002), 12, '0'))::uuid, 'NGOAI', 'Ngoại tổng hợp', 'CLINICAL', true, CURRENT_TIMESTAMP, 'demo-hr', CURRENT_TIMESTAMP, 'demo-hr'),
  (('10000000-0000-0000-0000-' || lpad(to_hex(1003), 12, '0'))::uuid, 'NHI', 'Nhi khoa', 'CLINICAL', true, CURRENT_TIMESTAMP, 'demo-hr', CURRENT_TIMESTAMP, 'demo-hr'),
  (('10000000-0000-0000-0000-' || lpad(to_hex(1004), 12, '0'))::uuid, 'SAN', 'Sản khoa', 'CLINICAL', true, CURRENT_TIMESTAMP, 'demo-hr', CURRENT_TIMESTAMP, 'demo-hr'),
  (('10000000-0000-0000-0000-' || lpad(to_hex(1005), 12, '0'))::uuid, 'TIM-MACH', 'Tim mạch', 'CLINICAL', true, CURRENT_TIMESTAMP, 'demo-hr', CURRENT_TIMESTAMP, 'demo-hr'),
  (('10000000-0000-0000-0000-' || lpad(to_hex(1006), 12, '0'))::uuid, 'HO-HAP', 'Hô hấp', 'CLINICAL', true, CURRENT_TIMESTAMP, 'demo-hr', CURRENT_TIMESTAMP, 'demo-hr'),
  (('10000000-0000-0000-0000-' || lpad(to_hex(1007), 12, '0'))::uuid, 'THAN-KINH', 'Thần kinh', 'CLINICAL', true, CURRENT_TIMESTAMP, 'demo-hr', CURRENT_TIMESTAMP, 'demo-hr'),
  (('10000000-0000-0000-0000-' || lpad(to_hex(1008), 12, '0'))::uuid, 'CAP-CUU', 'Cấp cứu', 'CLINICAL', true, CURRENT_TIMESTAMP, 'demo-hr', CURRENT_TIMESTAMP, 'demo-hr'),
  (('10000000-0000-0000-0000-' || lpad(to_hex(1009), 12, '0'))::uuid, 'CDHA', 'Chẩn đoán hình ảnh', 'DIAGNOSTIC', true, CURRENT_TIMESTAMP, 'demo-hr', CURRENT_TIMESTAMP, 'demo-hr'),
  (('10000000-0000-0000-0000-' || lpad(to_hex(1010), 12, '0'))::uuid, 'DUOC', 'Dược', 'PHARMACY', true, CURRENT_TIMESTAMP, 'demo-hr', CURRENT_TIMESTAMP, 'demo-hr'),
  (('10000000-0000-0000-0000-' || lpad(to_hex(1011), 12, '0'))::uuid, 'HC-NS', 'Hành chính - Nhân sự', 'ADMIN', true, CURRENT_TIMESTAMP, 'demo-hr', CURRENT_TIMESTAMP, 'demo-hr')
ON CONFLICT (department_code) DO NOTHING;

-- 2. Role profiles referenced by employment rows.
INSERT INTO hr.role_profiles
  (role_profile_id, title_name, professional_group, qualification, description,
   created_at, created_by, updated_at, updated_by)
VALUES
  (('10000000-0000-0000-0000-' || lpad(to_hex(2001), 12, '0'))::uuid, 'Bác sĩ đa khoa', 'CLINICAL', 'Bác sĩ y khoa', 'Khám và điều trị bệnh nhân nội trú, ngoại trú.', CURRENT_TIMESTAMP, 'demo-hr', CURRENT_TIMESTAMP, 'demo-hr'),
  (('10000000-0000-0000-0000-' || lpad(to_hex(2002), 12, '0'))::uuid, 'Điều dưỡng viên', 'NURSING', 'Cử nhân điều dưỡng', 'Chăm sóc bệnh nhân và hỗ trợ bác sĩ.', CURRENT_TIMESTAMP, 'demo-hr', CURRENT_TIMESTAMP, 'demo-hr'),
  (('10000000-0000-0000-0000-' || lpad(to_hex(2003), 12, '0'))::uuid, 'Kỹ thuật viên CLS', 'DIAGNOSTIC', 'Kỹ thuật viên xét nghiệm / chẩn đoán hình ảnh', 'Thực hiện xét nghiệm và chẩn đoán hình ảnh.', CURRENT_TIMESTAMP, 'demo-hr', CURRENT_TIMESTAMP, 'demo-hr'),
  (('10000000-0000-0000-0000-' || lpad(to_hex(2004), 12, '0'))::uuid, 'Nhân viên hồ sơ bệnh án', 'ADMIN', 'Trung cấp y tế', 'Quản lý hồ sơ bệnh án và lưu trữ.', CURRENT_TIMESTAMP, 'demo-hr', CURRENT_TIMESTAMP, 'demo-hr'),
  (('10000000-0000-0000-0000-' || lpad(to_hex(2005), 12, '0'))::uuid, 'Lễ tân tiếp đón', 'ADMIN', 'Trung cấp', 'Tiếp đón, hướng dẫn bệnh nhân.', CURRENT_TIMESTAMP, 'demo-hr', CURRENT_TIMESTAMP, 'demo-hr'),
  (('10000000-0000-0000-0000-' || lpad(to_hex(2006), 12, '0'))::uuid, 'Chuyên viên nhân sự', 'HR', 'Cử nhân quản trị nhân lực', 'Quản lý hồ sơ nhân sự và quyết định nhân sự.', CURRENT_TIMESTAMP, 'demo-hr', CURRENT_TIMESTAMP, 'demo-hr')
ON CONFLICT (title_name) DO NOTHING;

-- 3. Employees: 48 new employees (EMP-2026-001..048). Roles: 1-16 doctors,
-- 17-30 nurses, 31-36 CLS technicians, 37-40 records, 41-44 reception,
-- 45-48 HR. Status mix: 36 ACTIVE, 6 ONBOARDING, 4 SUSPENDED, 2 TERMINATED.
INSERT INTO hr.employee
  (employee_id, employee_code, full_name, gender, birth_date, hire_date,
   phone, email, address, status, status_effective_at, row_version,
   created_at, created_by, updated_at, updated_by)
SELECT
  ('10000000-0000-0000-0000-' || lpad(to_hex(3000 + s.i), 12, '0'))::uuid,
  'EMP-2026-' || lpad(s.i::text, 3, '0'),
  (ARRAY['Nguyễn','Trần','Lê','Phạm','Hoàng','Huỳnh','Phan','Vũ','Võ','Đặng','Bùi','Đỗ','Hồ','Ngô','Dương','Lý'])[(s.i * 5) % 16 + 1]
    || ' ' || (ARRAY['Văn','Thị','Hữu','Minh','Quốc','Thu','Đình','Ngọc'])[(s.i * 3) % 8 + 1]
    || ' ' || (ARRAY['Anh','Bảo','Chi','Dũng','Hà','Hoa','Hùng','Khánh','Lan','Linh','Minh','Nam','Ngọc','Phúc','Quang','Sơn','Thảo','Thu','Trang','Tuấn','Tuyết','Vân','Việt','Yến','Đức','Hải','Long','Phương','Quỳnh','Tâm'])[(s.i * 7) % 30 + 1],
  CASE WHEN s.i % 2 = 0 THEN 'FEMALE' ELSE 'MALE' END,
  DATE '1970-01-01' + ((s.i * 373) % 11000),
  s.hire_date,
  '090' || lpad((1000000 + s.i * 137)::text, 7, '0'),
  'emp' || lpad(s.i::text, 3, '0') || '@smarthospital.local',
  (ARRAY['Số 12 Nguyễn Văn Linh, Quận 7, TP.HCM','Số 45 Trần Hưng Đạo, Quận 1, TP.HCM','Số 8 Lê Lợi, Quận Bình Thạnh, TP.HCM','Số 120 Phạm Văn Đồng, Quận Thủ Đức, TP.HCM','Số 3 Hai Bà Trưng, Quận 3, TP.HCM'])[(s.i % 5) + 1],
  s.status,
  CASE WHEN s.status = 'TERMINATED' THEN CURRENT_TIMESTAMP - interval '45 days'
       WHEN s.status = 'SUSPENDED' THEN CURRENT_TIMESTAMP - interval '20 days'
       ELSE s.hire_date::timestamp END,
  1, CURRENT_TIMESTAMP, 'demo-hr', CURRENT_TIMESTAMP, 'demo-hr'
FROM (
  SELECT i, DATE '2016-01-01' + ((i * 211) % 3500) AS hire_date,
    CASE
      WHEN i BETWEEN 1 AND 16 THEN CASE WHEN i = 5 THEN 'ONBOARDING' WHEN i = 11 THEN 'SUSPENDED' ELSE 'ACTIVE' END
      WHEN i BETWEEN 17 AND 30 THEN CASE WHEN i = 20 THEN 'ONBOARDING' WHEN i = 25 THEN 'SUSPENDED' WHEN i = 30 THEN 'TERMINATED' ELSE 'ACTIVE' END
      WHEN i BETWEEN 31 AND 36 THEN CASE WHEN i = 33 THEN 'ONBOARDING' WHEN i = 36 THEN 'SUSPENDED' ELSE 'ACTIVE' END
      WHEN i BETWEEN 37 AND 40 THEN CASE WHEN i = 39 THEN 'ONBOARDING' WHEN i = 40 THEN 'SUSPENDED' ELSE 'ACTIVE' END
      WHEN i BETWEEN 41 AND 44 THEN CASE WHEN i = 43 THEN 'ONBOARDING' ELSE 'ACTIVE' END
      ELSE CASE WHEN i = 47 THEN 'ONBOARDING' WHEN i = 48 THEN 'TERMINATED' ELSE 'ACTIVE' END
    END AS status
  FROM generate_series(1, 48) AS g(i)
) s
ON CONFLICT (employee_code) DO NOTHING;

-- 4. Employment assignments: one current assignment per employee (the
-- demo doctor gets one in the DEMO department), plus ended historical
-- assignments for employees 1-8. No employee has two active assignments.
INSERT INTO hr.employment
  (employment_id, employee_id, department_id, job_title, role_profile_id,
   start_at, end_at, decision_ref, created_at, created_by, updated_at, updated_by)
SELECT
  ('10000000-0000-0000-0000-' || lpad(to_hex(4000 + s.i), 12, '0'))::uuid,
  ('10000000-0000-0000-0000-' || lpad(to_hex(3000 + s.i), 12, '0'))::uuid,
  ('10000000-0000-0000-0000-' || lpad(to_hex(1000 + s.dept_no), 12, '0'))::uuid,
  s.job_title,
  ('10000000-0000-0000-0000-' || lpad(to_hex(2000 + s.role_no), 12, '0'))::uuid,
  s.hire_date::timestamp,
  CASE WHEN s.status = 'TERMINATED' THEN CURRENT_TIMESTAMP - interval '45 days' ELSE NULL END,
  NULL, CURRENT_TIMESTAMP, 'demo-hr', CURRENT_TIMESTAMP, 'demo-hr'
FROM (
  SELECT i, DATE '2016-01-01' + ((i * 211) % 3500) AS hire_date,
    CASE
      WHEN i BETWEEN 1 AND 16 THEN CASE WHEN i = 5 THEN 'ONBOARDING' WHEN i = 11 THEN 'SUSPENDED' ELSE 'ACTIVE' END
      WHEN i BETWEEN 17 AND 30 THEN CASE WHEN i = 20 THEN 'ONBOARDING' WHEN i = 25 THEN 'SUSPENDED' WHEN i = 30 THEN 'TERMINATED' ELSE 'ACTIVE' END
      WHEN i BETWEEN 31 AND 36 THEN CASE WHEN i = 33 THEN 'ONBOARDING' WHEN i = 36 THEN 'SUSPENDED' ELSE 'ACTIVE' END
      WHEN i BETWEEN 37 AND 40 THEN CASE WHEN i = 39 THEN 'ONBOARDING' WHEN i = 40 THEN 'SUSPENDED' ELSE 'ACTIVE' END
      WHEN i BETWEEN 41 AND 44 THEN CASE WHEN i = 43 THEN 'ONBOARDING' ELSE 'ACTIVE' END
      ELSE CASE WHEN i = 47 THEN 'ONBOARDING' WHEN i = 48 THEN 'TERMINATED' ELSE 'ACTIVE' END
    END AS status,
    CASE
      WHEN i BETWEEN 1 AND 16 THEN ((i - 1) % 8) + 1
      WHEN i BETWEEN 17 AND 30 THEN ((i - 17) % 8) + 1
      WHEN i BETWEEN 31 AND 34 THEN 9
      WHEN i BETWEEN 35 AND 36 THEN 8
      WHEN i BETWEEN 37 AND 40 THEN 1
      WHEN i BETWEEN 41 AND 44 THEN 8
      ELSE 11
    END AS dept_no,
    CASE
      WHEN i BETWEEN 1 AND 16 THEN CASE WHEN i % 4 = 0 THEN 'Bác sĩ chuyên khoa' ELSE 'Bác sĩ' END
      WHEN i BETWEEN 17 AND 30 THEN 'Điều dưỡng'
      WHEN i BETWEEN 31 AND 36 THEN 'Kỹ thuật viên CLS'
      WHEN i BETWEEN 37 AND 40 THEN 'Nhân viên hồ sơ bệnh án'
      WHEN i BETWEEN 41 AND 44 THEN 'Lễ tân'
      ELSE 'Chuyên viên nhân sự'
    END AS job_title,
    CASE
      WHEN i BETWEEN 1 AND 16 THEN 1
      WHEN i BETWEEN 17 AND 30 THEN 2
      WHEN i BETWEEN 31 AND 36 THEN 3
      WHEN i BETWEEN 37 AND 40 THEN 4
      WHEN i BETWEEN 41 AND 44 THEN 5
      ELSE 6
    END AS role_no
  FROM generate_series(1, 48) AS g(i)
) s
ON CONFLICT (employment_id) DO NOTHING;

-- Current assignment of the demo doctor in the DEMO department.
INSERT INTO hr.employment
  (employment_id, employee_id, department_id, job_title, role_profile_id,
   start_at, end_at, decision_ref, created_at, created_by, updated_at, updated_by)
VALUES
  (('10000000-0000-0000-0000-' || lpad(to_hex(4100), 12, '0'))::uuid,
   '00000000-0000-0000-0000-000000000102',
   '00000000-0000-0000-0000-000000000101',
   'Bác sĩ',
   ('10000000-0000-0000-0000-' || lpad(to_hex(2001), 12, '0'))::uuid,
   DATE '2024-01-01', NULL, NULL, CURRENT_TIMESTAMP, 'demo-hr', CURRENT_TIMESTAMP, 'demo-hr')
ON CONFLICT (employment_id) DO NOTHING;

-- Ended historical assignments for employees 1-8 (they moved departments).
INSERT INTO hr.employment
  (employment_id, employee_id, department_id, job_title, role_profile_id,
   start_at, end_at, decision_ref, created_at, created_by, updated_at, updated_by)
SELECT
  ('10000000-0000-0000-0000-' || lpad(to_hex(4048 + k), 12, '0'))::uuid,
  ('10000000-0000-0000-0000-' || lpad(to_hex(3000 + k), 12, '0'))::uuid,
  ('10000000-0000-0000-0000-' || lpad(to_hex(1008), 12, '0'))::uuid,
  CASE WHEN k % 4 = 0 THEN 'Bác sĩ chuyên khoa' ELSE 'Bác sĩ' END,
  ('10000000-0000-0000-0000-' || lpad(to_hex(2001), 12, '0'))::uuid,
  (DATE '2016-01-01' + ((k * 211) % 3500) - 1200)::timestamp,
  (DATE '2016-01-01' + ((k * 211) % 3500) - 30)::timestamp,
  NULL, CURRENT_TIMESTAMP, 'demo-hr', CURRENT_TIMESTAMP, 'demo-hr'
FROM generate_series(1, 8) AS g(k)
ON CONFLICT (employment_id) DO NOTHING;

-- 5. CCHN licences for clinical staff (doctors 1-16, nurses 17-30, CLS 31-36)
-- plus one for the demo doctor. Validity mix: 20 VALID (incl. the demo
-- doctor), 8 EXPIRING_SOON (expiry = CURRENT_DATE + 5..25 days), 5 EXPIRED,
-- 3 UNVERIFIED, 1 REJECTED. Every ACTIVE doctor holds VALID or
-- EXPIRING_SOON, so every prescriber used below passes the backend's
-- hasValidCchn check (VALID or EXPIRING_SOON).
INSERT INTO hr.license_certification
  (credential_id, employee_id, credential_type, credential_no, issuer,
   scope_note, valid_from, valid_until, verification_status, source_ref,
   created_at, created_by, updated_at, updated_by)
SELECT
  ('10000000-0000-0000-0000-' || lpad(to_hex(5000 + s.i), 12, '0'))::uuid,
  ('10000000-0000-0000-0000-' || lpad(to_hex(3000 + s.i), 12, '0'))::uuid,
  'CCHN',
  'CCHN-' || lpad(s.i::text, 4, '0'),
  'Bộ Y tế',
  s.scope_note, s.valid_from, s.valid_until, s.verification_status,
  'SEED-DEMO',
  CURRENT_TIMESTAMP - interval '100 days', 'demo-hr',
  CURRENT_TIMESTAMP - interval '100 days', 'demo-hr'
FROM (
  SELECT i,
    CASE WHEN i BETWEEN 1 AND 16 THEN 'Khám chữa bệnh đa khoa'
         WHEN i BETWEEN 17 AND 30 THEN 'Chăm sóc điều dưỡng'
         ELSE 'Kỹ thuật chẩn đoán hình ảnh và xét nghiệm' END AS scope_note,
    CASE
      WHEN i IN (5, 20, 33) THEN 'UNVERIFIED'
      WHEN i = 25 THEN 'REJECTED'
      ELSE 'VERIFIED'
    END AS verification_status,
    CASE
      WHEN i IN (11, 18, 27, 30, 36) THEN CURRENT_DATE - 1200
      WHEN i IN (2, 6, 10, 14, 19, 24, 29, 34) THEN CURRENT_DATE - 800
      WHEN i IN (5, 20, 25, 33) THEN CURRENT_DATE - 400
      ELSE CURRENT_DATE - 800
    END AS valid_from,
    CASE
      WHEN i IN (11, 18, 27, 30, 36) THEN CURRENT_DATE - (i % 90 + 10)
      WHEN i IN (2, 6, 10, 14, 19, 24, 29, 34) THEN CURRENT_DATE + (5 + i % 21)
      ELSE CURRENT_DATE + 730
    END AS valid_until
  FROM generate_series(1, 36) AS g(i)
) s
ON CONFLICT (credential_id) DO NOTHING;

-- The demo doctor's own VALID, VERIFIED CCHN, so his prescriptions are valid.
INSERT INTO hr.license_certification
  (credential_id, employee_id, credential_type, credential_no, issuer,
   scope_note, valid_from, valid_until, verification_status, source_ref,
   created_at, created_by, updated_at, updated_by)
VALUES
  (('10000000-0000-0000-0000-' || lpad(to_hex(5100), 12, '0'))::uuid,
   '00000000-0000-0000-0000-000000000102', 'CCHN', 'CCHN-DEMO-001', 'Bộ Y tế',
   'Khám chữa bệnh đa khoa', DATE '2024-01-15', CURRENT_DATE + 730, 'VERIFIED',
   'SEED-DEMO', CURRENT_TIMESTAMP - interval '100 days', 'demo-hr',
   CURRENT_TIMESTAMP - interval '100 days', 'demo-hr')
ON CONFLICT (credential_id) DO NOTHING;

-- 6. Personnel decisions: 3 already APPROVED/APPLIED (their employees'
-- current status matches the decision) and 3 left DRAFT/PENDING with
-- created_by 'demo-hr', so demo-hr-approver can approve them in the UI.
-- There is no requester column on the table; created_by is the requester.
-- The approver differs from the requester (four-eyes rule in the backend).
INSERT INTO hr.personnel_decision
  (decision_id, decision_ref, employee_id, decision_type, decision_status,
   execution_status, effective_at, approved_at, approved_by, reason,
   created_at, created_by, updated_at, updated_by)
VALUES
  (('10000000-0000-0000-0000-' || lpad(to_hex(6001), 12, '0'))::uuid, 'DEC-2026-001',
   ('10000000-0000-0000-0000-' || lpad(to_hex(3003), 12, '0'))::uuid, 'ACTIVATE', 'APPROVED', 'APPLIED',
   (CURRENT_DATE - 90)::timestamp, (CURRENT_DATE - 92)::timestamp, 'demo-hr-approver',
   'Hết thời gian thử việc, chính thức nhận việc.', (CURRENT_DATE - 95)::timestamp, 'demo-hr', CURRENT_TIMESTAMP, 'demo-hr'),
  (('10000000-0000-0000-0000-' || lpad(to_hex(6002), 12, '0'))::uuid, 'DEC-2026-002',
   ('10000000-0000-0000-0000-' || lpad(to_hex(3025), 12, '0'))::uuid, 'SUSPEND', 'APPROVED', 'APPLIED',
   (CURRENT_DATE - 60)::timestamp, (CURRENT_DATE - 62)::timestamp, 'demo-hr-approver',
   'Tạm đình chỉ công tác để xác minh vi phạm quy trình.', (CURRENT_DATE - 65)::timestamp, 'demo-hr', CURRENT_TIMESTAMP, 'demo-hr'),
  (('10000000-0000-0000-0000-' || lpad(to_hex(6003), 12, '0'))::uuid, 'DEC-2026-003',
   ('10000000-0000-0000-0000-' || lpad(to_hex(3030), 12, '0'))::uuid, 'TERMINATE', 'APPROVED', 'APPLIED',
   (CURRENT_DATE - 30)::timestamp, (CURRENT_DATE - 32)::timestamp, 'demo-hr-approver',
   'Chấm dứt hợp đồng theo nguyện vọng của nhân viên.', (CURRENT_DATE - 35)::timestamp, 'demo-hr', CURRENT_TIMESTAMP, 'demo-hr'),
  (('10000000-0000-0000-0000-' || lpad(to_hex(6004), 12, '0'))::uuid, 'DEC-2026-004',
   ('10000000-0000-0000-0000-' || lpad(to_hex(3007), 12, '0'))::uuid, 'TRANSFER', 'DRAFT', 'PENDING',
   (CURRENT_DATE + 7)::timestamp, NULL, NULL,
   'Điều chuyển sang khoa Cấp cứu do thiếu nhân lực.', CURRENT_TIMESTAMP - interval '2 days', 'demo-hr', CURRENT_TIMESTAMP, 'demo-hr'),
  (('10000000-0000-0000-0000-' || lpad(to_hex(6005), 12, '0'))::uuid, 'DEC-2026-005',
   ('10000000-0000-0000-0000-' || lpad(to_hex(3021), 12, '0'))::uuid, 'SUSPEND', 'DRAFT', 'PENDING',
   (CURRENT_DATE + 3)::timestamp, NULL, NULL,
   'Đề xuất tạm đình chỉ công tác để xác minh sự cố.', CURRENT_TIMESTAMP - interval '2 days', 'demo-hr', CURRENT_TIMESTAMP, 'demo-hr'),
  (('10000000-0000-0000-0000-' || lpad(to_hex(6006), 12, '0'))::uuid, 'DEC-2026-006',
   ('10000000-0000-0000-0000-' || lpad(to_hex(3033), 12, '0'))::uuid, 'ACTIVATE', 'DRAFT', 'PENDING',
   (CURRENT_DATE + 5)::timestamp, NULL, NULL,
   'Đề xuất chính thức nhận việc sau thời gian thử việc.', CURRENT_TIMESTAMP - interval '2 days', 'demo-hr', CURRENT_TIMESTAMP, 'demo-hr')
ON CONFLICT (decision_ref) DO NOTHING;

-- 7. Employment status history: the 3 applied decisions above plus the
-- other status changes (suspensions, terminations, onboardings).
INSERT INTO hr.employee_status_history
  (history_id, employee_id, old_status, new_status, effective_at, decision_id,
   created_at, created_by, updated_at, updated_by)
VALUES
  (('10000000-0000-0000-0000-' || lpad(to_hex(7001), 12, '0'))::uuid, ('10000000-0000-0000-0000-' || lpad(to_hex(3003), 12, '0'))::uuid, 'ONBOARDING', 'ACTIVE', (CURRENT_DATE - 90)::timestamp, ('10000000-0000-0000-0000-' || lpad(to_hex(6001), 12, '0'))::uuid, (CURRENT_DATE - 90)::timestamp, 'demo-hr-approver', CURRENT_TIMESTAMP, 'demo-hr-approver'),
  (('10000000-0000-0000-0000-' || lpad(to_hex(7002), 12, '0'))::uuid, ('10000000-0000-0000-0000-' || lpad(to_hex(3025), 12, '0'))::uuid, 'ACTIVE', 'SUSPENDED', (CURRENT_DATE - 60)::timestamp, ('10000000-0000-0000-0000-' || lpad(to_hex(6002), 12, '0'))::uuid, (CURRENT_DATE - 60)::timestamp, 'demo-hr-approver', CURRENT_TIMESTAMP, 'demo-hr-approver'),
  (('10000000-0000-0000-0000-' || lpad(to_hex(7003), 12, '0'))::uuid, ('10000000-0000-0000-0000-' || lpad(to_hex(3030), 12, '0'))::uuid, 'ACTIVE', 'TERMINATED', (CURRENT_DATE - 30)::timestamp, ('10000000-0000-0000-0000-' || lpad(to_hex(6003), 12, '0'))::uuid, (CURRENT_DATE - 30)::timestamp, 'demo-hr-approver', CURRENT_TIMESTAMP, 'demo-hr-approver'),
  (('10000000-0000-0000-0000-' || lpad(to_hex(7004), 12, '0'))::uuid, ('10000000-0000-0000-0000-' || lpad(to_hex(3011), 12, '0'))::uuid, 'ACTIVE', 'SUSPENDED', (CURRENT_DATE - 25)::timestamp, NULL, (CURRENT_DATE - 25)::timestamp, 'demo-hr', CURRENT_TIMESTAMP, 'demo-hr'),
  (('10000000-0000-0000-0000-' || lpad(to_hex(7005), 12, '0'))::uuid, ('10000000-0000-0000-0000-' || lpad(to_hex(3036), 12, '0'))::uuid, 'ACTIVE', 'SUSPENDED', (CURRENT_DATE - 18)::timestamp, NULL, (CURRENT_DATE - 18)::timestamp, 'demo-hr', CURRENT_TIMESTAMP, 'demo-hr'),
  (('10000000-0000-0000-0000-' || lpad(to_hex(7006), 12, '0'))::uuid, ('10000000-0000-0000-0000-' || lpad(to_hex(3040), 12, '0'))::uuid, 'ACTIVE', 'SUSPENDED', (CURRENT_DATE - 12)::timestamp, NULL, (CURRENT_DATE - 12)::timestamp, 'demo-hr', CURRENT_TIMESTAMP, 'demo-hr'),
  (('10000000-0000-0000-0000-' || lpad(to_hex(7007), 12, '0'))::uuid, ('10000000-0000-0000-0000-' || lpad(to_hex(3048), 12, '0'))::uuid, 'ACTIVE', 'TERMINATED', (CURRENT_DATE - 45)::timestamp, NULL, (CURRENT_DATE - 45)::timestamp, 'demo-hr', CURRENT_TIMESTAMP, 'demo-hr'),
  (('10000000-0000-0000-0000-' || lpad(to_hex(7008), 12, '0'))::uuid, ('10000000-0000-0000-0000-' || lpad(to_hex(3005), 12, '0'))::uuid, NULL, 'ONBOARDING', (DATE '2016-01-01' + ((5 * 211) % 3500))::timestamp, NULL, (DATE '2016-01-01' + ((5 * 211) % 3500))::timestamp, 'demo-hr', CURRENT_TIMESTAMP, 'demo-hr'),
  (('10000000-0000-0000-0000-' || lpad(to_hex(7009), 12, '0'))::uuid, ('10000000-0000-0000-0000-' || lpad(to_hex(3020), 12, '0'))::uuid, NULL, 'ONBOARDING', (DATE '2016-01-01' + ((20 * 211) % 3500))::timestamp, NULL, (DATE '2016-01-01' + ((20 * 211) % 3500))::timestamp, 'demo-hr', CURRENT_TIMESTAMP, 'demo-hr'),
  (('10000000-0000-0000-0000-' || lpad(to_hex(7010), 12, '0'))::uuid, ('10000000-0000-0000-0000-' || lpad(to_hex(3033), 12, '0'))::uuid, NULL, 'ONBOARDING', (DATE '2016-01-01' + ((33 * 211) % 3500))::timestamp, NULL, (DATE '2016-01-01' + ((33 * 211) % 3500))::timestamp, 'demo-hr', CURRENT_TIMESTAMP, 'demo-hr'),
  (('10000000-0000-0000-0000-' || lpad(to_hex(7011), 12, '0'))::uuid, ('10000000-0000-0000-0000-' || lpad(to_hex(3039), 12, '0'))::uuid, NULL, 'ONBOARDING', (DATE '2016-01-01' + ((39 * 211) % 3500))::timestamp, NULL, (DATE '2016-01-01' + ((39 * 211) % 3500))::timestamp, 'demo-hr', CURRENT_TIMESTAMP, 'demo-hr'),
  (('10000000-0000-0000-0000-' || lpad(to_hex(7012), 12, '0'))::uuid, ('10000000-0000-0000-0000-' || lpad(to_hex(3043), 12, '0'))::uuid, NULL, 'ONBOARDING', (DATE '2016-01-01' + ((43 * 211) % 3500))::timestamp, NULL, (DATE '2016-01-01' + ((43 * 211) % 3500))::timestamp, 'demo-hr', CURRENT_TIMESTAMP, 'demo-hr'),
  (('10000000-0000-0000-0000-' || lpad(to_hex(7013), 12, '0'))::uuid, ('10000000-0000-0000-0000-' || lpad(to_hex(3047), 12, '0'))::uuid, NULL, 'ONBOARDING', (DATE '2016-01-01' + ((47 * 211) % 3500))::timestamp, NULL, (DATE '2016-01-01' + ((47 * 211) % 3500))::timestamp, 'demo-hr', CURRENT_TIMESTAMP, 'demo-hr'),
  (('10000000-0000-0000-0000-' || lpad(to_hex(7014), 12, '0'))::uuid, '00000000-0000-0000-0000-000000000102', NULL, 'ACTIVE', DATE '2024-01-01', NULL, DATE '2024-01-01', 'demo-hr', CURRENT_TIMESTAMP, 'demo-hr')
ON CONFLICT (history_id) DO NOTHING;

-- 8. Patients: 80 new VERIFIED patient fixtures. The schema stores only
-- a display name for a patient (there is no date-of-birth/gender column
-- on ref.patient_fixture), so variety is in the names. All are VERIFIED,
-- which the backend requires before an encounter can be opened.
INSERT INTO ref.patient_fixture
  (patient_id, display_name, identity_status, source_version,
   created_at, created_by, updated_at, updated_by)
SELECT
  ('10000000-0000-0000-0000-' || lpad(to_hex(8000 + i), 12, '0'))::uuid,
  (ARRAY['Nguyễn','Trần','Lê','Phạm','Hoàng','Huỳnh','Phan','Vũ','Võ','Đặng','Bùi','Đỗ','Hồ','Ngô','Dương','Lý'])[(i * 11) % 16 + 1]
    || ' ' || (ARRAY['Văn','Thị','Hữu','Minh','Quốc','Thu','Đình','Ngọc'])[(i * 2) % 8 + 1]
    || ' ' || (ARRAY['Anh','Bảo','Chi','Dũng','Hà','Hoa','Hùng','Khánh','Lan','Linh','Minh','Nam','Ngọc','Phúc','Quang','Sơn','Thảo','Thu','Trang','Tuấn','Tuyết','Vân','Việt','Yến','Đức','Hải','Long','Phương','Quỳnh','Tâm'])[(i * 13) % 30 + 1],
  'VERIFIED', 'SEED-DEMO',
  CURRENT_TIMESTAMP - (i % 7 || ' days')::interval, 'demo-reception',
  CURRENT_TIMESTAMP - (i % 7 || ' days')::interval, 'demo-reception'
FROM generate_series(1, 80) AS g(i)
ON CONFLICT (patient_id) DO NOTHING;

-- 9. Allergy state for every patient: 28 HAS_ALLERGY with a real allergen,
-- 32 NONE_CONFIRMED, 20 UNKNOWN. No allergen is ever 'Synthetic Medication'
-- or a catalogue drug name, so no prescription conflicts with an allergy.
INSERT INTO ref.allergy_fixture
  (allergy_id, patient_id, allergen, allergy_status, verified_at, verified_by,
   source_version, created_at, created_by, updated_at, updated_by)
SELECT
  ('10000000-0000-0000-0000-' || lpad(to_hex(9000 + i), 12, '0'))::uuid,
  ('10000000-0000-0000-0000-' || lpad(to_hex(8000 + i), 12, '0'))::uuid,
  CASE WHEN i % 20 < 7 THEN (ARRAY['Penicillin','NSAIDs','Hải sản','Thuốc cản quang','Sulfonamide','Latex'])[(i % 6) + 1] ELSE NULL END,
  CASE WHEN i % 20 < 7 THEN 'HAS_ALLERGY' WHEN i % 20 < 15 THEN 'NONE_CONFIRMED' ELSE 'UNKNOWN' END,
  CASE WHEN i % 20 < 7 THEN CURRENT_TIMESTAMP - interval '10 days' ELSE NULL END,
  CASE WHEN i % 20 < 7 THEN 'EMP-DEMO-001' ELSE NULL END,
  'SEED-DEMO', CURRENT_TIMESTAMP, 'demo-reception', CURRENT_TIMESTAMP, 'demo-reception'
FROM generate_series(1, 80) AS g(i)
ON CONFLICT (allergy_id) DO NOTHING;

-- 10. Drug catalogue: 12 real drugs added next to the existing
-- 'Synthetic Medication' row. Every prescription item below uses only
-- one of these 13 catalogue drugs.
INSERT INTO ref.medication_fixture
  (medication_id, display_name, is_active, source_version,
   created_at, created_by, updated_at, updated_by)
VALUES
  (('10000000-0000-0000-0000-' || lpad(to_hex(10001), 12, '0'))::uuid, 'Paracetamol 500mg', true, 'SEED-DEMO', CURRENT_TIMESTAMP, 'demo-hr', CURRENT_TIMESTAMP, 'demo-hr'),
  (('10000000-0000-0000-0000-' || lpad(to_hex(10002), 12, '0'))::uuid, 'Amoxicillin 500mg', true, 'SEED-DEMO', CURRENT_TIMESTAMP, 'demo-hr', CURRENT_TIMESTAMP, 'demo-hr'),
  (('10000000-0000-0000-0000-' || lpad(to_hex(10003), 12, '0'))::uuid, 'Ibuprofen 400mg', true, 'SEED-DEMO', CURRENT_TIMESTAMP, 'demo-hr', CURRENT_TIMESTAMP, 'demo-hr'),
  (('10000000-0000-0000-0000-' || lpad(to_hex(10004), 12, '0'))::uuid, 'Omeprazole 20mg', true, 'SEED-DEMO', CURRENT_TIMESTAMP, 'demo-hr', CURRENT_TIMESTAMP, 'demo-hr'),
  (('10000000-0000-0000-0000-' || lpad(to_hex(10005), 12, '0'))::uuid, 'Metformin 500mg', true, 'SEED-DEMO', CURRENT_TIMESTAMP, 'demo-hr', CURRENT_TIMESTAMP, 'demo-hr'),
  (('10000000-0000-0000-0000-' || lpad(to_hex(10006), 12, '0'))::uuid, 'Amlodipine 5mg', true, 'SEED-DEMO', CURRENT_TIMESTAMP, 'demo-hr', CURRENT_TIMESTAMP, 'demo-hr'),
  (('10000000-0000-0000-0000-' || lpad(to_hex(10007), 12, '0'))::uuid, 'Atorvastatin 20mg', true, 'SEED-DEMO', CURRENT_TIMESTAMP, 'demo-hr', CURRENT_TIMESTAMP, 'demo-hr'),
  (('10000000-0000-0000-0000-' || lpad(to_hex(10008), 12, '0'))::uuid, 'Cetirizine 10mg', true, 'SEED-DEMO', CURRENT_TIMESTAMP, 'demo-hr', CURRENT_TIMESTAMP, 'demo-hr'),
  (('10000000-0000-0000-0000-' || lpad(to_hex(10009), 12, '0'))::uuid, 'Salbutamol 100mcg', true, 'SEED-DEMO', CURRENT_TIMESTAMP, 'demo-hr', CURRENT_TIMESTAMP, 'demo-hr'),
  (('10000000-0000-0000-0000-' || lpad(to_hex(10010), 12, '0'))::uuid, 'Vitamin D3 1000IU', true, 'SEED-DEMO', CURRENT_TIMESTAMP, 'demo-hr', CURRENT_TIMESTAMP, 'demo-hr'),
  (('10000000-0000-0000-0000-' || lpad(to_hex(10011), 12, '0'))::uuid, 'Cefixime 200mg', true, 'SEED-DEMO', CURRENT_TIMESTAMP, 'demo-hr', CURRENT_TIMESTAMP, 'demo-hr'),
  (('10000000-0000-0000-0000-' || lpad(to_hex(10012), 12, '0'))::uuid, 'Loratadine 10mg', true, 'SEED-DEMO', CURRENT_TIMESTAMP, 'demo-hr', CURRENT_TIMESTAMP, 'demo-hr')
ON CONFLICT (medication_id) DO NOTHING;

-- 11. Encounters: 120 over the last 7 days including today. The first 40
-- are assigned to the demo doctor EMP-DEMO-001 (attending_employee_id =
-- '00000000-0000-0000-0000-000000000102'); the rest go to the 14 ACTIVE
-- seeded doctors. Status mix: 18 WAITING, 14 IN_PROGRESS, 16 PENDING_LAB_RAD,
-- 20 DIAGNOSED, 40 COMPLETED, 6 CANCELLED, 6 NO_SHOW. appointment_id is NULL.
INSERT INTO emr.medical_encounter
  (encounter_id, patient_id, attending_employee_id, status, started_at,
   completed_at, row_version, created_at, created_by, updated_at, updated_by)
SELECT
  ('10000000-0000-0000-0000-' || lpad(to_hex(20000 + s.i), 12, '0'))::uuid,
  ('10000000-0000-0000-0000-' || lpad(to_hex(8000 + ((s.i * 7) % 80) + 1), 12, '0'))::uuid,
  CASE WHEN s.i <= 40 THEN '00000000-0000-0000-0000-000000000102'::uuid
       ELSE ('10000000-0000-0000-0000-' || lpad(to_hex(3000 + d.docs[((s.i - 41) % 14) + 1]), 12, '0'))::uuid END,
  s.status,
  CASE WHEN s.status IN ('IN_PROGRESS', 'PENDING_LAB_RAD', 'DIAGNOSED', 'COMPLETED') THEN s.started_at ELSE NULL END,
  CASE WHEN s.status = 'COMPLETED' THEN s.started_at + (45 + (s.i % 75)) * interval '1 minute' ELSE NULL END,
  1, CURRENT_TIMESTAMP,
  CASE WHEN s.status IN ('WAITING', 'CANCELLED', 'NO_SHOW') THEN 'demo-reception'
       WHEN s.i <= 40 THEN 'EMP-DEMO-001'
       ELSE 'EMP-2026-' || lpad(d.docs[((s.i - 41) % 14) + 1]::text, 3, '0') END,
  CURRENT_TIMESTAMP,
  CASE WHEN s.status IN ('WAITING', 'CANCELLED', 'NO_SHOW') THEN 'demo-reception'
       WHEN s.i <= 40 THEN 'EMP-DEMO-001'
       ELSE 'EMP-2026-' || lpad(d.docs[((s.i - 41) % 14) + 1]::text, 3, '0') END
FROM (
  SELECT i,
    CASE WHEN i BETWEEN 1 AND 18 THEN 'WAITING'
         WHEN i BETWEEN 19 AND 32 THEN 'IN_PROGRESS'
         WHEN i BETWEEN 33 AND 48 THEN 'PENDING_LAB_RAD'
         WHEN i BETWEEN 49 AND 68 THEN 'DIAGNOSED'
         WHEN i BETWEEN 69 AND 108 THEN 'COMPLETED'
         WHEN i BETWEEN 109 AND 114 THEN 'CANCELLED'
         ELSE 'NO_SHOW' END AS status,
    (CURRENT_DATE - ((i * 3) % 7))::timestamp + interval '8 hours' + ((i * 29) % 540) * interval '1 minute' AS started_at
  FROM generate_series(1, 120) AS g(i)
) s
CROSS JOIN (SELECT ARRAY[1, 2, 3, 4, 6, 7, 8, 9, 10, 12, 13, 14, 15, 16] AS docs) d
ON CONFLICT (encounter_id) DO NOTHING;

-- 12. Chief complaints: one text observation per encounter (the schema has
-- no complaint column on the encounter itself, so it lives here).
INSERT INTO emr.clinical_observation
  (observation_id, encounter_id, metric_code, numeric_value, text_value,
   unit_code, observed_at, source_type, source_ref, quality,
   created_at, created_by, updated_at, updated_by)
SELECT
  ('10000000-0000-0000-0000-' || lpad(to_hex(30000 + i), 12, '0'))::uuid,
  ('10000000-0000-0000-0000-' || lpad(to_hex(20000 + i), 12, '0'))::uuid,
  'CHIEF_COMPLAINT', NULL,
  (ARRAY['Sốt cao 3 ngày, mệt mỏi','Ho khan kéo dài 1 tuần','Đau bụng vùng thượng vị sau ăn','Đau ngực khi gắng sức','Khó thở về đêm, thở khò khè','Đau đầu dữ dội kèm buồn nôn','Chóng mặt, ù tai','Đau khớp gối khi vận động','Phát ban ngứa toàn thân','Tiêu chảy cấp 2 ngày','Đau họng, nuốt vướng','Mệt mỏi, chán ăn kéo dài'])[(i % 12) + 1],
  NULL,
  (CURRENT_DATE - ((i * 3) % 7))::timestamp + interval '8 hours' + ((i * 29) % 540) * interval '1 minute',
  'MANUAL', 'SEED-DEMO', 'VALID',
  CURRENT_TIMESTAMP, 'demo-reception', CURRENT_TIMESTAMP, 'demo-reception'
FROM generate_series(1, 120) AS g(i)
ON CONFLICT (observation_id) DO NOTHING;

-- 13. Vitals for the 90 examined encounters (IN_PROGRESS, PENDING_LAB_RAD,
-- DIAGNOSED, COMPLETED): heart rate, SpO2, temperature, respiratory rate.
INSERT INTO emr.clinical_observation
  (observation_id, encounter_id, metric_code, numeric_value, text_value,
   unit_code, observed_at, source_type, source_ref, quality,
   created_at, created_by, updated_at, updated_by)
SELECT
  ('10000000-0000-0000-0000-' || lpad(to_hex(32000 + (e.i - 19) * 4 + m.m), 12, '0'))::uuid,
  ('10000000-0000-0000-0000-' || lpad(to_hex(20000 + e.i), 12, '0'))::uuid,
  CASE m.m WHEN 1 THEN 'HEART_RATE' WHEN 2 THEN 'SPO2' WHEN 3 THEN 'TEMPERATURE' ELSE 'RESPIRATORY_RATE' END,
  CASE m.m WHEN 1 THEN 62 + (e.i * 7 % 37) WHEN 2 THEN 95 + (e.i % 6) WHEN 3 THEN 36.2 + ((e.i % 25) / 10.0) ELSE 14 + (e.i % 9) END,
  NULL,
  CASE m.m WHEN 1 THEN 'bpm' WHEN 2 THEN '%' WHEN 3 THEN 'degC' ELSE 'breaths/min' END,
  e.started_at, 'MANUAL', 'SEED-DEMO', 'VALID',
  CURRENT_TIMESTAMP,
  CASE WHEN e.i <= 40 THEN 'EMP-DEMO-001' ELSE 'EMP-2026-' || lpad(d.docs[((e.i - 41) % 14) + 1]::text, 3, '0') END,
  CURRENT_TIMESTAMP,
  CASE WHEN e.i <= 40 THEN 'EMP-DEMO-001' ELSE 'EMP-2026-' || lpad(d.docs[((e.i - 41) % 14) + 1]::text, 3, '0') END
FROM (
  SELECT i, (CURRENT_DATE - ((i * 3) % 7))::timestamp + interval '8 hours' + ((i * 29) % 540) * interval '1 minute' AS started_at
  FROM generate_series(19, 108) AS g(i)
) e
CROSS JOIN generate_series(1, 4) AS m(m)
CROSS JOIN (SELECT ARRAY[1, 2, 3, 4, 6, 7, 8, 9, 10, 12, 13, 14, 15, 16] AS docs) d
ON CONFLICT (observation_id) DO NOTHING;

-- 14. Diagnoses for the 60 DIAGNOSED/COMPLETED encounters: exactly one
-- CONFIRMED PRIMARY diagnosis each, with a real ICD-10 code.
INSERT INTO emr.diagnosis
  (diagnosis_id, encounter_id, diagnosis_type, diagnosis_status, icd10_code,
   narrative, confirmed_by, created_at, created_by, updated_at, updated_by)
SELECT
  ('10000000-0000-0000-0000-' || lpad(to_hex(50000 + (i - 48)), 12, '0'))::uuid,
  ('10000000-0000-0000-0000-' || lpad(to_hex(20000 + i), 12, '0'))::uuid,
  'PRIMARY', 'CONFIRMED',
  (ARRAY['J06.9','I10','E11.9','J45.9','K29.7','M17.0','N39.0','J18.9','G43.9','I25.10','J02.9','A09.0','L50.0','H10.9'])[(i % 14) + 1],
  (ARRAY['Viêm đường hô hấp trên cấp','Tăng huyết áp nguyên phát','Đái tháo đường type 2','Hen phế quản','Viêm dạ dày','Thoái hóa khớp gối','Nhiễm trùng đường tiểu','Viêm phổi','Đau đầu Migraine','Bệnh tim thiếu máu cục bộ','Viêm họng cấp','Tiêu chảy nhiễm khuẩn cấp','Mày đay','Viêm kết mạc'])[(i % 14) + 1] || ': chẩn đoán xác định, cho điều trị và theo dõi.',
  ('10000000-0000-0000-0000-' || lpad(to_hex(3000 + d.docs[((i - 41) % 14) + 1]), 12, '0'))::uuid,
  CURRENT_TIMESTAMP,
  'EMP-2026-' || lpad(d.docs[((i - 41) % 14) + 1]::text, 3, '0'),
  CURRENT_TIMESTAMP,
  'EMP-2026-' || lpad(d.docs[((i - 41) % 14) + 1]::text, 3, '0')
FROM generate_series(49, 108) AS g(i)
CROSS JOIN (SELECT ARRAY[1, 2, 3, 4, 6, 7, 8, 9, 10, 12, 13, 14, 15, 16] AS docs) d
ON CONFLICT (diagnosis_id) DO NOTHING;

-- 15. Secondary PROVISIONAL diagnoses for every third diagnosed encounter.
INSERT INTO emr.diagnosis
  (diagnosis_id, encounter_id, diagnosis_type, diagnosis_status, icd10_code,
   narrative, confirmed_by, created_at, created_by, updated_at, updated_by)
SELECT
  ('10000000-0000-0000-0000-' || lpad(to_hex(51000 + (i - 48)), 12, '0'))::uuid,
  ('10000000-0000-0000-0000-' || lpad(to_hex(20000 + i), 12, '0'))::uuid,
  'SECONDARY', 'PROVISIONAL',
  (ARRAY['J06.9','I10','E11.9','J45.9','K29.7','M17.0','N39.0','J18.9','G43.9','I25.10','J02.9','A09.0','L50.0','H10.9'])[((i * 5) % 14) + 1],
  (ARRAY['Viêm đường hô hấp trên cấp','Tăng huyết áp nguyên phát','Đái tháo đường type 2','Hen phế quản','Viêm dạ dày','Thoái hóa khớp gối','Nhiễm trùng đường tiểu','Viêm phổi','Đau đầu Migraine','Bệnh tim thiếu máu cục bộ','Viêm họng cấp','Tiêu chảy nhiễm khuẩn cấp','Mày đay','Viêm kết mạc'])[((i * 5) % 14) + 1] || ': chẩn đoán phụ, cần theo dõi thêm.',
  NULL, CURRENT_TIMESTAMP,
  'EMP-2026-' || lpad(d.docs[((i - 41) % 14) + 1]::text, 3, '0'),
  CURRENT_TIMESTAMP,
  'EMP-2026-' || lpad(d.docs[((i - 41) % 14) + 1]::text, 3, '0')
FROM generate_series(49, 108) AS g(i)
CROSS JOIN (SELECT ARRAY[1, 2, 3, 4, 6, 7, 8, 9, 10, 12, 13, 14, 15, 16] AS docs) d
WHERE i % 3 = 0
ON CONFLICT (diagnosis_id) DO NOTHING;

-- 16. CLS orders for 60 encounters (encounters 19-78): LAB or IMAGING.
-- IN_PROGRESS encounters stay ORDERED, PENDING_LAB_RAD orders are
-- ACKNOWLEDGED or IN_PROGRESS, DIAGNOSED/COMPLETED orders are COMPLETED.
INSERT INTO emr.medical_order
  (order_id, encounter_id, order_type, order_status, ordered_by, ordered_at,
   external_order_ref, created_at, created_by, updated_at, updated_by)
SELECT
  ('10000000-0000-0000-0000-' || lpad(to_hex(60000 + (i - 18)), 12, '0'))::uuid,
  ('10000000-0000-0000-0000-' || lpad(to_hex(20000 + i), 12, '0'))::uuid,
  CASE WHEN i % 3 = 0 THEN 'IMAGING' ELSE 'LAB' END,
  CASE WHEN i BETWEEN 19 AND 32 THEN 'ORDERED'
       WHEN i BETWEEN 33 AND 48 THEN CASE WHEN i % 2 = 0 THEN 'ACKNOWLEDGED' ELSE 'IN_PROGRESS' END
       ELSE 'COMPLETED' END,
  CASE WHEN i <= 40 THEN '00000000-0000-0000-0000-000000000102'::uuid
       ELSE ('10000000-0000-0000-0000-' || lpad(to_hex(3000 + d.docs[((i - 41) % 14) + 1]), 12, '0'))::uuid END,
  (CURRENT_DATE - ((i * 3) % 7))::timestamp + interval '8 hours' + ((i * 29) % 540) * interval '1 minute' + interval '20 minutes',
  NULL, CURRENT_TIMESTAMP,
  CASE WHEN i <= 40 THEN 'EMP-DEMO-001' ELSE 'EMP-2026-' || lpad(d.docs[((i - 41) % 14) + 1]::text, 3, '0') END,
  CURRENT_TIMESTAMP,
  CASE WHEN i <= 40 THEN 'EMP-DEMO-001' ELSE 'EMP-2026-' || lpad(d.docs[((i - 41) % 14) + 1]::text, 3, '0') END
FROM generate_series(19, 78) AS g(i)
CROSS JOIN (SELECT ARRAY[1, 2, 3, 4, 6, 7, 8, 9, 10, 12, 13, 14, 15, 16] AS docs) d
ON CONFLICT (order_id) DO NOTHING;

-- 17. CLS results for the 30 COMPLETED orders: quality VALID, patientId in
-- the payload always matches the order's encounter patient (so nothing is
-- QUARANTINED); every third result is flagged abnormal.
INSERT INTO emr.clinical_result
  (result_id, order_id, source_system, source_result_id, source_version,
   quality, abnormal_flag, result_payload, received_at, result_at,
   created_at, created_by, updated_at, updated_by)
SELECT
  ('10000000-0000-0000-0000-' || lpad(to_hex(70000 + (o - 30)), 12, '0'))::uuid,
  ('10000000-0000-0000-0000-' || lpad(to_hex(60000 + o), 12, '0'))::uuid,
  CASE WHEN (o + 18) % 3 = 0 THEN 'CLS-IMAGING' ELSE 'CLS-LAB' END,
  'RES-2026-' || lpad(o::text, 4, '0'),
  '1', 'VALID',
  CASE WHEN o % 3 = 0 THEN true ELSE false END,
  jsonb_build_object(
    'patientId', ('10000000-0000-0000-0000-' || lpad(to_hex(8000 + (((o + 18) * 7) % 80) + 1), 12, '0')),
    'test', CASE WHEN (o + 18) % 3 = 0 THEN (ARRAY['X-quang ngực','Siêu âm bụng','CT sọ não','ECG'])[((o + 18) % 4) + 1] ELSE (ARRAY['Công thức máu','HbA1c','Đường huyết đói','Cholesterol toàn phần','Chức năng gan ALT'])[((o + 18) % 5) + 1] END,
    'value', CASE WHEN o % 3 = 0 THEN 'cao hơn giới hạn bình thường' ELSE 'trong giới hạn bình thường' END,
    'unit', CASE WHEN (o + 18) % 3 = 0 THEN 'hình ảnh' ELSE 'đơn vị' END,
    'referenceRange', 'theo chuẩn phòng xét nghiệm'),
  (CURRENT_DATE - (((o + 18) * 3) % 7))::timestamp + interval '8 hours' + (((o + 18) * 29) % 540) * interval '1 minute' + interval '110 minutes',
  (CURRENT_DATE - (((o + 18) * 3) % 7))::timestamp + interval '8 hours' + (((o + 18) * 29) % 540) * interval '1 minute' + interval '110 minutes',
  CURRENT_TIMESTAMP, 'demo-cls', CURRENT_TIMESTAMP, 'demo-cls'
FROM generate_series(31, 60) AS g(o)
ON CONFLICT (result_id) DO NOTHING;

-- 18. Prescriptions for 32 COMPLETED encounters (69-100), issued by their
-- attending doctor: all of them are ACTIVE with a VALID or EXPIRING_SOON
-- CCHN. They were issued while the encounter was open and the encounter
-- was completed afterwards, so a COMPLETED encounter can hold an ORDERED
-- prescription. Status mix: 28 ORDERED, 2 DRAFT, 2 CANCELLED.
INSERT INTO emr.prescription
  (prescription_id, encounter_id, prescriber_id, prescription_status, issued_at,
   created_at, created_by, updated_at, updated_by)
SELECT
  ('10000000-0000-0000-0000-' || lpad(to_hex(80000 + (i - 68)), 12, '0'))::uuid,
  ('10000000-0000-0000-0000-' || lpad(to_hex(20000 + i), 12, '0'))::uuid,
  ('10000000-0000-0000-0000-' || lpad(to_hex(3000 + d.docs[((i - 41) % 14) + 1]), 12, '0'))::uuid,
  CASE WHEN i % 16 = 0 THEN 'CANCELLED' WHEN i % 16 = 8 THEN 'DRAFT' ELSE 'ORDERED' END,
  CASE WHEN i % 16 = 8 THEN NULL
       ELSE (CURRENT_DATE - ((i * 3) % 7))::timestamp + interval '8 hours' + ((i * 29) % 540) * interval '1 minute' + interval '75 minutes' END,
  CURRENT_TIMESTAMP,
  'EMP-2026-' || lpad(d.docs[((i - 41) % 14) + 1]::text, 3, '0'),
  CURRENT_TIMESTAMP,
  'EMP-2026-' || lpad(d.docs[((i - 41) % 14) + 1]::text, 3, '0')
FROM generate_series(69, 100) AS g(i)
CROSS JOIN (SELECT ARRAY[1, 2, 3, 4, 6, 7, 8, 9, 10, 12, 13, 14, 15, 16] AS docs) d
ON CONFLICT (prescription_id) DO NOTHING;

-- 19. Prescription items (1-2 lines per prescription, 43 lines in total).
-- Only catalogue drugs are used. The drug is shifted away from
-- Amoxicillin/Cefixime for penicillin-allergic patients and away from
-- Ibuprofen for NSAID-allergic patients.
INSERT INTO emr.prescription_item
  (prescription_id, line_no, medication_id, medication_label, dose, route,
   frequency, unit_code, quantity, days_count,
   created_at, created_by, updated_at, updated_by)
SELECT
  ('10000000-0000-0000-0000-' || lpad(to_hex(80000 + (q.i - 68)), 12, '0'))::uuid,
  q.line_no,
  ('10000000-0000-0000-0000-' || lpad(to_hex(10000 + q.drug_no), 12, '0'))::uuid,
  (ARRAY['Paracetamol 500mg','Amoxicillin 500mg','Ibuprofen 400mg','Omeprazole 20mg','Metformin 500mg','Amlodipine 5mg','Atorvastatin 20mg','Cetirizine 10mg','Salbutamol 100mcg','Vitamin D3 1000IU','Cefixime 200mg','Loratadine 10mg'])[q.drug_no],
  (ARRAY['500 mg','500 mg','400 mg','20 mg','500 mg','5 mg','20 mg','10 mg','100 mcg','1000 IU','200 mg','10 mg'])[q.drug_no],
  'Oral',
  CASE WHEN q.line_no = 1 THEN 'Twice daily' ELSE 'Once daily' END,
  CASE WHEN q.drug_no = 9 THEN 'INHALER' ELSE 'TABLET' END,
  (CASE WHEN q.line_no = 1 THEN 2 ELSE 1 END) * q.days_count,
  q.days_count,
  CURRENT_TIMESTAMP, q.prescriber_code, CURRENT_TIMESTAMP, q.prescriber_code
FROM (
  SELECT r.*,
    CASE WHEN r.allergen = 'Penicillin' AND r.drug_raw IN (2, 11) THEN r.drug_raw + 1
         WHEN r.allergen = 'NSAIDs' AND r.drug_raw = 3 THEN 4
         ELSE r.drug_raw END AS drug_no
  FROM (
    SELECT p.*, l.line_no,
      CASE WHEN l.line_no = 1 THEN p.drug_raw1 ELSE p.drug_raw2 END AS drug_raw
    FROM (
      SELECT i,
        CASE WHEN ((((i * 7) % 80) + 1) % 20) < 7
             THEN (ARRAY['Penicillin','NSAIDs','Hải sản','Thuốc cản quang','Sulfonamide','Latex'])[((((i * 7) % 80) + 1) % 6) + 1]
             ELSE NULL END AS allergen,
        ((i * 5) % 12) + 1 AS drug_raw1,
        (((i * 5) % 12) + 4) % 12 + 1 AS drug_raw2,
        5 + (i % 6) AS days_count,
        'EMP-2026-' || lpad(d.docs[((i - 41) % 14) + 1]::text, 3, '0') AS prescriber_code
      FROM generate_series(69, 100) AS g(i)
      CROSS JOIN (SELECT ARRAY[1, 2, 3, 4, 6, 7, 8, 9, 10, 12, 13, 14, 15, 16] AS docs) d
    ) p
    CROSS JOIN (SELECT 1 AS line_no UNION ALL SELECT 2) l
    WHERE l.line_no = 1 OR p.i % 3 = 0
  ) r
) q
ON CONFLICT (prescription_id, line_no) DO NOTHING;

-- 20. Medical records for encounters 19-88, across the whole lifecycle:
-- 22 DRAFT, 34 PENDING_SIGNATURE, 7 LOCKED, 7 ARCHIVED. LOCKED/ARCHIVED
-- records carry locked_at (locked_by stays NULL: sys.user_account is empty).
INSERT INTO emr.medical_record
  (record_id, encounter_id, record_status, current_version, locked_at, locked_by,
   row_version, created_at, created_by, updated_at, updated_by)
SELECT
  ('10000000-0000-0000-0000-' || lpad(to_hex(95000 + (s.i - 18)), 12, '0'))::uuid,
  ('10000000-0000-0000-0000-' || lpad(to_hex(20000 + s.i), 12, '0'))::uuid,
  s.record_status,
  CASE WHEN s.record_status = 'DRAFT' THEN 1 ELSE 2 END,
  CASE WHEN s.record_status IN ('LOCKED', 'ARCHIVED') THEN s.started_at + interval '3 hours' ELSE NULL END,
  NULL, 1, CURRENT_TIMESTAMP, s.created_by, CURRENT_TIMESTAMP, s.created_by
FROM (
  SELECT i,
    CASE WHEN i BETWEEN 19 AND 32 THEN 'DRAFT'
         WHEN i BETWEEN 33 AND 48 THEN CASE WHEN i % 2 = 0 THEN 'DRAFT' ELSE 'PENDING_SIGNATURE' END
         WHEN i BETWEEN 49 AND 68 THEN 'PENDING_SIGNATURE'
         ELSE CASE WHEN i % 3 = 0 THEN 'LOCKED' WHEN i % 3 = 1 THEN 'ARCHIVED' ELSE 'PENDING_SIGNATURE' END END AS record_status,
    (CURRENT_DATE - ((i * 3) % 7))::timestamp + interval '8 hours' + ((i * 29) % 540) * interval '1 minute' AS started_at,
    CASE WHEN i <= 40 THEN 'EMP-DEMO-001' ELSE 'EMP-2026-' || lpad(d.docs[((i - 41) % 14) + 1]::text, 3, '0') END AS created_by
  FROM generate_series(19, 88) AS g(i)
  CROSS JOIN (SELECT ARRAY[1, 2, 3, 4, 6, 7, 8, 9, 10, 12, 13, 14, 15, 16] AS docs) d
) s
ON CONFLICT (encounter_id) DO NOTHING;

-- 21. Record versions: version 1 is always DRAFT; version 2 (when the
-- record's current_version is 2) carries the record's current status.
-- The payload shape mirrors what the backend writes (encounterId,
-- recordId, status, version). sha256_hex is left NULL: it is nullable
-- and the backend computes it when it writes a version itself.
INSERT INTO emr.record_version
  (record_id, version_no, payload, sha256_hex, version_created_at,
   created_at, created_by, updated_at, updated_by)
SELECT
  ('10000000-0000-0000-0000-' || lpad(to_hex(95000 + (r.i - 18)), 12, '0'))::uuid,
  v.v,
  jsonb_build_object(
    'encounterId', ('10000000-0000-0000-0000-' || lpad(to_hex(20000 + r.i), 12, '0')),
    'recordId', ('10000000-0000-0000-0000-' || lpad(to_hex(95000 + (r.i - 18)), 12, '0')),
    'status', CASE WHEN v.v = 1 THEN 'DRAFT' ELSE r.record_status END,
    'version', v.v),
  NULL,
  CURRENT_TIMESTAMP - (r.i % 5 || ' days')::interval,
  CURRENT_TIMESTAMP, r.created_by, CURRENT_TIMESTAMP, r.created_by
FROM (
  SELECT i,
    CASE WHEN i BETWEEN 19 AND 32 THEN 'DRAFT'
         WHEN i BETWEEN 33 AND 48 THEN CASE WHEN i % 2 = 0 THEN 'DRAFT' ELSE 'PENDING_SIGNATURE' END
         WHEN i BETWEEN 49 AND 68 THEN 'PENDING_SIGNATURE'
         ELSE CASE WHEN i % 3 = 0 THEN 'LOCKED' WHEN i % 3 = 1 THEN 'ARCHIVED' ELSE 'PENDING_SIGNATURE' END END AS record_status,
    CASE WHEN i <= 40 THEN 'EMP-DEMO-001' ELSE 'EMP-2026-' || lpad(d.docs[((i - 41) % 14) + 1]::text, 3, '0') END AS created_by
  FROM generate_series(19, 88) AS g(i)
  CROSS JOIN (SELECT ARRAY[1, 2, 3, 4, 6, 7, 8, 9, 10, 12, 13, 14, 15, 16] AS docs) d
) r
CROSS JOIN generate_series(1, 2) AS v(v)
WHERE v.v = 1 OR r.record_status <> 'DRAFT'
ON CONFLICT (record_id, version_no) DO NOTHING;

COMMIT;

-- Verification (runs automatically when this file is applied): row counts
-- after seeding. Expected: department 12, employee 49, employment 57,
-- license 37, decision 6, status history 14, patient 81, allergy 80,
-- medication 13, encounter 121, observation 480, diagnosis 80, order 60,
-- result 30, prescription 32, prescription item 43, record 70, version 118.
SELECT 'hr.department' AS table_name, count(*) AS rows FROM hr.department
UNION ALL SELECT 'hr.employee', count(*) FROM hr.employee
UNION ALL SELECT 'hr.employment', count(*) FROM hr.employment
UNION ALL SELECT 'hr.license_certification', count(*) FROM hr.license_certification
UNION ALL SELECT 'hr.personnel_decision', count(*) FROM hr.personnel_decision
UNION ALL SELECT 'hr.employee_status_history', count(*) FROM hr.employee_status_history
UNION ALL SELECT 'ref.patient_fixture', count(*) FROM ref.patient_fixture
UNION ALL SELECT 'ref.allergy_fixture', count(*) FROM ref.allergy_fixture
UNION ALL SELECT 'ref.medication_fixture', count(*) FROM ref.medication_fixture
UNION ALL SELECT 'emr.medical_encounter', count(*) FROM emr.medical_encounter
UNION ALL SELECT 'emr.clinical_observation', count(*) FROM emr.clinical_observation
UNION ALL SELECT 'emr.diagnosis', count(*) FROM emr.diagnosis
UNION ALL SELECT 'emr.medical_order', count(*) FROM emr.medical_order
UNION ALL SELECT 'emr.clinical_result', count(*) FROM emr.clinical_result
UNION ALL SELECT 'emr.prescription', count(*) FROM emr.prescription
UNION ALL SELECT 'emr.prescription_item', count(*) FROM emr.prescription_item
UNION ALL SELECT 'emr.medical_record', count(*) FROM emr.medical_record
UNION ALL SELECT 'emr.record_version', count(*) FROM emr.record_version
ORDER BY 1;
