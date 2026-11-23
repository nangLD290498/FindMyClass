import {
  useEffect,
  useRef,
  useState,
  type FormEvent,
  type ReactNode,
  type ChangeEvent,
} from "react";
import {
  Link,
  useNavigate,
  useParams,
  useSearchParams,
} from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api, type User } from "./api";
import { useUser, ErrorNotice, Loading, Retry } from "./session";
import {
  type Course,
  type CourseInput,
  type PageResult,
  type Teacher,
  type Application,
  type Status,
  type Analytics,
  type Plan,
  statusText,
  courseStatus,
  money,
  day,
  date,
  timestamp,
  sourceQuery,
  sourceSuffix,
} from "./learning-api";
function Heading({
  title,
  description,
  children,
}: {
  title: string;
  description?: string;
  children?: ReactNode;
}) {
  return (
    <div className="page-heading">
      <div>
        <p className="eyebrow">FINDMYCLASS</p>
        <h1>{title}</h1>
        {description && <p>{description}</p>}
      </div>
      {children}
    </div>
  );
}
function Empty({ children }: { children: ReactNode }) {
  return <div className="empty-state">{children}</div>;
}
function QueryState({
  query,
}: {
  query: { isPending: boolean; error: Error | null; refetch: () => unknown };
}) {
  return query.isPending ? (
    <Loading />
  ) : query.error ? (
    <Retry error={query.error} retry={() => void query.refetch()} />
  ) : null;
}
function CourseCard({ course: c }: { course: Course }) {
  return (
    <article className="course-card">
      <div className="course-art" aria-hidden="true">
        {c.subject.slice(0, 1)}
      </div>
      <div className="course-card-body">
        <span className="pill">
          {c.mode === "ONLINE" ? "Trực tuyến" : "Trực tiếp"} · {c.subject}
        </span>
        <h2>
          <Link to={`/courses/${c.id}${sourceSuffix()}`}>{c.title}</Link>
        </h2>
        <p>
          {c.teacherName} · {c.level}
        </p>
        <p>
          {c.area} · {date(c.startDate)}
        </p>
        <p>
          {c.meetings
            .map((m) => `${day(m.weekday)} ${m.startTime}`)
            .join(" · ")}
        </p>
        <div className="course-card-bottom">
          <strong>{money(c.tuition)} / khóa</strong>
          <span>{Math.max(0, c.capacity - c.enrolled)} chỗ còn lại</span>
        </div>
      </div>
    </article>
  );
}
export function Marketplace() {
  const [params, setParams] = useSearchParams();
  const q = useQuery({
    queryKey: ["marketplace", params.toString()],
    queryFn: () => api<PageResult<Course>>("/api/public/courses?" + params),
  });
  function filter(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const p = new URLSearchParams();
    new FormData(e.currentTarget).forEach((v, k) => {
      if (String(v)) p.set(k, String(v));
    });
    if (params.has("utm_source"))
      p.set("utm_source", params.get("utm_source")!);
    setParams(p);
  }
  return (
    <section className="feature-page">
      <Heading
        title="Tìm lớp học dành cho bạn"
        description="Khám phá lớp học, kết nối giáo viên và bắt đầu hành trình mới."
      />
      <form className="filters" onSubmit={filter} key={params.toString()}>
        <label>
          Tìm kiếm
          <input
            name="q"
            defaultValue={params.get("q") || ""}
            placeholder="Khóa học, giáo viên, môn học"
            maxLength={160}
          />
        </label>
        <label>
          Môn học
          <input
            name="subject"
            defaultValue={params.get("subject") || ""}
            maxLength={100}
          />
        </label>
        <label>
          Khu vực
          <input
            name="area"
            defaultValue={params.get("area") || ""}
            maxLength={160}
          />
        </label>
        <label>
          Hình thức
          <select name="mode" defaultValue={params.get("mode") || ""}>
            <option value="">Tất cả</option>
            <option value="ONLINE">Trực tuyến</option>
            <option value="OFFLINE">Trực tiếp</option>
          </select>
        </label>
        <label>
          Học phí từ (đ/khóa)
          <input
            name="min"
            type="number"
            min="0"
            defaultValue={params.get("min") || ""}
          />
        </label>
        <label>
          Học phí đến (đ/khóa)
          <input
            name="max"
            type="number"
            min="0"
            defaultValue={params.get("max") || ""}
          />
        </label>
        <label>
          Ngày học
          <select name="weekday" defaultValue={params.get("weekday") || ""}>
            <option value="">Tất cả</option>
            {[1, 2, 3, 4, 5, 6, 7].map((n) => (
              <option key={n} value={n}>
                {day(n)}
              </option>
            ))}
          </select>
        </label>
        <button className="primary">Tìm lớp</button>
        <button
          type="button"
          className="secondary"
          onClick={() => setParams({})}
        >
          Xóa bộ lọc
        </button>
      </form>
      <QueryState query={q} />
      {q.data && (
        <>
          <p role="status">Tìm thấy {q.data.totalElements} khóa học</p>
          <div className="course-grid">
            {q.data.content.map((c) => (
              <CourseCard key={c.id} course={c} />
            ))}
          </div>
          {!q.data.content.length && (
            <Empty>Chưa có lớp phù hợp. Bạn hãy thử thay đổi bộ lọc.</Empty>
          )}
          <div className="pagination">
            <button
              className="secondary"
              disabled={q.data.page === 0}
              onClick={() => {
                const p = new URLSearchParams(params);
                p.set("page", String(q.data.page - 1));
                setParams(p);
              }}
            >
              Trước
            </button>
            <span>
              Trang {q.data.page + 1} / {Math.max(1, q.data.totalPages)}
            </span>
            <button
              className="secondary"
              disabled={q.data.page + 1 >= q.data.totalPages}
              onClick={() => {
                const p = new URLSearchParams(params);
                p.set("page", String(q.data.page + 1));
                setParams(p);
              }}
            >
              Sau
            </button>
          </div>
        </>
      )}
    </section>
  );
}
function useTrack(kind: "courseId" | "teacherId", id: number, ready: boolean) {
  const sent = useRef("");
  const source = sourceQuery();
  useEffect(() => {
    const key = kind + id + source;
    if (!ready || sent.current === key) return;
    sent.current = key;
    void api("/api/public/views", {
      method: "POST",
      body: JSON.stringify({ [kind]: id, source }),
    }).catch(() => {});
  }, [kind, id, source, ready]);
}
export function CourseDetail() {
  const { id } = useParams();
  const user = useUser();
  const client = useQueryClient();
  const [submitted, setSubmitted] = useState(false);
  const q = useQuery({
    queryKey: ["course", id],
    queryFn: () => api<Course>("/api/public/courses/" + id),
  });
  useTrack("courseId", Number(id), q.isSuccess);
  const apply = useMutation({
    mutationFn: (body: unknown) =>
      api("/api/student/courses/" + id + "/applications", {
        method: "POST",
        body: JSON.stringify(body),
      }),
    onSuccess: () => {
      setSubmitted(true);
      void client.invalidateQueries({ queryKey: ["student-applications"] });
    },
  });
  function send(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const f = new FormData(e.currentTarget);
    apply.mutate({
      phone: f.get("phone"),
      message: f.get("message"),
      source: sourceQuery(),
      contactConsent: f.get("consent") === "on",
    });
  }
  useEffect(() => setSubmitted(false), [id]);
  if (q.error || !q.data) return <QueryState query={q} />;
  const c = q.data;
  return (
    <section className="feature-page">
      <Link to={"/courses" + sourceSuffix()}>← Tìm lớp</Link>
      <Heading title={c.title} description={`${c.subject} · ${c.level}`} />
      <div className="detail-grid">
        <article className="panel">
          <span className="pill">
            {c.mode === "ONLINE" ? "Trực tuyến" : "Trực tiếp"}
          </span>
          <h2>Nội dung khóa học</h2>
          <p className="preserve-lines">{c.description}</p>
          <h2>Giáo viên</h2>
          <Link
            className="inline-link"
            to={`/teachers/${c.teacherId}${sourceSuffix()}`}
          >
            {c.teacherName} → Xem trang giáo viên
          </Link>
          <h2>Lịch học</h2>
          <p>
            {date(c.startDate)} – {date(c.endDate)} · Giờ Việt Nam
          </p>
          <ul>
            {c.meetings.map((m, i) => (
              <li key={i}>
                {day(m.weekday)}: {m.startTime}–{m.endTime}
              </li>
            ))}
          </ul>
          <h2>Địa điểm / nền tảng</h2>
          <p>
            {c.area} · {c.location}
          </p>
        </article>
        <aside className="panel">
          <h2>{money(c.tuition)} / khóa</h2>
          <p>
            {c.enrolled}/{c.capacity} học viên đã được xác nhận
          </p>
          {submitted ? (
            <div role="status">
              <h3>Đã gửi đăng ký!</h3>
              <p>
                Giáo viên sẽ liên hệ với bạn. Đăng ký chưa phải xác nhận nhập
                học.
              </p>
              <Link className="primary button-link" to="/student/applications">
                Xem đăng ký của tôi
              </Link>
            </div>
          ) : c.enrolled >= c.capacity ? (
            <Empty>Lớp đã đủ chỗ.</Empty>
          ) : user.isPending ? (
            <Loading />
          ) : user.error ? (
            <Retry error={user.error} retry={() => void user.refetch()} />
          ) : !user.data ? (
            <>
              <p>Đăng nhập để gửi đăng ký cho giáo viên.</p>
              <Link
                className="primary button-link"
                to="/login"
                onClick={() =>
                  sessionStorage.setItem(
                    "pending-course",
                    String(c.id) + sourceSuffix(),
                  )
                }
              >
                Đăng nhập để đăng ký
              </Link>
            </>
          ) : user.data.role !== "STUDENT" ? (
            <p>
              Chức năng đăng ký dành cho tài khoản học viên.{" "}
              <Link to={user.data.nextPath}>Về trang của bạn</Link>
            </p>
          ) : (
            <form onSubmit={send} className="stack">
              <h3>Đăng ký nhận tư vấn</h3>
              <label>
                Số điện thoại
                <input
                  name="phone"
                  type="tel"
                  required
                  pattern="[+0-9() .\-]{7,30}"
                  maxLength={30}
                />
              </label>
              <label>
                Lời nhắn
                <textarea name="message" rows={3} maxLength={1000} />
              </label>
              <label className="check">
                <input type="checkbox" name="consent" required />
                Tôi đồng ý chia sẻ tên, email và số điện thoại với giáo viên để
                xử lý đăng ký.
              </label>
              <p className="fine-print">
                Chỗ học được tính khi giáo viên xác nhận. Mỗi học viên gửi một
                đăng ký cho mỗi khóa học.
              </p>
              <ErrorNotice error={apply.error} />
              <button className="primary" disabled={apply.isPending}>
                {apply.isPending ? "Đang gửi…" : "Gửi đăng ký"}
              </button>
            </form>
          )}
        </aside>
      </div>
    </section>
  );
}
export function TeacherPage() {
  const { id } = useParams();
  const q = useQuery({
    queryKey: ["public-teacher", id],
    queryFn: () => api<Teacher>("/api/public/teachers/" + id),
  });
  useTrack("teacherId", Number(id), q.isSuccess);
  if (q.error || !q.data) return <QueryState query={q} />;
  return (
    <section className="feature-page">
      <Heading title={q.data.name} description={q.data.subjects} />
      <div className="panel">
        <h2>Giới thiệu giáo viên</h2>
        <p className="preserve-lines">{q.data.bio}</p>
      </div>
      <h2>Các khóa học đang tuyển sinh</h2>
      <div className="course-grid">
        {q.data.courses.map((c) => (
          <CourseCard course={c} key={c.id} />
        ))}
      </div>
      {!q.data.courses.length && (
        <Empty>Giáo viên chưa có khóa học công khai.</Empty>
      )}
    </section>
  );
}
export function TeacherNav() {
  return (
    <nav className="section-nav" aria-label="Quản lý giáo viên">
      <Link to="/teacher/courses">Khóa học</Link>
      <Link to="/teacher/applications">Đăng ký & CRM</Link>
      <Link to="/teacher/analytics">Thống kê</Link>
      <Link to="/teacher/share">Trang tuyển sinh</Link>
      <Link to="/teacher/plan">Gói dịch vụ</Link>
    </nav>
  );
}
export function DashboardLinks({ role }: { role: "teacher" | "student" }) {
  return role === "teacher" ? (
    <TeacherNav />
  ) : (
    <nav className="section-nav">
      <Link to="/courses">Tìm lớp học</Link>
      <Link to="/student/applications">Đăng ký của tôi</Link>
    </nav>
  );
}
export function TeacherCourses() {
  const q = useQuery({
    queryKey: ["teacher-courses"],
    queryFn: () => api<Course[]>("/api/teacher/courses"),
  });
  const client = useQueryClient();
  const change = useMutation({
    mutationFn: ({ id, status }: { id: number; status: Course["status"] }) =>
      api("/api/teacher/courses/" + id + "/status", {
        method: "PUT",
        body: JSON.stringify({ status }),
      }),
    onSuccess: () => {
      void client.invalidateQueries({ queryKey: ["teacher-courses"] });
      void client.invalidateQueries({ queryKey: ["marketplace"] });
    },
  });
  return (
    <section className="feature-page">
      <Heading
        title="Khóa học của tôi"
        description="Soạn nội dung, công khai khóa học và nhận đăng ký."
      >
        <Link className="primary button-link" to="/teacher/courses/new">
          + Tạo khóa học
        </Link>
      </Heading>
      <TeacherNav />
      <ErrorNotice error={change.error} />
      <QueryState query={q} />
      {q.data && !q.data.length && (
        <Empty>Bắt đầu với khóa học đầu tiên của bạn.</Empty>
      )}
      <div className="management-list">
        {q.data?.map((c) => (
          <article className="panel" key={c.id}>
            <span className="pill">{courseStatus[c.status]}</span>
            <h2>{c.title}</h2>
            <p>
              {c.subject} · {money(c.tuition)}/khóa · {c.enrolled}/{c.capacity}{" "}
              học viên
            </p>
            <div className="actions">
              {c.status !== "ARCHIVED" && (
                <>
                  <Link
                    className="secondary button-link"
                    to={`/teacher/courses/${c.id}/edit`}
                  >
                    Chỉnh sửa
                  </Link>
                  <button
                    className="secondary"
                    disabled={change.isPending}
                    onClick={() =>
                      change.mutate({
                        id: c.id,
                        status:
                          c.status === "PUBLISHED" ? "HIDDEN" : "PUBLISHED",
                      })
                    }
                  >
                    {c.status === "PUBLISHED" ? "Ẩn khóa học" : "Công khai"}
                  </button>
                  <button
                    className="text-button"
                    disabled={change.isPending}
                    onClick={() => {
                      if (
                        confirm(
                          "Lưu trữ khóa học? Khóa học sẽ ngừng tuyển sinh và không thể chỉnh sửa.",
                        )
                      )
                        change.mutate({ id: c.id, status: "ARCHIVED" });
                    }}
                  >
                    Lưu trữ
                  </button>
                </>
              )}
              {c.status === "PUBLISHED" && (
                <Link to={`/courses/${c.id}`}>Xem trang công khai →</Link>
              )}
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}
const blank: CourseInput = {
  title: "",
  description: "",
  subject: "",
  level: "",
  area: "",
  location: "",
  mode: "OFFLINE",
  tuition: 0,
  capacity: 20,
  startDate: "",
  endDate: "",
  meetings: [{ weekday: 1, startTime: "18:00", endTime: "19:30" }],
};
export function CourseEditor() {
  const { id } = useParams();
  const q = useQuery({
    queryKey: ["edit-course", id],
    queryFn: () => api<Course>("/api/teacher/courses/" + id),
    enabled: !!id,
  });
  if (id && (q.error || !q.data)) return <QueryState query={q} />;
  return <EditorForm key={id || "new"} id={id} initial={q.data || blank} />;
}
function EditorForm({ id, initial }: { id?: string; initial: CourseInput }) {
  const [form, setForm] = useState<CourseInput>(initial);
  const nav = useNavigate();
  const client = useQueryClient();
  const save = useMutation({
    mutationFn: () =>
      api("/api/teacher/courses" + (id ? "/" + id : ""), {
        method: id ? "PUT" : "POST",
        body: JSON.stringify(form),
      }),
    onSuccess: () => {
      void client.invalidateQueries({ queryKey: ["teacher-courses"] });
      void client.invalidateQueries({ queryKey: ["edit-course", id] });
      nav("/teacher/courses");
    },
  });
  const field =
    (key: keyof CourseInput) =>
    (
      e: ChangeEvent<
        HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement
      >,
    ) =>
      setForm({
        ...form,
        [key]: ["tuition", "capacity"].includes(key)
          ? Number(e.target.value)
          : e.target.value,
      });
  return (
    <section className="feature-page">
      <Heading
        title={id ? "Chỉnh sửa khóa học" : "Tạo khóa học mới"}
        description="Khóa học mới được lưu dưới dạng bản nháp. Công khai khi bạn đã sẵn sàng."
      />
      <Link to="/teacher/courses">← Danh sách khóa học</Link>
      <form
        className="panel stack"
        onSubmit={(e) => {
          e.preventDefault();
          save.mutate();
        }}
      >
        <label>
          Tên khóa học
          <input
            required
            maxLength={160}
            value={form.title}
            onChange={field("title")}
          />
        </label>
        <label>
          Mô tả
          <textarea
            required
            maxLength={4000}
            rows={5}
            value={form.description}
            onChange={field("description")}
          />
        </label>
        <div className="form-grid">
          <label>
            Môn học
            <input
              required
              maxLength={100}
              value={form.subject}
              onChange={field("subject")}
            />
          </label>
          <label>
            Trình độ
            <input
              required
              maxLength={100}
              value={form.level}
              onChange={field("level")}
            />
          </label>
          <label>
            Khu vực
            <input
              required
              maxLength={160}
              value={form.area}
              onChange={field("area")}
            />
          </label>
          <label>
            Hình thức
            <select value={form.mode} onChange={field("mode")}>
              <option value="OFFLINE">Trực tiếp</option>
              <option value="ONLINE">Trực tuyến</option>
            </select>
          </label>
          <label>
            Học phí toàn khóa (VND)
            <input
              type="number"
              min="0"
              max="999999999999"
              step="1"
              required
              value={form.tuition}
              onChange={field("tuition")}
            />
          </label>
          <label>
            Số chỗ tối đa
            <input
              type="number"
              required
              min="1"
              max="10000"
              value={form.capacity}
              onChange={field("capacity")}
            />
          </label>
          <label>
            Ngày bắt đầu
            <input
              type="date"
              required
              value={form.startDate}
              onChange={field("startDate")}
            />
          </label>
          <label>
            Ngày kết thúc
            <input
              type="date"
              required
              min={form.startDate}
              value={form.endDate}
              onChange={field("endDate")}
            />
          </label>
        </div>
        <label>
          Địa điểm / tên nền tảng học
          <input
            required
            maxLength={500}
            value={form.location}
            onChange={field("location")}
          />
          <small>
            Hiển thị công khai. Không nhập mật khẩu hoặc đường dẫn phòng học
            riêng.
          </small>
        </label>
        <fieldset>
          <legend>Lịch học hằng tuần (giờ Việt Nam)</legend>
          {form.meetings.map((m, i) => (
            <div className="meeting-row" key={i}>
              <label>
                Ngày
                <select
                  aria-label={`Ngày buổi ${i + 1}`}
                  value={m.weekday}
                  onChange={(e) =>
                    setForm({
                      ...form,
                      meetings: form.meetings.map((n, j) =>
                        j === i ? { ...n, weekday: Number(e.target.value) } : n,
                      ),
                    })
                  }
                >
                  {[1, 2, 3, 4, 5, 6, 7].map((n) => (
                    <option key={n} value={n}>
                      {day(n)}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Bắt đầu
                <input
                  type="time"
                  required
                  value={m.startTime}
                  onChange={(e) =>
                    setForm({
                      ...form,
                      meetings: form.meetings.map((n, j) =>
                        j === i ? { ...n, startTime: e.target.value } : n,
                      ),
                    })
                  }
                />
              </label>
              <label>
                Kết thúc
                <input
                  type="time"
                  required
                  value={m.endTime}
                  onChange={(e) =>
                    setForm({
                      ...form,
                      meetings: form.meetings.map((n, j) =>
                        j === i ? { ...n, endTime: e.target.value } : n,
                      ),
                    })
                  }
                />
              </label>
              <button
                type="button"
                className="secondary"
                disabled={form.meetings.length === 1}
                aria-label={`Xóa buổi ${i + 1}`}
                onClick={() =>
                  setForm({
                    ...form,
                    meetings: form.meetings.filter((_, j) => j !== i),
                  })
                }
              >
                Xóa
              </button>
            </div>
          ))}
          <button
            type="button"
            className="secondary"
            disabled={form.meetings.length >= 21}
            onClick={() =>
              setForm({
                ...form,
                meetings: [
                  ...form.meetings,
                  { weekday: 1, startTime: "18:00", endTime: "19:30" },
                ],
              })
            }
          >
            + Thêm buổi học
          </button>
        </fieldset>
        <ErrorNotice error={save.error} />
        <div className="actions">
          <button className="primary" disabled={save.isPending}>
            {save.isPending ? "Đang lưu…" : "Lưu khóa học"}
          </button>
          <Link to="/teacher/courses">Hủy</Link>
        </div>
      </form>
    </section>
  );
}
const nextStatuses: Record<Status, Status[]> = {
  NEW: ["CONTACTED", "CONFIRMED", "REJECTED", "CANCELLED"],
  CONTACTED: ["CONFIRMED", "REJECTED", "CANCELLED"],
  CONFIRMED: ["ATTENDING", "CANCELLED"],
  ATTENDING: ["CANCELLED"],
  CANCELLED: [],
  REJECTED: [],
};
export function Applications({ teacher = false }: { teacher?: boolean }) {
  const [filter, setFilter] = useState("");
  const [course, setCourse] = useState("");
  const key = teacher ? "teacher-applications" : "student-applications";
  const q = useQuery({
    queryKey: [key],
    queryFn: () =>
      api<Application[]>(
        "/api/" + (teacher ? "teacher" : "student") + "/applications",
      ),
  });
  const records = q.data?.filter(
    (a) =>
      (!filter || a.status === filter) &&
      (!course || String(a.course.id) === course),
  );
  const options = [
    ...new Map(q.data?.map((a) => [a.course.id, a.course.title])).entries(),
  ];
  return (
    <section className="feature-page">
      <Heading
        title={teacher ? "Đăng ký & CRM" : "Đăng ký của tôi"}
        description={
          teacher
            ? "Theo dõi người quan tâm và xác nhận học viên."
            : "Theo dõi kết quả đăng ký. Chỉ giáo viên mới xác nhận nhập học."
        }
      />
      {teacher && <TeacherNav />}
      <div className="filters">
        <label>
          Trạng thái
          <select value={filter} onChange={(e) => setFilter(e.target.value)}>
            <option value="">Tất cả</option>
            {Object.entries(statusText).map(([k, v]) => (
              <option value={k} key={k}>
                {v}
              </option>
            ))}
          </select>
        </label>
        <label>
          Khóa học
          <select value={course} onChange={(e) => setCourse(e.target.value)}>
            <option value="">Tất cả</option>
            {options.map(([id, name]) => (
              <option key={id} value={id}>
                {name}
              </option>
            ))}
          </select>
        </label>
      </div>
      <QueryState query={q} />
      {records?.length === 0 && (
        <Empty>
          Chưa có đăng ký phù hợp.{" "}
          {!teacher && <Link to="/courses">Khám phá lớp học →</Link>}
        </Empty>
      )}
      <div className="management-list">
        {records?.map((a) => (
          <ApplicationCard
            key={a.id}
            application={a}
            teacher={teacher}
            queryKey={key}
          />
        ))}
      </div>
    </section>
  );
}
function ApplicationCard({
  application: a,
  teacher,
  queryKey,
}: {
  application: Application;
  teacher: boolean;
  queryKey: string;
}) {
  const [status, setStatus] = useState<Status>(a.status);
  const [note, setNote] = useState("");
  const client = useQueryClient();
  useEffect(() => setStatus(a.status), [a.status]);
  const update = useMutation({
    mutationFn: () =>
      api(
        "/api/" +
          (teacher
            ? "teacher/applications/" + a.id
            : "student/applications/" + a.id + "/cancel"),
        {
          method: "PUT",
          ...(teacher ? { body: JSON.stringify({ status, note }) } : {}),
        },
      ),
    onSuccess: () => {
      setNote("");
      void client.invalidateQueries({ queryKey: [queryKey] });
      void client.invalidateQueries({ queryKey: ["teacher-analytics"] });
    },
  });
  return (
    <article className="panel">
      <div className="card-title">
        <h2>{a.course.title}</h2>
        <span className="pill">{statusText[a.status]}</span>
      </div>
      <p>
        {teacher ? a.studentName : a.course.teacherName} ·{" "}
        {timestamp(a.createdAt)}
      </p>
      {teacher && (
        <>
          <p>
            <a href={"mailto:" + a.email}>{a.email}</a> ·{" "}
            <a href={"tel:" + a.phone}>{a.phone}</a>
          </p>
          <p>Nguồn: {a.source}</p>
          <p className="preserve-lines">{a.message}</p>
        </>
      )}
      {!teacher && (
        <>
          <p>
            {date(a.course.startDate)} – {date(a.course.endDate)} ·{" "}
            {a.course.meetings
              .map((m) => `${day(m.weekday)} ${m.startTime}–${m.endTime}`)
              .join(" · ")}
          </p>
          <p>{a.course.location}</p>
        </>
      )}
      <details>
        <summary>Lịch sử xử lý</summary>
        <ul>
          {a.history.map((h, i) => (
            <li key={i}>
              {timestamp(h.createdAt)} · {statusText[h.status]}{" "}
              {h.actorName && `· ${h.actorName}`}{" "}
              {h.note && <p className="preserve-lines">{h.note}</p>}
            </li>
          ))}
        </ul>
      </details>
      {teacher ? (
        <form
          className="stack"
          onSubmit={(e) => {
            e.preventDefault();
            update.mutate();
          }}
        >
          <label>
            Trạng thái
            <select
              value={status}
              onChange={(e) => setStatus(e.target.value as Status)}
            >
              <option value={a.status}>{statusText[a.status]}</option>
              {nextStatuses[a.status].map((s) => (
                <option key={s} value={s}>
                  {statusText[s]}
                </option>
              ))}
            </select>
          </label>
          <label>
            Ghi chú riêng
            <textarea
              rows={2}
              maxLength={2000}
              value={note}
              onChange={(e) => setNote(e.target.value)}
            />
          </label>
          <button
            className="primary"
            disabled={update.isPending || (status === a.status && !note.trim())}
          >
            Lưu cập nhật
          </button>
        </form>
      ) : (
        ["NEW", "CONTACTED", "CONFIRMED"].includes(a.status) && (
          <button
            className="secondary"
            disabled={update.isPending}
            onClick={() => {
              if (confirm("Bạn muốn hủy đăng ký này?")) update.mutate();
            }}
          >
            Hủy đăng ký
          </button>
        )
      )}
      <ErrorNotice error={update.error} />
    </article>
  );
}
export function TeacherAnalytics() {
  const [range, setRange] = useState("");
  const q = useQuery({
    queryKey: ["teacher-analytics", range],
    queryFn: () => api<Analytics>("/api/teacher/analytics" + range),
  });
  return (
    <section className="feature-page">
      <Heading
        title="Hiệu quả tuyển sinh"
        description="Mặc định 30 ngày gần nhất. Ngày được tính theo giờ Việt Nam."
      />
      <TeacherNav />
      <form
        className="filters"
        onSubmit={(e) => {
          e.preventDefault();
          const f = new FormData(e.currentTarget);
          const p = new URLSearchParams();
          for (const k of ["from", "to"])
            if (f.get(k)) p.set(k, String(f.get(k)));
          setRange("?" + p);
        }}
      >
        <label>
          Từ ngày
          <input type="date" name="from" />
        </label>
        <label>
          Đến ngày
          <input type="date" name="to" />
        </label>
        <button className="primary">Xem thống kê</button>
      </form>
      <QueryState query={q} />
      {q.data && (
        <>
          <div className="stats-grid">
            {[
              ["Lượt xem", q.data.views],
              ["Đăng ký", q.data.applications],
              ["Đã nhận học", q.data.confirmed],
              ["Tỷ lệ chuyển đổi", q.data.conversionRate.toFixed(1) + "%"],
            ].map(([label, value]) => (
              <div className="panel" key={label}>
                <span>{label}</span>
                <strong>{value}</strong>
              </div>
            ))}
          </div>
          <div className="table-scroll">
            <table>
              <caption>Hiệu quả theo nguồn</caption>
              <thead>
                <tr>
                  <th>Nguồn</th>
                  <th>Lượt xem</th>
                  <th>Đăng ký</th>
                  <th>Đã nhận học</th>
                </tr>
              </thead>
              <tbody>
                {q.data.sources.map((s) => (
                  <tr key={s.source}>
                    <td>{s.source}</td>
                    <td>{s.views}</td>
                    <td>{s.applications}</td>
                    <td>{s.confirmed}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            {!q.data.sources.length && (
              <Empty>Chưa có dữ liệu trong khoảng thời gian này.</Empty>
            )}
          </div>
          <p className="fine-print">
            Lượt xem là số lần mở trang giáo viên/khóa học được ghi nhận, không
            phải người xem duy nhất; không ghi lượt của chủ trang đang đăng
            nhập. Đăng ký được nhóm theo ngày gửi và nguồn UTM trên link đăng
            ký. “Đã nhận học” là đăng ký hiện ở trạng thái đã xác nhận hoặc đang
            học; tỷ lệ = đã nhận học / đăng ký cùng nhóm. Dữ liệu có thể bị ảnh
            hưởng bởi bot hoặc chặn theo dõi.
          </p>
        </>
      )}
    </section>
  );
}
export function TeacherShare() {
  const user = useUser().data as User;
  const [source, setSource] = useState("facebook");
  const [copied, setCopied] = useState(false);
  const [failed, setFailed] = useState(false);
  const link =
    location.origin +
    "/teachers/" +
    user.id +
    "?utm_source=" +
    encodeURIComponent(source || "direct");
  return (
    <section className="feature-page">
      <Heading
        title="Trang tuyển sinh của bạn"
        description="Chia sẻ hồ sơ và danh sách khóa học công khai."
      />
      <TeacherNav />
      <div className="panel stack">
        <label>
          Nguồn chia sẻ
          <input
            maxLength={80}
            value={source}
            onChange={(e) => {
              setSource(e.target.value.replace(/[^a-zA-Z0-9_.-]/g, ""));
              setCopied(false);
            }}
            placeholder="facebook, zalo, website"
          />
        </label>
        <label>
          Link tuyển sinh
          <input readOnly value={link} />
        </label>
        <div className="actions">
          <button
            className="primary"
            onClick={async () => {
              try {
                await navigator.clipboard.writeText(link);
                setCopied(true);
                setFailed(false);
              } catch {
                setFailed(true);
              }
            }}
          >
            Sao chép link
          </button>
          <Link className="secondary button-link" to={`/teachers/${user.id}`}>
            Xem trang công khai
          </Link>
        </div>
        {copied && <p role="status">Đã sao chép link.</p>}
        {failed && (
          <p role="alert">
            Không thể sao chép tự động. Bạn hãy chọn và sao chép link phía trên.
          </p>
        )}
        <p>
          Giới thiệu và môn học được lấy từ hồ sơ giáo viên.{" "}
          <Link className="inline-link" to="/teacher/profile/edit">
            Cập nhật hồ sơ
          </Link>
        </p>
        <ShareQR link={link} />
      </div>
    </section>
  );
}
function ShareQR({ link }: { link: string }) {
  const [image, setImage] = useState("");
  useEffect(() => {
    let active = true;
    import("qrcode")
      .then((q) => q.toDataURL(link, { width: 240, margin: 2 }))
      .then((url) => {
        if (active) setImage(url);
      })
      .catch(() => {});
    return () => {
      active = false;
    };
  }, [link]);
  return image ? (
    <div>
      <img width="240" height="240" src={image} alt="Mã QR trang tuyển sinh" />
      <p>
        <a className="inline-link" href={image} download="findmyclass-qr.png">
          Tải mã QR
        </a>
      </p>
    </div>
  ) : null;
}
export function TeacherPlan() {
  const q = useQuery({
    queryKey: ["teacher-plan"],
    queryFn: () => api<Plan>("/api/teacher/plan"),
  });
  return (
    <section className="feature-page">
      <Heading title="Gói dịch vụ" />
      <TeacherNav />
      <QueryState query={q} />
      {q.data && (
        <div className="panel">
          <span className="pill">Gói hiện tại: {q.data.name}</span>
          <h2>{q.data.coursesUsed} khóa học đang hoạt động</h2>
          <p>
            Giới hạn:{" "}
            {q.data.courseLimit < 0
              ? "Chưa áp dụng giới hạn"
              : q.data.courseLimit + " khóa học"}
            . Khóa học lưu trữ không tính vào giới hạn.
          </p>
          <p>
            Các công cụ quản lý khóa học, CRM và thống kê đang có sẵn trong bản
            dùng thử.
          </p>
          <h3>Pro — đang hoàn thiện</h3>
          <p>
            Giá và quyền lợi chưa được công bố. Chưa nhận thanh toán hoặc tự
            động gia hạn.
          </p>
        </div>
      )}
    </section>
  );
}
