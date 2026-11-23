export type Meeting = { weekday: number; startTime: string; endTime: string };
export type Course = {
  id: number;
  teacherId: number;
  teacherName: string;
  title: string;
  description: string;
  subject: string;
  level: string;
  area: string;
  location: string;
  mode: "ONLINE" | "OFFLINE";
  tuition: number;
  pricingUnit: "COURSE";
  capacity: number;
  enrolled: number;
  startDate: string;
  endDate: string;
  status: "DRAFT" | "PUBLISHED" | "HIDDEN" | "ARCHIVED";
  meetings: Meeting[];
};
export type CourseInput = Omit<
  Course,
  "id" | "teacherId" | "teacherName" | "pricingUnit" | "enrolled" | "status"
>;
export type PageResult<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
};
export type Teacher = {
  id: number;
  name: string;
  bio: string;
  subjects: string;
  courses: Course[];
};
export type Status =
  "NEW" | "CONTACTED" | "CONFIRMED" | "ATTENDING" | "CANCELLED" | "REJECTED";
export type Application = {
  id: number;
  course: Course;
  studentName: string;
  email: string;
  phone: string;
  message: string | null;
  source: string;
  status: Status;
  createdAt: string;
  history: {
    status: Status;
    note: string | null;
    actorName: string | null;
    createdAt: string;
  }[];
};
export type Analytics = {
  views: number;
  applications: number;
  confirmed: number;
  conversionRate: number;
  sources: {
    source: string;
    views: number;
    applications: number;
    confirmed: number;
  }[];
};
export type Plan = {
  name: string;
  courseLimit: number;
  coursesUsed: number;
  upgradesAvailable: boolean;
};
export const statusText: Record<Status, string> = {
  NEW: "Mới",
  CONTACTED: "Đang tư vấn",
  CONFIRMED: "Đã xác nhận",
  ATTENDING: "Đang học",
  CANCELLED: "Đã hủy",
  REJECTED: "Từ chối",
};
export const courseStatus: Record<Course["status"], string> = {
  DRAFT: "Bản nháp",
  PUBLISHED: "Công khai",
  HIDDEN: "Đã ẩn",
  ARCHIVED: "Lưu trữ",
};
export const money = (n: number) =>
  new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
    maximumFractionDigits: 0,
  }).format(n);
export const day = (n: number) => (n === 7 ? "Chủ nhật" : `Thứ ${n + 1}`);
export const date = (s: string) =>
  new Intl.DateTimeFormat("vi-VN").format(new Date(s + "T00:00:00"));
export const timestamp = (s: string) =>
  new Intl.DateTimeFormat("vi-VN", {
    dateStyle: "short",
    timeStyle: "short",
    timeZone: "Asia/Ho_Chi_Minh",
  }).format(new Date(s + "Z"));
export const sourceQuery = () =>
  new URLSearchParams(location.search).get("utm_source") || "";
export const sourceSuffix = () =>
  sourceQuery() ? "?utm_source=" + encodeURIComponent(sourceQuery()) : "";
