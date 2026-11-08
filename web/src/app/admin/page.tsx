"use client";

import React, { useEffect, useState } from "react";
import Link from "next/link";
import { AdminLayout } from "@/components/layout/AdminLayout";
import { supabaseBrowserClient } from "@/utils/supabase-client";
import {
  Users,
  ShieldCheck,
  Calendar,
  Home,
  Bell,
  ArrowRight,
  UserCheck,
  AlertCircle,
} from "lucide-react";

export default function AdminDashboardPage() {
  const [stats, setStats] = useState({
    totalUsers: 0,
    students: 0,
    lecturers: 0,
    verifiedStudents: 0,
    events: 0,
    housing: 0,
    notifications: 0,
  });

  useEffect(() => {
    let isMounted = true;

    async function loadStats() {
      const [
        { count: totalU },
        { count: verifiedS },
        { count: evtCount },
        { count: notifCount },
        { count: houseCount },
      ] = await Promise.all([
        supabaseBrowserClient.from("profiles").select("*", { count: "exact", head: true }),
        supabaseBrowserClient.from("profiles").select("*", { count: "exact", head: true }).eq("student_id_verified", true),
        supabaseBrowserClient.from("events").select("*", { count: "exact", head: true }),
        supabaseBrowserClient.from("notifications").select("*", { count: "exact", head: true }),
        supabaseBrowserClient.from("housing_posts").select("*", { count: "exact", head: true }),
      ]);

      const { count: lecCount } = await supabaseBrowserClient
        .from("profiles")
        .select("*", { count: "exact", head: true })
        .in("role", ["lecturer", "organizer"]);

      if (!isMounted) return;

      setStats({
        totalUsers: totalU || 0,
        students: (totalU || 0) - (lecCount || 0) - 1, // minus admin & lecturers
        lecturers: lecCount || 0,
        verifiedStudents: verifiedS || 0,
        events: evtCount || 0,
        housing: houseCount || 0,
        notifications: notifCount || 0,
      });
    }

    loadStats();

    return () => {
      isMounted = false;
    };
  }, []);

  return (
    <AdminLayout>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-xl font-bold text-slate-900 tracking-tight">
            Bảng Điều Khiển Quản Trị Hệ Thống
          </h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Giám sát tài khoản, phân quyền RBAC và kiểm duyệt dữ liệu toàn trường UTH
          </p>
        </div>

        <Link
          href="/admin/users"
          className="inline-flex items-center gap-1.5 px-3 py-2 bg-slate-900 text-white rounded-lg text-xs font-semibold hover:bg-slate-800 transition active:scale-[0.98]"
        >
          <Users className="w-3.5 h-3.5 stroke-[1.5]" />
          <span>Quản lý Phân quyền</span>
        </Link>
      </div>

      {/* KPI Tonal Grid (No borders, high density tabular nums) */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <div className="bg-white rounded-xl p-5 shadow-subtle">
          <div className="text-[11px] font-semibold uppercase tracking-wider text-slate-400">
            Tổng tài khoản
          </div>
          <div className="text-2xl font-bold text-slate-900 mt-1 tabular-nums">
            {stats.totalUsers}
          </div>
          <div className="text-[11px] text-slate-500 mt-1">Đồng bộ qua Supabase Auth</div>
        </div>

        <div className="bg-white rounded-xl p-5 shadow-subtle">
          <div className="text-[11px] font-semibold uppercase tracking-wider text-slate-400">
            MSSV đã duyệt
          </div>
          <div className="text-2xl font-bold text-emerald-700 mt-1 tabular-nums">
            {stats.verifiedStudents}
          </div>
          <div className="text-[11px] text-slate-500 mt-1">Đủ điều kiện cấp Vé QR</div>
        </div>

        <div className="bg-white rounded-xl p-5 shadow-subtle">
          <div className="text-[11px] font-semibold uppercase tracking-wider text-slate-400">
            Giảng viên / BTC
          </div>
          <div className="text-2xl font-bold text-slate-900 mt-1 tabular-nums">
            {stats.lecturers}
          </div>
          <div className="text-[11px] text-slate-500 mt-1">Có quyền tạo sự kiện</div>
        </div>

        <div className="bg-white rounded-xl p-5 shadow-subtle">
          <div className="text-[11px] font-semibold uppercase tracking-wider text-slate-400">
            Tin trọ sinh viên
          </div>
          <div className="text-2xl font-bold text-slate-900 mt-1 tabular-nums">
            {stats.housing}
          </div>
          <div className="text-[11px] text-slate-500 mt-1">Cần theo dõi & kiểm duyệt</div>
        </div>
      </div>

      {/* Bento Modules Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Module 1: Phân quyền */}
        <div className="bg-white rounded-xl p-6 shadow-subtle">
          <div className="flex items-center gap-2 mb-3">
            <Users className="w-4 h-4 stroke-[1.5] text-slate-600" />
            <h3 className="text-sm font-bold text-slate-900">
              Phân Quyền RBAC & Xác minh
            </h3>
          </div>
          <p className="text-xs text-slate-500 leading-relaxed mb-4">
            Thay đổi vai trò người dùng giữa Sinh viên, Giảng viên và Admin. Phê duyệt mã số sinh viên để sinh viên được cấp vé sự kiện.
          </p>
          <Link
            href="/admin/users"
            className="inline-flex items-center gap-1.5 text-xs font-semibold text-slate-900 hover:text-blue-600 transition"
          >
            <span>Mở danh sách người dùng</span>
            <ArrowRight className="w-3.5 h-3.5" />
          </Link>
        </div>

        {/* Module 2: Kiểm duyệt tin trọ */}
        <div className="bg-white rounded-xl p-6 shadow-subtle">
          <div className="flex items-center gap-2 mb-3">
            <Home className="w-4 h-4 stroke-[1.5] text-slate-600" />
            <h3 className="text-sm font-bold text-slate-900">
              Kiểm Duyệt Ghép Trọ
            </h3>
          </div>
          <p className="text-xs text-slate-500 leading-relaxed mb-4">
            Rà soát nội dung bài đăng tìm bạn ở ghép và phòng trọ của sinh viên. Đóng hoặc ẩn bài viết vi phạm tiêu chuẩn cộng đồng.
          </p>
          <Link
            href="/admin/moderation"
            className="inline-flex items-center gap-1.5 text-xs font-semibold text-slate-900 hover:text-blue-600 transition"
          >
            <span>Quản lý bài đăng</span>
            <ArrowRight className="w-3.5 h-3.5" />
          </Link>
        </div>

        {/* Module 3: Soạn thảo thông báo trường */}
        <div className="bg-white rounded-xl p-6 shadow-subtle">
          <div className="flex items-center gap-2 mb-3">
            <Bell className="w-4 h-4 stroke-[1.5] text-slate-600" />
            <h3 className="text-sm font-bold text-slate-900">
              Thông Báo Nhà Trường
            </h3>
          </div>
          <p className="text-xs text-slate-500 leading-relaxed mb-4">
            Phát hành các thông báo khẩn cấp, khảo thí, đào tạo và học phí tới toàn thể sinh viên và giảng viên qua app và portal.
          </p>
          <Link
            href="/admin/announcements"
            className="inline-flex items-center gap-1.5 text-xs font-semibold text-slate-900 hover:text-blue-600 transition"
          >
            <span>Soạn thảo thông báo</span>
            <ArrowRight className="w-3.5 h-3.5" />
          </Link>
        </div>
      </div>
    </AdminLayout>
  );
}
