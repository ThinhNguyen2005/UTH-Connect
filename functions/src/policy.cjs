"use strict";
function normalizeUthEmail(raw) {
  if (typeof raw !== "string") return null;
  const email = raw.trim().toLowerCase();
  if (email.length > 254 || email.split("@").length !== 2) return null;
  const [local, domain] = email.split("@");
  if (domain !== "ut.edu.vn" || !local || local.length > 64 || local.startsWith(".") || local.endsWith(".") || local.includes("..")) return null;
  return /^[a-z0-9._%+\-]+$/.test(local) ? email : null;
}
function mergeVerifiedClaims(existing) {
  // Explicit false is an administrative revocation, not a missing onboarding claim.
  if (existing.uthVerified === false) {
    const error = new Error("UTH access revoked by backend");
    error.code = "AccessRevoked";
    throw error;
  }
  if (existing.role !== undefined && (typeof existing.role !== "string" || !["STUDENT", "ORGANIZER", "ADMIN"].includes(existing.role.toUpperCase()))) throw new Error("Unknown backend role");
  return { ...existing, role: existing.role === undefined ? "STUDENT" : existing.role, uthVerified: true };
}
module.exports = { normalizeUthEmail, mergeVerifiedClaims };
