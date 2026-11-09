import { useEffect, useRef, useState } from "react";
import type { ReactNode } from "react";
import type { Auth, Department, Employee, Encounter } from "../../lib/api";
import { listDepartments, listEncounters, searchEmployees } from "../../lib/api";
import { isClinical, isHr } from "../../auth/auth";

export interface SearchHit {
  kind: "Encounter" | "Employee" | "Department";
  id: string;
  title: string;
  detail: string;
}

export interface NavEntry {
  id: string;
  label: string;
  group: string;
  icon: string;
  active: boolean;
  badge?: number;
  onSelect: () => void;
}

export interface AppShellProps {
  auth: Auth;
  nav: NavEntry[];
  bottomNav: NavEntry[];
  pendingDecisions: number;
  onOpenPendingDecisions: () => void;
  onSearchSelect: (hit: SearchHit) => void;
  onSignOut: () => void;
  fab: { label: string; onClick: () => void } | null;
  children: ReactNode;
}

export function greetingFor(date: Date): string {
  const h = date.getHours();
  if (h < 5) return "Good night";
  if (h < 12) return "Good morning";
  if (h < 18) return "Good afternoon";
  return "Good evening";
}

export function shiftFor(date: Date): string {
  const h = date.getHours();
  if (h >= 7 && h < 15) return "Day shift";
  if (h >= 15 && h < 23) return "Evening shift";
  return "Night shift";
}

export function initialsFor(label: string): string {
  const parts = label.split(" ").filter((p) => /^[A-Za-z]/.test(p));
  const first = parts.length > 0 ? parts[0].charAt(0) : "S";
  const second = parts.length > 1 ? parts[parts.length - 1].charAt(0) : "";
  return (first + second).toUpperCase();
}

function toArray<T>(value: unknown): T[] {
  return Array.isArray(value) ? (value as T[]) : [];
}

