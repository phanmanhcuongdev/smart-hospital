import { useCallback, useEffect, useState } from "react";
import type { FormEvent } from "react";
import { emrApi, errorMessage } from "./emrApi";
import { PRESCRIPTION_STATUS_LABEL } from "./types";
import type {
  EncounterStatus,
  MedicationSummary,
  Prescription,
  PrescriptionLineInput,
  PrescriptionWarning,
} from "./types";

const CAN_PRESCRIBE: EncounterStatus[] = [
  "IN_PROGRESS",
  "PENDING_LAB_RAD",
  "DIAGNOSED",
];

const WARNING_TEXT: Record<string, string> = {
  ALLERGY_UNKNOWN: "Thông tin dị ứng chưa biết hoặc chưa xác minh.",
  ALLERGY_MATCH: "Thuốc có thể liên quan đến dị ứng đã ghi nhận.",
};
const POLICY_NOTE =
  "Chưa có quy tắc xử lý được duyệt (OPEN-08): hệ thống chỉ cảnh báo, không chặn.";

function Warnings({ warnings }: { warnings: PrescriptionWarning[] }) {
  return (
    <>
      {warnings.map((w, index) => (
        <p key={`${w.code}-${index}`} role="alert" className="warning">
          {WARNING_TEXT[w.code] ?? w.code}
          {w.code === "ALLERGY_MATCH" ? ` (${w.message})` : ""} {POLICY_NOTE}
        </p>
      ))}
    </>
  );
}

interface LineFormProps {
  medications: MedicationSummary[];
  submitLabel: string;
  busy: boolean;
  onSubmit: (line: PrescriptionLineInput) => Promise<boolean>;
}

function LineForm({ medications, submitLabel, busy, onSubmit }: LineFormProps) {
  const [medicationId, setMedicationId] = useState("");
  const [dose, setDose] = useState("");
  const [route, setRoute] = useState("");
  const [frequency, setFrequency] = useState("");
  const [daysCount, setDaysCount] = useState("1");
  const [quantity, setQuantity] = useState("");
  const [unitCode, setUnitCode] = useState("");
  const [error, setError] = useState<string | null>(null);

  const selected = medicationId || medications[0]?.medicationId || "";

  async function submit(event: FormEvent) {
    event.preventDefault();
    const missing: string[] = [];
    if (selected === "") missing.push("Thuốc");
    if (dose.trim() === "") missing.push("Liều");
    if (route.trim() === "") missing.push("Đường dùng");
    if (frequency.trim() === "") missing.push("Tần suất");
    if (quantity.trim() === "") missing.push("Số lượng");
    if (unitCode.trim() === "") missing.push("Đơn vị");
    if (missing.length > 0) {
      setError(`Thiếu thông tin: ${missing.join(", ")}.`);
      return;
    }
    const days = Number(daysCount);
    const qty = Number(quantity);
    if (!Number.isInteger(days) || days < 1) {
      setError("Số ngày phải là số nguyên từ 1 trở lên.");
      return;
    }
    if (Number.isNaN(qty) || qty <= 0) {
      setError("Số lượng phải lớn hơn 0.");
      return;
    }
    setError(null);
    const ok = await onSubmit({
      medicationId: selected,
      dose: dose.trim(),
      route: route.trim(),
      frequency: frequency.trim(),
      daysCount: days,
      quantity: qty,
      unitCode: unitCode.trim(),
    });
    if (ok) {
      setDose("");
      setRoute("");
      setFrequency("");
      setDaysCount("1");
      setQuantity("");
      setUnitCode("");
    }
  }

  return (
    <>
      <form onSubmit={(event) => void submit(event)}>
        <label>
          Thuốc
          <select
            value={selected}
            onChange={(e) => setMedicationId(e.target.value)}
          >
            {medications.map((m) => (
              <option key={m.medicationId} value={m.medicationId}>
                {m.displayName}
              </option>
            ))}
          </select>
        </label>
        <label>
          Liều
          <input
            value={dose}
            onChange={(e) => setDose(e.target.value)}
            maxLength={100}
          />
        </label>
        <label>
          Đường dùng
          <input
            value={route}
            onChange={(e) => setRoute(e.target.value)}
            maxLength={80}
          />
        </label>
        <label>
          Tần suất
          <input
            value={frequency}
            onChange={(e) => setFrequency(e.target.value)}
            maxLength={120}
          />
        </label>
        <label>
          Số ngày
          <input
            value={daysCount}
            onChange={(e) => setDaysCount(e.target.value)}
            inputMode="numeric"
          />
        </label>
        <label>
          Số lượng
          <input
            value={quantity}
            onChange={(e) => setQuantity(e.target.value)}
            inputMode="decimal"
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
        <button type="submit" disabled={busy}>
          {submitLabel}
        </button>
      </form>
      {error && (
        <p role="alert" className="error">
          {error}
        </p>
      )}
    </>
  );
}

