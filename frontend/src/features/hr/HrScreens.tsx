import { useEffect, useState, type FormEvent } from "react";
import type { Auth } from "../../lib/api";
import { addCertification, applyDueDecisions, createDecision, createDepartment, createEmployee, createRoleProfile, createTransfer, decisionAction, getEmployee, listAssignments, listCertifications, listDecisions, listDepartments, listRoleProfiles, listStatusHistory, searchEmployees, updateCertification, updateDepartment, updateEmployee, type Certification, type Decision, type Department, type Employee } from "../../lib/api";
import { Badge, EmptyNote, ErrorBanner, Field, ResultCount, SubmitRow, errText, fmtDate, fmtDateTime, shortId, toInstant, toneFor, useAsync, useForm } from "../../components/ui/ui";

export type HrTab = "overview" | "certs" | "decisions" | "transfers" | "history";
export type HrView = { name: "hr-employees" } | { name: "hr-employee"; employeeId: string; tab: HrTab } | { name: "hr-departments" } | { name: "hr-roles" };

export function HrScreens({ auth, view, go }: { auth: Auth; view: HrView; go: (v: HrView) => void }) {
  if (view.name === "hr-employee") {
    return <EmployeeDetail auth={auth} employeeId={view.employeeId} tab={view.tab} go={go} />;
  }
  if (view.name === "hr-departments") {
    return <DepartmentList auth={auth} />;
  }
  if (view.name === "hr-roles") {
    return <RoleProfileList auth={auth} />;
  }
  return <EmployeeList auth={auth} go={go} />;
}

