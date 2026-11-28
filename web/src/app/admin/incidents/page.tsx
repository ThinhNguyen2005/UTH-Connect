"use client";

import React, { useCallback, useEffect, useRef, useState } from "react";
import { AdminLayout } from "@/components/layout/AdminLayout";
import { supabaseBrowserClient } from "@/utils/supabase-client";

type Incident = {
  id: string; code: string; campus: string; room_code: string; title: string;
  description: string; severity: string; status: string; row_version: number;
  reporter_id: string; image_paths: string[]; created_at: string; assigned_unit: string | null;
};

const labels: Record<string, string> = {
  SUBMITTED: "Mới gửi", ACKNOWLEDGED: "Đã tiếp nhận", IN_PROGRESS: "Đang xử lý",
  RESOLVED: "Đã khắc phục", REOPENED: "Mở lại", CLOSED: "Đã đóng",
  REJECTED: "Từ chối", DUPLICATE: "Trùng lặp", CANCELLED: "Đã hủy",
};
const options: Record<string, string[]> = {
  SUBMITTED: ["ACKNOWLEDGED", "REJECTED", "DUPLICATE"],
  ACKNOWLEDGED: ["IN_PROGRESS"], REOPENED: ["IN_PROGRESS"], IN_PROGRESS: ["RESOLVED"],
};
const input = "w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900";

// Excel treats fields beginning with formula characters as executable formulas.
function csvCell(value: string): string {
  const safe = /^[\s\u0000-\u001f]*[=+@-]/.test(value) ? `'${value}` : value;
  return `"${safe.replace(/"/g, '""')}"`;
}

