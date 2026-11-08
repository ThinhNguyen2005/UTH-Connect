"use client";

import React, { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useGetIdentity } from "@refinedev/core";
import { ShieldAlert, ArrowLeft } from "lucide-react";
import type { AppUserIdentity } from "@/providers/auth-provider";
import Link from "next/link";

export const AdminGuard: React.FC<{ children: React.ReactNode }> = ({
  children,
}) => {
  const router = useRouter();
  const { data: user, isLoading } = useGetIdentity<AppUserIdentity>();
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
  }, []);

  if (isLoading || !mounted) {
    return (
      <div className="min-h-screen bg-[#F8FAFC] flex items-center justify-center p-4">
        <div className="text-xs font-semibold text-slate-500 tracking-wider uppercase animate-pulse">
          Đang xác thực quyền Admin...
        </div>
      </div>
    );
  }

  if (user?.role !== "admin") {
    return (
      <div className="min-h-screen bg-[#F8FAFC] flex items-center justify-center p-6">
        <div className="w-full max-w-md bg-white rounded-xl p-8 shadow-subtle text-center">
          <div className="w-12 h-12 rounded-lg bg-red-50 text-red-600 flex items-center justify-center mx-auto mb-4">
            <ShieldAlert className="w-6 h-6 stroke-[1.5]" />
          </div>
          <h2 className="text-lg font-bold text-slate-900 tracking-tight">
            Khu Vực Quản Trị Tối Mật
          </h2>
          <p className="mt-2 text-xs text-slate-500 leading-relaxed">
            Tài khoản <span className="font-semibold text-slate-800">{user?.email || "của bạn"}</span> ({user?.role || "sinh viên"}) không có quyền truy cập vào Admin Console.
          </p>
          <div className="mt-6 pt-4 border-t border-slate-100 flex items-center justify-center gap-3">
            <Link
              href="/"
              className="inline-flex items-center gap-1.5 px-4 py-2 bg-slate-900 text-white text-xs font-medium rounded-lg hover:bg-slate-800 transition active:scale-[0.98]"
            >
              <ArrowLeft className="w-3.5 h-3.5" />
              Quay lại Cổng Sinh viên
            </Link>
          </div>
        </div>
      </div>
    );
  }

  return <>{children}</>;
};
