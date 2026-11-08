"use client";

import React, { useEffect, useState } from "react";
import { AdminLayout } from "@/components/layout/AdminLayout";
import { supabaseBrowserClient } from "@/utils/supabase-client";
import { Home, RefreshCw, Eye, EyeOff, CheckCircle } from "lucide-react";

export default function AdminModerationPage() {
  const [posts, setPosts] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [toast, setToast] = useState<string | null>(null);

  const fetchPosts = async () => {
    setLoading(true);
    const { data, error } = await supabaseBrowserClient
      .from("housing_posts")
      .select("*, profiles:author_id(display_name, email)")
      .order("created_at", { ascending: false });

    if (!error && data) {
      setPosts(data);
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
        if (!error && data) {
          setPosts(data);
        }
        setLoading(false);
      }
    };
    fetchInitial();
    return () => {
      isMounted = false;
    };
  }, []);

  const handleUpdateStatus = async (postId: string, newStatus: string) => {
    setToast(null);
    const { error } = await supabaseBrowserClient
      .from("housing_posts")
      .update({ status: newStatus })
      .eq("id", postId);

    if (error) {
      setToast(`Lỗi: ${error.message}`);
    } else {
      setToast(`Đã chuyển trạng thái bài đăng sang "${newStatus}".`);
      fetchPosts();
    }
  };

  return (
    <AdminLayout>
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
        <div>
          <h1 className="text-xl font-bold text-slate-900 tracking-tight">
            Kiểm Duyệt Bài Đăng Ghép Trọ & KTX
          </h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Rà soát tin đăng của sinh viên, ẩn hoặc khóa các tin vi phạm quy định
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

      <div className="bg-white rounded-xl shadow-subtle overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-[#F8FAFC] text-slate-400 font-semibold uppercase tracking-wider">
              <tr>
                <th className="py-3 px-5">Tiêu đề & Tác giả</th>
                <th className="py-3 px-5">Cơ sở & Loại phòng</th>
                <th className="py-3 px-5">Giá (VNĐ)</th>
                <th className="py-3 px-5">Trạng thái</th>
                <th className="py-3 px-5 text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {loading ? (
                <tr>
                  <td colSpan={5} className="py-8 text-center text-slate-400">
                    Đang tải danh sách bài đăng...
                  </td>
                </tr>
              ) : posts.length === 0 ? (
                <tr>
                  <td colSpan={5} className="py-8 text-center text-slate-400">
                    Chưa có bài đăng nào.
                  </td>
                </tr>
              ) : (
                posts.map((p) => (
                  <tr key={p.id} className="hover:bg-[#F8FAFC]/80 transition">
                    <td className="py-3.5 px-5">
                      <div className="font-semibold text-slate-900 line-clamp-1">
                        {p.title}
                      </div>
                      <div className="text-[11px] text-slate-400 mt-0.5">
                        {p.profiles?.display_name || "Sinh viên"} ({p.profiles?.email})
                      </div>
                    </td>

                    <td className="py-3.5 px-5">
                      <span className="font-semibold text-slate-700 mr-2">
                        {p.campus}
                      </span>
                      <span className="text-slate-400 capitalize">{p.room_type}</span>
                    </td>

                    <td className="py-3.5 px-5 font-bold text-slate-900 tabular-nums">
                      {new Intl.NumberFormat("vi-VN").format(p.price)} đ
                    </td>

                    <td className="py-3.5 px-5">
                      <span
                        className={`px-2 py-0.5 rounded text-[10px] font-semibold ${
                          p.status === "ACTIVE"
                            ? "bg-emerald-50 text-emerald-700"
                            : p.status === "CLOSED"
                            ? "bg-slate-100 text-slate-600"
                            : "bg-red-50 text-red-700"
                        }`}
                      >
                        {p.status}
                      </span>
                    </td>

                    <td className="py-3.5 px-5 text-right space-x-1">
                      {p.status !== "ACTIVE" && (
                        <button
                          onClick={() => handleUpdateStatus(p.id, "ACTIVE")}
                          className="px-2 py-1 bg-emerald-50 text-emerald-700 rounded text-[11px] font-semibold hover:bg-emerald-100 transition"
                        >
                          Hiện
                        </button>
                      )}
                      {p.status === "ACTIVE" && (
                        <button
                          onClick={() => handleUpdateStatus(p.id, "HIDDEN")}
                          className="px-2 py-1 bg-red-50 text-red-700 rounded text-[11px] font-semibold hover:bg-red-100 transition"
                        >
                          Ẩn vi phạm
                        </button>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </AdminLayout>
  );
}
