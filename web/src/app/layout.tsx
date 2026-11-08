import type { Metadata } from "next";
import "./globals.css";
import { RefineWrapper } from "@/components/RefineWrapper";

export const metadata: Metadata = {
  title: "UTH Connect — Cổng thông tin & Dashboard Quản trị UTH",
  description:
    "Hệ thống đa phân quyền dành cho Sinh viên, Giảng viên và Quản trị viên Trường Đại học Giao thông vận tải TP.HCM.",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="vi">
      <body className="bg-uth-slate-50 text-uth-slate-900 antialiased min-h-screen">
        <RefineWrapper>{children}</RefineWrapper>
      </body>
    </html>
  );
}
