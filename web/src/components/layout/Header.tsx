"use client";

import React from "react";
import { useGetIdentity, useLogout } from "@refinedev/core";
import { LogOut, ShieldCheck, GraduationCap, School } from "lucide-react";
import type { AppUserIdentity } from "@/providers/auth-provider";

export const Header: React.FC = () => {
  const { data: user } = useGetIdentity<AppUserIdentity>();
  const { mutate: logout } = useLogout();

  const getRoleBadge = (role?: string) => {
    switch (role) {
      case "admin":
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-red-100 text-red-800 border border-red-200">
            <ShieldCheck className="w-3.5 h-3.5" />
            Quản trị viên (Admin)
          </span>
        );
      case "lecturer":
      case "organizer":
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-blue-100 text-uth-navy border border-blue-200">
            <School className="w-3.5 h-3.5" />
            Giảng viên / Ban tổ chức
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-800 border border-emerald-200">
            <GraduationCap className="w-3.5 h-3.5" />
            Sinh viên UTH
          </span>
        );
    }
  };

  return (
    <header className="sticky top-0 z-30 flex items-center justify-between h-16 px-6 bg-white border-b border-uth-slate-200 shadow-sm">
      <div className="flex items-center gap-3">
        <div className="w-9 h-9 rounded-xl bg-uth-navy flex items-center justify-center text-white font-bold text-lg shadow-sm">
          U
        </div>
        <div>
          <h1 className="text-base font-bold text-uth-slate-900 leading-tight">
            UTH Connect Portal
          </h1>
          <p className="text-xs text-uth-slate-600">
            Trường Đại học Giao thông vận tải TP.HCM
          </p>
        </div>
      </div>

      <div className="flex items-center gap-4">
        {user && (
          <div className="flex items-center gap-3">
            <div className="text-right hidden sm:block">
              <div className="text-sm font-semibold text-uth-slate-900">
                {user.name}
              </div>
              <div className="flex justify-end">{getRoleBadge(user.role)}</div>
            </div>
            {user.avatar ? (
              <img
                src={user.avatar}
                alt={user.name}
                className="w-10 h-10 rounded-full border-2 border-uth-navy object-cover"
              />
            ) : (
              <div className="w-10 h-10 rounded-full bg-uth-navy-subtle text-uth-navy flex items-center justify-center font-bold text-sm">
                {user.name.charAt(0).toUpperCase()}
              </div>
            )}
            <button
              onClick={() => logout()}
              title="Đăng xuất"
              className="p-2 ml-1 text-uth-slate-600 hover:text-red-600 hover:bg-red-50 rounded-xl transition"
            >
              <LogOut className="w-5 h-5" />
            </button>
          </div>
        )}
      </div>
    </header>
  );
};
