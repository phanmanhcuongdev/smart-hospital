import { useEffect, useState } from "react";
import { LanguageSwitcher, statusLabel, t, tf } from "./lib/i18n";
import type { FormEvent } from "react";
import { ApiError, health, listDecisions, listDepartments, listEncounters, listRoleProfiles, searchEmployees } from "./lib/api";
import type { Auth, Decision, Department, Employee, Encounter } from "./lib/api";
import { DEMO_USERS, demoUser, isClinical, isHr } from "./auth/auth";
import { EmrScreens } from "./features/emr/EmrScreens";
import type { EmrView } from "./features/emr/EmrScreens";
import { HrScreens } from "./features/hr/HrScreens";
import type { HrView } from "./features/hr/HrScreens";
import { ErrorBanner, Field, StatCard, SubmitRow, errText } from "./components/ui/ui";
import { AppShell } from "./components/ui/layout";
import type { NavEntry, SearchHit } from "./components/ui/layout";

type View = EmrView | HrView | { name: "overview" };

const SESSION_KEY = "smarthms-auth";

function loadSession(): Auth | null {
  try {
    const raw = sessionStorage.getItem(SESSION_KEY);
    if (raw === null) return null;
    const parsed = JSON.parse(raw) as Auth;
    if (typeof parsed !== "object" || parsed === null) return null;
    const known = demoUser(parsed.username);
    if (known === undefined) return null;
    if (parsed.password !== "demo") return null;
    return { username: known.username, password: parsed.password, role: known.role, label: known.label };
  } catch {
    return null;
  }
}

function defaultView(auth: Auth): View {
  return isClinical(auth.role) ? { name: "emr-list" } : { name: "hr-employees" };
}

function asArray<T>(value: unknown): T[] {
  return Array.isArray(value) ? (value as T[]) : [];
}

