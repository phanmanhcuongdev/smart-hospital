import { createContext, useContext, useEffect, useState, type ReactNode } from "react";

export type Language = "vi" | "en";

const STORAGE_KEY = "smarthms.language";
const DEFAULT_LANGUAGE: Language = "vi";

export function getInitialLanguage(): Language {
  try {
    const saved = window.localStorage.getItem(STORAGE_KEY);
    if (saved === "vi" || saved === "en") return saved;
  } catch {
    // storage unavailable
  }
  return DEFAULT_LANGUAGE;
}

let current: Language = getInitialLanguage();
const listeners = new Set<() => void>();

export function currentLanguage(): Language {
  return current;
}

export function currentLocale(): string {
  return current === "vi" ? "vi-VN" : "en-US";
}

export function setLanguage(lang: Language): void {
  if (lang !== "vi" && lang !== "en") return;
  current = lang;
  try {
    window.localStorage.setItem(STORAGE_KEY, lang);
  } catch {
    // storage unavailable
  }
  if (typeof document !== "undefined") {
    document.documentElement.lang = lang;
  }
  listeners.forEach((fn) => fn());
}

// Vietnamese translations keyed by the English source string.
// English is the source language, so only Vietnamese entries are listed here.
const vi: Record<string, string> = {
    "A department must exist and be active before an employee can be assigned to it.": "Phòng ban phải tồn tại và đang hoạt động thì nhân viên mới được phân công về đó.",
  "A transfer closes the current assignment and opens a new one. Overlapping assignments are rejected. Assignment history is never overwritten.": "Một lần chuyển đổi sẽ đóng phân công hiện tại và mở phân công mới. Các phân công trùng thời gian bị từ chối. Lịch sử phân công không bao giờ bị ghi đè.",
  "ACTIVE": "Đang hoạt động",
  "Abnormal flag (true or false, optional)": "Cờ bất thường (true hoặc false, tùy chọn)",
  "Abnormal flag": "Cờ bất thường",
  "Abnormal": "Bất thường",
  "Actions": "Thao tác",
  "Active": "Đang hoạt động",
  "Add addendum": "Thêm phụ lục",
  "Addenda (SCR-EMR-09)": "Phụ lục (SCR-EMR-09)",
  "Addendum": "Phụ lục",
  "Address": "Địa chỉ",
  "All departments": "Tất cả phòng ban",
  "All statuses": "Tất cả trạng thái",
  "Allergy warnings come from the prescription response. A known allergy blocks the prescription; missing or unconfirmed allergy data only produces a warning. Missing allergy data never means the medication is safe.": "Cảnh báo dị ứng lấy từ phản hồi tạo đơn thuốc. Dị ứng đã biết sẽ chặn đơn thuốc; dữ liệu dị ứng thiếu hoặc chưa xác nhận chỉ tạo cảnh báo. Dữ liệu dị ứng bị thiếu không có nghĩa là thuốc an toàn.",
  "Allergy warnings": "Cảnh báo dị ứng",
  "Appointment ID (optional)": "Mã hẹn khám (tùy chọn)",
  "Approved by": "Người duyệt",
  "Assignment": "Phân công",
  "Attending employee ID": "ID nhân viên phụ trách",
  "Attending employee": "Nhân viên phụ trách",
  "Birth date": "Ngày sinh",
  "CLS results (SCR-EMR-05)": "Kết quả CLS (SCR-EMR-05)",
  "CONFIRMED": "Đã xác nhận",
  "Clinical orders (SCR-EMR-04)": "Chỉ định lâm sàng (SCR-EMR-04)",
  "Code": "Mã",
  "Completed": "Đã hoàn thành",
  "Confirmed at": "Xác nhận lúc",
  "Confirmed by": "Người xác nhận",
  "Content": "Nội dung",
  "Create decision": "Tạo quyết định",
  "Create order": "Tạo chỉ định",
  "Create prescription": "Tạo đơn thuốc",
  "Create role profile": "Tạo hồ sơ vai trò",
  "Created at": "Tạo lúc",
  "Created by": "Người tạo",
  "Credential number": "Số chứng chỉ",
  "Credential type": "Loại chứng chỉ",
  "Current version": "Phiên bản hiện tại",
  "Day shift": "Ca ngày",
    "Days": "Ngày",
  "Decision reference (optional)": "Số hiệu quyết định (tùy chọn)",
  "Decision reference": "Số hiệu quyết định",
  "Decision type": "Loại quyết định",
  "Decision": "Quyết định",
  "Demo accounts:": "Tài khoản demo:",
  "Demo confirmation (SCR-EMR-08): Locking a record here is a demo confirmation only. It is NOT a digital signature and NOT a legally binding signature. No cryptographic signature is created or verified.": "Xác nhận demo (SCR-EMR-08): Việc khóa bệnh án tại đây chỉ là xác nhận demo. Đây KHÔNG phải chữ ký số và KHÔNG phải chữ ký có giá trị pháp lý. Không có chữ ký mật mã nào được tạo ra hay xác minh.",
  "Demo user": "Người dùng demo",
  "Department and job title are not changed here; use a transfer for those.": "Phòng ban và chức danh không thay đổi tại đây; hãy dùng chuyển đổi cho các thay đổi đó.",
  "Department code": "Mã phòng ban",
  "Department type (optional)": "Loại phòng ban (tùy chọn)",
  "Department": "Phòng ban",
  "Departments": "Phòng ban",
  "Description (optional)": "Mô tả (tùy chọn)",
  "Description": "Mô tả",
  "Diagnosis": "Chẩn đoán",
  "Display name": "Tên hiển thị",
  "Dose": "Liều dùng",
  "EMR": "Bệnh án điện tử", "Effective at (UTC)": "Hiệu lực lúc (UTC)",
  "Effective at": "Hiệu lực lúc",
  "Effective from (UTC)": "Hiệu lực từ (UTC)",
  "Email": "Email",
  "Employee": "Nhân viên", "Employee code": "Mã nhân viên",
  "Employee sections": "Các mục nhân viên",
  "Active and inactive departments": "Các phòng ban đang hoạt động và không hoạt động", "Decisions waiting for a second approver": "Các quyết định đang chờ người phê duyệt thứ hai", "Defined roles and their rules": "Các vai trò đã định nghĩa và quy tắc của chúng", "Encounters currently open": "Các lượt khám đang mở", "Staff records you can access": "Hồ sơ nhân viên bạn có thể xem", "Employees": "Nhân viên",
  "Encounter sections": "Các mục lượt khám",
  "Encounters (SCR-EMR-01)": "Lượt khám (SCR-EMR-01)",
  "Encounters": "Lượt khám",
    "End": "Kết thúc",
  "Evening shift": "Ca chiều",
  "Every status change is kept here with its effective time and the decision that caused it. Records are never deleted.": "Mọi thay đổi trạng thái được lưu tại đây kèm thời điểm hiệu lực và quyết định gây ra thay đổi. Hồ sơ không bao giờ bị xóa.",
  "Execution": "Thi hành",
  "External order reference (optional)": "Mã tham chiếu bên ngoài (tùy chọn)",
  "External ref": "Tham chiếu ngoài",
  "FEMALE": "Nữ",
  "Filter encounters by status": "Lọc lượt khám theo trạng thái",
  "Four-eyes: you created this decision.": "Nguyên tắc bốn mắt: bạn là người tạo quyết định này.",
  "Frequency": "Tần suất",
  "From": "Từ",
  "Full name": "Họ và tên",
  "Gender": "Giới tính",
  "Global search": "Tìm kiếm toàn cục",
  "Good afternoon": "Chào buổi chiều",
  "Good evening": "Chào buổi tối",
  "Good morning": "Chào buổi sáng",
  "Good night": "Chào buổi đêm",
  "Hire date": "Ngày tuyển dụng",
  "Hospital Management": "Quản lý bệnh viện",
  "ICD-10 code (required for a CONFIRMED diagnosis)": "Mã ICD-10 (bắt buộc đối với chẩn đoán CONFIRMED)",
  "In progress": "Đang thực hiện",
    "Inactive": "Không hoạt động",
  "Issued at": "Cấp lúc",
  "Issuer": "Đơn vị cấp",
  "Items": "Mục",
  "Job title": "Chức danh",
  "LAB": "Xét nghiệm",
  "Language": "Ngôn ngữ",
  "Latest encounter": "Lượt khám mới nhất",
  "Loading addenda...": "Đang tải phụ lục...",
  "Loading assignments...": "Đang tải phân công...",
  "Loading certifications...": "Đang tải chứng chỉ...",
  "Loading decisions...": "Đang tải quyết định...",
  "Loading departments...": "Đang tải phòng ban...",
  "Loading employee...": "Đang tải nhân viên...",
  "Loading employees...": "Đang tải danh sách nhân viên...",
  "Loading encounter...": "Đang tải lượt khám...",
  "Loading": "Đang tải", "Open departments": "Mở phòng ban", "Open employees": "Mở nhân viên", "Open encounters": "Mở lượt khám", "Open role profiles": "Mở hồ sơ vai trò", "is the password for every account.": "là mật khẩu cho mọi tài khoản.", "Wrong password. The demo password is the one configured by SMARTHMS_DEMO_PASSWORD (default demo).": "Sai mật khẩu. Mật khẩu demo là mật khẩu đã cấu hình trong SMARTHMS_DEMO_PASSWORD (mặc định là demo).", "Loading encounters...": "Đang tải các lượt khám...",
  "Loading results...": "Đang tải kết quả...",
  "Loading role profiles...": "Đang tải hồ sơ vai trò...",
  "Loading status history...": "Đang tải lịch sử trạng thái...",
  "Loading versions...": "Đang tải phiên bản...",
  "Locked at": "Khóa lúc",
    "Locked by": "Người khóa",
  "MALE": "Nam",
  "Medical record (SCR-EMR-07, SCR-EMR-08)": "Bệnh án (SCR-EMR-07, SCR-EMR-08)",
  "Medication ID": "ID thuốc",
  "Metric code": "Mã chỉ số",
  "Metric": "Chỉ số",
  "Name": "Tên",
  "Narrative": "Ghi chú",
  "Needs attention": "Cần chú ý",
  "New addendum": "Phụ lục mới",
  "New order (doctor)": "Chỉ định mới (bác sĩ)",
  "New prescription (doctor)": "Đơn thuốc mới (bác sĩ)",
  "New role profile": "Hồ sơ vai trò mới",
  "New status decision (HR specialist)": "Quyết định trạng thái mới (chuyên viên nhân sự)",
  "New transfer (HR specialist)": "Chuyển mới (chuyên viên nhân sự)",
  "Night shift": "Ca đêm",
  "No allergy warning was returned for this prescription. This does not mean the medication is safe; allergy data may simply be missing or unconfirmed.": "Không có cảnh báo dị ứng nào được trả về cho đơn thuốc này. Điều đó không có nghĩa là thuốc an toàn; dữ liệu dị ứng có thể chỉ đơn giản là bị thiếu hoặc chưa được xác nhận.",
  "No employees match the search.": "Không có nhân viên nào khớp với tìm kiếm.",
  "No encounters in progress.": "Không có lượt khám nào đang diễn ra.",
  "No encounters yet.": "Chưa có lượt khám nào.",
    "No matches in the records you can access.": "Không có kết quả nào trong các hồ sơ bạn có thể xem.",
  "No pending decisions.": "Không có quyết định chờ duyệt nào.",
  "None": "Không có",
  "Not specified": "Chưa chỉ định",
  "Number": "Số",
  "Numeric value": "Giá trị số",
  "ONBOARDING": "Đang nhận việc",
  "OTHER": "Khác",
  "Observed at (UTC)": "Quan trắc lúc (UTC)",
  "Observed at": "Quan trắc lúc",
  "On duty": "Đang trực",
  "Open encounter": "Mở lượt khám",
  "Open navigation": "Mở điều hướng",
  "Opened by": "Người mở",
  "Order type": "Loại chỉ định",
  "Order": "Chỉ định",
  "Order status:": "Trạng thái chỉ định:",
  "Ordered at": "Chỉ định lúc",
  "Ordered by": "Người chỉ định",
  "Overview": "Tổng quan",
  "PENDING": "Đang chờ",
  "PRIMARY": "Chính",
  "PROVISIONAL": "Tạm thời",
  "Parent department ID (optional)": "ID phòng ban cha (tùy chọn)",
  "Parent": "Phòng ban cha",
  "Password": "Mật khẩu",
  "Patient ID": "ID bệnh nhân",
  "Patient": "Bệnh nhân",
  "Payload": "Dữ liệu",
    "Pending decisions": "Quyết định chờ duyệt",
  "Phone": "Điện thoại",
  "Practice certificates (CCHN)": "Chứng chỉ hành nghề (CCHN)",
  "Prescriber": "Người kê đơn",
  "Prescription created": "Đã tạo đơn thuốc",
  "Prescription": "Đơn thuốc",
  "Prescriptions (SCR-EMR-06)": "Đơn thuốc (SCR-EMR-06)",
  "Primary navigation": "Điều hướng chính", "Primary mobile": "Số di động chính",
  "Primary": "Chính",
  "Professional group (optional)": "Nhóm chuyên môn (tùy chọn)",
  "Professional group": "Nhóm chuyên môn",
  "Qualification (optional)": "Trình độ chuyên môn (tùy chọn)",
  "Qualification": "Trình độ chuyên môn",
  "Quantity": "Số lượng",
  "RAD": "Chẩn đoán hình ảnh",
  "REJECTED": "Đã từ chối",
  "Reason": "Lý do",
  "Receive result (CLS technician)": "Nhận kết quả (kỹ thuật viên CLS)",
  "Receive result": "Nhận kết quả",
  "Received at": "Nhận lúc",
  "Record diagnosis": "Ghi chẩn đoán",
  "Record lifecycle: DRAFT → PENDING_SIGNATURE → LOCKED → ARCHIVED. Submitting uses the current row version; if the record changed, the request is refused with a conflict (409) and nothing is overwritten.": "Vòng đời bệnh án: DRAFT → PENDING_SIGNATURE → LOCKED → ARCHIVED. Khi gửi, hệ thống dùng phiên bản dòng hiện tại; nếu bệnh án đã thay đổi, yêu cầu bị từ chối do xung đột (409) và không có gì bị ghi đè.",
  "Record transfer": "Ghi nhận chuyển đổi",
  "Record vital sign (nurse)": "Ghi dấu hiệu sinh tồn (điều dưỡng)",
  "Record vital sign": "Ghi dấu hiệu sinh tồn",
  "Record": "Bệnh án",
  "Recorded at": "Ghi nhận lúc",
  "Reference": "Tham chiếu",
  "Result payload (JSON or text)": "Dữ liệu kết quả (JSON hoặc văn bản)",
  "Result": "Kết quả",
  "{inProgress} in progress - {completed} completed": "{inProgress} đang thực hiện - {completed} đã hoàn thành", "Review decision": "Xem xét quyết định", "Review decisions": "Xem xét các quyết định",
    "Role profile (optional)": "Hồ sơ vai trò (tùy chọn)",
  "Role profile": "Hồ sơ vai trò",
  "Role profiles name the professional group and qualification used when an employee is hired or transferred.": "Hồ sơ vai trò nêu nhóm chuyên môn và trình độ chuyên môn dùng khi nhân viên được tuyển dụng hoặc chuyển đổi.",
  "Role profiles": "Hồ sơ vai trò",
  "Route": "Đường dùng",
  "Row version": "Phiên bản dòng",
  "Row": "Dòng",
  "SECONDARY": "Phụ",
  "SUSPENDED": "Tạm đình chỉ",
  "Scope note": "Ghi chú phạm vi",
  "Search and filter employees by department, status and job title. Creating an employee grants no system role.": "Tìm kiếm và lọc nhân viên theo phòng ban, trạng thái và chức danh. Tạo nhân viên không cấp quyền hệ thống.",
  "Search patients, employees, departments": "Tìm kiếm bệnh nhân, nhân viên, phòng ban",
  "Search records": "Tìm kiếm hồ sơ",
  "Search": "Tìm kiếm",
  "Searching real records": "Đang tìm trong hồ sơ thực.",
  "Sections": "Mục",
  "Select a department": "Chọn phòng ban",
  "Sign in (demo)": "Đăng nhập (demo)",
  "Sign in": "Đăng nhập",
  "Sign out": "Đăng xuất",
  "Smart Hospital Management System - demo environment.": "Hệ thống quản lý bệnh viện thông minh - môi trường demo.",
  "Source reference": "Tham chiếu nguồn",
  "Source result ID": "ID kết quả nguồn",
  "Source system": "Hệ thống nguồn",
  "Source version (optional)": "Phiên bản nguồn (tùy chọn)",
  "Source": "Nguồn",
  "Start": "Bắt đầu",
    "Started": "Đã bắt đầu",
  "Status decisions": "Quyết định trạng thái",
  "Status effective": "Trạng thái hiệu lực",
  "Status history": "Lịch sử trạng thái",
  "Status lifecycle: ONBOARDING to ACTIVE, ACTIVE and SUSPENDED, TERMINATED is terminal. A decision takes effect only at its effective time; future decisions are not applied early. The approver must differ from the person who created the decision (four-eyes).": "Vòng đời trạng thái: ONBOARDING sang ACTIVE, ACTIVE và SUSPENDED, TERMINATED là trạng thái cuối. Quyết định chỉ có hiệu lực vào thời điểm hiệu lực của nó; các quyết định trong tương lai không được áp dụng sớm. Người phê duyệt phải khác người tạo quyết định (bốn mắt).",
  "Status": "Trạng thái",
  "TERMINATED": "Đã nghỉ việc",
  "Text value (optional)": "Giá trị văn bản (tùy chọn)",
  "The employee code is required and must be unique. Department and job title are set here; later changes to the department go through a transfer.": "Mã nhân viên là bắt buộc và phải duy nhất. Phòng ban và chức danh được đặt tại đây; các thay đổi phòng ban về sau thực hiện qua chuyển đổi.",
  "The patient ID must match the encounter patient. A result for a different patient is quarantined and does not complete the order.": "ID bệnh nhân phải trùng với bệnh nhân của lượt khám. Kết quả của bệnh nhân khác sẽ bị cách ly và không hoàn thành chỉ định.",
  "The prescriber must have a valid CCHN and the medication must be active. A known allergy blocks the prescription with an error; the prescription is not created.": "Người kê đơn phải có CCHN còn hiệu lực và thuốc phải đang được lưu hành. Dị ứng đã biết sẽ chặn đơn thuốc kèm lỗi; đơn thuốc không được tạo.",
  "Title name": "Tên chức danh",
  "Title": "Chức danh",
  "To": "Đến",
  "Transfers and assignments": "Chuyển đổi và phân công",
  "Type at least 2 characters. Encounters are matched by patient and encounter ID; employees by name or code; departments by name or code.": "Nhập ít nhất 2 ký tự. Lượt khám được tìm theo bệnh nhân và ID lượt khám; nhân viên theo tên hoặc mã; phòng ban theo tên hoặc mã.",
  "Type": "Loại",
  "UNVERIFIED": "Chưa xác minh",
  "Unit": "Đơn vị",
  "VERIFIED": "Đã xác minh",
  "Valid from": "Có hiệu lực từ",
  "Valid until": "Có hiệu lực đến",
  "Validity is computed by the backend: VALID, EXPIRING_SOON (1 to 30 days left), EXPIRED (the day after the expiry date), UNVERIFIED (never eligible) or REJECTED. An expired CCHN does not lock the employee account.": "Tính hiệu lực do backend tính: VALID, EXPIRING_SOON (còn 1 đến 30 ngày), EXPIRED (ngày sau ngày hết hạn), UNVERIFIED (không bao giờ đủ điều kiện) hoặc REJECTED. CCHN hết hạn không khóa tài khoản nhân viên.",
  "Validity": "Hiệu lực",
  "Value": "Giá trị",
  "Verification status": "Trạng thái xác minh",
  "Verification": "Xác minh",
  "Version": "Phiên bản",  "Version history": "Lịch sử phiên bản",
  "Vitals and diagnosis (SCR-EMR-03)": "Dấu hiệu sinh tồn và chẩn đoán (SCR-EMR-03)",
  "encounters": "lượt khám",
  "employees": "nhân viên",
    "Unexpected error.": "Đã xảy ra lỗi không mong đợi.",
  "Showing {shown} of {total} {noun}": "Hiển thị {shown} trên tổng số {total} {noun}",
  "All": "Tất cả",
  "Employee directory": "Danh sách nhân viên",
  "Labs & results": "Xét nghiệm & kết quả",
  "Mobile navigation": "Điều hướng di động", "Medical records": "Bệnh án",
  "Add employee": "Thêm nhân viên",
  "Add department": "Thêm phòng ban",
  "HR": "Nhân sự",
  "Backend status:": "Trạng thái backend:",
  "Live summary of the records you can access. Every card opens the section it summarises.": "Tóm tắt trực tiếp các hồ sơ bạn có thể xem. Mỗi thẻ sẽ mở phần tương ứng.",
  "Encounters visible to {name}. Doctors see only their own encounters.": "Các lượt khám hiển thị cho {name}. Bác sĩ chỉ xem được lượt khám của mình.",
  "The patient must be VERIFIED. Demo patient:": "Bệnh nhân phải ở trạng thái VERIFIED. Bệnh nhân demo:",
  "Demo attending:": "Bác sĩ phụ trách demo:",
  "Encounter": "Lượt khám",
  "None returned": "Không có cảnh báo dị ứng nào.", "Record status:": "Trạng thái bệnh án:", "Addenda can only be added to a LOCKED record. The original record is kept unchanged; an addendum is a separate, confirmed annotation.": "Chỉ có thể thêm phụ lục vào bệnh án đã khóa. Bệnh án gốc được giữ nguyên; phụ lục là một ghi chú riêng, đã xác nhận.", "No medical record exists for this encounter yet. The record is created when the encounter completes.": "Chưa có bệnh án cho lượt khám này. Bệnh án được tạo khi lượt khám hoàn thành.", "No medical record exists for this encounter yet. Addenda can only be added to a LOCKED record.": "Chưa có bệnh án cho lượt khám này. Chỉ có thể thêm phụ lục vào bệnh án đã khóa.", "not started": "chưa bắt đầu", "Medical record:": "Bệnh án:", "No orders yet. Create an order on the Orders tab first.": "Chưa có chỉ định nào. Hãy tạo chỉ định ở tab Chỉ định trước.", "No results for this order yet. The result is pending.": "Chưa có kết quả nào cho chỉ định này. Kết quả đang chờ.", "No addenda yet.": "Chưa có phụ lục nào.", "This encounter has {vital} vital-sign records, {diagnoses} diagnoses, {orders} orders and {prescriptions} prescriptions.": "Lượt khám này có {vital} bản ghi dấu hiệu sinh tồn, {diagnoses} chẩn đoán, {orders} chỉ định và {prescriptions} đơn thuốc.", "No orders yet.": "Chưa có chỉ định nào.", "No encounters match this status.": "Không có lượt khám nào khớp với trạng thái này.", "Encounter not found.": "Không tìm thấy lượt khám.", "No vital signs recorded yet.": "Chưa ghi dấu hiệu sinh tồn nào.", "No diagnoses recorded yet.": "Chưa ghi chẩn đoán nào.", "No prescriptions yet.": "Chưa có đơn thuốc nào.", "No versions recorded yet.": "Chưa ghi phiên bản nào.", "Employee not found.": "Không tìm thấy nhân viên.", "No certifications yet.": "Chưa có chứng chỉ nào.", "No decisions yet.": "Chưa có quyết định nào.", "No assignments yet.": "Chưa có phân công nào.", "No status history yet.": "Chưa có lịch sử trạng thái nào.", "No departments yet.": "Chưa có phòng ban nào.", "No role profiles yet.": "Chưa có hồ sơ vai trò nào.", "Attending": "Bác sĩ điều trị", "CCHN": "CCHN", "Ctrl K": "Ctrl K", "Diagnosis (doctor)": "Chẩn đoán (bác sĩ)", "ICD-10": "ICD-10", "Loading encounters": "Đang tải các lượt khám", "Quality": "Chất lượng", "SHA-256": "SHA-256", "SmartHMS": "SmartHMS", "Unknown demo user.": "Không tìm thấy người dùng demo.", "Completed at": "Hoàn thành lúc",
  "Vital signs can only be recorded by a nurse. You are signed in as": "Chỉ điều dưỡng mới ghi được dấu hiệu sinh tồn. Bạn đang đăng nhập với vai trò",
  "Diagnoses can only be recorded by a doctor. You are signed in as": "Chỉ bác sĩ mới ghi được chẩn đoán. Bạn đang đăng nhập với vai trò",
  "Orders can only be created by a doctor. You are signed in as": "Chỉ bác sĩ mới tạo được chỉ định. Bạn đang đăng nhập với vai trò",
  "Prescriptions can only be created by a doctor. You are signed in as": "Chỉ bác sĩ mới tạo được đơn thuốc. Bạn đang đăng nhập với vai trò",
  "Item": "Mục",
  "The record is {status}, not LOCKED, so no addendum can be added yet.": "Bệnh án đang ở trạng thái {status}, chưa LOCKED, nên chưa thể thêm phụ lục.",
  "Search failed": "Tìm kiếm thất bại",
  "Notifications": "Thông báo",
  "Notifications, {n} pending decisions": "Thông báo, {n} quyết định chờ duyệt"
};

