export type Role = "TEACHER" | "STUDENT";
export type User = {
  id: number;
  email: string;
  displayName: string;
  role: Role | null;
  teacherProfileComplete: boolean;
  nextPath: string;
};

export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
  ) {
    super(message);
  }
}

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  if (init.body) headers.set("Content-Type", "application/json");
  if (
    init.method &&
    !["GET", "HEAD", "OPTIONS"].includes(init.method.toUpperCase())
  ) {
    // Fetch a fresh token: authentication and logout rotate the CSRF token.
    const response = await fetch("/api/auth/csrf", {
      credentials: "same-origin",
    });
    if (!response.ok)
      throw new ApiError(
        response.status,
        "Không thể xác thực yêu cầu. Vui lòng thử lại.",
      );
    const csrf: { headerName: string; token: string } = await response.json();
    headers.set(csrf.headerName, csrf.token);
  }
  const response = await fetch(path, {
    ...init,
    headers,
    credentials: "same-origin",
  });
  if (!response.ok) {
    const problem = await response.json().catch(() => ({}));
    throw new ApiError(
      response.status,
      problem.detail ||
        (response.status === 401
          ? "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại."
          : response.status === 403
            ? "Bạn không có quyền thực hiện thao tác này."
            : "Đã xảy ra lỗi. Vui lòng thử lại."),
    );
  }
  return response.status === 204 ? (undefined as T) : response.json();
}
