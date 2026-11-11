// Demo users for the HTTP Basic login. The backend defines these users in
// its in-memory demo configuration; the password comes from the
// SMARTHMS_DEMO_PASSWORD environment variable (default "demo").
// Demo environment only: never use real credentials here.

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
}

export const DEMO_USERS: DemoUser[] = [
  { username: "EMP-DEMO-001", role: "DOCTOR", label: "Bác sĩ khám (EMP-DEMO-001)" },
  { username: "demo-nurse", role: "NURSE", label: "Điều dưỡng" },
  { username: "demo-cls", role: "CLS_TECHNICIAN", label: "Kỹ thuật viên CLS" },
  { username: "demo-records", role: "RECORDS_MANAGER", label: "Quản lý hồ sơ bệnh án" },
  { username: "demo-reception", role: "RECEPTION", label: "Tiếp đón bệnh nhân" },
  { username: "demo-hr", role: "HR_SPECIALIST", label: "Chuyên viên nhân sự" },
  { username: "demo-hr-approver", role: "HR_APPROVER", label: "Người phê duyệt nhân sự" },
  { username: "demo-admin", role: "ADMIN", label: "Quản trị viên (Toàn quyền HR & EMR)" },
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
  return HR_ROLES.includes(role);
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
