import { useEffect, useState, type ReactNode } from "react";
import { Navigate, Route, Routes, useLocation } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { api, ApiError, type Role, type User } from "./api";

function ArrowIcon() {
  return (
    <svg
      aria-hidden="true"
      width="18"
      height="18"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
    >
      <path d="M6 18 18 6M6 6h12v12" />
    </svg>
  );
}
function GoogleIcon() {
  return (
    <svg aria-hidden="true" width="20" height="20" viewBox="0 0 48 48">
      <path
        fill="#4285F4"
        d="M43.61 24.46c0-1.36-.12-2.65-.35-3.9H24v7.74h11a9.4 9.4 0 0 1-4.08 6.17v5.12h6.6c3.86-3.56 6.09-8.8 6.09-15.13Z"
      />
      <path
        fill="#34A853"
        d="M24 44c5.51 0 10.13-1.83 13.51-4.96l-6.6-5.12c-1.83 1.23-4.18 1.97-6.91 1.97-5.32 0-9.84-3.59-11.46-8.42H5.73v5.28A20 20 0 0 0 24 44Z"
      />
      <path
        fill="#FBBC05"
        d="M12.54 27.47A12 12 0 0 1 11.9 24c0-1.2.22-2.37.64-3.47v-5.28H5.73A20 20 0 0 0 4 24c0 3.22.77 6.27 1.73 8.75l6.81-5.28Z"
      />
      <path
        fill="#EA4335"
        d="M24 12.11c3 0 5.69 1.03 7.81 3.05l5.86-5.86C34.12 5.99 29.51 4 24 4A20 20 0 0 0 5.73 15.25l6.81 5.28C14.16 15.7 18.68 12.11 24 12.11Z"
      />
    </svg>
  );
}

function useUser() {
  return useQuery({
    queryKey: ["me"],
    queryFn: async () => {
      try {
        return await api<User>("/api/auth/me");
      } catch (error) {
        if (error instanceof ApiError && error.status === 401) return null;
        throw error;
      }
    },
  });
}

function ErrorNotice({ error }: { error: Error | null }) {
  const client = useQueryClient();
  useEffect(() => {
    if (error instanceof ApiError && error.status === 401)
      client.setQueryData(["me"], null);
  }, [error, client]);
  return error ? (
    <p className="notice error" role="alert">
      {error instanceof ApiError
        ? error.message
        : "Không thể kết nối đến máy chủ. Vui lòng thử lại."}
    </p>
  ) : null;
}

function Frame({ children }: { children: ReactNode }) {
  const user = useUser().data;
  const client = useQueryClient();
  const logout = useMutation({
    mutationFn: () => api<void>("/api/auth/logout", { method: "POST" }),
    onSuccess: () => {
      client.clear();
      client.setQueryData(["me"], null);
      window.location.assign("/login");
    },
  });
  return (
    <div className="app">
      <header>
        <a className="brand" href="/">
          <span className="brand-icon">f.</span>FindMyClass
        </a>
        {user && (
          <div className="account">
            <span>{user.displayName}</span>
            <button
              className="text-button"
              onClick={() => logout.mutate()}
              disabled={logout.isPending}
            >
              Đăng xuất
            </button>
          </div>
        )}
      </header>
      <ErrorNotice error={logout.error} />
      <main>{children}</main>
      <footer>Khơi dậy đam mê. Mở ra cơ hội.</footer>
    </div>
  );
}

