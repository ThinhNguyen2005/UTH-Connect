"use client";

import React, { useState } from "react";
import { PortalLayout } from "@/components/layout/PortalLayout";
import { supabaseBrowserClient } from "@/utils/supabase-client";
import {
  Shield,
  CheckCircle2,
  AlertCircle,
  LogOut,
  Laptop,
} from "lucide-react";

export default function SecuritySettingsPage() {
  const [isSigningOut, setIsSigningOut] = useState(false);
  const [feedback, setFeedback] = useState<string | null>(null);
  const [feedbackType, setFeedbackType] = useState<"success" | "error">("success");
  const feedbackTimerRef = React.useRef<NodeJS.Timeout | null>(null);

  React.useEffect(() => {
    return () => {
      if (feedbackTimerRef.current) clearTimeout(feedbackTimerRef.current);
    };
  }, []);

  const showFeedback = (msg: string, type: "success" | "error" = "success") => {
    if (feedbackTimerRef.current) clearTimeout(feedbackTimerRef.current);
    setFeedback(msg);
    setFeedbackType(type);
    feedbackTimerRef.current = setTimeout(() => {
      setFeedback(null);
    }, 4000);
  };

  const handleSignOutEverywhereElse = async () => {
    setIsSigningOut(true);
    try {
      const { error } = await supabaseBrowserClient.auth.signOut({ scope: "others" });
      if (error) {
        showFeedback(`Lỗi khi đăng xuất: ${error.message}`, "error");
      } else {
        showFeedback("Đã thu hồi phiên và đăng xuất khỏi tất cả các thiết bị khác thành công.", "success");
      }
    } catch {
      showFeedback("Không thể kết nối đến máy chủ xác thực.", "error");
    } finally {
      setIsSigningOut(false);
    }
  };

  return (
    <PortalLayout>
      <section className="mx-auto max-w-2xl px-6 py-12">
        {/* Header */}
        <div className="flex flex-wrap items-end justify-between gap-3">
          <div>
            <h1 className="text-xl font-semibold tracking-tight text-neutral-900">
              Bảo mật tài khoản
            </h1>
            <p className="mt-1 text-sm text-neutral-500">
              Quản lý phiên đăng nhập và bảo mật tài khoản UTH Connect.
            </p>
          </div>
        </div>

        {/* Feedback notification */}
        {feedback && (
          <div
            className={`mt-4 flex items-center gap-2 rounded-xl px-4 py-3 text-xs transition ${
              feedbackType === "success"
                ? "bg-emerald-50 text-emerald-800"
                : "bg-red-50 text-red-800"
            }`}
          >
            {feedbackType === "success" ? (
              <CheckCircle2 className="h-4 w-4 shrink-0 text-emerald-600" />
            ) : (
              <AlertCircle className="h-4 w-4 shrink-0 text-red-600" />
            )}
            <span>{feedback}</span>
          </div>
        )}

        {/* Current Session Card */}
        <div className="mt-6 overflow-hidden rounded-xl bg-white p-6 shadow-sm">
          <div className="flex items-start justify-between gap-4">
            <div className="flex items-center gap-3">
              <span className="grid h-10 w-10 shrink-0 place-items-center rounded-lg bg-neutral-100 text-neutral-700">
                <Laptop className="h-5 w-5 stroke-[1.5]" />
              </span>
              <div>
                <div className="flex items-center gap-2">
                  <p className="text-sm font-medium text-neutral-900">
                    Phiên hiện tại trên trình duyệt này
                  </p>
                  <span className="rounded-full bg-emerald-100 px-2 py-0.5 text-[10px] font-medium text-emerald-700">
                    Đang hoạt động
                  </span>
                </div>
                <p className="mt-0.5 text-xs text-neutral-500">
                  Được bảo vệ bởi Supabase Auth JWT Token & mã hóa TLS.
                </p>
              </div>
            </div>
          </div>
        </div>

        {/* Sign Out Everywhere Else Action */}
        <div className="mt-6 rounded-xl bg-white p-6 shadow-sm">
          <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
            <div className="max-w-md">
              <h2 className="text-sm font-semibold text-neutral-900">
                Đăng xuất khỏi các thiết bị khác
              </h2>
              <p className="mt-1 text-xs text-neutral-500 leading-relaxed">
                Nếu bạn từng đăng nhập trên máy tính công cộng hoặc thiết bị khác, hành động này sẽ thu hồi ngay lập tức Refresh Token ở mọi phiên làm việc khác.
              </p>
            </div>
            <button
              type="button"
              disabled={isSigningOut}
              onClick={handleSignOutEverywhereElse}
              className="inline-flex shrink-0 items-center justify-center gap-2 rounded-lg bg-red-600 px-4 py-2 text-xs font-medium text-white transition hover:bg-red-700 disabled:opacity-50"
            >
              <LogOut className="h-4 w-4 stroke-[1.5]" />
              <span>{isSigningOut ? "Đang xử lý..." : "Đăng xuất nơi khác"}</span>
            </button>
          </div>
        </div>

        {/* Informational Footer Note */}
        <div className="mt-4 flex items-start gap-2.5 rounded-xl bg-neutral-100/70 p-4 text-xs text-neutral-600">
          <Shield className="mt-0.5 h-4 w-4 shrink-0 text-neutral-500" />
          <p className="leading-relaxed">
            Hệ thống UTH Connect áp dụng kiến trúc phân quyền RBAC và Row Level Security (RLS). Khi thu hồi phiên, các thiết bị khác sẽ bị chặn truy cập dữ liệu ngay lập tức trong lần gửi request tiếp theo.
          </p>
        </div>
      </section>
    </PortalLayout>
  );
}
