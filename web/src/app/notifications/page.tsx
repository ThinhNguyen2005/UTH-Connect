"use client";

import React, { useEffect, useState } from "react";
import Link from "next/link";
import { useGetIdentity } from "@refinedev/core";
import { PortalLayout } from "@/components/layout/PortalLayout";
import { supabaseBrowserClient } from "@/utils/supabase-client";
import { Bell, Plus, Pin, Calendar, Search } from "lucide-react";
import type { AppUserIdentity } from "@/providers/auth-provider";

export default function NotificationsListPage() {
  const { data: user } = useGetIdentity<AppUserIdentity>();
  const [notifications, setNotifications] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedCategory, setSelectedCategory] = useState<string>("ALL");
  const [searchTerm, setSearchTerm] = useState("");

  const role = user?.role || "student";
  const canPublish = role === "admin" || role === "lecturer" || role === "organizer";

  const categories = [
    { label: "Tất cả", value: "ALL" },
    { label: "Đào tạo", value: "ACADEMIC" },
    { label: "Khảo thí & Lịch thi", value: "EXAMINATION" },
    { label: "Học phí", value: "TUITION" },
    { label: "Hoạt động & Sự kiện", value: "ACTIVITIES" },
    { label: "Thông báo chung", value: "GENERAL" },
  ];

  useEffect(() => {
    let isMounted = true;

    async function fetchNotifications() {
      setLoading(true);
      let query = supabaseBrowserClient
        .from("notifications")
        .select("*")
        .order("is_pinned", { ascending: false })
        .order("published_at", { ascending: false });

      if (selectedCategory !== "ALL") {
        query = query.eq("category", selectedCategory);
      }

      const { data, error } = await query;
      if (!isMounted) return;

      if (!error && data) {
        setNotifications(data);
      }
      setLoading(false);
    }

    fetchNotifications();

    return () => {
      isMounted = false;
    };
  }, [selectedCategory]);

  const filtered = notifications.filter((item) =>
    item.title?.toLowerCase().includes(searchTerm.toLowerCase()) ||
    item.summary?.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <PortalLayout>
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
        <div>
          <h1 className="text-xl font-bold text-slate-900 tracking-tight">
            Thông Báo Nhà Trường
          </h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Thông tin chính thức từ Ban Giám Hiệu, Phòng Đào Tạo và các Khoa
          </p>
        </div>

        {canPublish && (
          <Link
            href="/notifications/create"
            className="inline-flex items-center gap-1.5 px-3 py-2 bg-slate-900 text-white rounded-lg text-xs font-semibold hover:bg-slate-800 transition active:scale-[0.98]"
          >
            <Plus className="w-3.5 h-3.5" />
            <span>Đăng thông báo mới</span>
          </Link>
        )}
      </div>

      {/* Categories & Search */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-3 mb-6">
        <div className="flex flex-wrap gap-1.5">
          {categories.map((c) => (
            <button
              key={c.value}
              onClick={() => setSelectedCategory(c.value)}
              className={`px-3 py-1.5 rounded-lg text-xs font-medium transition active:scale-[0.98] ${
                selectedCategory === c.value
                  ? "bg-slate-900 text-white"
                  : "bg-white text-slate-600 hover:bg-slate-100 shadow-subtle"
              }`}
            >
              {c.label}
            </button>
          ))}
        </div>

        <div className="relative w-full md:w-64">
          <Search className="w-3.5 h-3.5 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 stroke-[1.5]" />
          <input
            type="text"
            placeholder="Tìm kiếm thông báo..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-9 pr-3 py-1.5 text-xs bg-white rounded-lg shadow-subtle focus:outline-none focus:ring-1 focus:ring-slate-900 transition"
          />
        </div>
      </div>

      {/* Notifications List (No card borders, pure white on slate 50, rounded 12px) */}
      {loading ? (
        <div className="py-12 text-center text-xs text-slate-400">
          Đang tải thông báo từ hệ thống...
        </div>
      ) : filtered.length === 0 ? (
        <div className="py-12 bg-white rounded-xl shadow-subtle text-center">
          <Bell className="w-8 h-8 text-slate-300 stroke-[1.5] mx-auto mb-2" />
          <div className="text-sm font-semibold text-slate-800">
            Không có thông báo nào phù hợp
          </div>
          <p className="text-xs text-slate-400 mt-1">
            Hãy thử chọn danh mục khác hoặc tìm kiếm từ khóa khác.
          </p>
        </div>
      ) : (
        <div className="space-y-3">
          {filtered.map((notif) => (
            <div
              key={notif.id}
              className={`p-5 bg-white rounded-xl shadow-subtle transition hover:bg-[#F8FAFC]/50 ${
                notif.is_pinned ? "ring-1 ring-amber-200" : ""
              }`}
            >
              <div className="flex items-center gap-2 mb-1.5">
                {notif.is_pinned && (
                  <span className="inline-flex items-center gap-1 text-[10px] font-semibold text-amber-700 bg-amber-50 px-1.5 py-0.5 rounded">
                    <Pin className="w-3 h-3 text-amber-600" />
                    Ghim quan trọng
                  </span>
                )}
                <span className="text-[10px] font-semibold text-slate-600 bg-slate-100 px-2 py-0.5 rounded">
                  {notif.category || "GENERAL"}
                </span>
                <span className="text-[11px] text-slate-400 tabular-nums">
                  {new Date(notif.published_at).toLocaleString("vi-VN")}
                </span>
              </div>

              <h3 className="text-sm font-bold text-slate-900 hover:text-blue-600 transition cursor-pointer mb-1.5">
                {notif.title}
              </h3>

              {notif.summary && (
                <p className="text-xs text-slate-600 line-clamp-2 leading-relaxed mb-2">
                  {notif.summary}
                </p>
              )}

              <div className="text-[11px] text-slate-400">
                Đơn vị ban hành: <span className="text-slate-700 font-medium">{notif.author || "Ban Giám Hiệu"}</span>
              </div>
            </div>
          ))}
        </div>
      )}
    </PortalLayout>
  );
}
