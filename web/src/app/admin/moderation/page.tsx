"use client";

import React, { useEffect, useState } from "react";
import { AdminLayout } from "@/components/layout/AdminLayout";
import { supabaseBrowserClient } from "@/utils/supabase-client";
import { RefreshCw, CheckCircle, XCircle } from "lucide-react";

const STATUS_LABEL: Record<string, string> = {
  PENDING: "Chờ duyệt",
  OPEN: "Đang hiện",
  REJECTED: "Bị từ chối",
  FULFILLED: "Hết phòng",
  HIDDEN_BY_REPORTS: "Bị ẩn",
};

const STATUS_CLASS: Record<string, string> = {
  PENDING: "bg-amber-50 text-amber-700",
  OPEN: "bg-emerald-50 text-emerald-700",
  REJECTED: "bg-red-50 text-red-700",
  FULFILLED: "bg-slate-100 text-slate-600",
  HIDDEN_BY_REPORTS: "bg-slate-100 text-slate-600",
};

const CAMPUS_LABEL: Record<string, string> = {
  CS1_QUANG_TRUNG: "CS1 Võ Oanh",
  CS2_THU_DUC: "CS2 Thủ Đức",
  CS3_TO_KY: "CS3 Tô Ký",
};

const TYPE_LABEL: Record<string, string> = {
  ROOM_FOR_RENT: "Phòng cho thuê",
  LOOKING_FOR_ROOMMATE: "Tìm người ở ghép",
  LOOKING_FOR_ROOM: "Tìm phòng",
};

const AMENITY_LABEL: Record<string, string> = {
  MEZZANINE: "Gác lửng", AIR_CONDITIONER: "Điều hòa", MACHINE_WASH: "Máy giặt",
  FREE_HOURS: "Giờ giấc tự do", GOVERNMENT_UTILITY_RATES: "Điện nước giá nhà nước",
  SECURED_PARKING: "Chỗ để xe an toàn", COOKING: "Được nấu ăn", HIGH_SPEED_WIFI: "Wi-Fi tốc độ cao",
  HOT_WATER: "Nước nóng", BALCONY: "Ban công", KEY_CARD: "Thẻ ra vào",
};
const GENDER_LABEL: Record<string, string> = { ANY: "Bất kỳ", MALE: "Nam", FEMALE: "Nữ" };

