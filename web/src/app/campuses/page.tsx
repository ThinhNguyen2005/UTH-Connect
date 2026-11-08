"use client";

import React from "react";
import { PortalLayout } from "@/components/layout/PortalLayout";
import { Building2, MapPin, Phone, Mail, ExternalLink } from "lucide-react";

export default function CampusesDirectoryPage() {
  const campuses = [
    {
      name: "Trụ sở chính (Cơ sở 1)",
      address: "Số 2, Đường Võ Oanh, Phường 25, Quận Bình Thạnh, TP. Hồ Chí Minh",
      phone: "028 3899 1373",
      email: "ut-hcmc@ut.edu.vn",
      mapUrl: "https://maps.google.com/?q=Trường+Đại+học+Giao+thông+vận+tải+TP.HCM+Võ+Oanh",
      departments: [
        "Ban Giám Hiệu",
        "Phòng Đào Tạo & Khảo Thí",
        "Phòng Công Tác Sinh Viên",
        "Khoa Công Nghệ Thông Tin",
        "Thư Viện Trung Tâm",
      ],
    },
    {
      name: "Cơ sở 2 (Khuôn viên Công nghệ & Thực hành)",
      address: "Số 10, Đường Tô Ký, Phường Tân Chánh Hiệp, Quận 12, TP. Hồ Chí Minh",
      phone: "028 3883 2341",
      email: "cs2@ut.edu.vn",
      mapUrl: "https://maps.google.com/?q=Đại+học+Giao+thông+vận+tải+TP.HCM+Cơ+sở+2",
      departments: [
        "Xưởng Thực hành Cơ khí & Tự động hóa",
        "Trung tâm Đào tạo Logistics",
        "Sân Vận Động & Thể Thao UTH",
        "Khu Ký Túc Xá Sinh Viên CS2",
      ],
    },
    {
      name: "Cơ sở 3 (Khuôn viên Mở rộng)",
      address: "Số 70, Đường Tô Ký, Phường Tân Chánh Hiệp, Quận 12, TP. Hồ Chí Minh",
      phone: "028 3883 2342",
      email: "cs3@ut.edu.vn",
      mapUrl: "https://maps.google.com/?q=Đại+học+Giao+thông+vận+tải+TP.HCM+Cơ+sở+3",
      departments: [
        "Khoa Kinh Tế Vận Tải",
        "Trung tâm Khởi nghiệp & Đổi mới Sáng tạo",
        "Hội trường Đa năng",
      ],
    },
  ];

  return (
    <PortalLayout>
      <div className="mb-6">
        <h1 className="text-xl font-bold text-slate-900 tracking-tight">
          Cơ Sở & Danh Bạ Liên Hệ UTH
        </h1>
        <p className="text-xs text-slate-500 mt-0.5">
          Thông tin địa chỉ 3 cơ sở đào tạo, số điện thoại hotline và phòng ban hỗ trợ sinh viên
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
        {campuses.map((c, i) => (
          <div
            key={i}
            className="bg-white rounded-xl p-5 shadow-subtle flex flex-col justify-between hover:bg-[#F8FAFC]/50 transition"
          >
            <div>
              <div className="text-[11px] font-semibold uppercase tracking-wider text-slate-400 mb-1">
                Khuôn viên CS{i + 1}
              </div>

              <h2 className="text-sm font-bold text-slate-900 mb-2">
                {c.name}
              </h2>

              <div className="space-y-1.5 text-xs text-slate-500 mb-4">
                <div className="flex items-start gap-1.5">
                  <MapPin className="w-3.5 h-3.5 shrink-0 mt-0.5 text-slate-400" />
                  <span className="leading-relaxed">{c.address}</span>
                </div>
                <div className="flex items-center gap-1.5">
                  <Phone className="w-3.5 h-3.5 shrink-0 text-slate-400" />
                  <a href={`tel:${c.phone}`} className="hover:text-slate-900 tabular-nums">
                    {c.phone}
                  </a>
                </div>
                <div className="flex items-center gap-1.5">
                  <Mail className="w-3.5 h-3.5 shrink-0 text-slate-400" />
                  <a href={`mailto:${c.email}`} className="hover:text-slate-900">
                    {c.email}
                  </a>
                </div>
              </div>

              <div className="pt-3 border-t border-slate-100">
                <div className="text-[11px] font-semibold text-slate-700 mb-2">
                  Đơn vị trọng điểm:
                </div>
                <ul className="space-y-1 text-xs text-slate-500">
                  {c.departments.map((dept, idx) => (
                    <li key={idx} className="flex items-center gap-1.5">
                      <span className="w-1 h-1 rounded-full bg-slate-400" />
                      <span>{dept}</span>
                    </li>
                  ))}
                </ul>
              </div>
            </div>

            <div className="pt-4 mt-4 border-t border-slate-100">
              <a
                href={c.mapUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="w-full inline-flex items-center justify-center gap-1.5 py-2 px-3 rounded-lg bg-[#F8FAFC] text-xs font-semibold text-slate-700 hover:bg-slate-100 transition active:scale-[0.98]"
              >
                <span>Xem bản đồ Google Maps</span>
                <ExternalLink className="w-3.5 h-3.5 stroke-[1.5]" />
              </a>
            </div>
          </div>
        ))}
      </div>
    </PortalLayout>
  );
}
