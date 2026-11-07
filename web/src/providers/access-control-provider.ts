import { AccessControlProvider } from "@refinedev/core";
import { authProvider, AppUserRole } from "./auth-provider";

export const accessControlProvider: AccessControlProvider = {
  can: async ({ resource, action, params }) => {
    const role: AppUserRole =
      ((await authProvider.getPermissions?.()) as AppUserRole) || "student";

    // 1. Super Admin: full access
    if (role === "admin") {
      return { can: true };
    }

    // 2. Lecturer / Organizer:
    if (role === "lecturer" || role === "organizer") {
      // Cannot manage user roles or admin settings
      if (resource === "users" || resource === "user_roles" || resource === "student_verification_requests") {
        if (action === "list" || action === "show") return { can: true };
        return {
          can: false,
          reason: "Chỉ Quản trị viên (Admin) mới có quyền quản lý người dùng và duyệt phân quyền.",
        };
      }

      // Can manage events and notifications
      if (resource === "notifications" || resource === "events") {
        return { can: true };
      }

      // Read-only for other resources
      if (action === "list" || action === "show") {
        return { can: true };
      }
      return { can: false, reason: "Bạn không có quyền thực hiện thao tác này." };
    }

    // 3. Student:
    // Notifications & Events: Read-only
    if (resource === "notifications" || resource === "events") {
      if (action === "list" || action === "show") return { can: true };
      return {
        can: false,
        reason: "Sinh viên không có quyền đăng hoặc chỉnh sửa thông báo / sự kiện của trường.",
      };
    }

    // Housing Posts: Can view, create, edit own
    if (resource === "housing_posts") {
      if (action === "list" || action === "show" || action === "create") return { can: true };
      if (action === "edit" || action === "delete") {
        // Handled by RLS on database or params.id check
        return { can: true };
      }
    }

    // Users / Admin settings: Blocked completely
    if (resource === "users" || resource === "user_roles" || resource === "student_verification_requests") {
      return {
        can: false,
        reason: "Khu vực này chỉ dành riêng cho Ban Quản trị.",
      };
    }

    return { can: true };
  },
};
