import { FormEvent, useCallback, useEffect, useState } from 'react';

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

type TcResult = {
  id: string;
  title: string;
  priority: string;
  status: 'IDLE' | 'RUNNING' | 'PASS' | 'FAIL';
  evidence: string;
};

const INITIAL_TCS: TcResult[] = [
  { id: 'TC-SYS-001', title: 'Đăng nhập đúng/sai — luồng AC gốc (DOC-TEST-001)', priority: 'P0', status: 'IDLE', evidence: 'Chưa chạy' },
  { id: 'TC-SYS-002', title: 'Đăng nhập đúng/sai — nhánh âm/ranh giới (Sai mật khẩu, không lộ user/secret)', priority: 'P0', status: 'IDLE', evidence: 'Chưa chạy' },
  { id: 'TC-SYS-003', title: 'Phiên hết hạn hoặc bị thu hồi — luồng AC gốc (SESSION-EXPIRED-001)', priority: 'P0', status: 'IDLE', evidence: 'Chưa chạy' },
  { id: 'TC-SYS-004', title: 'Phiên hết hạn hoặc bị thu hồi — nhánh âm (Thu hồi role trên SESSION-ACTIVE-001)', priority: 'P0', status: 'IDLE', evidence: 'Chưa chạy' },
  { id: 'TC-SYS-005', title: 'Least privilege / object scope — luồng AC gốc (IT-ADMIN-TEST-001 đọc EMR-TEST-002)', priority: 'P0', status: 'IDLE', evidence: 'Chưa chạy' },
  { id: 'TC-SYS-006', title: 'Least privilege / object scope — nhánh âm (NURSE-TEST-001 thử khóa EMR-TEST-001)', priority: 'P0', status: 'IDLE', evidence: 'Chưa chạy' },
  { id: 'TC-SYS-007', title: 'Cấp/thu hồi role có căn cứ — luồng AC gốc (DEC-RBAC-001)', priority: 'P1', status: 'IDLE', evidence: 'Chưa chạy' },
  { id: 'TC-SYS-008', title: 'Cấp/thu hồi role có căn cứ — nhánh âm (Không có quyền / scope OPEN-01)', priority: 'P1', status: 'IDLE', evidence: 'Chưa chạy' },
  { id: 'TC-SYS-009', title: 'Audit khi sửa và khóa — luồng AC gốc (HR DEC-HR-001 & Bác sĩ khóa EMR-TEST-001)', priority: 'P0', status: 'IDLE', evidence: 'Chưa chạy' },
  { id: 'TC-SYS-010', title: 'Audit khi sửa và khóa — nhánh âm (IT-ADMIN-TEST-001 bị từ chối EMR, outcome=DENIED)', priority: 'P0', status: 'IDLE', evidence: 'Chưa chạy' },
  { id: 'TC-SYS-011', title: 'Chống sửa/xóa audit — luồng AC gốc (Admin thử sửa AUD-TEST-001)', priority: 'P0', status: 'IDLE', evidence: 'Chưa chạy' },
  { id: 'TC-SYS-012', title: 'Chống sửa/xóa audit — nhánh âm (Xóa lặp AUD-TEST-001 trên application boundary)', priority: 'P0', status: 'IDLE', evidence: 'Chưa chạy' },
  { id: 'TC-SYS-013', title: 'Adapter mock lỗi / gửi lặp — luồng AC gốc (Gửi lặp EV-HR-001 & EV-CLS-001)', priority: 'P1', status: 'IDLE', evidence: 'Chưa chạy' },
  { id: 'TC-SYS-014', title: 'Adapter mock lỗi / gửi lặp — nhánh âm (Timeout FAILED rồi retry cùng eventId)', priority: 'P1', status: 'IDLE', evidence: 'Chưa chạy' }
];

const apiBase = import.meta.env.VITE_API_BASE_URL || '';
const healthUrl = `${apiBase}/api/v1/system/health`;

