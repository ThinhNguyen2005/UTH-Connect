"use client";

import React, { useEffect, useState, useCallback, useMemo } from "react";
import Link from "next/link";
import Image from "next/image";
import { AdminLayout } from "@/components/layout/AdminLayout";
import { supabaseBrowserClient } from "@/utils/supabase-client";
import {
  PackageSearch,
  RefreshCw,
  CheckCircle2,
  Trash2,
  ExternalLink,
  Search,
  X,
  Tag,
  MapPin,
  Image as ImageIcon,
  RotateCcw,
  CheckSquare,
  Square,
  MinusSquare,
  User,
} from "lucide-react";
import type { LostFoundItem } from "@/app/lost-found/page";

const CATEGORIES = [
  { id: "ALL", label: "Tất cả danh mục" },
  { id: "STUDENT_CARD", label: "Thẻ sinh viên / Thẻ xe" },
  { id: "ELECTRONICS", label: "Đồ điện tử (Laptop, ĐT..)" },
  { id: "WALLET_PAPERS", label: "Ví & Giấy tờ" },
  { id: "PERSONAL_ITEMS", label: "Đồ dùng cá nhân" },
  { id: "OTHERS", label: "Khác" },
] as const;

export default function AdminLostFoundPage() {
  const [items, setItems] = useState<LostFoundItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [toast, setToast] = useState<string | null>(null);

  // Bộ lọc theo các mục riêng biệt
  const [searchKeyword, setSearchKeyword] = useState("");
  const [searchLocation, setSearchLocation] = useState("");
  const [searchContact, setSearchContact] = useState("");
  const [selectedKind, setSelectedKind] = useState<"ALL" | "LOST" | "FOUND">("ALL");
  const [selectedCategory, setSelectedCategory] = useState<string>("ALL");
  const [selectedStatus, setSelectedStatus] = useState<"ALL" | "UNRESOLVED" | "RESOLVED">("ALL");
  const [onlyHasImage, setOnlyHasImage] = useState(false);

  // Thao tác đơn lẻ & hàng loạt
  const [updatingId, setUpdatingId] = useState<string | null>(null);
  const [selectedIds, setSelectedIds] = useState<string[]>([]);
  const [bulkActionLoading, setBulkActionLoading] = useState(false);

  // Lấy dữ liệu từ Supabase
  const fetchItems = useCallback(async () => {
    setLoading(true);
    const { data, error } = await supabaseBrowserClient
      .from("lost_found_items")
      .select("*")
      .order("created_at", { ascending: false });

    if (!error && data) {
      setItems(data as LostFoundItem[]);
    }
    setLoading(false);
  }, []);

  useEffect(() => {
    fetchItems();
  }, [fetchItems]);

  // Bộ lọc dữ liệu phía client với từng trường tìm kiếm riêng rẽ
  const filteredItems = useMemo(() => {
    return items.filter((item) => {
      // 1. Lọc theo Phân loại (LOST / FOUND)
      if (selectedKind !== "ALL" && item.kind !== selectedKind) return false;

      // 2. Lọc theo Danh mục
      if (selectedCategory !== "ALL" && item.category !== selectedCategory) return false;

      // 3. Lọc theo Trạng thái trao trả
      if (selectedStatus === "UNRESOLVED" && item.is_resolved) return false;
      if (selectedStatus === "RESOLVED" && !item.is_resolved) return false;

      // 4. Lọc chỉ tin có ảnh
      if (onlyHasImage && !item.image_url) return false;

      // 5. Tìm theo Từ khóa (Tên đồ vật / Mô tả)
      if (searchKeyword.trim()) {
        const kw = searchKeyword.toLowerCase().trim();
        const matchTitle = item.title?.toLowerCase().includes(kw);
        const matchDesc = item.description?.toLowerCase().includes(kw);
        if (!matchTitle && !matchDesc) return false;
      }

      // 6. Tìm theo Địa điểm / Cơ sở
      if (searchLocation.trim()) {
        const loc = searchLocation.toLowerCase().trim();
        if (!item.location?.toLowerCase().includes(loc)) return false;
      }

      // 7. Tìm theo Người liên hệ / SĐT
      if (searchContact.trim()) {
        const cont = searchContact.toLowerCase().trim();
        const matchName = item.contact_name?.toLowerCase().includes(cont);
        const matchPhone = item.contact_phone?.toLowerCase().includes(cont);
        if (!matchName && !matchPhone) return false;
      }

      return true;
    });
  }, [
    items,
    selectedKind,
    selectedCategory,
    selectedStatus,
    onlyHasImage,
    searchKeyword,
    searchLocation,
    searchContact,
  ]);

  // Thống kê số lượng theo Tag
  const stats = useMemo(() => {
    return {
      total: items.length,
      lost: items.filter((i) => i.kind === "LOST").length,
      found: items.filter((i) => i.kind === "FOUND").length,
      resolved: items.filter((i) => i.is_resolved).length,
      unresolved: items.filter((i) => !i.is_resolved).length,
      withImage: items.filter((i) => Boolean(i.image_url)).length,
      byCat: CATEGORIES.reduce((acc, cat) => {
        if (cat.id === "ALL") {
          acc[cat.id] = items.length;
        } else {
          acc[cat.id] = items.filter((i) => i.category === cat.id).length;
        }
        return acc;
      }, {} as Record<string, number>),
    };
  }, [items]);

  // Kiểm tra có đang áp dụng bộ lọc nào không
  const hasActiveFilters =
    searchKeyword.trim() !== "" ||
    searchLocation.trim() !== "" ||
    searchContact.trim() !== "" ||
    selectedKind !== "ALL" ||
    selectedCategory !== "ALL" ||
    selectedStatus !== "ALL" ||
    onlyHasImage;

  // Đặt lại toàn bộ bộ lọc về mặc định
  const handleResetFilters = () => {
    setSearchKeyword("");
    setSearchLocation("");
    setSearchContact("");
    setSelectedKind("ALL");
    setSelectedCategory("ALL");
    setSelectedStatus("ALL");
    setOnlyHasImage(false);
  };

  // Quản lý Checkbox
  const allFilteredSelected =
    filteredItems.length > 0 &&
    filteredItems.every((item) => selectedIds.includes(item.id));
  const someFilteredSelected =
    filteredItems.some((item) => selectedIds.includes(item.id)) && !allFilteredSelected;

  const handleToggleSelectAll = () => {
    if (allFilteredSelected) {
      // Bỏ chọn tất cả các item đang hiển thị
      const currentFilteredIds = new Set(filteredItems.map((i) => i.id));
      setSelectedIds((prev) => prev.filter((id) => !currentFilteredIds.has(id)));
    } else {
      // Chọn tất cả các item đang hiển thị
      const newIds = new Set([...selectedIds, ...filteredItems.map((i) => i.id)]);
      setSelectedIds(Array.from(newIds));
    }
  };

  const handleToggleSelectItem = (id: string) => {
    setSelectedIds((prev) =>
      prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]
    );
  };

  // 1. Cập nhật đơn lẻ trạng thái qua Supabase Browser Client (RLS cho phép Admin/Organizer)
  const handleToggleResolved = async (itemId: string, currentState: boolean) => {
    setUpdatingId(itemId);
    setToast(null);

    const nextState = !currentState;
    const { data, error } = await supabaseBrowserClient
      .from("lost_found_items")
      .update({ is_resolved: nextState, updated_at: new Date().toISOString() })
      .eq("id", itemId)
      .select();

    setUpdatingId(null);

    if (error) {
      setToast(`Lỗi: ${error.message}`);
      return;
    }

    setToast(
      nextState
        ? "Đã đánh dấu bàn giao / trao trả đồ thành công!"
        : "Đã chuyển lại trạng thái đang tìm kiếm."
    );
    setItems((prev) =>
      prev.map((i) => (i.id === itemId ? { ...i, is_resolved: nextState } : i))
    );
  };

  // 2. Xóa đơn lẻ qua Supabase Browser Client (RLS cho phép Admin/Organizer)
  const handleDeleteItem = async (itemId: string, itemTitle: string) => {
    const confirmed = window.confirm(`Bạn có chắc muốn xóa tin "${itemTitle}" này không?`);
    if (!confirmed) return;

    setUpdatingId(itemId);
    setToast(null);

    const { error } = await supabaseBrowserClient
      .from("lost_found_items")
      .delete()
      .eq("id", itemId);

    setUpdatingId(null);

    if (error) {
      setToast(`Lỗi xóa tin: ${error.message}`);
      return;
    }

    setToast(`Đã xóa thành công tin "${itemTitle}".`);
    setItems((prev) => prev.filter((i) => i.id !== itemId));
    setSelectedIds((prev) => prev.filter((id) => id !== itemId));
  };

  // 3. Thao tác hàng loạt: Cập nhật trạng thái cho nhiều tin (RLS cho phép Admin/Organizer)
  const handleBulkUpdateStatus = async (isResolved: boolean) => {
    if (selectedIds.length === 0) return;
    setBulkActionLoading(true);
    setToast(null);

    const { data, error } = await supabaseBrowserClient
      .from("lost_found_items")
      .update({ is_resolved: isResolved, updated_at: new Date().toISOString() })
      .in("id", selectedIds)
      .select();

    setBulkActionLoading(false);

    if (error) {
      setToast(`Lỗi cập nhật hàng loạt: ${error.message}`);
      return;
    }

    const affected = data?.length || selectedIds.length;
    setToast(
      isResolved
        ? `Đã đánh dấu bàn giao thành công cho ${affected} tin!`
        : `Đã chuyển lại trạng thái đang tìm kiếm cho ${affected} tin!`
    );

    const updatedSet = new Set(selectedIds);
    setItems((prev) =>
      prev.map((i) => (updatedSet.has(i.id) ? { ...i, is_resolved: isResolved } : i))
    );
    setSelectedIds([]);
  };

  // 4. Thao tác hàng loạt: Xóa nhiều tin vi phạm (RLS cho phép Admin/Organizer)
  const handleBulkDelete = async () => {
    if (selectedIds.length === 0) return;
    const confirmed = window.confirm(
      `CẢNH BÁO: Bạn có chắc chắn muốn xóa vĩnh viễn ${selectedIds.length} tin đăng đã chọn không?`
    );
    if (!confirmed) return;

    setBulkActionLoading(true);
    setToast(null);

    const { error } = await supabaseBrowserClient
      .from("lost_found_items")
      .delete()
      .in("id", selectedIds);

    setBulkActionLoading(false);

    if (error) {
      setToast(`Lỗi xóa hàng loạt: ${error.message}`);
      return;
    }

    setToast(`Đã xóa thành công ${selectedIds.length} tin đồ thất lạc.`);
    const deletedSet = new Set(selectedIds);
    setItems((prev) => prev.filter((i) => !deletedSet.has(i.id)));
    setSelectedIds([]);
  };

  return (
    <AdminLayout>
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
        <div>
          <h1 className="text-xl font-bold text-slate-900 tracking-tight flex items-center gap-2">
            <PackageSearch className="w-5 h-5 text-slate-800" />
            <span>Kiểm Duyệt & Quản Lý Đồ Thất Lạc UTH</span>
          </h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Tiếp nhận đồ bàn giao tập trung, kiểm duyệt nội dung vi phạm, xác nhận trao trả
          </p>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={fetchItems}
            disabled={loading}
            className="inline-flex items-center gap-1.5 px-3 py-2 bg-white text-slate-700 text-xs font-medium rounded-lg shadow-subtle hover:bg-slate-50 transition active:scale-[0.98] disabled:opacity-60"
          >
            <RefreshCw className={`w-3.5 h-3.5 stroke-[1.5] ${loading ? "animate-spin" : ""}`} />
            <span>Làm mới</span>
          </button>

          <Link
            href="/lost-found"
            target="_blank"
            className="inline-flex items-center gap-1.5 px-3 py-2 bg-slate-900 text-white text-xs font-semibold rounded-lg hover:bg-slate-800 transition active:scale-[0.98]"
          >
            <span>Xem Cổng Sinh Viên</span>
            <ExternalLink className="w-3.5 h-3.5" />
          </Link>
        </div>
      </div>

      {toast && (
        <div className="mb-5 p-3 bg-blue-50 text-slate-800 text-xs rounded-lg font-medium flex items-center justify-between border border-blue-100">
          <span>{toast}</span>
          <button onClick={() => setToast(null)} className="text-slate-400 hover:text-slate-700">
            <X className="w-3.5 h-3.5" />
          </button>
        </div>
      )}

      {/* KPI Tonal Row */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <div className="bg-white rounded-xl p-4 shadow-subtle border border-slate-100">
          <div className="text-[10px] font-semibold uppercase tracking-wider text-slate-400">
            Tổng tin ghi nhận
          </div>
          <div className="text-2xl font-bold text-slate-900 mt-1 tabular-nums">
            {stats.total}
          </div>
        </div>

        <div className="bg-white rounded-xl p-4 shadow-subtle border border-slate-100">
          <div className="text-[10px] font-semibold uppercase tracking-wider text-rose-500">
            Tin báo mất đồ (Lost)
          </div>
          <div className="text-2xl font-bold text-rose-600 mt-1 tabular-nums">
            {stats.lost}
          </div>
        </div>

        <div className="bg-white rounded-xl p-4 shadow-subtle border border-slate-100">
          <div className="text-[10px] font-semibold uppercase tracking-wider text-emerald-600">
            Tin nhặt được (Found)
          </div>
          <div className="text-2xl font-bold text-emerald-700 mt-1 tabular-nums">
            {stats.found}
          </div>
        </div>

        <div className="bg-white rounded-xl p-4 shadow-subtle border border-slate-100">
          <div className="text-[10px] font-semibold uppercase tracking-wider text-blue-600">
            Đã trao trả thành công
          </div>
          <div className="text-2xl font-bold text-blue-700 mt-1 tabular-nums">
            {stats.resolved}
          </div>
        </div>
      </div>

      {/* Multi-field Search & Tag Filtering Section */}
      <div className="bg-white rounded-xl p-5 shadow-subtle border border-slate-100 mb-5 space-y-4">
        {/* Row 1: Quick Tag Pills for Kind */}
        <div className="flex flex-wrap items-center justify-between gap-3 pb-3 border-b border-slate-100">
          <div className="flex items-center gap-1.5 text-xs font-semibold text-slate-700">
            <Tag className="w-3.5 h-3.5 text-slate-400" />
            <span>Phân loại tin:</span>
          </div>

          <div className="flex flex-wrap items-center gap-1.5">
            <button
              onClick={() => setSelectedKind("ALL")}
              className={`px-3 py-1.5 rounded-lg text-xs font-medium transition ${
                selectedKind === "ALL"
                  ? "bg-slate-900 text-white shadow-sm"
                  : "bg-slate-100 text-slate-600 hover:bg-slate-200"
              }`}
            >
              Tất cả ({stats.total})
            </button>
            <button
              onClick={() => setSelectedKind("LOST")}
              className={`px-3 py-1.5 rounded-lg text-xs font-medium transition ${
                selectedKind === "LOST"
                  ? "bg-rose-600 text-white shadow-sm"
                  : "bg-rose-50 text-rose-700 hover:bg-rose-100"
              }`}
            >
              Tin mất đồ ({stats.lost})
            </button>
            <button
              onClick={() => setSelectedKind("FOUND")}
              className={`px-3 py-1.5 rounded-lg text-xs font-medium transition ${
                selectedKind === "FOUND"
                  ? "bg-emerald-600 text-white shadow-sm"
                  : "bg-emerald-50 text-emerald-700 hover:bg-emerald-100"
              }`}
            >
              Tin nhặt được ({stats.found})
            </button>
          </div>
        </div>

        {/* Row 2: Category Tag Pills with Badges */}
        <div className="flex flex-wrap items-center gap-1.5">
          <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider mr-1">
            Danh mục:
          </span>
          {CATEGORIES.map((cat) => {
            const count = stats.byCat[cat.id] ?? 0;
            const isSelected = selectedCategory === cat.id;
            return (
              <button
                key={cat.id}
                onClick={() => setSelectedCategory(cat.id)}
                className={`px-2.5 py-1 rounded-md text-xs font-medium transition inline-flex items-center gap-1.5 ${
                  isSelected
                    ? "bg-blue-600 text-white shadow-sm"
                    : "bg-slate-50 text-slate-600 hover:bg-slate-100 border border-slate-200/60"
                }`}
              >
                <span>{cat.label}</span>
                <span
                  className={`text-[10px] px-1.5 py-0.2 rounded-full font-bold ${
                    isSelected ? "bg-white/20 text-white" : "bg-slate-200 text-slate-600"
                  }`}
                >
                  {count}
                </span>
              </button>
            );
          })}
        </div>

        {/* Row 3: Multi-field Detailed Search Grid */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-3 pt-2">
            {/* Ô 1: Tên đồ vật / Mô tả */}
          <div className="relative">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
            <input
              type="text"
              value={searchKeyword}
              onChange={(e) => setSearchKeyword(e.target.value)}
              placeholder="1. Tên món đồ, chi tiết mô tả..."
              className="w-full pl-9 pr-7 py-2 bg-[#F8FAFC] border border-slate-200 rounded-lg text-xs focus:outline-none focus:ring-1 focus:ring-slate-900 focus:bg-white transition"
            />
            {searchKeyword && (
              <button
                type="button"
                aria-label="Xóa từ khóa tìm kiếm"
                onClick={() => setSearchKeyword("")}
                className="absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
              >
                <X className="w-3.5 h-3.5" />
              </button>
            )}
          </div>

          {/* Ô 2: Địa điểm / Cơ sở */}
          <div className="relative">
            <MapPin className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
            <input
              type="text"
              value={searchLocation}
              onChange={(e) => setSearchLocation(e.target.value)}
              placeholder="2. Vị trí, phòng học, Cơ sở 1/2/3..."
              className="w-full pl-9 pr-7 py-2 bg-[#F8FAFC] border border-slate-200 rounded-lg text-xs focus:outline-none focus:ring-1 focus:ring-slate-900 focus:bg-white transition"
            />
            {searchLocation && (
              <button
                type="button"
                aria-label="Xóa địa điểm tìm kiếm"
                onClick={() => setSearchLocation("")}
                className="absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
              >
                <X className="w-3.5 h-3.5" />
              </button>
            )}
          </div>

          {/* Ô 3: Người liên hệ / SĐT */}
          <div className="relative">
            <User className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
            <input
              type="text"
              value={searchContact}
              onChange={(e) => setSearchContact(e.target.value)}
              placeholder="3. Tên người liên hệ, số điện thoại..."
              className="w-full pl-9 pr-7 py-2 bg-[#F8FAFC] border border-slate-200 rounded-lg text-xs focus:outline-none focus:ring-1 focus:ring-slate-900 focus:bg-white transition"
            />
            {searchContact && (
              <button
                type="button"
                aria-label="Xóa thông tin liên hệ"
                onClick={() => setSearchContact("")}
                className="absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
              >
                <X className="w-3.5 h-3.5" />
              </button>
            )}
          </div>
        </div>

        {/* Row 4: Status filter, Image toggle & Reset Button */}
        <div className="flex flex-wrap items-center justify-between gap-3 pt-2 border-t border-slate-100">
          <div className="flex flex-wrap items-center gap-4">
            {/* Status Dropdown */}
            <div className="flex items-center gap-2">
              <span className="text-xs text-slate-500 font-medium">Trạng thái:</span>
              <select
                value={selectedStatus}
                onChange={(e) => setSelectedStatus(e.target.value as any)}
                className="px-3 py-1.5 bg-[#F8FAFC] border border-slate-200 rounded-lg text-xs font-medium focus:outline-none focus:ring-1 focus:ring-slate-900 text-slate-700"
              >
                <option value="ALL">Tất cả trạng thái</option>
                <option value="UNRESOLVED">Chưa trao trả ({stats.unresolved})</option>
                <option value="RESOLVED">Đã trao trả xong ({stats.resolved})</option>
              </select>
            </div>

            {/* Checkbox: Chỉ hiện tin có ảnh */}
            <label className="inline-flex items-center gap-2 cursor-pointer text-xs text-slate-700 select-none">
              <input
                type="checkbox"
                checked={onlyHasImage}
                onChange={(e) => setOnlyHasImage(e.target.checked)}
                className="w-4 h-4 rounded text-slate-900 focus:ring-0 cursor-pointer"
              />
              <span className="inline-flex items-center gap-1 font-medium">
                <ImageIcon className="w-3.5 h-3.5 text-slate-500" />
                <span>Chỉ hiện tin có ảnh ({stats.withImage})</span>
              </span>
            </label>
          </div>

          {/* Reset Filters Button */}
          {hasActiveFilters && (
            <button
              onClick={handleResetFilters}
              className="inline-flex items-center gap-1 px-2.5 py-1.5 text-xs text-rose-600 hover:bg-rose-50 rounded-lg font-medium transition"
            >
              <RotateCcw className="w-3 h-3" />
              <span>Xóa toàn bộ bộ lọc</span>
            </button>
          )}
        </div>

        {/* Active Filter Chips summary */}
        {hasActiveFilters && (
          <div className="pt-2 flex flex-wrap items-center gap-1.5 text-[11px] text-slate-500">
            <span className="font-semibold text-slate-400">Đang lọc theo:</span>
            {selectedKind !== "ALL" && (
              <span className="inline-flex items-center gap-1 px-2 py-0.5 bg-slate-100 rounded text-slate-700">
                Loại: {selectedKind === "LOST" ? "Báo mất" : "Nhặt được"}
                <button onClick={() => setSelectedKind("ALL")}>
                  <X className="w-3 h-3 hover:text-rose-600" />
                </button>
              </span>
            )}
            {selectedCategory !== "ALL" && (
              <span className="inline-flex items-center gap-1 px-2 py-0.5 bg-slate-100 rounded text-slate-700">
                Danh mục: {CATEGORIES.find((c) => c.id === selectedCategory)?.label}
                <button onClick={() => setSelectedCategory("ALL")}>
                  <X className="w-3 h-3 hover:text-rose-600" />
                </button>
              </span>
            )}
            {selectedStatus !== "ALL" && (
              <span className="inline-flex items-center gap-1 px-2 py-0.5 bg-slate-100 rounded text-slate-700">
                Trạng thái: {selectedStatus === "RESOLVED" ? "Đã trao trả" : "Chưa trao trả"}
                <button onClick={() => setSelectedStatus("ALL")}>
                  <X className="w-3 h-3 hover:text-rose-600" />
                </button>
              </span>
            )}
            {searchKeyword.trim() && (
              <span className="inline-flex items-center gap-1 px-2 py-0.5 bg-slate-100 rounded text-slate-700">
                Từ khóa: &quot;{searchKeyword}&quot;
                <button onClick={() => setSearchKeyword("")}>
                  <X className="w-3 h-3 hover:text-rose-600" />
                </button>
              </span>
            )}
            {searchLocation.trim() && (
              <span className="inline-flex items-center gap-1 px-2 py-0.5 bg-slate-100 rounded text-slate-700">
                Vị trí: &quot;{searchLocation}&quot;
                <button onClick={() => setSearchLocation("")}>
                  <X className="w-3 h-3 hover:text-rose-600" />
                </button>
              </span>
            )}
            {searchContact.trim() && (
              <span className="inline-flex items-center gap-1 px-2 py-0.5 bg-slate-100 rounded text-slate-700">
                Liên hệ: &quot;{searchContact}&quot;
                <button onClick={() => setSearchContact("")}>
                  <X className="w-3 h-3 hover:text-rose-600" />
                </button>
              </span>
            )}
            {onlyHasImage && (
              <span className="inline-flex items-center gap-1 px-2 py-0.5 bg-slate-100 rounded text-slate-700">
                Chỉ có ảnh
                <button onClick={() => setOnlyHasImage(false)}>
                  <X className="w-3 h-3 hover:text-rose-600" />
                </button>
              </span>
            )}
          </div>
        )}
      </div>

      {/* Floating Bulk Actions Toolbar when Checkboxes are Selected */}
      {selectedIds.length > 0 && (
        <div className="mb-4 p-3 bg-slate-900 text-white rounded-xl shadow-lg flex flex-wrap items-center justify-between gap-3 animate-in fade-in slide-in-from-top-2 duration-150">
          <div className="flex items-center gap-3">
            <span className="text-xs font-semibold px-2 py-1 bg-white/10 rounded-md">
              Đã chọn: {selectedIds.length} tin
            </span>
            <button
              onClick={() => setSelectedIds([])}
              className="text-xs text-slate-300 hover:text-white underline"
            >
              Bỏ chọn tất cả
            </button>
          </div>

          <div className="flex flex-wrap items-center gap-2">
            <button
              onClick={() => handleBulkUpdateStatus(true)}
              disabled={bulkActionLoading}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-semibold rounded-lg transition active:scale-[0.98] disabled:opacity-50"
            >
              <CheckCircle2 className="w-3.5 h-3.5" />
              <span>Đánh dấu Đã trao trả ({selectedIds.length})</span>
            </button>

            <button
              onClick={() => handleBulkUpdateStatus(false)}
              disabled={bulkActionLoading}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-slate-700 hover:bg-slate-600 text-white text-xs font-semibold rounded-lg transition active:scale-[0.98] disabled:opacity-50"
            >
              <RotateCcw className="w-3.5 h-3.5" />
              <span>Mở lại / Đang tìm ({selectedIds.length})</span>
            </button>

            <button
              onClick={handleBulkDelete}
              disabled={bulkActionLoading}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-rose-600 hover:bg-rose-500 text-white text-xs font-semibold rounded-lg transition active:scale-[0.98] disabled:opacity-50"
            >
              <Trash2 className="w-3.5 h-3.5" />
              <span>Xóa ({selectedIds.length})</span>
            </button>
          </div>
        </div>
      )}

      {/* Moderation Table */}
      <div className="bg-white rounded-xl shadow-subtle border border-slate-100 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-[#F8FAFC] text-slate-500 font-semibold uppercase tracking-wider border-b border-slate-100">
              <tr>
                {/* Header Checkbox */}
                <th className="py-3 px-4 w-10">
                  <button
                    onClick={handleToggleSelectAll}
                    className="flex items-center text-slate-400 hover:text-slate-800 transition"
                    title={allFilteredSelected ? "Bỏ chọn tất cả" : "Chọn tất cả tin đang lọc"}
                  >
                    {allFilteredSelected ? (
                      <CheckSquare className="w-4 h-4 text-slate-900" />
                    ) : someFilteredSelected ? (
                      <MinusSquare className="w-4 h-4 text-slate-900" />
                    ) : (
                      <Square className="w-4 h-4" />
                    )}
                  </button>
                </th>
                <th className="py-3 px-4">Ảnh</th>
                <th className="py-3 px-4">Món đồ & Danh mục</th>
                <th className="py-3 px-4">Phân loại</th>
                <th className="py-3 px-4">Địa điểm & Ngày</th>
                <th className="py-3 px-4">Liên hệ</th>
                <th className="py-3 px-4">Trạng thái</th>
                <th className="py-3 px-4 text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {loading ? (
                <tr>
                  <td colSpan={8} className="py-12 text-center text-slate-400">
                    <RefreshCw className="w-5 h-5 animate-spin mx-auto mb-2 text-slate-300" />
                    Đang tải dữ liệu đồ thất lạc...
                  </td>
                </tr>
              ) : filteredItems.length === 0 ? (
                <tr>
                  <td colSpan={8} className="py-12 text-center text-slate-400">
                    <PackageSearch className="w-8 h-8 mx-auto mb-2 text-slate-300 stroke-[1.5]" />
                    <p className="font-medium text-slate-600">Không tìm thấy tin đăng nào phù hợp</p>
                    <p className="text-[11px] text-slate-400 mt-0.5">
                      Hãy thử nới lỏng các tiêu chí tìm kiếm hoặc xóa bộ lọc.
                    </p>
                    {hasActiveFilters && (
                      <button
                        onClick={handleResetFilters}
                        className="mt-3 inline-flex items-center gap-1 px-3 py-1.5 bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-semibold rounded-lg transition"
                      >
                        <RotateCcw className="w-3 h-3" />
                        <span>Đặt lại bộ lọc</span>
                      </button>
                    )}
                  </td>
                </tr>
              ) : (
                filteredItems.map((item) => {
                  const isChecked = selectedIds.includes(item.id);
                  return (
                    <tr
                      key={item.id}
                      className={`transition ${
                        isChecked ? "bg-blue-50/50" : "hover:bg-[#F8FAFC]/80"
                      }`}
                    >
                      {/* Checkbox */}
                      <td className="py-3 px-4 w-10">
                        <button
                          onClick={() => handleToggleSelectItem(item.id)}
                          className="flex items-center text-slate-400 hover:text-slate-800 transition"
                        >
                          {isChecked ? (
                            <CheckSquare className="w-4 h-4 text-slate-900" />
                          ) : (
                            <Square className="w-4 h-4" />
                          )}
                        </button>
                      </td>

                      {/* Thumbnail */}
                      <td className="py-3 px-4">
                        {item.image_url ? (
                          <div className="relative w-12 h-12 rounded-lg overflow-hidden border border-slate-200">
                            <Image
                              src={item.image_url}
                              alt={item.title}
                              fill
                              sizes="48px"
                              className="object-cover"
                              unoptimized
                            />
                          </div>
                        ) : (
                          <div className="w-12 h-12 rounded-lg bg-slate-100 flex items-center justify-center text-slate-400">
                            <PackageSearch className="w-5 h-5 stroke-[1.5]" />
                          </div>
                        )}
                      </td>

                      {/* Title & Category */}
                      <td className="py-3 px-4 max-w-[220px]">
                        <Link
                          href={`/lost-found/${item.id}`}
                          target="_blank"
                          className="font-bold text-slate-900 hover:text-blue-600 line-clamp-1 flex items-center gap-1"
                        >
                          <span>{item.title}</span>
                          <ExternalLink className="w-3 h-3 text-slate-400 shrink-0" />
                        </Link>
                        <div className="text-[11px] text-slate-500 mt-0.5 truncate">
                          {CATEGORIES.find((c) => c.id === item.category)?.label || item.category}
                        </div>
                      </td>

                      {/* Kind */}
                      <td className="py-3 px-4">
                        <span
                          className={`px-2 py-0.5 rounded text-[10px] font-bold uppercase tracking-wider ${
                            item.kind === "LOST"
                              ? "bg-rose-50 text-rose-700 border border-rose-200/50"
                              : "bg-emerald-50 text-emerald-700 border border-emerald-200/50"
                          }`}
                        >
                          {item.kind === "LOST" ? "Mất đồ" : "Nhặt được"}
                        </span>
                      </td>

                      {/* Location & Date */}
                      <td className="py-3 px-4">
                        <div className="font-medium text-slate-800 line-clamp-1">
                          {item.location}
                        </div>
                        <div className="text-[11px] text-slate-400 mt-0.5">
                          {item.date}
                        </div>
                      </td>

                      {/* Contact info */}
                      <td className="py-3 px-4">
                        <div className="font-semibold text-slate-800">
                          {item.contact_name}
                        </div>
                        <div className="text-[11px] text-slate-500 tabular-nums">
                          {item.contact_phone}
                        </div>
                      </td>

                      {/* Resolved status */}
                      <td className="py-3 px-4">
                        <span
                          className={`px-2 py-0.5 rounded text-[10px] font-semibold ${
                            item.is_resolved
                              ? "bg-blue-50 text-blue-700 border border-blue-200/50"
                              : "bg-amber-50 text-amber-700 border border-amber-200/50"
                          }`}
                        >
                          {item.is_resolved ? "✓ Đã trao trả" : "• Đang tìm"}
                        </span>
                      </td>

                      {/* Actions */}
                      <td className="py-3 px-4 text-right space-x-1 whitespace-nowrap">
                        <button
                          onClick={() => handleToggleResolved(item.id, item.is_resolved)}
                          disabled={updatingId === item.id}
                          className={`px-2.5 py-1 rounded text-[11px] font-semibold transition ${
                            item.is_resolved
                              ? "bg-slate-100 text-slate-700 hover:bg-slate-200"
                              : "bg-emerald-50 text-emerald-700 hover:bg-emerald-100"
                          }`}
                          title={
                            item.is_resolved
                              ? "Đánh dấu chưa nhận"
                              : "Xác nhận bàn giao thành công"
                          }
                        >
                          {item.is_resolved ? "Mở lại" : "Đã trao trả"}
                        </button>

                        <button
                          onClick={() => handleDeleteItem(item.id, item.title)}
                          disabled={updatingId === item.id}
                          className="px-2 py-1 bg-rose-50 text-rose-700 rounded text-[11px] font-semibold hover:bg-rose-100 transition"
                          title="Xóa tin vi phạm"
                        >
                          <Trash2 className="w-3.5 h-3.5 inline" />
                        </button>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>
    </AdminLayout>
  );
}
