"use client";

import React, { useEffect, useState } from "react";
import Link from "next/link";
import { useGetIdentity } from "@refinedev/core";
import { PortalLayout } from "@/components/layout/PortalLayout";
import { supabaseBrowserClient } from "@/utils/supabase-client";
import {
  Bell,
  Calendar,
  Home,
  Plus,
  ArrowRight,
  Pin,
  MapPin,
  Building2,
} from "lucide-react";
import type { AppUserIdentity } from "@/providers/auth-provider";

export default function PortalHomePage() {
  const [mounted, setMounted] = useState(false);
  const { data: user } = useGetIdentity<AppUserIdentity>();
  const [stats, setStats] = useState({
    eventsCount: 0,
    notificationsCount: 0,
    housingCount: 0,
  });
  const [notifications, setNotifications] = useState<any[]>([]);
  const [events, setEvents] = useState<any[]>([]);
  const [housing, setHousing] = useState<any[]>([]);

  useEffect(() => {
    setMounted(true);
    if (typeof window !== "undefined") {
      const searchParams = new URLSearchParams(window.location.search);
      const code = searchParams.get("code");
      if (code) {
        window.location.href = `/auth/callback${window.location.search}`;
        return;
      }

      if (window.location.hash) {
        const hash = window.location.hash.substring(1);
        const params = new URLSearchParams(hash);
        const type = params.get("type");
        const error = params.get("error");
        const errorCode = params.get("error_code");

        if (type === "recovery" || errorCode || error) {
          window.location.href = `/reset-password${window.location.hash}`;
        }
      }
    }
  }, []);

  const role = user?.role || "student";
  const canPublish = role === "admin" || role === "lecturer" || role === "organizer";

  useEffect(() => {
    let isMounted = true;

    async function loadData() {
      const [{ count: evtCount }, { count: notifCount }, { count: hCount }] =
        await Promise.all([
          supabaseBrowserClient.from("events").select("*", { count: "exact", head: true }),
          supabaseBrowserClient.from("notifications").select("*", { count: "exact", head: true }),
          supabaseBrowserClient.from("housing_posts").select("*", { count: "exact", head: true }),
        ]);

      if (!isMounted) return;

      setStats({
        eventsCount: evtCount || 0,
        notificationsCount: notifCount || 0,
        housingCount: hCount || 0,
      });

      const { data: notifData } = await supabaseBrowserClient
        .from("notifications")
        .select("id, title, category, published_at, is_pinned")
        .order("is_pinned", { ascending: false })
        .order("published_at", { ascending: false })
        .limit(4);

      if (!isMounted) return;
      if (notifData) setNotifications(notifData);

      const { data: evtData } = await supabaseBrowserClient
        .from("events")
        .select("id, title, campus, location, start_at, points")
        .order("start_at", { ascending: true })
        .limit(3);

      if (!isMounted) return;
      if (evtData) setEvents(evtData);

      const { data: houseData } = await supabaseBrowserClient
        .from("housing_posts")
        .select("id, title, monthly_rent_vnd, campus, type")
        .order("created_at", { ascending: false })
        .limit(3);

      if (!isMounted) return;
      if (houseData) setHousing(houseData);
    }

    loadData();

    return () => {
      isMounted = false;
    };
  }, []);

  return (
    <PortalLayout>
      {/* Welcome & Quick Action Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
        <div>
          <h1 className="text-xl font-bold text-slate-900 tracking-tight">
            Xin chào, {mounted && user?.name ? user.name : "Sinh viên UTH"}
          </h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Cổng thông tin và tiện ích sinh viên Trường ĐH Giao thông vận tải TP.HCM
          </p>
        </div>

        <div className="flex items-center gap-2">
          {mounted && canPublish && (
            <Link
              href="/notifications/create"
              className="inline-flex items-center gap-1.5 px-3 py-2 bg-slate-900 text-white rounded-lg text-xs font-medium hover:bg-slate-800 transition active:scale-[0.98]"
            >
              <Plus className="w-3.5 h-3.5" />
              <span>Đăng thông báo</span>
            </Link>
          )}
          <Link
            href="/housing/create"
            className="inline-flex items-center gap-1.5 px-3 py-2 bg-white text-slate-700 hover:bg-slate-50 rounded-lg text-xs font-medium shadow-subtle transition active:scale-[0.98]"
          >
            <Plus className="w-3.5 h-3.5" />
            <span>Đăng tin tìm trọ</span>
          </Link>
        </div>
      </div>

      {/* KPI Tonal Row (No borders, pure white cards on slate 50) */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6">
        <div className="bg-white rounded-xl p-5 shadow-subtle flex items-center justify-between">
          <div>
            <div className="text-[11px] font-semibold uppercase tracking-wider text-slate-400">
              Thông báo trường
            </div>
            <div className="text-2xl font-bold text-slate-900 mt-0.5 tabular-nums">
              {stats.notificationsCount}
            </div>
            <div className="text-[11px] text-slate-500 mt-0.5">Mới cập nhật trong kỳ</div>
          </div>
          <Bell className="w-5 h-5 stroke-[1.5] text-slate-400" />
        </div>

        <div className="bg-white rounded-xl p-5 shadow-subtle flex items-center justify-between">
          <div>
            <div className="text-[11px] font-semibold uppercase tracking-wider text-slate-400">
              Sự kiện & Hoạt động
            </div>
            <div className="text-2xl font-bold text-slate-900 mt-0.5 tabular-nums">
              {stats.eventsCount}
            </div>
            <div className="text-[11px] text-slate-500 mt-0.5">Tích lũy Điểm rèn luyện</div>
          </div>
          <Calendar className="w-5 h-5 stroke-[1.5] text-slate-400" />
        </div>

        <div className="bg-white rounded-xl p-5 shadow-subtle flex items-center justify-between">
          <div>
            <div className="text-[11px] font-semibold uppercase tracking-wider text-slate-400">
              Phòng trọ & Ở ghép
            </div>
            <div className="text-2xl font-bold text-slate-900 mt-0.5 tabular-nums">
              {stats.housingCount}
            </div>
            <div className="text-[11px] text-slate-500 mt-0.5">Sinh viên đang tìm bạn</div>
          </div>
          <Home className="w-5 h-5 stroke-[1.5] text-slate-400" />
        </div>
      </div>

      {/* Asymmetric Bento Layout: 2/3 Content Feed, 1/3 Quick Hub */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left Column (2/3): Latest Announcements & Upcoming Events */}
        <div className="lg:col-span-2 space-y-6">
          {/* Notifications Card */}
          <div className="bg-white rounded-xl p-6 shadow-subtle">
            <div className="flex items-center justify-between mb-4">
              <div className="text-sm font-bold text-slate-900 tracking-tight">
                Thông báo mới nhất
              </div>
              <Link
                href="/notifications"
                className="text-xs font-semibold text-slate-600 hover:text-slate-900 flex items-center gap-1 transition"
              >
                <span>Xem tất cả</span>
                <ArrowRight className="w-3.5 h-3.5" />
              </Link>
            </div>

            <div className="space-y-3">
              {notifications.length === 0 ? (
                <div className="py-6 text-center text-xs text-slate-400">
                  Chưa có thông báo nào.
                </div>
              ) : (
                notifications.map((n) => (
                  <Link
                    key={n.id}
                    href="/notifications"
                    className="block p-3 rounded-lg hover:bg-[#F8FAFC] transition group"
                  >
                    <div className="flex items-center gap-2 mb-1">
                      {n.is_pinned && (
                        <span className="inline-flex items-center gap-1 text-[10px] font-semibold text-amber-700 bg-amber-50 px-1.5 py-0.5 rounded">
                          <Pin className="w-3 h-3 text-amber-600" />
                          Ghim
                        </span>
                      )}
                      <span className="text-[10px] font-semibold text-slate-600 bg-slate-100 px-1.5 py-0.5 rounded">
                        {n.category || "GENERAL"}
                      </span>
                      <span suppressHydrationWarning className="text-[11px] text-slate-400 tabular-nums">
                        {new Date(n.published_at).toLocaleDateString("vi-VN")}
                      </span>
                    </div>
                    <div className="text-xs font-semibold text-slate-800 group-hover:text-slate-900 line-clamp-1">
                      {n.title}
                    </div>
                  </Link>
                ))
              )}
            </div>
          </div>

          {/* Upcoming Events Card */}
          <div className="bg-white rounded-xl p-6 shadow-subtle">
            <div className="flex items-center justify-between mb-4">
              <div className="text-sm font-bold text-slate-900 tracking-tight">
                Sự kiện sắp diễn ra
              </div>
              <Link
                href="/events"
                className="text-xs font-semibold text-slate-600 hover:text-slate-900 flex items-center gap-1 transition"
              >
                <span>Xem lịch</span>
                <ArrowRight className="w-3.5 h-3.5" />
              </Link>
            </div>

            <div className="space-y-3">
              {events.length === 0 ? (
                <div className="py-6 text-center text-xs text-slate-400">
                  Chưa có sự kiện nào.
                </div>
              ) : (
                events.map((e) => (
                  <Link
                    key={e.id}
                    href="/events"
                    className="block p-3 rounded-lg hover:bg-[#F8FAFC] transition group"
                  >
                    <div className="flex items-center justify-between gap-2 mb-1">
                      <div className="text-xs font-semibold text-slate-800 group-hover:text-slate-900 line-clamp-1">
                        {e.title}
                      </div>
                      <span className="text-[10px] font-semibold text-emerald-700 bg-emerald-50 px-1.5 py-0.5 rounded shrink-0 tabular-nums">
                        +{e.points || 0} ĐRL
                      </span>
                    </div>
                    <div className="flex items-center gap-3 text-[11px] text-slate-400">
                      <span suppressHydrationWarning>{new Date(e.start_at).toLocaleDateString("vi-VN")}</span>
                      <span>·</span>
                      <span className="flex items-center gap-1">
                        <MapPin className="w-3 h-3" />
                        {e.campus} - {e.location}
                      </span>
                    </div>
                  </Link>
                ))
              )}
            </div>
          </div>
        </div>

        {/* Right Column (1/3): Housing Feed & Campus Shortcuts */}
        <div className="space-y-6">
          {/* Housing Posts Widget */}
          <div className="bg-white rounded-xl p-6 shadow-subtle">
            <div className="flex items-center justify-between mb-4">
              <div className="text-sm font-bold text-slate-900 tracking-tight">
                Tìm trọ & Ở ghép
              </div>
              <Link
                href="/housing"
                className="text-xs font-semibold text-slate-600 hover:text-slate-900 flex items-center gap-1 transition"
              >
                <span>Tất cả</span>
                <ArrowRight className="w-3.5 h-3.5" />
              </Link>
            </div>

            <div className="space-y-3">
              {housing.length === 0 ? (
                <div className="py-6 text-center text-xs text-slate-400">
                  Chưa có tin trọ nào.
                </div>
              ) : (
                housing.map((h) => (
                  <div key={h.id} className="p-3 rounded-lg bg-[#F8FAFC]">
                    <div className="flex items-center justify-between text-[10px] text-slate-500 mb-1">
                      <span className="font-semibold text-slate-700 uppercase">
                        {h.campus ? h.campus.replace(/_/g, " ") : "UTH"}
                      </span>
                      <span className="font-bold text-emerald-700 tabular-nums">
                        {new Intl.NumberFormat("vi-VN").format(h.monthly_rent_vnd || h.price || 0)} đ
                      </span>
                    </div>
                    <div className="text-xs font-medium text-slate-800 line-clamp-1">
                      {h.title}
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>

          {/* Campus Directory Card */}
          <div className="bg-white rounded-xl p-6 shadow-subtle">
            <div className="flex items-center gap-2 text-sm font-bold text-slate-900 mb-3">
              <Building2 className="w-4 h-4 stroke-[1.5] text-slate-500" />
              <span>Cơ sở Đào tạo UTH</span>
            </div>
            <div className="space-y-2 text-xs text-slate-600">
              <div className="p-2.5 rounded-lg bg-[#F8FAFC]">
                <div className="font-semibold text-slate-800">Cơ sở 1 (Trụ sở chính)</div>
                <div className="text-[11px] text-slate-500 mt-0.5">Số 2 Võ Oanh, P.25, Bình Thạnh</div>
              </div>
              <div className="p-2.5 rounded-lg bg-[#F8FAFC]">
                <div className="font-semibold text-slate-800">Cơ sở 2 (Thực hành & KTX)</div>
                <div className="text-[11px] text-slate-500 mt-0.5">Số 10 Tô Ký, P. Tân Chánh Hiệp, Q.12</div>
              </div>
              <div className="p-2.5 rounded-lg bg-[#F8FAFC]">
                <div className="font-semibold text-slate-800">Cơ sở 3 (Khuôn viên mới)</div>
                <div className="text-[11px] text-slate-500 mt-0.5">Số 70 Tô Ký, P. Tân Chánh Hiệp, Q.12</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </PortalLayout>
  );
}
