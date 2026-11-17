/**
 * Mô tả: Liệt kê trạng thái hồ sơ bệnh án: đang soạn, chờ ký, đã khóa, đã lưu trữ.
 * Người thực hiện: Trần Duy Hiếu
 * Ngày thực hiện: 08/11/2026
 */
package vn.edu.ptit.smarthms.emr.domain;

public enum RecordStatus { DRAFT, PENDING_SIGNATURE, LOCKED, ARCHIVED }