const viStatus: Record<string, string> = {
  WAITING: "Đang chờ",
  IN_PROGRESS: "Đang khám",
  PENDING_LAB_RAD: "Chờ xét nghiệm / chẩn đoán hình ảnh",
  DIAGNOSED: "Đã chẩn đoán",
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
  PROVISIONAL: "Tạm thời",
  CONFIRMED: "Đã xác nhận",
  PRIMARY: "Chính",
  SECONDARY: "Phụ",
  LAB: "Xét nghiệm",
  RAD: "Chẩn đoán hình ảnh",
  FEMALE: "Nữ",
  MALE: "Nam",
  OTHER: "Khác",
  DRAFT: "Bản nháp",
  LOCKED: "Đã khóa",
  SIGNED: "Đã ký",
  KNOWN: "Đã biết",
  UNKNOWN: "Chưa rõ",
  WARNING: "Cảnh báo",
  CCHN: "CCHN",
  OPEN: "Đang mở",
  CLOSED: "Đã đóng",
  APPLIED: "Đã áp dụng",
  QUARANTINED: "Đã cách ly",
  REVOKED: "Đã thu hồi",
  FAILED: "Thất bại",
  PENDING_SIGNATURE: "Chờ ký",
  ARCHIVED: "Đã lưu trữ",
  DOCTOR: "Bác sĩ",
  NURSE: "Điều dưỡng",
  CLS_TECHNICIAN: "Kỹ thuật viên CLS",
  RECORDS_MANAGER: "Quản lý bệnh án",
  RECEPTION: "Lễ tân",
  HR_SPECIALIST: "Chuyên viên nhân sự",
  HR_APPROVER: "Người phê duyệt nhân sự",
  NORMAL: "Bình thường",
  ABNORMAL: "Bất thường",
  CRITICAL: "Nguy kịch",
  HIGH: "Cao",
  LOW: "Thấp",
  Normal: "Bình thường",
  Abnormal: "Bất thường"
};

