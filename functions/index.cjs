"use strict";
const { initializeApp } = require("firebase-admin/app");
const { getAuth } = require("firebase-admin/auth");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { normalizeUthEmail, mergeVerifiedClaims } = require("./src/policy.cjs");
const { withClaimsLock } = require("./src/claims-writer.cjs");
initializeApp();

exports.verifyUthAccount = onCall({ region: "us-central1", timeoutSeconds: 30, maxInstances: 5 }, async (request) => {
  if (!request.auth?.uid) throw new HttpsError("unauthenticated", "Authentication required", { reason: "InvalidSession" });
  if (!request.data || typeof request.data !== "object" || Array.isArray(request.data) || Object.keys(request.data).length !== 0) {
    throw new HttpsError("invalid-argument", "This operation accepts no identity or role fields", { reason: "InvalidInput" });
  }
  const uid = request.auth.uid;
  return withClaimsLock(getFirestore(), uid, async () => {
    const auth = getAuth();
    let user;
    try { user = await auth.getUser(uid); }
    catch { throw new HttpsError("unauthenticated", "Account unavailable", { reason: "InvalidSession" }); }
    if (user.disabled) throw new HttpsError("unauthenticated", "Account disabled", { reason: "InvalidSession" });
    const email = normalizeUthEmail(user.email);
    if (!email) throw new HttpsError("permission-denied", "University domain required", { reason: "DomainNotAllowed" });
    if (!user.emailVerified) throw new HttpsError("failed-precondition", "Verify your email first", { reason: "EmailNotVerified" });
    const checkedClaims = (record) => {
      try { return mergeVerifiedClaims(record.customClaims || {}); }
      catch (error) {
        if (error.code === "AccessRevoked") throw new HttpsError("permission-denied", "UTH access revoked", { reason: "PermissionDenied" });
        throw new HttpsError("failed-precondition", "Unknown role requires administrator review", { reason: "Configuration" });
      }
    };
    checkedClaims(user); // Reject explicit revocation before creating a private profile.
    const profile = getFirestore().collection("users").doc(uid);
    await getFirestore().runTransaction(async (transaction) => {
      const existing = await transaction.get(profile);
      if (!existing.exists) transaction.create(profile, {
        uid, email, displayName: (user.displayName || email.split("@")[0]).slice(0, 100),
        studentId: null, className: "", major: "", faculty: "",
        phoneNumber: "", personalEmail: "", currentAddress: "",
        createdAt: FieldValue.serverTimestamp(), profileVersion: 1,
    });
  });
  // Read the latest Admin record again before merging. Auth claims have no compare-and-swap API;
  // all privileged claim writers must use withClaimsLock.
  user = await auth.getUser(uid);
  if (user.disabled || !user.emailVerified || normalizeUthEmail(user.email) !== email) {
    throw new HttpsError("failed-precondition", "Identity changed, retry authentication", { reason: "InvalidSession" });
  }
  const claims = checkedClaims(user);
  await auth.setCustomUserClaims(uid, claims);
  return { status: "verified", uid, claimsVersion: 1 };
  });
});
