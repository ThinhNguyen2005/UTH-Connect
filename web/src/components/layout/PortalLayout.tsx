"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useGetIdentity, useLogout } from "@refinedev/core";
import {
  Bell,
  LogOut,
  Shield,
  Menu,
  X,
  GraduationCap,
  ChevronRight,
  ExternalLink,
} from "lucide-react";
import type { AppUserIdentity } from "@/providers/auth-provider";
import { AppSidebar } from "@/components/layout/AppSidebar";

export const PortalLayout: React.FC<{ children: React.ReactNode }> = ({
  children,
}) => {
  const pathname = usePathname();
  const [mounted, setMounted] = useState(false);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const { data: user } = useGetIdentity<AppUserIdentity>();
  const { mutate: logout } = useLogout();

  useEffect(() => {
    setMounted(true);
  }, []);

  // Close mobile drawer on route change
  useEffect(() => {
    setMobileMenuOpen(false);
  }, [pathname]);

  const isAdmin = user?.role === "admin";

  const getPageTitle = () => {
    if (pathname === "/") return "Tổng quan học tập & đời sống";
    if (pathname.startsWith("/notifications")) return "Thông báo toàn trường";
    if (pathname.startsWith("/events")) return "Sự kiện & Điểm rèn luyện";
    if (pathname.startsWith("/housing")) return "Ghép trọ & Ký túc xá";
    if (pathname.startsWith("/campuses")) return "Cơ sở đào tạo & Danh bạ";
    if (pathname.startsWith("/settings/security")) return "Bảo mật & Quản lý phiên";
    return "Cổng Thông Tin Sinh Viên";
  };

  return (
    <div className="min-h-screen bg-[#F8FAFC] flex font-sans text-slate-900">
      {/* Desktop Sidebar Rail */}
      <div className="hidden lg:block shrink-0">
        <AppSidebar />
      </div>

      {/* Mobile Backdrop & Drawer */}
      {mobileMenuOpen && (
        <div className="fixed inset-0 z-50 flex lg:hidden">
          <div
            className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm transition-opacity"
            onClick={() => setMobileMenuOpen(false)}
          />
          <div className="relative z-50 flex flex-col w-[280px] max-w-full bg-white shadow-2xl">
            <div className="flex items-center justify-between p-4 border-b border-slate-100">
              <div className="flex items-center gap-2">
                <div className="h-8 w-8 rounded-lg bg-slate-900 flex items-center justify-center text-white">
                  <GraduationCap className="h-4 w-4" />
                </div>
                <span className="text-sm font-bold text-slate-900">UTH Connect</span>
              </div>
              <button
                onClick={() => setMobileMenuOpen(false)}
                className="h-8 w-8 rounded-lg flex items-center justify-center text-slate-400 hover:bg-slate-100"
              >
                <X className="h-4 w-4" />
              </button>
            </div>
            <div className="flex-1 overflow-y-auto">
              <AppSidebar />
            </div>
          </div>
        </div>
      )}

      {/* Main Content Area */}
      <div className="flex-1 flex flex-col min-w-0 min-h-screen">
        {/* Top Header Bar */}
        <header className="sticky top-0 z-30 h-14 bg-white/95 backdrop-blur px-4 sm:px-6 lg:px-8 flex items-center justify-between border-b border-slate-200/70">
          <div className="flex items-center gap-3">
            {/* Mobile Hamburger Button */}
            <button
              type="button"
              onClick={() => setMobileMenuOpen(true)}
              className="lg:hidden flex h-8 w-8 items-center justify-center rounded-lg text-slate-600 hover:bg-slate-100"
              aria-label="Open navigation menu"
            >
              <Menu className="h-5 w-5 stroke-[1.8]" />
            </button>

            {/* Breadcrumb / Title */}
            <div className="flex items-center gap-2 text-xs">
              <span className="text-slate-400 hidden sm:inline">UTH Connect</span>
              <ChevronRight className="h-3 w-3 text-slate-300 hidden sm:inline" />
              <h1 className="font-semibold text-slate-800 text-xs sm:text-sm tracking-tight truncate max-w-[220px] sm:max-w-none">
                {getPageTitle()}
              </h1>
            </div>
          </div>

          {/* Right Action Icons & User Badge */}
          <div className="flex items-center gap-3">
            {mounted && isAdmin && (
              <Link
                href="/admin"
                className="inline-flex items-center gap-1.5 px-2.5 py-1.5 bg-red-50 text-red-700 hover:bg-red-100 rounded-lg text-xs font-semibold transition active:scale-[0.98]"
              >
                <Shield className="w-3.5 h-3.5 stroke-[1.75]" />
                <span className="hidden sm:inline">Admin Console</span>
              </Link>
            )}

            <Link
              href="/notifications"
              className="relative flex h-8 w-8 items-center justify-center rounded-lg text-slate-500 hover:bg-slate-100 hover:text-slate-900 transition"
              title="Thông báo"
            >
              <Bell className="h-4 w-4 stroke-[1.75]" />
              <span className="absolute top-1.5 right-1.5 w-2 h-2 rounded-full bg-blue-600 ring-2 ring-white" />
            </Link>

            <div className="h-4 w-px bg-slate-200" />

            {!mounted ? (
              <div className="w-16 h-7 rounded-lg bg-slate-100 animate-pulse" />
            ) : user ? (
              <div className="flex items-center gap-2.5">
                <div className="text-right hidden sm:block">
                  <div className="text-xs font-semibold text-slate-900 leading-tight">
                    {user.name}
                  </div>
                  <div className="text-[10px] text-slate-400 leading-tight">
                    {user.role === "admin"
                      ? "Quản trị viên"
                      : user.role === "lecturer"
                      ? "Giảng viên"
                      : "Sinh viên"}
                  </div>
                </div>
                <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-slate-900 text-xs font-bold text-white shadow-sm">
                  {user.name ? user.name.charAt(0).toUpperCase() : "U"}
                </div>
              </div>
            ) : (
              <Link
                href="/login"
                className="inline-flex items-center px-3 py-1.5 bg-slate-900 text-white rounded-lg text-xs font-semibold hover:bg-slate-800 transition active:scale-[0.98]"
              >
                Đăng nhập
              </Link>
            )}
          </div>
        </header>

        {/* Dynamic Page Children Content */}
        <main className="flex-1 p-4 sm:p-6 lg:p-8 overflow-y-auto">
          {children}
        </main>
      </div>
    </div>
  );
};
