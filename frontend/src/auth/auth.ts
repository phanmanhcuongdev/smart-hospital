// Demo users for the HTTP Basic login. The backend defines these users in
// its in-memory demo configuration; the password comes from the
// SMARTHMS_DEMO_PASSWORD environment variable (default "demo").
// Demo environment only: never use real credentials here.

export type Role =
  | "HR_SPECIALIST"
  | "HR_APPROVER";

export interface DemoUser {
  username: string;
  role: Role;
  label: string;
}

export const DEMO_USERS: DemoUser[] = [
  { username: "demo-hr", role: "HR_SPECIALIST", label: "HR specialist" },
  { username: "demo-hr-approver", role: "HR_APPROVER", label: "HR approver" },
];

export const HR_ROLES: Role[] = ["HR_SPECIALIST", "HR_APPROVER"];

export function isHr(role: Role): boolean {
  return HR_ROLES.includes(role);
}

export function demoUser(username: string): DemoUser | undefined {
  return DEMO_USERS.find((u) => u.username === username);
}
