import type {
  Addendum,
  ClinicalResult,
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
  ResultIngest,
  UserSummary,
  PrescriptionLineInput,
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

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`${base}/api/v1/emr${path}`, {
      ...init,
      headers: { "Content-Type": "application/json" },
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
    page: number;
    size: number;
  }) {
    const query = new URLSearchParams({
      page: String(params.page),
      size: String(params.size),
    });
    if (params.status) query.set("status", params.status);
    if (params.patientId) query.set("patientId", params.patientId);
    return request<Page<Encounter>>(`/encounters?${query.toString()}`);
  },

  createEncounter(body: { patientId: string; attendingEmployeeId: string }) {
    return request<Encounter>("/encounters", {
      method: "POST",
      body: JSON.stringify(body),
    });
  },

  getEncounter(id: string) {
    return request<Encounter>(`/encounters/${id}`);
  },

  changeStatus(id: string, status: EncounterStatus) {
    return request<Encounter>(`/encounters/${id}/status`, {
      method: "PATCH",
      body: JSON.stringify({ status }),
    });
  },

  searchPatients(q: string) {
    return request<PatientSummary[]>(`/patients?q=${encodeURIComponent(q)}`);
  },

  listPractitioners() {
    return request<PractitionerSummary[]>("/practitioners");
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

  createPrescription(encounterId: string, items: PrescriptionLineInput[]) {
    return request<Prescription>(`/encounters/${encounterId}/prescriptions`, {
      method: "POST",
      body: JSON.stringify({ items }),
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

  listAddenda(encounterId: string) {
    return request<Addendum[]>(`/encounters/${encounterId}/record/addenda`);
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

  listResults(orderId: string) {
    return request<ClinicalResult[]>(`/orders/${orderId}/results`);
  },

  submitMockResult(
    orderId: string,
    body: {
      sourceSystem: string;
      sourceResultId: string;
      sourceVersion: string;
      patientId: string;
      payload: string;
      abnormalFlag?: boolean;
    },
  ) {
    return request<ResultIngest>(`/orders/${orderId}/results`, {
      method: "POST",
      body: JSON.stringify(body),
    });
  },

  createVersion(encounterId: string, expectedVersion: number) {
    return request<RecordVersion>(
      `/encounters/${encounterId}/record/versions`,
      {
        method: "POST",
        body: JSON.stringify({ expectedVersion }),
      },
    );
  },

  submitRecord(encounterId: string, expectedVersion: number) {
    return request<MedicalRecord>(`/encounters/${encounterId}/record/submit`, {
      method: "POST",
      body: JSON.stringify({ expectedVersion }),
    });
  },

  returnRecord(encounterId: string, expectedVersion: number) {
    return request<MedicalRecord>(`/encounters/${encounterId}/record/return`, {
      method: "POST",
      body: JSON.stringify({ expectedVersion }),
    });
  },

  lockRecord(encounterId: string, userId: string, expectedVersion: number) {
    return request<MedicalRecord>(`/encounters/${encounterId}/record/lock`, {
      method: "POST",
      body: JSON.stringify({ userId, expectedVersion }),
    });
  },

  createAddendum(
    encounterId: string,
    body: { reason: string; content: string; expectedVersion: number },
  ) {
    return request<Addendum>(`/encounters/${encounterId}/record/addenda`, {
      method: "POST",
      body: JSON.stringify(body),
    });
  },
};
