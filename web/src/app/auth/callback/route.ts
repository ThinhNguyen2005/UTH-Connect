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

  const supabase = createClient();

  // 1. Token Hash Verification (Email verification & Password recovery links)
  if (token_hash && type) {
    const { error } = await supabase.auth.verifyOtp({
      type,
      token_hash,
    });
    if (!error) {
      if (type === "recovery") {
        return NextResponse.redirect(`${baseUrl}/reset-password`);
      }
      if (type === "signup" || type === "email") {
        return NextResponse.redirect(`${baseUrl}/login?verified=true`);
      }
      return NextResponse.redirect(`${baseUrl}${next}`);
    } else {
      return NextResponse.redirect(
        `${baseUrl}/login?error=${encodeURIComponent(error.message)}`
      );
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
      return NextResponse.redirect(`${baseUrl}${target}`);
    }
  }

  // Return user to login page with error state
  return NextResponse.redirect(`${baseUrl}/login?error=auth_callback_failed`);
}