export function App() {
  const [auth, setAuth] = useState<Auth | null>(() => loadSession());
  const [view, setView] = useState<View>(() => {
    const restored = loadSession();
    return restored === null ? { name: "emr-list" } : defaultView(restored);
  });
  const [healthStatus, setHealthStatus] = useState("Unknown");
  const [healthError, setHealthError] = useState<string | null>(null);
  const [pendingCount, setPendingCount] = useState(0);

  useEffect(() => {
    health()
      .then((r) => setHealthStatus(r.status))
      .catch((err: unknown) => setHealthError(errText(err)));
  }, []);

  useEffect(() => {
    if (auth === null || isHr(auth.role) === false) { setPendingCount(0); return; }
    let cancelled = false;
    listDecisions(auth, "")
      .then((decisions) => { if (cancelled === false) setPendingCount(asArray<Decision>(decisions).filter((d) => d.decisionStatus === "PENDING").length); })
      .catch(() => { if (cancelled === false) setPendingCount(0); });
    return () => { cancelled = true; };
  }, [auth, view.name]);

  const signIn = (next: Auth) => {
    setAuth(next);
    sessionStorage.setItem(SESSION_KEY, JSON.stringify(next));
    setView(defaultView(next));
  };

  const signOut = () => {
    setAuth(null);
    sessionStorage.removeItem(SESSION_KEY);
    setView({ name: "emr-list" });
  };

  const goToEncounterTab = async (tab: "results" | "record") => {
    if (auth === null) return;
    if (view.name === "emr-detail") { setView({ name: "emr-detail", encounterId: view.encounterId, tab }); return; }
    try {
      const sorted = asArray<Encounter>(await listEncounters(auth)).sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
      if (sorted.length > 0 && sorted[0] !== undefined) { setView({ name: "emr-detail", encounterId: sorted[0].id, tab }); return; }
    } catch { /* fall through to the list */ }
    setView({ name: "emr-list" });
  };

  const goToPendingDecisions = async () => {
    if (auth === null) return;
    try {
      const pending = asArray<Decision>(await listDecisions(auth, "")).filter((d) => d.decisionStatus === "PENDING").sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime());
      if (pending.length > 0 && pending[0] !== undefined) { setView({ name: "hr-employee", employeeId: pending[0].employeeId, tab: "decisions" }); return; }
    } catch { /* fall through to the employee list */ }
    setView({ name: "hr-employees" });
  };

  const onSearchSelect = (hit: SearchHit) => {
    if (hit.kind === "Encounter") setView({ name: "emr-detail", encounterId: hit.id, tab: "overview" });
    else if (hit.kind === "Employee") setView({ name: "hr-employee", employeeId: hit.id, tab: "overview" });
    else setView({ name: "hr-departments" });
  };

  const primaryAction = () => { window.dispatchEvent(new CustomEvent("smarthms:primary-action")); };

  if (auth === null) {
    return (
      <div className="auth-shell">
        <main className="auth-card">
          <div className="auth-brand">
            <span className="sidebar-brand-mark" aria-hidden="true">+</span>
            <span><span className="sidebar-brand-name">{t("SmartHMS")}</span><br /><span className="sidebar-brand-sub">{t("Hospital Management")}</span></span>
          </div>
                    <div className="auth-lang"><LanguageSwitcher /></div>
          <h1>{t("SmartHMS")}</h1>
          <p>{t("Smart Hospital Management System - demo environment.")}</p>
          <p>{t("Backend status:")} {healthStatus}</p>
          {healthError !== null ? <ErrorBanner error={healthError} /> : null}
          <LoginForm onSignIn={signIn} />
        </main>
      </div>
    );
  }

  const clinical = isClinical(auth.role);
  const hr = isHr(auth.role);
  const nav: NavEntry[] = [
    { id: "overview", label: t("Overview"), group: t("Overview"), icon: "⌂", active: view.name === "overview", onSelect: () => setView({ name: "overview" }) },
    ...(clinical ? [
      { id: "emr-list", label: t("Encounters"), group: t("EMR"), icon: "▦", active: view.name === "emr-list" || view.name === "emr-detail", onSelect: () => setView({ name: "emr-list" }) },
      { id: "emr-results", label: t("Labs & results"), group: t("EMR"), icon: "◍", active: view.name === "emr-detail" && view.tab === "results", onSelect: () => { void goToEncounterTab("results"); } },
      { id: "emr-record", label: t("Medical records"), group: t("EMR"), icon: "▤", active: view.name === "emr-detail" && view.tab === "record", onSelect: () => { void goToEncounterTab("record"); } },
    ] : []),
    ...(hr ? [
      { id: "hr-employees", label: t("Employee directory"), group: t("HR"), icon: "👥", active: view.name === "hr-employees" || view.name === "hr-employee", onSelect: () => setView({ name: "hr-employees" }) },
      { id: "hr-departments", label: t("Departments"), group: t("HR"), icon: "🏢", active: view.name === "hr-departments", onSelect: () => setView({ name: "hr-departments" }) },
      { id: "hr-roles", label: t("Role profiles"), group: t("HR"), icon: "🪪", active: view.name === "hr-roles", onSelect: () => setView({ name: "hr-roles" }) },
      { id: "hr-decisions", label: t("Pending decisions"), group: t("HR"), icon: "✋", active: false, badge: pendingCount, onSelect: () => { void goToPendingDecisions(); } },
    ] : []),
  ];
  const bottomNav: NavEntry[] = clinical
    ? [nav[0], nav[1], nav[2], nav[3]].filter((e): e is NavEntry => e !== undefined)
    : [nav[0], nav[1], nav[2], nav[4]].filter((e): e is NavEntry => e !== undefined);
  const fab = view.name === "emr-list" && (auth.role === "DOCTOR" || auth.role === "RECEPTION")
    ? { label: t("Open encounter"), onClick: primaryAction }
    : view.name === "hr-employees" && hr
    ? { label: t("Add employee"), onClick: primaryAction }
    : view.name === "hr-departments" && hr
    ? { label: t("Add department"), onClick: primaryAction }
    : null;

  return (
    <AppShell auth={auth} nav={nav} bottomNav={bottomNav} pendingDecisions={pendingCount} onOpenPendingDecisions={() => { void goToPendingDecisions(); }} onSearchSelect={onSearchSelect} onSignOut={signOut} fab={fab}>
      {view.name === "overview" ? <Overview auth={auth} healthStatus={healthStatus} go={setView} /> : view.name === "emr-list" || view.name === "emr-detail" ? <EmrScreens auth={auth} view={view} go={setView} /> : <HrScreens auth={auth} view={view} go={setView} />}
    </AppShell>
  );
}

