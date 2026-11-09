import { FormEvent, useEffect, useState } from 'react';

type Health = { status: string; timestamp: string };

type LoginResponse = {
  token: string;
  expiresAt: string;
  username: string;
  roles: string[];
};

type MeResponse = {
  username: string;
  roles: string[];
  permissions: string[];
};

type AuditEventItem = {
  auditId: string;
  actorUserId: string | null;
  actorRef: string;
  targetType: string;
  targetId: string;
  action: string;
  outcome: string;
  happenedAt: string;
  safeMetadata: string | null;
};

type ApiLog = {
  endpoint: string;
  status: number;
  correlationId: string | null;
  payload: unknown;
};

const apiBase = import.meta.env.VITE_API_BASE_URL || '';
const healthUrl = `${apiBase}/api/v1/system/health`;

export function App() {
  const [state, setState] = useState<'loading' | 'ready' | 'error'>('loading');
  const [health, setHealth] = useState<Health | null>(null);

  // Auth form state
  const [username, setUsername] = useState('admin');
  const [password, setPassword] = useState('Admin@2026!');
  const [busy, setBusy] = useState(false);

  // Active session & user info
  const [session, setSession] = useState<LoginResponse | null>(null);
  const [meInfo, setMeInfo] = useState<MeResponse | null>(null);
  const [auditEvents, setAuditEvents] = useState<AuditEventItem[]>([]);
  const [lastLog, setLastLog] = useState<ApiLog | null>(null);

  useEffect(() => {
    fetch(healthUrl)
      .then(async (response) => {
        if (!response.ok) throw new Error('Health check failed');
        return response.json() as Promise<Health>;
      })
      .then((value) => {
        setHealth(value);
        setState('ready');
      })
      .catch(() => setState('error'));
  }, []);

  async function callApi(
    method: string,
    path: string,
    body?: unknown,
    tokenOverride?: string | null
  ) {
    setBusy(true);
    try {
      const headers: Record<string, string> = {};
      if (body !== undefined) {
        headers['Content-Type'] = 'application/json';
      }
      const activeToken = tokenOverride !== undefined ? tokenOverride : session?.token;
      if (activeToken) {
        headers['Authorization'] = `Bearer ${activeToken}`;
      }

      const response = await fetch(`${apiBase}${path}`, {
        method,
        headers,
        body: body !== undefined ? JSON.stringify(body) : undefined
      });

      const correlationId = response.headers?.get?.('X-Correlation-Id') ?? null;
      let payload: unknown = null;
      const text = await response.text?.();
      if (text) {
        try {
          payload = JSON.parse(text);
        } catch {
          payload = text;
        }
      }

      setLastLog({
        endpoint: `${method} ${path}`,
        status: response.status,
        correlationId,
        payload
      });

      return { ok: response.ok, status: response.status, payload };
    } catch (err) {
      setLastLog({
        endpoint: `${method} ${path}`,
        status: 0,
        correlationId: null,
        payload: { error: err instanceof Error ? err.message : 'Network error' }
      });
      return { ok: false, status: 0, payload: null };
    } finally {
      setBusy(false);
    }
  }

  async function handleLogin(e: FormEvent) {
    e.preventDefault();
    const res = await callApi('POST', '/api/v1/auth/login', { username, password }, null);
    if (res.ok && res.payload) {
      const loginData = res.payload as LoginResponse;
      setSession(loginData);
      setMeInfo(null);
      setAuditEvents([]);
    }
  }

  async function handleFetchMe() {
    const res = await callApi('GET', '/api/v1/auth/me');
    if (res.ok && res.payload) {
      setMeInfo(res.payload as MeResponse);
    }
  }

  async function handleFetchAuditEvents() {
    const res = await callApi('GET', '/api/v1/audit/events');
    if (res.ok && Array.isArray(res.payload)) {
      setAuditEvents(res.payload as AuditEventItem[]);
    }
  }

  async function handleLogout() {
    const res = await callApi('POST', '/api/v1/auth/logout');
    if (res.status === 204 || res.ok) {
      setSession(null);
      setMeInfo(null);
      setAuditEvents([]);
    }
  }

  return (
    <main className="container">
      <header className="header-bar">
        <div>
          <h1>SmartHMS</h1>
          <p>V.1 &amp; V.2 Console — Authentication, RBAC, Audit Trail &amp; Standardized Error Handling</p>
        </div>
        <div>
          {state === 'loading' && (
            <span className="health-badge" role="status">
              Checking system health…
            </span>
          )}
          {state === 'ready' && (
            <span className="health-badge up" role="status">
              Backend status: {health?.status}
            </span>
          )}
          {state === 'error' && (
            <span className="health-badge down" role="alert">
              Backend is unavailable. Check Docker Compose logs.
            </span>
          )}
        </div>
      </header>

      <div className="grid-2">
        {/* Card 1: Authentication */}
        <section className="card">
          <h2>1. Đăng nhập (Authentication)</h2>
          <div className="preset-group">
            <button
              type="button"
              className="btn-outline"
              onClick={() => {
                setUsername('admin');
                setPassword('Admin@2026!');
              }}
            >
              Điền Admin (Full quyền)
            </button>
            <button
              type="button"
              className="btn-outline"
              onClick={() => {
                setUsername('doctor1');
                setPassword('Doctor@2026!');
              }}
            >
              Điền Doctor (Giới hạn quyền)
            </button>
            <button
              type="button"
              className="btn-outline"
              onClick={() => {
                setUsername('admin');
                setPassword('WrongPass123');
              }}
            >
              Test sai mật khẩu (401)
            </button>
            <button
              type="button"
              className="btn-outline"
              onClick={() => {
                setUsername('');
                setPassword('');
              }}
            >
              Test rỗng (422 Validation)
            </button>
          </div>

          <form onSubmit={handleLogin}>
            <div className="form-group">
              <label htmlFor="username-input">Tài khoản (Username)</label>
              <input
                id="username-input"
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="Nhập username..."
              />
            </div>
            <div className="form-group">
              <label htmlFor="password-input">Mật khẩu (Password)</label>
              <input
                id="password-input"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Nhập mật khẩu..."
              />
            </div>
            <div className="btn-row">
              <button type="submit" className="btn-primary" disabled={busy}>
                POST /api/v1/auth/login
              </button>
              {session && (
                <button
                  type="button"
                  className="btn-danger"
                  disabled={busy}
                  onClick={handleLogout}
                >
                  POST /api/v1/auth/logout
                </button>
              )}
            </div>
          </form>
        </section>

        {/* Card 2: Session, Role & Permission Inspector */}
        <section className="card">
          <h2>2. Phiên &amp; Phân quyền (Session &amp; RBAC)</h2>
          {session ? (
            <div>
              <div className="token-box">
                <div>
                  <strong>User:</strong> {session.username}
                </div>
                <div>
                  <strong>Opaque Bearer Token:</strong> {session.token}
                </div>
                <div>
                  <strong>Expires At:</strong> {session.expiresAt}
                </div>
              </div>
              <div>
                <strong>Roles khi đăng nhập:</strong>
                <div className="badge-list">
                  {session.roles.map((r) => (
                    <span key={r} className="badge badge-role">
                      {r}
                    </span>
                  ))}
                </div>
              </div>
            </div>
          ) : (
            <p style={{ color: '#64748b', fontSize: '0.9rem' }}>
              Chưa có phiên đăng nhập. Bạn cũng có thể bấm các nút bên dưới khi chưa đăng nhập để test lỗi{' '}
              <code>401 AUTH_TOKEN_INVALID</code>.
            </p>
          )}

          <div className="btn-row" style={{ marginTop: '1rem' }}>
            <button
              type="button"
              className="btn-secondary"
              disabled={busy}
              onClick={handleFetchMe}
            >
              GET /api/v1/auth/me
            </button>
            <button
              type="button"
              className="btn-secondary"
              disabled={busy}
              onClick={handleFetchAuditEvents}
            >
              GET /api/v1/audit/events (Cần quyền AUDIT_EVENT:READ:ALL)
            </button>
          </div>

          {meInfo && (
            <div style={{ marginTop: '1rem' }}>
              <strong>Quyền chi tiết từ /me ({meInfo.permissions.length} permissions):</strong>
              <div className="badge-list">
                {meInfo.permissions.map((p) => (
                  <span key={p} className="badge badge-perm">
                    {p}
                  </span>
                ))}
              </div>
            </div>
          )}
        </section>
      </div>

      {/* Card 3: API Response & Error Handling Inspector */}
      {lastLog && (
        <section className="card" style={{ marginBottom: '1.5rem' }}>
          <h2>
            <span>3. Kết quả phản hồi API &amp; Error Handling</span>
            <span className="badge badge-role">HTTP {lastLog.status}</span>
          </h2>
          <div className="meta-bar">
            <span>
              <strong>Endpoint:</strong> <code>{lastLog.endpoint}</code>
            </span>
            <span>
              <strong>X-Correlation-Id:</strong>{' '}
              <code>{lastLog.correlationId ?? '(trong body JSON)'}</code>
            </span>
          </div>
          <pre className="response-console">{JSON.stringify(lastLog.payload, null, 2)}</pre>
        </section>
      )}

      {/* Card 4: Audit Trail Table */}
      {auditEvents.length > 0 && (
        <section className="card">
          <h2>4. Nhật ký Audit Hệ thống (sys.audit_event)</h2>
          <div className="table-wrapper">
            <table className="audit-table">
              <thead>
                <tr>
                  <th>Thời gian</th>
                  <th>Actor</th>
                  <th>Action</th>
                  <th>Target</th>
                  <th>Outcome</th>
                  <th>Metadata</th>
                </tr>
              </thead>
              <tbody>
                {auditEvents.map((ev) => (
                  <tr key={ev.auditId}>
                    <td>{new Date(ev.happenedAt).toLocaleString()}</td>
                    <td>{ev.actorRef}</td>
                    <td>
                      <code>{ev.action}</code>
                    </td>
                    <td>
                      {ev.targetType} ({ev.targetId})
                    </td>
                    <td className={`outcome-${ev.outcome}`}>{ev.outcome}</td>
                    <td>
                      <code>{ev.safeMetadata ?? '-'}</code>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}
    </main>
  );
}
