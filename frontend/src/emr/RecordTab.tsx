import { useCallback, useEffect, useState } from "react";
import type { FormEvent } from "react";
import { ApiRequestError, emrApi, errorMessage } from "./emrApi";
import { RECORD_STATUS_LABEL } from "./types";
import type {
  Addendum,
  EncounterStatus,
  MedicalRecord,
  RecordVersion,
  UserSummary,
} from "./types";

function formatTime(iso: string | null): string {
  return iso ? new Date(iso).toLocaleString("vi-VN") : "—";
}

function prettyPayload(payload: string | null): string {
  if (!payload) return "";
  try {
    return JSON.stringify(JSON.parse(payload), null, 2);
  } catch {
    return payload;
  }
}

export function RecordTab({
  encounterId,
  status,
}: {
  encounterId: string;
  status: EncounterStatus;
}) {
  const [record, setRecord] = useState<MedicalRecord | null>(null);
  const [versions, setVersions] = useState<RecordVersion[]>([]);
  const [addenda, setAddenda] = useState<Addendum[]>([]);
  const [users, setUsers] = useState<UserSummary[]>([]);
  const [userId, setUserId] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [viewing, setViewing] = useState<RecordVersion | null>(null);
  const [reason, setReason] = useState("");
  const [content, setContent] = useState("");

  const load = useCallback(async () => {
    setError(null);
    try {
      const current = await emrApi.getRecord(encounterId);
      const [v, a] = await Promise.all([
        emrApi.listVersions(encounterId),
        emrApi.listAddenda(encounterId),
      ]);
      setRecord(current);
      setVersions(v);
      setAddenda(a);
    } catch (e) {
      if (e instanceof ApiRequestError && e.status === 404) {
        setRecord(null);
        setVersions([]);
        setAddenda([]);
      } else {
        setError(errorMessage(e));
      }
    } finally {
      setLoading(false);
    }
  }, [encounterId]);

  // Tải lại khi trạng thái lần khám đổi (hồ sơ nháp được tạo lúc bắt đầu khám).
  useEffect(() => {
    void load();
  }, [load, status]);

  useEffect(() => {
    emrApi
      .listUsers()
      .then((list) => {
        setUsers(list);
        if (list[0]) setUserId(list[0].userId);
      })
      .catch((e) => setError(errorMessage(e)));
  }, []);

  // Sau mỗi thao tác (kể cả lỗi 409 do phiên bản cũ) tải lại để lấy phiên bản mới nhất.
  async function run(action: () => Promise<unknown>) {
    setBusy(true);
    setActionError(null);
    try {
      await action();
    } catch (e) {
      setActionError(errorMessage(e));
    } finally {
      await load();
      setBusy(false);
    }
  }

  async function view(versionNo: number) {
    setActionError(null);
    try {
      setViewing(await emrApi.getVersion(encounterId, versionNo));
    } catch (e) {
      setActionError(errorMessage(e));
    }
  }

  async function addAddendum(event: FormEvent) {
    event.preventDefault();
    if (!record) return;
    await run(async () => {
      await emrApi.createAddendum(encounterId, {
        reason: reason.trim(),
        content: content.trim(),
        expectedVersion: record.version,
      });
      setReason("");
      setContent("");
    });
  }

  if (loading) return <p role="status">Đang tải…</p>;

  const recordStatus = record?.recordStatus;
  const needsUser =
    recordStatus === "PENDING_SIGNATURE" || recordStatus === "LOCKED";

  return (
    <section>
      <h3>Hồ sơ bệnh án</h3>
      {error && (
        <p role="alert" className="error">
          {error}
        </p>
      )}
      {!record ? (
        <p>
          Chưa có hồ sơ. Hồ sơ nháp được tạo khi lần khám chuyển sang "Đang
          khám".
        </p>
      ) : (
        <>
          <dl className="info">
            <dt>Trạng thái</dt>
            <dd>
              <span className="badge">
                {RECORD_STATUS_LABEL[record.recordStatus]}
              </span>
            </dd>
            <dt>Phiên bản hiện tại</dt>
            <dd>{record.currentVersion}</dd>
            <dt>Mã kiểm soát thay đổi</dt>
            <dd>{record.version}</dd>
            <dt>Khóa lúc</dt>
            <dd>{formatTime(record.lockedAt)}</dd>
          </dl>

          {needsUser && (
            <div className="toolbar">
              {users.length === 0 ? (
                <span className="error">
                  Chưa có tài khoản người dùng nào để thực hiện thao tác
                  khóa/xác nhận.
                </span>
              ) : (
                <label>
                  Người thực hiện (tạm, chờ V.2)
                  <select
                    value={userId}
                    onChange={(e) => setUserId(e.target.value)}
                  >
                    {users.map((u) => (
                      <option key={u.userId} value={u.userId}>
                        {u.username}
                      </option>
                    ))}
                  </select>
                </label>
              )}
            </div>
          )}

          <div className="toolbar">
            {record.recordStatus === "DRAFT" && (
              <>
                <button
                  type="button"
                  disabled={busy}
                  onClick={() =>
                    void run(() =>
                      emrApi.createVersion(encounterId, record.version),
                    )
                  }
                >
                  Chốt phiên bản {record.currentVersion}
                </button>
                <button
                  type="button"
                  disabled={busy || status !== "COMPLETED"}
                  title={
                    status !== "COMPLETED"
                      ? "Cần hoàn tất lần khám trước"
                      : undefined
                  }
                  onClick={() =>
                    void run(() =>
                      emrApi.submitRecord(encounterId, record.version),
                    )
                  }
                >
                  Nộp hồ sơ để ký
                </button>
              </>
            )}
            {record.recordStatus === "PENDING_SIGNATURE" && (
              <>
                <button
                  type="button"
                  disabled={busy || !userId}
                  onClick={() =>
                    void run(() =>
                      emrApi.lockRecord(encounterId, userId, record.version),
                    )
                  }
                >
                  Khóa demo (không phải chữ ký số)
                </button>
                <button
                  type="button"
                  disabled={busy}
                  onClick={() =>
                    void run(() =>
                      emrApi.returnRecord(encounterId, record.version),
                    )
                  }
                >
                  Trả về sửa
                </button>
              </>
            )}
            {record.recordStatus === "DRAFT" && status !== "COMPLETED" && (
              <span>
                Cần hoàn tất lần khám và có chẩn đoán chính đã xác nhận trước
                khi nộp.
              </span>
            )}
          </div>
          {actionError && (
            <p role="alert" className="error">
              {actionError}
            </p>
          )}

          <h4>Các phiên bản đã chốt</h4>
          {versions.length === 0 ? (
            <p>Chưa có phiên bản nào.</p>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Phiên bản</th>
                  <th>Chốt lúc</th>
                  <th>Mã băm SHA-256</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {versions.map((v) => (
                  <tr key={v.versionNo}>
                    <td>{v.versionNo}</td>
                    <td>{formatTime(v.versionCreatedAt)}</td>
                    <td title={v.sha256Hex ?? undefined}>
                      {v.sha256Hex ? `${v.sha256Hex.slice(0, 16)}…` : "—"}
                    </td>
                    <td>
                      <button
                        type="button"
                        onClick={() => void view(v.versionNo)}
                      >
                        Xem nội dung
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
          {viewing && (
            <div className="card">
              <p>
                Nội dung phiên bản {viewing.versionNo}{" "}
                <button type="button" onClick={() => setViewing(null)}>
                  Đóng
                </button>
              </p>
              <pre className="payload">{prettyPayload(viewing.payload)}</pre>
            </div>
          )}

          <h4>Bổ sung sau khi khóa</h4>
          {record.recordStatus === "LOCKED" ? (
            <form onSubmit={(event) => void addAddendum(event)}>
              <label>
                Lý do
                <input
                  value={reason}
                  onChange={(e) => setReason(e.target.value)}
                  maxLength={2000}
                  required
                />
              </label>
              <label>
                Nội dung bổ sung
                <input
                  value={content}
                  onChange={(e) => setContent(e.target.value)}
                  maxLength={4000}
                  required
                />
              </label>
              <button
                type="submit"
                disabled={busy || reason.trim() === "" || content.trim() === ""}
              >
                Thêm bổ sung
              </button>
            </form>
          ) : (
            <p>Chỉ thêm bổ sung được khi hồ sơ đã khóa.</p>
          )}
          {addenda.length === 0 ? (
            <p>Chưa có bổ sung nào.</p>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Lý do</th>
                  <th>Nội dung</th>
                  <th>Trạng thái</th>
                  <th>Tạo lúc</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {addenda.map((a) => (
                  <tr key={a.addendumId}>
                    <td>{a.reason}</td>
                    <td>{a.content}</td>
                    <td>
                      <span className="badge">
                        {a.confirmationStatus === "CONFIRMED"
                          ? "Đã xác nhận"
                          : "Nháp"}
                      </span>
                    </td>
                    <td>{formatTime(a.createdAt)}</td>
                    <td>
                      {a.confirmationStatus === "DRAFT" && (
                        <button
                          type="button"
                          disabled={busy || !userId}
                          onClick={() =>
                            void run(() =>
                              emrApi.confirmAddendum(
                                encounterId,
                                a.addendumId,
                                userId,
                              ),
                            )
                          }
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
        </>
      )}
    </section>
  );
}
