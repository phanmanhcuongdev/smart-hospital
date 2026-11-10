import { useCallback, useEffect, useState } from "react";
import type { FormEvent } from "react";
import { emrApi, errorMessage } from "./emrApi";
import { PRESCRIPTION_STATUS_LABEL } from "./types";
import type { EncounterStatus, MedicationSummary, Prescription } from "./types";

const CAN_PRESCRIBE: EncounterStatus[] = [
  "IN_PROGRESS",
  "PENDING_LAB_RAD",
  "DIAGNOSED",
];

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
  const [medicationId, setMedicationId] = useState(
    medications[0]?.medicationId ?? "",
  );
  const [dose, setDose] = useState("");
  const [route, setRoute] = useState("");
  const [frequency, setFrequency] = useState("");
  const [daysCount, setDaysCount] = useState("1");
  const [quantity, setQuantity] = useState("");
  const [unitCode, setUnitCode] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const isDraft = prescription.status === "DRAFT";
  const canCancel =
    prescription.status === "DRAFT" || prescription.status === "ORDERED";

  async function run(action: () => Promise<unknown>) {
    setBusy(true);
    setError(null);
    try {
      await action();
      await onChanged();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  async function addItem(event: FormEvent) {
    event.preventDefault();
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
    await run(async () => {
      await emrApi.addPrescriptionItem(
        encounterId,
        prescription.prescriptionId,
        {
          medicationId,
          dose: dose.trim(),
          route: route.trim(),
          frequency: frequency.trim(),
          daysCount: days,
          quantity: qty,
          unitCode: unitCode.trim(),
        },
      );
      setDose("");
      setRoute("");
      setFrequency("");
      setDaysCount("1");
      setQuantity("");
      setUnitCode("");
    });
  }

  const formComplete =
    [medicationId, dose, route, frequency, unitCode].every(
      (v) => v.trim() !== "",
    ) && quantity.trim() !== "";

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
      {prescription.items.length === 0 ? (
        <p>Đơn chưa có thuốc.</p>
      ) : (
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
      )}
      {isDraft && (
        <form onSubmit={(event) => void addItem(event)}>
          <label>
            Thuốc
            <select
              value={medicationId}
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
          <button type="submit" disabled={busy || !formComplete}>
            Thêm thuốc
          </button>
        </form>
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

  async function create() {
    setCreating(true);
    setActionError(null);
    try {
      await emrApi.createPrescription(encounterId);
      await load();
    } catch (e) {
      setActionError(errorMessage(e));
    } finally {
      setCreating(false);
    }
  }

  return (
    <section>
      <h3>Đơn thuốc</h3>
      {CAN_PRESCRIBE.includes(status) ? (
        <div className="toolbar">
          <button
            type="button"
            disabled={creating}
            onClick={() => void create()}
          >
            {creating ? "Đang tạo…" : "Tạo đơn thuốc nháp"}
          </button>
        </div>
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