function Overview({ auth, healthStatus, go }: { auth: Auth; healthStatus: string; go: (next: View) => void }) {
  const [encounters, setEncounters] = useState<Encounter[] | null>(null);
  const [employees, setEmployees] = useState<Employee[] | null>(null);
  const [departments, setDepartments] = useState<Department[] | null>(null);
  const [roleCount, setRoleCount] = useState<number | null>(null);
  const [pending, setPending] = useState<Decision[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    const load = async () => {
      try {
        if (isClinical(auth.role)) {
          const list = await listEncounters(auth);
          if (cancelled === false) setEncounters(asArray<Encounter>(list));
        } else if (isHr(auth.role)) {
          const [emps, depts, roles, decs] = await Promise.all([searchEmployees(auth, ""), listDepartments(auth), listRoleProfiles(auth), listDecisions(auth, "")]);
          if (cancelled) return;
          setEmployees(asArray<Employee>(emps));
          setDepartments(asArray<Department>(depts));
          setRoleCount(asArray<unknown>(roles).length);
          setPending(asArray<Decision>(decs).filter((d) => d.decisionStatus === "PENDING"));
        }
      } catch (err) { if (cancelled === false) setError(errText(err)); }
    };
    void load();
    return () => { cancelled = true; };
  }, [auth]);

  const inProgress = (encounters ?? []).filter((e) => e.status === "IN_PROGRESS");
  const completed = (encounters ?? []).filter((e) => e.status === "COMPLETED");
  const latest = [...(encounters ?? [])].sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())[0];

  return (
    <section aria-label={t("Overview")}>
      <h2>{t("Overview")}</h2>
      <p className="overview-lead">{t("Live summary of the records you can access. Every card opens the section it summarises.")} {t("Backend status:")} {healthStatus}</p>
      {error !== null ? <ErrorBanner error={error} /> : null}
      <div className="stat-grid">
        {isClinical(auth.role) && (
          <>
            <StatCard label={t("Encounters")} value={encounters === null ? "-" : encounters.length} detail={encounters === null ? t("Loading") : tf("{inProgress} in progress - {completed} completed", { inProgress: inProgress.length, completed: completed.length })} action="Open encounters" onSelect={() => go({ name: "emr-list" })} />
            <StatCard label={t("In progress")} value={inProgress.length} detail={t("Encounters currently open")} action={t("Open encounters")} onSelect={() => go({ name: "emr-list" })} />
            <StatCard label={t("Latest encounter")} value={latest === undefined ? "-" : latest.id.slice(0, 8)} detail={latest === undefined ? "No encounters yet." : "Patient " + latest.patientId.slice(0, 8) + " - " + latest.status} action="Open latest" onSelect={() => { if (latest !== undefined) go({ name: "emr-detail", encounterId: latest.id, tab: "overview" }); else go({ name: "emr-list" }); }} />
          </>
        )}
        {isHr(auth.role) && (
          <>
            <StatCard label={t("Employees")} value={employees === null ? "-" : employees.length} detail={t("Staff records you can access")} action={t("Open employees")} onSelect={() => go({ name: "hr-employees" })} />
            <StatCard label={t("Departments")} value={departments === null ? "-" : departments.length} detail={t("Active and inactive departments")} action={t("Open departments")} onSelect={() => go({ name: "hr-departments" })} />
            <StatCard label={t("Role profiles")} value={roleCount === null ? "-" : roleCount} detail={t("Defined roles and their rules")} action={t("Open role profiles")} onSelect={() => go({ name: "hr-roles" })} />
            <StatCard label={t("Pending decisions")} value={pending.length} detail={t("Decisions waiting for a second approver")} action={t("Review decisions")} onSelect={() => go({ name: "hr-employees" })} />
          </>
        )}
      </div>
      <h3>{t("Needs attention")}</h3>
      {isClinical(auth.role) && (
        <div>
          {encounters === null && <p className="muted">{t("Loading encounters")}</p>}
          {encounters !== null && inProgress.length === 0 && <p className="muted">{t("No encounters in progress.")}</p>}
          {inProgress.slice(0, 5).map((e) => (
            <div key={e.id} className="card card-nested">
              <strong>{t("Encounter")} {e.id.slice(0, 8)}</strong> - {t("Patient")} {e.patientId.slice(0, 8)} - {statusLabel(e.status)}
              <div><button type="button" className="btn btn-small" onClick={() => go({ name: "emr-detail", encounterId: e.id, tab: "overview" })}>{t("Open encounter")}</button></div>
            </div>
          ))}
        </div>
      )}
      {isHr(auth.role) && (
        <div>
          {pending.length === 0 && <p className="muted">{t("No pending decisions.")}</p>}
          {pending.slice(0, 5).map((d) => (
            <div key={d.id} className="card card-nested">
              <strong>{d.decisionRef}</strong> - {statusLabel(d.decisionType)} - {statusLabel(d.decisionStatus)}
              <div><button type="button" className="btn btn-small" onClick={() => go({ name: "hr-employee", employeeId: d.employeeId, tab: "decisions" })}>{t("Review decision")}</button></div>
            </div>
          ))}
        </div>
      )}
    </section>
  );
}

