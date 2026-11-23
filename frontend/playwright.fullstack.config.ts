import { defineConfig, devices } from "@playwright/test";
export default defineConfig({
  testDir: "./tests/fullstack",
  fullyParallel: false,
  workers: 1,
  timeout: 45000,
  use: {
    baseURL: "http://localhost:5174",
    trace: "retain-on-failure",
    launchOptions: { executablePath: process.env.CHROMIUM_PATH },
  },
  projects: [{ name: "chromium", use: { ...devices["Desktop Chrome"] } }],
  webServer: [
    {
      command: "bash ../scripts/run_browser_backend.sh",
      url: "http://127.0.0.1:8081/api/auth/csrf",
      timeout: 180000,
      reuseExistingServer: false,
    },
    {
      command: "npm run dev -- --port 5174",
      env: { VITE_API_TARGET: "http://localhost:8081" },
      url: "http://localhost:5174",
      reuseExistingServer: false,
    },
  ],
});
