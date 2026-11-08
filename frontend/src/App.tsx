import { useEffect, useState, type FormEvent } from "react";
import { ApiError, health, listDepartments, type Auth } from "./lib/api";
import { DEMO_USERS, demoUser, isClinical, isHr } from "./auth/auth";
import { EmrScreens, type EmrView } from "./features/emr/EmrScreens";
import { HrScreens, type HrView } from "./features/hr/HrScreens";
import { ErrorBanner, Field, SubmitRow, errText } from "./components/ui/ui";

type View = EmrView | HrView;

const SESSION_KEY = "smarthms-auth";

function loadSession(): Auth | null {
  try {
    const raw = sessionStorage.getItem(SESSION_KEY);
    if (raw === null) {
      return null;
    }
    const parsed = JSON.parse(raw) as Auth;
    if (typeof parsed.username !== "string" || typeof parsed.password !== "string") {
      return null;
    }
    const known = demoUser(parsed.username);
    if (known === undefined || known.role !== parsed.role) {
      return null;
    }
    return { username: known.username, password: parsed.password, role: known.role, label: known.label };
  } catch {
    return null;
  }
}

function defaultView(auth: Auth): View {
  if (isClinical(auth.role)) {
    return { name: "emr-list" };
  }
  return { name: "hr-employees" };
}

export function App() {
  const [auth, setAuth] = useState<Auth | null>(() => loadSession());
  const [view, setView] = useState<View>(() => (auth === null ? { name: "emr-list" } : defaultView(auth)));
  const [healthStatus, setHealthStatus] = useState<string>("Unknown");
  const [healthError, setHealthError] = useState<string | null>(null);

  useEffect(() => {
    health()
      .then((h) => setHealthStatus(h.status))
      .catch((e: unknown) => setHealthError(errText(e)));
  }, []);

  function signIn(next: Auth) {
    setAuth(next);
    sessionStorage.setItem(SESSION_KEY, JSON.stringify(next));
    setView(defaultView(next));
  }

  function signOut() {
    setAuth(null);
    sessionStorage.removeItem(SESSION_KEY);
    setView({ name: "emr-list" });
  }

  if (auth === null) {
    return (
      <main>
        <h1>SmartHMS</h1>
        <p>Smart Hospital Management System - demo environment.</p>
        <p>Backend status: {healthStatus}</p>
        {healthError !== null ? <ErrorBanner error={healthError} /> : null}
        <LoginForm onSignIn={signIn} />
      </main>
    );
  }

  return (
    <main>
      <header>
        <h1>SmartHMS</h1>
        <p>
          Signed in as {auth.label} ({auth.role}). Backend status: {healthStatus}
        </p>
        <nav aria-label="Modules">
          {isClinical(auth.role) ? (
            <button className="btn" type="button" onClick={() => setView({ name: "emr-list" })}>
              Encounters
            </button>
          ) : null}
          {isHr(auth.role) ? (
            <button className="btn" type="button" onClick={() => setView({ name: "hr-employees" })}>
              Employees
            </button>
          ) : null}
          {isHr(auth.role) ? (
            <button className="btn" type="button" onClick={() => setView({ name: "hr-departments" })}>
              Departments
            </button>
          ) : null}
          {isHr(auth.role) ? (
            <button className="btn" type="button" onClick={() => setView({ name: "hr-roles" })}>
              Role profiles
            </button>
          ) : null}
          <button className="btn" type="button" onClick={signOut}>
            Sign out
          </button>
        </nav>
        {healthError !== null ? <ErrorBanner error={healthError} /> : null}
      </header>
      {view.name === "emr-list" || view.name === "emr-detail" ? (
        <EmrScreens auth={auth} view={view} go={setView} />
      ) : (
        <HrScreens auth={auth} view={view} go={setView} />
      )}
    </main>
  );
}

function LoginForm({ onSignIn }: { onSignIn: (auth: Auth) => void }) {
  const [username, setUsername] = useState(DEMO_USERS[0].username);
  const [password, setPassword] = useState("demo");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  async function submit(e: FormEvent) {
    e.preventDefault();
    const user = demoUser(username);
    if (user === undefined) {
      setError("Unknown demo user.");
      return;
    }
    setBusy(true);
    setError(null);
    // Verify the credentials with an authenticated endpoint. A 401 means
    // the password is wrong; any other HTTP response means the credentials
    // were accepted (a clinical role gets 403 on the HR endpoint).
    const candidate: Auth = { username: user.username, password, role: user.role, label: user.label };
    try {
      await listDepartments(candidate);
      onSignIn(candidate);
    } catch (err: unknown) {
      if (err instanceof ApiError && err.status === 401) {
        setError("Wrong password. The demo password is the one configured by SMARTHMS_DEMO_PASSWORD (default demo).");
      } else if (err instanceof ApiError && err.status === 403) {
        onSignIn(candidate);
      } else {
        setError(errText(err));
      }
    } finally {
      setBusy(false);
    }
  }
  return (
    <form className="card" onSubmit={submit}>
      <h2>Sign in (demo)</h2>
      <p className="muted">Demo login only. The backend uses HTTP Basic authentication with in-memory demo users; the password comes from the SMARTHMS_DEMO_PASSWORD environment variable (default "demo"). Never use real credentials here.</p>
      <Field label="Demo user">
        <select value={username} onChange={(e) => setUsername(e.target.value)}>
          {DEMO_USERS.map((u) => (
            <option key={u.username} value={u.username}>
              {u.label}
            </option>
          ))}
        </select>
      </Field>
      <Field label="Password">
        <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
      </Field>
      <ErrorBanner error={error} />
      <SubmitRow busy={busy} label="Sign in" />
    </form>
  );
}
export default App;