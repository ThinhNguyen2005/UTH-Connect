"use client";

import React, { useEffect, useState, useCallback } from "react";
import Link from "next/link";
import Image from "next/image";
import { useGetIdentity } from "@refinedev/core";
import { PortalLayout } from "@/components/layout/PortalLayout";
import { supabaseBrowserClient } from "@/utils/supabase-client";
import {
  PackageSearch,
  Plus,
  MapPin,
  Calendar,
  Phone,
  Search,
  CheckCircle2,
  Tag,
  ShieldAlert,
  ArrowRight,
  Lock,
  LogIn,
  AlertCircle,
  RefreshCw,
  ShieldCheck,
} from "lucide-react";
import type { AppUserIdentity } from "@/providers/auth-provider";

export interface LostFoundItem {
  id: string;
  title: string;
  description: string;
  kind: "LOST" | "FOUND";
  category: "ALL" | "STUDENT_CARD" | "ELECTRONICS" | "WALLET_PAPERS" | "PERSONAL_ITEMS" | "OTHER";
  location: string;
  date: string;
  contact_name: string;
  contact_phone: string;
  image_url: string | null;
  is_resolved: boolean;
  user_id: string | null;
  created_at: string;
  updated_at: string;
}

const CATEGORIES = [
  { label: "Tất cả loại đồ", value: "ALL" },
  { label: "Thẻ sinh viên", value: "STUDENT_CARD" },
  { label: "Đồ điện tử (Laptop, ĐT...)", value: "ELECTRONICS" },
  { label: "Ví & Giấy tờ", value: "WALLET_PAPERS" },
  { label: "Đồ dùng cá nhân", value: "PERSONAL_ITEMS" },
  { label: "Khác", value: "OTHER" },
];

