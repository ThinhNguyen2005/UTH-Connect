"use client";

import React, { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { authProvider } from "@/providers/auth-provider";
import { ShieldAlert, Loader2, CheckCircle2, GraduationCap } from "lucide-react";

const UTH_FACULTIES = [
  "Viện Công nghệ Thông tin",
  "Khoa Kinh tế Vận tải",
  "Khoa Cơ khí",
  "Khoa Điện - Điện tử viễn thông",
  "Viện Hàng hải",
  "Khoa Kỹ thuật Xây dựng",
  "Khoa Vận tải & Logistics",
];

export default function RegisterPage() {
  const router = useRouter();

  const [studentId, setStudentId] = useState("");
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [faculty, setFaculty] = useState(UTH_FACULTIES[0]);
  const [major, setMajor] = useState("");
  const [className, setClassName] = useState("");
  const [phoneNumber, setPhoneNumber] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Auto-fill student email when MSSV changes if user hasn't typed a custom email
  const handleStudentIdChange = (val: string) => {
    setStudentId(val);
    const trimmed = val.trim();
    if (trimmed && /^\d+$/.test(trimmed) && (!email || email.includes("@ut.edu.vn"))) {
      setEmail(`${trimmed}@ut.edu.vn`);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);

    const cleanStudentId = studentId.trim();
    const cleanName = fullName.trim();
    const cleanEmail = email.trim();

    if (!cleanStudentId) {
      setErrorMessage("Vui lòng nhập Mã số sinh viên (MSSV).");
      return;
    }
    if (!cleanName) {
      setErrorMessage("Vui lòng nhập Họ và tên sinh viên.");
      return;
    }
    if (!cleanEmail) {
      setErrorMessage("Vui lòng nhập Email.");
      return;
    }
    if (!password) {
      setErrorMessage("Vui lòng nhập Mật khẩu.");
      return;
    }
    if (password.length < 6) {
      setErrorMessage("Mật khẩu phải có tối thiểu 6 ký tự.");
      return;
    }
    if (password !== confirmPassword) {
      setErrorMessage("Mật khẩu xác nhận không trùng khớp.");
      return;
    }

    setIsLoading(true);

    try {
      const res = await authProvider.register?.({
        email: cleanEmail,
        password,
        name: cleanName,
        studentId: cleanStudentId,
        faculty,
        major: major.trim(),
        className: className.trim(),
        phoneNumber: phoneNumber.trim(),
      });

      if (res?.success) {
        const dest = res.redirectTo || `/verify-email?email=${encodeURIComponent(cleanEmail)}`;
        router.push(dest);
      } else {
        setErrorMessage(res?.error?.message || "Đăng ký không thành công. Vui lòng thử lại.");
        setIsLoading(false);
      }
    } catch (err: any) {
      setErrorMessage(err?.message || "Đã xảy ra lỗi khi tạo tài khoản.");
      setIsLoading(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-[#F8FAFC] py-12 px-4 sm:px-6">
      <div className="w-full max-w-lg rounded-2xl border border-slate-200/80 bg-white p-7 sm:p-9 shadow-sm">
        {/* Header */}
        <div className="flex items-center gap-3 mb-6">
          <div className="h-10 w-10 rounded-xl bg-slate-900 flex items-center justify-center text-white">
            <GraduationCap className="h-5 w-5 stroke-[1.75]" />
          </div>
          <div>
            <h1 className="text-xl font-bold text-slate-900 tracking-tight">
              Tạo tài khoản sinh viên UTH
            </h1>
            <p className="text-xs text-slate-500 mt-0.5">
              Đã có tài khoản?{" "}
              <Link href="/login" className="font-semibold text-slate-900 hover:underline">
                Đăng nhập
              </Link>
            </p>
          </div>
        </div>

        {errorMessage && (
          <div className="mb-5 flex items-start gap-2.5 rounded-xl bg-red-50 p-3.5 text-xs text-red-700">
            <ShieldAlert className="mt-0.5 h-4 w-4 shrink-0 text-red-600" />
            <span>{errorMessage}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          {/* MSSV & Full name */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
            <div>
              <label className="mb-1 block text-xs font-semibold text-slate-700">
                Mã số sinh viên (MSSV) *
              </label>
              <input
                type="text"
                required
                value={studentId}
                onChange={(e) => handleStudentIdChange(e.target.value)}
                placeholder="vd: 2351050001"
                className="w-full rounded-xl border border-slate-200 bg-[#F8FAFC] px-3.5 py-2.5 text-xs outline-none transition focus:border-slate-900 focus:bg-white focus:ring-2 focus:ring-slate-900/10 font-mono"
              />
            </div>
            <div>
              <label className="mb-1 block text-xs font-semibold text-slate-700">
                Họ và tên sinh viên *
              </label>
              <input
                type="text"
                required
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                placeholder="Nguyễn Văn A"
                className="w-full rounded-xl border border-slate-200 bg-[#F8FAFC] px-3.5 py-2.5 text-xs outline-none transition focus:border-slate-900 focus:bg-white focus:ring-2 focus:ring-slate-900/10"
              />
            </div>
          </div>

          {/* Email */}
          <div>
            <label className="mb-1 block text-xs font-semibold text-slate-700">
              Email trường / Email cá nhân *
            </label>
            <input
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="mssv@ut.edu.vn"
              className="w-full rounded-xl border border-slate-200 bg-[#F8FAFC] px-3.5 py-2.5 text-xs outline-none transition focus:border-slate-900 focus:bg-white focus:ring-2 focus:ring-slate-900/10 font-mono"
            />
          </div>

          {/* Khoa / Viện */}
          <div>
            <label className="mb-1 block text-xs font-semibold text-slate-700">
              Khoa / Viện đào tạo *
            </label>
            <select
              value={faculty}
              onChange={(e) => setFaculty(e.target.value)}
              className="w-full rounded-xl border border-slate-200 bg-[#F8FAFC] px-3.5 py-2.5 text-xs outline-none transition focus:border-slate-900 focus:bg-white focus:ring-2 focus:ring-slate-900/10"
            >
              {UTH_FACULTIES.map((f) => (
                <option key={f} value={f}>
                  {f}
                </option>
              ))}
            </select>
          </div>

          {/* Chuyên ngành & Lớp */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
            <div>
              <label className="mb-1 block text-xs font-semibold text-slate-700">
                Chuyên ngành
              </label>
              <input
                type="text"
                value={major}
                onChange={(e) => setMajor(e.target.value)}
                placeholder="vd: Kỹ thuật phần mềm"
                className="w-full rounded-xl border border-slate-200 bg-[#F8FAFC] px-3.5 py-2.5 text-xs outline-none transition focus:border-slate-900 focus:bg-white focus:ring-2 focus:ring-slate-900/10"
              />
            </div>
            <div>
              <label className="mb-1 block text-xs font-semibold text-slate-700">
                Lớp sinh hoạt
              </label>
              <input
                type="text"
                value={className}
                onChange={(e) => setClassName(e.target.value)}
                placeholder="vd: CN23A"
                className="w-full rounded-xl border border-slate-200 bg-[#F8FAFC] px-3.5 py-2.5 text-xs outline-none transition focus:border-slate-900 focus:bg-white focus:ring-2 focus:ring-slate-900/10 uppercase"
              />
            </div>
          </div>

          {/* Số điện thoại */}
          <div>
            <label className="mb-1 block text-xs font-semibold text-slate-700">
              Số điện thoại liên lạc
            </label>
            <input
              type="tel"
              value={phoneNumber}
              onChange={(e) => setPhoneNumber(e.target.value)}
              placeholder="0912345678"
              className="w-full rounded-xl border border-slate-200 bg-[#F8FAFC] px-3.5 py-2.5 text-xs outline-none transition focus:border-slate-900 focus:bg-white focus:ring-2 focus:ring-slate-900/10"
            />
          </div>

          {/* Password & Confirm */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
            <div>
              <label className="mb-1 block text-xs font-semibold text-slate-700">
                Mật khẩu *
              </label>
              <input
                type="password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                className="w-full rounded-xl border border-slate-200 bg-[#F8FAFC] px-3.5 py-2.5 text-xs outline-none transition focus:border-slate-900 focus:bg-white focus:ring-2 focus:ring-slate-900/10"
              />
            </div>
            <div>
              <label className="mb-1 block text-xs font-semibold text-slate-700">
                Xác nhận mật khẩu *
              </label>
              <input
                type="password"
                required
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                placeholder="••••••••"
                className="w-full rounded-xl border border-slate-200 bg-[#F8FAFC] px-3.5 py-2.5 text-xs outline-none transition focus:border-slate-900 focus:bg-white focus:ring-2 focus:ring-slate-900/10"
              />
            </div>
          </div>
          <p className="text-[11px] text-slate-400">
            Sử dụng tối thiểu 6 ký tự kết hợp chữ cái và số để đảm bảo an toàn.
          </p>

          <button
            type="submit"
            disabled={isLoading}
            className="w-full mt-2 rounded-xl bg-slate-900 py-3 text-xs font-semibold text-white transition hover:bg-slate-800 disabled:opacity-50 flex items-center justify-center gap-2 active:scale-[0.99] shadow-sm"
          >
            {isLoading ? (
              <>
                <Loader2 className="h-4 w-4 animate-spin" />
                <span>Đang khởi tạo tài khoản...</span>
              </>
            ) : (
              <span>Đăng ký tài khoản sinh viên</span>
            )}
          </button>
        </form>

        <p className="mt-6 text-center text-[11px] leading-relaxed text-slate-400">
          Bằng việc đăng ký, bạn đồng ý với{" "}
          <a href="#" className="underline hover:text-slate-600">
            Quy chế sinh viên UTH
          </a>{" "}
          và{" "}
          <a href="#" className="underline hover:text-slate-600">
            Chính sách bảo mật
          </a>
          .
        </p>
      </div>
    </div>
  );
}
