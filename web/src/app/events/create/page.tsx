"use client";

import React, { useState } from "react";
import { useRouter } from "next/navigation";
import { useGetIdentity } from "@refinedev/core";
import { PortalLayout } from "@/components/layout/PortalLayout";
import { supabaseBrowserClient } from "@/utils/supabase-client";
import { ArrowLeft, Send, AlertCircle } from "lucide-react";
import type { AppUserIdentity } from "@/providers/auth-provider";
import Link from "next/link";

export default function CreateEventPage() {
  const router = useRouter();
  const { data: user } = useGetIdentity<AppUserIdentity>();

  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [location, setLocation] = useState("");
  const [campus, setCampus] = useState("CS1");
  const [startAt, setStartAt] = useState("");
  const [endAt, setEndAt] = useState("");
  const [maxParticipants, setMaxParticipants] = useState<number>(100);
  const [points, setPoints] = useState<number>(5);
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const role = user?.role || "student";
  const canCreate = role === "admin" || role === "lecturer" || role === "organizer";

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg(null);

    if (!canCreate) {
      setErrorMsg("Bạn không có quyền tạo sự kiện (Chỉ Admin và Giảng viên).");
      return;
    }

    if (!title.trim() || !startAt || !endAt || !location.trim()) {
      setErrorMsg("Vui lòng điền đầy đủ các trường bắt buộc.");
      return;
    }

    setLoading(true);

    const { error } = await supabaseBrowserClient.from("events").insert({
      title: title.trim(),
      description: description.trim(),
      location: location.trim(),
      campus: campus,
      start_at: new Date(startAt).toISOString(),
      end_at: new Date(endAt).toISOString(),
      max_participants: Number(maxParticipants),
      points: Number(points),
      status: "PUBLISHED",
      organizer_id: user?.id,
      organizer_name: user?.name || "Ban Tổ Chức",
    });

    setLoading(false);

    if (error) {
      setErrorMsg(`Lỗi khi tạo sự kiện: ${error.message}`);
    } else {
      router.push("/events");
    }
  };

  return (
    <PortalLayout>
      <div className="max-w-2xl mx-auto">
        <Link
          href="/events"
          className="inline-flex items-center gap-1.5 text-xs font-semibold text-slate-500 hover:text-slate-900 mb-4 transition"
        >
          <ArrowLeft className="w-3.5 h-3.5" />
          <span>Quay lại sự kiện</span>
        </Link>

        <div className="bg-white rounded-xl p-6 shadow-subtle">
          <div className="mb-5 pb-4 border-b border-slate-100">
            <h1 className="text-base font-bold text-slate-900 tracking-tight">
              Tổ Chức Sự Kiện Mới
            </h1>
            <p className="text-xs text-slate-500 mt-0.5">
              Thiết lập thông tin sự kiện Đoàn - Hội, hội thảo và số lượng tham gia
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
                Tên sự kiện *
              </label>
              <input
                type="text"
                required
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="vd: Hội nghị Nghiên cứu Khoa học Sinh viên UTH 2026"
                className="w-full px-3 py-2 bg-[#F8FAFC] rounded-lg text-xs focus:outline-none focus:ring-1 focus:ring-slate-900 focus:bg-white transition"
              />
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              <div>
                <label className="block text-[11px] font-semibold text-slate-700 mb-1">
                  Cơ sở tổ chức
                </label>
                <select
                  value={campus}
                  onChange={(e) => setCampus(e.target.value)}
                  className="w-full px-3 py-2 bg-[#F8FAFC] rounded-lg text-xs focus:outline-none focus:ring-1 focus:ring-slate-900 focus:bg-white transition"
                >
                  <option value="CS1">Cơ sở 1 (Bình Thạnh)</option>
                  <option value="CS2">Cơ sở 2 (Quận 12)</option>
                  <option value="CS3">Cơ sở 3 (Thủ Đức)</option>
                </select>
              </div>

              <div>
                <label className="block text-[11px] font-semibold text-slate-700 mb-1">
                  Địa điểm cụ thể *
                </label>
                <input
                  type="text"
                  required
                  value={location}
                  onChange={(e) => setLocation(e.target.value)}
                  placeholder="vd: Hội trường A, Khu E CS1"
                  className="w-full px-3 py-2 bg-[#F8FAFC] rounded-lg text-xs focus:outline-none focus:ring-1 focus:ring-slate-900 focus:bg-white transition"
                />
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              <div>
                <label className="block text-[11px] font-semibold text-slate-700 mb-1">
                  Thời gian bắt đầu *
                </label>
                <input
                  type="datetime-local"
                  required
                  value={startAt}
                  onChange={(e) => setStartAt(e.target.value)}
                  className="w-full px-3 py-2 bg-[#F8FAFC] rounded-lg text-xs focus:outline-none focus:ring-1 focus:ring-slate-900 focus:bg-white transition tabular-nums"
                />
              </div>

              <div>
                <label className="block text-[11px] font-semibold text-slate-700 mb-1">
                  Thời gian kết thúc *
                </label>
                <input
                  type="datetime-local"
                  required
                  value={endAt}
                  onChange={(e) => setEndAt(e.target.value)}
                  className="w-full px-3 py-2 bg-[#F8FAFC] rounded-lg text-xs focus:outline-none focus:ring-1 focus:ring-slate-900 focus:bg-white transition tabular-nums"
                />
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              <div>
                <label className="block text-[11px] font-semibold text-slate-700 mb-1">
                  Số lượng người tối đa (0 = Không giới hạn)
                </label>
                <input
                  type="number"
                  min={0}
                  value={maxParticipants}
                  onChange={(e) => setMaxParticipants(Number(e.target.value))}
                  className="w-full px-3 py-2 bg-[#F8FAFC] rounded-lg text-xs focus:outline-none focus:ring-1 focus:ring-slate-900 focus:bg-white transition tabular-nums"
                />
              </div>

              <div>
                <label className="block text-[11px] font-semibold text-slate-700 mb-1">
                  Điểm rèn luyện (+ĐRL)
                </label>
                <input
                  type="number"
                  min={0}
                  value={points}
                  onChange={(e) => setPoints(Number(e.target.value))}
                  className="w-full px-3 py-2 bg-[#F8FAFC] rounded-lg text-xs focus:outline-none focus:ring-1 focus:ring-slate-900 focus:bg-white transition tabular-nums"
                />
              </div>
            </div>

            <div>
              <label className="block text-[11px] font-semibold text-slate-700 mb-1">
                Mô tả chi tiết sự kiện
              </label>
              <textarea
                rows={4}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Mô tả quyền lợi, diễn giả, lưu ý tham gia..."
                className="w-full px-3 py-2 bg-[#F8FAFC] rounded-lg text-xs focus:outline-none focus:ring-1 focus:ring-slate-900 focus:bg-white transition"
              />
            </div>

            <div className="pt-3 flex items-center justify-end gap-2.5">
              <Link
                href="/events"
                className="px-3.5 py-2 rounded-lg text-xs font-semibold text-slate-600 hover:bg-slate-100 transition"
              >
                Hủy bỏ
              </Link>
              <button
                type="submit"
                disabled={loading || !canCreate}
                className="inline-flex items-center gap-1.5 px-4 py-2 rounded-lg bg-slate-900 text-white text-xs font-semibold hover:bg-slate-800 transition active:scale-[0.98] disabled:opacity-50"
              >
                <Send className="w-3.5 h-3.5" />
                <span>{loading ? "Đang xuất bản..." : "Xuất bản sự kiện"}</span>
              </button>
            </div>
          </form>
        </div>
      </div>
    </PortalLayout>
  );
}
