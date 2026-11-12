// Authentication & RBAC helper definitions (integrated with phung-nam Bearer token auth).

export type Role =
  | "DOCTOR"
  | "NURSE"
  | "CLS_TECHNICIAN"
  | "RECORDS_MANAGER"
  | "RECEPTION"
  | "HR_SPECIALIST"
  | "HR_APPROVER"
  | "ADMIN";

export interface DemoUser {
  username: string;
  role: Role;
  label: string;
  defaultSecret: string;
}

export const DEMO_USERS: DemoUser[] = [
  { username: "admin", role: "ADMIN", label: "Quản trị hệ thống (admin)", defaultSecret: "Admin@2026!" },
  { username: "doctor1", role: "DOCTOR", label: "Bác sĩ điều trị (doctor1)", defaultSecret: "Doctor@2026!" },
  { username: "EMP-DEMO-001", role: "DOCTOR", label: "Bác sĩ khám (EMP-DEMO-001)", defaultSecret: "demo" },
  { username: "demo-nurse", role: "NURSE", label: "Điều dưỡng", defaultSecret: "demo" },
  { username: "demo-cls", role: "CLS_TECHNICIAN", label: "Kỹ thuật viên CLS", defaultSecret: "demo" },
  { username: "demo-records", role: "RECORDS_MANAGER", label: "Quản lý hồ sơ bệnh án", defaultSecret: "demo" },
  { username: "demo-reception", role: "RECEPTION", label: "Tiếp đón bệnh nhân", defaultSecret: "demo" },
  { username: "demo-hr", role: "HR_SPECIALIST", label: "Chuyên viên nhân sự", defaultSecret: "demo" },
  { username: "demo-hr-approver", role: "HR_APPROVER", label: "Người phê duyệt nhân sự", defaultSecret: "demo" },
  { username: "demo-admin", role: "ADMIN", label: "Quản trị viên (Toàn quyền HR & EMR)", defaultSecret: "demo" },
];

export const CLINICAL_ROLES: Role[] = [
  "DOCTOR",
  "NURSE",
  "CLS_TECHNICIAN",
  "RECORDS_MANAGER",
  "RECEPTION",
];

export const HR_ROLES: Role[] = ["HR_SPECIALIST", "HR_APPROVER"];

export function isClinical(role: Role): boolean {
  return CLINICAL_ROLES.includes(role);
}

export function isHr(role: Role): boolean {
  return HR_ROLES.includes(role) || role === "ADMIN";
}

export function canAccessEmr(role: Role): boolean {
  return isClinical(role) || role === "ADMIN";
}

export function canAccessHr(role: Role): boolean {
  return isHr(role) || role === "ADMIN";
}

export function demoUser(username: string): DemoUser | undefined {
  return DEMO_USERS.find((u) => u.username === username);
}

const ROLE_PRIORITY: Role[] = [
  "ADMIN",
  "HR_SPECIALIST",
  "HR_APPROVER",
  "DOCTOR",
  "NURSE",
  "CLS_TECHNICIAN",
  "RECORDS_MANAGER",
  "RECEPTION",
];

export function resolvePrimaryRole(roles: string[] | undefined, username?: string): Role {
  const normalized = new Set<string>();
  for (const raw of roles ?? []) {
    const stripped = raw.startsWith("ROLE_") ? raw.slice("ROLE_".length) : raw;
    normalized.add(stripped === "RECEPTIONIST" ? "RECEPTION" : stripped);
  }
  for (const candidate of ROLE_PRIORITY) {
    if (normalized.has(candidate)) {
      return candidate;
    }
  }
  const known = username ? demoUser(username) : undefined;
  return known?.role ?? "DOCTOR";
}

export function resolveUserLabel(username: string, role: Role): string {
  const known = demoUser(username);
  if (known) return known.label;
  return `${username} (${role})`;
}
