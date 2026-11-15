"use client";

import React, { useEffect, useState } from "react";
import Link from "next/link";
import { PortalLayout } from "@/components/layout/PortalLayout";
import { supabaseBrowserClient } from "@/utils/supabase-client";

const amenities: Record<string, string> = {
  MEZZANINE: "Gác lửng", AIR_CONDITIONER: "Điều hòa", MACHINE_WASH: "Máy giặt",
  FREE_HOURS: "Giờ giấc tự do", GOVERNMENT_UTILITY_RATES: "Điện nước giá nhà nước",
  SECURED_PARKING: "Chỗ để xe an toàn", COOKING: "Được nấu ăn", HIGH_SPEED_WIFI: "Wi-Fi tốc độ cao",
  HOT_WATER: "Nước nóng", BALCONY: "Ban công", KEY_CARD: "Thẻ ra vào",
};
const genders: Record<string, string> = { ANY: "Bất kỳ", MALE: "Nam", FEMALE: "Nữ" };
const campuses: Record<string, string> = { CS1_QUANG_TRUNG: "CS1 Võ Oanh", CS2_THU_DUC: "CS2 Thủ Đức", CS3_TO_KY: "CS3 Tô Ký" };
const types: Record<string, string> = { ROOM_FOR_RENT: "Phòng cho thuê", LOOKING_FOR_ROOMMATE: "Tìm người ở ghép", LOOKING_FOR_ROOM: "Tìm phòng" };

