"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";
import { supabaseBrowserClient } from "@/utils/supabase-client";

export default function VerifyEmailPage() {
  const [email, setEmail] = useState("");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [sends, setSends] = useState(0);
  const [wait, setWait] = useState(30);
  const [left, setLeft] = useState(0);
  const [isResending, setIsResending] = useState(false);

  useEffect(() => {
    if (typeof window !== "undefined") {
      const params = new URLSearchParams(window.location.search);
      const queryEmail = params.get("email")?.trim() || "";
      const alreadySent = params.get("sent") === "1";
      if (queryEmail) {
        setEmail(queryEmail);
        if (alreadySent) {
          setSends(1);
          setLeft(30);
        }
      }
    }
  }, []);

  useEffect(() => {
    if (left <= 0) return;
    const interval = setInterval(() => {
      setLeft((prev) => prev - 1);
    }, 1000);
    return () => clearInterval(interval);
  }, [left]);

  const handleResend = async () => {
    if (left > 0 || isResending) return;

    const cleanEmail = email.trim();
    if (!cleanEmail) {
      setErrorMessage("Vui lòng nhập email trường hoặc MSSV để gửi liên kết xác thực.");
      return;
    }

    const targetEmail = cleanEmail.includes("@") ? cleanEmail : `${cleanEmail}@ut.edu.vn`;

    setIsResending(true);
    setErrorMessage(null);

    try {
      const origin =
        typeof window !== "undefined"
          ? window.location.origin
          : "https://uth-connect-web-git-main-nguyenthinhk52005-5982.vercel.app";

      const { error } = await supabaseBrowserClient.auth.resend({
        type: "signup",
        email: targetEmail,
        options: {
          emailRedirectTo: `${origin}/auth/callback`,
        },
      });
      if (error) {
        setErrorMessage(error.message || "Không thể gửi email xác thực. Vui lòng thử lại.");
      } else {
        setEmail(targetEmail);
        // Each retry costs a little more patience than the last
        const nextWait = sends === 0 ? 30 : Math.min(wait * 2, 120);
        setSends((prev) => prev + 1);
        setWait(nextWait);
        setLeft(nextWait);
        setErrorMessage(null);
      }
    } catch {
      setErrorMessage("Không thể kết nối đến máy chủ.");
    } finally {
      setIsResending(false);
    }
  };

  const isLocked = left > 0 || isResending || !email.trim();

  return (
    <div className="flex min-h-screen items-center justify-center bg-neutral-50 px-6 py-14">
      <div className="w-full max-w-sm rounded-2xl border border-neutral-200 bg-white p-7 text-center shadow-sm">
        {/* Envelope Tile */}
        <div className="mx-auto mb-5 grid h-12 w-12 place-items-center rounded-full bg-neutral-100 text-neutral-700">
          <svg
            aria-hidden="true"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.8"
            strokeLinecap="round"
            strokeLinejoin="round"
            className="h-5 w-5"
          >
            <rect x="3" y="5" width="18" height="14" rx="2" />
            <path d="m4 7 8 6 8-6" />
          </svg>
        </div>

        {sends > 0 ? (
          <>
            <h1 className="text-lg font-semibold text-neutral-900">Check your inbox</h1>
            <p className="mt-1.5 text-sm leading-relaxed text-neutral-500">
              We sent a verification link to{" "}
              <span className="font-medium text-neutral-900">{email}</span>
              <Link
                href="/register"
                className="ml-1 text-neutral-500 underline underline-offset-4 hover:text-neutral-900"
              >
                change
              </Link>
            </p>
          </>
        ) : (
          <>
            <h1 className="text-lg font-semibold text-neutral-900">Verify your email</h1>
            <p className="mt-1.5 text-sm leading-relaxed text-neutral-500">
              Enter your student email or Student ID to receive a verification link.
            </p>
            <div className="mt-4 text-left">
              <input
                type="text"
                value={email}
                onChange={(e) => {
                  setEmail(e.target.value);
                  setErrorMessage(null);
                }}
                placeholder="you@ut.edu.vn or 2151050000"
                aria-label="Student email or Student ID"
                className="w-full rounded-xl border border-neutral-200 px-3.5 py-2.5 text-sm outline-none ring-neutral-900/10 focus:border-neutral-900 focus:ring-4 transition"
              />
            </div>
          </>
        )}

        {errorMessage && (
          <div
            role="alert"
            className="mt-4 rounded-xl bg-red-50 p-3 text-left text-xs text-red-700"
          >
            {errorMessage}
          </div>
        )}

        {/* Hints Checklist */}
        <ul className="mt-6 space-y-2 rounded-xl bg-neutral-50 p-4 text-left text-xs text-neutral-600">
          <li className="flex items-start gap-2">
            <svg
              aria-hidden="true"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="1.8"
              strokeLinecap="round"
              strokeLinejoin="round"
              className="mt-0.5 h-3.5 w-3.5 shrink-0 text-neutral-700"
            >
              <path d="m5 12.5 4.25 4.25L19 7" />
            </svg>
            <span>The link signs you in once, then stops working.</span>
          </li>
          <li className="flex items-start gap-2">
            <svg
              aria-hidden="true"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="1.8"
              strokeLinecap="round"
              strokeLinejoin="round"
              className="mt-0.5 h-3.5 w-3.5 shrink-0 text-neutral-700"
            >
              <path d="m5 12.5 4.25 4.25L19 7" />
            </svg>
            <span>It expires 60 minutes after it was sent.</span>
          </li>
          <li className="flex items-start gap-2">
            <svg
              aria-hidden="true"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="1.8"
              strokeLinecap="round"
              strokeLinejoin="round"
              className="mt-0.5 h-3.5 w-3.5 shrink-0 text-neutral-700"
            >
              <path d="m5 12.5 4.25 4.25L19 7" />
            </svg>
            <span>You can close this tab — we will pick up where you left off.</span>
          </li>
        </ul>

        {/* Resend Button */}
        <button
          id="resendBtn"
          type="button"
          onClick={handleResend}
          disabled={isLocked}
          className={`mt-6 w-full rounded-xl border border-neutral-200 py-2.5 text-sm font-medium transition ${
            isLocked
              ? "cursor-not-allowed text-neutral-400"
              : "text-neutral-700 hover:bg-neutral-50 active:scale-[0.99]"
          }`}
        >
          {isResending
            ? "Sending..."
            : left > 0
            ? `Resend email in ${left}s`
            : sends > 0
            ? "Resend email"
            : "Send verification email"}
        </button>

        {/* Sent Counter Note */}
        {sends > 0 && (
          <p id="resendNote" className="mt-2 text-xs text-neutral-400">
            Sent {sends === 1 ? "once" : `${sends} times`} to this address.
          </p>
        )}

        {/* Contact Support Footer */}
        <p className="mt-6 border-t border-neutral-200 pt-4 text-xs text-neutral-400">
          Nothing arrived?{" "}
          <a
            href="mailto:support@ut.edu.vn"
            className="font-medium text-neutral-600 underline underline-offset-4 hover:text-neutral-900"
          >
            Contact support
          </a>
        </p>
      </div>
    </div>
  );
}
