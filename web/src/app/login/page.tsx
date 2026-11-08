"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";
import { authProvider } from "@/providers/auth-provider";
import {
  ShieldAlert,
  CheckCircle2,
  Eye,
  EyeOff,
  Sparkles,
  Loader2,
} from "lucide-react";

export default function LoginPage() {
  const [mode, setMode] = useState<"login" | "register">("login");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [fullName, setFullName] = useState("");
  const [rememberMe, setRememberMe] = useState(true);
  const [showPassword, setShowPassword] = useState(false);

  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  useEffect(() => {
    if (typeof window !== "undefined") {
      const params = new URLSearchParams(window.location.search);
      const err = params.get("error");
      const msg = params.get("message");

      if (err === "auth_callback_failed") {
        setErrorMessage("Đăng nhập Google không thành công hoặc phiên làm việc đã hết hạn.");
      }
      if (msg === "check_email") {
        setSuccessMessage("Đăng ký thành công! Vui lòng kiểm tra email trường để xác nhận tài khoản.");
      }
    }
  }, []);

  const handleTabChange = (newMode: "login" | "register") => {
    setMode(newMode);
    setErrorMessage(null);
    setSuccessMessage(null);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);
    setSuccessMessage(null);

    const cleanEmail = email.trim();
    if (!cleanEmail) {
      setErrorMessage("Vui lòng nhập Email hoặc MSSV.");
      return;
    }

    if (!password) {
      setErrorMessage("Vui lòng nhập mật khẩu.");
      return;
    }

    setIsLoading(true);

    if (mode === "register") {
      if (!fullName.trim()) {
        setErrorMessage("Vui lòng nhập họ và tên của bạn.");
        setIsLoading(false);
        return;
      }
      if (password.length < 6) {
        setErrorMessage("Mật khẩu phải có tối thiểu 6 ký tự.");
        setIsLoading(false);
        return;
      }
      if (password !== confirmPassword) {
        setErrorMessage("Mật khẩu xác nhận không khớp.");
        setIsLoading(false);
        return;
      }

      try {
        const res = await authProvider.register?.({
          email: cleanEmail,
          password,
          name: fullName.trim(),
        });

        if (res?.success) {
          if (res.redirectTo?.includes("check_email")) {
            setSuccessMessage("Đăng ký thành công! Vui lòng kiểm tra hộp thư email trường để kích hoạt.");
            setIsLoading(false);
          } else {
            setSuccessMessage("Đăng ký thành công! Đang chuyển hướng...");
            window.location.href = res.redirectTo || "/";
          }
        } else {
          setErrorMessage(res?.error?.message || "Đăng ký không thành công. Vui lòng thử lại.");
          setIsLoading(false);
        }
      } catch (err: any) {
        setErrorMessage(err?.message || "Đăng ký thất bại.");
        setIsLoading(false);
      }
      return;
    }

    // Login mode
    try {
      const res = await authProvider.login({
        email: cleanEmail,
        password,
        remember: rememberMe,
      });

      if (res?.success) {
        setSuccessMessage("Đăng nhập thành công! Đang chuyển hướng...");
        const target = res.redirectTo || "/";
        window.location.href = target;
      } else {
        setErrorMessage(res?.error?.message || "Tài khoản hoặc mật khẩu không chính xác.");
        setIsLoading(false);
      }
    } catch (err: any) {
      setErrorMessage(err?.message || "Đăng nhập thất bại. Vui lòng thử lại.");
      setIsLoading(false);
    }
  };

  const handleGoogleLogin = async () => {
    setErrorMessage(null);
    setSuccessMessage("Đang chuyển hướng tới Google...");
    setIsLoading(true);

    try {
      const res = await authProvider.login({ providerName: "google" });
      if (!res?.success) {
        setErrorMessage(res?.error?.message || "Đăng nhập Google thất bại.");
        setIsLoading(false);
        setSuccessMessage(null);
      }
    } catch (err: any) {
      setErrorMessage(err?.message || "Đăng nhập Google thất bại.");
      setIsLoading(false);
      setSuccessMessage(null);
    }
  };

  const handleQuickFill = (presetEmail: string, presetPassword = "123456") => {
    setEmail(presetEmail);
    setPassword(presetPassword);
    setMode("login");
    setErrorMessage(null);
    setSuccessMessage(null);
  };

  return (
    <div className="flex min-h-screen bg-white">
      {/* Form Left Side */}
      <div className="flex w-full items-center justify-center px-6 py-12 lg:w-1/2">
        <div className="w-full max-w-sm">
          {/* Header Branding */}
          <div className="mb-8">
            <div className="mb-6 flex h-9 w-9 items-center justify-center rounded-lg bg-neutral-900 text-sm font-bold text-white shadow-sm">
              U
            </div>
            <h1 className="text-2xl font-semibold text-neutral-900 tracking-tight">
              {mode === "login" ? "Welcome back" : "Create an account"}
            </h1>
            <p className="mt-1 text-sm text-neutral-500">
              {mode === "login"
                ? "Sign in to continue to your account."
                : "Register for UTH Connect portal."}
            </p>
          </div>

          {/* Mode Switcher Tabs */}
          <div className="mb-6 flex rounded-xl border border-neutral-200 p-1 text-xs font-medium text-neutral-600">
            <button
              type="button"
              onClick={() => handleTabChange("login")}
              className={`flex-1 rounded-lg py-1.5 transition ${
                mode === "login"
                  ? "bg-neutral-900 font-semibold text-white shadow-sm"
                  : "hover:text-neutral-900"
              }`}
            >
              Sign in
            </button>
            <button
              type="button"
              onClick={() => handleTabChange("register")}
              className={`flex-1 rounded-lg py-1.5 transition ${
                mode === "register"
                  ? "bg-neutral-900 font-semibold text-white shadow-sm"
                  : "hover:text-neutral-900"
              }`}
            >
              Sign up
            </button>
          </div>

          {/* Feedback alerts */}
          {errorMessage && (
            <div className="mb-4 flex items-start gap-2.5 rounded-xl bg-red-50 p-3 text-xs text-red-700 animate-in fade-in duration-200">
              <ShieldAlert className="mt-0.5 h-4 w-4 shrink-0 text-red-600" />
              <span>{errorMessage}</span>
            </div>
          )}

          {successMessage && (
            <div className="mb-4 flex items-start gap-2.5 rounded-xl bg-emerald-50 p-3 text-xs text-emerald-800 animate-in fade-in duration-200">
              <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0 text-emerald-600" />
              <span>{successMessage}</span>
            </div>
          )}

          {/* Form */}
          <form onSubmit={handleSubmit} className="space-y-4">
            {mode === "register" && (
              <div>
                <label className="mb-1.5 block text-sm font-medium text-neutral-700">
                  Full name
                </label>
                <input
                  type="text"
                  required
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                  placeholder="Nguyen Van A"
                  className="w-full rounded-xl border border-neutral-200 px-3.5 py-2.5 text-sm outline-none ring-neutral-900/10 focus:border-neutral-900 focus:ring-4 transition"
                />
              </div>
            )}

            <div>
              <label className="mb-1.5 block text-sm font-medium text-neutral-700">
                Email or Student ID
              </label>
              <input
                type="text"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="you@ut.edu.vn or 2151050000"
                className="w-full rounded-xl border border-neutral-200 px-3.5 py-2.5 text-sm outline-none ring-neutral-900/10 focus:border-neutral-900 focus:ring-4 transition"
              />
            </div>

            <div>
              <label className="mb-1.5 block text-sm font-medium text-neutral-700">
                Password
              </label>
              <div className="relative">
                <input
                  type={showPassword ? "text" : "password"}
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••"
                  className="w-full rounded-xl border border-neutral-200 px-3.5 py-2.5 pr-10 text-sm outline-none ring-neutral-900/10 focus:border-neutral-900 focus:ring-4 transition"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute inset-y-0 right-0 flex items-center pr-3 text-neutral-400 hover:text-neutral-700 transition"
                >
                  {showPassword ? (
                    <EyeOff className="h-4 w-4" />
                  ) : (
                    <Eye className="h-4 w-4" />
                  )}
                </button>
              </div>
            </div>

            {mode === "register" && (
              <div>
                <label className="mb-1.5 block text-sm font-medium text-neutral-700">
                  Confirm Password
                </label>
                <input
                  type={showPassword ? "text" : "password"}
                  required
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  placeholder="••••••••"
                  className="w-full rounded-xl border border-neutral-200 px-3.5 py-2.5 text-sm outline-none ring-neutral-900/10 focus:border-neutral-900 focus:ring-4 transition"
                />
              </div>
            )}

            {mode === "login" && (
              <div className="flex items-center justify-between text-sm">
                <label className="flex items-center gap-2 text-neutral-600 cursor-pointer select-none">
                  <input
                    type="checkbox"
                    checked={rememberMe}
                    onChange={(e) => setRememberMe(e.target.checked)}
                    className="rounded border-neutral-300 text-neutral-900 focus:ring-neutral-900"
                  />
                  Remember me
                </label>
                <div className="flex items-center gap-3">
                  <Link
                    href={`/verify-email?email=${encodeURIComponent(email || "")}`}
                    className="font-medium text-neutral-600 hover:text-neutral-900 hover:underline text-xs"
                  >
                    Xác thực email...
                  </Link>
                  <a
                    href="#"
                    onClick={(e) => {
                      e.preventDefault();
                      setErrorMessage("Vui lòng liên hệ Phòng CTSV hoặc quản trị viên để cấp lại mật khẩu.");
                    }}
                    className="font-medium text-neutral-900 hover:underline text-xs"
                  >
                    Forgot password?
                  </a>
                </div>
              </div>
            )}

            <button
              type="submit"
              disabled={isLoading}
              className="w-full rounded-xl bg-neutral-900 py-2.5 text-sm font-medium text-white transition hover:bg-neutral-800 disabled:opacity-50 flex items-center justify-center gap-2 active:scale-[0.99]"
            >
              {isLoading && <Loader2 className="h-4 w-4 animate-spin" />}
              <span>
                {isLoading
                  ? "Processing..."
                  : mode === "login"
                  ? "Sign in"
                  : "Create account"}
              </span>
            </button>
          </form>

          {/* Divider */}
          <div className="my-6 flex items-center gap-3 text-xs text-neutral-400">
            <span className="h-px flex-1 bg-neutral-200"></span>
            OR
            <span className="h-px flex-1 bg-neutral-200"></span>
          </div>

          {/* Google OAuth Button */}
          <button
            type="button"
            disabled={isLoading}
            onClick={handleGoogleLogin}
            className="flex w-full items-center justify-center gap-2.5 rounded-xl border border-neutral-200 py-2.5 text-sm font-medium text-neutral-700 transition hover:bg-neutral-50 active:scale-[0.99] disabled:opacity-50"
          >
            <svg className="h-4 w-4" viewBox="0 0 24 24">
              <path
                fill="#4285F4"
                d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
              />
              <path
                fill="#34A853"
                d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
              />
              <path
                fill="#FBBC05"
                d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z"
              />
              <path
                fill="#EA4335"
                d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z"
              />
            </svg>
            Continue with Google
          </button>

          {/* Demo Accounts Quick-Fill Box */}
          <div className="mt-8 rounded-xl border border-neutral-100 bg-neutral-50/80 p-3.5 text-xs text-neutral-600">
            <div className="mb-2 font-semibold text-neutral-800 flex items-center gap-1.5">
              <Sparkles className="w-3.5 h-3.5 text-neutral-500" />
              <span>Tài khoản kiểm thử nhanh (bấm để điền sẵn pass 123456):</span>
            </div>
            <div className="space-y-1.5 text-[11px]">
              <div className="flex items-center justify-between">
                <span>👑 Admin:</span>
                <button
                  type="button"
                  onClick={() => handleQuickFill("nguyenthinhk52005@gmail.com", "123456")}
                  className="font-mono text-[10px] text-blue-700 hover:underline bg-white px-2 py-0.5 rounded border border-neutral-200"
                  title="Click to auto-fill"
                >
                  nguyenthinhk52005@gmail.com
                </button>
              </div>
              <div className="flex items-center justify-between">
                <span>👨‍🏫 Giảng viên:</span>
                <button
                  type="button"
                  onClick={() => handleQuickFill("thinh@ut.edu.vn", "123456")}
                  className="font-mono text-[10px] text-blue-700 hover:underline bg-white px-2 py-0.5 rounded border border-neutral-200"
                  title="Click to auto-fill"
                >
                  thinh@ut.edu.vn
                </button>
              </div>
              <div className="flex items-center justify-between">
                <span>🎓 Sinh viên:</span>
                <button
                  type="button"
                  onClick={() => handleQuickFill("duc1@ut.edu.vn", "123456")}
                  className="font-mono text-[10px] text-blue-700 hover:underline bg-white px-2 py-0.5 rounded border border-neutral-200"
                  title="Click to auto-fill"
                >
                  duc1@ut.edu.vn
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Visual Right Side */}
      <div className="hidden bg-neutral-900 lg:flex lg:w-1/2 lg:flex-col lg:justify-between lg:p-14">
        {/* Top Header in Visual Panel */}
        <div className="flex items-center justify-between">
          <div className="inline-flex items-center gap-2 rounded-full border border-neutral-800 bg-neutral-950/60 px-3.5 py-1 text-xs text-neutral-300 backdrop-blur-sm">
            <span className="h-2 w-2 rounded-full bg-emerald-400 animate-pulse" />
            <span>UTH Connect Platform v1.0</span>
          </div>
          <span className="text-xs text-neutral-500 font-mono">Ho Chi Minh City, VN</span>
        </div>

        {/* Center / Bottom Quote */}
        <div>
          <blockquote className="max-w-md text-2xl font-medium leading-snug text-white">
            &ldquo;This is the fastest we&apos;ve ever shipped a product. It just works.&rdquo;
          </blockquote>
          <p className="mt-4 text-sm text-neutral-400">Alex Rivera — CTO, Northwind</p>

          <div className="mt-8 border-t border-neutral-800/80 pt-6">
            <p className="text-xs text-neutral-500 leading-relaxed">
              Trường Đại học Giao thông vận tải TP.HCM — Cổng kết nối Sinh viên & Giảng viên.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
