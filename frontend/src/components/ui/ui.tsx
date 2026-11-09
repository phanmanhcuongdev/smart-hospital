import { useEffect, useState, type ReactNode } from "react";
 import { currentLocale, t, tf } from "../../lib/i18n";

// Shared UI helpers for the EMR and HR screens.

export function errText(e: unknown): string {
  return e instanceof Error ? e.message : t("Unexpected error.");
}

export interface LoadState<T> {
  data: T | null;
  loading: boolean;
  error: string | null;
  reload: () => void;
}

export function useAsync<T>(load: () => Promise<T>, deps: unknown[]): LoadState<T> {
  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [tick, setTick] = useState(0);
  useEffect(() => {
    let alive = true;
    setLoading(true);
    setError(null);
    load()
      .then((v) => {
        if (alive) {
          setData(v);
          setLoading(false);
        }
      })
      .catch((e: unknown) => {
        if (alive) {
          setError(errText(e));
          setLoading(false);
        }
      });
    return () => {
      alive = false;
    };
  }, [tick, ...deps]);
  return { data, loading, error, reload: () => setTick((t) => t + 1) };
}

export function fmtDateTime(value: string | null | undefined): string {
  if (value === null || value === undefined || value === "") {
    return "-";
  }
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) {
    return value;
  }
  return d.toLocaleString(currentLocale());
}

export function fmtDate(value: string | null | undefined): string {
  if (value === null || value === undefined || value === "") {
    return "-";
  }
  const dateOnly = value.length === 10 && value[4] === "-" && value[7] === "-";
  const d = dateOnly ? new Date(Number(value.slice(0, 4)), Number(value.slice(5, 7)) - 1, Number(value.slice(8, 10)))
    : new Date(value);
  if (Number.isNaN(d.getTime())) {
    return value;
  }
  return d.toLocaleDateString(currentLocale());
}

export function shortId(id: string | null | undefined): string {
  if (id === null || id === undefined) {
    return "-";
  }
  return id.length > 13 ? id.slice(0, 8) + "..." : id;
}

// Convert a datetime-local input value to an ISO instant.
export function toInstant(local: string): string {
  if (local === "") {
    return "";
  }
  return local.length === 16 ? local + ":00Z" : local;
}

export function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <label className="field">
      <span>{label}</span>
      {children}
    </label>
  );
}

export function ErrorBanner({ error }: { error: string | null }) {
  if (error === null || error === "") {
    return null;
  }
  return (
    <p role="alert" className="error">
      {error}
    </p>
  );
}

export function EmptyNote({ text }: { text: string }) {
  return <p className="muted">{text}</p>;
}

export function SubmitRow({ busy, label, onCancel }: { busy: boolean; label: string; onCancel?: () => void }) {
  return (
    <div className="row">
      <button className="btn" type="submit" disabled={busy}>
        {busy ? "Working..." : label}
      </button>
      {onCancel ? (
        <button className="btn" type="button" onClick={onCancel}>
          Cancel
        </button>
      ) : null}
    </div>
  );
}

export function Badge({ tone, children }: { tone: string; children: ReactNode }) {
  return <span className={"badge badge-" + tone}>{children}</span>;
}

export function toneFor(status: string): string {
  const ok = ["ACTIVE", "VALID", "COMPLETED", "OPEN", "LOCKED", "CONFIRMED", "APPLIED", "VERIFIED", "CLOSED"];
  const bad = ["EXPIRED", "REJECTED", "QUARANTINED", "CANCELLED", "TERMINATED", "REVOKED", "FAILED", "NO_SHOW"];
  if (ok.includes(status)) {
    return "ok";
  }
  if (bad.includes(status)) {
    return "bad";
  }
  return "warn";
}

export function useForm(initial: Record<string, string>): [Record<string, string>, (key: string, value: string) => void, () => void] {
  const [form, setForm] = useState<Record<string, string>>(initial);
  const set = (key: string, value: string): void => {
    setForm((f) => ({ ...f, [key]: value }));
  };
  const reset = (): void => {
    setForm(initial);
  };
  return [form, set, reset];
}

export interface FilterChipOption { value: string; label: string; count?: number; }

export function FilterChips({ label, options, value, onChange }: { label: string; options: FilterChipOption[]; value: string; onChange: (value: string) => void }) {
  return (
    <div className="chips" role="group" aria-label={label}>
      {options.map((option) => (
        <button key={option.value} type="button" className="chip" aria-pressed={value === option.value} onClick={() => onChange(option.value)}>
          {option.label}
          {option.count !== undefined && (<span className="chip-count">({option.count})</span>)}
        </button>
      ))}
    </div>
  );
}

export function ResultCount({ shown, total, noun }: { shown: number; total: number; noun: string }) {
  return <p className="result-count" role="status">{tf("Showing {shown} of {total} {noun}", { shown, total, noun })}</p>;
}

export function StatCard({ label, value, detail, action, onSelect }: { label: string; value: number | string; detail: string; action: string; onSelect: () => void }) {
  return (
    <button type="button" className="stat-card" onClick={onSelect}>
      <span className="stat-label">{label}</span>
      <span className="stat-value">{value}</span>
      {detail !== "" && <span className="stat-detail">{detail}</span>}
      <span className="stat-action">{action}</span>
    </button>
  );
}
