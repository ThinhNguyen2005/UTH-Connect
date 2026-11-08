"use client";

import React, { useEffect, useState } from "react";
import Link from "next/link";
import { useGetIdentity } from "@refinedev/core";
import { PortalLayout } from "@/components/layout/PortalLayout";
import { supabaseBrowserClient } from "@/utils/supabase-client";
import { Calendar, Plus, MapPin, Users, Tag } from "lucide-react";
import type { AppUserIdentity } from "@/providers/auth-provider";

export default function EventsListPage() {
  const { data: user } = useGetIdentity<AppUserIdentity>();
  const [events, setEvents] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedCampus, setSelectedCampus] = useState<string>("ALL");

  const role = user?.role || "student";
  const canCreate = role === "admin" || role === "lecturer" || role === "organizer";

  const campuses = [
    { label: "Tất cả cơ sở", value: "ALL" },
    { label: "Cơ sở 1 (Bình Thạnh)", value: "CS1" },
    { label: "Cơ sở 2 (Quận 12)", value: "CS2" },
    { label: "Cơ sở 3 (Thủ Đức)", value: "CS3" },
  ];

  useEffect(() => {
    let isMounted = true;

    async function fetchEvents() {
      setLoading(true);
      let query = supabaseBrowserClient
        .from("events")
        .select("*")
        .order("start_at", { ascending: true });

      if (selectedCampus !== "ALL") {
        query = query.eq("campus", selectedCampus);
      }

      const { data, error } = await query;
      if (!isMounted) return;

      if (!error && data) {
        setEvents(data);
      }
      setLoading(false);
    }

    fetchEvents();

    return () => {
      isMounted = false;
    };
  }, [selectedCampus]);

  return (
    <PortalLayout>
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
        <div>
          <h1 className="text-xl font-bold text-slate-900 tracking-tight">
            Sự Kiện & Hoạt Động Đoàn - Hội UTH
          </h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Tham gia các workshop, hội thảo khoa học và tích lũy Điểm rèn luyện
          </p>
        </div>

        {canCreate && (
          <Link
            href="/events/create"
            className="inline-flex items-center gap-1.5 px-3 py-2 bg-slate-900 text-white rounded-lg text-xs font-semibold hover:bg-slate-800 transition active:scale-[0.98]"
          >
            <Plus className="w-3.5 h-3.5" />
            <span>Tổ chức sự kiện mới</span>
          </Link>
        )}
      </div>

      {/* Campus Filter */}
      <div className="flex flex-wrap gap-1.5 mb-6">
        {campuses.map((c) => (
          <button
            key={c.value}
            onClick={() => setSelectedCampus(c.value)}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium transition active:scale-[0.98] ${
              selectedCampus === c.value
                ? "bg-slate-900 text-white"
                : "bg-white text-slate-600 hover:bg-slate-100 shadow-subtle"
            }`}
          >
            {c.label}
          </button>
        ))}
      </div>

      {/* Events List (No borders, 12px rounded cards, subtle shadow) */}
      {loading ? (
        <div className="py-12 text-center text-xs text-slate-400">
          Đang tải danh sách sự kiện...
        </div>
      ) : events.length === 0 ? (
        <div className="py-12 bg-white rounded-xl shadow-subtle text-center">
          <Calendar className="w-8 h-8 text-slate-300 stroke-[1.5] mx-auto mb-2" />
          <div className="text-sm font-semibold text-slate-800">
            Không có sự kiện nào sắp diễn ra
          </div>
          <p className="text-xs text-slate-400 mt-1">
            Hãy theo dõi thường xuyên để không bỏ lỡ các hoạt động hấp dẫn tại UTH.
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {events.map((evt) => (
            <div
              key={evt.id}
              className="bg-white rounded-xl p-5 shadow-subtle flex flex-col justify-between hover:bg-[#F8FAFC]/50 transition"
            >
              <div>
                <div className="flex items-center justify-between gap-2 mb-2.5">
                  <span className="text-[10px] font-semibold px-2 py-0.5 rounded bg-emerald-50 text-emerald-800 tabular-nums">
                    +{evt.points || 0} ĐRL
                  </span>
                  <span className="text-[10px] font-semibold px-2 py-0.5 rounded bg-slate-100 text-slate-600">
                    {evt.campus || "CS1"}
                  </span>
                </div>

                <h3 className="text-sm font-bold text-slate-900 mb-1.5 line-clamp-2">
                  {evt.title}
                </h3>

                <p className="text-xs text-slate-500 line-clamp-2 mb-3 leading-relaxed">
                  {evt.description || "Chưa có mô tả chi tiết."}
                </p>

                <div className="space-y-1 text-[11px] text-slate-400 mb-4">
                  <div className="flex items-center gap-1.5">
                    <Calendar className="w-3.5 h-3.5 shrink-0" />
                    <span className="tabular-nums">
                      {new Date(evt.start_at).toLocaleString("vi-VN")}
                    </span>
                  </div>
                  <div className="flex items-center gap-1.5">
                    <MapPin className="w-3.5 h-3.5 shrink-0" />
                    <span className="truncate">{evt.location || "Hội trường"}</span>
                  </div>
                </div>
              </div>

              <div className="pt-3 border-t border-slate-100 flex items-center justify-between">
                <div className="flex items-center gap-1 text-[11px] text-slate-500 tabular-nums">
                  <Users className="w-3.5 h-3.5 stroke-[1.5]" />
                  <span>
                    {evt.current_registered || 0}
                    {evt.max_participants > 0 ? ` / ${evt.max_participants}` : " đăng ký"}
                  </span>
                </div>

                <span
                  className={`text-[10px] font-semibold px-2 py-0.5 rounded ${
                    evt.status === "PUBLISHED"
                      ? "bg-emerald-50 text-emerald-700"
                      : "bg-slate-100 text-slate-600"
                  }`}
                >
                  {evt.status === "PUBLISHED" ? "Đang mở" : evt.status}
                </span>
              </div>
            </div>
          ))}
        </div>
      )}
    </PortalLayout>
  );
}
