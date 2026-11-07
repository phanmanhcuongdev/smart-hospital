import { useEffect, useState } from 'react';
type Health = { status: string; timestamp: string };
const healthUrl = import.meta.env.VITE_API_BASE_URL ? `${import.meta.env.VITE_API_BASE_URL}/api/v1/system/health` : '/api/v1/system/health';
export function App() {
  const [state, setState] = useState<'loading' | 'ready' | 'error'>('loading');
  const [health, setHealth] = useState<Health | null>(null);
  useEffect(() => { fetch(healthUrl).then(async response => { if (!response.ok) throw new Error('Health check failed'); return response.json() as Promise<Health>; }).then(value => { setHealth(value); setState('ready'); }).catch(() => setState('error')); }, []);
  return <main><h1>SmartHMS</h1><p>V.1 foundation — EMR, HR, and common infrastructure.</p>{state === 'loading' && <p role="status">Checking system health…</p>}{state === 'ready' && <p role="status">Backend status: {health?.status}</p>}{state === 'error' && <p role="alert">Backend is unavailable. Check Docker Compose logs.</p>}</main>;
}
