"use client";

import React, { useEffect, useRef, useState } from "react";
import { AdminLayout } from "@/components/layout/AdminLayout";
import { supabaseBrowserClient } from "@/utils/supabase-client";

const STATUS_LABEL: Record<string, string> = { PENDING: "Chờ duyệt", OPEN: "Đang hiện", REJECTED: "Bị từ chối", FULFILLED: "Hết phòng", HIDDEN_BY_REPORTS: "Bị ẩn" };
const CAMPUS_LABEL: Record<string, string> = { CS1_QUANG_TRUNG: "CS1 Võ Oanh", CS2_THU_DUC: "CS2 Thủ Đức", CS3_TO_KY: "CS3 Tô Ký" };
const TYPE_LABEL: Record<string, string> = { ROOM_FOR_RENT: "Phòng cho thuê", LOOKING_FOR_ROOMMATE: "Tìm người ở ghép", LOOKING_FOR_ROOM: "Tìm phòng" };
const AMENITY_LABEL: Record<string, string> = {
  MEZZANINE: "Gác lửng", AIR_CONDITIONER: "Điều hòa", MACHINE_WASH: "Máy giặt",
  FREE_HOURS: "Giờ giấc tự do", GOVERNMENT_UTILITY_RATES: "Điện nước giá nhà nước",
  SECURED_PARKING: "Chỗ để xe an toàn", COOKING: "Được nấu ăn", HIGH_SPEED_WIFI: "Wi-Fi tốc độ cao",
  HOT_WATER: "Nước nóng", BALCONY: "Ban công", KEY_CARD: "Thẻ ra vào",
};
const GENDER_LABEL: Record<string, string> = { ANY: "Bất kỳ", MALE: "Nam", FEMALE: "Nữ" };
type Action = "approve" | "reject" | "hide" | "delete";
type Report = { reason: string; created_at: string };