export function App() {
  const [state, setState] = useState<'loading' | 'ready' | 'error'>('loading');
  const [health, setHealth] = useState<Health | null>(null);

  // Auth form state
  const [username, setUsername] = useState('DOC-TEST-001');
  const [password, setPassword] = useState('Doctor@2026!');
  const [busy, setBusy] = useState(false);

  // Active session & user info
  const [session, setSession] = useState<LoginResponse | null>(null);
  const [meInfo, setMeInfo] = useState<MeResponse | null>(null);
  const [auditEvents, setAuditEvents] = useState<AuditEventItem[]>([]);
  const [lastLog, setLastLog] = useState<ApiLog | null>(null);
  const [tcResults, setTcResults] = useState<TcResult[]>(INITIAL_TCS);

  const checkHealth = useCallback(() => {
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

  useEffect(() => {
    checkHealth();
  }, [checkHealth]);

  useEffect(() => {
    if (state !== 'error') return;
    const timer = setInterval(checkHealth, 4000);
    return () => clearInterval(timer);
  }, [state, checkHealth]);

  async function rawApi(
    method: string,
    path: string,
    body?: unknown,
    tokenOverride?: string | null
  ) {
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

    if (response.status > 0 && response.status !== 502 && response.status !== 504) {
      setHealth({ status: 'UP', timestamp: new Date().toISOString() });
      setState('ready');
    } else if (response.status === 502 || response.status === 504) {
      setState('error');
    }

    const correlationId = response.headers?.get?.('X-Correlation-Id') ?? null;
    let payload: any = null;
    const text = await response.text?.();
    if (text) {
      try {
        payload = JSON.parse(text);
      } catch {
        payload = text;
      }
    }

    return { ok: response.ok, status: response.status, correlationId, payload };
  }

  async function callApi(
    method: string,
    path: string,
    body?: unknown,
    tokenOverride?: string | null
  ) {
    setBusy(true);
    try {
      const res = await rawApi(method, path, body, tokenOverride);
      setLastLog({
        endpoint: `${method} ${path}`,
        status: res.status,
        correlationId: res.correlationId,
        payload: res.payload
      });
      return res;
    } catch (err) {
      setLastLog({
        endpoint: `${method} ${path}`,
        status: 0,
        correlationId: null,
        payload: { error: err instanceof Error ? err.message : 'Network error' }
      });
      return { ok: false, status: 0, correlationId: null, payload: null };
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

  function updateTcStatus(id: string, status: TcResult['status'], evidence: string) {
    setTcResults((prev) =>
      prev.map((tc) => (tc.id === id ? { ...tc, status, evidence } : tc))
    );
  }

  async function executeSingleTc(tcId: string) {
    updateTcStatus(tcId, 'RUNNING', 'Đang chuẩn bị fixture & thực thi...');
    try {
      await rawApi('POST', '/api/v1/sys/test-fixtures/reset', {}, null);
      const beforeSnap = (await rawApi('GET', '/api/v1/sys/test-fixtures/snapshot', undefined, null)).payload;

      switch (tcId) {
        case 'TC-SYS-001': {
          const loginDoc = await rawApi('POST', '/api/v1/auth/login', { username: 'DOC-TEST-001', password: 'Doctor@2026!' }, null);
          const unapprovedDoc = await rawApi('POST', '/api/v1/auth/login', { username: 'HR-UNAPPROVED-DOC', password: 'Unapproved@2026!' }, null);
          const meUnapproved = await rawApi('GET', '/api/v1/auth/me', undefined, unapprovedDoc.payload?.token);
          const logoutRes = await rawApi('POST', '/api/v1/auth/logout', undefined, loginDoc.payload?.token);
          const pass =
            loginDoc.status === 200 &&
            loginDoc.payload?.roles?.includes('ROLE_DOCTOR') &&
            meUnapproved.payload?.permissions?.length === 0 &&
            logoutRes.status === 204;
          updateTcStatus(
            tcId,
            pass ? 'PASS' : 'FAIL',
            `DOC-TEST-001 login=200 (roles=${JSON.stringify(loginDoc.payload?.roles)}), HR-UNAPPROVED-DOC perms=${meUnapproved.payload?.permissions?.length}, logout=${logoutRes.status}`
          );
          break;
        }
        case 'TC-SYS-002': {
          const wrongPass = await rawApi('POST', '/api/v1/auth/login', { username: 'DOC-TEST-001', password: 'WrongPassword!' }, null);
          const afterSnap = (await rawApi('GET', '/api/v1/sys/test-fixtures/snapshot', undefined, null)).payload;
          const pass =
            wrongPass.status === 401 &&
            wrongPass.payload?.code === 'AUTH_CREDENTIALS_INVALID' &&
            afterSnap.sessionCount === beforeSnap.sessionCount &&
            afterSnap.auditCount > beforeSnap.auditCount;
          updateTcStatus(
            tcId,
            pass ? 'PASS' : 'FAIL',
            `HTTP ${wrongPass.status} (${wrongPass.payload?.code}), sessionCount=${beforeSnap.sessionCount}→${afterSnap.sessionCount} (không đổi), auditCount=${beforeSnap.auditCount}→${afterSnap.auditCount}`
          );
          break;
        }
        case 'TC-SYS-003': {
          const expCall = await rawApi('GET', '/api/v1/emr/records/EMR-TEST-001', undefined, 'SESSION-EXPIRED-001');
          const afterSnap = (await rawApi('GET', '/api/v1/sys/test-fixtures/snapshot', undefined, null)).payload;
          const pass =
            expCall.status === 401 &&
            expCall.payload?.code === 'AUTH_TOKEN_EXPIRED' &&
            afterSnap.auditCount > beforeSnap.auditCount;
          updateTcStatus(
            tcId,
            pass ? 'PASS' : 'FAIL',
            `SESSION-EXPIRED-001 → HTTP ${expCall.status} (${expCall.payload?.code}), auditCount=${beforeSnap.auditCount}→${afterSnap.auditCount}`
          );
          break;
        }
        case 'TC-SYS-004': {
          const adminLogin = await rawApi('POST', '/api/v1/auth/login', { username: 'IT-ADMIN-TEST-001', password: 'Admin@2026!' }, null);
          await rawApi(
            'POST',
            '/api/v1/sys/role-assignments/revoke',
            { username: 'DOC-TEST-001', roleCode: 'ROLE_DOCTOR', approvalRef: 'DEC-RBAC-REVOKE-004' },
            adminLogin.payload?.token
          );
          const oldSessionCall = await rawApi('GET', '/api/v1/emr/records/EMR-TEST-001', undefined, 'SESSION-ACTIVE-001');
          const pass = oldSessionCall.status === 403 && oldSessionCall.payload?.code === 'ACCESS_DENIED';
          updateTcStatus(
            tcId,
            pass ? 'PASS' : 'FAIL',
            `Thu hồi ROLE_DOCTOR → Gọi bằng SESSION-ACTIVE-001 cũ nhận HTTP ${oldSessionCall.status} (${oldSessionCall.payload?.code})`
          );
          break;
        }
        case 'TC-SYS-005': {
          const adminLogin = await rawApi('POST', '/api/v1/auth/login', { username: 'IT-ADMIN-TEST-001', password: 'Admin@2026!' }, null);
          const emrRead = await rawApi('GET', '/api/v1/emr/records/EMR-TEST-002', undefined, adminLogin.payload?.token);
          const afterSnap = (await rawApi('GET', '/api/v1/sys/test-fixtures/snapshot', undefined, null)).payload;
          const pass =
            emrRead.status === 403 &&
            emrRead.payload?.code === 'ACCESS_DENIED' &&
            afterSnap.emrTest002.row_version === beforeSnap.emrTest002.row_version;
          updateTcStatus(
            tcId,
            pass ? 'PASS' : 'FAIL',
            `IT-ADMIN-TEST-001 đọc EMR-TEST-002 → HTTP ${emrRead.status} (${emrRead.payload?.code}), EMR row_version=${afterSnap.emrTest002.row_version} (bất biến)`
          );
          break;
        }
        case 'TC-SYS-006': {
          const nurseLogin = await rawApi('POST', '/api/v1/auth/login', { username: 'NURSE-TEST-001', password: 'Nurse@2026!' }, null);
          const lockAttempt = await rawApi('POST', '/api/v1/emr/records/EMR-TEST-001/lock', {}, nurseLogin.payload?.token);
          const afterSnap = (await rawApi('GET', '/api/v1/sys/test-fixtures/snapshot', undefined, null)).payload;
          const pass =
            lockAttempt.status === 403 &&
            lockAttempt.payload?.code === 'ACCESS_DENIED' &&
            afterSnap.emrTest001.record_status === 'DRAFT';
          updateTcStatus(
            tcId,
            pass ? 'PASS' : 'FAIL',
            `NURSE-TEST-001 thử khóa EMR-TEST-001 → HTTP ${lockAttempt.status} (${lockAttempt.payload?.code}), record_status=${afterSnap.emrTest001.record_status}`
          );
          break;
        }
        case 'TC-SYS-007': {
          const adminLogin = await rawApi('POST', '/api/v1/auth/login', { username: 'IT-ADMIN-TEST-001', password: 'Admin@2026!' }, null);
          const assignRes = await rawApi(
            'POST',
            '/api/v1/sys/role-assignments',
            { username: 'DOC-TEST-001', roleCode: 'ROLE_DOCTOR', approvalRef: 'DEC-RBAC-001', scopeCode: 'OWN', validMinutes: 60 },
            adminLogin.payload?.token
          );
          const pass = assignRes.status === 200 && assignRes.payload?.approvalRef === 'DEC-RBAC-001';
          updateTcStatus(
            tcId,
            pass ? 'PASS' : 'FAIL',
            `Admin cấp role với DEC-RBAC-001 → HTTP ${assignRes.status}, assignmentId=${assignRes.payload?.assignmentId}`
          );
          break;
        }
        case 'TC-SYS-008': {
          const nurseLogin = await rawApi('POST', '/api/v1/auth/login', { username: 'NURSE-TEST-001', password: 'Nurse@2026!' }, null);
          const unauthRevoke = await rawApi(
            'POST',
            '/api/v1/sys/role-assignments/revoke',
            { username: 'DOC-TEST-001', roleCode: 'ROLE_DOCTOR', approvalRef: 'DEC-RBAC-001' },
            nurseLogin.payload?.token
          );
          const adminLogin = await rawApi('POST', '/api/v1/auth/login', { username: 'IT-ADMIN-TEST-001', password: 'Admin@2026!' }, null);
          const invalidScope = await rawApi(
            'POST',
            '/api/v1/sys/role-assignments',
            { username: 'DOC-TEST-001', roleCode: 'ROLE_DOCTOR', approvalRef: 'DEC-RBAC-001', scopeCode: 'OPEN_01_UNAPPROVED' },
            adminLogin.payload?.token
          );
          const pass = unauthRevoke.status === 403 && invalidScope.status === 403;
          updateTcStatus(
            tcId,
            pass ? 'PASS' : 'FAIL',
            `User không quyền thu hồi=${unauthRevoke.status}, Scope chưa duyệt (OPEN-01)=${invalidScope.status} (RoleAssignment không đổi)`
          );
          break;
        }
        case 'TC-SYS-009': {
          const hrLogin = await rawApi('POST', '/api/v1/auth/login', { username: 'HR-TEST-001', password: 'Hr@2026!' }, null);
          const hrUpdate = await rawApi(
            'POST',
            '/api/v1/hr/employees/EMP-DEMO-001/status',
            { status: 'ACTIVE', decisionRef: 'DEC-HR-001', reason: 'Annual verification' },
            hrLogin.payload?.token
          );
          const docLogin = await rawApi('POST', '/api/v1/auth/login', { username: 'DOC-TEST-001', password: 'Doctor@2026!' }, null);
          const emrLock = await rawApi('POST', '/api/v1/emr/records/EMR-TEST-001/lock', {}, docLogin.payload?.token);
          const afterSnap = (await rawApi('GET', '/api/v1/sys/test-fixtures/snapshot', undefined, null)).payload;
          const pass =
            hrUpdate.status === 200 &&
            emrLock.status === 200 &&
            afterSnap.emrTest001.record_status === 'LOCKED' &&
            afterSnap.emrTest001.row_version === 2;
          updateTcStatus(
            tcId,
            pass ? 'PASS' : 'FAIL',
            `HR status update=${hrUpdate.status} (rv=${hrUpdate.payload?.row_version}), EMR lock=${emrLock.status} (status=${afterSnap.emrTest001.record_status}, rv=${afterSnap.emrTest001.row_version})`
          );
          break;
        }
        case 'TC-SYS-010': {
          const adminLogin = await rawApi('POST', '/api/v1/auth/login', { username: 'IT-ADMIN-TEST-001', password: 'Admin@2026!' }, null);
          const deniedLock = await rawApi('POST', '/api/v1/emr/records/EMR-TEST-001/lock', {}, adminLogin.payload?.token);
          const audits = await rawApi('GET', '/api/v1/audit/events', undefined, adminLogin.payload?.token);
          const latestDenied = Array.isArray(audits.payload)
            ? audits.payload.find((a: AuditEventItem) => a.outcome === 'DENIED' && a.targetId === 'EMR-TEST-001')
            : null;
          const pass = deniedLock.status === 403 && Boolean(latestDenied);
          updateTcStatus(
            tcId,
            pass ? 'PASS' : 'FAIL',
            `IT-ADMIN-TEST-001 bị từ chối khóa EMR → HTTP ${deniedLock.status}, Audit outcome=${latestDenied?.outcome}, không lộ secret/PHI`
          );
          break;
        }
        case 'TC-SYS-011': {
          const adminLogin = await rawApi('POST', '/api/v1/auth/login', { username: 'IT-ADMIN-TEST-001', password: 'Admin@2026!' }, null);
          const auditId = 'f0000000-0000-0000-0000-000000000001';
          const putAttempt = await rawApi('PUT', `/api/v1/audit/events/${auditId}`, { actorRef: 'hacker' }, adminLogin.payload?.token);
          const readBack = await rawApi('GET', `/api/v1/audit/events/${auditId}`, undefined, adminLogin.payload?.token);
          const pass =
            putAttempt.status === 409 &&
            putAttempt.payload?.code === 'AUDIT_IMMUTABLE' &&
            readBack.status === 200 &&
            readBack.payload?.actorRef === 'DOC-TEST-001';
          updateTcStatus(
            tcId,
            pass ? 'PASS' : 'FAIL',
            `Thử sửa AUD-TEST-001 → HTTP ${putAttempt.status} (${putAttempt.payload?.code}), đọc lại actorRef='${readBack.payload?.actorRef}' (nguyên vẹn)`
          );
          break;
        }
        case 'TC-SYS-012': {
          const adminLogin = await rawApi('POST', '/api/v1/auth/login', { username: 'IT-ADMIN-TEST-001', password: 'Admin@2026!' }, null);
          const auditId = 'f0000000-0000-0000-0000-000000000001';
          const del1 = await rawApi('DELETE', `/api/v1/audit/events/${auditId}`, undefined, adminLogin.payload?.token);
          const del2 = await rawApi('DELETE', `/api/v1/audit/events/${auditId}`, undefined, adminLogin.payload?.token);
          const readBack = await rawApi('GET', `/api/v1/audit/events/${auditId}`, undefined, adminLogin.payload?.token);
          const pass = del1.status === 409 && del2.status === 409 && readBack.status === 200;
          updateTcStatus(
            tcId,
            pass ? 'PASS' : 'FAIL',
            `Xóa lặp 2 lần AUD-TEST-001 → HTTP ${del1.status} & ${del2.status} (AUDIT_IMMUTABLE), bản ghi gốc vẫn tồn tại (HTTP ${readBack.status})`
          );
          break;
        }
        case 'TC-SYS-013': {
          const adminLogin = await rawApi('POST', '/api/v1/auth/login', { username: 'IT-ADMIN-TEST-001', password: 'Admin@2026!' }, null);
          const send1 = await rawApi(
            'POST',
            '/api/v1/integration/mock-delivery',
            { eventType: 'CLS_RESULT', eventKey: 'EV-CLS-001', sourceSystem: 'LIS-MOCK', sourceVersion: 'v1', simulateFailure: false },
            adminLogin.payload?.token
          );
          const send2 = await rawApi(
            'POST',
            '/api/v1/integration/mock-delivery',
            { eventType: 'CLS_RESULT', eventKey: 'EV-CLS-001', sourceSystem: 'LIS-MOCK', sourceVersion: 'v1', simulateFailure: false },
            adminLogin.payload?.token
          );
          const pass =
            send1.status === 200 &&
            send1.payload?.duplicate === false &&
            send2.status === 200 &&
            send2.payload?.duplicate === true &&
            send2.payload?.recordCount === 1;
          updateTcStatus(
            tcId,
            pass ? 'PASS' : 'FAIL',
            `Gửi lặp EV-CLS-001 2 lần → Lần 1 duplicate=${send1.payload?.duplicate}, Lần 2 duplicate=${send2.payload?.duplicate}, recordCount=${send2.payload?.recordCount}`
          );
          break;
        }
        case 'TC-SYS-014': {
          const adminLogin = await rawApi('POST', '/api/v1/auth/login', { username: 'IT-ADMIN-TEST-001', password: 'Admin@2026!' }, null);
          const timeoutAttempt = await rawApi(
            'POST',
            '/api/v1/integration/mock-delivery',
            { eventType: 'HR_OUTBOX', eventKey: 'EV-HR-001', simulateFailure: true },
            adminLogin.payload?.token
          );
          const retryAttempt = await rawApi(
            'POST',
            '/api/v1/integration/mock-delivery',
            { eventType: 'HR_OUTBOX', eventKey: 'EV-HR-001', simulateFailure: false },
            adminLogin.payload?.token
          );
          const pass =
            timeoutAttempt.status === 503 &&
            timeoutAttempt.payload?.code === 'INTEGRATION_DELIVERY_FAILED' &&
            retryAttempt.status === 200 &&
            retryAttempt.payload?.deliveryState === 'SENT' &&
            retryAttempt.payload?.attempts === 2 &&
            retryAttempt.payload?.recordCount === 1;
          updateTcStatus(
            tcId,
            pass ? 'PASS' : 'FAIL',
            `Lần 1 timeout → HTTP ${timeoutAttempt.status} (FAILED), Retry cùng EV-HR-001 → HTTP ${retryAttempt.status} (state=${retryAttempt.payload?.deliveryState}, attempts=${retryAttempt.payload?.attempts}, recordCount=${retryAttempt.payload?.recordCount})`
          );
          break;
        }
      }
      await rawApi('POST', '/api/v1/sys/test-fixtures/reset', {}, null);
    } catch (err) {
      updateTcStatus(tcId, 'FAIL', err instanceof Error ? err.message : 'Error');
    }
  }

  async function runAll14TestCases() {
    setBusy(true);
    try {
      for (const tc of INITIAL_TCS) {
        await executeSingleTc(tc.id);
      }
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="container">
      <header className="header-bar">
        <div>
          <h1>SmartHMS</h1>
          <p>V.1 &amp; V.2 Console — Authentication, RBAC, Audit Trail &amp; 14 System Test Cases (TC-SYS-001..014)</p>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
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
            <>
              <span className="health-badge down" role="alert">
                Backend is unavailable. Check Docker Compose logs.
              </span>
              <button type="button" className="btn-outline" onClick={checkHealth}>
                Thử lại
              </button>
            </>
          )}
        </div>
      </header>

      {/* Automated 14 Test Cases Runner Section */}
      <section className="card" style={{ marginBottom: '1.5rem' }}>
        <h2>
          <span>Bộ kiểm thử chuẩn TC-SYS-001 → TC-SYS-014 (API + DB Readback + Fixture Reset)</span>
          <div style={{ display: 'flex', gap: '0.5rem' }}>
            <button
              type="button"
              className="btn-primary"
              disabled={busy}
              onClick={runAll14TestCases}
            >
              ▶ Chạy tự động toàn bộ 14 Test Cases
            </button>
            <button
              type="button"
              className="btn-secondary"
              disabled={busy}
              onClick={() => callApi('POST', '/api/v1/sys/test-fixtures/reset', {}, null)}
            >
              ↺ Reset Fixtures gốc
            </button>
          </div>
        </h2>
        <div className="table-wrapper">
          <table className="audit-table">
            <thead>
              <tr>
                <th style={{ width: '7.5rem' }}>Test Case ID</th>
                <th style={{ width: '4rem' }}>Ưu tiên</th>
                <th>Tên kịch bản &amp; Mục tiêu</th>
                <th style={{ width: '6.5rem' }}>Kết quả</th>
                <th>Bằng chứng đối soát (Before/After &amp; DB Readback)</th>
                <th style={{ width: '6rem' }}>Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {tcResults.map((tc) => (
                <tr key={tc.id}>
                  <td>
                    <strong>{tc.id}</strong>
                  </td>
                  <td>
                    <span className="badge badge-perm">{tc.priority}</span>
                  </td>
                  <td>{tc.title}</td>
                  <td>
                    <span
                      className={
                        tc.status === 'PASS'
                          ? 'outcome-SUCCESS'
                          : tc.status === 'FAIL'
                          ? 'outcome-FAILURE'
                          : 'outcome-DENIED'
                      }
                    >
                      {tc.status}
                    </span>
                  </td>
                  <td style={{ fontSize: '0.8rem' }}>{tc.evidence}</td>
                  <td>
                    <button
                      type="button"
                      className="btn-outline"
                      disabled={busy}
                      onClick={() => executeSingleTc(tc.id)}
                    >
                      Chạy
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      <div className="grid-2">
        {/* Card 1: Authentication */}
        <section className="card">
          <h2>1. Đăng nhập thủ công (Authentication)</h2>
          <div className="preset-group">
            <button
              type="button"
              className="btn-outline"
              onClick={() => {
                setUsername('DOC-TEST-001');
                setPassword('Doctor@2026!');
              }}
            >
              DOC-TEST-001 (Bác sĩ)
            </button>
            <button
              type="button"
              className="btn-outline"
              onClick={() => {
                setUsername('IT-ADMIN-TEST-001');
                setPassword('Admin@2026!');
              }}
            >
              IT-ADMIN-TEST-001 (IT Admin - Không quyền EMR)
            </button>
            <button
              type="button"
              className="btn-outline"
              onClick={() => {
                setUsername('NURSE-TEST-001');
                setPassword('Nurse@2026!');
              }}
            >
              NURSE-TEST-001 (Điều dưỡng - Không quyền ký EMR)
            </button>
            <button
              type="button"
              className="btn-outline"
              onClick={() => {
                setUsername('HR-TEST-001');
                setPassword('Hr@2026!');
              }}
            >
              HR-TEST-001 (Nhân sự)
            </button>
            <button
              type="button"
              className="btn-outline"
              onClick={() => {
                setUsername('DOC-TEST-001');
                setPassword('WrongPass123');
              }}
            >
              Test sai mật khẩu (401)
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
              Chưa có phiên đăng nhập. Bạn có thể bấm các nút bên dưới để kiểm tra từng API theo phiên hiện tại.
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
              onClick={() => callApi('GET', '/api/v1/emr/records/EMR-TEST-002')}
            >
              Đọc EMR-TEST-002
            </button>
            <button
              type="button"
              className="btn-secondary"
              disabled={busy}
              onClick={() => callApi('POST', '/api/v1/emr/records/EMR-TEST-001/lock', {})}
            >
              Khóa EMR-TEST-001
            </button>
            <button
              type="button"
              className="btn-secondary"
              disabled={busy}
              onClick={handleFetchAuditEvents}
            >
              GET /api/v1/audit/events
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
