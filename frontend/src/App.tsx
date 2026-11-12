import { useEffect, useState } from "react";
import { statusLabel } from "./lib/labels";
import type { FormEvent } from "react";
import { ApiError, health, listDecisions, listDepartments, listRoleProfiles, searchEmployees } from "./lib/api";
import type { Auth, Decision, Department, Employee } from "./lib/api";
import { DEMO_USERS, canAccessEmr, canAccessHr, demoUser, isHr } from "./auth/auth";
import { HrScreens } from "./features/hr/HrScreens";
import type { HrView } from "./features/hr/HrScreens";
import { EmrApp } from "./emr/EmrApp";
import { emrApi } from "./emr/emrApi";
import type { Encounter } from "./emr/types";
import { ErrorBanner, Field, StatCard, SubmitRow, errText } from "./components/ui/ui";
import { AppShell } from "./components/ui/layout";
import type { NavEntry, SearchHit } from "./components/ui/layout";

export type EmrView =
  | { name: "emr-list" }
  | { name: "emr-detail"; encounterId: string };

export type View = HrView | EmrView | { name: "overview" };

const SESSION_KEY = "smarthms-auth";
const DETAIL_ROUTE = /^#\/encounters\/([0-9a-fA-F-]{36})$/;

function isEmrView(v: View): v is EmrView {
  return v.name === "emr-list" || v.name === "emr-detail";
}

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
  if (canAccessHr(auth.role) && !canAccessEmr(auth.role)) {
    return { name: "hr-employees" };
  }
  if (canAccessEmr(auth.role) && !canAccessHr(auth.role)) {
    return { name: "emr-list" };
  }
  return { name: "overview" };
}

function asArray<T>(value: unknown): T[] {
  return Array.isArray(value) ? (value as T[]) : [];
}