function Login() {
  const query = useUser();
  const location = useLocation();
  if (query.isPending) return <Loading />;
  if (query.error)
    return <Retry error={query.error} retry={() => void query.refetch()} />;
  if (query.data) return <Navigate to={query.data.nextPath} replace />;
  return (
    <div className="login-layout">
      <section className="intro">
        <p className="eyebrow">HÀNH TRÌNH MỚI BẮT ĐẦU TỪ ĐÂY</p>
        <h1>
          Điều tốt đẹp
          <br />
          bắt đầu từ
          <br />
          <em>việc học.</em>
        </h1>
        <p className="intro-copy">
          Chia sẻ kiến thức. Khám phá đam mê.
          <br />
          Cùng kết nối, học hỏi và phát triển mỗi ngày.
        </p>
        <div className="illustration" aria-hidden="true">
          <div className="sun" />
          <div className="book book-one">Một góc nhìn mới</div>
          <div className="book book-two">Một tương lai tươi sáng</div>
          <div className="plant">✳</div>
        </div>
        <span className="caption">DÀNH CHO NGƯỜI HAM HỌC. DÀNH CHO BẠN.</span>
      </section>
      <section className="card login-card">
        <span className="pill">CÙNG BẮT ĐẦU NÀO</span>
        <h2>
          Chào mừng đến với
          <br />
          FindMyClass
        </h2>
        <p>Một lần đăng nhập. Muôn vàn cơ hội.</p>
        {new URLSearchParams(location.search).has("error") && (
          <p role="alert" className="notice error">
            Đăng nhập bằng Google chưa thành công. Vui lòng thử lại.
          </p>
        )}
        <a className="google-button" href="/oauth2/authorization/google">
          <GoogleIcon />
          Tiếp tục với Google
          <ArrowIcon />
        </a>
        <p className="fine-print">
          Bạn mới tham gia? Sau khi đăng nhập, hãy chọn vai trò giáo viên hoặc
          học viên.
        </p>
        <div className="card-bottom">
          <span className="tiny-dot" /> Không gian kết nối giáo viên và học viên
        </div>
      </section>
    </div>
  );
}

function Loading() {
  return (
    <section className="center-card" role="status">
      Đang tải…
    </section>
  );
}
function Retry({ error, retry }: { error: Error; retry: () => void }) {
  return (
    <section className="center-card">
      <ErrorNotice error={error} />
      <button className="primary" onClick={retry}>
        Thử lại
      </button>
    </section>
  );
}

function Protected({ children, path }: { children: ReactNode; path: string }) {
  const query = useUser();
  if (query.isPending) return <Loading />;
  if (query.error)
    return <Retry error={query.error} retry={() => void query.refetch()} />;
  if (!query.data) return <Navigate to="/login" replace />;
  if (query.data.nextPath !== path)
    return <Navigate to={query.data.nextPath} replace />;
  return children;
}

function ChooseRole() {
  const [role, setRole] = useState<Role | null>(null);
  const client = useQueryClient();
  const mutation = useMutation({
    mutationFn: (choice: Role) =>
      api<User>("/api/onboarding/role", {
        method: "POST",
        body: JSON.stringify({ role: choice }),
      }),
    onSuccess: (user) => {
      client.setQueryData(["me"], user);
    },
    onError: (error) => {
      if (error instanceof ApiError && error.status === 409)
        void client.invalidateQueries({ queryKey: ["me"] });
    },
  });
  return (
    <section className="onboarding">
      <p className="eyebrow">BẮT ĐẦU HÀNH TRÌNH CỦA BẠN</p>
      <h1>Bạn muốn tham gia với vai trò nào?</h1>
      <p>Chọn vai trò phù hợp để bắt đầu cùng FindMyClass.</p>
      <div className="role-options">
        <button
          className={"role-card " + (role === "TEACHER" ? "selected" : "")}
          aria-pressed={role === "TEACHER"}
          onClick={() => setRole("TEACHER")}
          disabled={mutation.isPending}
        >
          <span className="role-icon" aria-hidden="true">
            ✳
          </span>
          <strong>Tôi là giáo viên</strong>
          <span>Chia sẻ kiến thức và truyền cảm hứng cho học viên.</span>
        </button>
        <button
          className={"role-card " + (role === "STUDENT" ? "selected" : "")}
          aria-pressed={role === "STUDENT"}
          onClick={() => setRole("STUDENT")}
          disabled={mutation.isPending}
        >
          <span className="role-icon" aria-hidden="true">
            <ArrowIcon />
          </span>
          <strong>Tôi là học viên</strong>
          <span>Khám phá điều bạn yêu thích và tìm khóa học phù hợp.</span>
        </button>
      </div>
      <p className="fine-print">
        Vai trò được chọn một lần và không thể thay đổi tại đây sau này.
      </p>
      <ErrorNotice error={mutation.error} />
      <button
        className="primary"
        disabled={!role || mutation.isPending}
        onClick={() => role && mutation.mutate(role)}
      >
        {mutation.isPending ? "Đang thiết lập tài khoản…" : "Tiếp tục"}
      </button>
    </section>
  );
}

