import { useCallback, useEffect, useState } from "react";
import type { FormEvent } from "react";
import { emrApi, errorMessage } from "./emrApi";
import type { ClinicalResult, OrderStatus, ResultDisposition } from "./types";

const ACCEPTING: OrderStatus[] = ["ACKNOWLEDGED", "IN_PROGRESS"];

const DISPOSITION_LABEL: Record<ResultDisposition, string> = {
  ACCEPTED: "Đã nhận kết quả hợp lệ.",
  QUARANTINED:
    "Kết quả bị cách ly (sai bệnh nhân), không dùng như kết quả bình thường.",
  DUPLICATE:
    "Kết quả trùng (cùng nguồn, mã, phiên bản): giữ bản cũ, không ghi đè.",
};

interface Props {
  encounterId: string;
  orderId: string;
  orderStatus: OrderStatus;
}

export function ResultsPanel({ encounterId, orderId, orderStatus }: Props) {
  const [open, setOpen] = useState(false);
  const [items, setItems] = useState<ClinicalResult[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [resultId, setResultId] = useState("");
  const [version, setVersion] = useState("1");
  const [payload, setPayload] = useState('{"test":"CBC","value":"normal"}');
  const [abnormal, setAbnormal] = useState(false);
  const [wrongPatient, setWrongPatient] = useState(false);
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setItems(await emrApi.listResults(orderId));
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setLoading(false);
    }
  }, [orderId]);

  useEffect(() => {
    if (open) void load();
  }, [open, load]);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      const encounter = await emrApi.getEncounter(encounterId);
      const outcome = await emrApi.submitMockResult(orderId, {
        sourceSystem: "CLS_MOCK",
        sourceResultId: resultId.trim() || `R-${Date.now()}`,
        sourceVersion: version.trim(),
        patientId: wrongPatient ? crypto.randomUUID() : encounter.patientId,
        payload,
        abnormalFlag: abnormal,
      });
      setNotice(DISPOSITION_LABEL[outcome.disposition]);
      await load();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div>
      <button type="button" onClick={() => setOpen((o) => !o)}>
        {open ? "Ẩn kết quả" : "Xem kết quả"}
      </button>
      {open && (
        <div className="card">
          {loading ? (
            <p role="status">Đang tải…</p>
          ) : items.length === 0 ? (
            <p>Chưa có kết quả.</p>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Nguồn</th>
                  <th>Mã</th>
                  <th>Phiên bản</th>
                  <th>Bất thường</th>
                  <th>Trạng thái</th>
                  <th>Nội dung</th>
                </tr>
              </thead>
              <tbody>
                {items.map((r) => (
                  <tr
                    key={r.resultId}
                    className={
                      r.quality === "QUARANTINED" ? "quarantined" : undefined
                    }
                  >
                    <td>{r.sourceSystem}</td>
                    <td>{r.sourceResultId}</td>
                    <td>{r.sourceVersion}</td>
                    <td>
                      {r.abnormalFlag === null
                        ? "—"
                        : r.abnormalFlag
                          ? "Có"
                          : "Không"}
                    </td>
                    <td>
                      <span className="badge">
                        {r.quality === "VALID" ? "Hợp lệ" : "Cách ly"}
                      </span>
                    </td>
                    <td>
                      <code>{r.payload}</code>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
          {ACCEPTING.includes(orderStatus) ? (
            <form onSubmit={(event) => void submit(event)}>
              <label>
                Mã kết quả (để trống = tự sinh)
                <input
                  value={resultId}
                  onChange={(e) => setResultId(e.target.value)}
                  maxLength={160}
                />
              </label>
              <label>
                Phiên bản
                <input
                  value={version}
                  onChange={(e) => setVersion(e.target.value)}
                  maxLength={60}
                />
              </label>
              <label>
                Nội dung (JSON)
                <input
                  value={payload}
                  onChange={(e) => setPayload(e.target.value)}
                />
              </label>
              <label>
                <span>
                  <input
                    type="checkbox"
                    checked={abnormal}
                    onChange={(e) => setAbnormal(e.target.checked)}
                  />{" "}
                  Bất thường
                </span>
              </label>
              <label>
                <span>
                  <input
                    type="checkbox"
                    checked={wrongPatient}
                    onChange={(e) => setWrongPatient(e.target.checked)}
                  />{" "}
                  Giả lập sai bệnh nhân
                </span>
              </label>
              <button
                type="submit"
                disabled={
                  busy || version.trim() === "" || payload.trim() === ""
                }
              >
                Giả lập nhận kết quả CLS
              </button>
            </form>
          ) : (
            <p>
              Chỉ nhận kết quả khi chỉ định ở trạng thái "Đã tiếp nhận" hoặc
              "Đang thực hiện".
            </p>
          )}
          {notice && (
            <p role="status" className="notice">
              {notice}
            </p>
          )}
          {error && (
            <p role="alert" className="error">
              {error}
            </p>
          )}
        </div>
      )}
    </div>
  );
}