function IncidentsContent() {
  const [items, setItems] = useState<Incident[]>([]);
  const [selected, setSelected] = useState<Incident | null>(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState("");
  const [campus, setCampus] = useState("");
  const [severity, setSeverity] = useState("");
  const [target, setTarget] = useState("");
  const [note, setNote] = useState("");
  const [duplicateId, setDuplicateId] = useState("");
  const [assignedUnit, setAssignedUnit] = useState("");
  const running = useRef(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const { data, error: loadError } = await supabaseBrowserClient.from("incident_reports")
        .select("id,code,campus,room_code,title,description,severity,status,row_version,reporter_id,image_paths,created_at,assigned_unit")
        .order("created_at", { ascending: false }).limit(200);
      if (loadError) throw loadError;
      setItems((data || []) as Incident[]); setError("");
    } catch {
      setError("Không tải được báo cáo sự cố. Vui lòng thử lại.");
    } finally { setLoading(false); }
  }, []);

  useEffect(() => { void load(); }, [load]);
  const filtered = items.filter(item => {
    const query = search.trim().toLocaleLowerCase("vi");
    return (!status || item.status === status) && (!campus || item.campus === campus) &&
      (!severity || item.severity === severity) &&
      (!query || `${item.code} ${item.room_code} ${item.title}`.toLocaleLowerCase("vi").includes(query));
  });

  async function changeStatus() {
    if (!selected || !target || running.current) return;
    if (["REJECTED", "DUPLICATE", "RESOLVED"].includes(target) && !note.trim()) {
      setError("Vui lòng nhập ghi chú hoặc lý do xử lý."); return;
    }
    if (target === "DUPLICATE" && !duplicateId.trim()) {
      setError("Vui lòng nhập ID của báo cáo gốc."); return;
    }
    running.current = true; setBusy(true); setError(""); setMessage("");
    try {
      const { error: updateError } = await supabaseBrowserClient.rpc("transition_incident_report", {
        p_incident_id: selected.id, p_expected_version: selected.row_version,
        p_target_status: target, p_note: note.trim() || null,
        p_duplicate_of_id: target === "DUPLICATE" ? duplicateId.trim() : null,
      });
      if (updateError) {
        if (updateError.message.includes("INCIDENT_CONFLICT")) {
          setSelected(null); await load(); setError("Phiếu đã được cập nhật bởi người khác. Vui lòng mở lại và kiểm tra.");
        } else setError("Không cập nhật được trạng thái. Kiểm tra thông tin và thử lại.");
        return;
      }
      setSelected(null); setTarget(""); setNote(""); setDuplicateId("");
      await load(); setMessage("Đã cập nhật trạng thái báo cáo.");
    } catch {
      setError("Không thể kết nối để cập nhật. Vui lòng thử lại.");
    } finally { running.current = false; setBusy(false); }
  }

  async function assignReport() {
    if (!selected || running.current) return;
    if (!assignedUnit.trim()) { setError("Vui lòng nhập đơn vị xử lý."); return; }
    running.current = true; setBusy(true); setError(""); setMessage("");
    try {
      const { error: assignError } = await supabaseBrowserClient.rpc("assign_incident_report", {
        p_incident_id: selected.id, p_expected_version: selected.row_version,
        p_assigned_to: null, p_assigned_unit: assignedUnit.trim(),
      });
      if (assignError) {
        if (assignError.message.includes("INCIDENT_CONFLICT")) {
          setSelected(null); await load(); setError("Phiếu đã được cập nhật bởi người khác. Vui lòng mở lại và kiểm tra.");
        } else setError("Không phân công được báo cáo. Vui lòng thử lại.");
        return;
      }
      setSelected(null); setAssignedUnit(""); await load(); setMessage("Đã phân công báo cáo.");
    } catch {
      setError("Không thể kết nối để phân công. Vui lòng thử lại.");
    } finally { running.current = false; setBusy(false); }
  }

  function exportCsv() {
    const rows = [["Mã phiếu", "Cơ sở", "Phòng", "Tiêu đề", "Mức độ", "Trạng thái", "Thời gian"],
      ...filtered.map(i => [i.code, i.campus, i.room_code, i.title, i.severity, labels[i.status] || i.status, i.created_at])];
    const content = "\ufeff" + rows.map(row => row.map(csvCell).join(",")).join("\r\n");
    const url = URL.createObjectURL(new Blob([content], { type: "text/csv;charset=utf-8" }));
    const anchor = document.createElement("a"); anchor.href = url; anchor.download = "bao-cao-su-co.csv"; anchor.click();
    URL.revokeObjectURL(url);
  }

  return <div className="space-y-6">
    <header className="flex flex-wrap items-end justify-between gap-3">
      <div><h1 className="text-2xl font-bold text-slate-900">Báo cáo sự cố</h1>
        <p className="mt-1 text-sm text-slate-600">Tiếp nhận và theo dõi các sự cố cơ sở vật chất.</p></div>
      <button type="button" onClick={exportCsv} disabled={!filtered.length} className="rounded-lg bg-slate-900 px-4 py-2 text-sm text-white disabled:opacity-50">Xuất CSV đang hiển thị</button>
    </header>
    {error && <div role="alert" className="rounded-lg bg-red-50 p-3 text-sm text-red-800">{error}</div>}
    {message && <div role="status" className="rounded-lg bg-emerald-50 p-3 text-sm text-emerald-800">{message}</div>}
    <section aria-label="Bộ lọc báo cáo" className="grid gap-3 rounded-xl bg-white p-4 shadow-sm md:grid-cols-4">
      <label className="text-sm">Tìm mã, phòng hoặc tiêu đề<input className={input} value={search} onChange={e => setSearch(e.target.value)} /></label>
      <label className="text-sm">Trạng thái<select className={input} value={status} onChange={e => setStatus(e.target.value)}><option value="">Tất cả</option>{Object.entries(labels).map(([key, value]) => <option key={key} value={key}>{value}</option>)}</select></label>
      <label className="text-sm">Cơ sở<select className={input} value={campus} onChange={e => setCampus(e.target.value)}><option value="">Tất cả</option>{Array.from(new Set(items.map(i => i.campus))).sort().map(value => <option key={value}>{value}</option>)}</select></label>
      <label className="text-sm">Mức độ<select className={input} value={severity} onChange={e => setSeverity(e.target.value)}><option value="">Tất cả</option><option value="LOW">Thấp</option><option value="MEDIUM">Trung bình</option><option value="HIGH">Cao</option><option value="CRITICAL">Khẩn cấp</option></select></label>
    </section>
    <section className="rounded-xl bg-white p-4 shadow-sm" aria-label="Danh sách báo cáo">
      <button type="button" onClick={() => void load()} disabled={loading} className="mb-3 text-sm text-blue-700 underline disabled:opacity-50">Tải lại</button>
      <p className="mb-3 text-xs text-slate-500">Hiển thị tối đa 200 báo cáo gần nhất. Có {filtered.length} kết quả theo bộ lọc.</p>
      {loading ? <p>Đang tải báo cáo…</p> : filtered.length === 0 ? <p>Không có báo cáo phù hợp.</p> :
        <div className="overflow-x-auto"><table className="w-full text-left text-sm"><thead><tr className="border-b text-slate-600"><th className="p-2">Mã / phòng</th><th className="p-2">Tiêu đề</th><th className="p-2">Mức độ</th><th className="p-2">Trạng thái</th><th className="p-2"><span className="sr-only">Chi tiết</span></th></tr></thead>
          <tbody>{filtered.map(item => <tr key={item.id} className="border-b border-slate-100"><td className="p-2"><span className="font-medium">{item.code}</span><br />{item.campus} · {item.room_code}</td><td className="p-2">{item.title}</td><td className="p-2">{item.severity}</td><td className="p-2">{labels[item.status] || item.status}</td><td className="p-2"><button type="button" className="text-blue-700 underline" onClick={() => { setSelected(item); setTarget(""); setNote(""); setDuplicateId(""); setAssignedUnit(item.assigned_unit || ""); setError(""); }}>Xem</button></td></tr>)}</tbody></table></div>}
    </section>
    {selected && <section className="space-y-4 rounded-xl bg-white p-5 shadow-sm" aria-label="Chi tiết báo cáo">
      <div className="flex justify-between gap-3"><h2 className="text-lg font-semibold">{selected.code} · {selected.title}</h2><button type="button" onClick={() => setSelected(null)} className="text-sm underline">Đóng chi tiết</button></div>
      <p className="whitespace-pre-wrap text-sm">{selected.description}</p>
      <p className="text-xs text-slate-500">Người gửi: {selected.reporter_id} · Phiên bản: {selected.row_version}</p>
      {["SUBMITTED", "ACKNOWLEDGED", "IN_PROGRESS", "REOPENED"].includes(selected.status) && <div className="flex flex-wrap items-end gap-3">
        <label className="flex-1 text-sm">Đơn vị xử lý<input className={input} maxLength={200} value={assignedUnit} onChange={e => setAssignedUnit(e.target.value)} /></label>
        <button type="button" onClick={() => void assignReport()} disabled={busy} className="rounded-lg border border-slate-300 px-4 py-2 text-sm disabled:opacity-50">Phân công</button>
      </div>}
      {selected.image_paths.length > 0 && <div className="flex flex-wrap gap-3">{selected.image_paths.map(path => {
        const url = supabaseBrowserClient.storage.from("media_uploads").getPublicUrl(path).data.publicUrl;
        return <a key={path} href={url} target="_blank" rel="noreferrer" className="text-sm text-blue-700 underline">Xem ảnh hiện trạng</a>;
      })}</div>}
      {(options[selected.status] || []).length > 0 && <div className="grid gap-3 md:grid-cols-2">
        <label className="text-sm">Chuyển trạng thái<select className={input} value={target} onChange={e => setTarget(e.target.value)}><option value="">Chọn trạng thái</option>{options[selected.status].map(value => <option key={value} value={value}>{labels[value]}</option>)}</select></label>
        <label className="text-sm">Ghi chú hoặc lý do<input className={input} maxLength={1000} value={note} onChange={e => setNote(e.target.value)} /></label>
        {target === "DUPLICATE" && <label className="text-sm">ID báo cáo gốc<input className={input} value={duplicateId} onChange={e => setDuplicateId(e.target.value)} /></label>}
        <div className="flex items-end"><button type="button" onClick={() => void changeStatus()} disabled={busy || !target} className="rounded-lg bg-slate-900 px-4 py-2 text-sm text-white disabled:opacity-50">{busy ? "Đang cập nhật…" : "Cập nhật trạng thái"}</button></div>
      </div>}
    </section>}
  </div>;
}

export default function AdminIncidentsPage() {
  return <AdminLayout><IncidentsContent /></AdminLayout>;
}