export function t(key: string): string {
  if (current === "vi") return vi[key] ?? key;
  return key;
}

export function tf(key: string, vars: Record<string, string | number>): string {
  let out = t(key);
  for (const [name, value] of Object.entries(vars)) {
    out = out.split(`{${name}}`).join(String(value));
  }
  return out;
}

export function statusLabel(value: string | null | undefined): string {
  if (value == null || value === "") return value ?? "";
  if (current === "vi") return viStatus[value] ?? value;
  return value;
}

export function greetingFor(date: Date): string {
  const h = date.getHours();
  if (h < 5) return t("Good night");
  if (h < 12) return t("Good morning");
  if (h < 18) return t("Good afternoon");
  return t("Good evening");
}

export function shiftFor(date: Date): string {
  const h = date.getHours();
  if (h >= 7 && h < 15) return t("Day shift");
  if (h >= 15 && h < 23) return t("Evening shift");
  return t("Night shift");
}

type LanguageContextValue = {
  lang: Language;
  setLang: (lang: Language) => void;
};

const LanguageContext = createContext<LanguageContextValue>({
  lang: DEFAULT_LANGUAGE,
  setLang: setLanguage,
});

export function useLanguage(): LanguageContextValue {
  return useContext(LanguageContext);
}

export function LanguageProvider({ children }: { children: ReactNode }) {
  const [lang, setLangState] = useState<Language>(current);
  useEffect(() => {
    document.documentElement.lang = lang;
  }, [lang]);
  useEffect(() => {
    const refresh = () => setLangState(current);
    listeners.add(refresh);
    return () => {
      listeners.delete(refresh);
    };
  }, []);
  return (
    <LanguageContext.Provider value={{ lang, setLang: (next: Language) => { setLanguage(next); setLangState(next); } }}>
    {children}
  </LanguageContext.Provider>
  );
}

export function LanguageSwitcher() {
  const { lang, setLang } = useLanguage();
  return (
    <div className="lang-switcher" role="group" aria-label={t("Language")}>
      {(["vi", "en"] as const).map((code) => (
        <button
          key={code}
          type="button"
          className={lang === code ? "lang-switcher-btn active" : "lang-switcher-btn"}
          onClick={() => setLang(code)}
        >
          {code === "vi" ? "Tiếng Việt" : "English"}
        </button>
        ))}
    </div>
  );
}
