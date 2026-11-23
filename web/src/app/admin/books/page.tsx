"use client";

import React, { useEffect, useState, useMemo } from "react";
import Link from "next/link";
import { AdminLayout } from "@/components/layout/AdminLayout";
import { supabaseBrowserClient } from "@/utils/supabase-client";
import { getBookImageUrl, deleteBookImage } from "@/utils/bookImages";
import type { BookListingItem } from "@/app/books/page";
import {
  BookOpen,
  Search,
  Filter,
  Trash2,
  ExternalLink,
  CheckCircle2,
  XCircle,
  AlertCircle,
  Loader2,
  RefreshCw,
  User,
  Phone,
  Tag,
  ArrowUpDown,
} from "lucide-react";

const CONDITION_LABEL: Record<string, string> = {
  NEW: "Mới 100%",
  GOOD: "Còn tốt",
  USED: "Đã qua sử dụng",
};

export default function AdminBookManagementPage() {
  const [listings, setListings] = useState<BookListingItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [searchQuery, setSearchQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState<"ALL" | "AVAILABLE" | "SOLD">("ALL");
  const [dealFilter, setDealFilter] = useState<"ALL" | "SALE" | "EXCHANGE">("ALL");
  const [busyId, setBusyId] = useState<string | null>(null);

  async function loadData() {
    setLoading(true);
    setError(null);
    try {
      const { data, error: err } = await supabaseBrowserClient
        .from("book_listings")
        .select("*, profiles:owner_user_id(display_name, email, student_id)")
        .order("created_at", { ascending: false });

      if (err) throw err;
      setListings((data as BookListingItem[]) || []);
    } catch (err: any) {
      setError(err.message || "Không thể tải danh sách sách giáo trình.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadData();
  }, []);

  // Stats calculation
  const stats = useMemo(() => {
    const total = listings.length;
    const available = listings.filter((l) => l.status === "AVAILABLE").length;
    const sold = listings.filter((l) => l.status === "SOLD").length;
    const exchange = listings.filter((l) => l.deal_type === "EXCHANGE").length;
    return { total, available, sold, exchange };
  }, [listings]);

  // Filtered listings
  const filteredListings = useMemo(() => {
    return listings.filter((item) => {
      if (statusFilter !== "ALL" && item.status !== statusFilter) return false;
      if (dealFilter !== "ALL" && item.deal_type !== dealFilter) return false;

      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase().trim();
        const matchesTitle = item.title.toLowerCase().includes(q);
        const matchesSubject = item.subject.toLowerCase().includes(q);
        const matchesSeller = item.seller_name.toLowerCase().includes(q);
        const matchesContact = item.contact.toLowerCase().includes(q);
        const matchesStudentId = item.profiles?.student_id?.toLowerCase().includes(q);
        return matchesTitle || matchesSubject || matchesSeller || matchesContact || matchesStudentId;
      }

      return true;
    });
  }, [listings, statusFilter, dealFilter, searchQuery]);

  // Admin action: Toggle status
  const handleToggleStatus = async (item: BookListingItem) => {
    if (busyId) return;
    const nextStatus = item.status === "AVAILABLE" ? "SOLD" : "AVAILABLE";
    setBusyId(item.id);

    try {
      const { error: updErr } = await supabaseBrowserClient
        .from("book_listings")
        .update({
          status: nextStatus,
          updated_at: new Date().toISOString(),
        })
        .eq("id", item.id);

      if (updErr) throw updErr;

      setListings((prev) =>
        prev.map((l) => (l.id === item.id ? { ...l, status: nextStatus } : l))
      );
    } catch (err: any) {
      alert("Lỗi khi cập nhật trạng thái: " + (err.message || "Thao tác thất bại"));
    } finally {
      setBusyId(null);
    }
  };

  // Admin action: Delete listing
  const handleDeleteListing = async (item: BookListingItem) => {
    if (busyId) return;
    const confirmDel = window.confirm(
      `[ADMIN ACTION] Bạn có chắc muốn xóa tin sách "${item.title}" của người bán "${item.seller_name}" không? Thao tác này sẽ xóa vĩnh viễn bài đăng.`
    );
    if (!confirmDel) return;

    setBusyId(item.id);
    try {
      if (item.image_path) {
        await deleteBookImage(item.image_path);
      }

      const { error: delErr } = await supabaseBrowserClient
        .from("book_listings")
        .delete()
        .eq("id", item.id);

      if (delErr) throw delErr;

      setListings((prev) => prev.filter((l) => l.id !== item.id));
    } catch (err: any) {
      alert("Không thể xóa bài đăng: " + (err.message || "Đã xảy ra lỗi"));
    } finally {
      setBusyId(null);
    }
  };

  return (
    <AdminLayout>
      <div className="p-6 max-w-7xl mx-auto space-y-6">
        {/* Top Header */}
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
          <div>
            <div className="flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-blue-600" />
              <h1 className="text-xl font-bold tracking-tight text-slate-900">
                Quản Trị Chợ Sách Giáo Trình & Tài Liệu
              </h1>
            </div>
            <p className="text-xs text-slate-500 mt-1">
              Giám sát các tin mua bán, trao đổi giáo trình; xử lý bài đăng vi phạm hoặc không hợp lệ.
            </p>
          </div>

          <div className="flex items-center gap-2">
            <Link
              href="/books"
              target="_blank"
              className="inline-flex items-center gap-1.5 px-3 py-2 bg-white text-slate-700 hover:bg-slate-100 rounded-lg text-xs font-semibold shadow-subtle transition"
            >
              <span>Xem trang Sinh viên</span>
              <ExternalLink className="w-3.5 h-3.5" />
            </Link>

            <button
              onClick={loadData}
              disabled={loading}
              className="p-2 bg-white text-slate-600 hover:text-slate-900 rounded-lg shadow-subtle transition"
              title="Làm mới dữ liệu"
            >
              <RefreshCw className={`w-4 h-4 ${loading ? "animate-spin" : ""}`} />
            </button>
          </div>
        </div>

        {/* KPI Stats Cards: Tonal Layering, Tabular Numbers */}
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          <div className="bg-white rounded-xl p-4 shadow-subtle">
            <div className="text-[11px] font-semibold uppercase tracking-wider text-slate-400">
              Tổng số sách đăng
            </div>
            <div className="text-2xl font-bold text-slate-900 mt-1 tabular-nums">
              {stats.total}
            </div>
            <div className="text-[11px] text-slate-500 mt-0.5">Toàn bộ kho tài liệu</div>
          </div>

          <div className="bg-white rounded-xl p-4 shadow-subtle">
            <div className="text-[11px] font-semibold uppercase tracking-wider text-slate-400">
              Đang rao trên chợ
            </div>
            <div className="text-2xl font-bold text-emerald-700 mt-1 tabular-nums">
              {stats.available}
            </div>
            <div className="text-[11px] text-slate-500 mt-0.5">Sinh viên có thể liên hệ</div>
          </div>

          <div className="bg-white rounded-xl p-4 shadow-subtle">
            <div className="text-[11px] font-semibold uppercase tracking-wider text-slate-400">
              Đã giao dịch xong
            </div>
            <div className="text-2xl font-bold text-slate-600 mt-1 tabular-nums">
              {stats.sold}
            </div>
            <div className="text-[11px] text-slate-500 mt-0.5">Đã sang tay sinh viên</div>
          </div>

          <div className="bg-white rounded-xl p-4 shadow-subtle">
            <div className="text-[11px] font-semibold uppercase tracking-wider text-slate-400">
              Sách Trao đổi / Tặng
            </div>
            <div className="text-2xl font-bold text-purple-700 mt-1 tabular-nums">
              {stats.exchange}
            </div>
            <div className="text-[11px] text-slate-500 mt-0.5">Không thu phí bằng tiền</div>
          </div>
        </div>

        {/* Filter Bar */}
        <div className="bg-white rounded-xl p-4 shadow-subtle flex flex-col md:flex-row gap-3 items-center justify-between">
          <div className="w-full md:w-80 relative">
            <Search className="w-4 h-4 stroke-[1.5] text-slate-400 absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Tìm theo tên sách, môn, người bán, SĐT..."
              className="w-full pl-9 pr-3 py-2 bg-[#F1F5F9] rounded-lg text-xs text-slate-900 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-slate-900"
            />
          </div>

          <div className="flex items-center gap-2 w-full md:w-auto">
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value as any)}
              className="px-3 py-2 bg-[#F1F5F9] rounded-lg text-xs text-slate-700 font-medium focus:outline-none"
            >
              <option value="ALL">Mọi trạng thái</option>
              <option value="AVAILABLE">Còn sách (AVAILABLE)</option>
              <option value="SOLD">Đã bán (SOLD)</option>
            </select>

            <select
              value={dealFilter}
              onChange={(e) => setDealFilter(e.target.value as any)}
              className="px-3 py-2 bg-[#F1F5F9] rounded-lg text-xs text-slate-700 font-medium focus:outline-none"
            >
              <option value="ALL">Mọi hình thức</option>
              <option value="SALE">Bán lại (SALE)</option>
              <option value="EXCHANGE">Trao đổi (EXCHANGE)</option>
            </select>
          </div>
        </div>

        {/* Error message */}
        {error && (
          <div className="bg-red-50 text-red-800 rounded-xl p-4 text-xs font-medium">
            {error}
          </div>
        )}

        {/* Data-Dense Table (Pure White Card, No heavy borders, Subtle Rows) */}
        <div className="bg-white rounded-xl shadow-subtle overflow-hidden">
          {loading ? (
            <div className="p-12 text-center text-slate-400 space-y-2">
              <Loader2 className="w-6 h-6 animate-spin mx-auto text-slate-400" />
              <p className="text-xs">Đang tải dữ liệu chợ sách...</p>
            </div>
          ) : filteredListings.length === 0 ? (
            <div className="p-12 text-center text-slate-400 space-y-1">
              <BookOpen className="w-8 h-8 stroke-[1.2] mx-auto text-slate-300" />
              <p className="text-xs font-semibold text-slate-700">Không có bài đăng nào</p>
              <p className="text-[11px]">Thử điều chỉnh bộ lọc hoặc từ khóa tìm kiếm.</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs border-collapse">
                <thead>
                  <tr className="bg-[#F8FAFC] text-slate-400 font-semibold uppercase tracking-wider text-[10px]">
                    <th className="py-3 px-4">Ảnh</th>
                    <th className="py-3 px-4">Tên sách & Môn học</th>
                    <th className="py-3 px-4">Người đăng</th>
                    <th className="py-3 px-4">Hình thức / Tình trạng</th>
                    <th className="py-3 px-4">Giá bán</th>
                    <th className="py-3 px-4">Trạng thái</th>
                    <th className="py-3 px-4">Ngày đăng</th>
                    <th className="py-3 px-4 text-right">Thao tác</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {filteredListings.map((item) => {
                    const imageUrl = getBookImageUrl(item.image_path);
                    const isSold = item.status === "SOLD";

                    return (
                      <tr
                        key={item.id}
                        className={`hover:bg-[#F8FAFC] transition-colors ${
                          isSold ? "bg-slate-50/50" : ""
                        }`}
                      >
                        {/* Ảnh thu nhỏ */}
                        <td className="py-3 px-4">
                          <div className="w-11 h-11 rounded-lg bg-slate-100 overflow-hidden shrink-0 flex items-center justify-center">
                            {imageUrl ? (
                              // eslint-disable-next-line @next/next/no-img-element
                              <img
                                src={imageUrl}
                                alt=""
                                className="w-full h-full object-cover"
                              />
                            ) : (
                              <BookOpen className="w-4 h-4 text-slate-400" />
                            )}
                          </div>
                        </td>

                        {/* Tên sách & Môn học */}
                        <td className="py-3 px-4 max-w-xs">
                          <Link
                            href={`/books/${item.id}`}
                            className="font-bold text-slate-900 hover:text-blue-600 line-clamp-1 block"
                            title={item.title}
                          >
                            {item.title}
                          </Link>
                          <div className="text-[11px] text-slate-500 truncate mt-0.5">
                            {item.subject}
                          </div>
                        </td>

                        {/* Người đăng */}
                        <td className="py-3 px-4">
                          <div className="font-semibold text-slate-900">
                            {item.seller_name}
                          </div>
                          <div className="text-[11px] text-slate-500 font-mono">
                            {item.contact}
                          </div>
                          {item.profiles?.student_id && (
                            <div className="text-[10px] text-blue-700 font-medium">
                              MSSV: {item.profiles.student_id}
                            </div>
                          )}
                        </td>

                        {/* Hình thức & Tình trạng */}
                        <td className="py-3 px-4">
                          <div className="flex flex-col gap-1 items-start">
                            <span
                              className={`px-1.5 py-0.5 rounded text-[10px] font-semibold ${
                                item.deal_type === "SALE"
                                  ? "bg-amber-50 text-amber-800"
                                  : "bg-purple-50 text-purple-700"
                              }`}
                            >
                              {item.deal_type === "SALE" ? "Bán lại" : "Trao đổi"}
                            </span>
                            <span className="text-[11px] text-slate-600">
                              {CONDITION_LABEL[item.condition] || item.condition}
                            </span>
                          </div>
                        </td>

                        {/* Giá bán */}
                        <td className="py-3 px-4">
                          {item.deal_type === "SALE" && item.price_vnd ? (
                            <span className="font-bold text-slate-900 tabular-nums">
                              {new Intl.NumberFormat("vi-VN").format(item.price_vnd)} đ
                            </span>
                          ) : (
                            <span className="text-purple-700 font-medium text-[11px]">
                              Trao đổi
                            </span>
                          )}
                        </td>

                        {/* Trạng thái */}
                        <td className="py-3 px-4">
                          {isSold ? (
                            <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[10px] font-bold bg-slate-100 text-slate-600 uppercase">
                              Đã bán
                            </span>
                          ) : (
                            <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-50 text-emerald-800 uppercase">
                              Đang rao
                            </span>
                          )}
                        </td>

                        {/* Ngày đăng */}
                        <td className="py-3 px-4 text-slate-400 tabular-nums text-[11px]">
                          {new Date(item.created_at).toLocaleDateString("vi-VN")}
                        </td>

                        {/* Thao tác Admin */}
                        <td className="py-3 px-4 text-right">
                          <div className="flex items-center justify-end gap-1.5">
                            <Link
                              href={`/books/${item.id}`}
                              target="_blank"
                              title="Xem chi tiết trên Web"
                              className="p-1.5 text-slate-500 hover:text-slate-900 hover:bg-slate-100 rounded-lg transition"
                            >
                              <ExternalLink className="w-3.5 h-3.5" />
                            </Link>

                            <button
                              onClick={() => handleToggleStatus(item)}
                              disabled={busyId === item.id}
                              title={isSold ? "Đổi sang Còn sách" : "Đổi sang Đã bán"}
                              className={`p-1.5 rounded-lg transition ${
                                isSold
                                  ? "text-emerald-700 hover:bg-emerald-50"
                                  : "text-slate-500 hover:bg-slate-100"
                              }`}
                            >
                              {isSold ? (
                                <CheckCircle2 className="w-3.5 h-3.5" />
                              ) : (
                                <XCircle className="w-3.5 h-3.5" />
                              )}
                            </button>

                            <button
                              onClick={() => handleDeleteListing(item)}
                              disabled={busyId === item.id}
                              title="Xóa bài đăng vi phạm"
                              className="p-1.5 text-slate-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition"
                            >
                              <Trash2 className="w-3.5 h-3.5 stroke-[1.5]" />
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </AdminLayout>
  );
}
