import { useState, type FormEvent } from "react";
import type { Auth } from "../../lib/api";
import { addAddendum, addDiagnosis, addVitals, archiveRecord, confirmAddendum, createOrder, createPrescription, encounterAction, getEncounterDetail, listAddenda, listResults, listVersions, lockRecord, listEncounters, openEncounter, orderAction, prescriptionAction, receiveResult, submitRecord, type Addendum, type CResult, type Encounter, type EncounterDetail, type Prescription, type RecordVersion } from "../../lib/api";
import { Badge, EmptyNote, ErrorBanner, Field, SubmitRow, errText, fmtDateTime, shortId, toInstant, toneFor, useAsync, useForm } from "../../components/ui/ui";

export type EmrTab = "overview" | "vitals" | "orders" | "results" | "prescriptions" | "record" | "addenda";
export type EmrView = { name: "emr-list" } | { name: "emr-detail"; encounterId: string; tab: EmrTab };

const SEED_PATIENT = "00000000-0000-0000-0000-000000000103";
const SEED_EMPLOYEE = "00000000-0000-0000-0000-000000000102";
const SEED_MEDICATION = "00000000-0000-0000-0000-000000000104";

export function EmrScreens({ auth, view, go }: { auth: Auth; view: EmrView; go: (v: EmrView) => void }) {
  if (view.name === "emr-detail") {
    return <EncounterDetailScreen auth={auth} encounterId={view.encounterId} tab={view.tab} go={go} />;
  }
  return <EncounterList auth={auth} go={go} />;
}

function EncounterList({ auth, go }: { auth: Auth; go: (v: EmrView) => void }) {
  const list = useAsync(() => listEncounters(auth), [auth.username]);
  const [showForm, setShowForm] = useState(false);
  const canOpen = auth.role === "DOCTOR" || auth.role === "RECEPTION";
  return (
    <section>
      <h2>Encounters (SCR-EMR-01)</h2>
      <p className="muted">Encounters visible to {auth.label}. Doctors see only their own encounters.</p>
      {canOpen ? (
        <button className="btn" type="button" onClick={() => setShowForm(true)}>
          Open encounter
        </button>
      ) : null}
      {showForm ? (
        <OpenEncounterForm
          auth={auth}
          onDone={(id) => {
            setShowForm(false);
            list.reload();
            if (id !== null) {
              go({ name: "emr-detail", encounterId: id, tab: "overview" });
            }
          }}
          onCancel={() => setShowForm(false)}
        />
      ) : null}
      {list.loading ? <p role="status">Loading encounters...</p> : null}
      <ErrorBanner error={list.error} />
      {list.data !== null && list.data.length === 0 ? <EmptyNote text="No encounters yet." /> : null}
      {list.data !== null && list.data.length > 0 ? <EncounterTable encounters={list.data} go={go} /> : null}
    </section>
  );
}

