"use client";

import React, { useState, useEffect, useRef } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { supabaseBrowserClient } from "@/utils/supabase-client";
import { ArrowLeft, ShieldAlert, CheckCircle2 } from "lucide-react";

export default function VerifyOtpPage() {
  const router = useRouter();
  const [email, setEmail] = useState<string>("you@ut.edu.vn");
  const [digits, setDigits] = useState<string[]>(["", "", "", "", "", ""]);
  const [timeLeft, setTimeLeft] = useState<number>(30);
  const [isVerifying, setIsVerifying] = useState<boolean>(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const inputRefs = useRef<(HTMLInputElement | null)[]>([]);

  // Read email from search query on mount
  useEffect(() => {
    if (typeof window !== "undefined") {
      const params = new URLSearchParams(window.location.search);
      const queryEmail = params.get("email");
      if (queryEmail) {
        setEmail(queryEmail);
      }
    }
  }, []);

  // Countdown timer from 30 seconds
  useEffect(() => {
    if (timeLeft <= 0) return;

    const interval = setInterval(() => {
      setTimeLeft((prev) => prev - 1);
    }, 1000);

    return () => clearInterval(interval);
  }, [timeLeft]);

  // Focus first input on mount
  useEffect(() => {
    inputRefs.current[0]?.focus();
  }, []);

  const isComplete = digits.every((d) => d.trim() !== "");

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>, index: number) => {
    const rawVal = e.target.value.replace(/\D/g, "");
    const singleDigit = rawVal.slice(-1); // Take last digit if user types over

    const newDigits = [...digits];
    newDigits[index] = singleDigit;
    setDigits(newDigits);
    setErrorMessage(null);

    // Advance to next box if digit entered
    if (singleDigit && index < digits.length - 1) {
      inputRefs.current[index + 1]?.focus();
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>, index: number) => {
    if (e.key === "Backspace" && !digits[index] && index > 0) {
      // Step back on Backspace from an empty box
      inputRefs.current[index - 1]?.focus();
    } else if (e.key === "ArrowLeft" && index > 0) {
      inputRefs.current[index - 1]?.focus();
    } else if (e.key === "ArrowRight" && index < digits.length - 1) {
      inputRefs.current[index + 1]?.focus();
    }
  };

  const handlePaste = (e: React.ClipboardEvent<HTMLInputElement>, startIndex: number) => {
    e.preventDefault();
    const pastedText = e.clipboardData.getData("text");
    const cleanDigits = pastedText.replace(/\D/g, "");

    if (!cleanDigits) return;

    const newDigits = [...digits];
    for (let k = 0; k < digits.length; k++) {
      if (cleanDigits[k]) {
        newDigits[k] = cleanDigits[k];
      }
    }
    setDigits(newDigits);
    setErrorMessage(null);

    // Focus the box right after the last pasted digit (up to 5)
    const nextFocus = Math.min(cleanDigits.length, digits.length - 1);
    inputRefs.current[nextFocus]?.focus();
  };

  const handleVerify = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    if (!isComplete || isVerifying) return;

    setIsVerifying(true);
    setErrorMessage(null);

    const token = digits.join("");

    try {
      // Supabase OTP verification
      const { data, error } = await supabaseBrowserClient.auth.verifyOtp({
        email: email.trim(),
        token,
        type: "email",
      });

      if (error) {
        // Fallback check for signup token type
        const fallback = await supabaseBrowserClient.auth.verifyOtp({
          email: email.trim(),
          token,
          type: "signup",
        });

        if (fallback.error) {
          setErrorMessage(fallback.error.message || "Mã xác thực không hợp lệ hoặc đã hết hạn.");
          setIsVerifying(false);
          return;
        }
      }

      setSuccessMessage("Xác thực thành công! Đang chuyển hướng...");
      setTimeout(() => {
        router.push("/");
      }, 800);
    } catch (err: any) {
      setErrorMessage(err?.message || "Xác thực thất bại. Vui lòng thử lại.");
    } finally {
      setIsVerifying(false);
    }
  };

  const handleResend = async () => {
    setTimeLeft(30);
    setDigits(["", "", "", "", "", ""]);
    setErrorMessage(null);
    setSuccessMessage("Đã gửi mã xác thực mới vào email.");
    inputRefs.current[0]?.focus();

    try {
      await supabaseBrowserClient.auth.signInWithOtp({
        email: email.trim(),
        options: {
          shouldCreateUser: false,
        },
      });
    } catch {
      // Ignored for UI responsiveness
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-neutral-100 p-6">
      <div className="w-full max-w-sm rounded-2xl border border-neutral-200 bg-white p-8 text-center shadow-sm">
        {/* Back Link */}
        <div className="flex items-center justify-start mb-4">
          <Link
            href="/login"
            className="inline-flex items-center gap-1.5 text-xs text-neutral-500 hover:text-neutral-900 transition font-medium"
          >
            <ArrowLeft className="w-3.5 h-3.5" />
            <span>Quay lại</span>
          </Link>
        </div>

        <h1 className="text-lg font-semibold text-neutral-900">Check your email</h1>
        <p className="mt-1 text-sm text-neutral-500">
          We sent a 6-digit code to <span className="font-medium text-neutral-800">{email}</span>
        </p>

        {/* Feedback message */}
        {errorMessage && (
          <div className="mt-4 p-3 rounded-xl bg-red-50 text-red-700 text-xs flex items-center justify-center gap-2 text-left">
            <ShieldAlert className="w-4 h-4 shrink-0 text-red-600" />
            <span>{errorMessage}</span>
          </div>
        )}

        {successMessage && (
          <div className="mt-4 p-3 rounded-xl bg-emerald-50 text-emerald-800 text-xs flex items-center justify-center gap-2 text-left">
            <CheckCircle2 className="w-4 h-4 shrink-0 text-emerald-600" />
            <span>{successMessage}</span>
          </div>
        )}

        {/* 6-digit OTP Inputs */}
        <div id="otp" className="mt-6 flex justify-center gap-2">
          {digits.map((digit, i) => (
            <input
              key={i}
              ref={(el) => {
                inputRefs.current[i] = el;
              }}
              type="text"
              inputMode="numeric"
              maxLength={1}
              autoComplete={i === 0 ? "one-time-code" : "off"}
              value={digit}
              onChange={(e) => handleInputChange(e, i)}
              onKeyDown={(e) => handleKeyDown(e, i)}
              onPaste={(e) => handlePaste(e, i)}
              className="size-12 rounded-xl border border-neutral-200 text-center text-lg font-semibold outline-none transition focus:border-neutral-900 focus:ring-4 focus:ring-neutral-900/10"
            />
          ))}
        </div>

        {/* Verify Button */}
        <button
          id="verify"
          type="button"
          onClick={() => handleVerify()}
          disabled={!isComplete || isVerifying}
          className={`mt-6 w-full rounded-xl bg-neutral-900 py-2.5 text-sm font-medium text-white transition hover:bg-neutral-800 ${
            !isComplete || isVerifying ? "cursor-not-allowed opacity-40" : ""
          }`}
        >
          {isVerifying ? "Verifying..." : "Verify"}
        </button>

        {/* Resend Countdown */}
        <div id="resend" className="mt-4 text-xs text-neutral-400">
          {timeLeft > 0 ? (
            <span>
              Didn&apos;t get it? Resend in 0:{timeLeft < 10 ? `0${timeLeft}` : timeLeft}
            </span>
          ) : (
            <span>
              Didn&apos;t get it?{" "}
              <button
                type="button"
                onClick={handleResend}
                className="font-medium text-neutral-900 underline hover:text-black"
              >
                Resend code
              </button>
            </span>
          )}
        </div>
      </div>
    </div>
  );
}