function LoginForm({ onSignIn }: { onSignIn: (next: Auth) => void }) {
  const [username, setUsername] = useState(DEMO_USERS[0].username);
  const [password, setPassword] = useState("demo");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    const user = demoUser(username);
    if (user === undefined) { setError(t("Unknown demo user.")); return; }
    setBusy(true);
    setError(null);
    const candidate: Auth = { username: user.username, password, role: user.role, label: user.label };
    try {
      await listDepartments(candidate);
      onSignIn(candidate);
    } catch (err: unknown) {
      if (err instanceof ApiError && err.status === 401) setError(t("Wrong password. The demo password is the one configured by SMARTHMS_DEMO_PASSWORD (default demo)."));
      else if (err instanceof ApiError && err.status === 403) onSignIn(candidate);
      else setError(errText(err));
    } finally { setBusy(false); }
  };

  return (
    <form onSubmit={submit}>
      <h2>{t("Sign in (demo)")}</h2>
      <Field label={t("Demo user")}>
        <select value={username} onChange={(e) => setUsername(e.target.value)}>
          {DEMO_USERS.map((u) => (
            <option key={u.username} value={u.username}>{u.label} ({statusLabel(u.role)})</option>
          ))}
        </select>
      </Field>
      <Field label={t("Password")}>
        <input type="password" autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} />
      </Field>
      {error !== null && <ErrorBanner error={error} />}
      <SubmitRow busy={busy} label={t("Sign in")} />
      <p><strong>{t("Demo accounts:")}</strong>"demo" {t("is the password for every account.")}</p>
      <ul className="demo-list">
        {DEMO_USERS.map((u) => (
          <li key={u.username}><strong>{u.username}</strong> — {u.label} — {statusLabel(u.role)}</li>
        ))}
      </ul>
    </form>
  );
}

export default App;
