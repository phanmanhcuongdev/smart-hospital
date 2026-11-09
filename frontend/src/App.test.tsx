import '@testing-library/jest-dom/vitest';
import { render, screen } from '@testing-library/react';
import { expect, test, vi } from 'vitest';
import { App } from './App';

test('shows backend health and auth console', async () => {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
    ok: true,
    status: 200,
    headers: new Headers({ 'X-Correlation-Id': 'test-cid' }),
    json: async () => ({ status: 'UP', timestamp: '2026-11-07T00:00:00Z' }),
    text: async () => JSON.stringify({ status: 'UP', timestamp: '2026-11-07T00:00:00Z' })
  }));
  render(<App />);
  expect(await screen.findByText('Backend status: UP')).toBeInTheDocument();
  expect(screen.getByText('1. Đăng nhập (Authentication)')).toBeInTheDocument();
  expect(screen.getByText('POST /api/v1/auth/login')).toBeInTheDocument();
});
