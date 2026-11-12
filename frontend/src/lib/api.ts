import type { Role } from "../auth/auth";

// Authenticated API client for the SmartHMS backend.
// Uses Opaque Bearer Tokens issued by POST /api/v1/auth/login (merged from phung-nam).

export interface Auth {
  username: string;
  token?: string;
  expiresAt?: string;
  roles?: string[];
  password?: string;
  role: Role;
  label: string;
}

export interface LoginResponse {
  token: string;
  expiresAt: string;
  username: string;
  roles: string[];
}

export interface MeResponse {
  username: string;
  roles: string[];
  permissions: string[];
}

export interface AuditEventItem {
  auditId: string;
  actorUserId: string | null;
  actorRef: string;
  targetType: string;
  targetId: string;
  action: string;
  outcome: string;
  happenedAt: string;
  safeMetadata: string | null;
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
  if (status === 400 || status === 422) {
    return "Validation failed: " + message;
  }
  if (status === 401) {
    return "Sign-in failed: wrong username/password or expired session.";
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

export function buildAuthorizationHeader(auth: Pick<Auth, "token" | "username" | "password">): string | undefined {
  if (auth.token) {
    return "Bearer " + auth.token;
  }
  if (auth.username && auth.password) {
    return "Basic " + btoa(auth.username + ":" + auth.password);
  }
  return undefined;
}

export async function login(username: string, password: string): Promise<LoginResponse> {
  const res = await fetch(apiUrl("/api/v1/auth/login"), {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, password }),
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
  if (res.ok && data && typeof data === "object") {
    return data as LoginResponse;
  }
  const obj = (data && typeof data === "object" ? data : {}) as Record<string, unknown>;
  const message = typeof obj.message === "string" ? obj.message : res.statusText || "Login failed";
  const code = typeof obj.code === "string" ? obj.code : "AUTH_CREDENTIALS_INVALID";
  throw new ApiError(res.status, code, friendlyMessage(res.status, message));
}

export async function logout(auth: Auth): Promise<void> {
  if (!auth.token) return;
  try {
    await fetch(apiUrl("/api/v1/auth/logout"), {
      method: "POST",
      headers: { Authorization: "Bearer " + auth.token },
    });
  } catch {
    // Ignore network errors on sign-out; local session is cleared regardless.
  }
}

export const getMe = (a: Auth): Promise<MeResponse> => api(a, "GET", "/api/v1/auth/me");
export const listAuditEvents = (a: Auth): Promise<AuditEventItem[]> => api(a, "GET", "/api/v1/audit/events");

export async function api<T>(auth: Auth, method: string, path: string, body?: unknown): Promise<T> {
  const headers: Record<string, string> = {};
  const authHeader = buildAuthorizationHeader(auth);
  if (authHeader) {
    headers.Authorization = authHeader;
  }
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
  try {
    const res = await fetch(apiUrl("/api/v1/system/health"));
    if (!res.ok) {
      throw new ApiError(res.status, "ERROR", "Backend chưa sẵn sàng (HTTP " + res.status + ").");
    }
    const text = await res.text();
    if (!text) {
      throw new ApiError(res.status, "ERROR", "Phản hồi rỗng từ backend.");
    }
    return JSON.parse(text) as { status: string; timestamp?: string };
  } catch (err) {
    if (err instanceof ApiError) throw err;
    throw new ApiError(0, "ERROR", "Không thể kết nối đến backend (cổng 8080). Vui lòng kiểm tra backend đã được khởi động chưa.");
  }
}

// HR response shapes (mirror the backend HrResponses records).
export interface Department { id: string; departmentCode: string; displayName: string; parentDepartmentId: string | null; active: boolean; departmentType: string | null; createdAt: string; createdBy: string; }
export interface Employee { id: string; employeeCode: string; fullName: string; birthDate: string | null; status: string; statusEffectiveAt: string; gender: string | null; address: string | null; phone: string | null; email: string | null; hireDate: string | null; createdAt: string; createdBy: string; }
export interface Assignment { id: string; employeeId: string; departmentId: string; departmentCode: string; jobTitle: string; roleProfileId: string | null; startAt: string; endAt: string | null; status: string; }
export interface Certification { id: string; employeeId: string; credentialType: string; credentialNo: string | null; issuer: string | null; scopeNote: string | null; validFrom: string | null; validUntil: string | null; verificationStatus: string; validity: string; sourceRef: string; createdAt: string; createdBy: string; }
export interface Decision { id: string; employeeId: string; decisionRef: string; decisionType: string; decisionStatus: string; executionStatus: string; effectiveAt: string; approvedBy: string | null; approvedAt: string | null; reason: string | null; createdAt: string; createdBy: string; }
export interface RoleProfile { id: string; titleName: string; professionalGroup: string | null; qualification: string | null; description: string | null; createdAt: string; createdBy: string; }
export interface StatusHistory { id: string; employeeId: string; oldStatus: string | null; newStatus: string; effectiveAt: string; decisionId: string | null; createdAt: string; }

// Request bodies (mirror the backend HrRequests records).
export interface EmployeeBody { employeeCode: string; fullName: string; birthDate?: string | null; departmentId: string | null; jobTitle?: string | null; roleProfileId?: string | null; gender?: string | null; address?: string | null; phone?: string | null; email?: string | null; hireDate?: string | null; }
export interface DepartmentBody { departmentCode: string; displayName: string; parentDepartmentId?: string | null; active: boolean; departmentType?: string | null; }
export interface RoleProfileBody { titleName: string; professionalGroup?: string | null; qualification?: string | null; description?: string | null; }
export interface CertificationBody { credentialType: string; credentialNo?: string | null; issuer?: string | null; scopeNote?: string | null; validFrom?: string | null; validUntil?: string | null; verificationStatus?: string | null; sourceRef: string; }
export interface DecisionBody { decisionRef: string; decisionType: string; effectiveAt: string; reason?: string | null; }
export interface TransferBody { departmentId: string; jobTitle: string; roleProfileId?: string | null; effectiveFrom: string; decisionRef?: string | null; }

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

export async function deleteDepartment(auth: Auth, id: string) {
  await api<void>(auth, "DELETE", `/api/v1/hr/departments/${id}`);
}

export async function deleteEmployee(auth: Auth, id: string) {
  await api<void>(auth, "DELETE", `/api/v1/hr/employees/${id}`);
}

export async function deleteCertification(auth: Auth, id: string) {
  await api<void>(auth, "DELETE", `/api/v1/hr/certifications/${id}`);
}

export async function updateDecision(auth: Auth, id: string, body: { decisionRef?: string; decisionType?: string; effectiveAt?: string; reason?: string | null }) {
  return await api<Decision>(auth, "PUT", `/api/v1/hr/decisions/${id}`, body);
}

export async function deleteDecision(auth: Auth, id: string) {
  await api<void>(auth, "DELETE", `/api/v1/hr/decisions/${id}`);
}

export async function updateRoleProfile(auth: Auth, id: string, body: { titleName?: string; professionalGroup?: string | null; qualification?: string | null; description?: string | null }) {
  return await api<RoleProfile>(auth, "PUT", `/api/v1/hr/role-profiles/${id}`, body);
}

export async function deleteRoleProfile(auth: Auth, id: string) {
  await api<void>(auth, "DELETE", `/api/v1/hr/role-profiles/${id}`);
}
