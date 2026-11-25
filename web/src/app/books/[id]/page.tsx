"use client";

import React, { useEffect, useState, useCallback } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useGetIdentity } from "@refinedev/core";
import { PortalLayout } from "@/components/layout/PortalLayout";
import { supabaseBrowserClient } from "@/utils/supabase-client";
import { getBookImageUrl, deleteBookImage } from "@/utils/bookImages";
import type { AppUserIdentity } from "@/providers/auth-provider";
import type { BookListingItem } from "../page";
import {
  ArrowLeft,
  BookOpen,
  User,
  Phone,
  MessageCircle,
  Copy,
  Check,
  ShieldCheck,
  Trash2,
  Edit3,
  Calendar,
  AlertTriangle,
  Loader2,
  X,
  ExternalLink,
} from "lucide-react";

const CONDITION_MAP: Record<string, { label: string; badgeClass: string; desc: string }> = {
  NEW: {
    label: "Mới 100%",
    badgeClass: "bg-emerald-50 text-emerald-700",
    desc: "Chưa qua sử dụng, sách nguyên vẹn như lúc mua từ nhà xuất bản.",
  },
  GOOD: {
    label: "Còn tốt",
    badgeClass: "bg-blue-50 text-blue-700",
    desc: "Gáy sách chắc chắn, trang sách sạch sẽ, ít nếp gấp.",
  },
  USED: {
    label: "Đã qua sử dụng",
    badgeClass: "bg-slate-100 text-slate-700",
    desc: "Có thể có bút chì, highlight hoặc ghi chú bài tập trên các trang.",
  },
};

const DEAL_TYPE_MAP: Record<string, { label: string; badgeClass: string }> = {
  SALE: { label: "Bán lại", badgeClass: "bg-amber-50 text-amber-800" },
  EXCHANGE: { label: "Trao đổi sách", badgeClass: "bg-purple-50 text-purple-700" },
};

