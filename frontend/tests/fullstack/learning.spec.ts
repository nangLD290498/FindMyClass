import { test, expect, type BrowserContext, type Page } from "@playwright/test";
async function login(
  context: BrowserContext,
  subject: string,
  role: "TEACHER" | "STUDENT",
) {
  const tokenResponse = await context.request.get(
    "http://localhost:8081/api/auth/csrf",
  );
  expect(tokenResponse.ok()).toBeTruthy();
  const csrf = await tokenResponse.json();
  const r = await context.request.post(
    `http://localhost:8081/__test/login?subject=${subject}&role=${role}`,
    { headers: { [csrf.headerName]: csrf.token } },
  );
  expect(r.ok()).toBeTruthy();
  return r.json();
}
async function csrfPost(context: BrowserContext, path: string, data: unknown) {
  const r = await context.request.get("/api/auth/csrf");
  const token = await r.json();
  return context.request.post(path, {
    headers: { [token.headerName]: token.token },
    data,
  });
}
async function shot(page: Page, name: string) {
  await page.screenshot({
    path: `test-results/screenshots/${name}.png`,
    fullPage: true,
  });
}

test("teacher publishes, student finds/applies, teacher manages CRM, student sees confirmation", async ({
  browser,
}) => {
  const suffix = Date.now().toString();
  const teacherContext = await browser.newContext({
    baseURL: "http://localhost:5174",
  });
  const teacher = await login(
    teacherContext,
    "test-teacher-" + suffix,
    "TEACHER",
  );
  const t = await teacherContext.newPage();
  await t.goto("/teacher/courses");
  await t.getByRole("link", { name: "+ Tạo khóa học" }).click();
  await t.getByLabel("Tên khóa học").fill("Toán lớp 10 — nền tảng vững chắc");
  await t
    .getByLabel("Mô tả", { exact: true })
    .fill("Ôn tập đại số, hình học và phương pháp giải bài tập.");
  await t.getByLabel("Môn học", { exact: true }).fill("Toán");
  await t.getByLabel("Trình độ").fill("Lớp 10");
  await t.getByLabel("Khu vực", { exact: true }).fill("Quận 3, TP. HCM");
  await t.getByLabel("Học phí toàn khóa").fill("1200000");
  await t.getByLabel("Số chỗ tối đa").fill("2");
  await t.getByLabel("Ngày bắt đầu").fill("2030-01-01");
  await t.getByLabel("Ngày kết thúc").fill("2030-06-01");
  await t
    .getByLabel("Địa điểm / tên nền tảng học")
    .fill("Trung tâm học tập Quận 3");
  await shot(t, "01-course-editor");
  await t.getByRole("button", { name: "Lưu khóa học", exact: true }).click();
  await expect(t).toHaveURL(/\/teacher\/courses$/);
  await expect(t.getByText("Bản nháp", { exact: true })).toBeVisible();
  await t.getByRole("button", { name: "Công khai", exact: true }).click();
  await expect(t.getByText("Công khai", { exact: true })).toBeVisible();
  await shot(t, "02-teacher-courses");
  const studentContext = await browser.newContext({
    baseURL: "http://localhost:5174",
  });
  await login(studentContext, "test-student-" + suffix, "STUDENT");
  const s = await studentContext.newPage();
  await s.goto(`/teachers/${teacher.id}?utm_source=zalo`);
  await expect(
    s.getByText("Giáo viên Toán với 10 năm kinh nghiệm."),
  ).toBeVisible();
  await shot(s, "03-public-teacher");
  await s
    .getByRole("link", {
      name: "Toán lớp 10 — nền tảng vững chắc",
      exact: true,
    })
    .click();
  await expect(s).toHaveURL(/utm_source=zalo/);
  await s.getByLabel("Số điện thoại", { exact: true }).fill("0901234567");
  await s.getByLabel("Lời nhắn").fill("Em muốn tìm hiểu khóa học.");
  await s.getByLabel("Tôi đồng ý chia sẻ").check();
  await shot(s, "04-course-application");
  await s.getByRole("button", { name: "Gửi đăng ký", exact: true }).click();
  await expect(
    s.getByRole("heading", { name: "Đã gửi đăng ký!" }),
  ).toBeVisible();
  await t.goto("/teacher/applications");
  await expect(t.getByText("Minh Anh", { exact: false }).first()).toBeVisible();
  await t
    .getByRole("article")
    .getByRole("combobox", { name: "Trạng thái", exact: true })
    .selectOption("CONFIRMED");
  await t
    .getByLabel("Ghi chú riêng")
    .fill("Đã gọi điện tư vấn, ghi chú riêng.");
  await t.getByRole("button", { name: "Lưu cập nhật" }).click();
  await expect(t.locator(".card-title .pill")).toHaveText("Đã xác nhận");
  await t.getByText("Lịch sử xử lý", { exact: true }).click();
  await expect(
    t.getByText("Đã gọi điện tư vấn, ghi chú riêng.", { exact: true }),
  ).toBeVisible();
  await shot(t, "05-teacher-crm");
  await s.goto("/student/applications");
  await expect(s.locator(".card-title .pill")).toHaveText("Đã xác nhận");
  await s.getByText("Lịch sử xử lý", { exact: true }).click();
  await expect(s.getByText("Đã gọi điện tư vấn, ghi chú riêng.")).toHaveCount(
    0,
  );
  await shot(s, "06-student-applications");
  await s.reload();
  await expect(s.locator(".card-title .pill")).toHaveText("Đã xác nhận");
  await t.goto("/teacher/analytics");
  await expect(
    t.getByRole("cell", { name: "zalo", exact: true }),
  ).toBeVisible();
  await shot(t, "07-analytics");
  await t.goto("/teacher/share");
  await expect(
    t.getByRole("img", { name: "Mã QR trang tuyển sinh" }),
  ).toBeVisible();
  await shot(t, "08-share-qr");
  await t.getByRole("link", { name: "Cập nhật hồ sơ", exact: true }).click();
  await expect(t.getByLabel("Giới thiệu về bạn")).toHaveValue(
    "Giáo viên Toán với 10 năm kinh nghiệm.",
  );
  await t
    .getByLabel("Giới thiệu về bạn")
    .fill("Giáo viên Toán, hỗ trợ học viên từ nền tảng.");
  await t.getByRole("button", { name: "Lưu hồ sơ", exact: true }).click();
  await expect(t).toHaveURL(/\/teacher\/share$/);
  await t.getByRole("link", { name: "Gói dịch vụ", exact: true }).click();
  await expect(
    t.getByText("Gói hiện tại: Free", { exact: true }),
  ).toBeVisible();

  await s.goto("/courses");
  await s.getByLabel("Môn học", { exact: true }).fill("Toán");
  await s.getByLabel("Học phí đến").fill("100");
  await s.getByRole("button", { name: "Tìm lớp", exact: true }).click();
  await expect(
    s.getByText("Chưa có lớp phù hợp.", { exact: false }),
  ).toBeVisible();
  await s.getByRole("button", { name: "Xóa bộ lọc" }).click();
  await expect(
    s.getByRole("link", {
      name: "Toán lớp 10 — nền tảng vững chắc",
      exact: true,
    }),
  ).toBeVisible();
  await shot(s, "09-marketplace");
  await s.setViewportSize({ width: 375, height: 812 });
  await expect(
    s.getByRole("button", { name: "Tìm lớp", exact: true }),
  ).toBeVisible();
  expect(
    await s.evaluate(() => document.documentElement.scrollWidth <= innerWidth),
  ).toBe(true);
  await shot(s, "10-mobile-marketplace");
  await teacherContext.close();
  await studentContext.close();
});

