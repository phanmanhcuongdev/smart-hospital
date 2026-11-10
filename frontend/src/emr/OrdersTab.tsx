import { useCallback, useEffect, useState } from "react";
import type { FormEvent } from "react";
import { emrApi, errorMessage } from "./emrApi";
import {
  NEXT_ORDER_STATUS,
  ORDER_STATUS_LABEL,
  ORDER_TYPE_LABEL,
} from "./types";
import type {
  EncounterStatus,
  MedicalOrder,
  OrderStatus,
  OrderType,
} from "./types";
import { ResultsPanel } from "./ResultsPanel";

const CAN_ORDER: EncounterStatus[] = ["IN_PROGRESS", "PENDING_LAB_RAD"];
const ORDER_TYPES = Object.keys(ORDER_TYPE_LABEL) as OrderType[];

export function OrdersTab({
  encounterId,
  status,
}: {
  encounterId: string;
  status: EncounterStatus;
}) {
  const [items, setItems] = useState<MedicalOrder[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [orderType, setOrderType] = useState<OrderType>("LAB");
  const [externalOrderRef, setExternalOrderRef] = useState("");
  const [saving, setSaving] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setItems(await emrApi.listOrders(encounterId));
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setLoading(false);
    }
  }, [encounterId]);

  useEffect(() => {
    void load();
  }, [load]);

  async function create(event: FormEvent) {
    event.preventDefault();
    setSaving(true);
    setActionError(null);
    try {
      await emrApi.createOrder(encounterId, {
        orderType,
        externalOrderRef: externalOrderRef.trim() || undefined,
      });
      setExternalOrderRef("");
      await load();
    } catch (e) {
      setActionError(errorMessage(e));
    } finally {
      setSaving(false);
    }
  }

  async function change(orderId: string, next: OrderStatus) {
    setBusyId(orderId);
    setActionError(null);
    try {
      await emrApi.changeOrderStatus(encounterId, orderId, next);
      await load();
    } catch (e) {
      setActionError(errorMessage(e));
    } finally {
      setBusyId(null);
    }
  }

  return (
    <section>
      <h3>Chỉ định cận lâm sàng</h3>
      {CAN_ORDER.includes(status) ? (
        <form onSubmit={(event) => void create(event)}>
          <label>
            Loại chỉ định
            <select
              value={orderType}
              onChange={(e) => setOrderType(e.target.value as OrderType)}
            >
              {ORDER_TYPES.map((t) => (
                <option key={t} value={t}>
                  {ORDER_TYPE_LABEL[t]}
                </option>
              ))}
            </select>
          </label>
          <label>
            Mã tham chiếu ngoài (tùy chọn)
            <input
              value={externalOrderRef}
              onChange={(e) => setExternalOrderRef(e.target.value)}
              maxLength={160}
            />
          </label>
          <button type="submit" disabled={saving}>
            {saving ? "Đang tạo…" : "Tạo chỉ định"}
          </button>
        </form>
      ) : (
        <p>Không tạo chỉ định mới được khi lần khám ở trạng thái này.</p>
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
        <p>Chưa có chỉ định nào.</p>
      ) : (
        <table>
          <thead>
            <tr>
              <th>Loại</th>
              <th>Trạng thái</th>
              <th>Mã ngoài</th>
              <th>Chỉ định lúc</th>
              <th>Chuyển trạng thái</th>
              <th>Kết quả</th>
            </tr>
          </thead>
          <tbody>
            {items.map((o) => (
              <tr key={o.orderId}>
                <td>
                  {ORDER_TYPE_LABEL[o.orderType]}{" "}
                  <td>
                    <ResultsPanel
                      encounterId={encounterId}
                      orderId={o.orderId}
                      orderStatus={o.orderStatus}
                    />
                  </td>
                </td>
                <td>
                  <span className="badge">
                    {ORDER_STATUS_LABEL[o.orderStatus]}
                  </span>
                </td>
                <td>{o.externalOrderRef ?? "—"}</td>
                <td>
                  {o.orderedAt
                    ? new Date(o.orderedAt).toLocaleString("vi-VN")
                    : "—"}
                </td>
                <td>
                  {NEXT_ORDER_STATUS[o.orderStatus].map((next) => (
                    <button
                      key={next}
                      type="button"
                      disabled={busyId === o.orderId}
                      onClick={() => void change(o.orderId, next)}
                    >
                      {ORDER_STATUS_LABEL[next]}
                    </button>
                  ))}
                  {NEXT_ORDER_STATUS[o.orderStatus].length === 0 && "—"}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}