function EmployeeList({ auth, go }: { auth: Auth; go: (v: HrView) => void }) {
  const departments = useAsync(() => listDepartments(auth), [auth.username]);
  const [filters, setFilters] = useState({ departmentId: "", status: "", jobTitle: "" });
  const [query, setQuery] = useState("");
  const employees = useAsync(() => searchEmployees(auth, query), [query, auth.username]);
  const [showForm, setShowForm] = useState(false);
  useEffect(() => {
    const open = () => { if (auth.role === "HR_SPECIALIST") setShowForm(true); };
    window.addEventListener("smarthms:primary-action", open);
    return () => window.removeEventListener("smarthms:primary-action", open);
  }, [auth.role]);
  const canHr = auth.role === "HR_SPECIALIST";
  function applyFilters(e: FormEvent) {
    e.preventDefault();
    const params = new URLSearchParams();
    if (filters.departmentId !== "") {
      params.set("departmentId", filters.departmentId);
    }
    if (filters.status !== "") {
      params.set("status", filters.status);
    }
    if (filters.jobTitle.trim() !== "") {
      params.set("jobTitle", filters.jobTitle.trim());
    }
    const q = params.toString();
    setQuery(q === "" ? "" : "?" + q);
  }
  return (
    <section>
      <h2>Employees</h2>
      <p className="muted">Search and filter employees by department, status and job title. Creating an employee grants no system role.</p>
      {canHr ? (
        <button className="btn" type="button" onClick={() => setShowForm(true)}>
          New employee
        </button>
      ) : null}
      {showForm ? (
        <EmployeeForm
          auth={auth}
          initial={null}
          onDone={() => {
            setShowForm(false);
            employees.reload();
          }}
          onCancel={() => setShowForm(false)}
        />
      ) : null}
      <form className="card" onSubmit={applyFilters}>
        <h3>Search</h3>
        <div className="row">
          <Field label="Department">
            <select value={filters.departmentId} onChange={(e) => setFilters((f) => ({ ...f, departmentId: e.target.value }))}>
              <option value="">All departments</option>
              {(departments.data ?? []).map((d) => (
                <option key={d.id} value={d.id}>
                  {d.displayName} ({d.departmentCode})
                </option>
              ))}
            </select>
          </Field>
          <Field label="Status">
            <select value={filters.status} onChange={(e) => setFilters((f) => ({ ...f, status: e.target.value }))}>
              <option value="">All statuses</option>
              <option value="ONBOARDING">ONBOARDING</option>
              <option value="ACTIVE">ACTIVE</option>
              <option value="SUSPENDED">SUSPENDED</option>
              <option value="TERMINATED">TERMINATED</option>
            </select>
          </Field>
          <Field label="Job title">
            <input value={filters.jobTitle} onChange={(e) => setFilters((f) => ({ ...f, jobTitle: e.target.value }))} />
          </Field>
        </div>
        <SubmitRow busy={false} label="Search" />
      </form>
      {employees.loading ? <p role="status">Loading employees...</p> : null}
      <ErrorBanner error={employees.error} />
      {employees.data !== null ? <ResultCount shown={employees.data.length} total={employees.data.length} noun="employees" /> : null}
      {employees.data !== null && employees.data.length === 0 ? <EmptyNote text="No employees match the search." /> : null}
      {employees.data !== null && employees.data.length > 0 ? (
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>Code</th>
                <th>Full name</th>
                <th>Birth date</th>
                <th>Status</th>
                <th>Status effective</th>
                <th>Created by</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {employees.data.map((e) => (
                <tr key={e.id}>
                  <td>{e.employeeCode}</td>
                  <td>{e.fullName}</td>
                  <td>{fmtDate(e.birthDate)}</td>
                  <td><Badge tone={toneFor(e.status)}>{e.status}</Badge></td>
                  <td>{fmtDateTime(e.statusEffectiveAt)}</td>
                  <td>{e.createdBy}</td>
                  <td>
                    <button className="btn btn-small" type="button" onClick={() => go({ name: "hr-employee", employeeId: e.id, tab: "overview" })}>
                      Open
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}
    </section>
  );
}

function EmployeeForm({ auth, initial, onDone, onCancel }: { auth: Auth; initial: Employee | null; onDone: () => void; onCancel: () => void }) {
  const departments = useAsync(() => listDepartments(auth), [auth.username]);
  const roles = useAsync(() => listRoleProfiles(auth), [auth.username]);
  const isCreate = initial === null;
  const [form, set] = useForm({
    employeeCode: initial?.employeeCode ?? "",
    fullName: initial?.fullName ?? "",
    birthDate: initial?.birthDate ?? "",
    departmentId: "",
    jobTitle: "",
    roleProfileId: "",
    gender: initial?.gender ?? "",
    address: initial?.address ?? "",
    phone: initial?.phone ?? "",
    email: initial?.email ?? "",
    hireDate: initial?.hireDate ?? "",
  });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const empty = (v: string): string | null => (v.trim() === "" ? null : v.trim());
  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      if (initial === null) {
        await createEmployee(auth, {
          employeeCode: form.employeeCode.trim(),
          fullName: form.fullName.trim(),
          birthDate: empty(form.birthDate),
          departmentId: empty(form.departmentId),
          jobTitle: empty(form.jobTitle),
          roleProfileId: empty(form.roleProfileId),
          gender: empty(form.gender),
          address: empty(form.address),
          phone: empty(form.phone),
          email: empty(form.email),
          hireDate: empty(form.hireDate),
        });
      } else {
        await updateEmployee(auth, initial.id, {
          employeeCode: initial.employeeCode,
          fullName: form.fullName.trim(),
          birthDate: empty(form.birthDate),
          gender: empty(form.gender),
          address: empty(form.address),
          phone: empty(form.phone),
          email: empty(form.email),
          hireDate: empty(form.hireDate),
        });
      }
      onDone();
    } catch (err: unknown) {
      setError(errText(err));
    } finally {
      setBusy(false);
    }
  }
  return (
    <form className="card" onSubmit={submit}>
      <h3>{isCreate ? "New employee" : "Edit employee " + (initial?.employeeCode ?? "")}</h3>
      {isCreate ? (
        <p className="muted">The employee code is required and must be unique. Department and job title are set here; later changes to the department go through a transfer.</p>
      ) : (
        <p className="muted">Department and job title are not changed here; use a transfer for those.</p>
      )}
      <div className="row">
        <Field label="Employee code">
          <input value={form.employeeCode} onChange={(e) => set("employeeCode", e.target.value)} required disabled={!isCreate} />
        </Field>
        <Field label="Full name">
          <input value={form.fullName} onChange={(e) => set("fullName", e.target.value)} required />
        </Field>
      </div>
      <div className="row">
        <Field label="Birth date">
          <input type="date" value={form.birthDate} onChange={(e) => set("birthDate", e.target.value)} />
        </Field>
        <Field label="Hire date">
          <input type="date" value={form.hireDate} onChange={(e) => set("hireDate", e.target.value)} />
        </Field>
        <Field label="Gender">
          <select value={form.gender} onChange={(e) => set("gender", e.target.value)}>
            <option value="">Not specified</option>
            <option value="FEMALE">FEMALE</option>
            <option value="MALE">MALE</option>
            <option value="OTHER">OTHER</option>
          </select>
        </Field>
      </div>
      {isCreate ? (
        <div className="row">
          <Field label="Department">
            <select value={form.departmentId} onChange={(e) => set("departmentId", e.target.value)} required>
              <option value="">Select a department</option>
              {(departments.data ?? []).map((d) => (
                <option key={d.id} value={d.id}>
                  {d.displayName} ({d.departmentCode})
                </option>
              ))}
            </select>
          </Field>
          <Field label="Job title">
            <input value={form.jobTitle} onChange={(e) => set("jobTitle", e.target.value)} required />
          </Field>
          <Field label="Role profile (optional)">
            <select value={form.roleProfileId} onChange={(e) => set("roleProfileId", e.target.value)}>
              <option value="">None</option>
              {(roles.data ?? []).map((r) => (
                <option key={r.id} value={r.id}>
                  {r.titleName}
                </option>
              ))}
            </select>
          </Field>
        </div>
      ) : null}
      <div className="row">
        <Field label="Phone">
          <input value={form.phone} onChange={(e) => set("phone", e.target.value)} />
        </Field>
        <Field label="Email">
          <input type="email" value={form.email} onChange={(e) => set("email", e.target.value)} />
        </Field>
      </div>
      <Field label="Address">
        <input value={form.address} onChange={(e) => set("address", e.target.value)} />
      </Field>
      <ErrorBanner error={error} />
      <SubmitRow busy={busy} label={isCreate ? "Create employee" : "Save changes"} onCancel={onCancel} />
    </form>
  );
}

function hrTabLabel(t: HrTab): string {
  const labels: Record<HrTab, string> = {
    overview: "Overview",
    certs: "Certifications (CCHN)",
    decisions: "Status decisions",
    transfers: "Transfers",
    history: "Status history",
  };
  return labels[t];
}

function EmployeeDetail({ auth, employeeId, tab, go }: { auth: Auth; employeeId: string; tab: HrTab; go: (v: HrView) => void }) {
  const employee = useAsync(() => getEmployee(auth, employeeId), [employeeId, auth.username]);
  const [showEdit, setShowEdit] = useState(false);
  const canHr = auth.role === "HR_SPECIALIST";
  if (employee.loading && employee.data === null) {
    return <p role="status">Loading employee...</p>;
  }
  if (employee.error !== null && employee.data === null) {
    return <ErrorBanner error={employee.error} />;
  }
  const emp = employee.data;
  if (emp === null) {
    return <EmptyNote text="Employee not found." />;
  }
  return (
    <section>
      <button className="btn" type="button" onClick={() => go({ name: "hr-employees" })}>
        Back to employees
      </button>
      <h2>
        {emp.fullName} ({emp.employeeCode})
      </h2>
      <dl className="kv">
        <div><dt>Employee code</dt><dd>{emp.employeeCode}</dd></div>
        <div><dt>Full name</dt><dd>{emp.fullName}</dd></div>
        <div><dt>Birth date</dt><dd>{fmtDate(emp.birthDate)}</dd></div>
        <div><dt>Status</dt><dd><Badge tone={toneFor(emp.status)}>{emp.status}</Badge></dd></div>
        <div><dt>Status effective</dt><dd>{fmtDateTime(emp.statusEffectiveAt)}</dd></div>
        <div><dt>Gender</dt><dd>{emp.gender}</dd></div>
        <div><dt>Phone</dt><dd>{emp.phone}</dd></div>
        <div><dt>Email</dt><dd>{emp.email}</dd></div>
        <div><dt>Address</dt><dd>{emp.address}</dd></div>
        <div><dt>Hire date</dt><dd>{fmtDate(emp.hireDate)}</dd></div>
        <div><dt>Created by</dt><dd>{emp.createdBy}</dd></div>
      </dl>
      {canHr && !showEdit ? (
        <button className="btn" type="button" onClick={() => setShowEdit(true)}>
          Edit employee
        </button>
      ) : null}
      {showEdit ? (
        <EmployeeForm
          auth={auth}
          initial={emp}
          onDone={() => {
            setShowEdit(false);
            employee.reload();
          }}
          onCancel={() => setShowEdit(false)}
        />
      ) : null}
      <nav className="tabs" aria-label="Employee sections">
        {(["overview", "certs", "decisions", "transfers", "history"] as HrTab[]).map((t) => (
          <button key={t} type="button" className={tab === t ? "tab active" : "tab"} onClick={() => go({ name: "hr-employee", employeeId, tab: t })}>
            {hrTabLabel(t)}
          </button>
        ))}
      </nav>
      {tab === "certs" ? <CertificationsPanel auth={auth} employeeId={employeeId} /> : null}
      {tab === "decisions" ? <DecisionsPanel auth={auth} employeeId={employeeId} /> : null}
      {tab === "transfers" ? <TransfersPanel auth={auth} employeeId={employeeId} /> : null}
      {tab === "history" ? <StatusHistoryPanel auth={auth} employeeId={employeeId} /> : null}
    </section>
  );
}

function CertificationsPanel({ auth, employeeId }: { auth: Auth; employeeId: string }) {
  const list = useAsync(() => listCertifications(auth, employeeId), [employeeId, auth.username]);
  const [form, set, reset] = useForm({ credentialType: "CCHN", credentialNo: "", issuer: "", scopeNote: "", validFrom: "", validUntil: "", verificationStatus: "UNVERIFIED", sourceRef: "" });
  const [editing, setEditing] = useState<Certification | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const canHr = auth.role === "HR_SPECIALIST";
  function startEdit(c: Certification) {
    setEditing(c);
    set("credentialType", c.credentialType);
    set("credentialNo", c.credentialNo ?? "");
    set("issuer", c.issuer ?? "");
    set("scopeNote", c.scopeNote ?? "");
    set("validFrom", c.validFrom ?? "");
    set("validUntil", c.validUntil ?? "");
    set("verificationStatus", c.verificationStatus ?? "");
    set("sourceRef", c.sourceRef ?? "");
  }
  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const body = {
        credentialType: form.credentialType.trim(),
        credentialNo: form.credentialNo.trim() === "" ? null : form.credentialNo.trim(),
        issuer: form.issuer.trim() === "" ? null : form.issuer.trim(),
        scopeNote: form.scopeNote.trim() === "" ? null : form.scopeNote.trim(),
        validFrom: form.validFrom === "" ? null : form.validFrom,
        validUntil: form.validUntil === "" ? null : form.validUntil,
        verificationStatus: form.verificationStatus === "" ? null : form.verificationStatus,
        sourceRef: form.sourceRef.trim(),
      };
      if (editing === null) {
        await addCertification(auth, employeeId, body);
      } else {
        await updateCertification(auth, editing.id, body);
      }
      setEditing(null);
      reset();
      list.reload();
    } catch (err: unknown) {
      setError(errText(err));
    } finally {
      setBusy(false);
    }
  }
  return (
    <div>
      <h3>Practice certificates (CCHN)</h3>
      <p className="muted">Validity is computed by the backend: VALID, EXPIRING_SOON (1 to 30 days left), EXPIRED (the day after the expiry date), UNVERIFIED (never eligible) or REJECTED. An expired CCHN does not lock the employee account.</p>
      {list.loading ? <p role="status">Loading certifications...</p> : null}
      <ErrorBanner error={list.error} />
      {list.data !== null && list.data.length === 0 ? <EmptyNote text="No certifications yet." /> : null}
      {list.data !== null && list.data.length > 0 ? (
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>Type</th>
                <th>Number</th>
                <th>Issuer</th>
                <th>Valid from</th>
                <th>Valid until</th>
                <th>Verification</th>
                <th>Validity</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {list.data.map((c) => (
            <tr key={c.id} className={c.validity === "EXPIRED" || c.validity === "EXPIRING_SOON" ? "row-attention" : ""}>
                  <td>{c.credentialType}</td>
                  <td>{c.credentialNo}</td>
                  <td>{c.issuer}</td>
                  <td>{fmtDate(c.validFrom)}</td>
                  <td>{fmtDate(c.validUntil)}</td>
                  <td>{c.verificationStatus}</td>
                  <td><Badge tone={toneFor(c.validity)}>{c.validity}</Badge></td>
                  <td>
                    {canHr ? (
                      <button className="btn btn-small" type="button" onClick={() => startEdit(c)}>
                        Edit
                      </button>
                    ) : null}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}
      {canHr ? (
        <form className="card" onSubmit={submit}>
          <h4>{editing === null ? "New certification" : "Edit certification"}</h4>
          <div className="row">
            <Field label="Credential type">
              <select value={form.credentialType} onChange={(e) => set("credentialType", e.target.value)} required>
                <option value="CCHN">CCHN</option>
                <option value="OTHER">OTHER</option>
              </select>
            </Field>
            <Field label="Credential number">
              <input value={form.credentialNo} onChange={(e) => set("credentialNo", e.target.value)} />
            </Field>
            <Field label="Issuer">
              <input value={form.issuer} onChange={(e) => set("issuer", e.target.value)} />
            </Field>
          </div>
          <Field label="Scope note">
            <input value={form.scopeNote} onChange={(e) => set("scopeNote", e.target.value)} />
          </Field>
          <div className="row">
            <Field label="Valid from">
              <input type="date" value={form.validFrom} onChange={(e) => set("validFrom", e.target.value)} />
            </Field>
            <Field label="Valid until">
              <input type="date" value={form.validUntil} onChange={(e) => set("validUntil", e.target.value)} />
            </Field>
            <Field label="Verification status">
              <select value={form.verificationStatus} onChange={(e) => set("verificationStatus", e.target.value)}>
                <option value="UNVERIFIED">UNVERIFIED</option>
                <option value="VERIFIED">VERIFIED</option>
                <option value="REJECTED">REJECTED</option>
              </select>
            </Field>
          </div>
          <Field label="Source reference">
            <input value={form.sourceRef} onChange={(e) => set("sourceRef", e.target.value)} required />
          </Field>
          <ErrorBanner error={error} />
          <SubmitRow busy={busy} label={editing === null ? "Add certification" : "Save certification"} onCancel={editing === null ? undefined : () => { setEditing(null); reset(); }} />
        </form>
      ) : null}
    </div>
  );
}

function DecisionsPanel({ auth, employeeId }: { auth: Auth; employeeId: string }) {
  const list = useAsync(() => listDecisions(auth, "").then((all) => all.filter((d) => d.employeeId === employeeId)), [employeeId, auth.username]);
  const [form, set, reset] = useForm({ decisionRef: "", decisionType: "ACTIVE", effectiveAt: new Date().toISOString().slice(0, 16), reason: "" });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const canHr = auth.role === "HR_SPECIALIST";
  const canApprove = auth.role === "HR_APPROVER";
  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await createDecision(auth, employeeId, {
        decisionRef: form.decisionRef.trim(),
        decisionType: form.decisionType,
        effectiveAt: toInstant(form.effectiveAt),
        reason: form.reason.trim() === "" ? null : form.reason.trim(),
      });
      reset();
      list.reload();
    } catch (err: unknown) {
      setError(errText(err));
    } finally {
      setBusy(false);
    }
  }
  async function act(d: Decision, action: "approve" | "revoke") {
    setActionError(null);
    try {
      await decisionAction(auth, d.id, action);
      list.reload();
    } catch (err: unknown) {
      setActionError(errText(err));
    }
  }
  async function applyDue() {
    setNotice(null);
    setActionError(null);
    try {
      const applied = await applyDueDecisions(auth);
      setNotice("Apply-due completed. " + applied.length + " decision(s) returned by the server.");
      list.reload();
    } catch (err: unknown) {
      setActionError(errText(err));
    }
  }
  return (
    <div>
      <h3>Status decisions</h3>
      <p className="muted">Status lifecycle: ONBOARDING to ACTIVE, ACTIVE and SUSPENDED, TERMINATED is terminal. A decision takes effect only at its effective time; future decisions are not applied early. The approver must differ from the person who created the decision (four-eyes).</p>
      <div className="row">
        <button className="btn" type="button" onClick={applyDue}>
          Apply due decisions
        </button>
      </div>
      {notice !== null ? <p className="muted">{notice}</p> : null}
      <ErrorBanner error={actionError} />
      {list.loading ? <p role="status">Loading decisions...</p> : null}
      <ErrorBanner error={list.error} />
      {list.data !== null && list.data.length === 0 ? <EmptyNote text="No decisions yet." /> : null}
      {list.data !== null && list.data.length > 0 ? (
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>Reference</th>
                <th>Type</th>
                <th>Status</th>
                <th>Execution</th>
                <th>Effective at</th>
                <th>Approved by</th>
                <th>Reason</th>
                <th>Created by</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {list.data.map((d) => (
            <tr key={d.id} className={d.decisionStatus === "PENDING" ? "row-attention" : ""}>
                  <td>{d.decisionRef}</td>
                  <td>{d.decisionType}</td>
                  <td><Badge tone={toneFor(d.decisionStatus)}>{d.decisionStatus}</Badge></td>
                  <td><Badge tone={toneFor(d.executionStatus)}>{d.executionStatus}</Badge></td>
                  <td>{fmtDateTime(d.effectiveAt)}</td>
                  <td>{d.approvedBy}</td>
                  <td>{d.reason}</td>
                  <td>{d.createdBy}</td>
                  <td>
                    {canApprove && d.decisionStatus === "PENDING" && d.createdBy !== auth.username ? (
                      <button className="btn btn-small" type="button" onClick={() => act(d, "approve")}>
                        Approve
                      </button>
                    ) : null}
                    {canApprove && (d.decisionStatus === "PENDING" || d.decisionStatus === "APPROVED") && d.createdBy !== auth.username ? (
                      <button className="btn btn-small" type="button" onClick={() => act(d, "revoke")}>
                        Revoke
                      </button>
                    ) : null}
                    {canApprove && d.createdBy === auth.username ? <span className="muted">Four-eyes: you created this decision.</span> : null}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}
      {canHr ? (
        <form className="card" onSubmit={submit}>
          <h4>New status decision (HR specialist)</h4>
          <div className="row">
            <Field label="Decision reference">
              <input value={form.decisionRef} onChange={(e) => set("decisionRef", e.target.value)} required />
            </Field>
            <Field label="Decision type">
              <select value={form.decisionType} onChange={(e) => set("decisionType", e.target.value)}>
                <option value="ACTIVE">ACTIVE</option>
                <option value="SUSPENDED">SUSPENDED</option>
                <option value="TERMINATED">TERMINATED</option>
                <option value="ONBOARDING">ONBOARDING</option>
              </select>
            </Field>
            <Field label="Effective at (UTC)">
              <input type="datetime-local" value={form.effectiveAt} onChange={(e) => set("effectiveAt", e.target.value)} required />
            </Field>
          </div>
          <Field label="Reason">
            <textarea value={form.reason} onChange={(e) => set("reason", e.target.value)} />
          </Field>
          <ErrorBanner error={error} />
          <SubmitRow busy={busy} label="Create decision" />
        </form>
      ) : null}
    </div>
  );
}

function TransfersPanel({ auth, employeeId }: { auth: Auth; employeeId: string }) {
  const assignments = useAsync(() => listAssignments(auth, employeeId), [employeeId, auth.username]);
  const departments = useAsync(() => listDepartments(auth), [auth.username]);
  const roles = useAsync(() => listRoleProfiles(auth), [auth.username]);
  const [form, set, reset] = useForm({ departmentId: "", jobTitle: "", roleProfileId: "", effectiveFrom: new Date().toISOString().slice(0, 16), decisionRef: "" });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const canHr = auth.role === "HR_SPECIALIST";
  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await createTransfer(auth, employeeId, {
        departmentId: form.departmentId,
        jobTitle: form.jobTitle.trim(),
        roleProfileId: form.roleProfileId === "" ? null : form.roleProfileId,
        effectiveFrom: toInstant(form.effectiveFrom),
        decisionRef: form.decisionRef.trim() === "" ? null : form.decisionRef.trim(),
      });
      reset();
      assignments.reload();
    } catch (err: unknown) {
      setError(errText(err));
    } finally {
      setBusy(false);
    }
  }
  return (
    <div>
      <h3>Transfers and assignments</h3>
      <p className="muted">A transfer closes the current assignment and opens a new one. Overlapping assignments are rejected. Assignment history is never overwritten.</p>
      {assignments.loading ? <p role="status">Loading assignments...</p> : null}
      <ErrorBanner error={assignments.error} />
      {assignments.data !== null && assignments.data.length === 0 ? <EmptyNote text="No assignments yet." /> : null}
      {assignments.data !== null && assignments.data.length > 0 ? (
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>Assignment</th>
                <th>Department</th>
                <th>Job title</th>
                <th>Role profile</th>
                <th>Start</th>
                <th>End</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {assignments.data.map((a) => (
                <tr key={a.id}>
                  <td>{shortId(a.id)}</td>
                  <td>{a.departmentCode}</td>
                  <td>{a.jobTitle}</td>
                  <td>{a.roleProfileId}</td>
                  <td>{fmtDateTime(a.startAt)}</td>
                  <td>{fmtDateTime(a.endAt)}</td>
                  <td><Badge tone={toneFor(a.status)}>{a.status}</Badge></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}
      {canHr ? (
        <form className="card" onSubmit={submit}>
          <h4>New transfer (HR specialist)</h4>
          <div className="row">
            <Field label="Department">
              <select value={form.departmentId} onChange={(e) => set("departmentId", e.target.value)} required>
                <option value="">Select a department</option>
                {(departments.data ?? []).map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.displayName} ({d.departmentCode})
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Job title">
              <input value={form.jobTitle} onChange={(e) => set("jobTitle", e.target.value)} required />
            </Field>
            <Field label="Role profile (optional)">
              <select value={form.roleProfileId} onChange={(e) => set("roleProfileId", e.target.value)}>
                <option value="">None</option>
                {(roles.data ?? []).map((r) => (
                  <option key={r.id} value={r.id}>
                    {r.titleName}
                  </option>
                ))}
              </select>
            </Field>
          </div>
          <div className="row">
            <Field label="Effective from (UTC)">
              <input type="datetime-local" value={form.effectiveFrom} onChange={(e) => set("effectiveFrom", e.target.value)} required />
            </Field>
            <Field label="Decision reference (optional)">
              <input value={form.decisionRef} onChange={(e) => set("decisionRef", e.target.value)} />
            </Field>
          </div>
          <ErrorBanner error={error} />
          <SubmitRow busy={busy} label="Record transfer" />
        </form>
      ) : null}
    </div>
  );
}

function StatusHistoryPanel({ auth, employeeId }: { auth: Auth; employeeId: string }) {
  const history = useAsync(() => listStatusHistory(auth, employeeId), [employeeId, auth.username]);
  return (
    <div>
      <h3>Status history</h3>
      <p className="muted">Every status change is kept here with its effective time and the decision that caused it. Records are never deleted.</p>
      {history.loading ? <p role="status">Loading status history...</p> : null}
      <ErrorBanner error={history.error} />
      {history.data !== null && history.data.length === 0 ? <EmptyNote text="No status history yet." /> : null}
      {history.data !== null && history.data.length > 0 ? (
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>From</th>
                <th>To</th>
                <th>Effective at</th>
                <th>Decision</th>
                <th>Recorded at</th>
              </tr>
            </thead>
            <tbody>
              {history.data.map((h) => (
                <tr key={h.id}>
                  <td>{h.oldStatus}</td>
                  <td><Badge tone={toneFor(h.newStatus)}>{h.newStatus}</Badge></td>
                  <td>{fmtDateTime(h.effectiveAt)}</td>
                  <td>{h.decisionId}</td>
                  <td>{fmtDateTime(h.createdAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}
    </div>
  );
}

function DepartmentList({ auth }: { auth: Auth }) {
  const list = useAsync(() => listDepartments(auth), [auth.username]);
  const [form, set, reset] = useForm({ departmentCode: "", displayName: "", parentDepartmentId: "", active: "true", departmentType: "" });
  const [editing, setEditing] = useState<Department | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const canHr = auth.role === "HR_SPECIALIST";
  useEffect(() => {
    const focusForm = () => {
      const form = document.getElementById("department-form");
      if (form !== null) {
        form.scrollIntoView({ behavior: "smooth", block: "start" });
        const first = form.querySelector("input, select, textarea");
        if (first instanceof HTMLElement) first.focus();
      }
    };
    window.addEventListener("smarthms:primary-action", focusForm);
    return () => window.removeEventListener("smarthms:primary-action", focusForm);
  }, []);
  function startEdit(d: Department) {
    setEditing(d);
    set("departmentCode", d.departmentCode);
    set("displayName", d.displayName);
    set("parentDepartmentId", d.parentDepartmentId ?? "");
    set("active", d.active ? "true" : "false");
    set("departmentType", d.departmentType ?? "");
  }
  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const body = {
        departmentCode: form.departmentCode.trim(),
        displayName: form.displayName.trim(),
        parentDepartmentId: form.parentDepartmentId.trim() === "" ? null : form.parentDepartmentId.trim(),
        active: form.active === "true",
        departmentType: form.departmentType.trim() === "" ? null : form.departmentType.trim(),
      };
      if (editing === null) {
        await createDepartment(auth, body);
      } else {
        await updateDepartment(auth, editing.id, body);
      }
      setEditing(null);
      reset();
      list.reload();
    } catch (err: unknown) {
      setError(errText(err));
    } finally {
      setBusy(false);
    }
  }
  return (
    <section>
      <h2>Departments</h2>
      <p className="muted">A department must exist and be active before an employee can be assigned to it.</p>
      {list.loading ? <p role="status">Loading departments...</p> : null}
      <ErrorBanner error={list.error} />
      {list.data !== null && list.data.length === 0 ? <EmptyNote text="No departments yet." /> : null}
      {list.data !== null && list.data.length > 0 ? (
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>Code</th>
                <th>Name</th>
                <th>Parent</th>
                <th>Type</th>
                <th>Active</th>
                <th>Created by</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {list.data.map((d) => (
                <tr key={d.id}>
                  <td>{d.departmentCode}</td>
                  <td>{d.displayName}</td>
                  <td>{d.parentDepartmentId}</td>
                  <td>{d.departmentType}</td>
                  <td><Badge tone={d.active ? "ok" : "bad"}>{d.active ? "ACTIVE" : "INACTIVE"}</Badge></td>
                  <td>{d.createdBy}</td>
                  <td>
                    {canHr ? (
                      <button className="btn btn-small" type="button" onClick={() => startEdit(d)}>
                        Edit
                      </button>
                    ) : null}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}
      {canHr ? (
        <form id="department-form" className="card" onSubmit={submit}>
          <h3>{editing === null ? "New department" : "Edit department"}</h3>
          <div className="row">
            <Field label="Department code">
              <input value={form.departmentCode} onChange={(e) => set("departmentCode", e.target.value)} required />
            </Field>
            <Field label="Display name">
              <input value={form.displayName} onChange={(e) => set("displayName", e.target.value)} required />
            </Field>
            <Field label="Department type (optional)">
              <input value={form.departmentType} onChange={(e) => set("departmentType", e.target.value)} />
            </Field>
          </div>
          <div className="row">
            <Field label="Parent department ID (optional)">
              <input value={form.parentDepartmentId} onChange={(e) => set("parentDepartmentId", e.target.value)} />
            </Field>
            <Field label="Active">
              <select value={form.active} onChange={(e) => set("active", e.target.value)}>
                <option value="true">Active</option>
                <option value="false">Inactive</option>
              </select>
            </Field>
          </div>
          <ErrorBanner error={error} />
          <SubmitRow busy={busy} label={editing === null ? "Create department" : "Save department"} onCancel={editing === null ? undefined : () => { setEditing(null); reset(); }} />
        </form>
      ) : null}
    </section>
  );
}

function RoleProfileList({ auth }: { auth: Auth }) {
  const list = useAsync(() => listRoleProfiles(auth), [auth.username]);
  const [form, set, reset] = useForm({ titleName: "", professionalGroup: "", qualification: "", description: "" });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const canHr = auth.role === "HR_SPECIALIST";
  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await createRoleProfile(auth, {
        titleName: form.titleName.trim(),
        professionalGroup: form.professionalGroup.trim() === "" ? null : form.professionalGroup.trim(),
        qualification: form.qualification.trim() === "" ? null : form.qualification.trim(),
        description: form.description.trim() === "" ? null : form.description.trim(),
      });
      reset();
      list.reload();
    } catch (err: unknown) {
      setError(errText(err));
    } finally {
      setBusy(false);
    }
  }
  return (
    <section>
      <h2>Role profiles</h2>
      <p className="muted">Role profiles name the professional group and qualification used when an employee is hired or transferred.</p>
      {list.loading ? <p role="status">Loading role profiles...</p> : null}
      <ErrorBanner error={list.error} />
      {list.data !== null && list.data.length === 0 ? <EmptyNote text="No role profiles yet." /> : null}
      {list.data !== null && list.data.length > 0 ? (
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>Title</th>
                <th>Professional group</th>
                <th>Qualification</th>
                <th>Description</th>
                <th>Created by</th>
              </tr>
            </thead>
            <tbody>
              {list.data.map((r) => (
                <tr key={r.id}>
                  <td>{r.titleName}</td>
                  <td>{r.professionalGroup}</td>
                  <td>{r.qualification}</td>
                  <td>{r.description}</td>
                  <td>{r.createdBy}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}
      {canHr ? (
        <form className="card" onSubmit={submit}>
          <h3>New role profile</h3>
          <Field label="Title name">
            <input value={form.titleName} onChange={(e) => set("titleName", e.target.value)} required />
          </Field>
          <div className="row">
            <Field label="Professional group (optional)">
              <input value={form.professionalGroup} onChange={(e) => set("professionalGroup", e.target.value)} />
            </Field>
            <Field label="Qualification (optional)">
              <input value={form.qualification} onChange={(e) => set("qualification", e.target.value)} />
            </Field>
          </div>
          <Field label="Description (optional)">
            <textarea value={form.description} onChange={(e) => set("description", e.target.value)} />
          </Field>
          <ErrorBanner error={error} />
          <SubmitRow busy={busy} label="Create role profile" />
        </form>
      ) : null}
    </section>
  );
}