export default function AdminModerationPage() {
  const [posts, setPosts] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [toast, setToast] = useState<string | null>(null);
  const [actionLoading, setActionLoading] = useState<string | null>(null);
  const [tab, setTab] = useState<"pending" | "all">("pending");
  const [rejectTarget, setRejectTarget] = useState<any | null>(null);
  const [rejectReason, setRejectReason] = useState("");

  const fetchPosts = async () => {
    setLoading(true);
    const { data, error } = await supabaseBrowserClient
      .from("housing_posts")
      .select("*, profiles:author_id(display_name, email)")
      .order("created_at", { ascending: false });

    if (error) {
      setPosts([]);
      setToast(`Không tải được tin trọ: ${error.message}`);
    } else {
      setPosts(data || []);
    }
    setLoading(false);
  };

  useEffect(() => {
    let isMounted = true;
    const fetchInitial = async () => {
      setLoading(true);
      const { data, error } = await supabaseBrowserClient
        .from("housing_posts")
        .select("*, profiles:author_id(display_name, email)")
        .order("created_at", { ascending: false });

      if (isMounted) {
        if (error) {
          setToast(`Không tải được tin trọ: ${error.message}`);
        } else {
          setPosts(data || []);
        }
        setLoading(false);
      }
    };
    fetchInitial();
    return () => {
      isMounted = false;
    };
  }, []);

  const handleApprove = async (postId: string) => {
    setActionLoading(postId);
    setToast(null);

    const { error } = await supabaseBrowserClient.rpc("approve_housing_listing", {
      p_listing_id: postId,
    });

    setActionLoading(null);

    if (error) {
      setToast(`Lỗi: ${error.message}`);
    } else {
      setToast("Đã duyệt tin. Tin đã hiển thị trên bảng feed.");
      fetchPosts();
    }
  };

  const openRejectDialog = (post: any) => {
    setRejectTarget(post);
    setRejectReason("");
  };

  const handleReject = async () => {
    if (!rejectTarget) return;
    const reason = rejectReason.trim();
    if (!reason) {
      setToast("Vui lòng nhập lý do từ chối.");
      return;
    }

    setActionLoading(rejectTarget.id);
    setToast(null);

    const { error } = await supabaseBrowserClient.rpc("reject_housing_listing", {
      p_listing_id: rejectTarget.id,
      p_reason: reason,
    });

    setActionLoading(null);

    if (error) {
      setToast(`Lỗi: ${error.message}`);
    } else {
      setRejectTarget(null);
      setRejectReason("");
      setToast("Đã từ chối tin và gửi lý do cho chủ tin.");
      fetchPosts();
    }
  };

  const pendingCount = posts.filter((p) => p.status === "PENDING").length;
  const visiblePosts = tab === "pending" ? posts.filter((p) => p.status === "PENDING") : posts;

  return (
    <AdminLayout>
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
        <div>
          <h1 className="text-xl font-bold text-slate-900 tracking-tight">
            Kiểm Duyệt Tin Ghép Trọ & KTX
          </h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Tin mới đăng ở trạng thái chờ duyệt. Duyệt để tin hiển thị trên feed, hoặc từ chối kèm lý do.
          </p>
        </div>

        <button
          onClick={fetchPosts}
          className="inline-flex items-center gap-1.5 px-3 py-2 bg-white text-slate-700 text-xs font-medium rounded-lg shadow-subtle hover:bg-slate-50 transition active:scale-[0.98]"
        >
          <RefreshCw className="w-3.5 h-3.5 stroke-[1.5]" />
          <span>Làm mới</span>
        </button>
      </div>

      {toast && (
        <div className="mb-4 p-3 bg-blue-50 text-slate-800 text-xs rounded-lg font-medium">
          {toast}
        </div>
      )}

      <div className="flex items-center gap-2 mb-4">
        <button
          onClick={() => setTab("pending")}
          className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition ${
            tab === "pending"
              ? "bg-slate-900 text-white"
              : "bg-white text-slate-600 shadow-subtle hover:bg-slate-50"
          }`}
        >
          Chờ duyệt ({pendingCount})
        </button>
        <button
          onClick={() => setTab("all")}
          className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition ${
            tab === "all"
              ? "bg-slate-900 text-white"
              : "bg-white text-slate-600 shadow-subtle hover:bg-slate-50"
          }`}
        >
          Tất cả ({posts.length})
        </button>
      </div>

      <div className="bg-white rounded-xl shadow-subtle overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-[#F8FAFC] text-slate-400 font-semibold uppercase tracking-wider">
              <tr>
                <th className="py-3 px-5">Ảnh</th>
                <th className="py-3 px-5">Tiêu đề & Tác giả</th>
                <th className="py-3 px-5">Cơ sở & Loại</th>
                <th className="py-3 px-5">Giá (VNĐ/tháng)</th>
                <th className="py-3 px-5">Trạng thái</th>
                <th className="py-3 px-5">Ngày đăng</th>
                <th className="py-3 px-5 text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {loading ? (
                <tr>
                  <td colSpan={7} className="py-8 text-center text-slate-400">
                    Đang tải danh sách tin...
                  </td>
                </tr>
              ) : visiblePosts.length === 0 ? (
                <tr>
                  <td colSpan={7} className="py-8 text-center text-slate-400">
                    {tab === "pending" ? "Không có tin nào chờ duyệt." : "Chưa có tin nào."}
                  </td>
                </tr>
              ) : (
                visiblePosts.map((p) => (
                  <tr key={p.id} className="hover:bg-[#F8FAFC]/80 transition align-top">
                    <td className="py-3.5 px-5">
                      {p.images?.[0] ? (
                        // eslint-disable-next-line @next/next/no-img-element
                        <img
                          src={p.images[0]}
                          alt={p.title}
                          className="w-12 h-12 rounded-lg object-cover bg-slate-100"
                        />
                      ) : (
                        <div className="w-12 h-12 rounded-lg bg-slate-100" />
                      )}
                    </td>

                    <td className="py-3.5 px-5">
                      <div className="font-semibold text-slate-900 line-clamp-2 max-w-xs">
                        {p.title}
                      </div>
                      <div className="text-[11px] text-slate-400 mt-0.5">
                        {p.profiles?.display_name || "Sinh viên"} ({p.profiles?.email})
                      </div>
                      <details className="mt-3 text-sm text-slate-700 min-w-64 max-w-xl">
                        <summary className="cursor-pointer font-semibold py-2">Xem chi tiết</summary>
                        <div className="space-y-3 mt-2 break-words">
                          {(p.images || []).map((image: string, index: number) => (
                            // eslint-disable-next-line @next/next/no-img-element
                            <img key={`${image}-${index}`} src={image} alt={`Ảnh phòng ${index + 1}: ${p.title}`} loading="lazy" className="w-full rounded-lg object-contain" />
                          ))}
                          {!p.images?.length && <p>Chưa có ảnh phòng.</p>}
                          <p className="whitespace-pre-wrap">{p.description || "Chưa có mô tả."}</p>
                          <p>Địa chỉ: {p.address_label}</p>
                          <a className="inline-block underline" target="_blank" rel="noopener noreferrer"
                            href={`https://www.google.com/maps/dir/?api=1&destination=${encodeURIComponent(p.latitude || p.longitude ? `${p.latitude},${p.longitude}` : p.address_label)}`}>Xem vị trí trên bản đồ</a>
                          <p>Tiền cọc: {new Intl.NumberFormat("vi-VN").format(p.deposit_vnd)} đ</p>
                          <p>Diện tích: {p.area_m2 == null ? "Chưa cung cấp" : `${p.area_m2} m²`}</p>
                          <p>Số chỗ còn trống: {p.available_slots}</p>
                          <p>Giới tính: {GENDER_LABEL[p.gender_preference]}</p>
                          <p>Điện nước: {p.utilities_included ? "Đã gồm trong giá" : "Chưa gồm trong giá"}</p>
                          <p>Giờ giấc: {p.schedule_note || "Chưa cung cấp"}</p>
                          <p>Tiện ích: {(p.amenities || []).map((item: string) => AMENITY_LABEL[item] || item).join(", ") || "Chưa cung cấp"}</p>
                          <p>Người đăng: {p.host_name} · MSSV: {p.host_student_id}</p>
                          <p>{p.verified ? "Đã xác minh sinh viên" : "Chưa xác minh sinh viên"}</p>
                          <p className="select-text">Điện thoại: {p.contact_phone}</p>
                          {p.contact_zalo_enabled !== false && /^https:\/\/zalo\.me\/[\w+/?=&%.-]+$/.test(p.contact_zalo_url || "") && (
                            <a href={p.contact_zalo_url} target="_blank" rel="noopener noreferrer" className="inline-block underline">Nhắn Zalo</a>
                          )}
                        </div>
                      </details>
                      {p.status === "REJECTED" && p.rejection_reason && (
                        <div className="text-[11px] text-red-600 mt-1">
                          Lý do: {p.rejection_reason}
                        </div>
                      )}
                    </td>

                    <td className="py-3.5 px-5">
                      <div className="font-semibold text-slate-700">
                        {CAMPUS_LABEL[p.campus] || p.campus}
                      </div>
                      <div className="text-slate-400 mt-0.5">
                        {TYPE_LABEL[p.type] || p.type}
                      </div>
                    </td>

                    <td className="py-3.5 px-5 font-bold text-slate-900 tabular-nums">
                      {new Intl.NumberFormat("vi-VN").format(p.monthly_rent_vnd)} đ
                    </td>

                    <td className="py-3.5 px-5">
                      <span
                        className={`px-2 py-0.5 rounded text-[10px] font-semibold ${
                          STATUS_CLASS[p.status] || "bg-slate-100 text-slate-600"
                        }`}
                      >
                        {STATUS_LABEL[p.status] || p.status}
                      </span>
                    </td>

                    <td className="py-3.5 px-5 text-slate-500 whitespace-nowrap">
                      {new Date(p.created_at).toLocaleString("vi-VN")}
                    </td>

                    <td className="py-3.5 px-5 text-right space-x-1 whitespace-nowrap">
                      {p.status === "PENDING" && (
                        <>
                          <button
                            onClick={() => handleApprove(p.id)}
                            disabled={actionLoading === p.id}
                            className="inline-flex items-center gap-1 px-2 py-1 bg-emerald-50 text-emerald-700 rounded text-[11px] font-semibold hover:bg-emerald-100 transition disabled:opacity-50"
                          >
                            <CheckCircle className="w-3 h-3" />
                            Duyệt
                          </button>
                          <button
                            onClick={() => openRejectDialog(p)}
                            disabled={actionLoading === p.id}
                            className="inline-flex items-center gap-1 px-2 py-1 bg-red-50 text-red-700 rounded text-[11px] font-semibold hover:bg-red-100 transition disabled:opacity-50"
                          >
                            <XCircle className="w-3 h-3" />
                            Từ chối
                          </button>
                        </>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
      {rejectTarget && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
          <div className="bg-white rounded-xl shadow-xl w-full max-w-md p-5">
            <h2 className="text-sm font-bold text-slate-900">Từ chối tin</h2>
            <p className="text-xs text-slate-500 mt-1 line-clamp-2">{rejectTarget.title}</p>
            <textarea
              value={rejectReason}
              onChange={(e) => setRejectReason(e.target.value)}
              rows={3}
              placeholder="Nhập lý do từ chối (bắt buộc). Chủ tin sẽ thấy lý do này."
              className="mt-3 w-full px-3 py-2 border border-slate-200 rounded-lg text-xs focus:outline-none focus:ring-2 focus:ring-slate-300"
            />
            <div className="flex justify-end gap-2 mt-4">
              <button
                onClick={() => {
                  setRejectTarget(null);
                  setRejectReason("");
                }}
                className="px-3 py-1.5 bg-white text-slate-600 text-xs font-semibold rounded-lg shadow-subtle hover:bg-slate-50 transition"
              >
                Hủy
              </button>
              <button
                onClick={handleReject}
                disabled={actionLoading === rejectTarget.id}
                className="px-3 py-1.5 bg-red-600 text-white text-xs font-semibold rounded-lg hover:bg-red-700 transition disabled:opacity-50"
              >
                Xác nhận từ chối
              </button>
            </div>
          </div>
        </div>
      )}
    </AdminLayout>
  );
}
