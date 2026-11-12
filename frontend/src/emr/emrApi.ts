import type {
  Addendum,
  DepartmentSummary,
  Diagnosis,
  Encounter,
  EncounterStatus,
  MedicalOrder,
  MedicalRecord,
  MedicationSummary,
  Observation,
  OrderStatus,
  OrderType,
  Page,
  PatientSummary,
  PractitionerSummary,
  Prescription,
  RecordVersion,
  UserSummary,
} from "./types";

const base: string = import.meta.env.VITE_API_BASE_URL ?? "";

export class ApiRequestError extends Error {
  status: number;
  code: string | undefined;

  constructor(status: number, message: string, code?: string) {
    super(message);
    this.status = status;
    this.code = code;
  }
}

export function errorMessage(error: unknown): string {
  return error instanceof Error
    ? error.message
    : "Đã xảy ra lỗi không xác định.";
}

function getAuthHeaders(): Record<string, string> {
  try {
    const raw = sessionStorage.getItem("smarthms-auth");
    if (raw) {
      const parsed = JSON.parse(raw);
      if (parsed?.username && parsed?.password) {
        return {
          Authorization: "Basic " + btoa(`${parsed.username}:${parsed.password}`),
        };
      }
    }
  } catch {
    // ignore json parse error
  }
  return {};
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`${base}/api/v1/emr${path}`, {
      ...init,
      headers: {
        "Content-Type": "application/json",
        ...getAuthHeaders(),
        ...init?.headers,
      },
    });
  } catch {
    throw new ApiRequestError(
      0,
      "Không kết nối được backend. Kiểm tra Docker Compose.",
    );
  }
  if (!response.ok) {
    let message = `Lỗi ${response.status}`;
    let code: string | undefined;
    try {
      const body = (await response.json()) as {
        message?: string;
        code?: string;
      };
      if (body.message) message = body.message;
      code = body.code;
    } catch {
      // phản hồi lỗi không phải JSON: giữ thông báo mặc định
    }
    throw new ApiRequestError(response.status, message, code);
  }
  return (await response.json()) as T;
}