const profileSchema = z.object({
  bio: z
    .string()
    .trim()
    .min(1, "Vui lòng giới thiệu đôi nét về bạn.")
    .max(2000, "Phần giới thiệu không được vượt quá 2.000 ký tự."),
  subjects: z
    .string()
    .trim()
    .min(1, "Vui lòng nhập ít nhất một môn học.")
    .max(500, "Danh sách môn học không được vượt quá 500 ký tự."),
});
type ProfileFields = z.infer<typeof profileSchema>;
function TeacherProfile() {
  const client = useQueryClient();
  const form = useForm<ProfileFields>({ resolver: zodResolver(profileSchema) });
  const mutation = useMutation({
    mutationFn: (fields: ProfileFields) =>
      api<User>("/api/teacher/profile", {
        method: "PUT",
        body: JSON.stringify(fields),
      }),
    onSuccess: (user) => client.setQueryData(["me"], user),
  });
  return (
    <section className="center-card profile-card">
      <p className="eyebrow">GIỚI THIỆU BẢN THÂN</p>
      <h1>Chia sẻ câu chuyện của bạn.</h1>
      <p>Hoàn thiện hồ sơ để bắt đầu hành trình giảng dạy của bạn.</p>
      <form onSubmit={form.handleSubmit((fields) => mutation.mutate(fields))}>
        <label htmlFor="bio">Giới thiệu về bạn</label>
        <textarea
          id="bio"
          rows={5}
          maxLength={2000}
          aria-invalid={!!form.formState.errors.bio}
          aria-describedby="bio-error"
          placeholder="Kinh nghiệm, phong cách giảng dạy và điều truyền cảm hứng cho bạn…"
          {...form.register("bio")}
        />
        <p id="bio-error" className="field-error">
          {form.formState.errors.bio?.message}
        </p>
        <label htmlFor="subjects">Môn học bạn giảng dạy</label>
        <input
          id="subjects"
          maxLength={500}
          aria-invalid={!!form.formState.errors.subjects}
          aria-describedby="subjects-error"
          placeholder="Ví dụ: Toán, Tiếng Anh"
          {...form.register("subjects")}
        />
        <p id="subjects-error" className="field-error">
          {form.formState.errors.subjects?.message}
        </p>
        <ErrorNotice error={mutation.error} />
        <button className="primary" disabled={mutation.isPending}>
          {mutation.isPending ? "Đang lưu hồ sơ…" : "Hoàn tất hồ sơ"}
        </button>
      </form>
    </section>
  );
}

function Dashboard({ role }: { role: "teacher" | "student" }) {
  const user = useUser().data!;
  const query = useQuery({
    queryKey: ["dashboard", role],
    queryFn: () => api<{ message: string }>(`/api/${role}/dashboard`),
  });
  if (query.isPending) return <Loading />;
  if (query.error)
    return <Retry error={query.error} retry={() => void query.refetch()} />;
  return (
    <section className="center-card dashboard">
      <span className="pill">
        {role === "teacher" ? "TRANG GIÁO VIÊN" : "TRANG HỌC VIÊN"}
      </span>
      <h1>Xin chào, {user.displayName}!</h1>
      <p>{query.data.message}</p>
      <div className="empty-state">
        <span aria-hidden="true">✳</span>
        <h2>
          {role === "teacher"
            ? "Hành trình giảng dạy bắt đầu từ đây."
            : "Sẵn sàng khám phá kiến thức mới."}
        </h2>
        <p>
          {role === "teacher"
            ? "Tính năng tạo khóa học sẽ được bổ sung trong bản cập nhật sắp tới."
            : "Tính năng tìm kiếm và đăng ký khóa học sẽ được bổ sung trong bản cập nhật sắp tới."}
        </p>
      </div>
    </section>
  );
}

function Landing() {
  const query = useUser();
  if (query.isPending) return <Loading />;
  if (query.error)
    return <Retry error={query.error} retry={() => void query.refetch()} />;
  return <Navigate to={query.data?.nextPath ?? "/login"} replace />;
}
export function App() {
  return (
    <Frame>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/auth/callback" element={<Landing />} />
        <Route
          path="/choose-role"
          element={
            <Protected path="/choose-role">
              <ChooseRole />
            </Protected>
          }
        />
        <Route
          path="/teacher/profile"
          element={
            <Protected path="/teacher/profile">
              <TeacherProfile />
            </Protected>
          }
        />
        <Route
          path="/teacher"
          element={
            <Protected path="/teacher">
              <Dashboard role="teacher" />
            </Protected>
          }
        />
        <Route
          path="/student"
          element={
            <Protected path="/student">
              <Dashboard role="student" />
            </Protected>
          }
        />
        <Route path="*" element={<Landing />} />
      </Routes>
    </Frame>
  );
}
