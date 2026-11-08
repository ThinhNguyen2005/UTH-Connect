"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useGetIdentity, useLogout } from "@refinedev/core";
import {
  LayoutDashboard,
  Bell,
  Calendar,
  Home,
  Building2,
  ShieldCheck,
  ShieldAlert,
  ChevronDown,
  ChevronsLeft,
  Search,
  LogOut,
  GraduationCap,
  ExternalLink,
} from "lucide-react";
import type { AppUserIdentity } from "@/providers/auth-provider";

interface NavItem {
  id: string;
  name: string;
  href: string;
  icon: React.ElementType;
  count?: number | string;
  badgeColor?: string;
  adminOnly?: boolean;
}

interface NavGroup {
  id: string;
  label: string;
  items: NavItem[];
}

const STORAGE_KEY = "uth-sidebar-collapsed";

export const AppSidebar: React.FC = () => {
  const pathname = usePathname();
  const { data: user } = useGetIdentity<AppUserIdentity>();
  const { mutate: logout } = useLogout();

  const [isCollapsed, setIsCollapsed] = useState(false);
  const [openGroups, setOpenGroups] = useState<Record<string, boolean>>({
    workspace: true,
    insights: true,
  });
  const [searchTerm, setSearchTerm] = useState("");
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
    try {
      const saved = localStorage.getItem(STORAGE_KEY);
      if (saved === "1") {
        setIsCollapsed(true);
      }
    } catch {
      // LocalStorage disabled or SSR
    }
  }, []);

  const toggleSidebar = () => {
    setIsCollapsed((prev) => {
      const next = !prev;
      try {
        localStorage.setItem(STORAGE_KEY, next ? "1" : "0");
      } catch {
        // Ignored
      }
      return next;
    });
  };

  const toggleGroup = (groupId: string) => {
    setOpenGroups((prev) => ({
      ...prev,
      [groupId]: !prev[groupId],
    }));
  };

  const navGroups: NavGroup[] = [
    {
      id: "workspace",
      label: "Học tập & Hoạt động",
      items: [
        { id: "dashboard", name: "Tổng quan", href: "/", icon: LayoutDashboard },
        {
          id: "notifications",
          name: "Thông báo",
          href: "/notifications",
          icon: Bell,
          count: "Mới",
          badgeColor: "bg-blue-100 text-blue-700",
        },
        {
          id: "events",
          name: "Sự kiện & Vé QR",
          href: "/events",
          icon: Calendar,
        },
        {
          id: "housing",
          name: "Ghép trọ & KTX",
          href: "/housing",
          icon: Home,
        },
      ],
    },
    {
      id: "insights",
      label: "Tra cứu & Tiện ích",
      items: [
        {
          id: "campuses",
          name: "Cơ sở & Danh bạ",
          href: "/campuses",
          icon: Building2,
        },
        {
          id: "security",
          name: "Bảo mật & Phiên",
          href: "/settings/security",
          icon: ShieldCheck,
        },
        ...(user?.role === "admin"
          ? [
              {
                id: "admin",
                name: "Admin Console",
                href: "/admin",
                icon: ShieldAlert,
                count: "Admin",
                badgeColor: "bg-red-100 text-red-700 font-semibold",
                adminOnly: true,
              },
            ]
          : []),
      ],
    },
  ];

  return (
    <aside
      id="rail"
      className={`flex h-screen shrink-0 flex-col border-r border-slate-200/80 bg-white transition-all duration-200 sticky top-0 z-40 ${
        isCollapsed ? "w-[72px]" : "w-[260px]"
      }`}
    >
      {/* Top Header / Brand Mark */}
      <div className="flex items-center gap-3 px-4 py-4 border-b border-slate-100/60">
        <Link href="/" className="flex items-center gap-2.5 overflow-hidden">
          <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-slate-900 text-sm font-bold text-white shadow-sm">
            <GraduationCap className="h-5 w-5 stroke-[1.8]" />
          </div>
          {!isCollapsed && (
            <div data-label className="min-w-0 flex-1">
              <span className="block truncate text-sm font-bold text-slate-900 tracking-tight leading-tight">
                UTH Connect
              </span>
              <span className="block truncate text-[11px] text-slate-400 leading-tight">
                Cổng Sinh Viên
              </span>
            </div>
          )}
        </Link>

        <button
          id="collapse"
          type="button"
          onClick={toggleSidebar}
          aria-label={isCollapsed ? "Expand sidebar" : "Collapse sidebar"}
          className="flex h-7 w-7 items-center justify-center rounded-lg text-slate-400 transition hover:bg-slate-100 hover:text-slate-900 ml-auto shrink-0"
        >
          <ChevronsLeft
            className={`h-4 w-4 transition-transform duration-200 ${
              isCollapsed ? "rotate-180" : ""
            }`}
          />
        </button>
      </div>

      {/* Quick Search */}
      {!isCollapsed && (
        <div className="px-3 pt-3 pb-1" data-label>
          <div className="relative">
            <Search className="absolute left-2.5 top-2.5 h-3.5 w-3.5 text-slate-400" />
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="Tìm tính năng..."
              className="w-full rounded-lg border border-slate-200 bg-[#F8FAFC] pl-8 pr-3 py-1.5 text-xs outline-none transition focus:border-slate-900 focus:bg-white focus:ring-1 focus:ring-slate-900"
            />
          </div>
        </div>
      )}

      {/* Navigation List */}
      <nav id="nav" className="flex-1 overflow-y-auto px-3 py-2 space-y-4">
        {navGroups.map((group) => {
          const isOpen = openGroups[group.id] ?? true;

          // Filter by search term if active
          const filteredItems = group.items.filter((item) =>
            item.name.toLowerCase().includes(searchTerm.toLowerCase())
          );

          if (filteredItems.length === 0) return null;

          return (
            <div key={group.id} className="space-y-1">
              {!isCollapsed && (
                <button
                  type="button"
                  data-group={group.id}
                  aria-expanded={isOpen}
                  onClick={() => toggleGroup(group.id)}
                  data-label
                  className="flex w-full items-center justify-between px-2 pb-1 pt-2 text-[10px] font-bold uppercase tracking-wider text-slate-400 hover:text-slate-600 transition"
                >
                  <span>{group.label}</span>
                  <span
                    data-caret
                    className={`transition-transform duration-200 ${
                      isOpen ? "" : "-rotate-90"
                    }`}
                  >
                    <ChevronDown className="h-3 w-3" />
                  </span>
                </button>
              )}

              {(isOpen || isCollapsed) && (
                <ul id={group.id} className="space-y-0.5">
                  {filteredItems.map((item) => {
                    const isActive =
                      item.href === "/"
                        ? pathname === "/"
                        : pathname.startsWith(item.href);
                    const Icon = item.icon;

                    return (
                      <li key={item.id}>
                        <Link
                          href={item.href}
                          data-nav
                          className={`flex items-center gap-3 rounded-lg border-l-2 py-2 text-xs transition ${
                            isCollapsed ? "justify-center px-0" : "px-2.5"
                          } ${
                            isActive
                              ? "border-slate-900 bg-slate-100 font-semibold text-slate-900"
                              : "border-transparent text-slate-600 hover:bg-[#F8FAFC] hover:text-slate-900"
                          }`}
                          title={isCollapsed ? item.name : undefined}
                        >
                          <span className="w-4 shrink-0 text-center font-mono flex items-center justify-center">
                            <Icon className="h-4 w-4 stroke-[1.8]" />
                          </span>

                          {!isCollapsed && (
                            <>
                              <span data-label className="flex-1 truncate">
                                {item.name}
                              </span>
                              {item.count && (
                                <span
                                  data-label
                                  className={`rounded-full px-1.5 py-0.5 text-[10px] font-semibold leading-none ${
                                    item.badgeColor || "bg-slate-100 text-slate-600"
                                  }`}
                                >
                                  {item.count}
                                </span>
                              )}
                            </>
                          )}
                        </Link>
                      </li>
                    );
                  })}
                </ul>
              )}
            </div>
          );
        })}
      </nav>

      {/* User Chip Pinned to Bottom */}
      <div className="border-t border-slate-100 px-3 py-3 bg-[#F8FAFC]/50">
        <div
          className={`flex items-center ${
            isCollapsed ? "justify-center" : "gap-2.5"
          }`}
        >
          {/* Avatar */}
          <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-slate-900 text-xs font-bold text-white shadow-sm">
            {user?.name ? user.name.charAt(0).toUpperCase() : "U"}
          </div>

          {!isCollapsed && (
            <div data-label className="min-w-0 flex-1">
              <p className="truncate text-xs font-bold text-slate-900 leading-tight">
                {user?.name || "Sinh viên UTH"}
              </p>
              <p className="truncate text-[11px] text-slate-400 capitalize leading-tight">
                {user?.role === "admin"
                  ? "👑 Quản trị viên"
                  : user?.role === "lecturer"
                  ? "👨‍🏫 Giảng viên"
                  : "🎓 Sinh viên"}
              </p>
            </div>
          )}

          {mounted && user && !isCollapsed && (
            <button
              onClick={() => logout()}
              title="Đăng xuất"
              className="flex h-7 w-7 items-center justify-center rounded-lg text-slate-400 hover:bg-slate-200/70 hover:text-red-600 transition"
            >
              <LogOut className="h-3.5 w-3.5 stroke-[1.75]" />
            </button>
          )}
        </div>
      </div>
    </aside>
  );
};
