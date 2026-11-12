# HR module boundary

HR owns employment/profile and professional-evidence behavior in the approved HR scope. HR does not own system accounts, roles, permissions, sessions, or clinical-record access grants.

When another module needs practitioner facts, HR will publish them through an approved typed in-process port. A job title or employee status alone must never be interpreted as a clinical authorization grant.

## Implemented (branch tran-nam)

FR-HR-01 to FR-HR-04 are implemented under backend/src/main/java/vn/edu/ptit/smarthms/hr/ (services in hr/service, routes in hr/api, entities and repositories in hr/domain):

- FR-HR-01 Employees and departments: employee code is required and unique; department must exist and be active; search by department, position and status. Creating an employee grants no system role. EmployeeService, DepartmentService, RoleProfileService.
- FR-HR-02 CCHN licenses: issue date must not follow expiry date; verification status is UNVERIFIED, VERIFIED, REJECTED or REVOKED. Computed validity is VALID, EXPIRING_SOON (verified, 1 to 30 days left), EXPIRED (the day after expiryDate), UNVERIFIED, or REJECTED (REVOKED maps to REJECTED). UNVERIFIED is never eligible; an expired CCHN does not lock the employee account or change labour status. LicenseCertificationService.
- FR-HR-03 Status decisions: states ONBOARDING, ACTIVE, SUSPENDED, TERMINATED (terminal). Approval is four-eyes: the approver must differ from the requester. A future decision is not applied early. On the TERMINATED effective date the service writes an EMPLOYEE_TERMINATED event to hr.integration_outbox for SYS to revoke the account and session; the record and its history are kept, never deleted. Affected future shifts are not listed because the Scheduling module is not implemented. PersonnelDecisionService.
- FR-HR-04 Transfers: a transfer closes the old assignment and opens the new one; overlapping assignments are rejected; an employee has no two active assignments; history is never overwritten. A transfer does not grant rights in the new unit. A TRANSFER decision is marked APPLIED only when the transfer is recorded. EmploymentService.

Endpoints live under /api/v1/hr/ and require authentication (HTTP Basic demo users, see the backend README). The EMR module and its practitioner integration were removed; HR no longer publishes practitioner facts to other modules.

## Spec-to-schema notes

The HR tables come from the V.1 baseline migration (V1__iv2_baseline.sql) plus the V3 migration (V3__emr_hr_extensions.sql). JPA is validate-only, so the entities map the baseline columns plus the V3 additions.

The spec license fields map to the baseline credential columns: license_certification uses credential_id, credential_type, credential_no, issuer, scope_note, valid_from, valid_until, verification_status and source_ref.

The baseline verification_status CHECK allowed only UNVERIFIED, VERIFIED and REVOKED; V3 added REJECTED.

The spec role_profiles and employee_status_history tables did not exist in the baseline; V3 creates them.

Spec employment_assignments maps to baseline employment (an open assignment has end_at null; role_profile_id was added by V3). Spec employment_decisions maps to baseline personnel_decision (execution_status was added by V3).

Spec employee fields gender, address, phone, email and hire_date, and department field department_type, did not exist in the baseline; V3 adds them as nullable columns.
