"use client";

import React, { useState } from "react";
import { useRouter } from "next/navigation";
import { useGetIdentity } from "@refinedev/core";
import { PortalLayout } from "@/components/layout/PortalLayout";
import { supabaseBrowserClient } from "@/utils/supabase-client";
import { ArrowLeft, Send, AlertCircle } from "lucide-react";
import type { AppUserIdentity } from "@/providers/auth-provider";
import Link from "next/link";

export default function CreateNotificationPage() {
  const router = useRouter();
  const { data: user } = useGetIdentity<AppUserIdentity>();

  const [title, setTitle] = useState("");
  const [summary, setSummary] = useState("");
  const [content, setContent] = useState("");
  const [category, setCategory] = useState("GENERAL");
  const [author, setAuthor] = useState("");
  const [isPinned, setIsPinned] = useState(false);
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const role = user?.role || "student";
  const canPublish = role === "admin" || role === "lecturer" || role === "organizer";

  const categories = [
    { label: "Thông báo chung (GENERAL)", value: "GENERAL" },
    { label: "Đào tạo & Học vụ (ACADEMIC)", value: "ACADEMIC" },
    { label: "Khảo thí & Lịch thi (EXAMINATION)", value: "EXAMINATION" },
    { label: "Học phí & Học bổng (TUITION)", value: "TUITION" },
    { label: "Hoạt động sinh viên & CLB (ACTIVITIES)", value: "ACTIVITIES" },
  ];

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg(null);

    if (!canPublish) {
      setErrorMsg("Bạn không có quyền đăng thông báo (Chỉ Admin và Giảng viên).");
      return;
    }

    if (!title.trim()) {
      setErrorMsg("Tiêu đề thông báo không được để trống.");
      return;
    }

    setLoading(true);

    const { error } = await supabaseBrowserClient.from("notifications").insert({
      title: title.trim(),
      summary: summary.trim(),
      content: content.trim(),
      category: category,
      author: author.trim() || user?.name || "Ban Giám Hiệu",
      is_pinned: isPinned,
      published_at: new Date().toISOString(),
    });

    setLoading(false);

    if (error) {
      setErrorMsg(`Lỗi khi tạo thông báo: ${error.message}`);
    } else {
      router.push("/notifications");
    }
  };

  return (
    <PortalLayout>
      <div className="max-w-2xl mx-auto">
        <Link
          href="/notifications"
          className="inline-flex items-center gap-1.5 text-xs font-semibold text-slate-500 hover:text-slate-900 mb-4 transition"
        >
          <ArrowLeft className="w-3.5 h-3.5" />
          <span>Quay lại thông báo</span>
        </Link>

        <div className="bg-white rounded-xl p-6 shadow-subtle">
          <div className="mb-5 pb-4 border-b border-slate-100">
            <h1 className="text-base font-bold text-slate-900 tracking-tight">
              Đăng Thông Báo Mới
            </h1>
            <p className="text-xs text-slate-500 mt-0.5">
              Dành riêng cho Giảng viên và Ban tổ chức phát hành tới sinh viên
            </p>
          </div>

          {errorMsg && (
            <div className="p-3 mb-4 rounded-lg bg-red-50 text-red-700 text-xs flex items-start gap-2">
              <AlertCircle className="w-4 h-4 text-red-600 shrink-0 mt-0.5" />
              <span>{errorMsg}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-[11px] font-semibold text-slate-700 mb-1">
                Tiêu đề thông báo *
              </label>
              <input
                type="text"
                required
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="vd: Thông báo đăng ký môn học học kỳ 1 năm học 2026-2027"
                className="w-full px-3 py-2 bg-[#F8FAFC] rounded-lg text-xs focus:outline-none focus:ring-1 focus:ring-slate-900 focus:bg-white transition"
              />
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              <div>
                <label className="block text-[11px] font-semibold text-slate-700 mb-1">
                  Danh mục
                </label>
                <select
                  value={category}
                  onChange={(e) => setCategory(e.target.value)}
                  className="w-full px-3 py-2 bg-[#F8FAFC] rounded-lg text-xs focus:outline-none focus:ring-1 focus:ring-slate-900 focus:bg-white transition"
                >
                  {categories.map((c) => (
                    <option key={c.value} value={c.value}>
                      {c.label}
                    </option>
                  ))}
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
                  placeholder={user?.name || "Phòng Đào Tạo"}
                  className="w-full px-3 py-2 bg-[#F8FAFC] rounded-lg text-xs focus:outline-none focus:ring-1 focus:ring-slate-900 focus:bg-white transition"
                />
              </div>
            </div>

            <div>
              <label className="block text-[11px] font-semibold text-slate-700 mb-1">
                Tóm tắt ngắn (1-2 câu)
              </label>
              <input
                type="text"
                value={summary}
                onChange={(e) => setSummary(e.target.value)}
                placeholder="Tóm tắt ngắn gọn để hiển thị trên danh sách..."
                className="w-full px-3 py-2 bg-[#F8FAFC] rounded-lg text-xs focus:outline-none focus:ring-1 focus:ring-slate-900 focus:bg-white transition"
              />
            </div>

            <div>
              <label className="block text-[11px] font-semibold text-slate-700 mb-1">
                Nội dung chi tiết
              </label>
              <textarea
                rows={5}
                value={content}
                onChange={(e) => setContent(e.target.value)}
                placeholder="Nhập nội dung đầy đủ của thông báo..."
                className="w-full px-3 py-2 bg-[#F8FAFC] rounded-lg text-xs focus:outline-none focus:ring-1 focus:ring-slate-900 focus:bg-white transition"
              />
            </div>

            <div className="flex items-center gap-2">
              <input
                type="checkbox"
                id="isPinned"
                checked={isPinned}
                onChange={(e) => setIsPinned(e.target.checked)}
                className="w-3.5 h-3.5 rounded text-slate-900"
              />
              <label htmlFor="isPinned" className="text-xs text-slate-700 cursor-pointer">
                Ghim thông báo lên đầu trang danh sách
              </label>
            </div>

            <div className="pt-3 flex items-center justify-end gap-2.5">
              <Link
                href="/notifications"
                className="px-3.5 py-2 rounded-lg text-xs font-semibold text-slate-600 hover:bg-slate-100 transition"
              >
                Hủy bỏ
              </Link>
              <button
                type="submit"
                disabled={loading || !canPublish}
                className="inline-flex items-center gap-1.5 px-4 py-2 rounded-lg bg-slate-900 text-white text-xs font-semibold hover:bg-slate-800 transition active:scale-[0.98] disabled:opacity-50"
              >
                <Send className="w-3.5 h-3.5" />
                <span>{loading ? "Đang phát hành..." : "Phát hành"}</span>
              </button>
            </div>
          </form>
        </div>
      </div>
    </PortalLayout>
  );
}
