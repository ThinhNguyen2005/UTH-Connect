"use client";

import React, { useEffect, useState, useMemo } from "react";
import Link from "next/link";
import { useGetIdentity } from "@refinedev/core";
import { PortalLayout } from "@/components/layout/PortalLayout";
import { supabaseBrowserClient } from "@/utils/supabase-client";
import { getBookImageUrl, deleteBookImage } from "@/utils/bookImages";
import type { AppUserIdentity } from "@/providers/auth-provider";
import {
  BookOpen,
  Plus,
  Search,
  Filter,
  ArrowUpDown,
  Tag,
  CheckCircle2,
  Trash2,
  User,
  Phone,
  RefreshCw,
  Sparkles,
  BookMarked,
  SlidersHorizontal,
  X,
} from "lucide-react";

export interface BookListingItem {
  id: string;
  owner_user_id: string;
  title: string;
  subject: string;
  condition: "NEW" | "GOOD" | "USED";
  deal_type: "SALE" | "EXCHANGE";
  price_vnd: number | null;
  description: string;
  seller_name: string;
  contact: string;
  image_path: string | null;
  status: "AVAILABLE" | "SOLD";
  created_at: string;
  updated_at: string;
  profiles?: {
    display_name?: string | null;
    email?: string | null;
    student_id?: string | null;
  } | null;
}

const CONDITION_MAP: Record<string, { label: string; badgeClass: string }> = {
  NEW: { label: "Mới 100%", badgeClass: "bg-emerald-50 text-emerald-700 font-medium" },
  GOOD: { label: "Còn tốt", badgeClass: "bg-blue-50 text-blue-700 font-medium" },
  USED: { label: "Đã qua sử dụng", badgeClass: "bg-slate-100 text-slate-700 font-medium" },
};

const DEAL_TYPE_MAP: Record<string, { label: string; badgeClass: string }> = {
  SALE: { label: "Bán lại", badgeClass: "bg-amber-50 text-amber-800 font-medium" },
  EXCHANGE: { label: "Trao đổi", badgeClass: "bg-purple-50 text-purple-700 font-medium" },
};

const POPULAR_SUBJECTS = [
  "Tất cả",
  "Công nghệ thông tin",
  "Kinh tế vận tải",
  "Cơ khí - Ô tô",
  "Hàng hải & Cảng",
  "Điện - Điện tử",
  "Toán & Khoa học cơ bản",
  "Ngoại ngữ",
  "Giáo trình Đại cương",
];

