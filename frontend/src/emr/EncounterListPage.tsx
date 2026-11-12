import { useCallback, useEffect, useState } from "react";
import type { FormEvent } from "react";
import { emrApi, errorMessage } from "./emrApi";
import { ENCOUNTER_STATUSES, STATUS_LABEL } from "./types";
import type {
  DepartmentSummary,
  Encounter,
  EncounterStatus,
  PatientSummary,
  PractitionerSummary,
} from "./types";

const PAGE_SIZE = 10;

function formatTime(iso: string | null): string {
  return iso ? new Date(iso).toLocaleString("vi-VN") : "—";
}

export function EncounterListPage() {
  const [status, setStatus] = useState<EncounterStatus | "">("");
  const [filterDepartmentId, setFilterDepartmentId] = useState("");
  const [page, setPage] = useState(0);
  const [items, setItems] = useState<Encounter[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [patients, setPatients] = useState<PatientSummary[]>([]);
  const [practitioners, setPractitioners] = useState<PractitionerSummary[]>([]);
  const [departments, setDepartments] = useState<DepartmentSummary[]>([]);
  const [patientId, setPatientId] = useState("");
  const [practitionerId, setPractitionerId] = useState("");
  const [departmentId, setDepartmentId] = useState("");
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await emrApi.listEncounters({
        status,
        departmentId: filterDepartmentId || undefined,
        page,
        size: PAGE_SIZE,
      });
      setItems(result.items);
      setTotal(result.total);
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setLoading(false);
    }
  }, [status, filterDepartmentId, page]);

  useEffect(() => {
    void load();
  }, [load]);

  useEffect(() => {
    Promise.all([
      emrApi.searchPatients(""),
      emrApi.listPractitioners(),
      emrApi.listDepartments(),
    ])
      .then(([p, docs, depts]) => {
        const pList = Array.isArray(p) ? p : [];
        const dList = Array.isArray(docs) ? docs : [];
        const deptList = Array.isArray(depts) ? depts : [];
        setPatients(pList);
        setPractitioners(dList);
        setDepartments(deptList);
        if (pList[0]) setPatientId(pList[0].patientId);
        if (dList[0]) {
          setPractitionerId(dList[0].employeeId);
          if (dList[0].departmentName) {
            const matchedDept = deptList.find(
              (d) => d.displayName === dList[0].departmentName,
            );
            if (matchedDept) setDepartmentId(matchedDept.departmentId);
          }
        }
      })
      .catch((e) => setFormError(errorMessage(e)));
  }, []);

  async function create(event: FormEvent) {
    event.preventDefault();
    setSaving(true);
    setFormError(null);
    setNotice(null);
    try {
      const created = await emrApi.createEncounter({
        patientId,
        attendingEmployeeId: practitionerId,
        departmentId: departmentId || undefined,
      });
      setNotice(
        `Đã tạo lần khám ${created.encounterId.slice(0, 8)} (${STATUS_LABEL[created.status]}).`,
      );
      if (page === 0) await load();
      else setPage(0);
    } catch (e) {
      setFormError(errorMessage(e));
    } finally {
      setSaving(false);
    }
  }

  const patientName = new Map(
    (patients || []).map((p) => [p.patientId, p.displayName]),
  );
  const doctorName = new Map(
    (practitioners || []).map((d) => [d.employeeId, d.fullName]),
  );
  const deptName = new Map(
    (departments || []).map((d) => [d.departmentId, d.displayName]),
  );
  const pages = Math.max(1, Math.ceil(total / PAGE_SIZE));

  return (
    <>
      <section>
        <h2>Tạo lần khám</h2>
        <form onSubmit={(event) => void create(event)}>
          <label>
            Bệnh nhân
            <select
              value={patientId}
              onChange={(e) => setPatientId(e.target.value)}
            >
              {patients.map((p) => (
                <option key={p.patientId} value={p.patientId}>
                  {p.displayName}
                </option>
              ))}
            </select>
          </label>
          <label>
            Bác sĩ phụ trách
            <select
              value={practitionerId}
              onChange={(e) => {
                const docId = e.target.value;
                setPractitionerId(docId);
                const selectedDoc = practitioners.find(
                  (d) => d.employeeId === docId,
                );
                if (selectedDoc?.departmentName) {
                  const matchedDept = departments.find(
                    (dept) => dept.displayName === selectedDoc.departmentName,
                  );
                  if (matchedDept) setDepartmentId(matchedDept.departmentId);
                }
              }}
            >
              {practitioners.map((d) => (
                <option key={d.employeeId} value={d.employeeId}>
                  {d.fullName} ({d.employeeCode})
                  {d.departmentName ? ` — ${d.departmentName}` : ""}
                  {d.hasVerifiedLicense === false ? " [⚠️ Chưa có CCHN]" : ""}
                </option>
              ))}
            </select>
          </label>
          <label>
            Khoa / Phòng khám
            <select
              value={departmentId}
              onChange={(e) => setDepartmentId(e.target.value)}
            >
              <option value="">-- Mặc định theo bác sĩ phụ trách --</option>
              {departments.map((dept) => (
                <option key={dept.departmentId} value={dept.departmentId}>
                  {dept.displayName} ({dept.departmentCode})
                </option>
              ))}
            </select>
          </label>
          <button
            type="submit"
            disabled={saving || !patientId || !practitionerId}
          >
            {saving ? "Đang tạo…" : "Tạo lần khám"}
          </button>
        </form>
        {formError && (
          <p role="alert" className="error">
            {formError}
          </p>
        )}
        {notice && (
          <p role="status" className="notice">
            {notice}
          </p>
        )}
      </section>

      <section>
        <h2>Danh sách lần khám</h2>
        <div className="toolbar">
          <label>
            Khoa / Phòng
            <select
              value={filterDepartmentId}
              onChange={(e) => {
                setFilterDepartmentId(e.target.value);
                setPage(0);
              }}
            >
              <option value="">Tất cả</option>
              {departments.map((d) => (
                <option key={d.departmentId} value={d.departmentId}>
                  {d.displayName}
                </option>
              ))}
            </select>
          </label>
          <label>
            Trạng thái
            <select
              value={status}
              onChange={(e) => {
                setStatus(e.target.value as EncounterStatus | "");
                setPage(0);
              }}
            >
              <option value="">Tất cả</option>
              {ENCOUNTER_STATUSES.map((s) => (
                <option key={s} value={s}>
                  {STATUS_LABEL[s]}
                </option>
              ))}
            </select>
          </label>
          <button type="button" onClick={() => void load()} disabled={loading}>
            Tải lại
          </button>
        </div>
        {error && (
          <p role="alert" className="error">
            {error}
          </p>
        )}
        {loading ? (
          <p role="status">Đang tải…</p>
        ) : items.length === 0 ? (
          <p>Chưa có lần khám nào.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Mã</th>
                <th>Bệnh nhân</th>
                <th>Bác sĩ</th>
                <th>Khoa / Phòng</th>
                <th>Trạng thái</th>
                <th>Bắt đầu</th>
                <th>Tạo lúc</th>
              </tr>
            </thead>
            <tbody>
              {items.map((e) => (
                <tr key={e.encounterId}>
                  <td>
                    <a href={`#/encounters/${e.encounterId}`}>
                      {e.encounterId.slice(0, 8)}
                    </a>
                  </td>
                  <td>
                    {patientName.get(e.patientId) ?? e.patientId.slice(0, 8)}
                  </td>
                  <td>
                    {doctorName.get(e.attendingEmployeeId) ??
                      e.attendingEmployeeId.slice(0, 8)}
                  </td>
                  <td>
                    {e.departmentId
                      ? deptName.get(e.departmentId) ?? e.departmentId.slice(0, 8)
                      : "—"}
                  </td>
                  <td>
                    <span className="badge">{STATUS_LABEL[e.status]}</span>
                  </td>
                  <td>{formatTime(e.startedAt)}</td>
                  <td>{formatTime(e.createdAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        <div className="toolbar">
          <button
            type="button"
            onClick={() => setPage((p) => p - 1)}
            disabled={loading || page === 0}
          >
            Trước
          </button>
          <span>
            Trang {page + 1} / {pages} ({total} lần khám)
          </span>
          <button
            type="button"
            onClick={() => setPage((p) => p + 1)}
            disabled={loading || page + 1 >= pages}
          >
            Sau
          </button>
        </div>
      </section>
    </>
  );
}
