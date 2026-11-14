"use client";

import React, { useEffect, useState } from "react";
import Link from "next/link";
import { PortalLayout } from "@/components/layout/PortalLayout";
import { supabaseBrowserClient } from "@/utils/supabase-client";

const campuses = [
  { label: "Tất cả cơ sở", value: "ALL" },
  { label: "CS1 Võ Oanh", value: "CS1_QUANG_TRUNG" },
  { label: "CS2 Thủ Đức", value: "CS2_THU_DUC" },
  { label: "CS3 Tô Ký", value: "CS3_TO_KY" },
];
const types: Record<string, string> = {
  ROOM_FOR_RENT: "Phòng cho thuê", LOOKING_FOR_ROOMMATE: "Tìm người ở ghép", LOOKING_FOR_ROOM: "Tìm phòng",
};
const amenities: Record<string, string> = {
  MEZZANINE: "Gác lửng", AIR_CONDITIONER: "Điều hòa", MACHINE_WASH: "Máy giặt",
  FREE_HOURS: "Giờ giấc tự do", GOVERNMENT_UTILITY_RATES: "Điện nước giá nhà nước",
  SECURED_PARKING: "Chỗ để xe an toàn", COOKING: "Được nấu ăn", HIGH_SPEED_WIFI: "Wi-Fi tốc độ cao",
  HOT_WATER: "Nước nóng", BALCONY: "Ban công", KEY_CARD: "Thẻ ra vào",
};
const genders: Record<string, string> = { ANY: "Bất kỳ", MALE: "Nam", FEMALE: "Nữ" };

