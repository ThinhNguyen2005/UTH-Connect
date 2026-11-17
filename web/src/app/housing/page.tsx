"use client";

import React, { useEffect, useState } from "react";
import Link from "next/link";
import { MapPin, Phone, MessageSquare } from "lucide-react";
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

export default function HousingPostsPage() {
  const [posts, setPosts] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedCampus, setSelectedCampus] = useState<string>("ALL");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let isMounted = true;
    async function fetchPosts() {
      setLoading(true);
      setError(null);
      let query = supabaseBrowserClient.from("housing_posts")
        .select("*, profiles:author_id(display_name, email)")
        .eq("status", "OPEN")
        .order("created_at", { ascending: false });
      if (selectedCampus !== "ALL") {
        query = query.or(
          `campus.eq.${selectedCampus},campus.eq.${selectedCampus.split("_")[0]}`
        );
      }
      const { data, error } = await query;
      if (!isMounted) return;
      setPosts(error ? [] : data || []);
      if (error) setError("Không tải được tin trọ. Vui lòng tải lại trang.");
      setLoading(false);
    }
    fetchPosts();
    return () => { isMounted = false; };
  }, [selectedCampus]);


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
      {error ? <p role="alert" className="text-red-700">{error}</p> : loading ? (
        <p className="py-12 text-center text-slate-500">Đang tải tin trọ...</p>
      ) : posts.length === 0 ? (
        <p className="py-12 text-center text-slate-500">Chưa có tin đã duyệt ở cơ sở này.</p>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {posts.map((post) => (
            <div
              key={post.id}
              className="bg-white rounded-xl p-5 shadow-subtle flex flex-col justify-between hover:bg-[#F8FAFC]/50 transition"
            >
              <div>
                <div className="flex items-center justify-between gap-2 mb-2.5">
                  <span className="text-[10px] font-semibold px-2 py-0.5 rounded bg-slate-100 text-slate-700">
                    {post.type === "LOOKING_FOR_ROOMMATE" || post.room_type === "roommate"
                      ? "Ở ghép"
                      : post.type === "ROOM_FOR_RENT" || post.room_type === "dormitory"
                      ? "Cho thuê"
                      : "Tìm phòng"}
                  </span>
                  <span className="text-[10px] font-semibold px-2 py-0.5 rounded bg-slate-100 text-slate-700 uppercase">
                    {post.campus ? post.campus.split("_")[0] : "CS1"}
                  </span>
                </div>

                <h3 className="text-sm font-bold text-slate-900 mb-1.5 line-clamp-2">
                  {post.title}
                </h3>

                <div className="text-base font-bold text-slate-900 mb-2 tabular-nums">
                  {new Intl.NumberFormat("vi-VN").format(post.monthly_rent_vnd || post.price || 0)} đ
                  <span className="text-xs font-normal text-slate-400">/tháng</span>
                </div>

                <p className="text-xs text-slate-500 line-clamp-3 mb-3 leading-relaxed">
                  {post.description || "Chưa có mô tả chi tiết."}
                </p>

                <div className="text-[11px] text-slate-400 flex items-center gap-1.5 mb-3">
                  <MapPin className="w-3.5 h-3.5 shrink-0" />
                  <span className="truncate">{post.address_label || post.location || "Gần trường UTH"}</span>
                </div>
              </div>

              <div className="pt-3 border-t border-slate-100 flex items-center justify-between">
                <Link
                  href={`/housing/${post.id}`}
                  className="text-xs font-medium text-slate-700 hover:text-slate-900 hover:underline underline-offset-2 transition"
                >
                  Xem chi tiết →
                </Link>

                <div className="flex items-center gap-1">
                  {post.contact_phone && (
                    <a
                      href={`tel:${post.contact_phone}`}
                      className="p-1.5 text-slate-500 hover:text-slate-900 rounded-md transition"
                      title="Gọi điện"
                    >
                      <Phone className="w-3.5 h-3.5 stroke-[1.5]" />
                    </a>
                  )}
                  {(post.contact_zalo_url || post.contact_zalo) && (
                    <a
                      href={
                        post.contact_zalo_url
                          ? post.contact_zalo_url
                          : `https://zalo.me/${post.contact_zalo}`
                      }
                      target="_blank"
                      rel="noopener noreferrer"
                      className="p-1.5 text-slate-500 hover:text-slate-900 rounded-md transition"
                      title="Nhắn Zalo"
                    >
                      <MessageSquare className="w-3.5 h-3.5 stroke-[1.5]" />
                    </a>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </PortalLayout>
  );
}