export function App() {
  const [auth, setAuth] = useState<Auth | null>(() => loadSession());
  const [view, setView] = useState<View>(() => {
    const restored = loadSession();
    return restored === null ? { name: "hr-employees" } : defaultView(restored);
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
    const onHashChange = () => {
      const hash = window.location.hash;
      const match = DETAIL_ROUTE.exec(hash);
      if (match?.[1]) {
        setView({ name: "emr-detail", encounterId: match[1] });
      } else if (hash === "#/" || hash === "#/encounters") {
        setView((prev) => (isEmrView(prev) ? { name: "emr-list" } : prev));
      }
    };
    window.addEventListener("hashchange", onHashChange);
    return () => window.removeEventListener("hashchange", onHashChange);
  }, []);

  useEffect(() => {
    if (auth === null || isHr(auth.role) === false) { setPendingCount(0); return; }
    let cancelled = false;
    listDecisions(auth, "")
      .then((decisions) => { if (cancelled === false) setPendingCount(asArray<Decision>(decisions).filter((d) => d.decisionStatus === "DRAFT").length); })
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
    window.location.hash = "";
    setView({ name: "hr-employees" });
  };

  const goToPendingDecisions = async () => {
    if (auth === null) return;
    try {
      const pending = asArray<Decision>(await listDecisions(auth, "")).filter((d) => d.decisionStatus === "DRAFT").sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime());
      if (pending.length > 0 && pending[0] !== undefined) {
        window.location.hash = "";
        setView({ name: "hr-employee", employeeId: pending[0].employeeId, tab: "decisions" });
        return;
      }
    } catch { /* fall through to the employee list */ }
    window.location.hash = "";
    setView({ name: "hr-employees" });
  };

  const onSearchSelect = (hit: SearchHit) => {
    if (hit.kind === "Employee") {
      window.location.hash = "";
      setView({ name: "hr-employee", employeeId: hit.id, tab: "overview" });
    } else if (hit.kind === "Department") {
      window.location.hash = "";
      setView({ name: "hr-departments" });
    } else if (hit.kind === "Encounter") {
      window.location.hash = `#/encounters/${hit.id}`;
      setView({ name: "emr-detail", encounterId: hit.id });
    }
  };

  const primaryAction = () => { window.dispatchEvent(new CustomEvent("smarthms:primary-action")); };

  if (auth === null) {
    return (
      <div className="auth-shell">
        <main className="auth-card">
          <div className="auth-brand">
            <span className="sidebar-brand-mark" aria-hidden="true">+</span>
            <span><span className="sidebar-brand-name">{"SmartHMS"}</span><br /><span className="sidebar-brand-sub">{"Quản lý bệnh viện"}</span></span>
          </div>
          <h1>{"SmartHMS"}</h1>
          <p>{"Hệ thống quản lý bệnh viện thông minh - môi trường demo."}</p>
          <p>{"Trạng thái backend:"} {healthStatus}</p>
          {healthError !== null ? <ErrorBanner error={healthError} /> : null}
          <LoginForm onSignIn={signIn} />
        </main>
      </div>
    );
  }

  const hr = canAccessHr(auth.role);
  const emr = canAccessEmr(auth.role);

  const nav: NavEntry[] = [
    {
      id: "overview",
      label: "Tổng quan",
      group: "Tổng quan",
      icon: "⌂",
      active: view.name === "overview",
      onSelect: () => {
        window.location.hash = "";
        setView({ name: "overview" });
      },
    },
    ...(emr
      ? [
          {
            id: "emr-list",
            label: "Khám bệnh & Bệnh án",
            group: "Khám bệnh",
            icon: "🩺",
            active: isEmrView(view),
            onSelect: () => {
              window.location.hash = "#/";
              setView({ name: "emr-list" });
            },
          },
        ]
      : []),
    ...(hr
      ? [
          {
            id: "hr-employees",
            label: "Danh sách nhân viên",
            group: "Nhân sự",
            icon: "👥",
            active: view.name === "hr-employees" || view.name === "hr-employee",
            onSelect: () => {
              window.location.hash = "";
              setView({ name: "hr-employees" });
            },
          },
          {
            id: "hr-departments",
            label: "Phòng ban",
            group: "Nhân sự",
            icon: "🏢",
            active: view.name === "hr-departments",
            onSelect: () => {
              window.location.hash = "";
              setView({ name: "hr-departments" });
            },
          },
          {
            id: "hr-roles",
            label: "Hồ sơ vai trò",
            group: "Nhân sự",
            icon: "🪪",
            active: view.name === "hr-roles",
            onSelect: () => {
              window.location.hash = "";
              setView({ name: "hr-roles" });
            },
          },
          {
            id: "hr-decisions",
            label: "Quyết định chờ duyệt",
            group: "Nhân sự",
            icon: "✋",
            active: false,
            badge: pendingCount,
            onSelect: () => {
              window.location.hash = "";
              void goToPendingDecisions();
            },
          },
        ]
      : []),
  ];

  const bottomNav: NavEntry[] = [
    nav.find((e) => e.id === "overview"),
    nav.find((e) => e.id === "emr-list"),
    nav.find((e) => e.id === "hr-employees"),
    nav.find((e) => e.id === "hr-departments"),
  ].filter((e): e is NavEntry => e !== undefined);

  const fab = view.name === "hr-employees" && hr
    ? { label: "Thêm nhân viên", onClick: primaryAction }
    : view.name === "hr-departments" && hr
    ? { label: "Thêm phòng ban", onClick: primaryAction }
    : null;

  return (
    <AppShell auth={auth} nav={nav} bottomNav={bottomNav} pendingDecisions={pendingCount} onOpenPendingDecisions={() => { void goToPendingDecisions(); }} onSearchSelect={onSearchSelect} onSignOut={signOut} fab={fab}>
      {view.name === "overview" ? (
        <Overview auth={auth} healthStatus={healthStatus} go={setView} />
      ) : isEmrView(view) ? (
        <EmrApp
          encounterId={view.name === "emr-detail" ? view.encounterId : undefined}
          onBackToList={() => {
            window.location.hash = "#/";
            setView({ name: "emr-list" });
          }}
        />
      ) : (
        <HrScreens auth={auth} view={view} go={setView} />
      )}
    </AppShell>
  );
}

