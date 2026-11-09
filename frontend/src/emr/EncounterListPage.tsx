import { useCallback, useEffect, useState } from "react";
import type { FormEvent } from "react";
import { emrApi, errorMessage } from "./emrApi";
import { ENCOUNTER_STATUSES, STATUS_LABEL } from "./types";
import type {
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
  const [page, setPage] = useState(0);
  const [items, setItems] = useState<Encounter[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [patients, setPatients] = useState<PatientSummary[]>([]);
  const [practitioners, setPractitioners] = useState<PractitionerSummary[]>([]);
  const [patientId, setPatientId] = useState("");
  const [practitionerId, setPractitionerId] = useState("");
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await emrApi.listEncounters({
        status,
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
  }, [status, page]);

  useEffect(() => {
    void load();
  }, [load]);

  useEffect(() => {
    Promise.all([emrApi.searchPatients(""), emrApi.listPractitioners()])
      .then(([p, d]) => {
        setPatients(p);
        setPractitioners(d);
        if (p[0]) setPatientId(p[0].patientId);
        if (d[0]) setPractitionerId(d[0].employeeId);
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
    patients.map((p) => [p.patientId, p.displayName]),
  );
  const doctorName = new Map(
    practitioners.map((d) => [d.employeeId, d.fullName]),
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
              onChange={(e) => setPractitionerId(e.target.value)}
            >
              {practitioners.map((d) => (
                <option key={d.employeeId} value={d.employeeId}>
                  {d.fullName}
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
