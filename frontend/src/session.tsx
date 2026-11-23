import { useEffect, type ReactNode } from "react";
import { Navigate } from "react-router-dom";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { api, ApiError, type User } from "./api";
export function useUser() {
  return useQuery({
    queryKey: ["me"],
    queryFn: async () => {
      try {
        return await api<User>("/api/auth/me");
      } catch (e) {
        if (e instanceof ApiError && e.status === 401) return null;
        throw e;
      }
    },
  });
}
export function ErrorNotice({ error }: { error: Error | null }) {
  const c = useQueryClient();
  useEffect(() => {
    if (error instanceof ApiError && error.status === 401)
      c.setQueryData(["me"], null);
  }, [error, c]);
  return error ? (
    <p className="notice error" role="alert">
      {error.message || "Không thể kết nối đến máy chủ."}
    </p>
  ) : null;
}
export function Loading() {
  return (
    <section className="center-card" role="status">
      Đang tải…
    </section>
  );
}
export function Retry({ error, retry }: { error: Error; retry: () => void }) {
  return (
    <section className="center-card">
      <ErrorNotice error={error} />
      <button className="primary" onClick={retry}>
        Thử lại
      </button>
    </section>
  );
}
export function Protected({
  children,
  path,
}: {
  children: ReactNode;
  path: string;
}) {
  const q = useUser();
  if (q.isPending) return <Loading />;
  if (q.error) return <Retry error={q.error} retry={() => void q.refetch()} />;
  if (!q.data) return <Navigate to="/login" replace />;
  if (q.data.nextPath !== path)
    return <Navigate to={q.data.nextPath} replace />;
  return children;
}