function matches(text: string, query: string): boolean {
  return text.toLowerCase().includes(query);
}
export function SearchPalette({ auth, onClose, onSelect }: { auth: Auth; onClose: () => void; onSelect: (hit: SearchHit) => void }) {
  const [query, setQuery] = useState("");
  const [hits, setHits] = useState<SearchHit[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [selected, setSelected] = useState(0);
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    const trimmed = query.trim();
    if (trimmed.length < 2) {
      setHits([]);
      setError("");
      setLoading(false);
      return;
    }
    let cancelled = false;
    setLoading(true);
    const timer = window.setTimeout(async () => {
      try {
        const q = trimmed.toLowerCase();
        const found: SearchHit[] = []; 
        if (isHr(auth.role)) {
          const employees = toArray<Employee>(await searchEmployees(auth, ""));
          for (const e of employees) {
            if (matches(e.fullName, q) || matches(e.employeeCode, q)) {
              found.push({ kind: "Employee", id: e.id, title: e.fullName, detail: e.employeeCode + " - " + e.status });
            }
          }
          const departments = toArray<Department>(await listDepartments(auth));
          for (const d of departments) {
            if (matches(d.displayName, q) || matches(d.departmentCode, q)) {
              found.push({ kind: "Department", id: d.id, title: d.displayName, detail: d.departmentCode });
            }
          }
        }
        if (isClinical(auth.role)) {
          const encounters = toArray<Encounter>(await listEncounters(auth));
          for (const e of encounters) {
            if (matches(e.patientId, q) || matches(e.id, q) || matches(e.status, q)) {
              found.push({ kind: "Encounter", id: e.id, title: "Encounter " + e.id.slice(0, 8), detail: "Patient " + e.patientId.slice(0, 8) + " - " + e.status });
            }
          }
        }
        if (cancelled === false) {
          setHits(found.slice(0, 8));
          setSelected(0);
          setError("");
        }
      } catch (err) {
        if (cancelled === false) setError(err instanceof Error ? err.message : "Search failed");
      } finally {
        if (cancelled === false) setLoading(false);
      }
    }, 250);
    return () => { cancelled = true; window.clearTimeout(timer); };
  }, [query, auth]);

  useEffect(() => { inputRef.current?.focus(); }, []);

  const openHit = (hit: SearchHit) => { onSelect(hit); onClose(); };

  return (
    <div className="palette-overlay" role="presentation" onClick={onClose}>
      <div className="palette" role="dialog" aria-modal="true" aria-label="Global search" onClick={(e) => e.stopPropagation()}>
        <input ref={inputRef} className="palette-input" type="search" placeholder="Search patients, employees, departments" aria-label="Search patients, employees, departments" value={query} onChange={(e) => setQuery(e.target.value)} onKeyDown={(e) => { if (e.key === "Escape") onClose(); if (e.key === "ArrowDown") { e.preventDefault(); setSelected((s) => Math.min(s + 1, hits.length - 1)); } if (e.key === "ArrowUp") { e.preventDefault(); setSelected((s) => Math.max(s - 1, 0)); } if (e.key === "Enter" && hits.length > 0) openHit(hits[selected] ?? hits[0]); }} />
        {loading && <p className="palette-hint">Searching real records</p>}
        {error !== "" && <p className="palette-hint" role="alert">{error}</p>}
        {query.trim().length >= 2 && loading === false && hits.length === 0 && error === "" && (<p className="palette-hint">No matches in the records you can access.</p>)}
        {query.trim().length < 2 && (<p className="palette-hint">Type at least 2 characters. Encounters are matched by patient and encounter ID; employees by name or code; departments by name or code.</p>)}
        <ul className="palette-list">
          {hits.map((hit, index) => (
            <li key={hit.kind + "-" + hit.id}>
              <button type="button" className={"palette-item" + (index === selected ? " selected" : "")} onClick={() => openHit(hit)} onMouseEnter={() => setSelected(index)}>
                <span className="palette-kind">{hit.kind}</span>
                <span><strong>{hit.title}</strong><br /><span className="muted">{hit.detail}</span></span>
              </button>
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}
export function AppShell({ auth, nav, bottomNav, pendingDecisions, onOpenPendingDecisions, onSearchSelect, onSignOut, fab, children }: AppShellProps) {
  const [now, setNow] = useState(() => new Date());
  const [paletteOpen, setPaletteOpen] = useState(false);
  const [sidebarOpen, setSidebarOpen] = useState(false);

  useEffect(() => {
    const timer = window.setInterval(() => setNow(new Date()), 1000);
    return () => window.clearInterval(timer);
  }, []);

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === "k") {
        e.preventDefault();
        setPaletteOpen((open) => !open);
      }
      if (e.key === "Escape") setPaletteOpen(false);
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, []);

  const groups: string[] = [];
  for (const entry of nav) { if (groups.includes(entry.group) === false) groups.push(entry.group); }
  const clock = now.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit", second: "2-digit" });
  const dateLine = now.toLocaleDateString([], { weekday: "long", day: "numeric", month: "long" });

  return (
    <div className="app-shell">
      <aside className={"sidebar" + (sidebarOpen ? " open" : "")} aria-label="Primary">
        <div className="sidebar-brand">
          <span className="sidebar-brand-mark" aria-hidden="true">+</span>
          <span><span className="sidebar-brand-name">SmartHMS</span><br /><span className="sidebar-brand-sub">Hospital Management</span></span>
        </div>
        <div className="sidebar-status">
          <span className="status-dot" aria-hidden="true"></span>
          <span>On duty - {shiftFor(now)}</span>
          <span className="sidebar-clock">{clock}</span>
        </div>
        <nav className="sidebar-nav" aria-label="Sections">
          {groups.map((group) => (
            <div className="nav-group" key={group}>
              <div className="nav-group-label">{group}</div>
              {nav.filter((entry) => entry.group === group).map((entry) => (
                <button key={entry.id} type="button" className={"nav-item" + (entry.active ? " active" : "")} aria-current={entry.active ? "page" : undefined} onClick={() => { entry.onSelect(); setSidebarOpen(false); }}>
                  <span className="nav-item-icon" aria-hidden="true">{entry.icon}</span>
                  <span>{entry.label}</span>
                  {entry.badge !== undefined && entry.badge > 0 && (<span className="nav-item-badge">{entry.badge}</span>)}
                </button>
              ))}
            </div>
          ))}
        </nav>
        <div className="sidebar-user">
          <div className="sidebar-user-row1">
            <span className="avatar" aria-hidden="true">{initialsFor(auth.label)}</span>
            <span className="sidebar-user-name">{auth.label}</span>
          </div>
          <div className="sidebar-user-row2">
            <span>{auth.role}</span>
            <span className="status-dot" aria-hidden="true"></span>
            <span>On duty</span>
        <button type="button" className="sidebar-user-signout" onClick={onSignOut}>Sign out</button>
          </div>
        </div>
      </aside>
      <div className="app-main">
        <header className="topbar">
          <button type="button" className="icon-btn menu-btn" aria-label="Open navigation" onClick={() => setSidebarOpen(true)}>&#9776;</button>
      <div className="topbar-text">
            <p className="topbar-greeting">{greetingFor(now)}, {auth.label}</p>
            <p className="topbar-sub">{dateLine} - {shiftFor(now)}</p>
          </div>
          <div className="topbar-spacer"></div>
          <button type="button" className="search-trigger" onClick={() => setPaletteOpen(true)} aria-label="Search records"><span aria-hidden="true">&#128269;</span><span className="search-label">Search records</span><kbd>Ctrl K</kbd></button>
          {isHr(auth.role) && (
            <button type="button" className="icon-btn" aria-label={pendingDecisions > 0 ? "Notifications, " + pendingDecisions + " pending decisions" : "Notifications"} onClick={onOpenPendingDecisions}>
              <span aria-hidden="true">&#128276;</span>
              {pendingDecisions > 0 && (<span className="icon-btn-badge">{pendingDecisions}</span>)}
            </button>
          )}
        </header>
        <div className="content">{children}</div>
        <nav className="bottom-nav" aria-label="Primary mobile">
          {bottomNav.map((entry) => (
            <button key={entry.id} type="button" className={"bottom-nav-item" + (entry.active ? " active" : "")} aria-current={entry.active ? "page" : undefined} onClick={entry.onSelect}>
              <span className="bottom-nav-icon" aria-hidden="true">{entry.icon}</span>
              <span>{entry.label}</span>
            </button>
          ))}
        </nav>
        {fab !== null && (<button type="button" className="fab" aria-label={fab.label} onClick={fab.onClick}>+</button>)}
      </div>
      {paletteOpen && (<SearchPalette auth={auth} onClose={() => setPaletteOpen(false)} onSelect={onSearchSelect} />)}
    </div>
  );
}
