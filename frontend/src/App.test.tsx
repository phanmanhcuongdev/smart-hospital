import "@testing-library/jest-dom/vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import App from "./App";

const HR_AUTH = "Basic " + btoa("demo-hr:demo");

function stubFetch() {
  vi.stubGlobal(
    "fetch",
    vi.fn(async (url: unknown, init?: { headers?: Record<string, string> }) => {
      const u = String(url);
      const auth = init?.headers?.Authorization ?? "";
      const body = (status: number, payload: unknown) => ({
        ok: status >= 200 && status < 300,
        status,
        text: async () => JSON.stringify(payload),
        json: async () => payload,
      });
      if (u.includes("/api/v1/system/health")) {
        return body(200, { status: "UP" });
      }
      if (u.includes("/api/v1/hr/departments")) {
        return auth === HR_AUTH ? body(200, []) : body(403, { code: "FORBIDDEN", message: "Forbidden" });
      }
      if (u.includes("/api/v1/emr/encounters")) {
        return body(200, []);
      }
      if (u.includes("/api/v1/hr/employees")) {
        return body(200, []);
      }
      return body(200, {});
    }),
  );
}

describe("App", () => {
  it("shows the demo login, signs in a doctor, and lists encounters", async () => {
    stubFetch();
    sessionStorage.clear();
    render(<App />);
    expect(screen.getByText("Đăng nhập (demo)")).toBeInTheDocument();
    expect(await screen.findByText("Trạng thái backend: UP")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Đăng nhập" }));
    expect(await screen.findByText("Lượt khám (SCR-EMR-01)")).toBeInTheDocument();
    expect(await screen.findByText("Chưa có lượt khám nào.")).toBeInTheDocument();
  });

  it("signs in an HR user and shows the employee list", async () => {
    stubFetch();
    sessionStorage.clear();
    render(<App />);
    fireEvent.change(screen.getByLabelText("Người dùng demo"), { target: { value: "demo-hr" } });
    fireEvent.click(screen.getByRole("button", { name: "Đăng nhập" }));
    expect(await screen.findByRole("heading", { name: "Nhân viên" })).toBeInTheDocument();
    expect(await screen.findByText("Không có nhân viên nào khớp với tìm kiếm.")).toBeInTheDocument();
  });
});