export default function HousingPostsPage() {
  const [posts, setPosts] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedCampus, setSelectedCampus] = useState("ALL");
  const [error, setError] = useState<string | null>(null);
  const [contactNotice, setContactNotice] = useState("");

  useEffect(() => {
    let isMounted = true;
    async function fetchPosts() {
      setLoading(true);
      setError(null);
      let query = supabaseBrowserClient.from("housing_posts")
        .select("*, profiles:author_id(display_name, email)")
        .eq("status", "OPEN")
        .order("created_at", { ascending: false });
      if (selectedCampus !== "ALL") query = query.eq("campus", selectedCampus);
      const { data, error } = await query;
      if (!isMounted) return;
      setPosts(error ? [] : data || []);
      if (error) setError("Không tải được tin trọ. Vui lòng tải lại trang.");
      setLoading(false);
    }
    fetchPosts();
    return () => { isMounted = false; };
  }, [selectedCampus]);

  async function copyPhone(phone: string) {
    try {
      await navigator.clipboard.writeText(phone);
      setContactNotice("Đã sao chép số điện thoại.");
    } catch {
      setContactNotice("Không thể sao chép tự động. Bạn có thể chọn số điện thoại để sao chép.");
    }
  }

  return (
    <PortalLayout>
      <div className="flex flex-wrap items-center justify-between gap-4 mb-6">
        <div>
          <h1 className="text-xl font-bold text-slate-900">Ghép Trọ & Ký Túc Xá Sinh Viên UTH</h1>
          <p className="text-sm text-slate-500 mt-1">Tìm phòng trọ và bạn ở ghép gần các cơ sở UTH. Chỉ hiển thị tin đã được duyệt.</p>
        </div>
        <Link href="/housing/create" className="px-3 py-2 bg-slate-900 text-white rounded-lg text-sm">Đăng tin tìm trọ</Link>
      </div>
      <div className="flex flex-wrap gap-2 mb-6">
        {campuses.map(c => (
          <button key={c.value} onClick={() => setSelectedCampus(c.value)} aria-pressed={selectedCampus === c.value}
            className={`px-3 py-2 rounded-lg text-sm ${selectedCampus === c.value ? "bg-slate-900 text-white" : "bg-white text-slate-600 shadow-subtle"}`}>
            {c.label}
          </button>
        ))}
      </div>
      <p role="status" className="text-sm text-slate-600 mb-3">{contactNotice}</p>
      {error ? <p role="alert" className="text-red-700">{error}</p> : loading ? (
        <p className="py-12 text-center text-slate-500">Đang tải tin trọ...</p>
      ) : posts.length === 0 ? (
        <p className="py-12 text-center text-slate-500">Chưa có tin đã duyệt ở cơ sở này.</p>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-5 items-start">
          {posts.map(post => (
            <article key={post.id} className="bg-white rounded-xl p-5 shadow-subtle min-w-0">
              <p className="text-xs text-slate-500">{types[post.type]} · {campuses.find(c => c.value === post.campus)?.label}</p>
              <h2 className="text-base font-bold text-slate-900 mt-2 break-words">{post.title}</h2>
              <p className="text-lg font-bold text-slate-900 mt-2 tabular-nums">
                {new Intl.NumberFormat("vi-VN").format(post.monthly_rent_vnd)} đ <span className="text-sm font-normal">/tháng</span>
              </p>
              <p className="text-sm text-slate-600 mt-2 break-words">{post.address_label}</p>
              <details className="mt-4 text-sm text-slate-700">
                <summary className="cursor-pointer font-semibold py-2">Xem chi tiết</summary>
                <div className="space-y-4 mt-3">
                  <div className="space-y-2">
                    {(post.images || []).map((image: string, index: number) => (
                      // eslint-disable-next-line @next/next/no-img-element
                      <img key={`${image}-${index}`} src={image} alt={`Ảnh phòng ${index + 1}: ${post.title}`} loading="lazy" className="w-full rounded-lg object-contain" />
                    ))}
                    {!post.images?.length && <p>Chưa có ảnh phòng.</p>}
                  </div>
                  <p className="whitespace-pre-wrap break-words">{post.description || "Chưa có mô tả."}</p>
                  <dl className="grid grid-cols-2 gap-2">
                    <dt>Tiền cọc</dt><dd>{new Intl.NumberFormat("vi-VN").format(post.deposit_vnd)} đ</dd>
                    <dt>Diện tích</dt><dd>{post.area_m2 == null ? "Chưa cung cấp" : `${post.area_m2} m²`}</dd>
                    <dt>Số chỗ còn trống</dt><dd>{post.available_slots}</dd>
                    <dt>Giới tính</dt><dd>{genders[post.gender_preference]}</dd>
                    <dt>Điện nước</dt><dd>{post.utilities_included ? "Đã gồm trong giá" : "Chưa gồm trong giá"}</dd>
                  </dl>
                  <a className="inline-block underline" target="_blank" rel="noopener noreferrer"
                    href={`https://www.google.com/maps/dir/?api=1&destination=${encodeURIComponent(post.latitude || post.longitude ? `${post.latitude},${post.longitude}` : post.address_label)}`}>Xem đường đi trên bản đồ</a>
                  <p>Giờ giấc: {post.schedule_note || "Chưa cung cấp"}</p>
                  <p>Tiện ích: {(post.amenities || []).map((item: string) => amenities[item] || item).join(", ") || "Chưa cung cấp"}</p>
                  <div className="bg-slate-50 rounded-lg p-3 space-y-2">
                    <p>Người đăng: {post.host_name || post.profiles?.display_name}</p>
                    <p>MSSV: {post.host_student_id} · {post.verified ? "Đã xác minh sinh viên" : "Chưa xác minh sinh viên"}</p>
                    <p className="select-text">Điện thoại: {post.contact_phone}</p>
                    <button className="px-3 py-2 rounded-lg bg-slate-100" onClick={() => copyPhone(post.contact_phone)}>Sao chép số</button>
                    {post.contact_zalo_enabled !== false && /^https:\/\/zalo\.me\/[\w+/?=&%.-]+$/.test(post.contact_zalo_url || "") && (
                      <a className="inline-block px-3 py-2 underline" href={post.contact_zalo_url} target="_blank" rel="noopener noreferrer">Nhắn Zalo</a>
                    )}
                  </div>
                </div>
              </details>
            </article>
          ))}
        </div>
      )}
    </PortalLayout>
  );
}
