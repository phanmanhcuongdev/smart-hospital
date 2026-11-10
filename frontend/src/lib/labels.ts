// Vietnamese-only display labels. Language switching was removed.
export const VI_LOCALE = "vi-VN";

const STATUS_LABELS: Record<string, string> = {
  WAITING: "Đang chờ",
  IN_PROGRESS: "Đang khám",
  COMPLETED: "Đã hoàn thành",
  CANCELLED: "Đã hủy",
  NO_SHOW: "Không đến khám",
  ACTIVE: "Đang hoạt động",
  ONBOARDING: "Đang nhận việc",
  SUSPENDED: "Tạm đình chỉ",
  TERMINATED: "Đã nghỉ việc",
  VALID: "Còn hiệu lực",
  EXPIRING_SOON: "Sắp hết hạn",
  EXPIRED: "Đã hết hạn",
  UNVERIFIED: "Chưa xác minh",
  VERIFIED: "Đã xác minh",
  REJECTED: "Đã từ chối",
  PENDING: "Đang chờ",
  APPROVED: "Đã duyệt",
  CONFIRMED: "Đã xác nhận",
  FEMALE: "Nữ",
  MALE: "Nam",
  OTHER: "Khác",
  DRAFT: "Bản nháp",
  LOCKED: "Đã khóa",
  CCHN: "CCHN",
  OPEN: "Đang mở",
  CLOSED: "Đã đóng",
  APPLIED: "Đã áp dụng",
  QUARANTINED: "Đã cách ly",
  REVOKED: "Đã thu hồi",
  FAILED: "Thất bại",
  HR_SPECIALIST: "Chuyên viên nhân sự",
  HR_APPROVER: "Người phê duyệt nhân sự",
  NORMAL: "Bình thường",
  ABNORMAL: "Bất thường",
  ACTIVATE: "Kích hoạt",
  SUSPEND: "Tạm đình chỉ",
  RETURN: "Trở lại làm việc",
  TERMINATE: "Chấm dứt",
  TRANSFER: "Điều chuyển",
};

export function statusLabel(value: string | null | undefined): string {
  if (value == null || value === "") return value ?? "";
  return STATUS_LABELS[value] ?? value;
}

export function greetingFor(date: Date): string {
  const hour = date.getHours();
  if (hour < 5) return "Chào buổi đêm";
  if (hour < 12) return "Chào buổi sáng";
  if (hour < 18) return "Chào buổi chiều";
  return "Chào buổi tối";
}

export function shiftFor(date: Date): string {
  const hour = date.getHours();
  if (hour >= 7 && hour < 15) return "Ca ngày";
  if (hour >= 15 && hour < 23) return "Ca chiều";
  return "Ca đêm";
}
