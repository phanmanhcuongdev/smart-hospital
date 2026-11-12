import { useEffect, useState, type FormEvent } from "react";
import { statusLabel } from "../../lib/labels";
import type { Auth } from "../../lib/api";
import { addCertification, applyDueDecisions, createDecision, updateDecision, deleteDecision, createDepartment, createEmployee, createRoleProfile, updateRoleProfile, deleteRoleProfile, createTransfer, decisionAction, getEmployee, listAssignments, listCertifications, listDecisions, listDepartments, listRoleProfiles, listStatusHistory, searchEmployees, updateCertification, deleteCertification, updateDepartment, deleteDepartment, updateEmployee, deleteEmployee, type Certification, type Decision, type Department, type Employee, type RoleProfile } from "../../lib/api";
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
  const [filters, setFilters] = useState({ departmentId: "", status: "", jobTitle: "", q: "" });
  const [query, setQuery] = useState("");
  const employees = useAsync(() => searchEmployees(auth, query), [query, auth.username]);
  const [showForm, setShowForm] = useState(false);
  useEffect(() => {
    const open = () => { if (auth.role === "HR_SPECIALIST" || auth.role === "ADMIN") setShowForm(true); };
    window.addEventListener("smarthms:primary-action", open);
    return () => window.removeEventListener("smarthms:primary-action", open);
  }, [auth.role]);
  const canHr = auth.role === "HR_SPECIALIST" || auth.role === "ADMIN";
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
    if (filters.q.trim() !== "") {
      params.set("q", filters.q.trim());
    }
    const q = params.toString();
    setQuery(q === "" ? "" : "?" + q);
  }
  return (
    <section>
      <h2>{"Nhân viên"}</h2>
      <p className="muted">{"Tìm kiếm và lọc nhân viên theo phòng ban, trạng thái và chức danh. Tạo nhân viên không cấp quyền hệ thống."}</p>
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
        <h3>{"Tìm kiếm"}</h3>
        <div className="row">
          <Field label={"Phòng ban"}>
            <select value={filters.departmentId} onChange={(e) => setFilters((f) => ({ ...f, departmentId: e.target.value }))}>
              <option value="">{"Tất cả phòng ban"}</option>
              {(departments.data ?? []).map((d) => (
                <option key={d.id} value={d.id}>
                  {d.displayName} ({d.departmentCode})
                </option>
              ))}
            </select>
          </Field>
          <Field label={"Trạng thái"}>
            <select value={filters.status} onChange={(e) => setFilters((f) => ({ ...f, status: e.target.value }))}>
              <option value="">{"Tất cả trạng thái"}</option>
              <option value="ONBOARDING">{"Đang nhận việc"}</option>
              <option value="ACTIVE">{"Đang hoạt động"}</option>
              <option value="SUSPENDED">{"Tạm đình chỉ"}</option>
              <option value="TERMINATED">{"Đã nghỉ việc"}</option>
            </select>
          </Field>
          <Field label={"Chức danh"}>
            <input value={filters.jobTitle} onChange={(e) => setFilters((f) => ({ ...f, jobTitle: e.target.value }))} />
          </Field>
          <Field label={"Tìm kiếm"}>
            <input value={filters.q} onChange={(e) => setFilters((f) => ({ ...f, q: e.target.value }))} placeholder={"Tên hoặc mã nhân viên"} />
          </Field>
        </div>
        <SubmitRow busy={false} label={"Tìm kiếm"} />
      </form>
      {employees.loading ? <p role="status">{"Đang tải danh sách nhân viên..."}</p> : null}
      <ErrorBanner error={employees.error} />
      {employees.data !== null ? <ResultCount shown={employees.data.length} total={employees.data.length} noun={"nhân viên"} /> : null}
      {employees.data !== null && employees.data.length === 0 ? <EmptyNote text={"Không có nhân viên nào khớp với tìm kiếm."} /> : null}
      {employees.data !== null && employees.data.length > 0 ? (
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>{"Mã"}</th>
                <th>{"Họ và tên"}</th>
                <th>{"Ngày sinh"}</th>
                <th>{"Trạng thái"}</th>
                <th>{"Trạng thái hiệu lực"}</th>
                <th>{"Người tạo"}</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {employees.data.map((e) => (
                <tr key={e.id}>
                  <td>{e.employeeCode}</td>
                  <td>{e.fullName}</td>
                  <td>{fmtDate(e.birthDate)}</td>
                  <td><Badge tone={toneFor(e.status)}>{statusLabel(e.status)}</Badge></td>
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
          departmentId: empty(form.departmentId),
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
        <p className="muted">{"Mã nhân viên là bắt buộc và phải duy nhất. Phòng ban và chức danh được đặt tại đây; các thay đổi phòng ban về sau thực hiện qua chuyển đổi."}</p>
      ) : (
        <p className="muted">{"Phòng ban và chức danh không thay đổi tại đây; hãy dùng chuyển đổi cho các thay đổi đó."}</p>
      )}
      <div className="row">
        <Field label={"Mã nhân viên"}>
          <input value={form.employeeCode} onChange={(e) => set("employeeCode", e.target.value)} required disabled={!isCreate} />
        </Field>
        <Field label={"Họ và tên"}>
          <input value={form.fullName} onChange={(e) => set("fullName", e.target.value)} required />
        </Field>
      </div>
      <div className="row">
        <Field label={"Ngày sinh"}>
          <input type="date" value={form.birthDate} onChange={(e) => set("birthDate", e.target.value)} />
        </Field>
        <Field label={"Ngày tuyển dụng"}>
          <input type="date" value={form.hireDate} onChange={(e) => set("hireDate", e.target.value)} />
        </Field>
        <Field label={"Giới tính"}>
          <select value={form.gender} onChange={(e) => set("gender", e.target.value)}>
            <option value="">{"Chưa chỉ định"}</option>
            <option value="FEMALE">{"Nữ"}</option>
            <option value="MALE">{"Nam"}</option>
            <option value="OTHER">{"Khác"}</option>
          </select>
        </Field>
      </div>
      {isCreate ? (
        <div className="row">
          <Field label={"Phòng ban"}>
            <select value={form.departmentId} onChange={(e) => set("departmentId", e.target.value)} required>
              <option value="">{"Chọn phòng ban"}</option>
              {(departments.data ?? []).map((d) => (
                <option key={d.id} value={d.id}>
                  {d.displayName} ({d.departmentCode})
                </option>
              ))}
            </select>
          </Field>
          <Field label={"Chức danh"}>
            <input value={form.jobTitle} onChange={(e) => set("jobTitle", e.target.value)} required />
          </Field>
          <Field label={"Hồ sơ vai trò (tùy chọn)"}>
            <select value={form.roleProfileId} onChange={(e) => set("roleProfileId", e.target.value)}>
              <option value="">{"Không có"}</option>
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
        <Field label={"Điện thoại"}>
          <input value={form.phone} onChange={(e) => set("phone", e.target.value)} />
        </Field>
        <Field label={"Email"}>
          <input type="email" value={form.email} onChange={(e) => set("email", e.target.value)} />
        </Field>
      </div>
      <Field label={"Địa chỉ"}>
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

  async function removeEmployee() {
    if (emp == null) return;
    if (!window.confirm("Xóa nhân viên này? Hành động này không thể hoàn tác.")) return;
    try {
      await deleteEmployee(auth, emp.id);
      go({ name: "hr-employees" });
    } catch (e) {
      window.alert(errText(e));
    }
  }
  const canHr = auth.role === "HR_SPECIALIST" || auth.role === "ADMIN";
  if (employee.loading && employee.data === null) {
    return <p role="status">{"Đang tải nhân viên..."}</p>;
  }
  if (employee.error !== null && employee.data === null) {
    return <ErrorBanner error={employee.error} />;
  }
  const emp = employee.data;
  if (emp === null) {
    return <EmptyNote text={"Không tìm thấy nhân viên."} />;
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
        <div><dt>{"Mã nhân viên"}</dt><dd>{emp.employeeCode}</dd></div>
        <div><dt>{"Họ và tên"}</dt><dd>{emp.fullName}</dd></div>
        <div><dt>{"Ngày sinh"}</dt><dd>{fmtDate(emp.birthDate)}</dd></div>
        <div><dt>{"Trạng thái"}</dt><dd><Badge tone={toneFor(emp.status)}>{statusLabel(emp.status)}</Badge></dd></div>
        <div><dt>{"Trạng thái hiệu lực"}</dt><dd>{fmtDateTime(emp.statusEffectiveAt)}</dd></div>
        <div><dt>{"Giới tính"}</dt><dd>{emp.gender}</dd></div>
        <div><dt>{"Điện thoại"}</dt><dd>{emp.phone}</dd></div>
        <div><dt>{"Email"}</dt><dd>{emp.email}</dd></div>
        <div><dt>{"Địa chỉ"}</dt><dd>{emp.address}</dd></div>
        <div><dt>{"Ngày tuyển dụng"}</dt><dd>{fmtDate(emp.hireDate)}</dd></div>
        <div><dt>{"Người tạo"}</dt><dd>{emp.createdBy}</dd></div>
      </dl>
      {canHr && !showEdit ? (
        <button className="btn" type="button" onClick={() => setShowEdit(true)}>
          Edit employee
        </button>
      ) : null}
      {canHr && !showEdit ? (
        <button className="btn" type="button" onClick={removeEmployee}>
          {"Xóa nhân viên"}
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
      <nav className="tabs" aria-label={"Các mục nhân viên"}>
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
  const canHr = auth.role === "HR_SPECIALIST" || auth.role === "ADMIN";
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
      <h3>{"Chứng chỉ hành nghề (CCHN)"}</h3>
      <p className="muted">{"Tính hiệu lực do backend tính: VALID, EXPIRING_SOON (còn 1 đến 30 ngày), EXPIRED (ngày sau ngày hết hạn), UNVERIFIED (không bao giờ đủ điều kiện) hoặc REJECTED. CCHN hết hạn không khóa tài khoản nhân viên."}</p>
      {list.loading ? <p role="status">{"Đang tải chứng chỉ..."}</p> : null}
      <ErrorBanner error={list.error} />
      {list.data !== null && list.data.length === 0 ? <EmptyNote text={"Chưa có chứng chỉ nào."} /> : null}
      {list.data !== null && list.data.length > 0 ? (
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>{"Loại"}</th>
                <th>{"Số"}</th>
                <th>{"Đơn vị cấp"}</th>
                <th>{"Có hiệu lực từ"}</th>
                <th>{"Có hiệu lực đến"}</th>
                <th>{"Xác minh"}</th>
                <th>{"Hiệu lực"}</th>
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
                  <td><Badge tone={toneFor(c.validity)}>{statusLabel(c.validity)}</Badge></td>
                  <td>
                    {canHr ? (
                      <button className="btn btn-small" type="button" onClick={() => startEdit(c)}>
                        Edit
                      </button>
          
          ) : null}
          {canHr ? (
            <button className="btn btn-small" type="button" onClick={async () => { if (!window.confirm("Xóa chứng chỉ này? Hành động này không thể hoàn tác.")) return; try { await deleteCertification(auth, c.id); list.reload(); } catch (err: unknown) { setError(errText(err)); } }}>
                        {"Xóa"}
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
            <Field label={"Loại chứng chỉ"}>
              <select value={form.credentialType} onChange={(e) => set("credentialType", e.target.value)} required>
                <option value="CCHN">{"CCHN"}</option>
                <option value="OTHER">{"Khác"}</option>
              </select>
            </Field>
            <Field label={"Số chứng chỉ"}>
              <input value={form.credentialNo} onChange={(e) => set("credentialNo", e.target.value)} />
            </Field>
            <Field label={"Đơn vị cấp"}>
              <input value={form.issuer} onChange={(e) => set("issuer", e.target.value)} />
            </Field>
          </div>
          <Field label={"Ghi chú phạm vi"}>
            <input value={form.scopeNote} onChange={(e) => set("scopeNote", e.target.value)} />
          </Field>
          <div className="row">
            <Field label={"Có hiệu lực từ"}>
              <input type="date" value={form.validFrom} onChange={(e) => set("validFrom", e.target.value)} />
            </Field>
            <Field label={"Có hiệu lực đến"}>
              <input type="date" value={form.validUntil} onChange={(e) => set("validUntil", e.target.value)} />
            </Field>
            <Field label={"Trạng thái xác minh"}>
              <select value={form.verificationStatus} onChange={(e) => set("verificationStatus", e.target.value)}>
                <option value="UNVERIFIED">{"Chưa xác minh"}</option>
                <option value="VERIFIED">{"Đã xác minh"}</option>
                <option value="REJECTED">{"Đã từ chối"}</option>
              </select>
            </Field>
          </div>
          <Field label={"Tham chiếu nguồn"}>
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
  const [form, set, reset] = useForm({ decisionRef: "", decisionType: "ACTIVATE", effectiveAt: new Date().toISOString().slice(0, 16), reason: "" });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const canHr = auth.role === "HR_SPECIALIST" || auth.role === "ADMIN";
  const canApprove = auth.role === "HR_APPROVER" || auth.role === "ADMIN";
  const [editing, setEditing] = useState<Decision | null>(null);
  function startEdit(d: Decision) {
    setEditing(d);
    set("decisionRef", d.decisionRef);
    set("decisionType", d.decisionType);
    set("effectiveAt", d.effectiveAt.slice(0, 16));
    set("reason", d.reason ?? "");
    setError(null);
  }
  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const body = {
        decisionRef: form.decisionRef.trim(),
        decisionType: form.decisionType,
        effectiveAt: toInstant(form.effectiveAt),
        reason: form.reason.trim() === "" ? null : form.reason.trim(),
      };
    if (editing) {
      await updateDecision(auth, editing.id, body);
      setEditing(null);
    } else {
      await createDecision(auth, employeeId, body);
    }
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
      <h3>{"Quyết định trạng thái"}</h3>
      <p className="muted">{"Vòng đời trạng thái: ONBOARDING sang ACTIVE, ACTIVE và SUSPENDED, TERMINATED là trạng thái cuối. Quyết định chỉ có hiệu lực vào thời điểm hiệu lực của nó; các quyết định trong tương lai không được áp dụng sớm. Người phê duyệt phải khác người tạo quyết định (bốn mắt)."}</p>
      <div className="row">
        <button className="btn" type="button" onClick={applyDue}>
          Apply due decisions
        </button>
      </div>
      {notice !== null ? <p className="muted">{notice}</p> : null}
      <ErrorBanner error={actionError} />
      {list.loading ? <p role="status">{"Đang tải quyết định..."}</p> : null}
      <ErrorBanner error={list.error} />
      {list.data !== null && list.data.length === 0 ? <EmptyNote text={"Chưa có quyết định nào."} /> : null}
      {list.data !== null && list.data.length > 0 ? (
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>{"Tham chiếu"}</th>
                <th>{"Loại"}</th>
                <th>{"Trạng thái"}</th>
                <th>{"Thi hành"}</th>
                <th>{"Hiệu lực lúc"}</th>
                <th>{"Người duyệt"}</th>
                <th>{"Lý do"}</th>
                <th>{"Người tạo"}</th>
                <th>{"Thao tác"}</th>
              </tr>
            </thead>
            <tbody>
              {list.data.map((d) => (
            <tr key={d.id} className={d.decisionStatus === "DRAFT" ? "row-attention" : ""}>
                  <td>{d.decisionRef}</td>
                  <td>{statusLabel(d.decisionType)}</td>
                  <td><Badge tone={toneFor(d.decisionStatus)}>{statusLabel(d.decisionStatus)}</Badge></td>
                  <td><Badge tone={toneFor(d.executionStatus)}>{statusLabel(d.executionStatus)}</Badge></td>
                  <td>{fmtDateTime(d.effectiveAt)}</td>
                  <td>{d.approvedBy}</td>
                  <td>{d.reason}</td>
                  <td>{d.createdBy}</td>
                  <td>
                    {canApprove && d.decisionStatus === "DRAFT" && d.createdBy !== auth.username ? (
                      <button className="btn btn-small" type="button" onClick={() => act(d, "approve")}>
                        Approve
                      </button>
                    ) : null}
                    {canApprove && (d.decisionStatus === "DRAFT" || (d.decisionStatus === "APPROVED" && d.executionStatus === "PENDING")) && d.createdBy !== auth.username ? (
                      <button className="btn btn-small" type="button" onClick={() => act(d, "revoke")}>
                        Revoke
                      </button>
                    ) : null}
                    {canApprove && d.createdBy === auth.username ? <span className="muted">{"Nguyên tắc bốn mắt: bạn là người tạo quyết định này."}</span> : null}
      {canHr && d.decisionStatus === "DRAFT" ? (
        <button className="btn btn-small" type="button" onClick={() => startEdit(d)}>
          {"Sửa quyết định"}
        </button>
      ) : null}
      {canHr && d.decisionStatus === "DRAFT" ? (
        <button className="btn btn-small" type="button" onClick={async () => { if (!window.confirm("Xóa quyết định này? Hành động này không thể hoàn tác.")) return; try { await deleteDecision(auth, d.id); list.reload(); } catch (err: unknown) { setActionError(errText(err)); } }}>
          {"Xóa quyết định"}
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
          <h4>{editing === null ? "Quyết định trạng thái mới (chuyên viên nhân sự)" : "Sửa quyết định"}</h4>
          <div className="row">
            <Field label={"Số hiệu quyết định"}>
              <input value={form.decisionRef} onChange={(e) => set("decisionRef", e.target.value)} required />
            </Field>
            <Field label={"Loại quyết định"}>
              <select value={form.decisionType} onChange={(e) => set("decisionType", e.target.value)}>
                <option value="ACTIVATE">{"Kích hoạt"}</option>
                <option value="SUSPEND">{"Tạm đình chỉ"}</option>
                <option value="RETURN">{"Trở lại làm việc"}</option>
                <option value="TERMINATE">{"Chấm dứt"}</option>
                <option value="TRANSFER">{"Điều chuyển"}</option>
              </select>
            </Field>
            <Field label={"Hiệu lực lúc (UTC)"}>
              <input type="datetime-local" value={form.effectiveAt} onChange={(e) => set("effectiveAt", e.target.value)} required />
            </Field>
          </div>
          <Field label={"Lý do"}>
            <textarea value={form.reason} onChange={(e) => set("reason", e.target.value)} />
          </Field>
          <ErrorBanner error={error} />
          <SubmitRow busy={busy} label={editing === null ? "Tạo quyết định" : "Lưu quyết định"}  onCancel={editing === null ? undefined : () => { setEditing(null); reset(); }} />
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
  const canHr = auth.role === "HR_SPECIALIST" || auth.role === "ADMIN";
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
      <h3>{"Chuyển đổi và phân công"}</h3>
      <p className="muted">{"Một lần chuyển đổi sẽ đóng phân công hiện tại và mở phân công mới. Các phân công trùng thời gian bị từ chối. Lịch sử phân công không bao giờ bị ghi đè."}</p>
      {assignments.loading ? <p role="status">{"Đang tải phân công..."}</p> : null}
      <ErrorBanner error={assignments.error} />
      {assignments.data !== null && assignments.data.length === 0 ? <EmptyNote text={"Chưa có phân công nào."} /> : null}
      {assignments.data !== null && assignments.data.length > 0 ? (
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>{"Phân công"}</th>
                <th>{"Phòng ban"}</th>
                <th>{"Chức danh"}</th>
                <th>{"Hồ sơ vai trò"}</th>
                <th>{"Bắt đầu"}</th>
                <th>{"Kết thúc"}</th>
                <th>{"Trạng thái"}</th>
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
                  <td><Badge tone={toneFor(a.status)}>{statusLabel(a.status)}</Badge></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}
      {canHr ? (
        <form className="card" onSubmit={submit}>
          <h4>{"Chuyển mới (chuyên viên nhân sự)"}</h4>
          <div className="row">
            <Field label={"Phòng ban"}>
              <select value={form.departmentId} onChange={(e) => set("departmentId", e.target.value)} required>
                <option value="">{"Chọn phòng ban"}</option>
                {(departments.data ?? []).map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.displayName} ({d.departmentCode})
                  </option>
                ))}
              </select>
            </Field>
            <Field label={"Chức danh"}>
              <input value={form.jobTitle} onChange={(e) => set("jobTitle", e.target.value)} required />
            </Field>
            <Field label={"Hồ sơ vai trò (tùy chọn)"}>
              <select value={form.roleProfileId} onChange={(e) => set("roleProfileId", e.target.value)}>
                <option value="">{"Không có"}</option>
                {(roles.data ?? []).map((r) => (
                  <option key={r.id} value={r.id}>
                    {r.titleName}
                  </option>
                ))}
              </select>
            </Field>
          </div>
          <div className="row">
            <Field label={"Hiệu lực từ (UTC)"}>
              <input type="datetime-local" value={form.effectiveFrom} onChange={(e) => set("effectiveFrom", e.target.value)} required />
            </Field>
            <Field label={"Số hiệu quyết định (tùy chọn)"}>
              <input value={form.decisionRef} onChange={(e) => set("decisionRef", e.target.value)} />
            </Field>
          </div>
          <ErrorBanner error={error} />
          <SubmitRow busy={busy} label={"Ghi nhận chuyển đổi"} />
        </form>
      ) : null}
    </div>
  );
}

function StatusHistoryPanel({ auth, employeeId }: { auth: Auth; employeeId: string }) {
  const history = useAsync(() => listStatusHistory(auth, employeeId), [employeeId, auth.username]);
  return (
    <div>
      <h3>{"Lịch sử trạng thái"}</h3>
      <p className="muted">{"Mọi thay đổi trạng thái được lưu tại đây kèm thời điểm hiệu lực và quyết định gây ra thay đổi. Hồ sơ không bao giờ bị xóa."}</p>
      {history.loading ? <p role="status">{"Đang tải lịch sử trạng thái..."}</p> : null}
      <ErrorBanner error={history.error} />
      {history.data !== null && history.data.length === 0 ? <EmptyNote text={"Chưa có lịch sử trạng thái nào."} /> : null}
      {history.data !== null && history.data.length > 0 ? (
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>{"Từ"}</th>
                <th>{"Đến"}</th>
                <th>{"Hiệu lực lúc"}</th>
                <th>{"Quyết định"}</th>
                <th>{"Ghi nhận lúc"}</th>
              </tr>
            </thead>
            <tbody>
              {history.data.map((h) => (
                <tr key={h.id}>
                  <td>{h.oldStatus}</td>
                  <td><Badge tone={toneFor(h.newStatus)}>{statusLabel(h.newStatus)}</Badge></td>
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
  const canHr = auth.role === "HR_SPECIALIST" || auth.role === "ADMIN";
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
      <h2>{"Phòng ban"}</h2>
      <p className="muted">{"Phòng ban phải tồn tại và đang hoạt động thì nhân viên mới được phân công về đó."}</p>
      {list.loading ? <p role="status">{"Đang tải phòng ban..."}</p> : null}
      <ErrorBanner error={list.error} />
      {list.data !== null && list.data.length === 0 ? <EmptyNote text={"Chưa có phòng ban nào."} /> : null}
      {list.data !== null && list.data.length > 0 ? (
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>{"Mã"}</th>
                <th>{"Tên"}</th>
                <th>{"Phòng ban cha"}</th>
                <th>{"Loại"}</th>
                <th>{"Đang hoạt động"}</th>
                <th>{"Người tạo"}</th>
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
          {canHr ? (
            <button className="btn btn-small" type="button" onClick={async () => { if (!window.confirm("Xóa phòng ban này? Hành động này không thể hoàn tác.")) return; try { await deleteDepartment(auth, d.id); list.reload(); } catch (err: unknown) { setError(errText(err)); } }}>
                        {"Xóa"}
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
            <Field label={"Mã phòng ban"}>
              <input value={form.departmentCode} onChange={(e) => set("departmentCode", e.target.value)} required />
            </Field>
            <Field label={"Tên hiển thị"}>
              <input value={form.displayName} onChange={(e) => set("displayName", e.target.value)} required />
            </Field>
            <Field label={"Loại phòng ban (tùy chọn)"}>
              <input value={form.departmentType} onChange={(e) => set("departmentType", e.target.value)} />
            </Field>
          </div>
          <div className="row">
            <Field label={"ID phòng ban cha (tùy chọn)"}>
              <input value={form.parentDepartmentId} onChange={(e) => set("parentDepartmentId", e.target.value)} />
            </Field>
            <Field label={"Đang hoạt động"}>
              <select value={form.active} onChange={(e) => set("active", e.target.value)}>
                <option value="true">{"Đang hoạt động"}</option>
                <option value="false">{"Không hoạt động"}</option>
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
  const canHr = auth.role === "HR_SPECIALIST" || auth.role === "ADMIN";
  const [editing, setEditing] = useState<RoleProfile | null>(null);
  function startEdit(r: RoleProfile) {
    setEditing(r);
    set("titleName", r.titleName);
    set("professionalGroup", r.professionalGroup ?? "");
    set("qualification", r.qualification ?? "");
    set("description", r.description ?? "");
    setError(null);
  }
  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const body = {
        titleName: form.titleName.trim(),
        professionalGroup: form.professionalGroup.trim() === "" ? null : form.professionalGroup.trim(),
        qualification: form.qualification.trim() === "" ? null : form.qualification.trim(),
        description: form.description.trim() === "" ? null : form.description.trim(),
      };
      if (editing) {
        await updateRoleProfile(auth, editing.id, body);
        setEditing(null);
      } else {
        await createRoleProfile(auth, body);
      }
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
      <h2>{"Hồ sơ vai trò"}</h2>
      <p className="muted">{"Hồ sơ vai trò nêu nhóm chuyên môn và trình độ chuyên môn dùng khi nhân viên được tuyển dụng hoặc chuyển đổi."}</p>
      {list.loading ? <p role="status">{"Đang tải hồ sơ vai trò..."}</p> : null}
      <ErrorBanner error={list.error} />
      {list.data !== null && list.data.length === 0 ? <EmptyNote text={"Chưa có hồ sơ vai trò nào."} /> : null}
      {list.data !== null && list.data.length > 0 ? (
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>{"Chức danh"}</th>
                <th>{"Nhóm chuyên môn"}</th>
                <th>{"Trình độ chuyên môn"}</th>
                <th>{"Mô tả"}</th>
                <th>{"Người tạo"}</th>
        <th>{"Thao tác"}</th>
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
      <td>
        {canHr ? (
          <button className="btn btn-small" type="button" onClick={() => startEdit(r)}>
            {"Sửa hồ sơ chức danh"}
          </button>
        ) : null}
        {canHr ? (
          <button className="btn btn-small" type="button" onClick={async () => { if (!window.confirm("Xóa hồ sơ chức danh này? Hành động này không thể hoàn tác.")) return; try { await deleteRoleProfile(auth, r.id); list.reload(); } catch (err: unknown) { setError(errText(err)); } }}>
            {"Xóa hồ sơ chức danh"}
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
          <h3>{"Hồ sơ vai trò mới"}</h3>
          <Field label={"Tên chức danh"}>
            <input value={form.titleName} onChange={(e) => set("titleName", e.target.value)} required />
          </Field>
          <div className="row">
            <Field label={"Nhóm chuyên môn (tùy chọn)"}>
              <input value={form.professionalGroup} onChange={(e) => set("professionalGroup", e.target.value)} />
            </Field>
            <Field label={"Trình độ chuyên môn (tùy chọn)"}>
              <input value={form.qualification} onChange={(e) => set("qualification", e.target.value)} />
            </Field>
          </div>
          <Field label={"Mô tả (tùy chọn)"}>
            <textarea value={form.description} onChange={(e) => set("description", e.target.value)} />
          </Field>
          <ErrorBanner error={error} />
          <SubmitRow busy={busy} label={editing === null ? "Tạo hồ sơ vai trò" : "Lưu hồ sơ chức danh"}  onCancel={editing === null ? undefined : () => { setEditing(null); reset(); }} />
        </form>
      ) : null}
    </section>
  );
}
