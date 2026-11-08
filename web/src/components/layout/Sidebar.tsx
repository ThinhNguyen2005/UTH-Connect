"use client";

import React from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useGetIdentity } from "@refinedev/core";
import {
  LayoutDashboard,
  Bell,
  CalendarDays,
  Home,
  Users,
  Building2,
} from "lucide-react";
import type { AppUserIdentity } from "@/providers/auth-provider";

export const Sidebar: React.FC = () => {
  const pathname = usePathname();
  const { data: user } = useGetIdentity<AppUserIdentity>();
  const role = user?.role || "student";
  const isAdmin = role === "admin";
  const isLecturerOrAdmin = role === "admin" || role === "lecturer" || role === "organizer";

  const navigation = [
    {
      name: "Tổng quan",
      href: "/",
      icon: LayoutDashboard,
      show: true,
    },
    {
      name: "Thông báo trường",
      href: "/notifications",
      icon: Bell,
      show: true,
      badge: isLecturerOrAdmin ? "Được đăng" : undefined,
    },
    {
      name: "Sự kiện & Hoạt động",
      href: "/events",
      icon: CalendarDays,
      show: true,
      badge: isLecturerOrAdmin ? "Quản lý" : undefined,
    },
    {
      name: "Ghép trọ & KTX",
      href: "/housing",
      icon: Home,
      show: true,
    },
    {
      name: "Cơ sở & Danh bạ",
      href: "/campuses",
      icon: Building2,
      show: true,
    },
    {
      name: "Quản trị Người dùng & RBAC",
      href: "/users",
      icon: Users,
      show: isAdmin,
      adminOnly: true,
    },
  ];

  return (
    <aside className="w-64 bg-white border-r border-uth-slate-200 flex flex-col shrink-0 min-h-[calc(100vh-4rem)]">
      <div className="p-4 border-b border-uth-slate-100">
        <div className="text-xs font-semibold uppercase tracking-wider text-uth-slate-600 px-3 py-1">
          Menu Điều Hướng
        </div>
      </div>

      <nav className="flex-1 p-3 space-y-1">
        {navigation
          .filter((item) => item.show)
          .map((item) => {
            const isActive = pathname === item.href;
            const Icon = item.icon;

            return (
              <Link
                key={item.href}
                href={item.href}
                className={`flex items-center justify-between px-3 py-2.5 rounded-xl text-sm font-medium transition ${
                  isActive
                    ? "bg-uth-navy text-white shadow-sm"
                    : "text-uth-slate-600 hover:bg-uth-slate-100 hover:text-uth-slate-900"
                }`}
              >
                <div className="flex items-center gap-3">
                  <Icon className={`w-5 h-5 ${isActive ? "text-white" : "text-uth-slate-600"}`} />
                  <span>{item.name}</span>
                </div>
                {item.badge && (
                  <span
                    className={`text-[10px] font-semibold px-2 py-0.5 rounded-full ${
                      isActive
                        ? "bg-white/20 text-white"
                        : "bg-blue-100 text-uth-navy"
                    }`}
                  >
                    {item.badge}
                  </span>
                )}
                {item.adminOnly && (
                  <span
                    className={`text-[10px] font-semibold px-2 py-0.5 rounded-full ${
                      isActive
                        ? "bg-red-400 text-white"
                        : "bg-red-100 text-red-700"
                    }`}
                  >
                    Admin
                  </span>
                )}
              </Link>
            );
          })}
      </nav>

      <div className="p-4 border-t border-uth-slate-200">
        <div className="p-3 bg-uth-slate-50 rounded-xl border border-uth-slate-200 text-xs text-uth-slate-600">
          <div className="font-semibold text-uth-slate-900 mb-1">
            UTH Connect Web v1.0
          </div>
          <div>Hệ thống kết nối trực tuyến Đại học Giao thông vận tải TP.HCM.</div>
        </div>
      </div>
    </aside>
  );
};
