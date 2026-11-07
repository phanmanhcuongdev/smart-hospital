import '@testing-library/jest-dom/vitest';
import { render, screen } from '@testing-library/react';
import { expect, test, vi } from 'vitest';
import { App } from './App';

test('shows backend health', async () => {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
    ok: true,
    json: async () => ({ status: 'UP', timestamp: '2026-11-07T00:00:00Z' })
  }));
  render(<App />);
  expect(await screen.findByText('Backend status: UP')).toBeInTheDocument();
});