export default function BookDetailPage({ params }: { params: { id: string } }) {
  const router = useRouter();
  const { data: user } = useGetIdentity<AppUserIdentity>();

  const [book, setBook] = useState<BookListingItem | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [copied, setCopied] = useState(false);
  const [actionBusy, setActionBusy] = useState(false);

  // Edit Modal State (cho Người đăng)
  const [isEditOpen, setIsEditOpen] = useState(false);
  const [editTitle, setEditTitle] = useState("");
  const [editSubject, setEditSubject] = useState("");
  const [editCondition, setEditCondition] = useState<"NEW" | "GOOD" | "USED">("GOOD");
  const [editDealType, setEditDealType] = useState<"SALE" | "EXCHANGE">("SALE");
  const [editPriceVnd, setEditPriceVnd] = useState<string>("");
  const [editDescription, setEditDescription] = useState("");
  const [editContact, setEditContact] = useState("");
  const [editError, setEditError] = useState<string | null>(null);
  const [editSaving, setEditSaving] = useState(false);

  const loadBook = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const { data, error: err } = await supabaseBrowserClient
        .from("book_listings")
        .select("*, profiles:owner_user_id(display_name, email, student_id)")
        .eq("id", params.id)
        .maybeSingle();

      if (err) throw err;
      if (!data) {
        setError("Không tìm thấy sách giáo trình hoặc tin đã bị xóa.");
        return;
      }

      const item = data as BookListingItem;
      setBook(item);
      // Init edit fields
      setEditTitle(item.title);
      setEditSubject(item.subject);
      setEditCondition(item.condition);
      setEditDealType(item.deal_type);
      setEditPriceVnd(item.price_vnd ? String(item.price_vnd) : "");
      setEditDescription(item.description || "");
      setEditContact(item.contact);
    } catch (err: any) {
      setError(err.message || "Lỗi khi tải thông tin sách giáo trình.");
    } finally {
      setLoading(false);
    }
  }, [params.id]);

  useEffect(() => {
    loadBook();
  }, [loadBook]);

  const isOwner = user?.id && book?.owner_user_id === user.id;
  const imageUrl = getBookImageUrl(book?.image_path);

  // Copy contact to clipboard
  const handleCopyContact = async () => {
    if (!book?.contact) return;
    try {
      await navigator.clipboard.writeText(book.contact);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // Fallback
    }
  };

  // Toggle status AVAILABLE <-> SOLD
  const handleToggleStatus = async () => {
    if (!book || actionBusy) return;
    const nextStatus = book.status === "AVAILABLE" ? "SOLD" : "AVAILABLE";
    setActionBusy(true);

    try {
      const { error: updateErr } = await supabaseBrowserClient
        .from("book_listings")
        .update({
          status: nextStatus,
          updated_at: new Date().toISOString(),
        })
        .eq("id", book.id);

      if (updateErr) throw updateErr;

      setBook((prev) => (prev ? { ...prev, status: nextStatus } : null));
    } catch (err: any) {
      alert("Lỗi cập nhật: " + (err.message || "Thao tác thất bại"));
    } finally {
      setActionBusy(false);
    }
  };

  // Delete listing
  const handleDelete = async () => {
    if (!book || actionBusy) return;
    const confirmDel = window.confirm(
      "Bạn có chắc muốn xóa tin đăng này? Thao tác này sẽ xóa vĩnh viễn dữ liệu và ảnh sách."
    );
    if (!confirmDel) return;

    setActionBusy(true);
    try {
      if (book.image_path) {
        await deleteBookImage(book.image_path);
      }

      const { error: delErr } = await supabaseBrowserClient
        .from("book_listings")
        .delete()
        .eq("id", book.id);

      if (delErr) throw delErr;

      router.push("/books");
    } catch (err: any) {
      alert("Không thể xóa: " + (err.message || "Đã xảy ra lỗi"));
      setActionBusy(false);
    }
  };

  // Submit Edit form
  const handleSaveEdit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!book || editSaving) return;
    setEditError(null);

    const cleanTitle = editTitle.trim();
    if (!cleanTitle || cleanTitle.length > 160) {
      setEditError("Tiêu đề sách phải từ 1 đến 160 ký tự.");
      return;
    }

    const cleanSubject = editSubject.trim();
    if (!cleanSubject || cleanSubject.length > 100) {
      setEditError("Môn học / Ngành phải từ 1 đến 100 ký tự.");
      return;
    }

    const cleanContact = editContact.trim();
    if (!cleanContact || cleanContact.length < 5) {
      setEditError("Thông tin liên hệ phải từ 5 ký tự trở lên.");
      return;
    }

    let parsedPrice: number | null = null;
    if (editDealType === "SALE") {
      parsedPrice = parseInt(editPriceVnd.replace(/\D/g, ""), 10);
      if (isNaN(parsedPrice) || parsedPrice <= 0) {
        setEditError("Vui lòng nhập giá bán hợp lệ lớn hơn 0 VNĐ.");
        return;
      }
    }

    setEditSaving(true);
    try {
      const { error: updErr } = await supabaseBrowserClient
        .from("book_listings")
        .update({
          title: cleanTitle,
          subject: cleanSubject,
          condition: editCondition,
          deal_type: editDealType,
          price_vnd: editDealType === "SALE" ? parsedPrice : null,
          description: editDescription.trim(),
          contact: cleanContact,
          updated_at: new Date().toISOString(),
        })
        .eq("id", book.id);

      if (updErr) throw updErr;

      setBook((prev) =>
        prev
          ? {
              ...prev,
              title: cleanTitle,
              subject: cleanSubject,
              condition: editCondition,
              deal_type: editDealType,
              price_vnd: editDealType === "SALE" ? parsedPrice : null,
              description: editDescription.trim(),
              contact: cleanContact,
            }
          : null
      );
      setIsEditOpen(false);
    } catch (err: any) {
      setEditError(err.message || "Lỗi khi cập nhật thông tin sách.");
    } finally {
      setEditSaving(false);
    }
  };

  // Trích xuất số điện thoại sạch để gọi hoặc nhắn Zalo
  const cleanPhone = book?.contact?.replace(/[^0-9]/g, "") || "";
  const isPhoneValid = cleanPhone.length >= 10 && cleanPhone.length <= 11;

  return (
    <PortalLayout>
      <div className="max-w-4xl mx-auto space-y-6">
        {/* Navigation Breadcrumb */}
        <Link
          href="/books"
          className="inline-flex items-center gap-1.5 text-xs font-semibold text-slate-500 hover:text-slate-900 transition"
        >
          <ArrowLeft className="w-3.5 h-3.5" />
          <span>Quay lại Chợ sách giáo trình</span>
        </Link>

        {/* Loading State */}
        {loading && (
          <div className="bg-white rounded-xl p-12 text-center shadow-subtle space-y-3">
            <Loader2 className="w-6 h-6 animate-spin mx-auto text-slate-400" />
            <p className="text-xs text-slate-500 font-medium">Đang tải thông tin sách...</p>
          </div>
        )}

        {/* Error State */}
        {!loading && error && (
          <div className="bg-white rounded-xl p-10 text-center shadow-subtle space-y-3">
            <h2 className="text-base font-bold text-slate-900">{error}</h2>
            <Link
              href="/books"
              className="inline-block px-4 py-2 bg-slate-900 text-white rounded-lg text-xs font-semibold"
            >
              Về trang Chợ sách
            </Link>
          </div>
        )}

        {/* Content State */}
        {!loading && !error && book && (
          <div className="space-y-6">
            {/* Top Bar dành riêng cho Người đăng (Owner Banner) */}
            {isOwner && (
              <div className="bg-blue-50/70 rounded-xl p-4 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 text-xs">
                <div className="flex items-center gap-2 text-blue-900 font-semibold">
                  <span className="w-2 h-2 rounded-full bg-[#0F5FC2]" />
                  <span>Bạn là người đăng bài viết này.</span>
                  {book.status === "SOLD" ? (
                    <span className="px-2 py-0.5 rounded bg-red-100 text-red-700 font-bold uppercase text-[10px]">
                      Đã bán xong
                    </span>
                  ) : (
                    <span className="px-2 py-0.5 rounded bg-emerald-100 text-emerald-800 font-bold uppercase text-[10px]">
                      Đang hiển thị trên chợ
                    </span>
                  )}
                </div>

                <div className="flex items-center gap-2">
                  <button
                    onClick={handleToggleStatus}
                    disabled={actionBusy}
                    className="px-3 py-1.5 bg-white shadow-subtle hover:bg-slate-100 text-slate-800 rounded-lg font-semibold transition active:scale-[0.98]"
                  >
                    {book.status === "SOLD" ? "Mở lại (Còn sách)" : "Đánh dấu Đã bán"}
                  </button>

                  <button
                    onClick={() => setIsEditOpen(true)}
                    className="inline-flex items-center gap-1 px-3 py-1.5 bg-white shadow-subtle hover:bg-slate-100 text-slate-800 rounded-lg font-semibold transition"
                  >
                    <Edit3 className="w-3.5 h-3.5" />
                    <span>Sửa tin</span>
                  </button>

                  <button
                    onClick={handleDelete}
                    disabled={actionBusy}
                    title="Xóa bài đăng"
                    className="p-1.5 bg-white shadow-subtle hover:bg-red-50 text-slate-500 hover:text-red-600 rounded-lg transition"
                  >
                    <Trash2 className="w-4 h-4 stroke-[1.5]" />
                  </button>
                </div>
              </div>
            )}

            {/* Main Details Grid: 2 Columns (Image + Content) */}
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
              {/* Left Column: Book Image (5 cols) */}
              <div className="lg:col-span-5 bg-white rounded-xl p-4 shadow-subtle space-y-3">
                <div className="relative w-full aspect-[4/3] bg-[#F1F5F9] rounded-lg overflow-hidden flex items-center justify-center">
                  {imageUrl ? (
                    // eslint-disable-next-line @next/next/no-img-element
                    <img
                      src={imageUrl}
                      alt={book.title}
                      className="w-full h-full object-contain"
                    />
                  ) : (
                    <div className="flex flex-col items-center justify-center text-slate-400 gap-2">
                      <BookOpen className="w-12 h-12 stroke-[1.2]" />
                      <span className="text-xs font-medium">Người bán chưa đính kèm ảnh</span>
                    </div>
                  )}

                  {book.status === "SOLD" && (
                    <div className="absolute inset-0 bg-slate-900/50 backdrop-blur-[1px] flex items-center justify-center">
                      <span className="px-4 py-1.5 bg-red-600 text-white rounded text-sm font-bold uppercase tracking-wider shadow">
                        Đã giao dịch xong
                      </span>
                    </div>
                  )}
                </div>

                {/* Badges info row */}
                <div className="flex flex-wrap gap-2 pt-1">
                  <span
                    className={`px-2.5 py-1 rounded text-xs font-semibold ${
                      DEAL_TYPE_MAP[book.deal_type]?.badgeClass || "bg-slate-100"
                    }`}
                  >
                    {DEAL_TYPE_MAP[book.deal_type]?.label || book.deal_type}
                  </span>

                  <span
                    className={`px-2.5 py-1 rounded text-xs font-semibold ${
                      CONDITION_MAP[book.condition]?.badgeClass || "bg-slate-100"
                    }`}
                  >
                    {CONDITION_MAP[book.condition]?.label || book.condition}
                  </span>
                </div>
              </div>

              {/* Right Column: Book Info & Contact (7 cols) */}
              <div className="lg:col-span-7 space-y-5">
                {/* Book Header Card */}
                <div className="bg-white rounded-xl p-6 shadow-subtle space-y-3">
                  <div className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
                    {book.subject}
                  </div>

                  <h1 className="text-xl sm:text-2xl font-bold tracking-tight text-slate-900 leading-tight">
                    {book.title}
                  </h1>

                  {/* Price */}
                  <div className="pt-1 flex items-baseline gap-2">
                    {book.deal_type === "SALE" && book.price_vnd ? (
                      <>
                        <span className="text-2xl sm:text-3xl font-bold text-slate-900 tabular-nums">
                          {new Intl.NumberFormat("vi-VN").format(book.price_vnd)}
                        </span>
                        <span className="text-sm font-semibold text-slate-500">VNĐ</span>
                      </>
                    ) : (
                      <span className="text-lg font-bold text-purple-700">
                        Trao đổi sách giáo trình / Cho tặng
                      </span>
                    )}
                  </div>

                  {/* Condition Explainer */}
                  <div className="pt-2">
                    <p className="text-xs text-slate-600 bg-[#F1F5F9] rounded-lg p-3 leading-relaxed">
                      <strong>Tình trạng sách ({CONDITION_MAP[book.condition]?.label}):</strong>{" "}
                      {CONDITION_MAP[book.condition]?.desc}
                    </p>
                  </div>

                  {/* Description */}
                  {book.description && (
                    <div className="pt-3 border-t border-slate-100">
                      <h3 className="text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1.5">
                        Mô tả chi tiết từ người bán
                      </h3>
                      <p className="text-xs text-slate-700 whitespace-pre-wrap leading-relaxed">
                        {book.description}
                      </p>
                    </div>
                  )}

                  <div className="pt-2 text-[11px] text-slate-400 flex items-center gap-1.5">
                    <Calendar className="w-3.5 h-3.5" />
                    <span>
                      Đăng ngày {new Date(book.created_at).toLocaleDateString("vi-VN")}
                    </span>
                  </div>
                </div>

                {/* Contact Box (White Card) */}
                <div className="bg-white rounded-xl p-6 shadow-subtle space-y-4">
                  <div className="flex items-center justify-between">
                    <div>
                      <h3 className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
                        Thông tin người bán
                      </h3>
                      <div className="text-base font-bold text-slate-900 mt-0.5">
                        {book.seller_name}
                      </div>
                      {book.profiles?.student_id && (
                        <div className="text-xs text-slate-500">
                          MSSV: {book.profiles.student_id}
                        </div>
                      )}
                    </div>

                    <div className="w-10 h-10 rounded-full bg-[#F1F5F9] text-slate-700 flex items-center justify-center font-bold text-sm">
                      {book.seller_name.charAt(0).toUpperCase()}
                    </div>
                  </div>

                  {/* Contact Methods */}
                  <div className="bg-[#F8FAFC] rounded-lg p-3 space-y-2">
                    <div className="text-xs font-medium text-slate-500">
                      Số điện thoại / Zalo:
                    </div>
                    <div className="text-sm font-bold text-slate-900 tracking-wide font-mono">
                      {book.contact}
                    </div>
                  </div>

                  {/* Quick Action Buttons for Buyer */}
                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-2">
                    {/* Call Button */}
                    {isPhoneValid && (
                      <a
                        href={`tel:${cleanPhone}`}
                        className="inline-flex items-center justify-center gap-1.5 py-2 px-3 bg-slate-900 hover:bg-slate-800 text-white rounded-lg text-xs font-semibold transition active:scale-[0.98]"
                      >
                        <Phone className="w-3.5 h-3.5" />
                        <span>Gọi điện</span>
                      </a>
                    )}

                    {/* Zalo Button */}
                    {isPhoneValid && (
                      <a
                        href={`https://zalo.me/${cleanPhone}`}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="inline-flex items-center justify-center gap-1.5 py-2 px-3 bg-[#0F5FC2] hover:bg-[#0c4ea3] text-white rounded-lg text-xs font-semibold transition active:scale-[0.98]"
                      >
                        <MessageCircle className="w-3.5 h-3.5" />
                        <span>Mở Zalo</span>
                      </a>
                    )}

                    {/* Copy Button */}
                    <button
                      type="button"
                      onClick={handleCopyContact}
                      className="inline-flex items-center justify-center gap-1.5 py-2 px-3 bg-[#F1F5F9] hover:bg-slate-200 text-slate-700 rounded-lg text-xs font-semibold transition active:scale-[0.98]"
                    >
                      {copied ? (
                        <>
                          <Check className="w-3.5 h-3.5 text-emerald-600" />
                          <span className="text-emerald-700">Đã sao chép</span>
                        </>
                      ) : (
                        <>
                          <Copy className="w-3.5 h-3.5" />
                          <span>Sao chép SĐT</span>
                        </>
                      )}
                    </button>
                  </div>

                  {/* Safe Trading Notice for UTH Students */}
                  <div className="pt-2 border-t border-slate-100 flex items-start gap-2 text-[11px] text-slate-500 leading-relaxed">
                    <ShieldCheck className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
                    <span>
                      <strong>Mẹo an toàn:</strong> Nên hẹn gặp nhận sách trực tiếp tại sảnh các cơ sở
                      UTH (CS1 Võ Oanh, CS2 Thủ Đức, CS3 Tô Ký) và kiểm tra kỹ sách trước khi thanh toán.
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Modal Chỉnh Sửa Dành Cho Người Đăng (Owner Edit Modal) */}
        {isEditOpen && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-sm p-4">
            <div className="bg-white rounded-2xl p-6 shadow-2xl max-w-lg w-full space-y-4 max-h-[90vh] overflow-y-auto">
              <div className="flex items-center justify-between pb-3 border-b border-slate-100">
                <h3 className="text-sm font-bold text-slate-900">
                  Chỉnh sửa thông tin sách
                </h3>
                <button
                  onClick={() => setIsEditOpen(false)}
                  className="text-slate-400 hover:text-slate-600 p-1"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>

              {editError && (
                <div className="bg-red-50 text-red-800 rounded-lg p-2.5 text-xs">
                  {editError}
                </div>
              )}

              <form onSubmit={handleSaveEdit} className="space-y-4 text-xs">
                <div>
                  <label className="block font-semibold text-slate-700 mb-1">
                    Tên sách / Giáo trình
                  </label>
                  <input
                    type="text"
                    required
                    maxLength={160}
                    value={editTitle}
                    onChange={(e) => setEditTitle(e.target.value)}
                    className="w-full px-3 py-2 bg-[#F1F5F9] rounded-lg text-slate-900 focus:outline-none focus:ring-2 focus:ring-[#0F5FC2]"
                  />
                </div>

                <div>
                  <label className="block font-semibold text-slate-700 mb-1">
                    Môn học / Ngành
                  </label>
                  <input
                    type="text"
                    required
                    maxLength={100}
                    value={editSubject}
                    onChange={(e) => setEditSubject(e.target.value)}
                    className="w-full px-3 py-2 bg-[#F1F5F9] rounded-lg text-slate-900 focus:outline-none focus:ring-2 focus:ring-[#0F5FC2]"
                  />
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block font-semibold text-slate-700 mb-1">
                      Hình thức
                    </label>
                    <select
                      value={editDealType}
                      onChange={(e) => setEditDealType(e.target.value as any)}
                      className="w-full px-3 py-2 bg-[#F1F5F9] rounded-lg text-slate-900 focus:outline-none"
                    >
                      <option value="SALE">Bán lại</option>
                      <option value="EXCHANGE">Trao đổi sách</option>
                    </select>
                  </div>

                  <div>
                    <label className="block font-semibold text-slate-700 mb-1">
                      Tình trạng
                    </label>
                    <select
                      value={editCondition}
                      onChange={(e) => setEditCondition(e.target.value as any)}
                      className="w-full px-3 py-2 bg-[#F1F5F9] rounded-lg text-slate-900 focus:outline-none"
                    >
                      <option value="GOOD">Còn tốt</option>
                      <option value="NEW">Mới 100%</option>
                      <option value="USED">Đã dùng</option>
                    </select>
                  </div>
                </div>

                {editDealType === "SALE" && (
                  <div>
                    <label className="block font-semibold text-slate-700 mb-1">
                      Giá bán (VNĐ)
                    </label>
                    <input
                      type="number"
                      min={1000}
                      step={1000}
                      required
                      value={editPriceVnd}
                      onChange={(e) => setEditPriceVnd(e.target.value)}
                      className="w-full px-3 py-2 bg-[#F1F5F9] rounded-lg text-slate-900 tabular-nums focus:outline-none focus:ring-2 focus:ring-[#0F5FC2]"
                    />
                  </div>
                )}

                <div>
                  <label className="block font-semibold text-slate-700 mb-1">
                    Số điện thoại / Zalo liên hệ
                  </label>
                  <input
                    type="text"
                    required
                    value={editContact}
                    onChange={(e) => setEditContact(e.target.value)}
                    className="w-full px-3 py-2 bg-[#F1F5F9] rounded-lg text-slate-900 focus:outline-none focus:ring-2 focus:ring-[#0F5FC2]"
                  />
                </div>

                <div>
                  <label className="block font-semibold text-slate-700 mb-1">
                    Mô tả thêm
                  </label>
                  <textarea
                    rows={3}
                    maxLength={2000}
                    value={editDescription}
                    onChange={(e) => setEditDescription(e.target.value)}
                    className="w-full px-3 py-2 bg-[#F1F5F9] rounded-lg text-slate-900 focus:outline-none focus:ring-2 focus:ring-[#0F5FC2]"
                  />
                </div>

                <div className="pt-2 flex items-center justify-end gap-2 border-t border-slate-100">
                  <button
                    type="button"
                    onClick={() => setIsEditOpen(false)}
                    className="px-3 py-1.5 bg-[#F1F5F9] hover:bg-slate-200 text-slate-700 rounded-lg font-semibold transition"
                  >
                    Hủy
                  </button>
                  <button
                    type="submit"
                    disabled={editSaving}
                    className="px-4 py-1.5 bg-[#0F5FC2] hover:bg-[#0c4ea3] text-white rounded-lg font-semibold transition shadow-subtle active:scale-[0.98]"
                  >
                    {editSaving ? "Đang lưu..." : "Lưu thay đổi"}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}
      </div>
    </PortalLayout>
  );
}
