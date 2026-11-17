"use client";

import React, { useState } from "react";
import { useRouter } from "next/navigation";
import { useGetIdentity } from "@refinedev/core";
import { PortalLayout } from "@/components/layout/PortalLayout";
import { supabaseBrowserClient } from "@/utils/supabase-client";
import { ArrowLeft, Send, AlertCircle } from "lucide-react";
import type { AppUserIdentity } from "@/providers/auth-provider";
import Link from "next/link";

export default function CreateHousingPostPage() {
  const router = useRouter();
  const { data: user } = useGetIdentity<AppUserIdentity>();

  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [price, setPrice] = useState<number>(1500000);
  const [location, setLocation] = useState("");
  const [campus, setCampus] = useState("CS1");
  const [roomType, setRoomType] = useState("roommate");
  const [contactPhone, setContactPhone] = useState("");
  const [contactZalo, setContactZalo] = useState("");
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg(null);

    if (!user?.id) {
      setErrorMsg("Bạn cần đăng nhập để đăng tin.");
      return;
    }

    if (!title.trim() || title.trim().length < 5) {
      setErrorMsg("Tiêu đề bài đăng phải từ 5 ký tự trở lên.");
      return;
    }

    setLoading(true);

    const resolvedCampus =
      campus === "CS1"
        ? "CS1_QUANG_TRUNG"
        : campus === "CS2"
        ? "CS2_THU_DUC"
        : campus === "CS3"
        ? "CS3_TO_KY"
        : campus;

    const resolvedType =
      roomType === "roommate"
        ? "LOOKING_FOR_ROOMMATE"
        : roomType === "dormitory"
        ? "ROOM_FOR_RENT"
        : "ROOM_FOR_RENT";

    const hostEmail = (user as any)?.email || "";
    const studentId = hostEmail.split("@")[0] || "UTH";

    const { error } = await supabaseBrowserClient.from("housing_posts").insert({
      author_id: user.id,
      title: title.trim(),
      description: description.trim(),
      monthly_rent_vnd: Number(price) || 1000000,
      type: resolvedType,
      campus: resolvedCampus,
      address_label: location.trim() || "Gần cơ sở UTH",
      host_name: user.name || "Sinh viên UTH",
      host_student_id: studentId,
      contact_phone: contactPhone.trim() || "0000000000",
      contact_zalo_url: contactZalo.trim()
        ? contactZalo.startsWith("http")
          ? contactZalo
          : `https://zalo.me/${contactZalo.replace(/[^0-9]/g, "")}`
        : "",
      status: "pending",
    });

    setLoading(false);

    if (error) {
      setErrorMsg(`Lỗi khi tạo bài đăng: ${error.message}`);
    } else {
      router.push("/housing");
    }
  };

  return (
    <PortalLayout>
      <div className="max-w-2xl mx-auto">
        <Link
          href="/housing"
          className="inline-flex items-center gap-1.5 text-xs font-semibold text-slate-500 hover:text-slate-900 mb-4 transition"
        >
          <ArrowLeft className="w-3.5 h-3.5" />
          <span>Quay lại danh sách phòng trọ</span>
        </Link>

        <div className="bg-white rounded-xl p-6 shadow-subtle">
          <div className="mb-5 pb-4 border-b border-slate-100">
            <h1 className="text-base font-bold text-slate-900 tracking-tight">
              Đăng Tin Ghép Trọ / Ký Túc Xá
            </h1>
            <p className="text-xs text-slate-500 mt-0.5">
              Chia sẻ thông tin phòng trọ sạch sẽ, an ninh cho bạn học UTH
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
                Tiêu đề bài đăng *
              </label>
              <input
                type="text"
                required
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="vd: Tìm 1 bạn nam ở ghép phòng trọ gần CS2 Quận 12"
                className="w-full px-3 py-2 bg-[#F8FAFC] rounded-lg text-xs focus:outline-none focus:ring-1 focus:ring-slate-900 focus:bg-white transition"
              />
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
              <div>
                <label className="block text-[11px] font-semibold text-slate-700 mb-1">
                  Hình thức
                </label>
                <select
                  value={roomType}
                  onChange={(e) => setRoomType(e.target.value)}
                  className="w-full px-3 py-2 bg-[#F8FAFC] rounded-lg text-xs focus:outline-none focus:ring-1 focus:ring-slate-900 focus:bg-white transition"
                >
                  <option value="roommate">Tìm bạn ở ghép</option>
                  <option value="rental">Cho thuê nguyên phòng</option>
                  <option value="dormitory">Ký túc xá</option>
                </select>
              </div>

              <div>
                <label className="block text-[11px] font-semibold text-slate-700 mb-1">
                  Cơ sở gần nhất
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
                  Giá phòng (VNĐ/tháng) *
                </label>
                <input
                  type="number"
                  required
                  min={0}
                  step={50000}
                  value={price}
                  onChange={(e) => setPrice(Number(e.target.value))}
                  className="w-full px-3 py-2 bg-[#F8FAFC] rounded-lg text-xs focus:outline-none focus:ring-1 focus:ring-slate-900 focus:bg-white transition tabular-nums"
                />
              </div>
            </div>

            <div>
              <label className="block text-[11px] font-semibold text-slate-700 mb-1">
                Địa chỉ chi tiết / Khu vực
              </label>
              <input
                type="text"
                value={location}
                onChange={(e) => setLocation(e.target.value)}
                placeholder="vd: Hẻm 125 Tân Chánh Hiệp, cách cổng CS2 300m"
                className="w-full px-3 py-2 bg-[#F8FAFC] rounded-lg text-xs focus:outline-none focus:ring-1 focus:ring-slate-900 focus:bg-white transition"
              />
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              <div>
                <label className="block text-[11px] font-semibold text-slate-700 mb-1">
                  Số điện thoại liên hệ
                </label>
                <input
                  type="tel"
                  value={contactPhone}
                  onChange={(e) => setContactPhone(e.target.value)}
                  placeholder="09xx xxx xxx"
                  className="w-full px-3 py-2 bg-[#F8FAFC] rounded-lg text-xs focus:outline-none focus:ring-1 focus:ring-slate-900 focus:bg-white transition tabular-nums"
                />
              </div>

              <div>
                <label className="block text-[11px] font-semibold text-slate-700 mb-1">
                  Số điện thoại Zalo
                </label>
                <input
                  type="tel"
                  value={contactZalo}
                  onChange={(e) => setContactZalo(e.target.value)}
                  placeholder="09xx xxx xxx"
                  className="w-full px-3 py-2 bg-[#F8FAFC] rounded-lg text-xs focus:outline-none focus:ring-1 focus:ring-slate-900 focus:bg-white transition tabular-nums"
                />
              </div>
            </div>

            <div>
              <label className="block text-[11px] font-semibold text-slate-700 mb-1">
                Mô tả chi tiết phòng trọ
              </label>
              <textarea
                rows={4}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Mô tả tiện nghi (máy lạnh, wifi, giờ giấc, có gác lửng, chỗ để xe...)"
                className="w-full px-3 py-2 bg-[#F8FAFC] rounded-lg text-xs focus:outline-none focus:ring-1 focus:ring-slate-900 focus:bg-white transition"
              />
            </div>

            <div className="pt-3 flex items-center justify-end gap-2.5">
              <Link
                href="/housing"
                className="px-3.5 py-2 rounded-lg text-xs font-semibold text-slate-600 hover:bg-slate-100 transition"
              >
                Hủy bỏ
              </Link>
              <button
                type="submit"
                disabled={loading}
                className="inline-flex items-center gap-1.5 px-4 py-2 rounded-lg bg-slate-900 text-white text-xs font-semibold hover:bg-slate-800 transition active:scale-[0.98] disabled:opacity-50"
              >
                <Send className="w-3.5 h-3.5" />
                <span>{loading ? "Đang đăng..." : "Đăng tin"}</span>
              </button>
            </div>
          </form>
        </div>
      </div>
    </PortalLayout>
  );
}
