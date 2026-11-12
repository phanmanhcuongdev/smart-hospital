import { useEffect, useState } from "react";
import { emrApi, errorMessage } from "./emrApi";
import { NEXT_STATUS, STATUS_LABEL } from "./types";
import type { Encounter, EncounterStatus } from "./types";
import { DiagnosesTab } from "./DiagnosesTab";
import { ObservationsTab } from "./ObservationsTab";
import { OrdersTab } from "./OrdersTab";
import { PrescriptionsTab } from "./PrescriptionsTab";
import { RecordTab } from "./RecordTab";

type Tab = "vitals" | "diagnoses" | "orders" | "prescriptions" | "record";

const TABS: { id: Tab; label: string }[] = [
  { id: "vitals", label: "Sinh hiệu" },
  { id: "diagnoses", label: "Chẩn đoán" },
  { id: "orders", label: "Chỉ định" },
  { id: "prescriptions", label: "Đơn thuốc" },
  { id: "record", label: "Hồ sơ bệnh án" },
];

function formatTime(iso: string | null): string {
  return iso ? new Date(iso).toLocaleString("vi-VN") : "—";
}

export function EncounterDetailPage({
  encounterId,
  onBack,
}: {
  encounterId: string;
  onBack?: () => void;
}) {
  const [encounter, setEncounter] = useState<Encounter | null>(null);
  const [patientName, setPatientName] = useState("");
  const [doctorName, setDoctorName] = useState("");
  const [departmentName, setDepartmentName] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [tab, setTab] = useState<Tab>("vitals");

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError(null);
    emrApi
      .getEncounter(encounterId)
      .then(async (e) => {
        const [patients, doctors, depts] = await Promise.all([
          emrApi.searchPatients(""),
          emrApi.listPractitioners(),
          emrApi.listDepartments(),
        ]);
        if (!active) return;
        setEncounter(e);
        setPatientName(
          patients.find((p) => p.patientId === e.patientId)?.displayName ??
            e.patientId,
        );
        setDoctorName(
          doctors.find((d) => d.employeeId === e.attendingEmployeeId)
            ?.fullName ?? e.attendingEmployeeId,
        );
        if (e.departmentId) {
          setDepartmentName(
            depts.find((d) => d.departmentId === e.departmentId)?.displayName ??
              e.departmentId,
          );
        } else {
          setDepartmentName("—");
        }
      })
      .catch((e) => {
        if (active) setError(errorMessage(e));
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [encounterId]);

  async function changeStatus(next: EncounterStatus) {
    setBusy(true);
    setActionError(null);
    try {
      setEncounter(await emrApi.changeStatus(encounterId, next));
    } catch (e) {
      setActionError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <p>
        <a
          href="#/"
          onClick={(e) => {
            if (onBack) {
              e.preventDefault();
              window.location.hash = "#/";
              onBack();
            }
          }}
        >
          ← Danh sách lần khám
        </a>
      </p>
      {loading && <p role="status">Đang tải…</p>}
      {error && (
        <p role="alert" className="error">
          {error}
        </p>
      )}
      {encounter && (
        <>
          <section>
            <h2>Lần khám {encounter.encounterId.slice(0, 8)}</h2>
            <dl className="info">
              <dt>Bệnh nhân</dt>
              <dd>{patientName}</dd>
              <dt>Bác sĩ phụ trách</dt>
              <dd>{doctorName}</dd>
              <dt>Khoa / Phòng</dt>
              <dd>{departmentName}</dd>
              <dt>Trạng thái</dt>
              <dd>
                <span className="badge">{STATUS_LABEL[encounter.status]}</span>
              </dd>
              <dt>Bắt đầu</dt>
              <dd>{formatTime(encounter.startedAt)}</dd>
              <dt>Kết thúc</dt>
              <dd>{formatTime(encounter.completedAt)}</dd>
            </dl>
            <div className="toolbar">
              {NEXT_STATUS[encounter.status].map((next) => (
                <button
                  key={next}
                  type="button"
                  disabled={busy}
                  onClick={() => void changeStatus(next)}
                >
                  {next === "IN_PROGRESS" && encounter.status === "WAITING"
                    ? "👉 Tiếp nhận ca khám (Bắt đầu khám)"
                    : `Chuyển sang: ${STATUS_LABEL[next]}`}
                </button>
              ))}
              {NEXT_STATUS[encounter.status].length === 0 && (
                <span>Lần khám đã kết thúc, không đổi trạng thái được.</span>
              )}
            </div>
            {actionError && (
              <p role="alert" className="error">
                {actionError}
              </p>
            )}
          </section>

          <div className="tabs" role="tablist">
            {TABS.map((t) => (
              <button
                key={t.id}
                type="button"
                role="tab"
                aria-selected={tab === t.id}
                onClick={() => setTab(t.id)}
              >
                {t.label}
              </button>
            ))}
          </div>

          {tab === "vitals" && (
            <ObservationsTab
              encounterId={encounterId}
              status={encounter.status}
            />
          )}
          {tab === "diagnoses" && (
            <DiagnosesTab encounterId={encounterId} status={encounter.status} />
          )}
          {tab === "orders" && (
            <OrdersTab encounterId={encounterId} status={encounter.status} />
          )}
          {tab === "prescriptions" && (
            <PrescriptionsTab
              encounterId={encounterId}
              status={encounter.status}
            />
          )}
          {tab === "record" && (
            <RecordTab encounterId={encounterId} status={encounter.status} />
          )}
        </>
      )}
    </>
  );
}
