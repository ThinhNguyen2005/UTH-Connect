import { NextResponse } from "next/server";
import { createClient } from "@/utils/supabase-server";
import { type EmailOtpType } from "@supabase/supabase-js";

export async function GET(request: Request) {
  const { searchParams, origin } = new URL(request.url);
  const code = searchParams.get("code");
  const token_hash = searchParams.get("token_hash");
  const type = searchParams.get("type") as EmailOtpType | null;
  const next = searchParams.get("next") ?? "/";

  const forwardedHost = request.headers.get("x-forwarded-host");
  const isLocalEnv = process.env.NODE_ENV === "development";
  const baseUrl = isLocalEnv
    ? origin
    : forwardedHost
    ? `https://${forwardedHost}`
    : origin;

  const safeRedirect = (path: string) => {
    if (!path.startsWith("/") || path.startsWith("//") || path.includes("\\")) {
      return new URL("/", baseUrl);
    }
    const target = new URL(path, baseUrl);
    return target.origin === new URL(baseUrl).origin ? target : new URL("/", baseUrl);
  };

  const supabase = createClient();

  // 1. Token Hash Verification (Email verification & Password recovery links)
  if (token_hash && type) {
    const { error } = await supabase.auth.verifyOtp({
      type,
      token_hash,
    });
    if (!error) {
      if (type === "recovery") {
        return NextResponse.redirect(new URL("/reset-password", baseUrl));
      }
      if (type === "signup" || type === "email") {
        return NextResponse.redirect(new URL("/login?verified=true", baseUrl));
      }
      return NextResponse.redirect(safeRedirect(next));
    } else {
      const loginUrl = new URL("/login", baseUrl);
      loginUrl.searchParams.set("error", error.message);
      return NextResponse.redirect(loginUrl);
    }
  }

  // 2. PKCE Code Exchange (OAuth Google & PKCE Auth)
  if (code) {
    const { data, error } = await supabase.auth.exchangeCodeForSession(code);
    if (!error) {
      let target = next;
      if (type === "recovery" || next === "/reset-password") {
        target = "/reset-password";
      } else {
        const role = data?.user?.app_metadata?.role;
        if (role === "admin" && next === "/") {
          target = "/admin";
        }
      }
      return NextResponse.redirect(safeRedirect(target));
    } else {
      if (type === "recovery" || next === "/reset-password") {
        const resetUrl = new URL("/reset-password", baseUrl);
        resetUrl.hash = new URLSearchParams({
          error: "access_denied",
          error_code: "otp_expired",
          error_description: error.message,
        }).toString();
        return NextResponse.redirect(resetUrl);
      }
      const loginUrl = new URL("/login", baseUrl);
      loginUrl.searchParams.set("error", error.message);
      return NextResponse.redirect(loginUrl);
    }
  }

  // Return user to login page with error state
  return NextResponse.redirect(new URL("/login?error=auth_callback_failed", baseUrl));
}