interface CardProps {
  encounterId: string;
  prescription: Prescription;
  medications: MedicationSummary[];
  onChanged: () => Promise<void>;
}

function PrescriptionCard({
  encounterId,
  prescription,
  medications,
  onChanged,
}: CardProps) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const isDraft = prescription.status === "DRAFT";
  const canCancel =
    prescription.status === "DRAFT" || prescription.status === "ORDERED";

  async function run(action: () => Promise<unknown>): Promise<boolean> {
    setBusy(true);
    setError(null);
    try {
      await action();
      await onChanged();
      return true;
    } catch (e) {
      setError(errorMessage(e));
      return false;
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="card">
      <p>
        Đơn {prescription.prescriptionId.slice(0, 8)}{" "}
        <span className="badge">
          {PRESCRIPTION_STATUS_LABEL[prescription.status]}
        </span>
        {prescription.issuedAt && (
          <>
            {" "}
            — phát hành{" "}
            {new Date(prescription.issuedAt).toLocaleString("vi-VN")}
          </>
        )}
      </p>
      <Warnings warnings={prescription.warnings} />
      <table>
        <thead>
          <tr>
            <th>#</th>
            <th>Thuốc</th>
            <th>Liều</th>
            <th>Đường dùng</th>
            <th>Tần suất</th>
            <th>Số ngày</th>
            <th>Số lượng</th>
          </tr>
        </thead>
        <tbody>
          {prescription.items.map((i) => (
            <tr key={i.lineNo}>
              <td>{i.lineNo}</td>
              <td>{i.medicationLabel}</td>
              <td>{i.dose}</td>
              <td>{i.route}</td>
              <td>{i.frequency}</td>
              <td>{i.daysCount}</td>
              <td>
                {i.quantity} {i.unitCode}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      {isDraft && (
        <LineForm
          medications={medications}
          submitLabel="Thêm thuốc"
          busy={busy}
          onSubmit={(line) =>
            run(() =>
              emrApi.addPrescriptionItem(
                encounterId,
                prescription.prescriptionId,
                line,
              ),
            )
          }
        />
      )}
      <div className="toolbar">
        {isDraft && (
          <button
            type="button"
            disabled={busy || prescription.items.length === 0}
            onClick={() =>
              void run(() =>
                emrApi.issuePrescription(
                  encounterId,
                  prescription.prescriptionId,
                ),
              )
            }
          >
            Phát hành đơn
          </button>
        )}
        {canCancel && (
          <button
            type="button"
            disabled={busy}
            onClick={() =>
              void run(() =>
                emrApi.cancelPrescription(
                  encounterId,
                  prescription.prescriptionId,
                ),
              )
            }
          >
            Hủy đơn
          </button>
        )}
      </div>
      {error && (
        <p role="alert" className="error">
          {error}
        </p>
      )}
    </div>
  );
}

export function PrescriptionsTab({
  encounterId,
  status,
}: {
  encounterId: string;
  status: EncounterStatus;
}) {
  const [items, setItems] = useState<Prescription[]>([]);
  const [medications, setMedications] = useState<MedicationSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [creating, setCreating] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setError(null);
    try {
      const [rx, meds] = await Promise.all([
        emrApi.listPrescriptions(encounterId),
        emrApi.listMedications(),
      ]);
      setItems(rx);
      setMedications(meds);
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setLoading(false);
    }
  }, [encounterId]);

  useEffect(() => {
    void load();
  }, [load]);

  async function create(line: PrescriptionLineInput): Promise<boolean> {
    setCreating(true);
    setActionError(null);
    try {
      await emrApi.createPrescription(encounterId, [line]);
      await load();
      return true;
    } catch (e) {
      setActionError(errorMessage(e));
      return false;
    } finally {
      setCreating(false);
    }
  }

  return (
    <section>
      <h3>Đơn thuốc</h3>
      <p>Đơn thuốc chỉ ghi nhận kê đơn, chưa cấp phát.</p>
      {CAN_PRESCRIBE.includes(status) ? (
        <>
          <h4>Tạo đơn thuốc (nhập dòng thuốc đầu tiên)</h4>
          <LineForm
            medications={medications}
            submitLabel="Tạo đơn"
            busy={creating}
            onSubmit={create}
          />
        </>
      ) : (
        <p>Không tạo đơn thuốc mới được khi lần khám ở trạng thái này.</p>
      )}
      {actionError && (
        <p role="alert" className="error">
          {actionError}
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
        <p>Chưa có đơn thuốc nào.</p>
      ) : (
        items.map((p) => (
          <PrescriptionCard
            key={p.prescriptionId}
            encounterId={encounterId}
            prescription={p}
            medications={medications}
            onChanged={load}
          />
        ))
      )}
    </section>
  );
}
