"use client";

import React, { useState } from "react";
import Link from "next/link";
import Image from "next/image";
import { supabaseBrowserClient } from "@/utils/supabase-client";
import {
  ShieldAlert,
  CheckCircle2,
  Loader2,
  ArrowLeft,
  Mail,
} from "lucide-react";

export default function ForgotPasswordPage() {
  const [identifier, setIdentifier] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successEmail, setSuccessEmail] = useState<string | null>(null);
  const [cooldown, setCooldown] = useState(0);

  const startCooldown = () => {
    setCooldown(60);
    const timer = setInterval(() => {
      setCooldown((prev) => {
        if (prev <= 1) {
          clearInterval(timer);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);

    const cleanInput = identifier.trim();
    if (!cleanInput) {
      setErrorMessage("Vui lòng nhập Email sinh viên hoặc Mã số sinh viên (MSSV).");
      return;
    }

    let normalizedEmail = cleanInput;
    if (!normalizedEmail.includes("@")) {
      normalizedEmail = `${normalizedEmail}@ut.edu.vn`;
    }

    setIsLoading(true);

    try {
      const origin =
        typeof window !== "undefined"
          ? window.location.origin
          : "https://uth-connect-web-git-main-nguyenthinhk52005-5982.vercel.app";

      const { error } = await supabaseBrowserClient.auth.resetPasswordForEmail(
        normalizedEmail,
        {
          redirectTo: `${origin}/auth/callback?next=/reset-password`,
        }
      );

      if (error) {
        setErrorMessage(error.message);
      } else {
        setSuccessEmail(normalizedEmail);
        startCooldown();
      }
    } catch (err: any) {
      setErrorMessage(err.message || "Đã xảy ra lỗi kết nối. Vui lòng thử lại.");
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
          Quên mật khẩu
        </h2>
        <p className="mt-1 text-center text-sm text-slate-500">
          Nhận liên kết đặt lại mật khẩu an toàn qua email trường của bạn
        </p>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md px-4 sm:px-0">
        <div className="bg-white py-8 px-6 shadow-sm rounded-xl border border-slate-200/80 sm:px-10">
          {successEmail ? (
            <div className="space-y-6 text-center">
              <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-emerald-50 text-emerald-600">
                <CheckCircle2 className="h-6 w-6 stroke-[1.75]" />
              </div>
              <div>
                <h3 className="text-base font-semibold text-slate-900">
                  Đã gửi email khôi phục!
                </h3>
                <p className="mt-2 text-sm text-slate-600 leading-relaxed">
                  Chúng tôi đã gửi một liên kết đặt lại mật khẩu đến:
                  <br />
                  <span className="font-semibold text-slate-900">{successEmail}</span>
                </p>
                <p className="mt-3 text-xs text-slate-500">
                  Vui lòng kiểm tra hộp thư (bao gồm cả thư mục Spam/Rác) và nhấn vào liên kết để tạo mật khẩu mới.
                </p>
              </div>

              <div className="pt-2 flex flex-col gap-3">
                <button
                  type="button"
                  onClick={handleSubmit}
                  disabled={cooldown > 0 || isLoading}
                  className="w-full inline-flex justify-center items-center py-2.5 px-4 border border-slate-300 rounded-lg shadow-sm text-sm font-medium text-slate-700 bg-white hover:bg-slate-50 disabled:opacity-50 transition"
                >
                  {isLoading ? (
                    <Loader2 className="h-4 w-4 animate-spin mr-2" />
                  ) : null}
                  {cooldown > 0
                    ? `Gửi lại sau ${cooldown}s`
                    : "Gửi lại email xác nhận"}
                </button>

                <Link
                  href="/login"
                  className="inline-flex items-center justify-center gap-1.5 text-sm font-medium text-slate-600 hover:text-slate-900 pt-2 transition"
                >
                  <ArrowLeft className="h-4 w-4" />
                  Quay lại đăng nhập
                </Link>
              </div>
            </div>
          ) : (
            <form onSubmit={handleSubmit} className="space-y-5">
              {errorMessage && (
                <div className="rounded-lg bg-rose-50 p-3.5 border border-rose-200/60 flex items-start gap-2.5">
                  <ShieldAlert className="h-5 w-5 text-rose-600 shrink-0 mt-0.5" />
                  <p className="text-sm text-rose-800 leading-tight">{errorMessage}</p>
                </div>
              )}

              <div>
                <label
                  htmlFor="identifier"
                  className="block text-sm font-medium text-slate-700"
                >
                  Email sinh viên hoặc MSSV
                </label>
                <div className="mt-1 relative rounded-lg">
                  <input
                    id="identifier"
                    type="text"
                    value={identifier}
                    onChange={(e) => setIdentifier(e.target.value)}
                    placeholder="VD: 2151120000 hoặc user@ut.edu.vn"
                    required
                    className="block w-full rounded-lg border border-slate-300 px-3.5 py-2.5 text-slate-900 placeholder-slate-400 focus:border-slate-900 focus:outline-none focus:ring-1 focus:ring-slate-900 sm:text-sm transition"
                  />
                  <div className="absolute inset-y-0 right-0 pr-3 flex items-center pointer-events-none text-slate-400">
                    <Mail className="h-4 w-4" />
                  </div>
                </div>
                <p className="mt-1.5 text-xs text-slate-500">
                  Nếu nhập MSSV, hệ thống sẽ tự động gửi đến địa chỉ <code>@ut.edu.vn</code> của bạn.
                </p>
              </div>

              <button
                type="submit"
                disabled={isLoading}
                className="w-full flex justify-center py-2.5 px-4 border border-transparent rounded-lg shadow-sm text-sm font-medium text-white bg-slate-900 hover:bg-slate-800 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-slate-900 disabled:opacity-50 transition"
              >
                {isLoading ? (
                  <Loader2 className="h-4 w-4 animate-spin mr-2" />
                ) : null}
                Gửi liên kết đặt lại mật khẩu
              </button>

              <div className="text-center pt-2">
                <Link
                  href="/login"
                  className="inline-flex items-center gap-1.5 text-sm font-medium text-slate-600 hover:text-slate-900 transition"
                >
                  <ArrowLeft className="h-4 w-4" />
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
