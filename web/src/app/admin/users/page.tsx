"use client";

import React, { useEffect, useState } from "react";
import { AdminLayout } from "@/components/layout/AdminLayout";
import { supabaseBrowserClient } from "@/utils/supabase-client";
import { Users, CheckCircle, RefreshCw, Shield, GraduationCap, School } from "lucide-react";
import type { AppUserRole } from "@/providers/auth-provider";

export default function AdminUsersPage() {
  const [users, setUsers] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState<string | null>(null);
  const [toast, setToast] = useState<string | null>(null);

  const fetchUsers = async () => {
    setLoading(true);
    const { data, error } = await supabaseBrowserClient
      .from("profiles")
      .select("id, email, display_name, student_id, student_id_verified, role, created_at")
      .order("created_at", { ascending: false });

    if (!error && data) {
      setUsers(data);
    }
    setLoading(false);
  };

  useEffect(() => {
    let isMounted = true;
    const fetchInitial = async () => {
      setLoading(true);
      const { data, error } = await supabaseBrowserClient
        .from("profiles")
        .select("id, email, display_name, student_id, student_id_verified, role, created_at")
        .order("created_at", { ascending: false });

      if (isMounted) {
        if (!error && data) {
          setUsers(data);
        }
        setLoading(false);
      }
    };
    fetchInitial();
    return () => {
      isMounted = false;
    };
  }, []);

  const handleRoleChange = async (userId: string, newRole: AppUserRole) => {
    setActionLoading(userId);
    setToast(null);

    const { error } = await supabaseBrowserClient.rpc("admin_set_user_role", {
      p_user_id: userId,
      p_role: newRole,
    });

    setActionLoading(null);

    if (error) {
      setToast(`Lỗi: ${error.message}`);
    } else {
      setToast(`Đã cập nhật vai trò người dùng thành "${newRole}" thành công.`);
      fetchUsers();
    }
  };

  const handleVerifyStudentId = async (userId: string, studentId: string) => {
    if (!studentId) {
      alert("Người dùng chưa cập nhật MSSV.");
      return;
    }

    setActionLoading(userId);
    const { error } = await supabaseBrowserClient.rpc("admin_verify_student", {
      p_user_id: userId,
      p_student_id: studentId,
    });
    setActionLoading(null);

    if (error) {
      setToast(`Lỗi xác thực: ${error.message}`);
    } else {
      setToast(`Đã duyệt MSSV ${studentId} thành công.`);
      fetchUsers();
    }
  };

  return (
    <AdminLayout>
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
        <div>
          <h1 className="text-xl font-bold text-slate-900 tracking-tight">
            Quản Lý Thành Viên & Phân Quyền (RBAC)
          </h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Cấp quyền Giảng viên, Sinh viên, Quản trị viên và xác minh MSSV chính thức
          </p>
        </div>

        <button
          onClick={fetchUsers}
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

      {/* Clean Tonal Table (No outer borders, subtle shadow, 12px rounded) */}
      <div className="bg-white rounded-xl shadow-subtle overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-[#F8FAFC] text-slate-400 font-semibold uppercase tracking-wider">
              <tr>
                <th className="py-3 px-5">Người dùng</th>
                <th className="py-3 px-5">Mã số SV (MSSV)</th>
                <th className="py-3 px-5">Vai trò hiện tại</th>
                <th className="py-3 px-5 text-right">Hành động Phân quyền</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {loading ? (
                <tr>
                  <td colSpan={4} className="py-8 text-center text-slate-400">
                    Đang tải danh sách...
                  </td>
                </tr>
              ) : users.length === 0 ? (
                <tr>
                  <td colSpan={4} className="py-8 text-center text-slate-400">
                    Chưa có tài khoản nào.
                  </td>
                </tr>
              ) : (
                users.map((u) => (
                  <tr key={u.id} className="hover:bg-[#F8FAFC]/80 transition">
                    <td className="py-3.5 px-5">
                      <div className="font-semibold text-slate-900">
                        {u.display_name || "Chưa đặt tên"}
                      </div>
                      <div className="text-[11px] text-slate-400 mt-0.5">{u.email}</div>
                    </td>

                    <td className="py-3.5 px-5">
                      {u.student_id ? (
                        <div className="flex items-center gap-2">
                          <span className="font-mono text-xs font-semibold text-slate-800 bg-slate-100 px-2 py-0.5 rounded">
                            {u.student_id}
                          </span>
                          {u.student_id_verified ? (
                            <span className="inline-flex items-center gap-1 text-[10px] font-semibold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded">
                              <CheckCircle className="w-3 h-3" /> Đã duyệt
                            </span>
                          ) : (
                            <button
                              onClick={() => handleVerifyStudentId(u.id, u.student_id)}
                              disabled={actionLoading === u.id}
                              className="text-[10px] font-semibold text-amber-800 bg-amber-100 hover:bg-amber-200 px-2 py-0.5 rounded transition"
                            >
                              Duyệt MSSV
                            </button>
                          )}
                        </div>
                      ) : (
                        <span className="text-slate-400">Chưa có</span>
                      )}
                    </td>

                    <td className="py-3.5 px-5">
                      <span
                        className={`inline-flex items-center gap-1 px-2 py-0.5 rounded text-[11px] font-semibold ${
                          u.role === "admin"
                            ? "bg-red-50 text-red-700"
                            : u.role === "lecturer" || u.role === "organizer"
                            ? "bg-blue-50 text-blue-700"
                            : "bg-emerald-50 text-emerald-700"
                        }`}
                      >
                        {u.role === "admin" && "Admin"}
                        {(u.role === "lecturer" || u.role === "organizer") && "Giảng viên / BTC"}
                        {u.role === "student" && "Sinh viên"}
                      </span>
                    </td>

                    <td className="py-3.5 px-5 text-right">
                      <select
                        value={u.role}
                        disabled={actionLoading === u.id}
                        onChange={(e) => handleRoleChange(u.id, e.target.value as AppUserRole)}
                        className="text-xs bg-[#F8FAFC] rounded-lg px-2.5 py-1.5 focus:outline-none focus:ring-1 focus:ring-slate-900 transition"
                      >
                        <option value="student">Gán: Sinh viên</option>
                        <option value="organizer">Gán: Ban tổ chức</option>
                        <option value="lecturer">Gán: Giảng viên</option>
                        <option value="admin">Gán: Admin</option>
                      </select>
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
