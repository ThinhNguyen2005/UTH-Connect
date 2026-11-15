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

export default function HousingPostsPage() {
  const [posts, setPosts] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedCampus, setSelectedCampus] = useState("ALL");
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
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-5 items-start">
          {posts.map(post => (
            <article key={post.id} className="bg-white rounded-xl p-5 shadow-subtle min-w-0">
              <p className="text-xs text-slate-500">{types[post.type]} · {campuses.find(c => c.value === post.campus)?.label}</p>
              <h2 className="text-base font-bold text-slate-900 mt-2 break-words">{post.title}</h2>
              <p className="text-lg font-bold text-slate-900 mt-2 tabular-nums">
                {new Intl.NumberFormat("vi-VN").format(post.monthly_rent_vnd)} đ <span className="text-sm font-normal">/tháng</span>
              </p>
              <p className="text-sm text-slate-600 mt-2 break-words">{post.address_label}</p>
              <Link href={`/housing/${post.id}`} className="inline-block mt-4 px-3 py-2 rounded-lg bg-slate-100 text-sm font-medium hover:bg-slate-200">Xem chi tiết</Link>
            </article>
          ))}
        </div>
      )}
    </PortalLayout>
  );
}