export default function LostFoundPage() {
  const { data: user, isLoading: authLoading } = useGetIdentity<AppUserIdentity>();

  const [items, setItems] = useState<LostFoundItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [fetchError, setFetchError] = useState<string | null>(null);
  const [selectedKind, setSelectedKind] = useState<"ALL" | "LOST" | "FOUND">("ALL");
  const [selectedCategory, setSelectedCategory] = useState<string>("ALL");
  const [onlyUnresolved, setOnlyUnresolved] = useState(true);
  const [searchTerm, setSearchTerm] = useState("");
  const [togglingId, setTogglingId] = useState<string | null>(null);

  const isStaffOrAdmin =
    user?.role === "admin" || user?.role === "organizer" || user?.role === "lecturer";

  const fetchItems = useCallback(async () => {
    if (!user) return;

    setLoading(true);
    setFetchError(null);

    try {
      let query = supabaseBrowserClient
        .from("lost_found_items")
        .select("*")
        .order("created_at", { ascending: false });

      if (selectedKind !== "ALL") {
        query = query.eq("kind", selectedKind);
      }
      if (selectedCategory !== "ALL") {
        query = query.eq("category", selectedCategory);
      }
      if (onlyUnresolved) {
        query = query.eq("is_resolved", false);
      }

      const { data, error } = await query;

      if (error) {
        throw new Error(error.message);
      }

      setItems((data as LostFoundItem[]) || []);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : "Không thể tải danh sách đồ thất lạc từ máy chủ.";
      setFetchError(message);
    } finally {
      setLoading(false);
    }
  }, [user, selectedKind, selectedCategory, onlyUnresolved]);

  useEffect(() => {
    if (user) {
      fetchItems();
    }
  }, [user, fetchItems]);

  const handleToggleResolved = async (e: React.MouseEvent, item: LostFoundItem) => {
    e.preventDefault();
    e.stopPropagation();

    if (!user) {
      alert("Vui lòng đăng nhập để thực hiện thao tác này.");
      return;
    }

    const canEdit =
      user.id === item.user_id || user.role === "admin" || user.role === "organizer";

    if (!canEdit) {
      alert("Chỉ người đăng tin hoặc Ban tổ chức / Quản trị viên mới có quyền cập nhật trạng thái.");
      return;
    }

    const nextState = !item.is_resolved;
    setTogglingId(item.id);

    const { data, error } = await supabaseBrowserClient
      .from("lost_found_items")
      .update({ is_resolved: nextState, updated_at: new Date().toISOString() })
      .eq("id", item.id)
      .select();

    setTogglingId(null);

    if (error) {
      alert(`Lỗi khi cập nhật: ${error.message}`);
    } else if (!data || data.length === 0) {
      alert("Không thể cập nhật: Lệnh UPDATE bị chặn bởi chính sách RLS của Database (0 dòng được sửa). Chỉ chủ sở hữu hoặc Admin được cấp quyền mới có thể cập nhật.");
    } else {
      setItems((prev) =>
        prev.map((i) => (i.id === item.id ? { ...i, is_resolved: nextState } : i))
      );
    }
  };

  const getCategoryLabel = (cat: string) => {
    const found = CATEGORIES.find((c) => c.value === cat);
    return found ? found.label : cat;
  };

  const filteredItems = items.filter((item) => {
    if (!searchTerm.trim()) return true;
    const term = searchTerm.toLowerCase();
    return (
      item.title.toLowerCase().includes(term) ||
      item.location.toLowerCase().includes(term) ||
      (item.description && item.description.toLowerCase().includes(term)) ||
      item.contact_name.toLowerCase().includes(term)
    );
  });

  return (
    <PortalLayout>
      {/* 1. Đang kiểm tra trạng thái đăng nhập (Skeleton) */}
      {authLoading && (
        <div className="py-20 text-center">
          <div className="w-8 h-8 border-2 border-slate-900 border-t-transparent rounded-full animate-spin mx-auto mb-3" />
          <div className="text-xs text-slate-500 font-medium">
            Đang xác thực tài khoản UTH...
          </div>
        </div>
      )}

      {/* 2. PHƯƠNG ÁN B: CHƯA ĐĂNG NHẬP -> HIỂN THỊ AUTH WALL */}
      {!authLoading && !user && (
        <div className="max-w-xl mx-auto my-12 text-center">
          <div className="bg-white rounded-2xl p-8 sm:p-10 shadow-subtle border border-slate-100">
            <div className="w-14 h-14 rounded-2xl bg-slate-900 text-white flex items-center justify-center mx-auto mb-5 shadow-sm">
              <Lock className="w-7 h-7 stroke-[1.75]" />
            </div>

            <h2 className="text-lg font-bold text-slate-900 tracking-tight">
              Yêu Cầu Đăng Nhập UTH Connect
            </h2>
            <p className="text-xs text-slate-500 mt-2 leading-relaxed max-w-md mx-auto">
              Để bảo vệ thông tin cá nhân (số điện thoại, họ tên) và tài sản của sinh viên,
              chức năng <span className="font-semibold text-slate-700">Đồ Thất Lạc & Nhặt Được Đồ</span> yêu cầu bạn đăng nhập bằng tài khoản UTH (<span className="text-slate-700 font-mono">@ut.edu.vn</span>).
            </p>

            <div className="my-6 p-4 bg-[#F8FAFC] rounded-xl text-left space-y-2.5">
              <div className="flex items-center gap-2.5 text-xs text-slate-700">
                <ShieldCheck className="w-4 h-4 text-emerald-600 shrink-0" />
                <span>Bảo mật danh tính & số điện thoại người mất/người nhặt</span>
              </div>
              <div className="flex items-center gap-2.5 text-xs text-slate-700">
                <PackageSearch className="w-4 h-4 text-blue-600 shrink-0" />
                <span>Tra cứu đầy đủ ảnh chụp, vị trí rơi và thời gian cụ thể</span>
              </div>
              <div className="flex items-center gap-2.5 text-xs text-slate-700">
                <CheckCircle2 className="w-4 h-4 text-purple-600 shrink-0" />
                <span>Hỗ trợ tiếp nhận bàn giao đồ tại Văn phòng Đoàn - Hội</span>
              </div>
            </div>

            <div className="flex items-center justify-center gap-3">
              <Link
                href="/login"
                className="inline-flex items-center gap-2 px-6 py-2.5 bg-slate-900 text-white rounded-xl text-xs font-semibold hover:bg-slate-800 transition active:scale-[0.98] shadow-sm"
              >
                <LogIn className="w-4 h-4" />
                <span>Đăng nhập ngay</span>
              </Link>
            </div>
          </div>
        </div>
      )}

      {/* 3. ĐÃ ĐĂNG NHẬP -> HIỂN THỊ TOÀN BỘ CHỨC NĂNG */}
      {!authLoading && user && (
        <>
          {/* Header bar */}
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
            <div>
              <h1 className="text-xl font-bold text-slate-900 tracking-tight flex items-center gap-2">
                <PackageSearch className="w-6 h-6 text-slate-800 stroke-[1.8]" />
                <span>Đồ Thất Lạc & Nhặt Được Đồ UTH</span>
              </h1>
              <p className="text-xs text-slate-500 mt-0.5">
                Kênh trao trả đồ thất lạc chính thức trong khuôn viên Đại học Giao thông vận tải TP.HCM
              </p>
            </div>

            <div className="flex items-center gap-2">
              {isStaffOrAdmin && (
                <Link
                  href="/admin/lost-found"
                  className="inline-flex items-center gap-1.5 px-3 py-2 bg-white text-slate-700 hover:bg-slate-50 rounded-lg text-xs font-semibold shadow-subtle transition active:scale-[0.98]"
                >
                  <ShieldAlert className="w-3.5 h-3.5 text-amber-600 stroke-[1.8]" />
                  <span>Quản lý Bàn giao</span>
                </Link>
              )}

              <Link
                href="/lost-found/create"
                className="inline-flex items-center gap-1.5 px-3.5 py-2 bg-slate-900 text-white rounded-lg text-xs font-semibold hover:bg-slate-800 transition active:scale-[0.98] shadow-sm"
              >
                <Plus className="w-3.5 h-3.5 stroke-[2]" />
                <span>Báo mất / Nhặt được đồ</span>
              </Link>
            </div>
          </div>

          {/* Control Bar: Search & Kind Tabs */}
          <div className="bg-white rounded-xl p-4 shadow-subtle mb-5 space-y-3.5">
            <div className="flex flex-col md:flex-row items-stretch md:items-center justify-between gap-3">
              {/* Search bar */}
              <div className="relative flex-1">
                <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
                <input
                  type="text"
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  placeholder="Tìm theo tên đồ vật, phòng học, vị trí rơi, người liên hệ..."
                  className="w-full pl-9 pr-3 py-2 bg-[#F8FAFC] rounded-lg text-xs focus:outline-none focus:ring-1 focus:ring-slate-900 focus:bg-white transition"
                />
              </div>

              {/* Kind Tabs: All vs Lost vs Found */}
              <div className="flex items-center bg-[#F8FAFC] p-1 rounded-lg self-start md:self-auto">
                <button
                  onClick={() => setSelectedKind("ALL")}
                  className={`px-3 py-1.5 rounded-md text-xs font-medium transition ${
                    selectedKind === "ALL"
                      ? "bg-white text-slate-900 shadow-sm"
                      : "text-slate-500 hover:text-slate-800"
                  }`}
                >
                  Tất cả
                </button>
                <button
                  onClick={() => setSelectedKind("LOST")}
                  className={`px-3 py-1.5 rounded-md text-xs font-medium transition ${
                    selectedKind === "LOST"
                      ? "bg-rose-500 text-white shadow-sm"
                      : "text-slate-500 hover:text-slate-800"
                  }`}
                >
                  Cần tìm đồ rơi (Lost)
                </button>
                <button
                  onClick={() => setSelectedKind("FOUND")}
                  className={`px-3 py-1.5 rounded-md text-xs font-medium transition ${
                    selectedKind === "FOUND"
                      ? "bg-emerald-600 text-white shadow-sm"
                      : "text-slate-500 hover:text-slate-800"
                  }`}
                >
                  Nhặt được đồ (Found)
                </button>
              </div>
            </div>

            {/* Category Pills & Resolved Toggle */}
            <div className="flex flex-wrap items-center justify-between gap-2 pt-2 border-t border-slate-100">
              <div className="flex flex-wrap items-center gap-1.5">
                {CATEGORIES.map((c) => (
                  <button
                    key={c.value}
                    onClick={() => setSelectedCategory(c.value)}
                    className={`px-2.5 py-1 rounded-lg text-[11px] font-medium transition active:scale-[0.98] ${
                      selectedCategory === c.value
                        ? "bg-slate-900 text-white"
                        : "bg-[#F8FAFC] text-slate-600 hover:bg-slate-200/70"
                    }`}
                  >
                    {c.label}
                  </button>
                ))}
              </div>

              <label className="flex items-center gap-2 cursor-pointer select-none text-xs text-slate-600 ml-auto">
                <input
                  type="checkbox"
                  checked={onlyUnresolved}
                  onChange={(e) => setOnlyUnresolved(e.target.checked)}
                  className="rounded border-slate-300 text-slate-900 focus:ring-slate-900 w-3.5 h-3.5"
                />
                <span>Chỉ hiện tin chưa nhận lại</span>
              </label>
            </div>
          </div>

          {/* Lỗi tải dữ liệu nếu có */}
          {fetchError && (
            <div className="p-4 mb-5 rounded-xl bg-red-50 border border-red-200/70 text-red-700 text-xs flex items-center justify-between gap-3">
              <div className="flex items-center gap-2">
                <AlertCircle className="w-4 h-4 shrink-0 text-red-600" />
                <span>{fetchError}</span>
              </div>
              <button
                onClick={fetchItems}
                className="inline-flex items-center gap-1 px-3 py-1 bg-white text-red-700 rounded-lg font-semibold shadow-sm hover:bg-red-50 transition"
              >
                <RefreshCw className="w-3 h-3" />
                <span>Thử lại</span>
              </button>
            </div>
          )}

          {/* Item Grid */}
          {loading ? (
            <div className="py-16 text-center text-xs text-slate-400">
              <RefreshCw className="w-5 h-5 animate-spin mx-auto mb-2 text-slate-400" />
              <span>Đang tải danh sách đồ thất lạc...</span>
            </div>
          ) : filteredItems.length === 0 ? (
            <div className="py-16 bg-white rounded-xl shadow-subtle text-center">
              <PackageSearch className="w-10 h-10 text-slate-300 stroke-[1.5] mx-auto mb-2.5" />
              <div className="text-sm font-semibold text-slate-800">
                Không tìm thấy món đồ nào phù hợp
              </div>
              <p className="text-xs text-slate-400 mt-1 max-w-sm mx-auto">
                Hãy thử tìm bằng từ khóa khác hoặc đăng tin mới nếu bạn vừa đánh rơi hay nhặt được đồ!
              </p>
              <div className="mt-4">
                <Link
                  href="/lost-found/create"
                  className="inline-flex items-center gap-1.5 px-3.5 py-2 bg-slate-900 text-white rounded-lg text-xs font-semibold hover:bg-slate-800 transition"
                >
                  <Plus className="w-3.5 h-3.5" />
                  <span>Đăng tin ngay</span>
                </Link>
              </div>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
              {filteredItems.map((item) => {
                const isOwner = user?.id === item.user_id;
                const canManage = isOwner || user?.role === "admin" || user?.role === "organizer";

                return (
                  <div
                    key={item.id}
                    className={`bg-white rounded-xl shadow-subtle flex flex-col justify-between overflow-hidden hover:shadow-md transition duration-200 border ${
                      item.is_resolved
                        ? "border-slate-200/60 opacity-80"
                        : item.kind === "LOST"
                        ? "border-rose-100/70"
                        : "border-emerald-100/70"
                    }`}
                  >
                    {/* Photo Preview if available */}
                    {item.image_url ? (
                      <div className="relative w-full h-44 bg-slate-100 overflow-hidden group">
                        <Image
                          src={item.image_url}
                          alt={item.title}
                          fill
                          sizes="(max-width: 768px) 100vw, 350px"
                          className="object-cover group-hover:scale-105 transition duration-300"
                          unoptimized
                        />
                        <div className="absolute top-2.5 left-2.5 flex items-center gap-1.5 z-10">
                          <span
                            className={`text-[10px] font-bold px-2.5 py-1 rounded-full uppercase tracking-wider shadow-sm ${
                              item.kind === "LOST"
                                ? "bg-rose-500 text-white"
                                : "bg-emerald-600 text-white"
                            }`}
                          >
                            {item.kind === "LOST" ? "Cần tìm đồ" : "Nhặt được đồ"}
                          </span>
                          {item.is_resolved && (
                            <span className="text-[10px] font-semibold px-2 py-0.5 rounded-full bg-slate-900/80 text-white backdrop-blur">
                              Đã trao trả
                            </span>
                          )}
                        </div>
                      </div>
                    ) : (
                      <div className="p-4 pb-0 flex items-center justify-between">
                        <span
                          className={`text-[10px] font-bold px-2.5 py-1 rounded-full uppercase tracking-wider ${
                            item.kind === "LOST"
                              ? "bg-rose-50 text-rose-700 font-semibold"
                              : "bg-emerald-50 text-emerald-700 font-semibold"
                          }`}
                        >
                          {item.kind === "LOST" ? "Cần tìm đồ (Lost)" : "Nhặt được đồ (Found)"}
                        </span>
                        {item.is_resolved && (
                          <span className="text-[10px] font-semibold px-2 py-0.5 rounded bg-slate-100 text-slate-600">
                            Đã trao trả
                          </span>
                        )}
                      </div>
                    )}

                    {/* Content body */}
                    <div className="p-5 flex-1 flex flex-col justify-between">
                      <div>
                        <div className="flex items-center gap-1.5 mb-2 text-[11px] text-slate-400">
                          <Tag className="w-3 h-3 text-slate-400" />
                          <span>{getCategoryLabel(item.category)}</span>
                        </div>

                        <Link
                          href={`/lost-found/${item.id}`}
                          className="block group mb-2"
                        >
                          <h3 className="text-sm font-bold text-slate-900 group-hover:text-blue-600 transition line-clamp-2">
                            {item.title}
                          </h3>
                        </Link>

                        <p className="text-xs text-slate-500 line-clamp-2 mb-3 leading-relaxed">
                          {item.description || "Chưa có mô tả chi tiết."}
                        </p>

                        <div className="space-y-1.5 text-[11px] text-slate-500 mb-4 bg-[#F8FAFC] p-2.5 rounded-lg">
                          <div className="flex items-center gap-1.5">
                            <MapPin className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                            <span className="truncate font-medium text-slate-700">
                              {item.location}
                            </span>
                          </div>
                          <div className="flex items-center gap-1.5 text-slate-400">
                            <Calendar className="w-3.5 h-3.5 shrink-0" />
                            <span>Thời gian: {item.date}</span>
                          </div>
                        </div>
                      </div>

                      {/* Contact & Actions footer */}
                      <div className="pt-3 border-t border-slate-100 flex items-center justify-between gap-2">
                        <div className="min-w-0">
                          <div className="text-[10px] uppercase font-semibold text-slate-400">
                            Liên hệ
                          </div>
                          <div className="text-xs font-semibold text-slate-800 truncate">
                            {item.contact_name || "Thành viên UTH"}
                          </div>
                        </div>

                        <div className="flex items-center gap-1.5 shrink-0">
                          {item.contact_phone && (
                            <a
                              href={`tel:${item.contact_phone}`}
                              className="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-lg bg-slate-100 hover:bg-slate-200 text-slate-700 text-[11px] font-semibold transition"
                              title="Gọi điện liên hệ"
                            >
                              <Phone className="w-3 h-3 text-slate-500" />
                              <span>{item.contact_phone}</span>
                            </a>
                          )}

                          {canManage && (
                            <button
                              onClick={(e) => handleToggleResolved(e, item)}
                              disabled={togglingId === item.id}
                              className={`p-1.5 rounded-lg text-xs font-semibold transition ${
                                item.is_resolved
                                  ? "bg-slate-100 text-slate-600 hover:bg-slate-200"
                                  : "bg-emerald-50 text-emerald-700 hover:bg-emerald-100"
                              }`}
                              title={
                                item.is_resolved
                                  ? "Mở lại bài đăng (chưa tìm thấy)"
                                  : "Đánh dấu đã trao trả / nhận lại thành công"
                              }
                            >
                              <CheckCircle2 className="w-4 h-4 stroke-[2]" />
                            </button>
                          )}

                          <Link
                            href={`/lost-found/${item.id}`}
                            className="p-1.5 text-slate-400 hover:text-slate-800 rounded-lg hover:bg-slate-100 transition"
                            title="Xem chi tiết"
                          >
                            <ArrowRight className="w-4 h-4" />
                          </Link>
                        </div>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </>
      )}
    </PortalLayout>
  );
}
