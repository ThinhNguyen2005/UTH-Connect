"use client";

import React, { useEffect, useState } from "react";
import { AdminLayout } from "@/components/layout/AdminLayout";
import { supabaseBrowserClient } from "@/utils/supabase-client";
import { Bell, Send, RefreshCw, Pin } from "lucide-react";

export default function AdminAnnouncementsPage() {
  const [notifications, setNotifications] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [title, setTitle] = useState("");
  const [category, setCategory] = useState("GENERAL");
  const [summary, setSummary] = useState("");
  const [author, setAuthor] = useState("Ban Giám Hiệu");
  const [isPinned, setIsPinned] = useState(false);
  const [toast, setToast] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const fetchNotifs = async () => {
    setLoading(true);
    const { data } = await supabaseBrowserClient
      .from("notifications")
      .select("*")
      .order("published_at", { ascending: false });
    if (data) setNotifications(data);
    setLoading(false);
  };

  useEffect(() => {
    let isMounted = true;
    const fetchInitial = async () => {
      setLoading(true);
      const { data } = await supabaseBrowserClient
        .from("notifications")
        .select("*")
        .order("published_at", { ascending: false });
      if (isMounted) {
        if (data) setNotifications(data);
        setLoading(false);
      }
    };
    fetchInitial();
    return () => {
      isMounted = false;
    };
  }, []);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim()) return;

    setSubmitting(true);
    setToast(null);

    const { error } = await supabaseBrowserClient.from("notifications").insert({
      title: title.trim(),
      summary: summary.trim(),
      category: category,
      author: author.trim(),
      is_pinned: isPinned,
      published_at: new Date().toISOString(),
    });

    setSubmitting(false);

    if (error) {
      setToast(`Lỗi: ${error.message}`);
    } else {
      setToast("Đã phát hành thông báo thành công!");
      setTitle("");
      setSummary("");
      setIsPinned(false);
      fetchNotifs();
    }
  };

  return (
    <AdminLayout>
      <div className="mb-6">
        <h1 className="text-xl font-bold text-slate-900 tracking-tight">
          Soạn Thảo & Quản Lý Thông Báo Toàn Trường
        </h1>
        <p className="text-xs text-slate-500 mt-0.5">
          Thông báo sau khi phát hành sẽ hiển thị trực tiếp trên App Sinh viên và Web Portal
        </p>
      </div>

      {toast && (
        <div className="mb-4 p-3 bg-blue-50 text-slate-800 text-xs rounded-lg font-medium">
          {toast}
        </div>
      )}

      {/* Two Column Layout: Form 1/3, History 2/3 */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Form Column */}
        <div className="bg-white rounded-xl p-5 shadow-subtle">
          <div className="text-xs font-bold text-slate-900 uppercase tracking-wider mb-4">
            Soạn thông báo mới
          </div>

          <form onSubmit={handleCreate} className="space-y-3.5">
            <div>
              <label className="block text-[11px] font-semibold text-slate-700 mb-1">
                Tiêu đề thông báo *
              </label>
              <input
                type="text"
                required
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="vd: Lịch nghỉ lễ và điều chỉnh thời khóa biểu..."
                className="w-full px-3 py-2 bg-[#F8FAFC] rounded-lg text-xs focus:outline-none focus:ring-1 focus:ring-slate-900 transition"
              />
            </div>

            <div>
              <label className="block text-[11px] font-semibold text-slate-700 mb-1">
                Danh mục
              </label>
              <select
                value={category}
                onChange={(e) => setCategory(e.target.value)}
                className="w-full px-3 py-2 bg-[#F8FAFC] rounded-lg text-xs focus:outline-none focus:ring-1 focus:ring-slate-900 transition"
              >
                <option value="GENERAL">Thông báo chung (GENERAL)</option>
                <option value="ACADEMIC">Đào tạo & Học vụ (ACADEMIC)</option>
                <option value="EXAMINATION">Khảo thí & Thi cử (EXAMINATION)</option>
                <option value="TUITION">Học phí (TUITION)</option>
                <option value="ACTIVITIES">Hoạt động Đoàn - Hội (ACTIVITIES)</option>
              </select>
            </div>

            <div>
              <label className="block text-[11px] font-semibold text-slate-700 mb-1">
                Đơn vị ban hành
              </label>
              <input
                type="text"
                value={author}
                onChange={(e) => setAuthor(e.target.value)}
                className="w-full px-3 py-2 bg-[#F8FAFC] rounded-lg text-xs focus:outline-none focus:ring-1 focus:ring-slate-900 transition"
              />
            </div>

            <div>
              <label className="block text-[11px] font-semibold text-slate-700 mb-1">
                Tóm tắt nội dung
              </label>
              <textarea
                rows={4}
                value={summary}
                onChange={(e) => setSummary(e.target.value)}
                placeholder="Nhập nội dung vắn tắt gửi tới sinh viên..."
                className="w-full px-3 py-2 bg-[#F8FAFC] rounded-lg text-xs focus:outline-none focus:ring-1 focus:ring-slate-900 transition"
              />
            </div>

            <div className="flex items-center gap-2">
              <input
                type="checkbox"
                id="adminPin"
                checked={isPinned}
                onChange={(e) => setIsPinned(e.target.checked)}
                className="w-3.5 h-3.5 rounded text-slate-900"
              />
              <label htmlFor="adminPin" className="text-xs text-slate-700 cursor-pointer">
                Ghim lên đầu trang
              </label>
            </div>

            <button
              type="submit"
              disabled={submitting}
              className="w-full flex items-center justify-center gap-1.5 py-2.5 px-4 bg-slate-900 text-white rounded-lg text-xs font-semibold hover:bg-slate-800 transition active:scale-[0.98] disabled:opacity-50"
            >
              <Send className="w-3.5 h-3.5" />
              <span>{submitting ? "Đang gửi..." : "Phát hành"}</span>
            </button>
          </form>
        </div>

        {/* History Column */}
        <div className="lg:col-span-2 bg-white rounded-xl p-5 shadow-subtle">
          <div className="flex items-center justify-between mb-4">
            <div className="text-xs font-bold text-slate-900 uppercase tracking-wider">
              Lịch sử thông báo đã phát hành ({notifications.length})
            </div>
            <button
              onClick={fetchNotifs}
              className="p-1 text-slate-400 hover:text-slate-700 transition"
            >
              <RefreshCw className="w-3.5 h-3.5 stroke-[1.5]" />
            </button>
          </div>

          <div className="space-y-3">
            {loading ? (
              <div className="py-8 text-center text-xs text-slate-400">
                Đang tải dữ liệu...
              </div>
            ) : notifications.length === 0 ? (
              <div className="py-8 text-center text-xs text-slate-400">
                Chưa có thông báo nào.
              </div>
            ) : (
              notifications.map((n) => (
                <div key={n.id} className="p-3.5 rounded-lg bg-[#F8FAFC]">
                  <div className="flex items-center justify-between gap-2 mb-1">
                    <div className="flex items-center gap-2">
                      {n.is_pinned && (
                        <span className="inline-flex items-center gap-1 text-[10px] font-semibold text-amber-700 bg-amber-50 px-1.5 py-0.5 rounded">
                          <Pin className="w-3 h-3 text-amber-600" />
                          Ghim
                        </span>
                      )}
                      <span className="text-[10px] font-semibold text-slate-600 bg-white px-1.5 py-0.5 rounded">
                        {n.category || "GENERAL"}
                      </span>
                    </div>
                    <span className="text-[11px] text-slate-400 tabular-nums">
                      {new Date(n.published_at).toLocaleString("vi-VN")}
                    </span>
                  </div>
                  <div className="text-xs font-semibold text-slate-900 mb-1">
                    {n.title}
                  </div>
                  {n.summary && (
                    <div className="text-[11px] text-slate-500 line-clamp-2">
                      {n.summary}
                    </div>
                  )}
                  <div className="text-[10px] text-slate-400 mt-2">
                    Người phát hành: {n.author}
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    </AdminLayout>
  );
}
