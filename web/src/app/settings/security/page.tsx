"use client";

import React, { useState } from "react";
import Link from "next/link";
import { PortalLayout } from "@/components/layout/PortalLayout";
import {
  Laptop,
  Smartphone,
  Monitor,
  Server,
  Tablet,
  CheckCircle2,
  AlertTriangle,
} from "lucide-react";

interface SessionItem {
  id: string;
  name: string;
  client: string;
  ip: string;
  location: string;
  lastActive: string;
  isCurrent?: boolean;
  isUnusual?: boolean;
  unusualNote?: string;
  icon: React.ElementType;
}

export default function SecuritySettingsPage() {
  const [sessions, setSessions] = useState<SessionItem[]>([
    {
      id: "1",
      name: "MacBook Pro",
      client: "Chrome 128",
      ip: "84.22.11.7",
      location: "Rotterdam, NL",
      lastActive: "active now",
      isCurrent: true,
      icon: Laptop,
    },
    {
      id: "2",
      name: "iPhone 15",
      client: "Northwind app",
      ip: "84.22.11.7",
      location: "Rotterdam, NL",
      lastActive: "2 hours ago",
      icon: Smartphone,
    },
    {
      id: "3",
      name: "Windows 11",
      client: "Firefox 129",
      ip: "203.0.113.44",
      location: "Warsaw, PL",
      lastActive: "6 hours ago",
      isUnusual: true,
      unusualNote: "Unusual location — you have not signed in from Poland before.",
      icon: Monitor,
    },
    {
      id: "4",
      name: "Linux",
      client: "CLI v4.2.1",
      ip: "10.4.8.19",
      location: "build runner",
      lastActive: "yesterday",
      icon: Server,
    },
    {
      id: "5",
      name: "iPad Air",
      client: "Safari 18",
      ip: "84.22.11.7",
      location: "Rotterdam, NL",
      lastActive: "12 days ago",
      icon: Tablet,
    },
  ]);

  const [feedback, setFeedback] = useState<string | null>(null);
  const feedbackTimerRef = React.useRef<NodeJS.Timeout | null>(null);

  React.useEffect(() => {
    return () => {
      if (feedbackTimerRef.current) clearTimeout(feedbackTimerRef.current);
    };
  }, []);

  const showFeedback = (msg: string) => {
    if (feedbackTimerRef.current) clearTimeout(feedbackTimerRef.current);
    setFeedback(msg);
    feedbackTimerRef.current = setTimeout(() => {
      setFeedback(null);
    }, 3000);
  };

  const handleSignOutSingle = (id: string, name: string) => {
    setSessions((prev) => prev.filter((s) => s.id !== id));
    showFeedback(`Signed out of ${name}.`);
  };

  const handleSignOutEverywhereElse = () => {
    setSessions((prev) => prev.filter((s) => s.isCurrent));
    showFeedback("Signed out of all other devices successfully.");
  };

  return (
    <PortalLayout>
      <section className="mx-auto max-w-2xl px-6 py-12">
        {/* Header */}
        <div className="flex flex-wrap items-end justify-between gap-3">
          <div>
            <h1 className="text-xl font-semibold tracking-tight text-neutral-900">
              Active sessions
            </h1>
            <p className="mt-1 text-sm text-neutral-500">
              You are signed in on {sessions.length} device{sessions.length > 1 ? "s" : ""}.
            </p>
          </div>
          {sessions.length > 1 && (
            <button
              type="button"
              onClick={handleSignOutEverywhereElse}
              className="rounded-xl border border-red-200 px-4 py-2 text-sm font-medium text-red-700 transition hover:bg-red-50 active:scale-[0.99]"
            >
              Sign out everywhere else
            </button>
          )}
        </div>

        {/* Feedback notification */}
        {feedback && (
          <div className="mt-4 flex items-center gap-2 rounded-xl bg-emerald-50 px-4 py-3 text-xs text-emerald-800 transition">
            <CheckCircle2 className="h-4 w-4 shrink-0 text-emerald-600" />
            <span>{feedback}</span>
          </div>
        )}

        {/* Sessions List Card */}
        <div className="mt-6 divide-y divide-neutral-100 overflow-hidden rounded-2xl border border-neutral-200 bg-white">
          {sessions.map((session) => {
            const Icon = session.icon;

            if (session.isUnusual) {
              return (
                <div
                  key={session.id}
                  className="relative flex items-center gap-4 p-5 pl-6"
                >
                  <span className="absolute inset-y-0 left-0 w-1 bg-amber-400"></span>
                  <span className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-amber-50 text-amber-600">
                    <Icon className="h-5 w-5 stroke-[1.8]" />
                  </span>
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-medium text-neutral-900">
                      {session.name} · {session.client}
                    </p>
                    <p className="mt-0.5 text-xs text-neutral-500">
                      {session.ip} · {session.location} · {session.lastActive}
                    </p>
                    <p className="mt-1 flex items-center gap-1 text-xs text-amber-700">
                      <AlertTriangle className="h-3.5 w-3.5 shrink-0" />
                      <span>{session.unusualNote}</span>
                    </p>
                  </div>
                  <button
                    type="button"
                    onClick={() => handleSignOutSingle(session.id, session.name)}
                    className="shrink-0 text-xs font-medium text-red-600 underline-offset-4 hover:underline"
                  >
                    Sign out
                  </button>
                </div>
              );
            }

            return (
              <div key={session.id} className="flex items-center gap-4 p-5">
                <span className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-neutral-100 text-neutral-700">
                  <Icon className="h-5 w-5 stroke-[1.8]" />
                </span>
                <div className="min-w-0 flex-1">
                  <p className="flex flex-wrap items-center gap-2 text-sm font-medium text-neutral-900">
                    {session.name} · {session.client}
                    {session.isCurrent && (
                      <span className="rounded-full bg-green-100 px-2 py-0.5 text-[10px] font-medium text-green-700">
                        This device
                      </span>
                    )}
                  </p>
                  <p className="mt-0.5 text-xs text-neutral-500">
                    {session.ip} · {session.location} · {session.lastActive}
                  </p>
                </div>
                {session.isCurrent ? (
                  <span className="shrink-0 text-xs text-neutral-400">Current</span>
                ) : (
                  <button
                    type="button"
                    onClick={() => handleSignOutSingle(session.id, session.name)}
                    className="shrink-0 text-xs font-medium text-neutral-600 underline-offset-4 hover:text-neutral-900 hover:underline"
                  >
                    Sign out
                  </button>
                )}
              </div>
            );
          })}
        </div>

        {/* Informational Footer Note */}
        <p className="mt-4 rounded-xl border border-neutral-200 bg-neutral-50 px-4 py-3 text-xs leading-relaxed text-neutral-500">
          Signing out revokes that device refresh token immediately — it will need the full sign-in flow again. API keys are separate and keep working; manage those on the{" "}
          <Link href="/settings/security" className="underline underline-offset-2 hover:text-neutral-900">
            API keys page
          </Link>
          .
        </p>
      </section>
    </PortalLayout>
  );
}
