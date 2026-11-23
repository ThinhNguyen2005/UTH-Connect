"use client";

import React, { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { useGetIdentity } from "@refinedev/core";
import { PortalLayout } from "@/components/layout/PortalLayout";
import { supabaseBrowserClient } from "@/utils/supabase-client";
import { uploadBookImage } from "@/utils/bookImages";
import type { AppUserIdentity } from "@/providers/auth-provider";
import {
  ArrowLeft,
  Upload,
  Image as ImageIcon,
  X,
  BookOpen,
  DollarSign,
  AlertCircle,
  Loader2,
  CheckCircle2,
} from "lucide-react";

export default function CreateBookListingPage() {
  const router = useRouter();
  const { data: user } = useGetIdentity<AppUserIdentity>();

  const [title, setTitle] = useState("");
  const [subject, setSubject] = useState("");
  const [condition, setCondition] = useState<"NEW" | "GOOD" | "USED">("GOOD");
  const [dealType, setDealType] = useState<"SALE" | "EXCHANGE">("SALE");
  const [priceVnd, setPriceVnd] = useState<string>("50000");
  const [description, setDescription] = useState("");
  const [sellerName, setSellerName] = useState(user?.name || "");
  const [contact, setContact] = useState("");
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);

  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  // Synchronize seller name when user identity loads
  React.useEffect(() => {
    if (user?.name && !sellerName) {
      setSellerName(user.name);
    }
  }, [user?.name, sellerName]);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (!["image/jpeg", "image/png", "image/webp"].includes(file.type)) {
      setErrorMsg("Chỉ hỗ trợ file ảnh JPG, PNG hoặc WebP.");
      return;
    }

    if (file.size > 5 * 1024 * 1024) {
      setErrorMsg("Kích thước ảnh tối đa là 5MB.");
      return;
    }

    setSelectedFile(file);
    setErrorMsg(null);
    const objectUrl = URL.createObjectURL(file);
    setPreviewUrl(objectUrl);
  };

  const removeSelectedFile = () => {
    setSelectedFile(null);
    if (previewUrl) {
      URL.revokeObjectURL(previewUrl);
      setPreviewUrl(null);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg(null);

    if (!user?.id) {
      setErrorMsg("Bạn cần đăng nhập để đăng sách giáo trình.");
      return;
    }

    const cleanTitle = title.trim();
    if (!cleanTitle || cleanTitle.length > 160) {
      setErrorMsg("Tiêu đề sách phải từ 1 đến 160 ký tự.");
      return;
    }

    const cleanSubject = subject.trim();
    if (!cleanSubject || cleanSubject.length > 100) {
      setErrorMsg("Môn học / Ngành đào tạo phải từ 1 đến 100 ký tự.");
      return;
    }

    const cleanSellerName = sellerName.trim();
    if (!cleanSellerName || cleanSellerName.length > 100) {
      setErrorMsg("Vui lòng nhập họ và tên người bán.");
      return;
    }

    const cleanContact = contact.trim();
    if (!cleanContact || cleanContact.length < 5 || cleanContact.length > 254) {
      setErrorMsg("Thông tin liên hệ (SĐT hoặc Zalo) phải từ 5 ký tự trở lên.");
      return;
    }

    let parsedPrice: number | null = null;
    if (dealType === "SALE") {
      parsedPrice = parseInt(priceVnd.replace(/\D/g, ""), 10);
      if (isNaN(parsedPrice) || parsedPrice <= 0) {
        setErrorMsg("Vui lòng nhập giá bán hợp lệ lớn hơn 0 VNĐ.");
        return;
      }
    }

    setLoading(true);

    try {
      const listingId = crypto.randomUUID();
      let uploadedImagePath: string | null = null;

      // 1. Upload ảnh nếu có
      if (selectedFile) {
        uploadedImagePath = await uploadBookImage(selectedFile, user.id, listingId);
      }

      // 2. Insert vào bảng public.book_listings
      const { error: insertError } = await supabaseBrowserClient
        .from("book_listings")
        .insert({
          id: listingId,
          owner_user_id: user.id,
          title: cleanTitle,
          subject: cleanSubject,
          condition,
          deal_type: dealType,
          price_vnd: dealType === "SALE" ? parsedPrice : null,
          description: description.trim(),
          seller_name: cleanSellerName,
          contact: cleanContact,
          image_path: uploadedImagePath,
          status: "AVAILABLE",
        });

      if (insertError) throw insertError;

      // Thành công: chuyển hướng về danh sách sách
      router.push("/books");
    } catch (err: any) {
      setErrorMsg(err.message || "Không thể đăng bài. Vui lòng kiểm tra lại thông tin.");
      setLoading(false);
    }
  };

  return (
    <PortalLayout>
      <div className="max-w-2xl mx-auto space-y-6">
        {/* Navigation Breadcrumb */}
        <Link
          href="/books"
          className="inline-flex items-center gap-1.5 text-xs font-semibold text-slate-500 hover:text-slate-900 transition"
        >
          <ArrowLeft className="w-3.5 h-3.5" />
          <span>Quay lại Chợ sách giáo trình</span>
        </Link>

        {/* Page Header */}
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">
            Đăng Sách Giáo Trình & Tài Liệu
          </h1>
          <p className="text-xs text-slate-500 mt-1">
            Chia sẻ hoặc nhượng lại giáo trình cũ cho các bạn sinh viên cùng trường UTH.
          </p>
        </div>

        {/* Form Container: Tonal Layering, No Borders, Concentric 12px -> 8px */}
        <form onSubmit={handleSubmit} className="bg-white rounded-xl p-6 shadow-subtle space-y-5">
          {errorMsg && (
            <div className="bg-red-50 text-red-800 rounded-lg p-3 text-xs flex items-start gap-2">
              <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
              <span>{errorMsg}</span>
            </div>
          )}

          {/* 1. Ảnh bìa sách */}
          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-2">
              Ảnh bìa hoặc trang sách (Tùy chọn)
            </label>

            {previewUrl ? (
              <div className="relative w-full aspect-[16/9] max-h-56 bg-slate-100 rounded-lg overflow-hidden">
                {/* eslint-disable-next-line @next/next/no-img-element */}
                <img
                  src={previewUrl}
                  alt="Ảnh xem trước"
                  className="w-full h-full object-contain"
                />
                <button
                  type="button"
                  onClick={removeSelectedFile}
                  className="absolute top-2 right-2 p-1.5 bg-slate-900/70 hover:bg-slate-900 text-white rounded-lg transition"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>
            ) : (
              <label className="flex flex-col items-center justify-center w-full aspect-[16/9] max-h-48 bg-[#F1F5F9] hover:bg-slate-200/60 rounded-lg cursor-pointer transition border border-dashed border-slate-300">
                <div className="flex flex-col items-center justify-center p-4 text-center">
                  <Upload className="w-6 h-6 stroke-[1.5] text-slate-400 mb-2" />
                  <p className="text-xs font-semibold text-slate-700">
                    Bấm để tải ảnh bìa giáo trình
                  </p>
                  <p className="text-[11px] text-slate-400 mt-0.5">
                    Hỗ trợ JPG, PNG, WebP (Tối đa 5MB)
                  </p>
                </div>
                <input
                  type="file"
                  accept="image/jpeg,image/png,image/webp"
                  onChange={handleFileChange}
                  className="hidden"
                />
              </label>
            )}
          </div>

          {/* 2. Tiêu đề sách */}
          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1.5">
              Tên sách / Giáo trình <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              required
              maxLength={160}
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="VD: Giáo trình Kỹ thuật lập trình C/C++ (Tái bản 2023)"
              className="w-full px-3 py-2.5 bg-[#F1F5F9] rounded-lg text-xs text-slate-900 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-[#0F5FC2]"
            />
          </div>

          {/* 3. Môn học / Khoa ngành */}
          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1.5">
              Môn học hoặc Ngành đào tạo <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              required
              maxLength={100}
              value={subject}
              onChange={(e) => setSubject(e.target.value)}
              placeholder="VD: Công nghệ thông tin, Cơ sở dữ liệu, Kinh tế lượng..."
              className="w-full px-3 py-2.5 bg-[#F1F5F9] rounded-lg text-xs text-slate-900 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-[#0F5FC2]"
            />
          </div>

          {/* 4. Hình thức giao dịch & Tình trạng sách */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1.5">
                Hình thức giao dịch
              </label>
              <select
                value={dealType}
                onChange={(e) => setDealType(e.target.value as any)}
                className="w-full px-3 py-2.5 bg-[#F1F5F9] rounded-lg text-xs text-slate-900 font-medium focus:outline-none focus:ring-2 focus:ring-[#0F5FC2]"
              >
                <option value="SALE">Bán lại (Có giá tiền)</option>
                <option value="EXCHANGE">Trao đổi sách / Cho tặng</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1.5">
                Tình trạng sách
              </label>
              <select
                value={condition}
                onChange={(e) => setCondition(e.target.value as any)}
                className="w-full px-3 py-2.5 bg-[#F1F5F9] rounded-lg text-xs text-slate-900 font-medium focus:outline-none focus:ring-2 focus:ring-[#0F5FC2]"
              >
                <option value="GOOD">Còn tốt (Gáy sách đẹp, ít nếp gấp)</option>
                <option value="NEW">Mới 100% (Chưa qua sử dụng)</option>
                <option value="USED">Đã dùng (Có highlight / ghi chú)</option>
              </select>
            </div>
          </div>

          {/* 5. Giá tiền (Chỉ hiện khi SALE) */}
          {dealType === "SALE" && (
            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1.5">
                Giá mong muốn (VNĐ) <span className="text-red-500">*</span>
              </label>
              <div className="relative">
                <input
                  type="number"
                  min={1000}
                  step={1000}
                  required
                  value={priceVnd}
                  onChange={(e) => setPriceVnd(e.target.value)}
                  placeholder="50000"
                  className="w-full pl-3 pr-14 py-2.5 bg-[#F1F5F9] rounded-lg text-xs font-bold text-slate-900 tabular-nums focus:outline-none focus:ring-2 focus:ring-[#0F5FC2]"
                />
                <span className="absolute right-3 top-1/2 -translate-y-1/2 text-xs text-slate-400 font-semibold pointer-events-none">
                  VNĐ
                </span>
              </div>
              <p className="text-[11px] text-slate-400 mt-1">
                Gợi ý: Giá từ 30% - 60% giá bìa sẽ giúp sách được sang tay nhanh nhất.
              </p>
            </div>
          )}

          {/* 6. Mô tả chi tiết */}
          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1.5">
              Mô tả thêm về sách
            </label>
            <textarea
              rows={3}
              maxLength={2000}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="VD: Sách in bản mới nhất, kèm đĩa CD bài tập, có thể hẹn nhận trực tiếp tại Cơ sở 1 Võ Oanh hoặc Cơ sở 2 Thủ Đức..."
              className="w-full px-3 py-2.5 bg-[#F1F5F9] rounded-lg text-xs text-slate-900 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-[#0F5FC2]"
            />
          </div>

          {/* 7. Thông tin người bán & Liên hệ */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-2">
            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1.5">
                Tên hiển thị người bán <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                required
                maxLength={100}
                value={sellerName}
                onChange={(e) => setSellerName(e.target.value)}
                placeholder="Tên hoặc biệt danh của bạn"
                className="w-full px-3 py-2.5 bg-[#F1F5F9] rounded-lg text-xs text-slate-900 focus:outline-none focus:ring-2 focus:ring-[#0F5FC2]"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1.5">
                Số điện thoại / Zalo <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                required
                maxLength={254}
                value={contact}
                onChange={(e) => setContact(e.target.value)}
                placeholder="VD: 0987654321 (Zalo)"
                className="w-full px-3 py-2.5 bg-[#F1F5F9] rounded-lg text-xs text-slate-900 focus:outline-none focus:ring-2 focus:ring-[#0F5FC2]"
              />
            </div>
          </div>

          {/* Actions */}
          <div className="pt-4 flex items-center justify-end gap-3 border-t border-slate-100">
            <Link
              href="/books"
              className="px-4 py-2 bg-[#F1F5F9] hover:bg-slate-200 text-slate-700 rounded-lg text-xs font-semibold transition"
            >
              Hủy bỏ
            </Link>

            <button
              type="submit"
              disabled={loading}
              className="inline-flex items-center gap-1.5 px-5 py-2.5 bg-[#0F5FC2] hover:bg-[#0c4ea3] disabled:opacity-50 text-white rounded-lg text-xs font-semibold transition shadow-subtle active:scale-[0.98]"
            >
              {loading ? (
                <>
                  <Loader2 className="w-3.5 h-3.5 animate-spin" />
                  <span>Đang đăng tin...</span>
                </>
              ) : (
                <span>Đăng sách lên chợ</span>
              )}
            </button>
          </div>
        </form>
      </div>
    </PortalLayout>
  );
}
