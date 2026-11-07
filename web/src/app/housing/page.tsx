"use client";

import React, { useEffect, useState } from "react";
import Link from "next/link";
import { PortalLayout } from "@/components/layout/PortalLayout";
import { supabaseBrowserClient } from "@/utils/supabase-client";
import { Home, Plus, MapPin, Phone, MessageSquare } from "lucide-react";

export default function HousingPostsPage() {
  const [posts, setPosts] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedCampus, setSelectedCampus] = useState<string>("ALL");

  const campuses = [
    { label: "Tất cả cơ sở", value: "ALL" },
    { label: "Cơ sở 1 (Bình Thạnh)", value: "CS1" },
    { label: "Cơ sở 2 (Quận 12)", value: "CS2" },
    { label: "Cơ sở 3 (Thủ Đức)", value: "CS3" },
  ];

  useEffect(() => {
    let isMounted = true;

    async function fetchPosts() {
      setLoading(true);
      let query = supabaseBrowserClient
        .from("housing_posts")
        .select("*, profiles:author_id(display_name, email)")
        .order("created_at", { ascending: false });

      if (selectedCampus !== "ALL") {
        query = query.eq("campus", selectedCampus);
      }

      const { data, error } = await query;
      if (!isMounted) return;

      if (!error && data) {
        setPosts(data);
      }
      setLoading(false);
    }

    fetchPosts();

    return () => {
      isMounted = false;
    };
  }, [selectedCampus]);

  return (
    <PortalLayout>
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
        <div>
          <h1 className="text-xl font-bold text-slate-900 tracking-tight">
            Ghép Trọ & Ký Túc Xá Sinh Viên UTH
          </h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Tìm bạn ở ghép, thuê phòng trọ uy tín gần các cơ sở đào tạo của trường
          </p>
        </div>

        <Link
          href="/housing/create"
          className="inline-flex items-center gap-1.5 px-3 py-2 bg-slate-900 text-white rounded-lg text-xs font-semibold hover:bg-slate-800 transition active:scale-[0.98]"
        >
          <Plus className="w-3.5 h-3.5" />
          <span>Đăng tin tìm trọ</span>
        </Link>
      </div>

      {/* Campus Filter */}
      <div className="flex flex-wrap gap-1.5 mb-6">
        {campuses.map((c) => (
          <button
            key={c.value}
            onClick={() => setSelectedCampus(c.value)}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium transition active:scale-[0.98] ${
              selectedCampus === c.value
                ? "bg-slate-900 text-white"
                : "bg-white text-slate-600 hover:bg-slate-100 shadow-subtle"
            }`}
          >
            {c.label}
          </button>
        ))}
      </div>

      {/* Posts Grid (No borders, 12px rounded cards, subtle shadow) */}
      {loading ? (
        <div className="py-12 text-center text-xs text-slate-400">
          Đang tải tin trọ sinh viên...
        </div>
      ) : posts.length === 0 ? (
        <div className="py-12 bg-white rounded-xl shadow-subtle text-center">
          <Home className="w-8 h-8 text-slate-300 stroke-[1.5] mx-auto mb-2" />
          <div className="text-sm font-semibold text-slate-800">
            Chưa có tin đăng nào ở cơ sở này
          </div>
          <p className="text-xs text-slate-400 mt-1">
            Hãy là người đầu tiên chia sẻ phòng trọ hoặc tìm bạn ở ghép!
          </p>
        </div>
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
                    {post.room_type === "roommate"
                      ? "Ở ghép"
                      : post.room_type === "dormitory"
                      ? "Ký túc xá"
                      : "Cho thuê"}
                  </span>
                  <span className="text-[10px] font-semibold px-2 py-0.5 rounded bg-slate-100 text-slate-700">
                    {post.campus || "CS1"}
                  </span>
                </div>

                <h3 className="text-sm font-bold text-slate-900 mb-1.5 line-clamp-2">
                  {post.title}
                </h3>

                <div className="text-base font-bold text-slate-900 mb-2 tabular-nums">
                  {new Intl.NumberFormat("vi-VN").format(post.price)} đ
                  <span className="text-xs font-normal text-slate-400">/tháng</span>
                </div>

                <p className="text-xs text-slate-500 line-clamp-3 mb-3 leading-relaxed">
                  {post.description || "Chưa có mô tả chi tiết."}
                </p>

                <div className="text-[11px] text-slate-400 flex items-center gap-1.5 mb-3">
                  <MapPin className="w-3.5 h-3.5 shrink-0" />
                  <span className="truncate">{post.location || "Gần trường"}</span>
                </div>
              </div>

              <div className="pt-3 border-t border-slate-100 flex items-center justify-between">
                <div className="text-xs font-medium text-slate-700 truncate max-w-[130px]">
                  {post.profiles?.display_name || "Sinh viên UTH"}
                </div>

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
                  {post.contact_zalo && (
                    <a
                      href={`https://zalo.me/${post.contact_zalo}`}
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
