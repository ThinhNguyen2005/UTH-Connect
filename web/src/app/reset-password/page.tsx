"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";
import Image from "next/image";
import { useRouter } from "next/navigation";
import { supabaseBrowserClient } from "@/utils/supabase-client";
import {
  ShieldAlert,
  CheckCircle2,
  Eye,
  EyeOff,
  Loader2,
  Lock,
  ArrowLeft,
} from "lucide-react";

export default function ResetPasswordPage() {
  const router = useRouter();
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [hasSession, setHasSession] = useState(false);
  const [isCheckingSession, setIsCheckingSession] = useState(true);

  useEffect(() => {
    let isMounted = true;

    // 1. Kiểm tra hash URL có lỗi từ Supabase không
    if (typeof window !== "undefined") {
      const hash = window.location.hash.substring(1);
      const hashParams = new URLSearchParams(hash);
      const error = hashParams.get("error");
      const errorCode = hashParams.get("error_code");
      const errorDescription = hashParams.get("error_description");

      if (error || errorCode) {
        if (errorCode === "otp_expired") {
          setErrorMessage(
            "Liên kết đặt lại mật khẩu đã hết hạn hoặc đã được sử dụng trước đó. Vui lòng gửi lại yêu cầu mới."
          );
        } else {
          setErrorMessage(
            decodeURIComponent(errorDescription || "Liên kết đặt lại mật khẩu không hợp lệ.")
          );
        }
        setIsCheckingSession(false);
        return;
      }
    }

    // 2. Lắng nghe trạng thái phiên phục hồi (PASSWORD_RECOVERY)
    const { data: authListener } = supabaseBrowserClient.auth.onAuthStateChange(
      async (event, session) => {
        if (!isMounted) return;
        if (event === "PASSWORD_RECOVERY" || session) {
          setHasSession(true);
          setErrorMessage(null);
        }
        setIsCheckingSession(false);
      }
    );

    // 3. Kiểm tra phiên hiện tại
    supabaseBrowserClient.auth.getSession().then(({ data: { session } }) => {
      if (!isMounted) return;
      if (session) {
        setHasSession(true);
      }
      setIsCheckingSession(false);
    });

    return () => {
      isMounted = false;
      authListener.subscription.unsubscribe();
    };
  }, []);

  const handleResetPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);
    setSuccessMessage(null);

    if (password.length < 6) {
      setErrorMessage("Mật khẩu mới phải có ít nhất 6 ký tự.");
      return;
    }

    if (password !== confirmPassword) {
      setErrorMessage("Xác nhận mật khẩu không khớp.");
      return;
    }

    setIsLoading(true);

    try {
      const { error } = await supabaseBrowserClient.auth.updateUser({
        password: password,
      });

      if (error) {
        setErrorMessage(error.message);
      } else {
        setSuccessMessage("Đặt lại mật khẩu thành công! Đang chuyển hướng về trang đăng nhập...");
        setTimeout(() => {
          router.push("/login");
        }, 2000);
      }
    } catch (err: any) {
      setErrorMessage(err.message || "Đã xảy ra lỗi không xác định. Vui lòng thử lại.");
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md">
        <div className="flex justify-center">
          <Image
            src="/icon-uth.png"
            alt="UTH Connect"
            width={64}
            height={64}
            className="h-16 w-16 object-contain"
            priority
          />
        </div>
        <h2 className="mt-4 text-center text-2xl font-bold tracking-tight text-slate-900">
          Đặt lại mật khẩu
        </h2>
        <p className="mt-1 text-center text-sm text-slate-500">
          Nhập mật khẩu mới an toàn cho tài khoản sinh viên UTH Connect của bạn
        </p>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md px-4 sm:px-0">
        <div className="bg-white py-8 px-6 shadow-sm rounded-xl border border-slate-200/80 sm:px-10">
          {errorMessage && (
            <div className="mb-6 p-4 rounded-lg bg-red-50 border border-red-200 text-red-700 flex items-start gap-3">
              <ShieldAlert className="w-5 h-5 flex-shrink-0 mt-0.5 text-red-600" />
              <div className="text-sm font-medium">{errorMessage}</div>
            </div>
          )}

          {successMessage && (
            <div className="mb-6 p-4 rounded-lg bg-emerald-50 border border-emerald-200 text-emerald-800 flex items-start gap-3">
              <CheckCircle2 className="w-5 h-5 flex-shrink-0 mt-0.5 text-emerald-600" />
              <div className="text-sm font-medium">{successMessage}</div>
            </div>
          )}

          {isCheckingSession ? (
            <div className="py-8 flex flex-col items-center justify-center text-slate-400 gap-3">
              <Loader2 className="w-8 h-8 animate-spin text-sky-600" />
              <p className="text-sm">Đang xác thực liên kết bảo mật...</p>
            </div>
          ) : !hasSession && errorMessage ? (
            <div className="space-y-4">
              <Link
                href="/login"
                className="w-full flex justify-center items-center gap-2 py-2.5 px-4 border border-slate-300 rounded-lg shadow-sm text-sm font-medium text-slate-700 bg-white hover:bg-slate-50 transition-colors"
              >
                <ArrowLeft className="w-4 h-4" />
                Quay lại Đăng nhập / Yêu cầu link mới
              </Link>
            </div>
          ) : (
            <form onSubmit={handleResetPassword} className="space-y-5">
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">
                  Mật khẩu mới
                </label>
                <div className="relative rounded-lg shadow-sm">
                  <input
                    type={showPassword ? "text" : "password"}
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    required
                    placeholder="Tối thiểu 6 ký tự"
                    className="block w-full rounded-lg border-slate-300 pr-10 focus:border-sky-600 focus:ring-sky-600 sm:text-sm py-2.5 px-3 border outline-none"
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute inset-y-0 right-0 pr-3 flex items-center text-slate-400 hover:text-slate-600"
                  >
                    {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">
                  Xác nhận mật khẩu mới
                </label>
                <div className="relative rounded-lg shadow-sm">
                  <input
                    type={showPassword ? "text" : "password"}
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    required
                    placeholder="Nhập lại mật khẩu mới"
                    className="block w-full rounded-lg border-slate-300 pr-10 focus:border-sky-600 focus:ring-sky-600 sm:text-sm py-2.5 px-3 border outline-none"
                  />
                </div>
              </div>

              <button
                type="submit"
                disabled={isLoading}
                className="w-full flex justify-center items-center py-2.5 px-4 border border-transparent rounded-lg shadow-sm text-sm font-medium text-white bg-sky-600 hover:bg-sky-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-sky-500 disabled:opacity-50 transition-colors"
              >
                {isLoading ? (
                  <>
                    <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                    Đang cập nhật...
                  </>
                ) : (
                  <>
                    <Lock className="w-4 h-4 mr-2" />
                    Lưu mật khẩu mới
                  </>
                )}
              </button>

              <div className="pt-2 flex flex-col items-center gap-3">
                <a
                  href={`uthconnect://auth/reset-password${typeof window !== "undefined" ? window.location.hash : ""}`}
                  className="w-full text-center py-2 px-3 border border-slate-200 rounded-lg text-xs font-medium text-slate-700 bg-slate-50 hover:bg-slate-100 transition-colors"
                >
                  Mở trong ứng dụng UTH Connect trên điện thoại
                </a>

                <Link
                  href="/login"
                  className="text-xs font-medium text-sky-600 hover:text-sky-500 flex items-center justify-center gap-1"
                >
                  <ArrowLeft className="w-3.5 h-3.5" />
                  Quay lại đăng nhập
                </Link>
              </div>
            </form>
          )}
        </div>
      </div>
    </div>
  );
}
