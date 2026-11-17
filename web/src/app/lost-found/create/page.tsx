"use client";

import React, { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import Image from "next/image";
import { useGetIdentity } from "@refinedev/core";
import { PortalLayout } from "@/components/layout/PortalLayout";
import { supabaseBrowserClient } from "@/utils/supabase-client";
import {
  ArrowLeft,
  Send,
  AlertCircle,
  UploadCloud,
  X,
  PackageSearch,
  CheckCircle2,
  Lock,
  LogIn,
} from "lucide-react";
import type { AppUserIdentity } from "@/providers/auth-provider";

export default function CreateLostFoundPostPage() {
  const router = useRouter();
  const { data: user, isLoading: authLoading } = useGetIdentity<AppUserIdentity>();

  const [kind, setKind] = useState<"LOST" | "FOUND">("LOST");
  const [title, setTitle] = useState("");
  const [category, setCategory] = useState<string>("WALLET_PAPERS");
  const [location, setLocation] = useState("");
  const [contactName, setContactName] = useState("");
  const [contactPhone, setContactPhone] = useState("");
  const [description, setDescription] = useState("");

  const [imageFile, setImageFile] = useState<File | null>(null);
  const [imagePreview, setImagePreview] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  useEffect(() => {
    if (user?.name && !contactName) {
      setContactName(user.name);
    }
  }, [user, contactName]);

  const handleImageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      const file = e.target.files[0];
      if (file.size > 5 * 1024 * 1024) {
        setErrorMsg("Kích thước ảnh không được vượt quá 5MB.");
        return;
      }
      setImageFile(file);
      setImagePreview(URL.createObjectURL(file));
      setErrorMsg(null);
    }
  };

  const handleRemoveImage = () => {
    setImageFile(null);
    if (imagePreview) {
      URL.revokeObjectURL(imagePreview);
      setImagePreview(null);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg(null);

    if (!user?.id) {
      setErrorMsg("Bạn cần đăng nhập tài khoản UTH để đăng tin.");
      return;
    }

    if (!title.trim() || title.trim().length < 3) {
      setErrorMsg("Tiêu đề bài đăng phải có ít nhất 3 ký tự.");
      return;
    }

    if (!location.trim()) {
      setErrorMsg("Vui lòng ghi rõ địa điểm rơi hoặc nhặt được.");
      return;
    }

    const cleanPhone = contactPhone.trim();
    if (!cleanPhone || cleanPhone.length < 9) {
      setErrorMsg("Số điện thoại liên hệ phải có ít nhất 9 số.");
      return;
    }

    setLoading(true);

    try {
      let uploadedFilePath: string | null = null;
      let uploadedImageUrl: string | null = null;

      // Upload image to Supabase Storage bucket 'lost-found' if selected
      if (imageFile) {
        const fileExt = imageFile.name.split(".").pop()?.toLowerCase() || "jpg";
        const randomId = crypto.randomUUID();
        const filePath = `items/${user.id}/${randomId}.${fileExt}`;

        const { error: uploadError } = await supabaseBrowserClient.storage
          .from("lost-found")
          .upload(filePath, imageFile, {
            contentType: imageFile.type,
            upsert: false,
          });

        if (uploadError) {
          throw new Error(`Lỗi tải ảnh lên: ${uploadError.message}`);
        }

        uploadedFilePath = filePath;
        const { data: publicUrlData } = supabaseBrowserClient.storage
          .from("lost-found")
          .getPublicUrl(filePath);

        uploadedImageUrl = publicUrlData.publicUrl;
      }

      // Tự động định dạng ngày hiện tại theo chuẩn dd/MM/yyyy (đồng bộ 100% với Mobile App)
      const now = new Date();
      const day = String(now.getDate()).padStart(2, "0");
      const month = String(now.getMonth() + 1).padStart(2, "0");
      const year = now.getFullYear();
      const formattedToday = `${day}/${month}/${year}`;

      // Insert record into lost_found_items
      const { error: insertError } = await supabaseBrowserClient
        .from("lost_found_items")
        .insert({
          title: title.trim(),
          description: description.trim(),
          kind: kind,
          category: category,
          location: location.trim(),
          date: formattedToday,
          contact_name: contactName.trim() || (user.name || "Thành viên UTH"),
          contact_phone: cleanPhone,
          image_url: uploadedImageUrl,
          is_resolved: false,
          user_id: user.id,
        });

      if (insertError) {
        if (uploadedFilePath) {
          // Xóa ảnh rác trên Storage nếu insert bản ghi thất bại
          await supabaseBrowserClient.storage
            .from("lost-found")
            .remove([uploadedFilePath]);
        }
        throw new Error(insertError.message);
      }

      router.push("/lost-found");
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : "Đã xảy ra lỗi khi tạo tin.";
      setErrorMsg(message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <PortalLayout>
      <div className="max-w-2xl mx-auto">
        <Link
          href="/lost-found"
          className="inline-flex items-center gap-1.5 text-xs font-semibold text-slate-500 hover:text-slate-900 mb-4 transition"
        >
          <ArrowLeft className="w-3.5 h-3.5" />
          <span>Quay lại danh sách Đồ thất lạc</span>
        </Link>

        {/* 1. Auth Loading */}
        {authLoading && (
          <div className="py-20 text-center">
            <div className="w-8 h-8 border-2 border-slate-900 border-t-transparent rounded-full animate-spin mx-auto mb-3" />
            <div className="text-xs text-slate-500 font-medium">
              Đang xác thực tài khoản UTH...
            </div>
          </div>
        )}

        {/* 2. Chưa đăng nhập: Auth Wall */}
        {!authLoading && !user && (
          <div className="bg-white rounded-2xl p-8 sm:p-10 shadow-subtle border border-slate-100 text-center my-6">
            <div className="w-14 h-14 rounded-2xl bg-slate-900 text-white flex items-center justify-center mx-auto mb-5 shadow-sm">
              <Lock className="w-7 h-7 stroke-[1.75]" />
            </div>
            <h2 className="text-base font-bold text-slate-900 tracking-tight">
              Yêu Cầu Đăng Nhập Để Đăng Tin
            </h2>
            <p className="text-xs text-slate-500 mt-2 leading-relaxed max-w-md mx-auto">
              Chỉ sinh viên và cán bộ giảng viên UTH mới có quyền đăng tin báo mất hoặc nhặt được đồ nhằm phòng chống tin rác và lừa đảo.
            </p>
            <div className="mt-6">
              <Link
                href="/login"
                className="inline-flex items-center gap-2 px-6 py-2.5 bg-slate-900 text-white rounded-xl text-xs font-semibold hover:bg-slate-800 transition active:scale-[0.98] shadow-sm"
              >
                <LogIn className="w-4 h-4" />
                <span>Đăng nhập tài khoản UTH</span>
              </Link>
            </div>
          </div>
        )}

        {/* 3. Đã đăng nhập: Form tạo tin */}
        {!authLoading && user && (
          <div className="bg-white rounded-xl p-6 shadow-subtle">
            <div className="mb-5 pb-4 border-b border-slate-100 flex items-center justify-between">
              <div>
                <h1 className="text-base font-bold text-slate-900 tracking-tight">
                  Đăng Tin Đồ Thất Lạc & Nhặt Được
                </h1>
                <p className="text-xs text-slate-500 mt-0.5">
                  Cung cấp chi tiết thông tin và ảnh chụp để sinh viên dễ dàng nhận diện và trao trả
                </p>
              </div>
              <PackageSearch className="w-6 h-6 text-slate-400 stroke-[1.5]" />
            </div>

            {errorMsg && (
              <div className="p-3 mb-5 rounded-lg bg-red-50 text-red-700 text-xs flex items-start gap-2">
                <AlertCircle className="w-4 h-4 text-red-600 shrink-0 mt-0.5" />
                <span>{errorMsg}</span>
              </div>
            )}

          <form onSubmit={handleSubmit} className="space-y-4">
            {/* Kind Selection: LOST vs FOUND */}
            <div>
              <label className="block text-[11px] font-semibold text-slate-700 mb-1.5">
                Loại tin đăng *
              </label>
              <div className="grid grid-cols-2 gap-3">
                <button
                  type="button"
                  onClick={() => setKind("LOST")}
                  className={`p-3 rounded-lg border text-left transition flex items-center justify-between ${
                    kind === "LOST"
                      ? "border-rose-500 bg-rose-50/50 text-rose-900 font-semibold"
                      : "border-slate-200 bg-white text-slate-600 hover:bg-slate-50"
                  }`}
                >
                  <div>
                    <div className="text-xs font-bold">Tôi bị mất đồ (Lost)</div>
                    <div className="text-[11px] text-slate-500 mt-0.5">
                      Đăng tin tìm lại đồ bị thất lạc
                    </div>
                  </div>
                  {kind === "LOST" && (
                    <CheckCircle2 className="w-4 h-4 text-rose-600 shrink-0" />
                  )}
                </button>

                <button
                  type="button"
                  onClick={() => setKind("FOUND")}
                  className={`p-3 rounded-lg border text-left transition flex items-center justify-between ${
                    kind === "FOUND"
                      ? "border-emerald-600 bg-emerald-50/50 text-emerald-900 font-semibold"
                      : "border-slate-200 bg-white text-slate-600 hover:bg-slate-50"
                  }`}
                >
                  <div>
                    <div className="text-xs font-bold">Tôi nhặt được đồ (Found)</div>
                    <div className="text-[11px] text-slate-500 mt-0.5">
                      Tìm người đánh rơi để trao trả
                    </div>
                  </div>
                  {kind === "FOUND" && (
                    <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
                  )}
                </button>
              </div>
            </div>

            {/* Title */}
            <div>
              <label className="block text-[11px] font-semibold text-slate-700 mb-1">
                Tên món đồ / Tiêu đề tin *
              </label>
              <input
                type="text"
                required
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="vd: Thẻ sinh viên mang tên Nguyễn Văn A, khoa CNTT"
                className="w-full px-3 py-2 bg-[#F8FAFC] rounded-lg text-xs focus:outline-none focus:ring-1 focus:ring-slate-900 focus:bg-white transition"
              />
            </div>

            {/* Category */}
            <div>
              <label className="block text-[11px] font-semibold text-slate-700 mb-1">
                Danh mục đồ vật *
              </label>
              <select
                value={category}
                onChange={(e) => setCategory(e.target.value)}
                className="w-full px-3 py-2 bg-[#F8FAFC] rounded-lg text-xs focus:outline-none focus:ring-1 focus:ring-slate-900 focus:bg-white transition"
              >
                <option value="STUDENT_CARD">Thẻ sinh viên / Thẻ gửi xe</option>
                <option value="ELECTRONICS">Thiết bị điện tử (Laptop, ĐT, Sạc...)</option>
                <option value="WALLET_PAPERS">Ví tiền, CCCD & Giấy tờ tùy thân</option>
                <option value="PERSONAL_ITEMS">Đồ cá nhân (Balo, Áo khoác, Chìa khóa...)</option>
                <option value="OTHER">Đồ vật khác</option>
              </select>
            </div>

            {/* Location */}
            <div>
              <label className="block text-[11px] font-semibold text-slate-700 mb-1">
                Địa điểm rơi / nhặt được *
              </label>
              <input
                type="text"
                required
                value={location}
                onChange={(e) => setLocation(e.target.value)}
                placeholder="vd: Phòng B.302 Cơ sở 1 (Bình Thạnh) hoặc Căng tin Cơ sở 2"
                className="w-full px-3 py-2 bg-[#F8FAFC] rounded-lg text-xs focus:outline-none focus:ring-1 focus:ring-slate-900 focus:bg-white transition"
              />
            </div>

            {/* Contact Name & Phone */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              <div>
                <label className="block text-[11px] font-semibold text-slate-700 mb-1">
                  Họ tên người liên hệ
                </label>
                <input
                  type="text"
                  value={contactName}
                  onChange={(e) => setContactName(e.target.value)}
                  placeholder="Họ tên của bạn"
                  className="w-full px-3 py-2 bg-[#F8FAFC] rounded-lg text-xs focus:outline-none focus:ring-1 focus:ring-slate-900 focus:bg-white transition"
                />
              </div>

              <div>
                <label className="block text-[11px] font-semibold text-slate-700 mb-1">
                  Số điện thoại liên hệ *
                </label>
                <input
                  type="tel"
                  required
                  value={contactPhone}
                  onChange={(e) => setContactPhone(e.target.value)}
                  placeholder="09xx xxx xxx"
                  className="w-full px-3 py-2 bg-[#F8FAFC] rounded-lg text-xs focus:outline-none focus:ring-1 focus:ring-slate-900 focus:bg-white transition tabular-nums"
                />
              </div>
            </div>

            {/* Description */}
            <div>
              <label className="block text-[11px] font-semibold text-slate-700 mb-1">
                Mô tả chi tiết đặc điểm nhận dạng
              </label>
              <textarea
                rows={3}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Mô tả màu sắc, ốp lưng, vết trầy xước, đặc điểm riêng biệt giúp chủ nhân nhận ra..."
                className="w-full px-3 py-2 bg-[#F8FAFC] rounded-lg text-xs focus:outline-none focus:ring-1 focus:ring-slate-900 focus:bg-white transition"
              />
            </div>

            {/* Photo Upload */}
            <div>
              <label className="block text-[11px] font-semibold text-slate-700 mb-1">
                Hình ảnh minh chứng (Tùy chọn, tối đa 5MB)
              </label>

              {imagePreview ? (
                <div className="relative inline-block mt-1">
                  <div className="relative w-36 h-36 rounded-xl overflow-hidden border border-slate-200 shadow-sm">
                    <Image
                      src={imagePreview}
                      alt="Ảnh xem trước"
                      fill
                      sizes="144px"
                      className="object-cover"
                      unoptimized
                    />
                  </div>
                  <button
                    type="button"
                    aria-label="Xóa ảnh này"
                    onClick={handleRemoveImage}
                    className="absolute -top-2 -right-2 p-1 bg-red-600 text-white rounded-full hover:bg-red-700 shadow transition"
                    title="Xóa ảnh này"
                  >
                    <X className="w-3.5 h-3.5" />
                  </button>
                </div>
              ) : (
                <label className="border-2 border-dashed border-slate-200 hover:border-slate-400 bg-[#F8FAFC] rounded-xl p-6 flex flex-col items-center justify-center cursor-pointer transition">
                  <UploadCloud className="w-8 h-8 text-slate-400 mb-2 stroke-[1.5]" />
                  <span className="text-xs font-semibold text-slate-700">
                    Bấm để tải ảnh lên
                  </span>
                  <span className="text-[10px] text-slate-400 mt-0.5">
                    Hỗ trợ định dạng JPG, PNG, WEBP (tối đa 5MB)
                  </span>
                  <input
                    type="file"
                    accept="image/jpeg,image/png,image/webp"
                    className="hidden"
                    onChange={handleImageChange}
                  />
                </label>
              )}
            </div>

            {/* Submit button */}
            <div className="pt-3 flex items-center justify-end gap-2.5">
              <Link
                href="/lost-found"
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
                <span>{loading ? "Đang đăng tin..." : "Đăng tin"}</span>
              </button>
            </div>
          </form>
        </div>
        )}
      </div>
    </PortalLayout>
  );
}
