"use client";

import React from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useGetIdentity, useLogout } from "@refinedev/core";
import {
  ShieldCheck,
  LayoutDashboard,
  Users,
  Home,
  Bell,
  LogOut,
  ExternalLink,
  Lock,
} from "lucide-react";
import type { AppUserIdentity } from "@/providers/auth-provider";
import { AdminGuard } from "@/components/auth/AdminGuard";

export const AdminLayout: React.FC<{ children: React.ReactNode }> = ({
  children,
}) => {
  const pathname = usePathname();
  const { data: user } = useGetIdentity<AppUserIdentity>();
  const { mutate: logout } = useLogout();

  const navItems = [
    {
      name: "Tổng quan Quản trị",
      href: "/admin",
      icon: LayoutDashboard,
    },
    {
      name: "Thành viên & RBAC",
      href: "/admin/users",
      icon: Users,
    },
    {
      name: "Kiểm duyệt Tin trọ",
      href: "/admin/moderation",
      icon: Home,
    },
    {
      name: "Soạn thảo Thông báo",
      href: "/admin/announcements",
      icon: Bell,
    },
  ];

  return (
    <AdminGuard>
      <div className="min-h-screen bg-[#F8FAFC] flex flex-col font-sans">
        {/* Top Header */}
        <header className="sticky top-0 z-30 h-14 bg-white px-6 flex items-center justify-between shadow-subtle">
          <div className="flex items-center gap-3">
            <div className="flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-red-500 animate-pulse" />
              <span className="text-xs font-bold tracking-wider uppercase text-slate-900">
                UTH Admin Console
              </span>
            </div>
            <span className="text-slate-300">/</span>
            <span className="text-xs text-slate-500 font-medium">
              Quản trị Toàn quyền
            </span>
          </div>

          <div className="flex items-center gap-4">
            <Link
              href="/"
              className="inline-flex items-center gap-1.5 text-xs text-slate-600 hover:text-slate-900 font-medium transition"
            >
              <span>Xem Cổng Sinh viên</span>
              <ExternalLink className="w-3.5 h-3.5 stroke-[1.5]" />
            </Link>

            <div className="h-4 w-[1px] bg-slate-200" />

            <div className="flex items-center gap-2">
              <span className="text-xs font-semibold text-slate-800">
                {user?.email}
              </span>
              <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-red-50 text-red-700">
                Admin
              </span>
              <button
                onClick={() => logout()}
                title="Đăng xuất"
                className="p-1.5 text-slate-400 hover:text-slate-700 rounded-md transition"
              >
                <LogOut className="w-4 h-4 stroke-[1.5]" />
              </button>
            </div>
          </div>
        </header>

        {/* Body Layout */}
        <div className="flex flex-1">
          {/* Admin Sidebar */}
          <aside className="w-60 bg-white p-4 shrink-0 flex flex-col justify-between shadow-subtle">
            <div>
              <div className="text-[11px] font-semibold uppercase tracking-wider text-slate-400 px-3 py-2 mb-1">
                Quản Trị Hệ Thống
              </div>
              <nav className="space-y-1">
                {navItems.map((item) => {
                  const isActive = pathname === item.href;
                  const Icon = item.icon;
                  return (
                    <Link
                      key={item.href}
                      href={item.href}
                      className={`flex items-center gap-2.5 px-3 py-2 rounded-lg text-xs font-medium transition active:scale-[0.98] ${
                        isActive
                          ? "bg-slate-900 text-white"
                          : "text-slate-600 hover:bg-slate-100 hover:text-slate-900"
                      }`}
                    >
                      <Icon
                        className={`w-4 h-4 stroke-[1.5] ${
                          isActive ? "text-white" : "text-slate-400"
                        }`}
                      />
                      <span>{item.name}</span>
                    </Link>
                  );
                })}
              </nav>
            </div>

            <div className="p-3 bg-[#F8FAFC] rounded-lg">
              <div className="flex items-center gap-1.5 text-[11px] font-semibold text-slate-800 mb-0.5">
                <Lock className="w-3.5 h-3.5 stroke-[1.5] text-slate-500" />
                <span>Bảo mật RLS Cấp cao</span>
              </div>
              <div className="text-[11px] text-slate-500 leading-normal">
                Tất cả các thao tác tại trang này được ghi log và kiểm soát bằng Supabase RLS.
              </div>
            </div>
          </aside>

          {/* Main Admin Content */}
          <main className="flex-1 p-6 lg:p-8 max-w-7xl mx-auto w-full">
            {children}
          </main>
        </div>
      </div>
    </AdminGuard>
  );
};