function EncounterTable({ encounters, go }: { encounters: Encounter[]; go: (v: EmrView) => void }) {
  return (
    <table className="table">
      <thead>
        <tr>
          <th>Encounter</th>
          <th>Patient</th>
          <th>Attending</th>
          <th>Status</th>
          <th>Started</th>
          <th>Opened by</th>
          <th>Row</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        {encounters.map((e) => (
          <tr key={e.id}>
            <td>{shortId(e.id)}</td>
            <td>{shortId(e.patientId)}</td>
            <td>{shortId(e.attendingEmployeeId)}</td>
            <td><Badge tone={toneFor(e.status)}>{e.status}</Badge></td>
            <td>{fmtDateTime(e.startedAt)}</td>
            <td>{e.createdBy}</td>
            <td>{e.rowVersion}</td>
            <td>
              <button className="btn btn-small" type="button" onClick={() => go({ name: "emr-detail", encounterId: e.id, tab: "overview" })}>
                Open
              </button>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

function OpenEncounterForm({ auth, onDone, onCancel }: { auth: Auth; onDone: (id: string | null) => void; onCancel: () => void }) {
  const [form, set, reset] = useForm({ patientId: SEED_PATIENT, appointmentId: "", attendingEmployeeId: SEED_EMPLOYEE });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const created = await openEncounter(auth, {
        patientId: form.patientId.trim(),
        appointmentId: form.appointmentId.trim() === "" ? null : form.appointmentId.trim(),
        attendingEmployeeId: form.attendingEmployeeId.trim(),
      });
      reset();
      onDone(created.id);
    } catch (err: unknown) {
      setError(errText(err));
    } finally {
      setBusy(false);
    }
  }
  return (
    <form className="card" onSubmit={submit}>
      <h3>Open encounter</h3>
      <p className="muted">The patient must be VERIFIED. Demo patient: Synthetic Patient ({SEED_PATIENT}). Demo attending: EMP-DEMO-001 ({SEED_EMPLOYEE}).</p>
      <Field label="Patient ID">
        <input value={form.patientId} onChange={(e) => set("patientId", e.target.value)} required />
      </Field>
      <Field label="Appointment ID (optional)">
        <input value={form.appointmentId} onChange={(e) => set("appointmentId", e.target.value)} />
      </Field>
      <Field label="Attending employee ID">
        <input value={form.attendingEmployeeId} onChange={(e) => set("attendingEmployeeId", e.target.value)} required />
      </Field>
      <ErrorBanner error={error} />
      <SubmitRow busy={busy} label="Open encounter" onCancel={onCancel} />
    </form>
  );
}

function tabLabel(t: EmrTab): string {
  const labels: Record<EmrTab, string> = {
    overview: "Overview",
    vitals: "Vitals & diagnosis",
    orders: "Orders",
    results: "Results",
    prescriptions: "Prescriptions",
    record: "Record",
    addenda: "Addenda",
  };
  return labels[t];
}

function EncounterDetailScreen({ auth, encounterId, tab, go }: { auth: Auth; encounterId: string; tab: EmrTab; go: (v: EmrView) => void }) {
  const detail = useAsync(() => getEncounterDetail(auth, encounterId), [encounterId, auth.username]);
  const [actionError, setActionError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  if (detail.loading && detail.data === null) {
    return <p role="status">Loading encounter...</p>;
  }
  if (detail.error !== null && detail.data === null) {
    return <ErrorBanner error={detail.error} />;
  }
  const d = detail.data;
  if (d === null) {
    return <EmptyNote text="Encounter not found." />;
  }
  const enc = d.encounter;
  const canDoctor = auth.role === "DOCTOR";
  async function act(action: "start" | "complete" | "cancel") {
    setBusy(true);
    setActionError(null);
    try {
      await encounterAction(auth, enc.id, action);
      detail.reload();
    } catch (e: unknown) {
      setActionError(errText(e));
    } finally {
      setBusy(false);
    }
  }
  return (
    <section>
      <button className="btn" type="button" onClick={() => go({ name: "emr-list" })}>
        Back to encounters
      </button>
      <h2>Encounter {shortId(enc.id)} (SCR-EMR-02)</h2>
      <dl className="kv">
        <div>
          <dt>Patient</dt>
          <dd>{enc.patientId}</dd>
        </div>
        <div>
          <dt>Attending employee</dt>
          <dd>{shortId(enc.attendingEmployeeId)}</dd>
        </div>
        <div>
          <dt>Status</dt>
          <dd>
            <Badge tone={toneFor(enc.status)}>{enc.status}</Badge>
          </dd>
        </div>
        <div>
          <dt>Started</dt>
          <dd>{fmtDateTime(enc.startedAt)}</dd>
        </div>
        <div>
          <dt>Completed</dt>
          <dd>{fmtDateTime(enc.completedAt)}</dd>
        </div>
        <div>
          <dt>Row version</dt>
          <dd>{enc.rowVersion}</dd>
        </div>
        <div>
          <dt>Opened by</dt>
          <dd>{enc.createdBy}</dd>
        </div>
      </dl>
      {canDoctor ? (
        <div className="row">
          <button className="btn" type="button" disabled={busy || enc.status !== "WAITING"} onClick={() => act("start")}>
            Start
          </button>
          <button className="btn" type="button" disabled={busy || (enc.status !== "IN_PROGRESS" && enc.status !== "DIAGNOSED" && enc.status !== "PENDING_LAB_RAD")} onClick={() => act("complete")}>
            Complete
          </button>
          <button className="btn" type="button" disabled={busy || (enc.status !== "WAITING" && enc.status !== "IN_PROGRESS")} onClick={() => act("cancel")}>
            Cancel
          </button>
        </div>
      ) : null}
      <ErrorBanner error={actionError} />
      <nav className="tabs" aria-label="Encounter sections">
        {(["overview", "vitals", "orders", "results", "prescriptions", "record", "addenda"] as EmrTab[]).map((t) => (
          <button key={t} type="button" className={tab === t ? "tab active" : "tab"} onClick={() => go({ name: "emr-detail", encounterId, tab: t })}>
            {tabLabel(t)}
          </button>
        ))}
      </nav>
      {tab === "overview" ? <EncounterOverview detail={d} /> : null}
      {tab === "vitals" ? <><VitalsPanel auth={auth} detail={d} reload={detail.reload} /><DiagnosisPanel auth={auth} detail={d} reload={detail.reload} /></> : null}
      {tab === "orders" ? <OrdersPanel auth={auth} detail={d} reload={detail.reload} go={go} encounterId={encounterId} /> : null}
      {tab === "results" ? <ResultsPanel auth={auth} detail={d} reload={detail.reload} /> : null}
      {tab === "prescriptions" ? <PrescriptionsPanel auth={auth} detail={d} reload={detail.reload} /> : null}
      {tab === "record" ? <RecordPanel auth={auth} detail={d} reload={detail.reload} /> : null}
      {tab === "addenda" ? <AddendaPanel auth={auth} detail={d} reload={detail.reload} /> : null}
    </section>
  );
}

function EncounterOverview({ detail }: { detail: EncounterDetail }) {
  return (
    <div>
      <h3>Overview</h3>
      <p className="muted">
        This encounter has {detail.observations.length} vital-sign records, {detail.diagnoses.length} diagnoses, {detail.orders.length} orders and {detail.prescriptions.length} prescriptions.
      </p>
      <p>
        Medical record: {detail.record === null ? "not started" : detail.record.status}
        {detail.record !== null ? " (version " + detail.record.currentVersion + ", row " + detail.record.rowVersion + ")" : ""}
      </p>
      <p className="muted">Completed at {fmtDateTime(detail.encounter.completedAt)}.</p>
    </div>
  );
}

function VitalsPanel({ auth, detail, reload }: { auth: Auth; detail: EncounterDetail; reload: () => void }) {
  const isNurse = auth.role === "NURSE";
  const [form, set, reset] = useForm({ metricCode: "BP_SYS", numericValue: "", textValue: "", unitCode: "mmHg", observedAt: new Date().toISOString().slice(0, 16) });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await addVitals(auth, detail.encounter.id, {
        metricCode: form.metricCode.trim(),
        numericValue: form.numericValue.trim() === "" ? null : Number(form.numericValue),
        textValue: form.textValue.trim() === "" ? null : form.textValue.trim(),
        unitCode: form.unitCode.trim() === "" ? null : form.unitCode.trim(),
        observedAt: toInstant(form.observedAt),
      });
      reset();
      reload();
    } catch (err: unknown) {
      setError(errText(err));
    } finally {
      setBusy(false);
    }
  }
  return (
    <div>
      <h3>Vitals and diagnosis (SCR-EMR-03)</h3>
      {isNurse ? (
        <form className="card" onSubmit={submit}>
          <h4>Record vital sign (nurse)</h4>
          <Field label="Metric code">
            <input value={form.metricCode} onChange={(e) => set("metricCode", e.target.value)} required />
          </Field>
          <div className="row">
            <Field label="Numeric value">
              <input type="number" step="any" value={form.numericValue} onChange={(e) => set("numericValue", e.target.value)} />
            </Field>
            <Field label="Unit">
              <input value={form.unitCode} onChange={(e) => set("unitCode", e.target.value)} />
            </Field>
          </div>
          <Field label="Text value (optional)">
            <input value={form.textValue} onChange={(e) => set("textValue", e.target.value)} />
          </Field>
          <Field label="Observed at (UTC)">
            <input type="datetime-local" value={form.observedAt} onChange={(e) => set("observedAt", e.target.value)} required />
          </Field>
          <ErrorBanner error={error} />
          <SubmitRow busy={busy} label="Record vital sign" />
        </form>
      ) : (
        <p className="muted">Vital signs can only be recorded by a nurse. You are signed in as {auth.role}.</p>
      )}
      {detail.observations.length === 0 ? <EmptyNote text="No vital signs recorded yet." /> : null}
      {detail.observations.length > 0 ? (
        <table className="table">
          <thead>
            <tr>
              <th>Metric</th>
              <th>Value</th>
              <th>Unit</th>
              <th>Observed at</th>
              <th>Source</th>
              <th>Quality</th>
            </tr>
          </thead>
          <tbody>
            {detail.observations.map((o) => (
              <tr key={o.id}>
                <td>{o.metricCode}</td>
                <td>{o.numericValue !== null ? o.numericValue : o.textValue}</td>
                <td>{o.unitCode}</td>
                <td>{fmtDateTime(o.observedAt)}</td>
                <td>{o.sourceType}</td>
                <td><Badge tone={toneFor(o.quality)}>{o.quality}</Badge></td>
              </tr>
            ))}
          </tbody>
        </table>
      ) : null}
    </div>
  );
}

function DiagnosisPanel({ auth, detail, reload }: { auth: Auth; detail: EncounterDetail; reload: () => void }) {
  const isDoctor = auth.role === "DOCTOR";
  const [form, set, reset] = useForm({ type: "PRIMARY", icd10Code: "", narrative: "", status: "PROVISIONAL" });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await addDiagnosis(auth, detail.encounter.id, {
        type: form.type,
        icd10Code: form.icd10Code.trim() === "" ? null : form.icd10Code.trim(),
        narrative: form.narrative.trim(),
        status: form.status,
      });
      reset();
      reload();
    } catch (err: unknown) {
      setError(errText(err));
    } finally {
      setBusy(false);
    }
  }
  return (
    <div>
      <h4>Diagnosis (doctor)</h4>
      {isDoctor ? (
        <form className="card" onSubmit={submit}>
          <Field label="Type">
            <select value={form.type} onChange={(e) => set("type", e.target.value)}>
              <option value="PRIMARY">PRIMARY</option>
              <option value="SECONDARY">SECONDARY</option>
            </select>
          </Field>
          <Field label="ICD-10 code (required for a CONFIRMED diagnosis)">
            <input value={form.icd10Code} onChange={(e) => set("icd10Code", e.target.value)} />
          </Field>
          <Field label="Narrative">
            <textarea value={form.narrative} onChange={(e) => set("narrative", e.target.value)} required />
          </Field>
          <Field label="Status">
            <select value={form.status} onChange={(e) => set("status", e.target.value)}>
              <option value="PROVISIONAL">PROVISIONAL</option>
              <option value="CONFIRMED">CONFIRMED</option>
            </select>
          </Field>
          <ErrorBanner error={error} />
          <SubmitRow busy={busy} label="Record diagnosis" />
        </form>
      ) : (
        <p className="muted">Diagnoses can only be recorded by a doctor. You are signed in as {auth.role}.</p>
      )}
      {detail.diagnoses.length === 0 ? <EmptyNote text="No diagnoses recorded yet." /> : null}
      {detail.diagnoses.length > 0 ? (
        <table className="table">
          <thead>
            <tr>
              <th>Type</th>
              <th>ICD-10</th>
              <th>Narrative</th>
              <th>Status</th>
              <th>Confirmed by</th>
            </tr>
          </thead>
          <tbody>
            {detail.diagnoses.map((d) => (
              <tr key={d.id}>
                <td>{d.type}</td>
                <td>{d.icd10Code}</td>
                <td>{d.narrative}</td>
                <td><Badge tone={toneFor(d.status)}>{d.status}</Badge></td>
                <td>{d.confirmedBy}</td>
              </tr>
            ))}
          </tbody>
        </table>
      ) : null}
    </div>
  );
}

