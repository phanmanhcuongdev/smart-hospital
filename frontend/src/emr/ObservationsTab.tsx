import { useCallback, useEffect, useState } from "react";
import type { FormEvent } from "react";
import { emrApi, errorMessage } from "./emrApi";
import type { EncounterStatus, Observation } from "./types";

const CAN_RECORD: EncounterStatus[] = [
  "WAITING",
  "IN_PROGRESS",
  "PENDING_LAB_RAD",
];

export function ObservationsTab({
  encounterId,
  status,
}: {
  encounterId: string;
  status: EncounterStatus;
}) {
  const [items, setItems] = useState<Observation[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [metricCode, setMetricCode] = useState("");
  const [numericValue, setNumericValue] = useState("");
  const [textValue, setTextValue] = useState("");
  const [unitCode, setUnitCode] = useState("");
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setItems(await emrApi.listObservations(encounterId));
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
    setFormError(null);
    const hasNumber = numericValue.trim() !== "";
    const hasText = textValue.trim() !== "";
    if (hasNumber === hasText) {
      setFormError("Nhập đúng một trong hai: giá trị số hoặc giá trị chữ.");
      return;
    }
    const number = Number(numericValue);
    if (hasNumber && Number.isNaN(number)) {
      setFormError("Giá trị số không hợp lệ.");
      return;
    }
    setSaving(true);
    try {
      await emrApi.createObservation(encounterId, {
        metricCode: metricCode.trim(),
        numericValue: hasNumber ? number : undefined,
        textValue: hasText ? textValue.trim() : undefined,
        unitCode: unitCode.trim() || undefined,
      });
      setMetricCode("");
      setNumericValue("");
      setTextValue("");
      setUnitCode("");
      await load();
    } catch (e) {
      setFormError(errorMessage(e));
    } finally {
      setSaving(false);
    }
  }

  const canRecord = CAN_RECORD.includes(status);

  return (
    <section>
      <h3>Sinh hiệu và quan sát</h3>
      {canRecord ? (
        <form onSubmit={(event) => void submit(event)}>
          <label>
            Chỉ số (ví dụ HEART_RATE)
            <input
              value={metricCode}
              onChange={(e) => setMetricCode(e.target.value)}
              maxLength={60}
              required
            />
          </label>
          <label>
            Giá trị số
            <input
              value={numericValue}
              onChange={(e) => setNumericValue(e.target.value)}
              inputMode="decimal"
            />
          </label>
          <label>
            hoặc giá trị chữ
            <input
              value={textValue}
              onChange={(e) => setTextValue(e.target.value)}
            />
          </label>
          <label>
            Đơn vị
            <input
              value={unitCode}
              onChange={(e) => setUnitCode(e.target.value)}
              maxLength={40}
            />
          </label>
          <button type="submit" disabled={saving || metricCode.trim() === ""}>
            {saving ? "Đang lưu…" : "Ghi nhận"}
          </button>
        </form>
      ) : (
        <p>Không ghi thêm sinh hiệu được khi lần khám ở trạng thái này.</p>
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
        <p>Chưa có quan sát nào.</p>
      ) : (
        <table>
          <thead>
            <tr>
              <th>Chỉ số</th>
              <th>Giá trị</th>
              <th>Đơn vị</th>
              <th>Thời điểm</th>
              <th>Nguồn</th>
            </tr>
          </thead>
          <tbody>
            {items.map((o) => (
              <tr key={o.observationId}>
                <td>{o.metricCode}</td>
                <td>{o.numericValue ?? o.textValue}</td>
                <td>{o.unitCode ?? "—"}</td>
                <td>{new Date(o.observedAt).toLocaleString("vi-VN")}</td>
                <td>{o.sourceType}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}
