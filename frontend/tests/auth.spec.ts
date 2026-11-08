import { test, expect, type Page } from "@playwright/test";

type User = {
  id: number;
  email: string;
  displayName: string;
  role: "TEACHER" | "STUDENT" | null;
  teacherProfileComplete: boolean;
  nextPath: string;
};
const newUser = (): User => ({
  id: 1,
  email: "learner@example.com",
  displayName: "Minh Anh",
  role: null,
  teacherProfileComplete: false,
  nextPath: "/choose-role",
});

// UI contracts are mocked here; real backend permissions/persistence are covered by AuthFlowTest.
async function backend(
  page: Page,
  initial: User | null,
  options: { failRole?: boolean; expireRole?: boolean; failMe?: boolean } = {},
) {
  let user = initial;
  const mutations: string[] = [];
  await page.route("**/api/**", async (route) => {
    const request = route.request(),
      path = new URL(request.url()).pathname;
    const fulfill = (status: number, json: unknown) =>
      route.fulfill({ status, json });
    if (path === "/api/auth/csrf")
      return fulfill(200, { headerName: "X-CSRF-TOKEN", token: "test-token" });
    if (path === "/api/auth/me")
      return fulfill(options.failMe ? 500 : user ? 200 : 401, user ?? {});
    if (request.method() !== "GET") {
      expect(request.headers()["x-csrf-token"]).toBe("test-token");
      mutations.push(path);
    }
    if (path === "/api/onboarding/role") {
      if (options.expireRole) {
        user = null;
        return fulfill(401, {});
      }
      if (options.failRole)
        return fulfill(500, {
          detail: "Không thể lưu vai trò. Thử lại.",
        });
      const role = request.postDataJSON().role;
      user = {
        ...user!,
        role,
        nextPath: role === "TEACHER" ? "/teacher/profile" : "/student",
      };
      return fulfill(200, user);
    }
    if (path === "/api/teacher/profile") {
      expect(request.postDataJSON()).toEqual({
        bio: "Tôi giảng dạy môn Toán.",
        subjects: "Toán",
      });
      user = { ...user!, teacherProfileComplete: true, nextPath: "/teacher" };
      return fulfill(200, user);
    }
    if (path === "/api/auth/logout") {
      user = null;
      return route.fulfill({ status: 204 });
    }
    if (path.endsWith("/dashboard"))
      return fulfill(200, { message: "Tài khoản của bạn đã sẵn sàng." });
    return fulfill(404, {});
  });
  return mutations;
}

test("login offers Google sign-in and explains first-time role choice", async ({
  page,
}) => {
  await backend(page, null);
  await page.goto("/login");
  await expect(
    page.getByRole("heading", { name: "Chào mừng đến với FindMyClass" }),
  ).toBeVisible();
  await expect(
    page.getByRole("link", { name: /Tiếp tục với Google/ }),
  ).toHaveAttribute("href", "/oauth2/authorization/google");
  await page.goto("/teacher");
  await expect(page).toHaveURL(/\/login$/);
});

test("first login lets a student choose once and restores their dashboard on refresh", async ({
  page,
}) => {
  const mutations = await backend(page, newUser());
  await page.goto("/auth/callback");
  await expect(page).toHaveURL(/\/choose-role$/);
  await expect(
    page.getByRole("button", { name: "Tiếp tục", exact: true }),
  ).toBeDisabled();
  await page.getByRole("button", { name: /Tôi là học viên/ }).click();
  await page.getByRole("button", { name: "Tiếp tục", exact: true }).click();
  await expect(page).toHaveURL(/\/student$/);
  await expect(page.getByText("TRANG HỌC VIÊN", { exact: true })).toBeVisible();
  await page.reload();
  await expect(
    page.getByRole("heading", { name: "Xin chào, Minh Anh!" }),
  ).toBeVisible();
  await page.goto("/choose-role");
  await expect(page).toHaveURL(/\/student$/);
  expect(mutations.filter((p) => p === "/api/onboarding/role")).toHaveLength(1);
});