function Overview({ auth, healthStatus, go }: { auth: Auth; healthStatus: string; go: (next: View) => void }) {
  const [employees, setEmployees] = useState<Employee[] | null>(null);
  const [departments, setDepartments] = useState<Department[] | null>(null);
  const [roleCount, setRoleCount] = useState<number | null>(null);
  const [pending, setPending] = useState<Decision[]>([]);
  const [encounters, setEncounters] = useState<Encounter[] | null>(null);
  const [encounterTotal, setEncounterTotal] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    const load = async () => {
      try {
        if (canAccessHr(auth.role)) {
          const [emps, depts, roles, decs] = await Promise.all([
            searchEmployees(auth, ""),
            listDepartments(auth),
            listRoleProfiles(auth),
            listDecisions(auth, ""),
          ]);
          if (!cancelled) {
            setEmployees(asArray<Employee>(emps));
            setDepartments(asArray<Department>(depts));
            setRoleCount(asArray<unknown>(roles).length);
            setPending(asArray<Decision>(decs).filter((d) => d.decisionStatus === "DRAFT"));
          }
        }
        if (canAccessEmr(auth.role)) {
          try {
            const res = await emrApi.listEncounters({ page: 0, size: 20 });
            if (!cancelled) {
              setEncounters(res.items);
              setEncounterTotal(res.total);
            }
          } catch {
            // ignore if EMR backend not reachable yet
          }
        }
      } catch (err) {
        if (cancelled === false) setError(errText(err));
      }
    };
    void load();
    return () => { cancelled = true; };
  }, [auth]);

  const inProgressEncounters = encounters?.filter((e) => e.status === "IN_PROGRESS") ?? [];
  const waitingEncounters = encounters?.filter((e) => e.status === "WAITING") ?? [];

  return (
    <section aria-label={"Tổng quan"}>
      <h2>{"Tổng quan"}</h2>
      <p className="overview-lead">{"Tóm tắt trực tiếp các hồ sơ bạn có thể xem. Mỗi thẻ sẽ mở phần tương ứng."} {"Trạng thái backend:"} {healthStatus}</p>
      {error !== null ? <ErrorBanner error={error} /> : null}

      <div className="stat-grid">
        {canAccessEmr(auth.role) && (
          <>
            <StatCard
              label={"Lượt khám"}
              value={encounterTotal === null ? "-" : encounterTotal}
              detail={"Tổng số lượt khám trong hệ thống"}
              action={"Mở danh sách khám"}
              onSelect={() => {
                window.location.hash = "#/";
                go({ name: "emr-list" });
              }}
            />
            <StatCard
              label={"Đang khám"}
              value={inProgressEncounters.length}
              detail={"Ca bệnh đang trong quá trình khám"}
              action={"Xem ca đang khám"}
              onSelect={() => {
                window.location.hash = "#/";
                go({ name: "emr-list" });
              }}
            />
            <StatCard
              label={"Đang chờ"}
              value={waitingEncounters.length}
              detail={"Bệnh nhân đang chờ tiếp nhận"}
              action={"Xem danh sách chờ"}
              onSelect={() => {
                window.location.hash = "#/";
                go({ name: "emr-list" });
              }}
            />
          </>
        )}

        {canAccessHr(auth.role) && (
          <>
            <StatCard label={"Nhân viên"} value={employees === null ? "-" : employees.length} detail={"Hồ sơ nhân viên bạn có thể xem"} action={"Mở nhân viên"} onSelect={() => go({ name: "hr-employees" })} />
            <StatCard label={"Phòng ban"} value={departments === null ? "-" : departments.length} detail={"Các phòng ban đang hoạt động và không hoạt động"} action={"Mở phòng ban"} onSelect={() => go({ name: "hr-departments" })} />
            <StatCard label={"Hồ sơ vai trò"} value={roleCount === null ? "-" : roleCount} detail={"Các vai trò đã định nghĩa và quy tắc của chúng"} action={"Mở hồ sơ vai trò"} onSelect={() => go({ name: "hr-roles" })} />
            <StatCard label={"Quyết định chờ duyệt"} value={pending.length} detail={"Các quyết định đang chờ người phê duyệt thứ hai"} action={"Xem xét các quyết định"} onSelect={() => go({ name: "hr-employees" })} />
          </>
        )}
      </div>

      <h3>{"Cần chú ý"}</h3>
      {canAccessEmr(auth.role) && (
        <div style={{ marginBottom: "1.5rem" }}>
          <h4 style={{ margin: "0.5rem 0" }}>{"Ca khám đang diễn ra"}</h4>
          {encounters === null && <p className="muted">{"Đang tải danh sách ca khám..."}</p>}
          {encounters !== null && inProgressEncounters.length === 0 && (
            <p className="muted">{"Không có ca khám nào đang diễn ra."}</p>
          )}
          {inProgressEncounters.slice(0, 5).map((e) => (
            <div key={e.encounterId} className="card card-nested">
              <strong>{"Lượt khám: " + e.encounterId.slice(0, 8)}</strong> -{" "}
              {"Trạng thái: " + statusLabel(e.status)}
              <div>
                <button
                  type="button"
                  className="btn btn-small"
                  onClick={() => {
                    window.location.hash = `#/encounters/${e.encounterId}`;
                    go({ name: "emr-detail", encounterId: e.encounterId });
                  }}
                >
                  {"Mở ca khám"}
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {canAccessHr(auth.role) && (
        <div>
          <h4 style={{ margin: "0.5rem 0" }}>{"Quyết định nhân sự chờ duyệt"}</h4>
          {pending.length === 0 && <p className="muted">{"Không có quyết định chờ duyệt nào."}</p>}
          {pending.slice(0, 5).map((d) => (
            <div key={d.id} className="card card-nested">
              <strong>{d.decisionRef}</strong> - {statusLabel(d.decisionType)} - {statusLabel(d.decisionStatus)}
              <div><button type="button" className="btn btn-small" onClick={() => go({ name: "hr-employee", employeeId: d.employeeId, tab: "decisions" })}>{"Xem xét quyết định"}</button></div>
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
    if (user === undefined) { setError("Không tìm thấy người dùng demo."); return; }
    setBusy(true);
    setError(null);
    const candidate: Auth = { username: user.username, password, role: user.role, label: user.label };
    try {
      await listDepartments(candidate);
      onSignIn(candidate);
    } catch (err: unknown) {
      if (err instanceof ApiError && err.status === 401) setError("Sai mật khẩu. Mật khẩu demo là mật khẩu đã cấu hình trong SMARTHMS_DEMO_PASSWORD (mặc định là demo).");
      else if (err instanceof ApiError && err.status === 403) onSignIn(candidate);
      else setError(errText(err));
    } finally { setBusy(false); }
  };

  return (
    <form onSubmit={submit}>
      <h2>{"Đăng nhập (demo)"}</h2>
      <Field label={"Người dùng demo"}>
        <select value={username} onChange={(e) => setUsername(e.target.value)}>
          {DEMO_USERS.map((u) => (
            <option key={u.username} value={u.username}>{u.label} ({statusLabel(u.role)})</option>
          ))}
        </select>
      </Field>
      <Field label={"Mật khẩu"}>
        <input type="password" autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} />
      </Field>
      {error !== null && <ErrorBanner error={error} />}
      <SubmitRow busy={busy} label={"Đăng nhập"} />
      <p><strong>{"Tài khoản demo:"}</strong>"demo" {"là mật khẩu cho mọi tài khoản."}</p>
      <ul className="demo-list">
        {DEMO_USERS.map((u) => (
          <li key={u.username}><strong>{u.username}</strong> — {u.label} — {statusLabel(u.role)}</li>
        ))}
      </ul>
    </form>
  );
}

export default App;
