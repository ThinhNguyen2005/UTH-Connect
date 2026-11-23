"use client";

import React, { useEffect, useState } from "react";
import Link from "next/link";
import { useGetIdentity } from "@refinedev/core";
import { PortalLayout } from "@/components/layout/PortalLayout";
import { supabaseBrowserClient } from "@/utils/supabase-client";
import { Calendar, Plus, MapPin, Users, Tag, Download, Check } from "lucide-react";
import type { AppUserIdentity } from "@/providers/auth-provider";

export default function EventsListPage() {
  const { data: user } = useGetIdentity<AppUserIdentity>();
  const [events, setEvents] = useState<any[]>([]);
  const [registeredEventIds, setRegisteredEventIds] = useState<Set<string>>(new Set());
  const [loading, setLoading] = useState(true);
  const [processingId, setProcessingId] = useState<string | null>(null);
  const [selectedCampus, setSelectedCampus] = useState<string>("ALL");

  const role = user?.role || "student";
  const canManage = role === "admin" || role === "lecturer" || role === "organizer";
  const canCreate = canManage;

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

      if (user?.id) {
        const { data: myRegs } = await supabaseBrowserClient
          .from("registrations")
          .select("event_id")
          .eq("user_id", user.id);

        if (myRegs && isMounted) {
          setRegisteredEventIds(new Set(myRegs.map((r) => r.event_id)));
        }
      }
      setLoading(false);
    }

    fetchEvents();

    return () => {
      isMounted = false;
    };
  }, [selectedCampus, user?.id]);

  const handleRegisterEvent = async (eventId: string) => {
    if (!user) {
      alert("Vui lòng đăng nhập để đăng ký vé tham gia sự kiện!");
      return;
    }
    setProcessingId(eventId);
    const { error } = await supabaseBrowserClient.from("registrations").insert({
      event_id: eventId,
      user_id: user.id,
      email: user.email,
      display_name: user.name || user.email,
      student_id: (user as any).studentId || user.email.split("@")[0] || "",
      status: "REGISTERED",
    });

    if (error) {
      alert("Đăng ký thất bại hoặc bạn đã có vé cho sự kiện này rồi!");
    } else {
      setRegisteredEventIds((prev) => new Set([...prev, eventId]));
      // Update local count
      setEvents((prev) =>
        prev.map((e) =>
          e.id === eventId
            ? { ...e, current_registered: (e.current_registered || 0) + 1 }
            : e
        )
      );
      alert("Đăng ký tham gia sự kiện thành công! Bạn có thể xem vé trên App hoặc tra cứu điểm danh.");
    }
    setProcessingId(null);
  };

  const handleExportAttendance = async (event: any) => {
    setProcessingId(event.id);
    const [{ data: regs }, { data: atts }] = await Promise.all([
      supabaseBrowserClient
        .from("registrations")
        .select("user_id, student_id, display_name, email, status, registered_at")
        .eq("event_id", event.id),
      supabaseBrowserClient
        .from("attendance")
        .select("user_id, status, checked_in_at, checked_in_by")
        .eq("event_id", event.id),
    ]);

    const attMap = new Map((atts || []).map((a) => [a.user_id, a]));
    const nowStr = new Date().toLocaleString("vi-VN", { timeZone: "Asia/Ho_Chi_Minh" });

    const rows: (string | number | null | undefined)[][] = [
      ["Sự kiện", event.title],
      ["Thời gian xuất báo cáo", `${nowStr} Asia/Ho_Chi_Minh`],
      [],
      ["MSSV", "Họ và tên", "Email", "Trạng thái điểm danh", "Thời gian điểm danh", "Người điểm danh"],
    ];

    (regs || []).forEach((reg) => {
      const att = attMap.get(reg.user_id);
      const isCheckedIn = Boolean(att);
      const checkedInTime = att?.checked_in_at
        ? new Date(att.checked_in_at).toLocaleString("vi-VN", { timeZone: "Asia/Ho_Chi_Minh" })
        : "";

      rows.push([
        reg.student_id || "",
        reg.display_name || "",
        reg.email || "",
        isCheckedIn ? "Đã điểm danh" : "Chưa điểm danh",
        checkedInTime,
        att?.checked_in_by || "",
      ]);
    });

    const escapeCsvCell = (value: string | number | null | undefined): string => {
      if (value === null || value === undefined) return '""';
      const str = String(value);
      const safe = /^[=+\-@]/.test(str.trimStart()) ? `'${str}` : str;
      return `"${safe.replace(/"/g, '""')}"`;
    };

    const content = rows.map((r) => r.map(escapeCsvCell).join(",")).join("\r\n");
    const blob = new Blob(["\uFEFF" + content], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `attendance-${event.id}.csv`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
    setProcessingId(null);
  };

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

              <div className="pt-3 border-t border-slate-100 flex items-center justify-between gap-2">
                <div className="flex items-center gap-1 text-[11px] text-slate-500 tabular-nums">
                  <Users className="w-3.5 h-3.5 stroke-[1.5]" />
                  <span>
                    {evt.current_registered || 0}
                    {evt.max_participants > 0 ? ` / ${evt.max_participants}` : " đăng ký"}
                  </span>
                </div>

                <div className="flex items-center gap-2">
                  {canManage ? (
                    <button
                      onClick={() => handleExportAttendance(evt)}
                      disabled={processingId === evt.id}
                      className="inline-flex items-center gap-1 px-2.5 py-1 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded text-[11px] font-semibold transition active:scale-[0.98]"
                      title="Xuất file CSV danh sách điểm danh cho sự kiện này"
                    >
                      <Download className="w-3 h-3 stroke-[1.8]" />
                      <span>{processingId === evt.id ? "Đang xuất..." : "Xuất CSV"}</span>
                    </button>
                  ) : registeredEventIds.has(evt.id) ? (
                    <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[10px] font-semibold bg-emerald-50 text-emerald-700">
                      <Check className="w-3 h-3 stroke-[2]" />
                      <span>Đã có vé</span>
                    </span>
                  ) : (
                    <button
                      onClick={() => handleRegisterEvent(evt.id)}
                      disabled={processingId === evt.id || (evt.max_participants > 0 && evt.current_registered >= evt.max_participants)}
                      className="inline-flex items-center gap-1 px-2.5 py-1 bg-slate-900 hover:bg-slate-800 text-white rounded text-[11px] font-semibold transition active:scale-[0.98] disabled:opacity-50"
                    >
                      <span>{processingId === evt.id ? "Đang xử lý..." : "Đăng ký vé"}</span>
                    </button>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </PortalLayout>
  );
}
