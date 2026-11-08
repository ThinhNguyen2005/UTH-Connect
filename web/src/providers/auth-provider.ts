import { AuthProvider } from "@refinedev/core";
import { supabaseBrowserClient } from "@/utils/supabase-client";

export type AppUserRole = "admin" | "lecturer" | "organizer" | "student";

export interface AppUserIdentity {
  id: string;
  email: string;
  name: string;
  avatar?: string;
  role: AppUserRole;
}

export const authProvider: AuthProvider = {
  login: async ({ email, password, providerName }) => {
    try {
      if (providerName === "google") {
        const { data, error } = await supabaseBrowserClient.auth.signInWithOAuth({
          provider: "google",
          options: {
            redirectTo: `${window.location.origin}/auth/callback`,
          },
        });
        if (error) {
          return {
            success: false,
            error: {
              name: "GoogleAuthError",
              message: error.message || "Đăng nhập Google không thành công.",
            },
          };
        }
        if (data?.url) {
          window.location.href = data.url;
          return { success: true };
        }
      }

      if (!email || !password) {
        return {
          success: false,
          error: {
            name: "ValidationError",
            message: "Vui lòng nhập đầy đủ email hoặc MSSV và mật khẩu.",
          },
        };
      }

      let normalizedEmail = email.trim();
      if (!normalizedEmail.includes("@")) {
        normalizedEmail = `${normalizedEmail}@ut.edu.vn`;
      }

      const { data, error } = await supabaseBrowserClient.auth.signInWithPassword({
        email: normalizedEmail,
        password,
      });

      if (error) {
        let msg = error.message;
        if (error.message.includes("Invalid login credentials")) {
          msg = "Tài khoản hoặc mật khẩu không chính xác.";
        } else if (error.message.includes("Email not confirmed")) {
          msg = "Tài khoản chưa được xác nhận email.";
        }
        return {
          success: false,
          error: {
            name: "AuthError",
            message: msg,
          },
        };
      }

      if (data?.user) {
        const role = data.user.app_metadata?.role;
        return {
          success: true,
          redirectTo: role === "admin" ? "/admin" : "/",
        };
      }
    } catch (error: any) {
      return {
        success: false,
        error: {
          name: "AuthError",
          message: error?.message || "Đăng nhập thất bại.",
        },
      };
    }

    return {
      success: false,
      error: { message: "Đăng nhập không thành công", name: "AuthError" },
    };
  },

  register: async (params: any) => {
    try {
      const {
        email,
        password,
        name,
        studentId,
        faculty,
        major,
        className,
        phoneNumber,
      } = params || {};

      if (!email || !password) {
        return {
          success: false,
          error: {
            name: "ValidationError",
            message: "Vui lòng nhập email/MSSV và mật khẩu.",
          },
        };
      }

      let normalizedEmail = email.trim();
      if (!normalizedEmail.includes("@")) {
        normalizedEmail = `${normalizedEmail}@ut.edu.vn`;
      }

      const derivedStudentId =
        studentId?.trim() ||
        (normalizedEmail.endsWith("@ut.edu.vn")
          ? normalizedEmail.replace("@ut.edu.vn", "")
          : null);

      const { data, error } = await supabaseBrowserClient.auth.signUp({
        email: normalizedEmail,
        password,
        options: {
          data: {
            full_name: name?.trim() || normalizedEmail.split("@")[0],
            display_name: name?.trim() || normalizedEmail.split("@")[0],
            student_id: derivedStudentId,
            studentId: derivedStudentId,
            faculty: faculty?.trim() || null,
            major: major?.trim() || null,
            class_name: className?.trim() || null,
            className: className?.trim() || null,
            phone_number: phoneNumber?.trim() || null,
            phoneNumber: phoneNumber?.trim() || null,
          },
          emailRedirectTo: `${typeof window !== "undefined" ? window.location.origin : ""}/auth/callback`,
        },
      });

      if (error) {
        let msg = error.message;
        if (error.message.includes("User already registered")) {
          msg = "Email hoặc MSSV này đã được đăng ký tài khoản.";
        } else if (error.message.includes("Password should be at least")) {
          msg = "Mật khẩu phải có tối thiểu 6 ký tự.";
        }
        return {
          success: false,
          error: {
            name: "RegisterError",
            message: msg,
          },
        };
      }

      if (data?.session) {
        return {
          success: true,
          redirectTo: "/",
        };
      }

      return {
        success: true,
        redirectTo: `/verify-email?email=${encodeURIComponent(normalizedEmail)}`,
      };
    } catch (err: any) {
      return {
        success: false,
        error: {
          name: "RegisterError",
          message: err?.message || "Đăng ký không thành công.",
        },
      };
    }
  },

  logout: async () => {
    const { error } = await supabaseBrowserClient.auth.signOut();
    if (error) return { success: false, error };
    return {
      success: true,
      redirectTo: "/login",
    };
  },

  check: async () => {
    try {
      const { data } = await supabaseBrowserClient.auth.getSession();
      const session = data?.session;

      if (!session) {
        return {
          authenticated: false,
          logout: true,
          redirectTo: "/login",
        };
      }
      return { authenticated: true };
    } catch {
      return {
        authenticated: false,
        logout: true,
        redirectTo: "/login",
      };
    }
  },

  onError: async (error) => {
    if (error?.status === 401 || error?.status === 403) {
      return {
        logout: true,
        redirectTo: "/login",
        error,
      };
    }
    return { error };
  },

  getPermissions: async (): Promise<AppUserRole> => {
    const { data } = await supabaseBrowserClient.auth.getSession();
    const session = data?.session;
    if (!session?.user) return "student";

    const appRole = session.user.app_metadata?.role as AppUserRole | undefined;
    if (appRole) return appRole;

    const { data: profile } = await supabaseBrowserClient
      .from("profiles")
      .select("role")
      .eq("id", session.user.id)
      .maybeSingle();

    return (profile?.role as AppUserRole) || "student";
  },

  getIdentity: async (): Promise<AppUserIdentity | null> => {
    const { data } = await supabaseBrowserClient.auth.getSession();
    const session = data?.session;
    if (!session?.user) return null;

    const user = session.user;
    const role = (user.app_metadata?.role as AppUserRole) || "student";

    const { data: profile } = await supabaseBrowserClient
      .from("profiles")
      .select("display_name, avatar_url, role")
      .eq("id", user.id)
      .maybeSingle();

    return {
      id: user.id,
      email: user.email || "",
      name:
        profile?.display_name ||
        user.user_metadata?.full_name ||
        user.user_metadata?.name ||
        user.email?.split("@")[0] ||
        "Người dùng",
      avatar:
        profile?.avatar_url ||
        user.user_metadata?.avatar_url ||
        user.user_metadata?.picture,
      role: (profile?.role as AppUserRole) || role,
    };
  },
};