export default function HousingDetailPage({ params }: { params: { id: string } }) {
  const [post, setPost] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [imageIndex, setImageIndex] = useState(0);
  const [notice, setNotice] = useState("");

  useEffect(() => {
    let active = true;
    setLoading(true);
    setPost(null);
    setError("");
    setImageIndex(0);
    supabaseBrowserClient.from("housing_posts").select("*").eq("id", params.id).eq("status", "OPEN").maybeSingle()
      .then(({ data, error }) => {
        if (!active) return;
        setPost(error ? null : data);
        setError(error ? "Không tải được tin trọ. Vui lòng thử lại." : "");
        setLoading(false);
      });
    return () => { active = false; };
  }, [params.id]);

  async function copyPhone() {
    try {
      await navigator.clipboard.writeText(post.contact_phone);
      setNotice("Đã sao chép số điện thoại.");
    } catch {
      setNotice("Không thể sao chép tự động. Bạn có thể chọn số điện thoại để sao chép.");
    }
  }

  return (
    <PortalLayout>
      <Link href="/housing" className="inline-block text-sm text-slate-600 mb-6 hover:underline">← Quay lại danh sách trọ</Link>
      {loading ? <p role="status">Đang tải tin trọ...</p> : error ? <p role="alert">{error}</p> : !post ? (
        <div className="rounded-xl bg-white p-8"><h1 className="text-xl font-semibold">Tin không còn hiển thị</h1><p className="text-slate-500 mt-2">Tin đã bị ẩn, chưa được duyệt hoặc đã được xóa.</p></div>
      ) : (
        <div className="grid lg:grid-cols-[minmax(0,1fr)_320px] gap-8 items-start">
          <div className="min-w-0 space-y-6">
            <div className="bg-slate-100 rounded-xl overflow-hidden">
              {post.images?.length ? (
                // eslint-disable-next-line @next/next/no-img-element
                <img src={post.images[imageIndex]} alt={`Ảnh phòng ${imageIndex + 1}: ${post.title}`} className="w-full aspect-[16/10] object-contain max-h-[480px]" />
              ) : <div className="aspect-[16/10] flex items-center justify-center text-slate-500">Chưa có ảnh phòng</div>}
            </div>
            {post.images?.length > 1 && <div className="flex gap-3 overflow-x-auto pb-2">
              {post.images.map((image: string, index: number) => (
                <button key={image} onClick={() => setImageIndex(index)} aria-label={`Xem ảnh ${index + 1}`} aria-pressed={imageIndex === index} className={`shrink-0 rounded-lg p-1 ${imageIndex === index ? "bg-slate-900" : "bg-slate-100"}`}>
                  {/* eslint-disable-next-line @next/next/no-img-element */}
                  <img src={image} alt="" className="w-20 h-16 object-cover rounded" />
                </button>
              ))}
            </div>}
            <header>
              <p className="text-sm text-slate-500">{types[post.type]} · {campuses[post.campus]}</p>
              <h1 className="text-2xl font-bold text-slate-900 mt-2 break-words">{post.title}</h1>
              <p className="text-slate-600 mt-3 break-words">{post.address_label}</p>
              <a className="inline-block text-sm underline mt-2" target="_blank" rel="noopener noreferrer" href={`https://www.google.com/maps/dir/?api=1&destination=${encodeURIComponent(post.latitude || post.longitude ? `${post.latitude},${post.longitude}` : post.address_label)}`}>Xem đường đi trên bản đồ</a>
            </header>
            <section className="bg-white p-6 rounded-xl space-y-4">
              <h2 className="font-semibold text-lg">Thông tin phòng</h2>
              <dl className="grid grid-cols-2 sm:grid-cols-3 gap-5 text-sm">
                <div><dt className="text-slate-500">Diện tích</dt><dd className="font-medium mt-1">{post.area_m2 == null ? "Chưa cung cấp" : `${post.area_m2} m²`}</dd></div>
                <div><dt className="text-slate-500">Số chỗ còn trống</dt><dd className="font-medium mt-1">{post.available_slots}</dd></div>
                <div><dt className="text-slate-500">Giới tính</dt><dd className="font-medium mt-1">{genders[post.gender_preference]}</dd></div>
                <div><dt className="text-slate-500">Tiền cọc</dt><dd className="font-medium mt-1 tabular-nums">{new Intl.NumberFormat("vi-VN").format(post.deposit_vnd)} đ</dd></div>
                <div><dt className="text-slate-500">Điện nước</dt><dd className="font-medium mt-1">{post.utilities_included ? "Đã gồm trong giá" : "Chưa gồm trong giá"}</dd></div>
                <div><dt className="text-slate-500">Giờ giấc</dt><dd className="font-medium mt-1 break-words">{post.schedule_note || "Chưa cung cấp"}</dd></div>
              </dl>
            </section>
            <section className="bg-white p-6 rounded-xl"><h2 className="font-semibold text-lg mb-3">Mô tả</h2><p className="whitespace-pre-wrap break-words text-slate-600 leading-relaxed">{post.description || "Chưa có mô tả."}</p></section>
            <section className="bg-white p-6 rounded-xl"><h2 className="font-semibold text-lg mb-3">Tiện ích</h2><div className="flex flex-wrap gap-2">{post.amenities?.length ? post.amenities.map((item: string) => <span key={item} className="bg-slate-50 rounded-lg px-3 py-2 text-sm">{amenities[item] || item}</span>) : <p className="text-slate-500">Chưa cung cấp</p>}</div></section>
          </div>
          <aside className="bg-white rounded-xl p-6 lg:sticky lg:top-6 space-y-5">
            <div><p className="text-sm text-slate-500">Giá thuê mỗi tháng</p><p className="text-2xl font-bold mt-1 tabular-nums">{new Intl.NumberFormat("vi-VN").format(post.monthly_rent_vnd)} đ</p></div>
            <div className="bg-slate-50 rounded-lg p-4 text-sm space-y-2 break-words"><p className="font-semibold">{post.host_name}</p><p>MSSV: {post.host_student_id}</p><p className="text-slate-500">{post.verified ? "Đã xác minh sinh viên" : "Chưa xác minh sinh viên"}</p></div>
            <p className="select-text text-lg font-semibold">{post.contact_phone}</p>
            <button onClick={copyPhone} className="w-full rounded-lg bg-slate-900 text-white py-3 text-sm font-medium">Sao chép số điện thoại</button>
            {post.contact_zalo_enabled !== false && /^https:\/\/zalo\.me\/[\w+/?=&%.-]+$/.test(post.contact_zalo_url || "") && <a className="block text-center rounded-lg bg-slate-100 py-3 text-sm" href={post.contact_zalo_url} target="_blank" rel="noopener noreferrer">Nhắn Zalo</a>}
            <p role="status" className="text-sm text-slate-500">{notice}</p>
          </aside>
        </div>
      )}
    </PortalLayout>
  );
}
