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
  | "HR_APPROVER";

export interface DemoUser {
  username: string;
  role: Role;
  label: string;
}

export const DEMO_USERS: DemoUser[] = [
  { username: "EMP-DEMO-001", role: "DOCTOR", label: "Doctor - seed clinician (EMP-DEMO-001)" },
  { username: "demo-nurse", role: "NURSE", label: "Nurse" },
  { username: "demo-cls", role: "CLS_TECHNICIAN", label: "CLS technician" },
  { username: "demo-records", role: "RECORDS_MANAGER", label: "Records manager" },
  { username: "demo-reception", role: "RECEPTION", label: "Reception" },
  { username: "demo-hr", role: "HR_SPECIALIST", label: "HR specialist" },
  { username: "demo-hr-approver", role: "HR_APPROVER", label: "HR approver" },
];

export const CLINICAL_ROLES: Role[] = ["DOCTOR", "NURSE", "CLS_TECHNICIAN", "RECORDS_MANAGER", "RECEPTION"];
export const HR_ROLES: Role[] = ["HR_SPECIALIST", "HR_APPROVER"];

export function isClinical(role: Role): boolean {
  return CLINICAL_ROLES.includes(role);
}

export function isHr(role: Role): boolean {
  return HR_ROLES.includes(role);
}

export function demoUser(username: string): DemoUser | undefined {
  return DEMO_USERS.find((u) => u.username === username);
}