test("public browsing displays sign-in and redirects private teacher pages", async ({
  page,
  context,
  browser,
}) => {
  const suffix = Date.now().toString();
  const fixture = await login(context, "test-public-" + suffix, "TEACHER");
  const course = await csrfPost(context, "/api/teacher/courses", {
    title: "Lớp riêng",
    description: "Chưa công khai",
    subject: "Toán",
    level: "Cơ bản",
    area: "Hà Nội",
    location: "Trung tâm",
    mode: "OFFLINE",
    tuition: 100,
    capacity: 2,
    startDate: "2030-01-01",
    endDate: "2030-06-01",
    meetings: [{ weekday: 1, startTime: "18:00", endTime: "19:00" }],
  });
  expect(course.ok()).toBeTruthy();
  const saved = await course.json();
  const token = await (await context.request.get("/api/auth/csrf")).json();
  expect(
    (
      await context.request.put(
        "/api/teacher/courses/" + saved.id + "/status",
        {
          headers: { [token.headerName]: token.token },
          data: { status: "PUBLISHED" },
        },
      )
    ).ok(),
  ).toBeTruthy();
  const teacherState = await context.storageState();
  await context.clearCookies();
  await page.goto("/courses/" + saved.id);
  await expect(
    page.getByRole("link", { name: "Đăng nhập để đăng ký" }),
  ).toBeVisible();
  await page.getByRole("link", { name: "← Tìm lớp", exact: true }).click();
  const manager = await browser.newContext({
    baseURL: "http://localhost:5174",
    storageState: teacherState,
  });
  const hideToken = await (await manager.request.get("/api/auth/csrf")).json();
  expect(
    (
      await manager.request.put(
        "/api/teacher/courses/" + saved.id + "/status",
        {
          headers: { [hideToken.headerName]: hideToken.token },
          data: { status: "HIDDEN" },
        },
      )
    ).ok(),
  ).toBeTruthy();
  await page.goBack();
  await expect(page.getByRole("alert")).toContainText(
    "Khóa học chưa công khai hoặc đã ẩn.",
  );
  await expect(
    page.getByRole("link", { name: "Đăng nhập để đăng ký" }),
  ).toHaveCount(0);
  await manager.close();
  await page.goto("/teacher/courses");
  await expect(page).toHaveURL(/\/login$/);
  expect(fixture.id).toBeGreaterThan(0);
});
