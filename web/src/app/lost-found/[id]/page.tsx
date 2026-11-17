"use client";

import React, { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import Image from "next/image";
import { useGetIdentity } from "@refinedev/core";
import { PortalLayout } from "@/components/layout/PortalLayout";
import { supabaseBrowserClient } from "@/utils/supabase-client";
import {
  ArrowLeft,
  MapPin,
  Calendar,
  Phone,
  Tag,
  CheckCircle2,
  Trash2,
  PackageSearch,
  AlertTriangle,
  Share2,
  Lock,
  LogIn,
  ShieldCheck,
} from "lucide-react";
import type { AppUserIdentity } from "@/providers/auth-provider";
import type { LostFoundItem } from "../page";

export default function LostFoundDetailPage() {
  const params = useParams();
  const router = useRouter();
  const { data: user, isLoading: authLoading } = useGetIdentity<AppUserIdentity>();

  const [item, setItem] = useState<LostFoundItem | null>(null);
  const [loading, setLoading] = useState(true);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [actionLoading, setActionLoading] = useState(false);
  const [copied, setCopied] = useState(false);

  const itemId = params?.id as string;

  useEffect(() => {
    if (!itemId || !user) return;
    let isMounted = true;

    async function loadItem() {
      setLoading(true);
      const { data, error } = await supabaseBrowserClient
        .from("lost_found_items")
        .select("*")
        .eq("id", itemId)
        .single();

      if (!isMounted) return;

      if (error || !data) {
        setErrorMsg("Không tìm thấy thông tin món đồ hoặc tin đăng đã bị gỡ.");
      } else {
        setItem(data as LostFoundItem);
      }
      setLoading(false);
    }

    loadItem();

    return () => {
      isMounted = false;
    };
  }, [itemId, user]);

  const handleToggleResolved = async () => {
    if (!item || !user) return;
    setActionLoading(true);

    const nextState = !item.is_resolved;
    const { data, error } = await supabaseBrowserClient
      .from("lost_found_items")
      .update({ is_resolved: nextState, updated_at: new Date().toISOString() })
      .eq("id", item.id)
      .select();

    setActionLoading(false);

    if (error) {
      alert(`Lỗi khi cập nhật trạng thái: ${error.message}`);
    } else {
      setItem((prev) => (prev ? { ...prev, is_resolved: nextState } : null));
    }
  };

  const handleDeleteItem = async () => {
    if (!item || !user) return;
    const confirmed = window.confirm("Bạn có chắc chắn muốn xóa tin đăng này không?");
    if (!confirmed) return;

    setActionLoading(true);

    const { error } = await supabaseBrowserClient
      .from("lost_found_items")
      .delete()
      .eq("id", item.id);

    setActionLoading(false);

    if (error) {
      alert(`Lỗi khi xóa tin: ${error.message}`);
    } else {
      router.push("/lost-found");
    }
  };

  const handleCopyLink = () => {
    navigator.clipboard.writeText(window.location.href);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const isOwner = user?.id === item?.user_id;
  const isOrganizer = user?.role === "organizer";
  const isAdmin = user?.role === "admin";
  const canManage = isOwner || isAdmin || isOrganizer;

  const getCategoryLabel = (cat: string) => {
    switch (cat) {
      case "STUDENT_CARD":
        return "Thẻ sinh viên / Thẻ xe";
      case "ELECTRONICS":
        return "Thiết bị điện tử";
      case "WALLET_PAPERS":
        return "Ví tiền & Giấy tờ";
      case "PERSONAL_ITEMS":
        return "Đồ dùng cá nhân";
      case "OTHER":
        return "Khác";
      default:
        return cat;
    }
  };

  return (
    <PortalLayout>
      <div className="max-w-3xl mx-auto">
        <div className="flex items-center justify-between mb-4">
          <Link
            href="/lost-found"
            className="inline-flex items-center gap-1.5 text-xs font-semibold text-slate-500 hover:text-slate-900 transition"
          >
            <ArrowLeft className="w-3.5 h-3.5" />
            <span>Quay lại danh sách Đồ thất lạc</span>
          </Link>

          {user && (
            <button
              onClick={handleCopyLink}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-white text-slate-600 hover:text-slate-900 text-xs font-medium rounded-lg shadow-subtle transition"
            >
              <Share2 className="w-3.5 h-3.5" />
              <span>{copied ? "Đã sao chép link!" : "Chia sẻ link"}</span>
            </button>
          )}
        </div>

        {/* 1. Auth Loading */}
        {authLoading && (
          <div className="py-20 text-center">
            <div className="w-8 h-8 border-2 border-slate-900 border-t-transparent rounded-full animate-spin mx-auto mb-3" />
            <div className="text-xs text-slate-500 font-medium">
              Đang xác thực tài khoản UTH...
            </div>
          </div>
        )}

        {/* 2. Chưa đăng nhập: Auth Wall */}
        {!authLoading && !user && (
          <div className="bg-white rounded-2xl p-8 sm:p-10 shadow-subtle border border-slate-100 text-center my-6">
            <div className="w-14 h-14 rounded-2xl bg-slate-900 text-white flex items-center justify-center mx-auto mb-5 shadow-sm">
              <Lock className="w-7 h-7 stroke-[1.75]" />
            </div>
            <h2 className="text-base font-bold text-slate-900 tracking-tight">
              Yêu Cầu Đăng Nhập Để Xem Chi Tiết
            </h2>
            <p className="text-xs text-slate-500 mt-2 leading-relaxed max-w-md mx-auto">
              Thông tin liên hệ (số điện thoại, họ tên người mất/người nhặt) được bảo mật nội bộ trong trường UTH. Vui lòng đăng nhập để tiếp tục.
            </p>
            <div className="mt-6">
              <Link
                href="/login"
                className="inline-flex items-center gap-2 px-6 py-2.5 bg-slate-900 text-white rounded-xl text-xs font-semibold hover:bg-slate-800 transition active:scale-[0.98] shadow-sm"
              >
                <LogIn className="w-4 h-4" />
                <span>Đăng nhập tài khoản UTH</span>
              </Link>
            </div>
          </div>
        )}

        {/* 3. Đã đăng nhập */}
        {!authLoading && user && (
          <>
            {loading ? (
              <div className="py-20 text-center text-xs text-slate-400">
                Đang tải thông tin món đồ...
              </div>
            ) : errorMsg || !item ? (
              <div className="py-16 bg-white rounded-xl shadow-subtle text-center p-6">
                <PackageSearch className="w-10 h-10 text-slate-300 mx-auto mb-2" />
                <div className="text-sm font-semibold text-slate-800">{errorMsg}</div>
                <div className="mt-4">
                  <Link
                    href="/lost-found"
                    className="inline-flex items-center gap-1.5 px-3.5 py-2 bg-slate-900 text-white rounded-lg text-xs font-semibold"
                  >
                    <span>Về danh sách chính</span>
                  </Link>
                </div>
              </div>
            ) : (
              <div className="bg-white rounded-xl shadow-subtle overflow-hidden">
                {/* Large Image Header */}
                {item.image_url && (
                  <div className="relative w-full h-80 bg-slate-900/5 overflow-hidden">
                    <Image
                      src={item.image_url}
                      alt={item.title}
                      fill
                      sizes="(max-width: 1024px) 100vw, 800px"
                      className="object-contain bg-slate-950/20"
                      unoptimized
                    />
                  </div>
                )}

                <div className="p-6 sm:p-8">
                  {/* Badges Bar */}
                  <div className="flex flex-wrap items-center gap-2 mb-3">
                    <span
                      className={`text-[11px] font-bold px-3 py-1 rounded-full uppercase tracking-wider ${
                        item.kind === "LOST"
                          ? "bg-rose-500 text-white shadow-sm"
                          : "bg-emerald-600 text-white shadow-sm"
                      }`}
                    >
                      {item.kind === "LOST" ? "Cần tìm đồ rơi (Lost)" : "Nhặt được đồ (Found)"}
                    </span>

                    <span
                      className={`text-[11px] font-semibold px-3 py-1 rounded-full ${
                        item.is_resolved
                          ? "bg-slate-100 text-slate-600"
                          : "bg-amber-50 text-amber-700"
                      }`}
                    >
                      {item.is_resolved
                        ? "✓ Đã trao trả / Đã tìm thấy"
                        : "• Đang chờ chủ nhân nhận lại"}
                    </span>

                    <span className="text-[11px] font-medium px-2.5 py-1 rounded-lg bg-slate-100 text-slate-700 flex items-center gap-1">
                      <Tag className="w-3 h-3 text-slate-400" />
                      <span>{getCategoryLabel(item.category)}</span>
                    </span>
                  </div>

                  {/* Title */}
                  <h1 className="text-xl sm:text-2xl font-bold text-slate-900 tracking-tight mb-4">
                    {item.title}
                  </h1>

                  {/* Meta Grid */}
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 p-4 bg-[#F8FAFC] rounded-xl mb-6">
                    <div className="flex items-center gap-2.5 text-xs text-slate-600">
                      <div className="p-2 rounded-lg bg-white shadow-subtle text-slate-700 shrink-0">
                        <MapPin className="w-4 h-4" />
                      </div>
                      <div>
                        <div className="text-[10px] uppercase font-semibold text-slate-400">
                          Địa điểm rơi / nhặt
                        </div>
                        <div className="font-semibold text-slate-800">{item.location}</div>
                      </div>
                    </div>

                    <div className="flex items-center gap-2.5 text-xs text-slate-600">
                      <div className="p-2 rounded-lg bg-white shadow-subtle text-slate-700 shrink-0">
                        <Calendar className="w-4 h-4" />
                      </div>
                      <div>
                        <div className="text-[10px] uppercase font-semibold text-slate-400">
                          Thời gian ghi nhận
                        </div>
                        <div className="font-semibold text-slate-800">{item.date}</div>
                      </div>
                    </div>
                  </div>

                  {/* Detailed Description */}
                  <div className="mb-6">
                    <h2 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-2">
                      Mô tả chi tiết đặc điểm
                    </h2>
                    <div className="text-sm text-slate-700 leading-relaxed bg-white border border-slate-100 rounded-xl p-4 whitespace-pre-line">
                      {item.description || "Người đăng không bổ sung thêm mô tả chi tiết."}
                    </div>
                  </div>

                  {/* Contact Box */}
                  <div className="p-5 rounded-xl border border-slate-200/80 bg-slate-50/50 mb-6 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                    <div>
                      <div className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                        Thông tin liên hệ nhận lại đồ
                      </div>
                      <div className="text-sm font-bold text-slate-900 mt-0.5">
                        {item.contact_name || "Thành viên UTH"}
                      </div>
                      <div className="text-xs text-slate-500 mt-0.5">
                        Số điện thoại: <span className="font-semibold text-slate-800">{item.contact_phone}</span>
                      </div>
                    </div>

                    {item.contact_phone && (
                      <a
                        href={`tel:${item.contact_phone}`}
                        className="inline-flex items-center justify-center gap-2 px-4 py-2.5 bg-slate-900 text-white rounded-lg text-xs font-semibold hover:bg-slate-800 transition active:scale-[0.98] shadow-sm"
                      >
                        <Phone className="w-3.5 h-3.5" />
                        <span>Gọi điện trực tiếp</span>
                      </a>
                    )}
                  </div>

                  {/* Safety Advisory Banner */}
                  <div className="p-3.5 bg-amber-50/80 rounded-xl border border-amber-200/60 mb-6 flex items-start gap-2.5 text-xs text-amber-800">
                    <AlertTriangle className="w-4 h-4 text-amber-600 shrink-0 mt-0.5" />
                    <div className="leading-relaxed">
                      <span className="font-bold">Khuyến cáo an toàn trao nhận:</span> Sinh viên vui lòng hẹn gặp và bàn giao đồ tại các địa điểm công cộng trong trường (Văn phòng Đoàn - Hội, Phòng Quản lý Sinh viên, hoặc phòng Bảo vệ tại các cơ sở). Vui lòng đối chiếu thẻ sinh viên hoặc CCCD trước khi giao trả.
                    </div>
                  </div>

                  {/* Management Controls for Owner, Organizer or Admin */}
                  {canManage && (
                    <div className="pt-6 border-t border-slate-100 flex flex-wrap items-center justify-between gap-3">
                      <div className="text-xs text-slate-500">
                        {isOwner ? "Bạn là người tạo bài đăng này." : "Bạn có quyền kiểm duyệt / quản lý bài đăng này."}
                      </div>

                      <div className="flex items-center gap-2">
                        <button
                          onClick={handleToggleResolved}
                          disabled={actionLoading}
                          className={`inline-flex items-center gap-1.5 px-3.5 py-2 rounded-lg text-xs font-semibold transition active:scale-[0.98] ${
                            item.is_resolved
                              ? "bg-slate-100 text-slate-700 hover:bg-slate-200"
                              : "bg-emerald-600 text-white hover:bg-emerald-700 shadow-sm"
                          }`}
                        >
                          <CheckCircle2 className="w-3.5 h-3.5" />
                          <span>
                            {item.is_resolved
                              ? "Mở lại tin (chưa nhận)"
                              : "Xác nhận đã trao trả / Đã tìm thấy"}
                          </span>
                        </button>

                        <button
                          onClick={handleDeleteItem}
                          disabled={actionLoading}
                          className="inline-flex items-center gap-1.5 px-3 py-2 rounded-lg text-xs font-semibold bg-rose-50 text-rose-700 hover:bg-rose-100 transition"
                          title="Xóa bài đăng này"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                          <span>Xóa tin</span>
                        </button>
                      </div>
                    </div>
                  )}
                </div>
              </div>
            )}
          </>
        )}
      </div>
    </PortalLayout>
  );
}