export default function AdminModerationPage() {
  const [posts, setPosts] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [toast, setToast] = useState("");
  const [selected, setSelected] = useState<any>(null);
  const [reports, setReports] = useState<Report[]>([]);
  const [reportError, setReportError] = useState("");
  const [reportsLoading, setReportsLoading] = useState(false);
  const [busy, setBusy] = useState(false);
  const [reason, setReason] = useState("");
  const [intent, setIntent] = useState<Action | null>(null);
  const [tab, setTab] = useState("pending");
  const [imageIndex, setImageIndex] = useState(0);
  const drawer = useRef<HTMLDialogElement>(null);
  const mutationRunning = useRef(false);

  async function fetchPosts() {
    setLoading(true);
    try {
      const { data, error } = await supabaseBrowserClient.from("housing_posts")
        .select("*, profiles:author_id(display_name, email)").order("created_at", { ascending: false });
      if (error) throw error;
      setPosts(data || []);
    } catch {
      setPosts([]);
      setToast("Không tải được tin trọ. Vui lòng thử làm mới.");
    } finally { setLoading(false); }
  }

  useEffect(() => { void fetchPosts(); }, []);

  useEffect(() => {
    if (!selected) return;
    drawer.current?.showModal();
    let active = true;
    setReports([]);
    setReportError("");
    setReportsLoading(true);
    supabaseBrowserClient.rpc("get_housing_reports", { p_listing_id: selected.id })
      .then(({ data, error }) => {
        if (!active) return;
        if (error) setReportError("Không tải được lý do báo cáo. Vui lòng đóng và mở lại chi tiết.");
        else setReports(data || []);
        setReportsLoading(false);
      });
    return () => { active = false; };
  }, [selected]);

  function openPost(post: any) {
    setSelected(post);
    setIntent(null);
    setReason("");
    setToast("");
    setImageIndex(0);
  }

  function closePost() {
    if (mutationRunning.current) return;
    drawer.current?.close();
    setSelected(null);
    setIntent(null);
  }

  async function act(action: Action) {
    if (!selected || mutationRunning.current) return;
    if (action === "reject" && !reason.trim()) { setToast("Vui lòng nhập lý do từ chối."); return; }
    mutationRunning.current = true;
    setBusy(true);
    setToast("");
    try {
      const args = action === "reject" ? { p_listing_id: selected.id, p_reason: reason.trim() } : { p_listing_id: selected.id };
      const { error } = await supabaseBrowserClient.rpc(`${action}_housing_listing`, args);
      if (error) throw error;
      setSelected(null);
      setIntent(null);
      drawer.current?.close();
      setToast(action === "delete" ? "Đã xóa tin và các báo cáo liên quan." : action === "hide" ? "Đã ẩn tin khỏi bảng tin sinh viên." : action === "approve" ? "Đã duyệt tin." : "Đã từ chối tin.");
      await fetchPosts();
    } catch {
      setToast("Không thực hiện được thao tác. Hãy làm mới để kiểm tra trạng thái tin trước khi thử lại.");
    } finally { mutationRunning.current = false; setBusy(false); }
  }

  const visible = posts.filter(p => tab === "pending" ? p.status === "PENDING" : tab === "reported" ? p.report_count > 0 : true);
  const p = selected;
  return (
    <AdminLayout>
      <div className="flex flex-wrap items-center justify-between gap-4 mb-6">
        <div><h1 className="text-xl font-bold">Kiểm duyệt tin trọ</h1><p className="text-sm text-slate-500 mt-1">Xem nội dung, kiểm tra báo cáo và quyết định hiển thị tin.</p></div>
        <button onClick={fetchPosts} disabled={loading} className="bg-white px-4 py-2 rounded-lg text-sm shadow-subtle">Làm mới</button>
      </div>
      {!selected && toast && <p role="status" className="bg-blue-50 p-3 rounded-lg text-sm mb-4">{toast}</p>}
      <div className="flex flex-wrap gap-2 mb-5">
        {[["pending", `Chờ duyệt (${posts.filter(p => p.status === "PENDING").length})`], ["reported", `Có báo cáo (${posts.filter(p => p.report_count > 0).length})`], ["all", `Tất cả (${posts.length})`]].map(([value, label]) => (
          <button key={value} onClick={() => setTab(value)} aria-pressed={tab === value} className={`px-4 py-2 rounded-lg text-sm ${tab === value ? "bg-slate-900 text-white" : "bg-white text-slate-600"}`}>{label}</button>
        ))}
      </div>
      <div className="bg-white rounded-xl overflow-x-auto shadow-subtle">
        <table className="w-full text-left text-sm">
          <thead className="bg-slate-50 text-slate-500"><tr><th className="p-4">Tin đăng</th><th className="p-4">Giá / tháng</th><th className="p-4">Trạng thái</th><th className="p-4">Báo cáo</th><th className="p-4"><span className="sr-only">Thao tác</span></th></tr></thead>
          <tbody>{loading ? <tr><td colSpan={5} className="p-8 text-center">Đang tải danh sách tin...</td></tr> : !visible.length ? <tr><td colSpan={5} className="p-8 text-center text-slate-500">Không có tin nào trong mục này.</td></tr> : visible.map(post => (
            <tr key={post.id} className="hover:bg-slate-50 align-top">
              <td className="p-4"><button onClick={() => openPost(post)} className="text-left font-semibold hover:underline max-w-sm break-words">{post.title}</button><p className="text-xs text-slate-500 mt-1">{post.profiles?.display_name || post.host_name} · {CAMPUS_LABEL[post.campus]}</p></td>
              <td className="p-4 tabular-nums whitespace-nowrap">{new Intl.NumberFormat("vi-VN").format(post.monthly_rent_vnd)} đ</td>
              <td className="p-4"><span className={`inline-block px-2 py-1 rounded-lg text-xs whitespace-nowrap ${post.status === "PENDING" ? "bg-amber-50 text-amber-700" : post.status === "OPEN" ? "bg-emerald-50 text-emerald-700" : "bg-slate-100 text-slate-600"}`}>{STATUS_LABEL[post.status]}</span></td>
              <td className="p-4"><button onClick={() => openPost(post)} className={post.report_count ? "font-semibold text-red-700 underline" : "text-slate-400"}>{post.report_count || 0}</button></td>
              <td className="p-4"><button onClick={() => openPost(post)} className="rounded-lg bg-slate-100 px-3 py-2 whitespace-nowrap text-xs font-medium">Xem chi tiết</button></td>
            </tr>
          ))}</tbody>
        </table>
      </div>
      {p && <dialog ref={drawer} aria-labelledby="housing-review-title" onCancel={event => { event.preventDefault(); closePost(); }} className="fixed inset-y-0 left-auto right-0 m-0 h-dvh max-h-none w-full max-w-2xl p-0 bg-slate-50 backdrop:bg-slate-900/40">
        <div className="flex flex-col h-full">
          <header className="bg-white px-6 py-4 flex items-start justify-between gap-4 shrink-0"><div><p className="text-xs text-slate-500 mb-1">{STATUS_LABEL[p.status]} · {TYPE_LABEL[p.type]}</p><h2 id="housing-review-title" className="font-semibold text-lg break-words">{p.title}</h2></div><button autoFocus onClick={closePost} disabled={busy} aria-label="Đóng chi tiết" className="rounded-lg bg-slate-100 px-3 py-2">Đóng</button></header>
          <div className="overflow-y-auto flex-1 min-h-0 p-6 space-y-5">
            {toast && <p role="alert" className="bg-red-50 text-red-700 rounded-lg p-3 text-sm">{toast}</p>}
            <section className="space-y-3">
              {p.images?.length ? <>
                {/* eslint-disable-next-line @next/next/no-img-element */}
                <img src={p.images[imageIndex]} alt={`Ảnh phòng ${imageIndex + 1}`} className="w-full h-64 object-contain rounded-xl bg-slate-100" />
                {p.images.length > 1 && <div className="flex gap-2 overflow-x-auto">{p.images.map((image: string, index: number) => <button key={image} onClick={() => setImageIndex(index)} aria-pressed={imageIndex === index} aria-label={`Xem ảnh ${index + 1}`} className={`shrink-0 rounded-lg p-1 ${imageIndex === index ? "bg-slate-900" : "bg-white"}`}>
                  {/* eslint-disable-next-line @next/next/no-img-element */}
                  <img src={image} alt="" className="w-16 h-12 object-cover rounded" />
                </button>)}</div>}
              </> : <p className="text-sm text-slate-500 bg-white p-5 rounded-xl">Chưa có ảnh phòng.</p>}
            </section>
            <section className="bg-white rounded-xl p-5 space-y-3 text-sm"><h3 className="font-semibold">Thông tin phòng</h3><p className="text-xl font-bold tabular-nums">{new Intl.NumberFormat("vi-VN").format(p.monthly_rent_vnd)} đ / tháng</p><p className="break-words">{p.address_label} · {CAMPUS_LABEL[p.campus]}</p><a className="inline-block underline" target="_blank" rel="noopener noreferrer" href={`https://www.google.com/maps/dir/?api=1&destination=${encodeURIComponent(p.latitude || p.longitude ? `${p.latitude},${p.longitude}` : p.address_label)}`}>Xem vị trí trên bản đồ</a>
              <dl className="grid grid-cols-2 gap-4 pt-2">
                <div><dt className="text-slate-500">Tiền cọc</dt><dd className="mt-1 tabular-nums">{new Intl.NumberFormat("vi-VN").format(p.deposit_vnd)} đ</dd></div>
                <div><dt className="text-slate-500">Diện tích</dt><dd className="mt-1">{p.area_m2 == null ? "Chưa cung cấp" : `${p.area_m2} m²`}</dd></div>
                <div><dt className="text-slate-500">Chỗ còn trống</dt><dd className="mt-1">{p.available_slots}</dd></div>
                <div><dt className="text-slate-500">Giới tính</dt><dd className="mt-1">{GENDER_LABEL[p.gender_preference]}</dd></div>
                <div><dt className="text-slate-500">Điện nước</dt><dd className="mt-1">{p.utilities_included ? "Đã gồm trong giá" : "Chưa gồm trong giá"}</dd></div>
                <div><dt className="text-slate-500">Giờ giấc</dt><dd className="mt-1 break-words">{p.schedule_note || "Chưa cung cấp"}</dd></div>
              </dl>
            </section>
            <section className="bg-white rounded-xl p-5 text-sm"><h3 className="font-semibold mb-3">Mô tả & tiện ích</h3><p className="whitespace-pre-wrap break-words leading-relaxed text-slate-600">{p.description || "Chưa có mô tả."}</p><div className="flex flex-wrap gap-2 mt-4">{(p.amenities || []).map((item: string) => <span key={item} className="bg-slate-50 rounded-lg px-3 py-2">{AMENITY_LABEL[item] || item}</span>)}</div></section>
            <section className="bg-white rounded-xl p-5 text-sm space-y-2 break-words"><h3 className="font-semibold mb-3">Người đăng & liên hệ</h3><p>{p.host_name} · MSSV: {p.host_student_id}</p><p className="text-slate-500">{p.verified ? "Đã xác minh sinh viên" : "Chưa xác minh sinh viên"}</p><p className="select-text">Điện thoại: {p.contact_phone}</p><p>{p.profiles?.email}</p><p className="text-slate-500">Đăng lúc {new Date(p.created_at).toLocaleString("vi-VN")}</p>{p.contact_zalo_enabled !== false && /^https:\/\/zalo\.me\/[\w+/?=&%.-]+$/.test(p.contact_zalo_url || "") && <a href={p.contact_zalo_url} target="_blank" rel="noopener noreferrer" className="inline-block underline">Nhắn Zalo</a>}</section>
            <section className="bg-white rounded-xl p-5 text-sm"><h3 className="font-semibold mb-2">Báo cáo ({p.report_count || 0})</h3><p className="text-slate-500 mb-3">Tin tự ẩn khi có báo cáo từ 3 người khác nhau.</p>{reportsLoading ? <p role="status">Đang tải báo cáo...</p> : reportError ? <p role="alert" className="text-red-700">{reportError}</p> : reports.length ? <ul className="space-y-3">{reports.map((r, index) => <li key={index} className="rounded-lg bg-red-50 p-3"><p className="whitespace-pre-wrap break-words">{r.reason}</p><p className="text-xs text-slate-500 mt-1">{new Date(r.created_at).toLocaleString("vi-VN")}</p></li>)}</ul> : <p className="text-slate-500">Chưa có lý do báo cáo được ghi nhận.</p>}</section>
            {p.rejection_reason && <p className="bg-red-50 p-4 rounded-xl text-sm break-words">Lý do từ chối: {p.rejection_reason}</p>}
          </div>
          <footer className="bg-white p-5 shrink-0 space-y-3">
            {intent ? <div className="space-y-3"><p className="text-sm font-semibold">{intent === "delete" ? "Xóa vĩnh viễn tin và toàn bộ báo cáo? Không thể hoàn tác." : intent === "hide" ? "Ẩn tin khỏi bảng tin? Nội dung và báo cáo vẫn được giữ để kiểm tra." : "Nhập lý do từ chối để chủ tin có thể chỉnh sửa."}</p>
              {intent === "delete" && <p className="text-xs text-slate-500">Tệp ảnh trong kho lưu trữ chưa bị xóa bởi thao tác này.</p>}
              {intent === "reject" && <textarea aria-label="Lý do từ chối" value={reason} onChange={e => setReason(e.target.value)} rows={2} disabled={busy} className="w-full bg-slate-50 rounded-lg p-3 text-sm" />}
              <div className="flex justify-end gap-2"><button disabled={busy} onClick={() => setIntent(null)} className="px-4 py-2 rounded-lg bg-slate-100 text-sm">Hủy</button><button disabled={busy || (intent === "reject" && !reason.trim())} onClick={() => act(intent)} className="px-4 py-2 rounded-lg bg-red-700 text-white text-sm disabled:opacity-50">{busy ? "Đang xử lý..." : "Xác nhận"}</button></div>
            </div> : <div className="flex flex-wrap gap-2">
              {p.status === "PENDING" && <><button disabled={busy} onClick={() => act("approve")} className="rounded-lg bg-slate-900 text-white px-4 py-2 text-sm">Duyệt tin</button><button disabled={busy} onClick={() => setIntent("reject")} className="rounded-lg bg-slate-100 px-4 py-2 text-sm">Từ chối</button></>}
              {p.status !== "HIDDEN_BY_REPORTS" && <button disabled={busy} onClick={() => setIntent("hide")} className="rounded-lg bg-amber-50 text-amber-800 px-4 py-2 text-sm">Ẩn tin</button>}
              <button disabled={busy} onClick={() => setIntent("delete")} className="rounded-lg bg-red-50 text-red-700 px-4 py-2 text-sm ml-auto">Xóa tin</button>
            </div>}
          </footer>
        </div>
      </dialog>}
    </AdminLayout>
  );
}