export const emrApi = {
  listEncounters(params: {
    patientId?: string;
    status?: EncounterStatus | "";
    departmentId?: string | "";
    page: number;
    size: number;
  }) {
    const query = new URLSearchParams({
      page: String(params.page),
      size: String(params.size),
    });
    if (params.status) query.set("status", params.status);
    if (params.patientId) query.set("patientId", params.patientId);
    if (params.departmentId) query.set("departmentId", params.departmentId);
    return request<Page<Encounter>>(`/encounters?${query.toString()}`);
  },

  createEncounter(body: {
    patientId: string;
    attendingEmployeeId: string;
    departmentId?: string | null;
    appointmentId?: string | null;
  }) {
    return request<Encounter>("/encounters", {
      method: "POST",
      body: JSON.stringify(body),
    });
  },

  getEncounter(id: string) {
    return request<Encounter>(`/encounters/${id}`);
  },

  changeStatus(
    id: string,
    status: EncounterStatus,
    attendingEmployeeId?: string | null,
    departmentId?: string | null,
  ) {
    return request<Encounter>(`/encounters/${id}/status`, {
      method: "PATCH",
      body: JSON.stringify({ status, attendingEmployeeId, departmentId }),
    });
  },

  searchPatients(q: string) {
    return request<PatientSummary[]>(`/patients?q=${encodeURIComponent(q)}`);
  },

  listPractitioners() {
    return request<PractitionerSummary[]>("/practitioners");
  },

  listDepartments() {
    return request<DepartmentSummary[]>("/departments");
  },

  listMedications() {
    return request<MedicationSummary[]>("/medications");
  },

  listUsers() {
    return request<UserSummary[]>("/users");
  },

  listObservations(encounterId: string) {
    return request<Observation[]>(`/encounters/${encounterId}/observations`);
  },

  createObservation(
    encounterId: string,
    body: {
      metricCode: string;
      numericValue?: number;
      textValue?: string;
      unitCode?: string;
    },
  ) {
    return request<Observation>(`/encounters/${encounterId}/observations`, {
      method: "POST",
      body: JSON.stringify(body),
    });
  },

  listDiagnoses(encounterId: string) {
    return request<Diagnosis[]>(`/encounters/${encounterId}/diagnoses`);
  },

  createDiagnosis(
    encounterId: string,
    body: {
      diagnosisType: "PRIMARY" | "SECONDARY";
      icd10Code?: string;
      narrative: string;
    },
  ) {
    return request<Diagnosis>(`/encounters/${encounterId}/diagnoses`, {
      method: "POST",
      body: JSON.stringify(body),
    });
  },

  confirmDiagnosis(encounterId: string, diagnosisId: string) {
    return request<Diagnosis>(
      `/encounters/${encounterId}/diagnoses/${diagnosisId}/confirm`,
      {
        method: "PATCH",
      },
    );
  },

  listOrders(encounterId: string) {
    return request<MedicalOrder[]>(`/encounters/${encounterId}/orders`);
  },

  createOrder(
    encounterId: string,
    body: { orderType: OrderType; externalOrderRef?: string },
  ) {
    return request<MedicalOrder>(`/encounters/${encounterId}/orders`, {
      method: "POST",
      body: JSON.stringify(body),
    });
  },

  changeOrderStatus(encounterId: string, orderId: string, status: OrderStatus) {
    return request<MedicalOrder>(
      `/encounters/${encounterId}/orders/${orderId}/status`,
      {
        method: "PATCH",
        body: JSON.stringify({ status }),
      },
    );
  },

  listPrescriptions(encounterId: string) {
    return request<Prescription[]>(`/encounters/${encounterId}/prescriptions`);
  },

  createPrescription(encounterId: string) {
    return request<Prescription>(`/encounters/${encounterId}/prescriptions`, {
      method: "POST",
    });
  },

  addPrescriptionItem(
    encounterId: string,
    prescriptionId: string,
    body: {
      medicationId: string;
      dose: string;
      route: string;
      frequency: string;
      daysCount: number;
      quantity: number;
      unitCode: string;
    },
  ) {
    return request<Prescription>(
      `/encounters/${encounterId}/prescriptions/${prescriptionId}/items`,
      {
        method: "POST",
        body: JSON.stringify(body),
      },
    );
  },

  issuePrescription(encounterId: string, prescriptionId: string) {
    return request<Prescription>(
      `/encounters/${encounterId}/prescriptions/${prescriptionId}/issue`,
      {
        method: "POST",
      },
    );
  },

  cancelPrescription(encounterId: string, prescriptionId: string) {
    return request<Prescription>(
      `/encounters/${encounterId}/prescriptions/${prescriptionId}/cancel`,
      {
        method: "POST",
      },
    );
  },

  getRecord(encounterId: string) {
    return request<MedicalRecord>(`/encounters/${encounterId}/record`);
  },

  listVersions(encounterId: string) {
    return request<RecordVersion[]>(
      `/encounters/${encounterId}/record/versions`,
    );
  },

  getVersion(encounterId: string, versionNo: number) {
    return request<RecordVersion>(
      `/encounters/${encounterId}/record/versions/${versionNo}`,
    );
  },

  createVersion(encounterId: string) {
    return request<RecordVersion>(
      `/encounters/${encounterId}/record/versions`,
      { method: "POST" },
    );
  },

  submitRecord(encounterId: string) {
    return request<MedicalRecord>(`/encounters/${encounterId}/record/submit`, {
      method: "POST",
    });
  },

  lockRecord(encounterId: string, userId: string) {
    return request<MedicalRecord>(`/encounters/${encounterId}/record/lock`, {
      method: "POST",
      body: JSON.stringify({ userId }),
    });
  },

  listAddenda(encounterId: string) {
    return request<Addendum[]>(`/encounters/${encounterId}/record/addenda`);
  },

  createAddendum(
    encounterId: string,
    body: { reason: string; content: string },
  ) {
    return request<Addendum>(`/encounters/${encounterId}/record/addenda`, {
      method: "POST",
      body: JSON.stringify(body),
    });
  },

  confirmAddendum(encounterId: string, addendumId: string, userId: string) {
    return request<Addendum>(
      `/encounters/${encounterId}/record/addenda/${addendumId}/confirm`,
      {
        method: "POST",
        body: JSON.stringify({ userId }),
      },
    );
  },
};