export default function BookMarketPage() {
  const { data: user } = useGetIdentity<AppUserIdentity>();

  const [listings, setListings] = useState<BookListingItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Filters & State
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedSubject, setSelectedSubject] = useState("Tất cả");
  const [dealFilter, setDealFilter] = useState<"ALL" | "SALE" | "EXCHANGE">("ALL");
  const [conditionFilter, setConditionFilter] = useState<"ALL" | "NEW" | "GOOD" | "USED">("ALL");
  const [statusFilter, setStatusFilter] = useState<"AVAILABLE" | "SOLD" | "ALL">("AVAILABLE");
  const [activeTab, setActiveTab] = useState<"all" | "mine">("all");
  const [sortBy, setSortBy] = useState<"newest" | "price_asc" | "price_desc">("newest");
  const [togglingId, setTogglingId] = useState<string | null>(null);
  const [deletingId, setDeletingId] = useState<string | null>(null);

  async function fetchListings() {
    setLoading(true);
    setError(null);
    try {
      const { data, error: fetchErr } = await supabaseBrowserClient
        .from("book_listings")
        .select("*, profiles:owner_user_id(display_name, email, student_id)")
        .order("created_at", { ascending: false });

      if (fetchErr) throw fetchErr;
      setListings((data as BookListingItem[]) || []);
    } catch (err: any) {
      setError(err.message || "Không thể tải danh sách sách giáo trình. Vui lòng thử lại.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    fetchListings();
  }, []);

  // Toggle status AVAILABLE <-> SOLD for owner
  const handleToggleStatus = async (item: BookListingItem) => {
    if (togglingId) return;
    const nextStatus = item.status === "AVAILABLE" ? "SOLD" : "AVAILABLE";
    setTogglingId(item.id);

    try {
      const { error: updateErr } = await supabaseBrowserClient
        .from("book_listings")
        .update({
          status: nextStatus,
          updated_at: new Date().toISOString(),
        })
        .eq("id", item.id);

      if (updateErr) throw updateErr;

      setListings((prev) =>
        prev.map((l) => (l.id === item.id ? { ...l, status: nextStatus } : l))
      );
    } catch (err: any) {
      alert("Không thể cập nhật trạng thái: " + (err.message || "Đã xảy ra lỗi"));
    } finally {
      setTogglingId(null);
    }
  };

  // Delete listing for owner
  const handleDeleteListing = async (item: BookListingItem) => {
    if (deletingId) return;
    const confirmDelete = window.confirm(
      `Bạn có chắc chắn muốn xóa tin sách "${item.title}" không? Thao tác này không thể hoàn tác.`
    );
    if (!confirmDelete) return;

    setDeletingId(item.id);
    try {
      // 1. Dọn dẹp ảnh storage trước nếu có
      if (item.image_path) {
        await deleteBookImage(item.image_path);
      }

      // 2. Xóa bản ghi database
      const { error: delErr } = await supabaseBrowserClient
        .from("book_listings")
        .delete()
        .eq("id", item.id);

      if (delErr) throw delErr;

      setListings((prev) => prev.filter((l) => l.id !== item.id));
    } catch (err: any) {
      alert("Không thể xóa tin sách: " + (err.message || "Đã xảy ra lỗi"));
    } finally {
      setDeletingId(null);
    }
  };

  // Filtered & Sorted listings
  const filteredListings = useMemo(() => {
    let result = [...listings];

    // Tab "Tin của tôi"
    if (activeTab === "mine") {
      if (!user?.id) return [];
      result = result.filter((l) => l.owner_user_id === user.id);
    }

    // Status filter
    if (statusFilter !== "ALL") {
      result = result.filter((l) => l.status === statusFilter);
    }

    // Deal type filter
    if (dealFilter !== "ALL") {
      result = result.filter((l) => l.deal_type === dealFilter);
    }

    // Condition filter
    if (conditionFilter !== "ALL") {
      result = result.filter((l) => l.condition === conditionFilter);
    }

    // Subject filter
    if (selectedSubject !== "Tất cả") {
      result = result.filter(
        (l) =>
          l.subject.toLowerCase().includes(selectedSubject.toLowerCase()) ||
          selectedSubject.toLowerCase().includes(l.subject.toLowerCase())
      );
    }

    // Search query
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase().trim();
      result = result.filter(
        (l) =>
          l.title.toLowerCase().includes(q) ||
          l.subject.toLowerCase().includes(q) ||
          l.description.toLowerCase().includes(q) ||
          l.seller_name.toLowerCase().includes(q)
      );
    }

    // Sorting
    result.sort((a, b) => {
      if (sortBy === "price_asc") {
        return (a.price_vnd || 0) - (b.price_vnd || 0);
      }
      if (sortBy === "price_desc") {
        return (b.price_vnd || 0) - (a.price_vnd || 0);
      }
      return new Date(b.created_at).getTime() - new Date(a.created_at).getTime();
    });

    return result;
  }, [
    listings,
    activeTab,
    user?.id,
    statusFilter,
    dealFilter,
    conditionFilter,
    selectedSubject,
    searchQuery,
    sortBy,
  ]);

  const myBookCount = useMemo(() => {
    if (!user?.id) return 0;
    return listings.filter((l) => l.owner_user_id === user.id).length;
  }, [listings, user?.id]);

  return (
    <PortalLayout>
      <div className="space-y-6">
        {/* Header Block: Tonal Layering, No Borders */}
        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-2xl font-bold tracking-tight text-slate-900">
                Chợ Sách Giáo Trình & Tài Liệu UTH
              </h1>
            </div>
            <p className="text-sm text-slate-600 mt-1">
              Trao đổi, mua bán giáo trình và đề cương ôn tập giữa các khóa sinh viên UTH.
            </p>
          </div>

          <div className="flex items-center gap-3">
            {user && (
              <button
                onClick={() => setActiveTab(activeTab === "mine" ? "all" : "mine")}
                className={`px-3 py-2 rounded-lg text-xs font-semibold transition active:scale-[0.98] ${
                  activeTab === "mine"
                    ? "bg-slate-900 text-white"
                    : "bg-white text-slate-700 shadow-subtle hover:bg-slate-100"
                }`}
              >
                Sách của tôi ({myBookCount})
              </button>
            )}

            <Link
              href="/books/create"
              className="inline-flex items-center gap-1.5 px-4 py-2 bg-[#0F5FC2] hover:bg-[#0c4ea3] text-white rounded-lg text-xs font-semibold transition shadow-subtle active:scale-[0.98]"
            >
              <Plus className="w-4 h-4 stroke-[2]" />
              <span>Đăng sách mới</span>
            </Link>
          </div>
        </div>

        {/* Filter & Search Bar Card (White Surface on Slate 50) */}
        <div className="bg-white rounded-xl p-4 shadow-subtle space-y-4">
          {/* Top Row: Search Input + Deal Type + Sort */}
          <div className="grid grid-cols-1 md:grid-cols-12 gap-3">
            {/* Search Input */}
            <div className="md:col-span-6 relative">
              <Search className="w-4 h-4 stroke-[1.5] text-slate-400 absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Tìm tên giáo trình, môn học, tác giả..."
                className="w-full pl-9 pr-8 py-2 bg-[#F1F5F9] rounded-lg text-xs text-slate-900 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-[#0F5FC2]"
              />
              {searchQuery && (
                <button
                  onClick={() => setSearchQuery("")}
                  className="absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
                >
                  <X className="w-3.5 h-3.5" />
                </button>
              )}
            </div>

            {/* Deal Type Filter */}
            <div className="md:col-span-3">
              <select
                value={dealFilter}
                onChange={(e) => setDealFilter(e.target.value as any)}
                className="w-full px-3 py-2 bg-[#F1F5F9] rounded-lg text-xs text-slate-700 font-medium focus:outline-none focus:ring-2 focus:ring-[#0F5FC2]"
              >
                <option value="ALL">Hình thức: Tất cả</option>
                <option value="SALE">Chỉ xem sách Bán lại</option>
                <option value="EXCHANGE">Chỉ xem sách Trao đổi</option>
              </select>
            </div>

            {/* Sort Options */}
            <div className="md:col-span-3">
              <select
                value={sortBy}
                onChange={(e) => setSortBy(e.target.value as any)}
                className="w-full px-3 py-2 bg-[#F1F5F9] rounded-lg text-xs text-slate-700 font-medium focus:outline-none focus:ring-2 focus:ring-[#0F5FC2]"
              >
                <option value="newest">Sắp xếp: Mới nhất</option>
                <option value="price_asc">Giá: Thấp đến cao</option>
                <option value="price_desc">Giá: Cao đến thấp</option>
              </select>
            </div>
          </div>

          {/* Bottom Row: Subject Filter Pills + Status Filter */}
          <div className="flex flex-wrap items-center justify-between gap-3 pt-2">
            {/* Subject horizontal scrolling pills */}
            <div className="flex items-center gap-1.5 overflow-x-auto pb-1 max-w-full text-xs">
              {POPULAR_SUBJECTS.map((subj) => {
                const isSelected = selectedSubject === subj;
                return (
                  <button
                    key={subj}
                    onClick={() => setSelectedSubject(subj)}
                    className={`shrink-0 px-3 py-1.5 rounded-lg transition active:scale-[0.98] ${
                      isSelected
                        ? "bg-slate-900 text-white font-medium"
                        : "bg-[#F1F5F9] text-slate-600 hover:bg-slate-200"
                    }`}
                  >
                    {subj}
                  </button>
                );
              })}
            </div>

            {/* Status & Condition dropdowns */}
            <div className="flex items-center gap-2 shrink-0">
              <select
                value={conditionFilter}
                onChange={(e) => setConditionFilter(e.target.value as any)}
                className="px-2.5 py-1.5 bg-[#F1F5F9] rounded-lg text-xs text-slate-600 focus:outline-none"
              >
                <option value="ALL">Tình trạng: Tất cả</option>
                <option value="NEW">Mới 100%</option>
                <option value="GOOD">Còn tốt</option>
                <option value="USED">Đã dùng</option>
              </select>

              <select
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value as any)}
                className="px-2.5 py-1.5 bg-[#F1F5F9] rounded-lg text-xs text-slate-600 focus:outline-none"
              >
                <option value="AVAILABLE">Còn sách</option>
                <option value="SOLD">Đã bán/xong</option>
                <option value="ALL">Mọi trạng thái</option>
              </select>
            </div>
          </div>
        </div>

        {/* Tab Indicator if filtering "mine" */}
        {activeTab === "mine" && (
          <div className="flex items-center justify-between bg-blue-50/70 rounded-xl px-4 py-2.5 text-xs text-blue-900">
            <span className="font-medium">
              Đang lọc các tin sách do bạn đăng ({filteredListings.length} tin)
            </span>
            <button
              onClick={() => setActiveTab("all")}
              className="text-blue-700 underline font-medium hover:text-blue-900"
            >
              Xem tất cả sách trên chợ
            </button>
          </div>
        )}

        {/* State 1: Error State */}
        {error && (
          <div className="bg-red-50 rounded-xl p-5 text-center space-y-2">
            <p className="text-sm font-semibold text-red-800">{error}</p>
            <button
              onClick={fetchListings}
              className="px-3 py-1.5 bg-red-100 hover:bg-red-200 text-red-800 text-xs font-semibold rounded-lg transition"
            >
              Tải lại danh sách
            </button>
          </div>
        )}

        {/* State 2: Loading Skeleton Grid */}
        {loading && !error && (
          <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-5">
            {[1, 2, 3, 4, 5, 6].map((k) => (
              <div key={k} className="bg-white rounded-xl p-4 shadow-subtle space-y-3 animate-pulse">
                <div className="w-full aspect-[16/10] bg-slate-100 rounded-lg" />
                <div className="h-4 bg-slate-100 rounded w-3/4" />
                <div className="h-3 bg-slate-100 rounded w-1/2" />
                <div className="h-6 bg-slate-100 rounded w-1/3" />
              </div>
            ))}
          </div>
        )}

        {/* State 3: Empty State */}
        {!loading && !error && filteredListings.length === 0 && (
          <div className="bg-white rounded-xl p-12 text-center shadow-subtle space-y-3 max-w-lg mx-auto">
            <div className="w-12 h-12 rounded-xl bg-slate-100 flex items-center justify-center mx-auto text-slate-400">
              <BookMarked className="w-6 h-6 stroke-[1.5]" />
            </div>
            <h3 className="text-base font-bold text-slate-900">
              Không tìm thấy sách giáo trình phù hợp
            </h3>
            <p className="text-xs text-slate-500 leading-relaxed">
              {activeTab === "mine"
                ? "Bạn chưa đăng bài chia sẻ sách nào. Đăng giáo trình cũ để giúp các khóa sau nhé!"
                : "Thử thay đổi từ khóa tìm kiếm hoặc chọn khoa/môn học khác."}
            </p>
            <div className="pt-2">
              <Link
                href="/books/create"
                className="inline-flex items-center gap-1.5 px-4 py-2 bg-slate-900 text-white rounded-lg text-xs font-semibold hover:bg-slate-800 transition active:scale-[0.98]"
              >
                <Plus className="w-3.5 h-3.5 stroke-[2]" />
                <span>Đăng bán hoặc trao đổi ngay</span>
              </Link>
            </div>
          </div>
        )}

        {/* State 4: Content Grid */}
        {!loading && !error && filteredListings.length > 0 && (
          <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-5 items-start">
            {filteredListings.map((item) => {
              const isOwner = user?.id && item.owner_user_id === user.id;
              const imageUrl = getBookImageUrl(item.image_path);
              const isSold = item.status === "SOLD";

              return (
                <article
                  key={item.id}
                  className={`bg-white rounded-xl p-4 shadow-subtle flex flex-col justify-between transition hover:shadow-subtle-hover ${
                    isSold ? "opacity-75" : ""
                  }`}
                >
                  <div className="space-y-3">
                    {/* Book Cover Image with nested concentric radius (12px card -> 8px image) */}
                    <Link
                      href={`/books/${item.id}`}
                      className="block relative w-full aspect-[16/10] bg-[#F1F5F9] rounded-lg overflow-hidden group"
                    >
                      {imageUrl ? (
                        // eslint-disable-next-line @next/next/no-img-element
                        <img
                          src={imageUrl}
                          alt={item.title}
                          className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                          loading="lazy"
                        />
                      ) : (
                        <div className="w-full h-full flex flex-col items-center justify-center text-slate-400 gap-1.5">
                          <BookOpen className="w-8 h-8 stroke-[1.2]" />
                          <span className="text-[11px] font-medium">Chưa có ảnh bìa</span>
                        </div>
                      )}

                      {/* Status Badges Overlay */}
                      <div className="absolute top-2 left-2 flex flex-wrap gap-1">
                        <span
                          className={`px-2 py-0.5 rounded text-[10px] font-semibold tracking-wide ${
                            DEAL_TYPE_MAP[item.deal_type]?.badgeClass || "bg-slate-100 text-slate-700"
                          }`}
                        >
                          {DEAL_TYPE_MAP[item.deal_type]?.label || item.deal_type}
                        </span>

                        <span
                          className={`px-2 py-0.5 rounded text-[10px] font-semibold tracking-wide ${
                            CONDITION_MAP[item.condition]?.badgeClass || "bg-slate-100 text-slate-700"
                          }`}
                        >
                          {CONDITION_MAP[item.condition]?.label || item.condition}
                        </span>
                      </div>

                      {/* Sold Stamp */}
                      {isSold && (
                        <div className="absolute inset-0 bg-slate-900/40 backdrop-blur-[1px] flex items-center justify-center">
                          <span className="px-3 py-1 bg-red-600 text-white rounded text-xs font-bold uppercase tracking-wider shadow-sm">
                            Đã giao dịch xong
                          </span>
                        </div>
                      )}
                    </Link>

                    {/* Book Details */}
                    <div>
                      {/* Subject Tag */}
                      <div className="flex items-center justify-between text-xs text-slate-500 mb-1">
                        <span className="font-medium text-slate-600 truncate max-w-[200px]">
                          {item.subject}
                        </span>
                        {isOwner && (
                          <span className="px-1.5 py-0.5 rounded text-[10px] font-bold bg-blue-100 text-[#0F5FC2]">
                            Tin của bạn
                          </span>
                        )}
                      </div>

                      {/* Book Title */}
                      <Link href={`/books/${item.id}`} className="block group">
                        <h2 className="text-sm font-bold text-slate-900 line-clamp-2 leading-snug group-hover:text-[#0F5FC2] transition-colors">
                          {item.title}
                        </h2>
                      </Link>

                      {/* Price Section with tabular-nums */}
                      <div className="mt-2.5 flex items-baseline gap-1.5">
                        {item.deal_type === "SALE" && item.price_vnd ? (
                          <>
                            <span className="text-base font-bold text-slate-900 tabular-nums">
                              {new Intl.NumberFormat("vi-VN").format(item.price_vnd)}
                            </span>
                            <span className="text-xs text-slate-500 font-medium">VNĐ</span>
                          </>
                        ) : (
                          <span className="text-sm font-bold text-purple-700">
                            Đổi lấy giáo trình khác
                          </span>
                        )}
                      </div>

                      {/* Description excerpt */}
                      {item.description && (
                        <p className="text-xs text-slate-500 line-clamp-2 mt-1.5 leading-relaxed">
                          {item.description}
                        </p>
                      )}
                    </div>
                  </div>

                  {/* Card Footer: Seller info + Action Buttons */}
                  <div className="pt-3 mt-3 border-t border-slate-100 space-y-2">
                    {/* Seller name & contact preview */}
                    <div className="flex items-center justify-between text-[11px] text-slate-500">
                      <div className="flex items-center gap-1.5 truncate">
                        <User className="w-3.5 h-3.5 stroke-[1.5] text-slate-400 shrink-0" />
                        <span className="truncate font-medium text-slate-700">
                          {item.seller_name}
                        </span>
                      </div>
                      <span className="shrink-0 text-slate-400">
                        {new Date(item.created_at).toLocaleDateString("vi-VN")}
                      </span>
                    </div>

                    {/* Role-based Bottom Actions */}
                    {isOwner ? (
                      <div className="flex items-center gap-2 pt-1">
                        <button
                          onClick={() => handleToggleStatus(item)}
                          disabled={togglingId === item.id}
                          className={`flex-1 py-1.5 px-2 rounded-lg text-xs font-semibold transition active:scale-[0.98] ${
                            isSold
                              ? "bg-slate-100 text-slate-700 hover:bg-slate-200"
                              : "bg-emerald-50 text-emerald-800 hover:bg-emerald-100"
                          }`}
                        >
                          {togglingId === item.id
                            ? "Đang lưu..."
                            : isSold
                            ? "Đăng lại (Còn sách)"
                            : "Đánh dấu Đã bán"}
                        </button>

                        <button
                          onClick={() => handleDeleteListing(item)}
                          disabled={deletingId === item.id}
                          title="Xóa bài đăng"
                          className="p-1.5 bg-slate-100 hover:bg-red-50 text-slate-500 hover:text-red-600 rounded-lg transition"
                        >
                          <Trash2 className="w-4 h-4 stroke-[1.5]" />
                        </button>
                      </div>
                    ) : (
                      <div className="flex items-center gap-2 pt-1">
                        <Link
                          href={`/books/${item.id}`}
                          className="flex-1 text-center py-1.5 px-3 bg-[#F1F5F9] hover:bg-[#E2E8F0] text-slate-800 rounded-lg text-xs font-semibold transition active:scale-[0.98]"
                        >
                          Xem chi tiết & Liên hệ
                        </Link>
                      </div>
                    )}
                  </div>
                </article>
              );
            })}
          </div>
        )}
      </div>
    </PortalLayout>
  );
}
