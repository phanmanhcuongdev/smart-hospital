import type { Role } from "./auth";

// Authenticated API client for the SmartHMS backend. All requests use HTTP
// Basic with the demo user credentials (demo environment only).

export interface Auth {
  username: string;
  password: string;
  role: Role;
  label: string;
}

export class ApiError extends Error {
  status: number;
  code: string;
  constructor(status: number, code: string, message: string) {
    super(message);
    this.status = status;
    this.code = code;
  }
}

export function friendlyMessage(status: number, message: string): string {
  if (status === 400) {
    return "Validation failed: " + message;
  }
  if (status === 401) {
    return "Sign-in failed: wrong demo username or password.";
  }
  if (status === 403) {
    return "Not allowed: " + message;
  }
  if (status === 404) {
    return "Not found: " + message;
  }
  if (status === 409) {
    return "Conflict: " + message + " Nothing was overwritten. Reload and try again.";
  }
  if (status >= 500) {
    return "Server error: " + message;
  }
  return message;
}

const base = (import.meta.env.VITE_API_BASE_URL as string | undefined) || "";

function apiUrl(path: string): string {
  return base + path;
}

export async function api<T>(auth: Auth, method: string, path: string, body?: unknown): Promise<T> {
  const headers: Record<string, string> = {
    Authorization: "Basic " + btoa(auth.username + ":" + auth.password),
  };
  if (body !== undefined) {
    headers["Content-Type"] = "application/json";
  }
  const res = await fetch(apiUrl(path), {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const text = await res.text();
  let data: unknown = null;
  if (text) {
    try {
      data = JSON.parse(text) as unknown;
    } catch {
      data = text;
    }
  }
  if (res.ok) {
    return data as T;
  }
  const obj = (data && typeof data === "object" ? data : {}) as Record<string, unknown>;
  const message = typeof obj.message === "string" ? obj.message : res.statusText;
  const code = typeof obj.code === "string" ? obj.code : "ERROR";
  throw new ApiError(res.status, code, friendlyMessage(res.status, message));
}

export async function health(): Promise<{ status: string; timestamp?: string }> {
  const res = await fetch(apiUrl("/api/v1/system/health"));
  const data = (await res.json()) as { status: string; timestamp?: string };
  if (res.ok) {
    return data;
  }
  throw new ApiError(res.status, "ERROR", "Backend is unavailable.");
}

// EMR response shapes (mirror the backend EmrResponses records).
export interface Encounter { id: string; patientId: string; appointmentId: string | null; attendingEmployeeId: string; status: string; startedAt: string | null; completedAt: string | null; rowVersion: number; createdAt: string; updatedAt: string; createdBy: string; }
export interface Observation { id: string; encounterId: string; metricCode: string; numericValue: number | null; textValue: string | null; unitCode: string | null; observedAt: string; sourceType: string; sourceRef: string | null; quality: string; createdAt: string; createdBy: string; }
export interface Diagnosis { id: string; encounterId: string; type: string; icd10Code: string | null; narrative: string; status: string; confirmedBy: string | null; createdAt: string; createdBy: string; }
export interface MedOrder { id: string; encounterId: string; orderedBy: string; type: string; status: string; externalOrderRef: string | null; orderedAt: string | null; createdAt: string; createdBy: string; }
export interface CResult { id: string; orderId: string; sourceSystem: string; sourceResultId: string; sourceVersion: string; receivedAt: string; resultPayload: string; abnormalFlag: boolean | null; quality: string; duplicate: boolean; createdAt: string; }
export interface PrescriptionItem { lineNo: number; medicationId: string; medicationLabel: string; dose: string; route: string; frequency: string; daysCount: number; quantity: number; unitCode: string; }
export interface Prescription { id: string; encounterId: string; prescriberId: string; status: string; issuedAt: string | null; items: PrescriptionItem[]; allergyWarnings: string[]; createdAt: string; createdBy: string; }
export interface MedRecord { id: string; encounterId: string; status: string; currentVersion: number; lockedBy: string | null; lockedAt: string | null; rowVersion: number; createdAt: string; createdBy: string; }
export interface RecordVersion { versionNo: number; sha256Hex: string; versionCreatedAt: string; createdBy: string; }
export interface Addendum { id: string; recordId: string; reason: string; content: string; confirmationStatus: string; confirmedAt: string | null; confirmedBy: string | null; createdAt: string; createdBy: string; }
export interface EncounterDetail { encounter: Encounter; observations: Observation[]; diagnoses: Diagnosis[]; orders: MedOrder[]; prescriptions: Prescription[]; record: MedRecord | null; }

// HR response shapes (mirror the backend HrResponses records).
export interface Department { id: string; departmentCode: string; displayName: string; parentDepartmentId: string | null; active: boolean; departmentType: string | null; createdAt: string; createdBy: string; }
export interface Employee { id: string; employeeCode: string; fullName: string; birthDate: string | null; status: string; statusEffectiveAt: string; gender: string | null; address: string | null; phone: string | null; email: string | null; hireDate: string | null; createdAt: string; createdBy: string; }
export interface Assignment { id: string; employeeId: string; departmentId: string; departmentCode: string; jobTitle: string; roleProfileId: string | null; startAt: string; endAt: string | null; status: string; }
export interface Certification { id: string; employeeId: string; credentialType: string; credentialNo: string | null; issuer: string | null; scopeNote: string | null; validFrom: string | null; validUntil: string | null; verificationStatus: string; validity: string; sourceRef: string; createdAt: string; createdBy: string; }
export interface Decision { id: string; employeeId: string; decisionRef: string; decisionType: string; decisionStatus: string; executionStatus: string; effectiveAt: string; approvedBy: string | null; approvedAt: string | null; reason: string | null; createdAt: string; createdBy: string; }
export interface RoleProfile { id: string; titleName: string; professionalGroup: string | null; qualification: string | null; description: string | null; createdAt: string; createdBy: string; }
export interface StatusHistory { id: string; employeeId: string; oldStatus: string | null; newStatus: string; effectiveAt: string; decisionId: string | null; createdAt: string; }

// Request bodies (mirror the backend EmrRequests and HrRequests records).
export interface OpenEncounterBody { patientId: string; appointmentId?: string | null; attendingEmployeeId: string; }
export interface VitalsBody { metricCode: string; numericValue?: number | null; textValue?: string | null; unitCode?: string | null; observedAt: string; }
export interface DiagnosisBody { type: string; icd10Code?: string | null; narrative: string; status: string; }
export interface OrderBody { orderType: string; externalOrderRef?: string | null; }
export interface ResultBody { patientId: string; sourceSystem: string; sourceResultId: string; sourceVersion: string; resultPayload: string; abnormalFlag?: boolean | null; }
export interface PrescriptionItemBody { medicationId: string; dose: string; route: string; frequency: string; daysCount: number; quantity: number; unitCode: string; }
export interface PrescriptionBody { items: PrescriptionItemBody[]; }
export interface EmployeeBody { employeeCode: string; fullName: string; birthDate?: string | null; departmentId?: string | null; jobTitle?: string | null; roleProfileId?: string | null; gender?: string | null; address?: string | null; phone?: string | null; email?: string | null; hireDate?: string | null; }
export interface DepartmentBody { departmentCode: string; displayName: string; parentDepartmentId?: string | null; active: boolean; departmentType?: string | null; }
export interface RoleProfileBody { titleName: string; professionalGroup?: string | null; qualification?: string | null; description?: string | null; }
export interface CertificationBody { credentialType: string; credentialNo?: string | null; issuer?: string | null; scopeNote?: string | null; validFrom?: string | null; validUntil?: string | null; verificationStatus?: string | null; sourceRef: string; }
export interface DecisionBody { decisionRef: string; decisionType: string; effectiveAt: string; reason?: string | null; }
export interface TransferBody { departmentId: string; jobTitle: string; roleProfileId?: string | null; effectiveFrom: string; decisionRef?: string | null; }

// EMR endpoints.
export const listEncounters = (a: Auth): Promise<Encounter[]> => api(a, "GET", "/api/v1/emr/encounters");
export const openEncounter = (a: Auth, b: OpenEncounterBody): Promise<Encounter> => api(a, "POST", "/api/v1/emr/encounters", b);
export const getEncounterDetail = (a: Auth, id: string): Promise<EncounterDetail> => api(a, "GET", "/api/v1/emr/encounters/" + id + "/detail");
export const encounterAction = (a: Auth, id: string, action: string): Promise<Encounter> => api(a, "POST", "/api/v1/emr/encounters/" + id + "/" + action);
export const addVitals = (a: Auth, id: string, b: VitalsBody): Promise<Observation> => api(a, "POST", "/api/v1/emr/encounters/" + id + "/vitals", b);
export const addDiagnosis = (a: Auth, id: string, b: DiagnosisBody): Promise<Diagnosis> => api(a, "POST", "/api/v1/emr/encounters/" + id + "/diagnoses", b);
export const createOrder = (a: Auth, id: string, b: OrderBody): Promise<MedOrder> => api(a, "POST", "/api/v1/emr/encounters/" + id + "/orders", b);
export const orderAction = (a: Auth, id: string, action: string): Promise<MedOrder> => api(a, "POST", "/api/v1/emr/orders/" + id + "/" + action);
export const listResults = (a: Auth, orderId: string): Promise<CResult[]> => api(a, "GET", "/api/v1/emr/orders/" + orderId + "/results");
export const receiveResult = (a: Auth, orderId: string, b: ResultBody): Promise<CResult> => api(a, "POST", "/api/v1/emr/orders/" + orderId + "/results", b);
export const listPrescriptions = (a: Auth, id: string): Promise<Prescription[]> => api(a, "GET", "/api/v1/emr/encounters/" + id + "/prescriptions");
export const createPrescription = (a: Auth, id: string, b: PrescriptionBody): Promise<Prescription> => api(a, "POST", "/api/v1/emr/encounters/" + id + "/prescriptions", b);
export const prescriptionAction = (a: Auth, id: string, action: string): Promise<Prescription> => api(a, "POST", "/api/v1/emr/prescriptions/" + id + "/" + action);
export const getRecord = (a: Auth, id: string): Promise<MedRecord> => api(a, "GET", "/api/v1/emr/encounters/" + id + "/record");
export const submitRecord = (a: Auth, id: string, expectedRowVersion: number): Promise<MedRecord> => api(a, "POST", "/api/v1/emr/records/" + id + "/submit", { expectedRowVersion });
export const lockRecord = (a: Auth, id: string): Promise<MedRecord> => api(a, "POST", "/api/v1/emr/records/" + id + "/lock");
export const archiveRecord = (a: Auth, id: string): Promise<MedRecord> => api(a, "POST", "/api/v1/emr/records/" + id + "/archive");
export const listVersions = (a: Auth, id: string): Promise<RecordVersion[]> => api(a, "GET", "/api/v1/emr/records/" + id + "/versions");
export const listAddenda = (a: Auth, id: string): Promise<Addendum[]> => api(a, "GET", "/api/v1/emr/records/" + id + "/addenda");
export const addAddendum = (a: Auth, id: string, b: { reason: string; content: string }): Promise<Addendum> => api(a, "POST", "/api/v1/emr/records/" + id + "/addenda", b);
export const confirmAddendum = (a: Auth, id: string): Promise<Addendum> => api(a, "POST", "/api/v1/emr/addenda/" + id + "/confirm");

// HR endpoints.
export const listDepartments = (a: Auth): Promise<Department[]> => api(a, "GET", "/api/v1/hr/departments");
export const createDepartment = (a: Auth, b: DepartmentBody): Promise<Department> => api(a, "POST", "/api/v1/hr/departments", b);
export const updateDepartment = (a: Auth, id: string, b: DepartmentBody): Promise<Department> => api(a, "PUT", "/api/v1/hr/departments/" + id, b);
export const listRoleProfiles = (a: Auth): Promise<RoleProfile[]> => api(a, "GET", "/api/v1/hr/role-profiles");
export const createRoleProfile = (a: Auth, b: RoleProfileBody): Promise<RoleProfile> => api(a, "POST", "/api/v1/hr/role-profiles", b);
export const searchEmployees = (a: Auth, q: string): Promise<Employee[]> => api(a, "GET", "/api/v1/hr/employees" + q);
export const createEmployee = (a: Auth, b: EmployeeBody): Promise<Employee> => api(a, "POST", "/api/v1/hr/employees", b);
export const updateEmployee = (a: Auth, id: string, b: EmployeeBody): Promise<Employee> => api(a, "PUT", "/api/v1/hr/employees/" + id, b);
export const listAssignments = (a: Auth, id: string): Promise<Assignment[]> => api(a, "GET", "/api/v1/hr/employees/" + id + "/assignments");
export const listStatusHistory = (a: Auth, id: string): Promise<StatusHistory[]> => api(a, "GET", "/api/v1/hr/employees/" + id + "/status-history");
export const listCertifications = (a: Auth, id: string): Promise<Certification[]> => api(a, "GET", "/api/v1/hr/employees/" + id + "/certifications");
export const addCertification = (a: Auth, id: string, b: CertificationBody): Promise<Certification> => api(a, "POST", "/api/v1/hr/employees/" + id + "/certifications", b);
export const updateCertification = (a: Auth, id: string, b: CertificationBody): Promise<Certification> => api(a, "PUT", "/api/v1/hr/certifications/" + id, b);
export const listDecisions = (a: Auth, q: string): Promise<Decision[]> => api(a, "GET", "/api/v1/hr/decisions" + q);
export const createDecision = (a: Auth, id: string, b: DecisionBody): Promise<Decision> => api(a, "POST", "/api/v1/hr/employees/" + id + "/decisions", b);
export const decisionAction = (a: Auth, id: string, action: string): Promise<Decision> => api(a, "POST", "/api/v1/hr/decisions/" + id + "/" + action);
export const applyDueDecisions = (a: Auth): Promise<Decision[]> => api(a, "POST", "/api/v1/hr/decisions/apply-due");
export const createTransfer = (a: Auth, id: string, b: TransferBody): Promise<Assignment> => api(a, "POST", "/api/v1/hr/employees/" + id + "/transfers", b);
export const getEmployee = (a: Auth, id: string): Promise<Employee> => api(a, "GET", "/api/v1/hr/employees/" + id);