test("teacher completes profile before reaching dashboard", async ({
  page,
}) => {
  await backend(page, newUser());
  await page.goto("/auth/callback");
  await page.getByRole("button", { name: /Tôi là giáo viên/ }).click();
  await page.getByRole("button", { name: "Tiếp tục", exact: true }).click();
  await expect(page).toHaveURL(/\/teacher\/profile$/);
  await page.getByRole("button", { name: "Hoàn tất hồ sơ" }).click();
  await expect(
    page.getByText("Vui lòng giới thiệu đôi nét về bạn."),
  ).toBeVisible();
  await page.getByLabel("Giới thiệu về bạn").fill("Tôi giảng dạy môn Toán.");
  await page.getByLabel("Môn học bạn giảng dạy").fill("Toán");
  await page.getByRole("button", { name: "Hoàn tất hồ sơ" }).click();
  await expect(page).toHaveURL(/\/teacher$/);
  await expect(
    page.getByText("TRANG GIÁO VIÊN", { exact: true }),
  ).toBeVisible();
});

test("returning teacher skips selection and wrong-role routes redirect", async ({
  page,
}) => {
  await backend(page, {
    ...newUser(),
    role: "TEACHER",
    teacherProfileComplete: true,
    nextPath: "/teacher",
  });
  await page.goto("/auth/callback");
  await expect(page).toHaveURL(/\/teacher$/);
  await page.goto("/student");
  await expect(page).toHaveURL(/\/teacher$/);
});

test("incomplete returning teacher resumes profile", async ({ page }) => {
  await backend(page, {
    ...newUser(),
    role: "TEACHER",
    nextPath: "/teacher/profile",
  });
  await page.goto("/teacher");
  await expect(page).toHaveURL(/\/teacher\/profile$/);
});

test("failed save displays error and keeps selection available", async ({
  page,
}) => {
  await backend(page, newUser(), { failRole: true });
  await page.goto("/choose-role");
  await page.getByRole("button", { name: /Tôi là học viên/ }).click();
  await page.getByRole("button", { name: "Tiếp tục", exact: true }).click();
  await expect(page.getByRole("alert")).toContainText("Không thể lưu vai trò.");
  await expect(page).toHaveURL(/\/choose-role$/);
  await expect(
    page.getByRole("button", { name: "Tiếp tục", exact: true }),
  ).toBeEnabled();
});

test("expired session during role selection returns to login", async ({
  page,
}) => {
  await backend(page, newUser(), { expireRole: true });
  await page.goto("/choose-role");
  await page.getByRole("button", { name: /Tôi là học viên/ }).click();
  await page.getByRole("button", { name: "Tiếp tục", exact: true }).click();
  await expect(page).toHaveURL(/\/login$/);
});

test("logout returns to login and protected pages remain inaccessible", async ({
  page,
}) => {
  await backend(page, { ...newUser(), role: "STUDENT", nextPath: "/student" });
  await page.goto("/student");
  await page.getByRole("button", { name: "Đăng xuất" }).click();
  await expect(page).toHaveURL(/\/login$/);
  await page.goto("/student");
  await expect(page).toHaveURL(/\/login$/);
});

test("Google error is visible and login fits mobile screen", async ({
  page,
}) => {
  await backend(page, null);
  await page.setViewportSize({ width: 375, height: 812 });
  await page.goto("/login?error=google");
  await expect(page.getByRole("alert")).toContainText(
    "Đăng nhập bằng Google chưa thành công",
  );
  await expect(
    page.getByRole("link", { name: /Tiếp tục với Google/ }),
  ).toBeVisible();
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= window.innerWidth,
    ),
  ).toBe(true);
});

test("backend outage is shown instead of pretending the user is logged out", async ({
  page,
}) => {
  await backend(page, null, { failMe: true });
  await page.goto("/login");
  await expect(page.getByRole("alert")).toBeVisible();
  await expect(page.getByRole("button", { name: "Thử lại" })).toBeVisible();
  await expect(
    page.getByRole("link", { name: /Tiếp tục với Google/ }),
  ).toHaveCount(0);
});