function OrdersPanel({ auth, detail, reload, go, encounterId }: { auth: Auth; detail: EncounterDetail; reload: () => void; go: (v: EmrView) => void; encounterId: string }) {
  const isDoctor = auth.role === "DOCTOR";
  const isCls = auth.role === "CLS_TECHNICIAN";
  const [form, set, reset] = useForm({ type: "LAB", externalOrderRef: "" });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await createOrder(auth, detail.encounter.id, {
        orderType: form.type,
        externalOrderRef: form.externalOrderRef.trim() === "" ? null : form.externalOrderRef.trim(),
      });
      reset();
      reload();
    } catch (err: unknown) {
      setError(errText(err));
    } finally {
      setBusy(false);
    }
  }
  async function act(orderId: string, action: "submit" | "acknowledge" | "start" | "cancel") {
    setActionError(null);
    try {
      await orderAction(auth, orderId, action);
      reload();
    } catch (err: unknown) {
      setActionError(errText(err));
    }
  }
  return (
    <div>
      <h3>Clinical orders (SCR-EMR-04)</h3>
      {isDoctor ? (
        <form className="card" onSubmit={submit}>
          <h4>New order (doctor)</h4>
          <Field label="Order type">
            <select value={form.type} onChange={(e) => set("type", e.target.value)}>
              <option value="LAB">LAB</option>
              <option value="RAD">RAD</option>
            </select>
          </Field>
          <Field label="External order reference (optional)">
            <input value={form.externalOrderRef} onChange={(e) => set("externalOrderRef", e.target.value)} />
          </Field>
          <ErrorBanner error={error} />
          <SubmitRow busy={busy} label="Create order" />
        </form>
      ) : (
        <p className="muted">Orders can only be created by a doctor. You are signed in as {auth.role}.</p>
      )}
      <ErrorBanner error={actionError} />
      {detail.orders.length === 0 ? <EmptyNote text="No orders yet." /> : null}
      {detail.orders.length > 0 ? (
        <table className="table">
          <thead>
            <tr>
              <th>Order</th>
              <th>Type</th>
              <th>Status</th>
              <th>External ref</th>
              <th>Ordered by</th>
              <th>Ordered at</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {detail.orders.map((o) => (
              <tr key={o.id}>
                <td>{shortId(o.id)}</td>
                <td>{o.type}</td>
                <td>
                  <Badge tone={toneFor(o.status)}>{o.status}</Badge>
                </td>
                <td>{o.externalOrderRef}</td>
                <td>{o.orderedBy}</td>
                <td>{fmtDateTime(o.orderedAt)}</td>
                <td>
                  {isDoctor && o.status === "DRAFT" ? (
                    <button className="btn btn-small" type="button" onClick={() => act(o.id, "submit")}>
                      Submit
                    </button>
                  ) : null}
                  {isCls && o.status === "ORDERED" ? (
                    <button className="btn btn-small" type="button" onClick={() => act(o.id, "acknowledge")}>
                      Acknowledge
                    </button>
                  ) : null}
                  {isCls && o.status === "ACKNOWLEDGED" ? (
                    <button className="btn btn-small" type="button" onClick={() => act(o.id, "start")}>
                      Start
                    </button>
                  ) : null}
                  {isDoctor && o.status !== "COMPLETED" && o.status !== "CANCELLED" ? (
                    <button className="btn btn-small" type="button" onClick={() => act(o.id, "cancel")}>
                      Cancel
                    </button>
                  ) : null}
                  <button className="btn btn-small" type="button" onClick={() => go({ name: "emr-detail", encounterId, tab: "results" })}>
                    Results
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      ) : null}
    </div>
  );
}

function ResultsPanel({ auth, detail, reload }: { auth: Auth; detail: EncounterDetail; reload: () => void }) {
  const isCls = auth.role === "CLS_TECHNICIAN";
  const [orderId, setOrderId] = useState(detail.orders.length > 0 ? detail.orders[0].id : "");
  const results = useAsync(() => (orderId === "" ? Promise.resolve([] as CResult[]) : listResults(auth, orderId)), [orderId, auth.username]);
  const [form, set, reset] = useForm({ patientId: detail.encounter.patientId, sourceSystem: "LAB-1", sourceResultId: "", sourceVersion: "1", resultPayload: "", abnormalFlag: "" });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const selected = detail.orders.find((o) => o.id === orderId) ?? null;
  async function submit(e: FormEvent) {
    e.preventDefault();
    if (orderId === "") {
      setError("Select an order first.");
      return;
    }
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      const res = await receiveResult(auth, orderId, {
        patientId: form.patientId.trim(),
        sourceSystem: form.sourceSystem.trim(),
        sourceResultId: form.sourceResultId.trim(),
        sourceVersion: form.sourceVersion.trim(),
        resultPayload: form.resultPayload.trim(),
        abnormalFlag: form.abnormalFlag.trim() === "" ? null : form.abnormalFlag.trim().toLowerCase() === "true",
      });
      reset();
      results.reload();
      reload();
      if (res.duplicate) {
        setNotice("Duplicate result received: the existing result was returned.");
      }
    } catch (err: unknown) {
      setError(errText(err));
    } finally {
      setBusy(false);
    }
  }
  return (
    <div>
      <h3>CLS results (SCR-EMR-05)</h3>
      {detail.orders.length === 0 ? (
        <EmptyNote text="No orders yet. Create an order on the Orders tab first." />
      ) : (
        <div>
          <Field label="Order">
            <select value={orderId} onChange={(e) => setOrderId(e.target.value)}>
              {detail.orders.map((o) => (
                <option key={o.id} value={o.id}>
                  {o.type} - {shortId(o.id)} ({o.status})
                </option>
              ))}
            </select>
          </Field>
          {selected !== null ? (
            <p>
              Order status: <Badge tone={toneFor(selected.status)}>{selected.status}</Badge>{" "}
              {selected.status !== "COMPLETED" ? <Badge tone="warn">PENDING</Badge> : null}
            </p>
          ) : null}
          {results.loading ? <p role="status">Loading results...</p> : null}
          <ErrorBanner error={results.error} />
          {results.data !== null && results.data.length === 0 ? <EmptyNote text="No results for this order yet. The result is pending." /> : null}
          {results.data !== null && results.data.length > 0 ? (
            <table className="table">
              <thead>
                <tr>
                  <th>Result</th>
                  <th>Source</th>
                  <th>Source result ID</th>
                  <th>Version</th>
                  <th>Received at</th>
                  <th>Abnormal flag</th>
                  <th>Quality</th>
                  <th>Payload</th>
                </tr>
              </thead>
              <tbody>
                {results.data.map((r) => (
                  <tr key={r.id} className={r.quality === "QUARANTINED" ? "row-quarantined" : ""}>
                    <td>{shortId(r.id)}</td>
                    <td>{r.sourceSystem}</td>
                    <td>{r.sourceResultId}</td>
                    <td>{r.sourceVersion}</td>
                    <td>{fmtDateTime(r.receivedAt)}</td>
                    <td>{r.abnormalFlag}</td>
                    <td>
                      <Badge tone={toneFor(r.quality)}>{r.quality}</Badge>
                      {r.duplicate ? " (duplicate)" : ""}
                    </td>
                    <td>{r.resultPayload}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : null}
        </div>
      )}
      {isCls && orderId !== "" ? (
        <form className="card" onSubmit={submit}>
          <h4>Receive result (CLS technician)</h4>
          <p className="muted">The patient ID must match the encounter patient. A result for a different patient is quarantined and does not complete the order.</p>
          <Field label="Patient ID">
            <input value={form.patientId} onChange={(e) => set("patientId", e.target.value)} required />
          </Field>
          <div className="row">
            <Field label="Source system">
              <input value={form.sourceSystem} onChange={(e) => set("sourceSystem", e.target.value)} required />
            </Field>
            <Field label="Source result ID">
              <input value={form.sourceResultId} onChange={(e) => set("sourceResultId", e.target.value)} required />
            </Field>
            <Field label="Source version (optional)">
              <input value={form.sourceVersion} onChange={(e) => set("sourceVersion", e.target.value)} required />
            </Field>
          </div>
          <Field label="Result payload (JSON or text)">
            <textarea value={form.resultPayload} onChange={(e) => set("resultPayload", e.target.value)} required />
          </Field>
          <Field label="Abnormal flag (true or false, optional)">
            <input value={form.abnormalFlag} onChange={(e) => set("abnormalFlag", e.target.value)} />
          </Field>
          {notice !== null ? <p className="muted">{notice}</p> : null}
          <ErrorBanner error={error} />
          <SubmitRow busy={busy} label="Receive result" />
        </form>
      ) : null}
    </div>
  );
}

interface ItemDraft { medicationId: string; dose: string; route: string; frequency: string; daysCount: string; quantity: string; unitCode: string; }

function emptyItem(): ItemDraft {
  return { medicationId: SEED_MEDICATION, dose: "", route: "PO", frequency: "1x/day", daysCount: "7", quantity: "1", unitCode: "tablet" };
}
function allergyTone(warning: string): string {
  if (warning.toLowerCase().includes("known")) {
    return "bad";
  }
  if (warning.toLowerCase().includes("unknown")) {
    return "warn";
  }
  return "warn";
}

function PrescriptionsPanel({ auth, detail, reload }: { auth: Auth; detail: EncounterDetail; reload: () => void }) {
  const isDoctor = auth.role === "DOCTOR";
  const [items, setItems] = useState<ItemDraft[]>([emptyItem()]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [created, setCreated] = useState<Prescription | null>(null);
  function setItem(index: number, key: keyof ItemDraft, value: string) {
    setItems((list) => list.map((it, i) => (i === index ? { ...it, [key]: value } : it)));
  }
  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    setCreated(null);
    try {
      const body = items.map((it) => ({
        medicationId: it.medicationId.trim(),
        dose: it.dose.trim(),
        route: it.route.trim(),
        frequency: it.frequency.trim(),
        daysCount: Number(it.daysCount),
        quantity: Number(it.quantity),
        unitCode: it.unitCode.trim(),
      }));
      const p = await createPrescription(auth, detail.encounter.id, { items: body });
      setCreated(p);
      setItems([emptyItem()]);
      reload();
    } catch (err: unknown) {
      setError(errText(err));
    } finally {
      setBusy(false);
    }
  }
  async function act(id: string, action: "issue" | "cancel") {
    setActionError(null);
    try {
      await prescriptionAction(auth, id, action);
      reload();
    } catch (err: unknown) {
      setActionError(errText(err));
    }
  }
  return (
    <div>
      <h3>Prescriptions (SCR-EMR-06)</h3>
      <p className="muted">Allergy warnings come from the prescription response. A known allergy blocks the prescription; missing or unconfirmed allergy data only produces a warning. Missing allergy data never means the medication is safe.</p>
      {created !== null ? (
        <div className="card">
          <h4>Prescription created</h4>
          <p>
            Prescription {shortId(created.id)} — status <Badge tone={toneFor(created.status)}>{created.status}</Badge>
          </p>
          {created.allergyWarnings.length === 0 ? (
            <p className="muted">No allergy warning was returned for this prescription. This does not mean the medication is safe; allergy data may simply be missing or unconfirmed.</p>
          ) : null}
          {created.allergyWarnings.map((w, i) => (
            <p key={i}>
              <Badge tone={allergyTone(w)}>{w.toLowerCase().includes("known") ? "KNOWN" : w.toLowerCase().includes("unknown") ? "UNKNOWN" : "WARNING"}</Badge> {w}
            </p>
          ))}
        </div>
      ) : null}
      <ErrorBanner error={actionError} />
      {detail.prescriptions.length === 0 ? <EmptyNote text="No prescriptions yet." /> : null}
      {detail.prescriptions.length > 0 ? (
        <table className="table">
          <thead>
            <tr>
              <th>Prescription</th>
              <th>Prescriber</th>
              <th>Status</th>
              <th>Issued at</th>
              <th>Items</th>
              <th>Allergy warnings</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {detail.prescriptions.map((p) => (
              <tr key={p.id}>
                <td>{shortId(p.id)}</td>
                <td>{p.prescriberId}</td>
                <td><Badge tone={toneFor(p.status)}>{p.status}</Badge></td>
                <td>{fmtDateTime(p.issuedAt)}</td>
                <td>{p.items.map((it) => it.medicationLabel + " " + (it.dose ?? "") + " " + (it.frequency ?? "")).join("; ")}</td>
                <td>
                  {p.allergyWarnings.length === 0 ? "None returned" : null}
                  {p.allergyWarnings.map((w, i) => (
                    <div key={i}>
                      <Badge tone={allergyTone(w)}>{w.toLowerCase().includes("known") ? "KNOWN" : w.toLowerCase().includes("unknown") ? "UNKNOWN" : "WARNING"}</Badge> {w}
                    </div>
                  ))}
                </td>
                <td>
                  {isDoctor && p.status === "ACTIVE" ? (
                    <button className="btn btn-small" type="button" onClick={() => act(p.id, "issue")}>
                      Issue
                    </button>
                  ) : null}
                  {isDoctor && p.status === "ACTIVE" ? (
                    <button className="btn btn-small" type="button" onClick={() => act(p.id, "cancel")}>
                      Cancel
                    </button>
                  ) : null}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      ) : null}
      {isDoctor ? (
        <form className="card" onSubmit={submit}>
          <h4>New prescription (doctor)</h4>
          <p className="muted">The prescriber must have a valid CCHN and the medication must be active. A known allergy blocks the prescription with an error; the prescription is not created.</p>
          {items.map((it, index) => (
            <div key={index} className="card card-nested">
              <p>Item {index + 1}</p>
              <Field label="Medication ID">
                <input value={it.medicationId} onChange={(e) => setItem(index, "medicationId", e.target.value)} required />
              </Field>
              <div className="row">
                <Field label="Dose">
                  <input value={it.dose} onChange={(e) => setItem(index, "dose", e.target.value)} required />
                </Field>
                <Field label="Route">
                  <input value={it.route} onChange={(e) => setItem(index, "route", e.target.value)} required />
                </Field>
                <Field label="Frequency">
                  <input value={it.frequency} onChange={(e) => setItem(index, "frequency", e.target.value)} required />
                </Field>
              </div>
              <div className="row">
                <Field label="Days">
                  <input type="number" min="1" value={it.daysCount} onChange={(e) => setItem(index, "daysCount", e.target.value)} required />
                </Field>
                <Field label="Quantity">
                  <input type="number" min="1" value={it.quantity} onChange={(e) => setItem(index, "quantity", e.target.value)} required />
                </Field>
                <Field label="Unit">
                  <input value={it.unitCode} onChange={(e) => setItem(index, "unitCode", e.target.value)} required />
                </Field>
              </div>
              {items.length > 1 ? (
                <button className="btn btn-small" type="button" onClick={() => setItems((list) => list.filter((_, i) => i !== index))}>
                  Remove item
                </button>
              ) : null}
            </div>
          ))}
          <div className="row">
            <button className="btn" type="button" onClick={() => setItems((list) => [...list, emptyItem()])}>
              Add item
            </button>
          </div>
          <ErrorBanner error={error} />
          <SubmitRow busy={busy} label="Create prescription" />
        </form>
      ) : (
        <p className="muted">Prescriptions can only be created by a doctor. You are signed in as {auth.role}.</p>
      )}
    </div>
  );
}

function RecordPanel({ auth, detail, reload }: { auth: Auth; detail: EncounterDetail; reload: () => void }) {
  const rec = detail.record;
  const versions = useAsync(() => (rec === null ? Promise.resolve([] as RecordVersion[]) : listVersions(auth, rec.id)), [rec?.id, auth.username]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  async function act(action: "submit" | "lock" | "archive") {
    if (rec === null) {
      return;
    }
    setBusy(true);
    setError(null);
    try {
      if (action === "submit") {
        await submitRecord(auth, rec.id, rec.rowVersion);
      } else if (action === "lock") {
        await lockRecord(auth, rec.id);
      } else {
        await archiveRecord(auth, rec.id);
      }
      reload();
    } catch (err: unknown) {
      setError(errText(err));
    } finally {
      setBusy(false);
    }
  }
  const canSign = auth.role === "DOCTOR" || auth.role === "RECORDS_MANAGER";
  const canArchive = auth.role === "RECORDS_MANAGER";
  return (
    <div>
      <h3>Medical record (SCR-EMR-07, SCR-EMR-08)</h3>
      {rec === null ? (
        <EmptyNote text="No medical record exists for this encounter yet. The record is created when the encounter completes." />
      ) : (
        <div>
          <dl className="kv">
            <div><dt>Record</dt><dd>{shortId(rec.id)}</dd></div>
            <div><dt>Status</dt><dd><Badge tone={toneFor(rec.status)}>{rec.status}</Badge></dd></div>
            <div><dt>Current version</dt><dd>{rec.currentVersion}</dd></div>
            <div><dt>Row version</dt><dd>{rec.rowVersion}</dd></div>
            <div><dt>Locked by</dt><dd>{rec.lockedBy}</dd></div>
            <div><dt>Locked at</dt><dd>{fmtDateTime(rec.lockedAt)}</dd></div>
          </dl>
          <p className="muted">Record lifecycle: DRAFT → PENDING_SIGNATURE → LOCKED → ARCHIVED. Submitting uses the current row version; if the record changed, the request is refused with a conflict (409) and nothing is overwritten.</p>
          {canSign ? (
            <div className="row">
              <button className="btn" type="button" disabled={busy || rec.status !== "DRAFT"} onClick={() => act("submit")}>
                Submit for signature
              </button>
              <button className="btn" type="button" disabled={busy || rec.status !== "PENDING_SIGNATURE"} onClick={() => act("lock")}>
                Lock record (demo)
              </button>
            </div>
          ) : null}
          {canArchive ? (
            <div className="row">
              <button className="btn" type="button" disabled={busy || rec.status !== "LOCKED"} onClick={() => act("archive")}>
                Archive record
              </button>
            </div>
          ) : null}
          <ErrorBanner error={error} />
          <p className="card">Demo confirmation (SCR-EMR-08): Locking a record here is a demo confirmation only. It is NOT a digital signature and NOT a legally binding signature. No cryptographic signature is created or verified.</p>
          <h4>Version history</h4>
          {versions.loading ? <p role="status">Loading versions...</p> : null}
          <ErrorBanner error={versions.error} />
          {versions.data !== null && versions.data.length === 0 ? <EmptyNote text="No versions recorded yet." /> : null}
          {versions.data !== null && versions.data.length > 0 ? (
            <table className="table">
              <thead>
                <tr>
                  <th>Version</th>
                  <th>SHA-256</th>
                  <th>Created at</th>
                  <th>Created by</th>
                </tr>
              </thead>
              <tbody>
                {versions.data.map((v) => (
                  <tr key={v.versionNo}>
                    <td>{v.versionNo}</td>
                    <td>{v.sha256Hex}</td>
                    <td>{fmtDateTime(v.versionCreatedAt)}</td>
                    <td>{v.createdBy}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : null}
        </div>
      )}
    </div>
  );
}

function AddendaPanel({ auth, detail, reload }: { auth: Auth; detail: EncounterDetail; reload: () => void }) {
  const rec = detail.record;
  const addenda = useAsync(() => (rec === null ? Promise.resolve([] as Addendum[]) : listAddenda(auth, rec.id)), [rec?.id, auth.username]);
  const [form, set, reset] = useForm({ reason: "", content: "" });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const canAdd = auth.role === "DOCTOR" || auth.role === "RECORDS_MANAGER";
  async function submit(e: FormEvent) {
    e.preventDefault();
    if (rec === null) {
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await addAddendum(auth, rec.id, { reason: form.reason.trim(), content: form.content.trim() });
      reset();
      addenda.reload();
      reload();
    } catch (err: unknown) {
      setError(errText(err));
    } finally {
      setBusy(false);
    }
  }
  async function confirm(id: string) {
    setActionError(null);
    try {
      await confirmAddendum(auth, id);
      addenda.reload();
    } catch (err: unknown) {
      setActionError(errText(err));
    }
  }
  return (
    <div>
      <h3>Addenda (SCR-EMR-09)</h3>
      {rec === null ? (
        <EmptyNote text="No medical record exists for this encounter yet. Addenda can only be added to a LOCKED record." />
      ) : (
        <div>
          <p className="muted">
            Record status: <Badge tone={toneFor(rec.status)}>{rec.status}</Badge> Addenda can only be added to a LOCKED record. The original record is kept unchanged; an addendum is a separate, confirmed annotation.
          </p>
          {canAdd && rec.status === "LOCKED" ? (
            <form className="card" onSubmit={submit}>
              <h4>New addendum</h4>
              <Field label="Reason">
                <input value={form.reason} onChange={(e) => set("reason", e.target.value)} required />
              </Field>
              <Field label="Content">
                <textarea value={form.content} onChange={(e) => set("content", e.target.value)} required />
              </Field>
              <ErrorBanner error={error} />
              <SubmitRow busy={busy} label="Add addendum" />
            </form>
          ) : null}
          {canAdd && rec.status !== "LOCKED" ? (
            <p className="muted">The record is {rec.status}, not LOCKED, so no addendum can be added yet.</p>
          ) : null}
          <ErrorBanner error={actionError} />
          {addenda.loading ? <p role="status">Loading addenda...</p> : null}
          <ErrorBanner error={addenda.error} />
          {addenda.data !== null && addenda.data.length === 0 ? <EmptyNote text="No addenda yet." /> : null}
          {addenda.data !== null && addenda.data.length > 0 ? (
            <table className="table">
              <thead>
                <tr>
                  <th>Addendum</th>
                  <th>Reason</th>
                  <th>Content</th>
                  <th>Status</th>
                  <th>Created at</th>
                  <th>Created by</th>
                  <th>Confirmed at</th>
                  <th>Confirmed by</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {addenda.data.map((a) => (
                  <tr key={a.id}>
                    <td>{shortId(a.id)}</td>
                    <td>{a.reason}</td>
                    <td>{a.content}</td>
                    <td><Badge tone={toneFor(a.confirmationStatus)}>{a.confirmationStatus}</Badge></td>
                    <td>{fmtDateTime(a.createdAt)}</td>
                    <td>{a.createdBy}</td>
                    <td>{fmtDateTime(a.confirmedAt)}</td>
                    <td>{a.confirmedBy}</td>
                    <td>
                      {canAdd && a.confirmationStatus === "DRAFT" ? (
                        <button className="btn btn-small" type="button" onClick={() => confirm(a.id)}>
                          Confirm
                        </button>
                      ) : null}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : null}
        </div>
      )}
    </div>
  );
}
