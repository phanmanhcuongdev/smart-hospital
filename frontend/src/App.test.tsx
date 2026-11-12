import "@testing-library/jest-dom/vitest";
import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import App from "./App";

const HR_AUTH = "Basic " + btoa("demo-hr:demo");
const ADMIN_AUTH = "Basic " + btoa("demo-admin:demo");

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
        return auth === HR_AUTH || auth === ADMIN_AUTH
          ? body(200, [])
          : body(403, { code: "FORBIDDEN", message: "Forbidden" });
      }
      if (u.includes("/api/v1/hr/employees")) {
        return body(200, []);
      }
      if (u.includes("/api/v1/emr/encounters")) {
        return body(200, { items: [], total: 0 });
      }
      if (u.includes("/api/v1/emr/patients")) {
        return body(200, []);
      }
      if (u.includes("/api/v1/emr/practitioners")) {
        return body(200, []);
      }
      if (u.includes("/api/v1/emr/departments")) {
        return body(200, []);
      }
      return body(200, {});
    }),
  );
}

describe("App", () => {
  afterEach(() => {
    cleanup();
    sessionStorage.clear();
    window.location.hash = "";
  });

  it("signs in an HR user and shows the employee list", async () => {
    stubFetch();
    render(<App />);
    fireEvent.change(screen.getByLabelText("Người dùng demo"), { target: { value: "demo-hr" } });
    fireEvent.click(screen.getByRole("button", { name: "Đăng nhập" }));
    expect(await screen.findByRole("heading", { name: "Nhân viên" })).toBeInTheDocument();
    expect(await screen.findByText("Không có nhân viên nào khớp với tìm kiếm.")).toBeInTheDocument();
    expect(screen.queryByText("Khám bệnh & Bệnh án")).not.toBeInTheDocument();
  });

  it("signs in a Doctor and shows the EMR encounter list", async () => {
    stubFetch();
    render(<App />);
    fireEvent.change(screen.getByLabelText("Người dùng demo"), { target: { value: "EMP-DEMO-001" } });
    fireEvent.click(screen.getByRole("button", { name: "Đăng nhập" }));
    expect((await screen.findAllByText("Khám bệnh & Bệnh án")).length).toBeGreaterThan(0);
    expect(screen.queryByText("Danh sách nhân viên")).not.toBeInTheDocument();
    expect(await screen.findByRole("heading", { name: "Tạo lần khám" })).toBeInTheDocument();
    expect(await screen.findByRole("heading", { name: "Danh sách lần khám" })).toBeInTheDocument();
  });

  it("signs in an Admin and allows switching between HR and EMR", async () => {
    stubFetch();
    render(<App />);
    fireEvent.change(screen.getByLabelText("Người dùng demo"), { target: { value: "demo-admin" } });
    fireEvent.click(screen.getByRole("button", { name: "Đăng nhập" }));

    // Both menus should be visible in the sidebar/bottom-nav
    expect((await screen.findAllByText("Khám bệnh & Bệnh án")).length).toBeGreaterThan(0);
    expect(screen.getAllByText("Danh sách nhân viên").length).toBeGreaterThan(0);

    // Switch to HR employees
    const hrBtn = screen.getAllByRole("button", { name: /Danh sách nhân viên/i })[0];
    fireEvent.click(hrBtn);
    expect(await screen.findByRole("heading", { name: "Nhân viên" })).toBeInTheDocument();

    // Switch to EMR
    const emrBtn = screen.getAllByRole("button", { name: /Khám bệnh & Bệnh án/i })[0];
    fireEvent.click(emrBtn);
    expect(await screen.findByRole("heading", { name: "Tạo lần khám" })).toBeInTheDocument();
    expect(await screen.findByRole("heading", { name: "Danh sách lần khám" })).toBeInTheDocument();
  });
});
