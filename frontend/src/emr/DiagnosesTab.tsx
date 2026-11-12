import { useCallback, useEffect, useState } from "react";
import type { FormEvent } from "react";
import { emrApi, errorMessage } from "./emrApi";
import type { Diagnosis, EncounterStatus } from "./types";

const CAN_ADD: EncounterStatus[] = ["IN_PROGRESS", "PENDING_LAB_RAD"];
const CAN_CONFIRM: EncounterStatus[] = [
  "IN_PROGRESS",
  "PENDING_LAB_RAD",
  "DIAGNOSED",
];

export function DiagnosesTab({
  encounterId,
  status,
}: {
  encounterId: string;
  status: EncounterStatus;
}) {
  const [items, setItems] = useState<Diagnosis[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [diagnosisType, setDiagnosisType] = useState<"PRIMARY" | "SECONDARY">(
    "PRIMARY",
  );
  const [icd10Code, setIcd10Code] = useState("");
  const [narrative, setNarrative] = useState("");
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setItems(await emrApi.listDiagnoses(encounterId));
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setLoading(false);
    }
  }, [encounterId]);

  useEffect(() => {
    void load();
  }, [load]);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setSaving(true);
    setFormError(null);
    try {
      await emrApi.createDiagnosis(encounterId, {
        diagnosisType,
        icd10Code: icd10Code.trim() || undefined,
        narrative: narrative.trim(),
      });
      setIcd10Code("");
      setNarrative("");
      await load();
    } catch (e) {
      setFormError(errorMessage(e));
    } finally {
      setSaving(false);
    }
  }

  async function confirm(id: string) {
    setBusyId(id);
    setFormError(null);
    try {
      await emrApi.confirmDiagnosis(encounterId, id);
      await load();
    } catch (e) {
      setFormError(errorMessage(e));
    } finally {
      setBusyId(null);
    }
  }

  const canAdd = CAN_ADD.includes(status);
  const canConfirm = CAN_CONFIRM.includes(status);

  return (
    <section>
      <h3>Chẩn đoán</h3>
      {canAdd ? (
        <form onSubmit={(event) => void submit(event)}>
          <label>
            Loại
            <select
              value={diagnosisType}
              onChange={(e) =>
                setDiagnosisType(e.target.value as "PRIMARY" | "SECONDARY")
              }
            >
              <option value="PRIMARY">Chính</option>
              <option value="SECONDARY">Phụ</option>
            </select>
          </label>
          <label>
            Mã ICD-10
            <input
              value={icd10Code}
              onChange={(e) => setIcd10Code(e.target.value)}
              maxLength={20}
            />
          </label>
          <label>
            Mô tả
            <input
              value={narrative}
              onChange={(e) => setNarrative(e.target.value)}
              maxLength={2000}
              required
            />
          </label>
          <button type="submit" disabled={saving || narrative.trim() === ""}>
            {saving ? "Đang lưu…" : "Thêm chẩn đoán"}
          </button>
        </form>
      ) : (
        <p>Không thêm chẩn đoán được khi lần khám ở trạng thái này.</p>
      )}
      {formError && (
        <p role="alert" className="error">
          {formError}
        </p>
      )}
      {error && (
        <p role="alert" className="error">
          {error}
        </p>
      )}
      {loading ? (
        <p role="status">Đang tải…</p>
      ) : items.length === 0 ? (
        <p>Chưa có chẩn đoán nào.</p>
      ) : (
        <table>
          <thead>
            <tr>
              <th>Loại</th>
              <th>ICD-10</th>
              <th>Mô tả</th>
              <th>Trạng thái</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {items.map((d) => (
              <tr key={d.diagnosisId}>
                <td>{d.diagnosisType === "PRIMARY" ? "Chính" : "Phụ"}</td>
                <td>{d.icd10Code ?? "—"}</td>
                <td>{d.narrative}</td>
                <td>
                  <span className="badge">
                    {d.diagnosisStatus === "CONFIRMED"
                      ? "Đã xác nhận"
                      : "Tạm thời"}
                  </span>
                </td>
                <td>
                  {d.diagnosisStatus === "PROVISIONAL" && canConfirm && (
                    <button
                      type="button"
                      disabled={busyId === d.diagnosisId}
                      onClick={() => void confirm(d.diagnosisId)}
                    >
                      Xác nhận
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}
