export type EncounterStatus =
  | "WAITING"
  | "IN_PROGRESS"
  | "PENDING_LAB_RAD"
  | "DIAGNOSED"
  | "COMPLETED"
  | "CANCELLED"
  | "NO_SHOW";

export const STATUS_LABEL: Record<EncounterStatus, string> = {
  WAITING: "Chờ khám",
  IN_PROGRESS: "Đang khám",
  PENDING_LAB_RAD: "Chờ cận lâm sàng",
  DIAGNOSED: "Đã chẩn đoán",
  COMPLETED: "Hoàn tất",
  CANCELLED: "Đã hủy",
  NO_SHOW: "Không đến",
};

export const ENCOUNTER_STATUSES = Object.keys(
  STATUS_LABEL,
) as EncounterStatus[];

export interface Encounter {
  encounterId: string;
  patientId: string;
  appointmentId: string | null;
  attendingEmployeeId: string;
  departmentId?: string | null;
  status: EncounterStatus;
  startedAt: string | null;
  completedAt: string | null;
  createdAt: string;
}

export interface Page<T> {
  items: T[];
  page: number;
  size: number;
  total: number;
}

export interface DepartmentSummary {
  departmentId: string;
  departmentCode: string;
  displayName: string;
  departmentType: string | null;
  active: boolean;
}

export interface PatientSummary {
  patientId: string;
  displayName: string;
  identityStatus: string;
}

export interface PractitionerSummary {
  employeeId: string;
  employeeCode: string;
  fullName: string;
  status?: string;
  active?: boolean;
  departmentName?: string | null;
  hasVerifiedLicense?: boolean;
}
export interface Diagnosis {
  diagnosisId: string;
  encounterId: string;
  diagnosisType: "PRIMARY" | "SECONDARY";
  icd10Code: string | null;
  narrative: string;
  diagnosisStatus: "PROVISIONAL" | "CONFIRMED";
  confirmedBy: string | null;
  createdAt: string;
}

export interface Observation {
  observationId: string;
  encounterId: string;
  metricCode: string;
  numericValue: number | null;
  textValue: string | null;
  unitCode: string | null;
  observedAt: string;
  sourceType: string;
  sourceRef: string;
  quality: string;
}

/** Chỉ để bật/tắt nút; backend mới là nơi quyết định. Giả định, cần đối chiếu IV.7. */
export const NEXT_STATUS: Record<EncounterStatus, EncounterStatus[]> = {
  WAITING: ["IN_PROGRESS", "CANCELLED", "NO_SHOW"],
  IN_PROGRESS: ["PENDING_LAB_RAD", "DIAGNOSED", "CANCELLED"],
  PENDING_LAB_RAD: ["IN_PROGRESS", "DIAGNOSED"],
  DIAGNOSED: ["COMPLETED"],
  COMPLETED: [],
  CANCELLED: [],
  NO_SHOW: [],
};
export type OrderType = "LAB" | "IMAGING" | "OTHER";
export type OrderStatus =
  | "DRAFT"
  | "ORDERED"
  | "ACKNOWLEDGED"
  | "IN_PROGRESS"
  | "COMPLETED"
  | "CANCELLED";

export const ORDER_TYPE_LABEL: Record<OrderType, string> = {
  LAB: "Xét nghiệm",
  IMAGING: "Chẩn đoán hình ảnh",
  OTHER: "Khác",
};

export const ORDER_STATUS_LABEL: Record<OrderStatus, string> = {
  DRAFT: "Nháp",
  ORDERED: "Đã chỉ định",
  ACKNOWLEDGED: "Đã tiếp nhận",
  IN_PROGRESS: "Đang thực hiện",
  COMPLETED: "Hoàn thành",
  CANCELLED: "Đã hủy",
};

/** Chỉ để bật/tắt nút; backend mới là nơi quyết định. Giả định, cần đối chiếu IV.7. */
export const NEXT_ORDER_STATUS: Record<OrderStatus, OrderStatus[]> = {
  DRAFT: ["ORDERED", "CANCELLED"],
  ORDERED: ["ACKNOWLEDGED", "CANCELLED"],
  ACKNOWLEDGED: ["IN_PROGRESS", "CANCELLED"],
  IN_PROGRESS: ["COMPLETED"],
  COMPLETED: [],
  CANCELLED: [],
};

export interface MedicalOrder {
  orderId: string;
  encounterId: string;
  orderedBy: string;
  orderType: OrderType;
  orderStatus: OrderStatus;
  externalOrderRef: string | null;
  orderedAt: string | null;
}

export type PrescriptionStatus = "DRAFT" | "ORDERED" | "CANCELLED";

export const PRESCRIPTION_STATUS_LABEL: Record<PrescriptionStatus, string> = {
  DRAFT: "Nháp",
  ORDERED: "Đã phát hành",
  CANCELLED: "Đã hủy",
};

export interface PrescriptionItem {
  lineNo: number;
  medicationId: string;
  medicationLabel: string;
  dose: string;
  route: string;
  frequency: string;
  daysCount: number;
  quantity: number;
  unitCode: string;
}

export interface Prescription {
  prescriptionId: string;
  encounterId: string;
  prescriberId: string;
  status: PrescriptionStatus;
  issuedAt: string | null;
  items: PrescriptionItem[];
}

export interface MedicationSummary {
  medicationId: string;
  displayName: string;
}
export type RecordStatus =
  | "DRAFT"
  | "PENDING_SIGNATURE"
  | "LOCKED"
  | "ARCHIVED";

export const RECORD_STATUS_LABEL: Record<RecordStatus, string> = {
  DRAFT: "Đang soạn",
  PENDING_SIGNATURE: "Chờ ký",
  LOCKED: "Đã khóa",
  ARCHIVED: "Đã lưu trữ",
};

export interface MedicalRecord {
  recordId: string;
  encounterId: string;
  recordStatus: RecordStatus;
  currentVersion: number;
  lockedAt: string | null;
  createdAt: string;
}

export interface RecordVersion {
  versionNo: number;
  sha256Hex: string | null;
  versionCreatedAt: string;
  payload: string | null;
}

export interface Addendum {
  addendumId: string;
  recordId: string;
  reason: string;
  content: string;
  confirmationStatus: "DRAFT" | "CONFIRMED";
  confirmedAt: string | null;
  confirmedBy: string | null;
  createdAt: string;
}

export interface UserSummary {
  userId: string;
  username: string;
}
