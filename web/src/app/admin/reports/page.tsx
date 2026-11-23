"use client";

import React, { useEffect, useState } from "react";
import { AdminLayout } from "@/components/layout/AdminLayout";
import { supabaseBrowserClient } from "@/utils/supabase-client";
import {
  FileSpreadsheet,
  Download,
  Calendar,
  Users,
  Home,
  BookOpen,
  CheckCircle2,
  Clock,
  Search,
} from "lucide-react";

interface EventItem {
  id: string;
  title: string;
  campus: string | null;
  location: string | null;
  start_at: string;
  end_at: string;
  current_registered: number;
  max_participants: number;
  points: number;
  status: string;
  organizer_name: string | null;
}

export default function AdminReportsPage() {
  const [activeTab, setActiveTab] = useState<"attendance" | "events" | "users" | "community">("attendance");
  const [events, setEvents] = useState<EventItem[]>([]);
  const [selectedEventId, setSelectedEventId] = useState<string>("");
  const [searchEvent, setSearchEvent] = useState("");
  const [isExporting, setIsExporting] = useState(false);
  const [exportMessage, setExportMessage] = useState<string | null>(null);

  useEffect(() => {
    async function loadEvents() {
      const { data } = await supabaseBrowserClient
        .from("events")
        .select("*")
        .order("start_at", { ascending: false });

      if (data && data.length > 0) {
        setEvents(data);
        setSelectedEventId(data[0].id);
      }
    }
    loadEvents();
  }, []);

  const escapeCsvCell = (value: string | number | null | undefined): string => {
    if (value === null || value === undefined) return '""';
    const str = String(value);
    const safe = /^[=+\-@]/.test(str.trimStart()) ? `'${str}` : str;
    return `"${safe.replace(/"/g, '""')}"`;
  };

  const downloadCsv = (filename: string, rows: (string | number | null | undefined)[][]) => {
    const content = rows.map((r) => r.map(escapeCsvCell).join(",")).join("\r\n");
    const blob = new Blob(["\uFEFF" + content], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  };

  // 1. Xuất danh sách điểm danh sự kiện
  const handleExportAttendance = async () => {
    if (!selectedEventId) return;
    setIsExporting(true);
    setExportMessage(null);

    const event = events.find((e) => e.id === selectedEventId);
    const eventTitle = event?.title || "Sự kiện";

    const [{ data: regs }, { data: atts }] = await Promise.all([
      supabaseBrowserClient
        .from("registrations")
        .select("user_id, student_id, display_name, email, status, registered_at")
        .eq("event_id", selectedEventId),
      supabaseBrowserClient
        .from("attendance")
        .select("user_id, status, checked_in_at, checked_in_by")
        .eq("event_id", selectedEventId),
    ]);

    const attMap = new Map((atts || []).map((a) => [a.user_id, a]));
    const nowStr = new Date().toLocaleString("vi-VN", { timeZone: "Asia/Ho_Chi_Minh" });

    const rows: (string | number | null | undefined)[][] = [
      ["Sự kiện", eventTitle],
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

    downloadCsv(`attendance-${selectedEventId}.csv`, rows);
    setIsExporting(false);
    setExportMessage(`Đã xuất thành công ${regs?.length || 0} sinh viên trong danh sách điểm danh!`);
  };

  // 2. Xuất toàn bộ sự kiện
  const handleExportEvents = async () => {
    setIsExporting(true);
    setExportMessage(null);

    const { data: allEvts } = await supabaseBrowserClient
      .from("events")
      .select("*")
      .order("start_at", { ascending: false });

    const rows: (string | number | null | undefined)[][] = [
      ["BÁO CÁO TOÀN BỘ SỰ KIỆN UTH CONNECT", ""],
      ["Thời gian xuất", new Date().toLocaleString("vi-VN")],
      [],
      [
        "Mã sự kiện",
        "Tên sự kiện",
        "Cơ sở",
        "Địa điểm",
        "Bắt đầu",
        "Kết thúc",
        "Đã đăng ký",
        "Sức chứa",
        "Điểm rèn luyện",
        "Trạng thái",
        "Ban tổ chức",
      ],
    ];

    (allEvts || []).forEach((e) => {
      rows.push([
        e.id,
        e.title,
        e.campus || "",
        e.location || "",
        new Date(e.start_at).toLocaleString("vi-VN"),
        new Date(e.end_at).toLocaleString("vi-VN"),
        e.current_registered || 0,
        e.max_participants || 0,
        e.points || 0,
        e.status,
        e.organizer_name || "",
      ]);
    });

    const dateStr = new Date().toISOString().slice(0, 10);
    downloadCsv(`events-report-${dateStr}.csv`, rows);
    setIsExporting(false);
    setExportMessage(`Đã xuất báo cáo ${allEvts?.length || 0} sự kiện.`);
  };

  // 3. Xuất người dùng & phân quyền RBAC
  const handleExportUsers = async () => {
    setIsExporting(true);
    setExportMessage(null);

    const { data: users } = await supabaseBrowserClient
      .from("profiles")
      .select("*")
      .order("created_at", { ascending: false });

    const rows: (string | number | null | undefined)[][] = [
      ["BÁO CÁO DANH SÁCH TÀI KHOẢN & PHÂN QUYỀN RBAC", ""],
      ["Thời gian xuất", new Date().toLocaleString("vi-VN")],
      [],
      ["ID", "Email", "Họ và tên", "Vai trò (Role)", "MSSV", "Đã xác thực SV", "Lớp", "Khoa", "Ngày tạo"],
    ];

    (users || []).forEach((u) => {
      rows.push([
        u.id,
        u.email,
        u.full_name || "",
        u.role,
        u.student_id || "",
        u.student_id_verified ? "Đã xác thực" : "Chưa xác thực",
        u.class_name || "",
        u.department || "",
        u.created_at ? new Date(u.created_at).toLocaleString("vi-VN") : "",
      ]);
    });

    const dateStr = new Date().toISOString().slice(0, 10);
    downloadCsv(`users-rbac-report-${dateStr}.csv`, rows);
    setIsExporting(false);
    setExportMessage(`Đã xuất báo cáo ${users?.length || 0} tài khoản.`);
  };

  // 4. Xuất tin trọ
  const handleExportHousing = async () => {
    setIsExporting(true);
    setExportMessage(null);

    const { data: posts } = await supabaseBrowserClient
      .from("housing_posts")
      .select("*")
      .order("created_at", { ascending: false });

    const rows: (string | number | null | undefined)[][] = [
      ["BÁO CÁO TIN ĐĂNG NHÀ TRỌ & GHÉP TRỌ", ""],
      ["Thời gian xuất", new Date().toLocaleString("vi-VN")],
      [],
      ["Mã tin", "Tiêu đề", "Cơ sở", "Giá thuê/tháng", "Chủ tin", "SĐT liên hệ", "Chỗ trống", "Trạng thái", "Ngày đăng"],
    ];

    (posts || []).forEach((p) => {
      rows.push([
        p.id,
        p.title,
        p.campus,
        p.monthly_rent_vnd,
        p.host_name,
        p.contact_phone,
        p.available_slots,
        p.status,
        p.created_at ? new Date(p.created_at).toLocaleString("vi-VN") : "",
      ]);
    });

    const dateStr = new Date().toISOString().slice(0, 10);
    downloadCsv(`housing-report-${dateStr}.csv`, rows);
    setIsExporting(false);
    setExportMessage(`Đã xuất báo cáo ${posts?.length || 0} tin trọ.`);
  };

  // 5. Xuất chợ sách
  const handleExportBooks = async () => {
    setIsExporting(true);
    setExportMessage(null);

    const { data: books } = await supabaseBrowserClient
      .from("book_listings")
      .select("*")
      .order("created_at", { ascending: false });

    const rows: (string | number | null | undefined)[][] = [
      ["BÁO CÁO BÀI ĐĂNG CHỢ SÁCH GIÁO TRÌNH", ""],
      ["Thời gian xuất", new Date().toLocaleString("vi-VN")],
      [],
      ["Mã bài", "Tên sách", "Môn học", "Giá bán (VND)", "Hình thức", "Tình trạng", "Trạng thái", "Người đăng", "Liên hệ", "Ngày đăng"],
    ];

    (books || []).forEach((b) => {
      rows.push([
        b.id,
        b.title,
        b.subject,
        b.price_vnd || 0,
        b.deal_type,
        b.condition,
        b.status,
        b.seller_name,
        b.contact,
        b.created_at ? new Date(b.created_at).toLocaleString("vi-VN") : "",
      ]);
    });

    const dateStr = new Date().toISOString().slice(0, 10);
    downloadCsv(`books-report-${dateStr}.csv`, rows);
    setIsExporting(false);
    setExportMessage(`Đã xuất báo cáo ${books?.length || 0} bài đăng sách.`);
  };

  const filteredEvents = events.filter((e) =>
    e.title.toLowerCase().includes(searchEvent.toLowerCase())
  );

  return (
    <AdminLayout>
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
        <div>
          <h1 className="text-xl font-bold text-slate-900 tracking-tight">
            Trung Tâm Xuất Báo Cáo &amp; Dữ Liệu
          </h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Xuất dữ liệu định dạng CSV chuẩn UTF-8 tương thích Microsoft Excel dành cho Ban Quản Trị và Ban Tổ Chức
          </p>
        </div>
      </div>

      {exportMessage && (
        <div className="mb-6 p-4 rounded-xl bg-emerald-50 text-emerald-800 text-xs font-semibold flex items-center gap-2 shadow-subtle">
          <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
          <span>{exportMessage}</span>
        </div>
      )}

      {/* Tabs */}
      <div className="flex gap-2 border-b border-slate-200/80 mb-6">
        <button
          onClick={() => setActiveTab("attendance")}
          className={`pb-3 text-xs font-semibold flex items-center gap-2 border-b-2 transition ${
            activeTab === "attendance"
              ? "border-slate-900 text-slate-900"
              : "border-transparent text-slate-500 hover:text-slate-800"
          }`}
        >
          <Clock className="w-4 h-4 stroke-[1.5]" />
          <span>Điểm danh Sự kiện</span>
        </button>

        <button
          onClick={() => setActiveTab("events")}
          className={`pb-3 text-xs font-semibold flex items-center gap-2 border-b-2 transition ${
            activeTab === "events"
              ? "border-slate-900 text-slate-900"
              : "border-transparent text-slate-500 hover:text-slate-800"
          }`}
        >
          <Calendar className="w-4 h-4 stroke-[1.5]" />
          <span>Tất cả Sự kiện</span>
        </button>

        <button
          onClick={() => setActiveTab("users")}
          className={`pb-3 text-xs font-semibold flex items-center gap-2 border-b-2 transition ${
            activeTab === "users"
              ? "border-slate-900 text-slate-900"
              : "border-transparent text-slate-500 hover:text-slate-800"
          }`}
        >
          <Users className="w-4 h-4 stroke-[1.5]" />
          <span>Tài khoản &amp; RBAC</span>
        </button>

        <button
          onClick={() => setActiveTab("community")}
          className={`pb-3 text-xs font-semibold flex items-center gap-2 border-b-2 transition ${
            activeTab === "community"
              ? "border-slate-900 text-slate-900"
              : "border-transparent text-slate-500 hover:text-slate-800"
          }`}
        >
          <Home className="w-4 h-4 stroke-[1.5]" />
          <span>Tiện ích Nhà trọ &amp; Chợ sách</span>
        </button>
      </div>

      {/* Tab 1: Điểm danh Sự kiện */}
      {activeTab === "attendance" && (
        <div className="space-y-6">
          <div className="bg-white rounded-xl p-6 shadow-subtle">
            <h2 className="text-sm font-bold text-slate-900 mb-2">
              Xuất Danh Sách Sinh Viên Điểm Danh Sự Kiện
            </h2>
            <p className="text-xs text-slate-500 mb-4">
              File CSV xuất ra chuẩn cấu trúc như trên ứng dụng Android: gồm MSSV, Họ tên, Email, Trạng thái Điểm danh, Giờ check-in và Người quét.
            </p>

            <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3 mb-4">
              <div className="relative flex-1">
                <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2 stroke-[1.5]" />
                <input
                  type="text"
                  placeholder="Tìm sự kiện theo tên..."
                  value={searchEvent}
                  onChange={(e) => setSearchEvent(e.target.value)}
                  className="w-full pl-9 pr-3 py-2 bg-slate-50 border-0 rounded-lg text-xs focus:ring-2 focus:ring-slate-900 focus:bg-white transition"
                />
              </div>

              <button
                onClick={handleExportAttendance}
                disabled={isExporting || !selectedEventId}
                className="inline-flex items-center justify-center gap-2 px-4 py-2 bg-slate-900 text-white rounded-lg text-xs font-semibold hover:bg-slate-800 transition active:scale-[0.98] disabled:opacity-50"
              >
                <Download className="w-4 h-4 stroke-[1.5]" />
                <span>{isExporting ? "Đang xử lý..." : "Tải CSV Điểm Danh"}</span>
              </button>
            </div>

            {/* Event list selection */}
            <div className="max-h-[360px] overflow-y-auto space-y-2 pr-1">
              {filteredEvents.map((evt) => (
                <div
                  key={evt.id}
                  onClick={() => setSelectedEventId(evt.id)}
                  className={`p-3.5 rounded-lg cursor-pointer transition flex items-center justify-between ${
                    selectedEventId === evt.id
                      ? "bg-slate-900 text-white shadow-sm"
                      : "bg-[#F8FAFC] text-slate-800 hover:bg-slate-100"
                  }`}
                >
                  <div>
                    <div className="font-semibold text-xs leading-snug line-clamp-1">
                      {evt.title}
                    </div>
                    <div
                      className={`text-[11px] mt-0.5 ${
                        selectedEventId === evt.id ? "text-slate-300" : "text-slate-400"
                      }`}
                    >
                      {new Date(evt.start_at).toLocaleDateString("vi-VN")} • {evt.campus || "Toàn trường"}
                    </div>
                  </div>

                  <div className="text-right">
                    <span
                      className={`text-[10px] font-semibold px-2 py-0.5 rounded tabular-nums ${
                        selectedEventId === evt.id
                          ? "bg-white/20 text-white"
                          : "bg-slate-200/80 text-slate-700"
                      }`}
                    >
                      {evt.current_registered} đăng ký
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* Tab 2: Tất cả Sự kiện */}
      {activeTab === "events" && (
        <div className="bg-white rounded-xl p-6 shadow-subtle">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h2 className="text-sm font-bold text-slate-900">
                Xuất Thống Kê Toàn Bộ Sự Kiện UTH
              </h2>
              <p className="text-xs text-slate-500 mt-0.5">
                Bao gồm danh sách sự kiện, thời gian, số lượng đăng ký, sức chứa và điểm rèn luyện.
              </p>
            </div>

            <button
              onClick={handleExportEvents}
              disabled={isExporting}
              className="inline-flex items-center gap-2 px-4 py-2 bg-slate-900 text-white rounded-lg text-xs font-semibold hover:bg-slate-800 transition active:scale-[0.98] disabled:opacity-50"
            >
              <Download className="w-4 h-4 stroke-[1.5]" />
              <span>{isExporting ? "Đang xuất..." : "Tải CSV Danh Sách Sự Kiện"}</span>
            </button>
          </div>
          <div className="p-4 rounded-lg bg-[#F8FAFC] text-xs text-slate-600">
            Hiện có <strong className="text-slate-900 font-semibold">{events.length}</strong> sự kiện được lưu trong hệ thống.
          </div>
        </div>
      )}

      {/* Tab 3: Tài khoản & RBAC */}
      {activeTab === "users" && (
        <div className="bg-white rounded-xl p-6 shadow-subtle">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h2 className="text-sm font-bold text-slate-900">
                Xuất Báo Cáo Tài Khoản &amp; Phân Quyền
              </h2>
              <p className="text-xs text-slate-500 mt-0.5">
                Danh sách chi tiết thành viên, email, vai trò (admin, lecturer, organizer, student) và trạng thái xác thực MSSV.
              </p>
            </div>

            <button
              onClick={handleExportUsers}
              disabled={isExporting}
              className="inline-flex items-center gap-2 px-4 py-2 bg-slate-900 text-white rounded-lg text-xs font-semibold hover:bg-slate-800 transition active:scale-[0.98] disabled:opacity-50"
            >
              <Download className="w-4 h-4 stroke-[1.5]" />
              <span>{isExporting ? "Đang xuất..." : "Tải CSV Người Dùng &amp; RBAC"}</span>
            </button>
          </div>
          <div className="p-4 rounded-lg bg-[#F8FAFC] text-xs text-slate-600">
            Hỗ trợ rà soát định kỳ danh sách sinh viên xác thực và ban tổ chức sự kiện.
          </div>
        </div>
      )}

      {/* Tab 4: Tiện ích Nhà trọ & Chợ sách */}
      {activeTab === "community" && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div className="bg-white rounded-xl p-6 shadow-subtle flex flex-col justify-between">
            <div>
              <div className="flex items-center gap-2 mb-2">
                <Home className="w-4 h-4 text-slate-700 stroke-[1.5]" />
                <h2 className="text-sm font-bold text-slate-900">
                  Báo Cáo Nhà Trọ &amp; Ghép Trọ
                </h2>
              </div>
              <p className="text-xs text-slate-500 mb-4 leading-relaxed">
                Xuất danh sách toàn bộ tin trọ: giá thuê, cơ sở gần nhất, thông tin chủ trọ và trạng thái kiểm duyệt (OPEN, PENDING, REJECTED, FULFILLED).
              </p>
            </div>

            <button
              onClick={handleExportHousing}
              disabled={isExporting}
              className="inline-flex items-center justify-center gap-2 px-4 py-2 bg-slate-900 text-white rounded-lg text-xs font-semibold hover:bg-slate-800 transition active:scale-[0.98] disabled:opacity-50"
            >
              <Download className="w-4 h-4 stroke-[1.5]" />
              <span>{isExporting ? "Đang xuất..." : "Tải CSV Tin Trọ"}</span>
            </button>
          </div>

          <div className="bg-white rounded-xl p-6 shadow-subtle flex flex-col justify-between">
            <div>
              <div className="flex items-center gap-2 mb-2">
                <BookOpen className="w-4 h-4 text-slate-700 stroke-[1.5]" />
                <h2 className="text-sm font-bold text-slate-900">
                  Báo Cáo Chợ Sách Giáo Trình
                </h2>
              </div>
              <p className="text-xs text-slate-500 mb-4 leading-relaxed">
                Xuất danh sách bài đăng sách: tên sách, môn học, giá bán/trao đổi, tình trạng sách và trạng thái giao dịch (AVAILABLE, SOLD).
              </p>
            </div>

            <button
              onClick={handleExportBooks}
              disabled={isExporting}
              className="inline-flex items-center justify-center gap-2 px-4 py-2 bg-slate-900 text-white rounded-lg text-xs font-semibold hover:bg-slate-800 transition active:scale-[0.98] disabled:opacity-50"
            >
              <Download className="w-4 h-4 stroke-[1.5]" />
              <span>{isExporting ? "Đang xuất..." : "Tải CSV Chợ Sách"}</span>
            </button>
          </div>
        </div>
      )}
    </AdminLayout>
  );
}
