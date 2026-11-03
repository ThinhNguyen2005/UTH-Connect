"use strict";
const { test, before, after } = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const { initializeApp, deleteApp } = require("firebase-admin/app");
const { getAuth } = require("firebase-admin/auth");
const { getFirestore } = require("firebase-admin/firestore");
const { initializeTestEnvironment, assertSucceeds, assertFails } = require("@firebase/rules-unit-testing");
const { doc, setDoc, getDoc, updateDoc, deleteDoc, collection, getDocs, setLogLevel } = require("firebase/firestore");
const { normalizeUthEmail, mergeVerifiedClaims } = require("../src/policy.cjs");
const projectId = "demo-uth-auth3";
let app, adminAuth, adminDb, env;
const verified = { email: "student@ut.edu.vn", email_verified: true, uthVerified: true, role: "STUDENT" };
const context = (uid = "rules-student", claims = {}) => env.authenticatedContext(uid, { ...verified, ...claims }).firestore();

before(async () => {
  for (const name of ["FIRESTORE_EMULATOR_HOST", "FIREBASE_AUTH_EMULATOR_HOST"]) {
    if (!process.env[name]?.match(/^(127\.0\.0\.1|localhost):/)) throw new Error("Local Auth and Firestore emulators required");
  }
  setLogLevel("silent");
  app = initializeApp({ projectId }, "auth3-tests");
  adminAuth = getAuth(app); adminDb = getFirestore(app);
  env = await initializeTestEnvironment({ projectId, firestore: { host: "127.0.0.1", port: 8080, rules: fs.readFileSync("../firestore.rules", "utf8") } });
  await env.withSecurityRulesDisabled(async (c) => {
    const db = c.firestore();
    for (const uid of ["rules-student", "rules-other", "rules-admin", "rules-organizer"]) {
      await setDoc(doc(db, "users", uid), { displayName: uid, studentId: null, phoneNumber: "", personalEmail: "", currentAddress: "", createdAt: "server-only", role: "admin" });
    }
    await setDoc(doc(db, "events/existing"), { title: "Existing event" });
    await setDoc(doc(db, "notifications/existing"), { title: "Existing notice" });
    await setDoc(doc(db, "schedules/own"), { uid: "rules-student", studentId: "trusted-id" });
    await setDoc(doc(db, "grades/own"), { uid: "rules-student", studentId: "trusted-id", grade: 8 });
  });
});
after(async () => { await env?.cleanup(); if (app) await deleteApp(app); });

async function account(uid, email, emailVerified = true, claims = {}) {
  await adminAuth.createUser({ uid, email, password: "EmulatorOnly@12345", emailVerified, displayName: "Emulator Student" });
  if (Object.keys(claims).length) await adminAuth.setCustomUserClaims(uid, claims);
  const response = await fetch(`http://${process.env.FIREBASE_AUTH_EMULATOR_HOST}/identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=emulator-only`, {
    method: "POST", headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password: "EmulatorOnly@12345", returnSecureToken: true }),
  });
  const result = await response.json();
  assert.ok(result.idToken, "Emulator sign-in must return a real emulator token");
  return result.idToken;
}
async function verify(token, data = {}) {
  const response = await fetch(`http://127.0.0.1:5001/${projectId}/us-central1/verifyUthAccount`, {
    method: "POST", headers: { "Content-Type": "application/json", ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: JSON.stringify({ data }),
  });
  return { status: response.status, body: await response.json() };
}

test("policy accepts normalized exact university domain only", () => {
  assert.equal(normalizeUthEmail(" STUDENT@UT.EDU.VN "), "student@ut.edu.vn");
  for (const value of ["user@gmail.com", "x@ut.edu.vn.evil.com", "x@sub.ut.edu.vn", "a@@ut.edu.vn", ".a@ut.edu.vn", "a..b@ut.edu.vn", "", null]) assert.equal(normalizeUthEmail(value), null);
});
test("policy preserves backend claims and valid roles", () => {
  assert.deepEqual(mergeVerifiedClaims({ role: "ADMIN", orgIds: ["club"] }), { role: "ADMIN", orgIds: ["club"], uthVerified: true });
  assert.deepEqual(mergeVerifiedClaims({ feature: true }), { feature: true, role: "STUDENT", uthVerified: true });
  assert.throws(() => mergeVerifiedClaims({ role: "SUPERUSER" }));
});
test("anonymous callable is rejected", async () => {
  const r = await verify(); assert.equal(r.status, 401);
});
test("verified email creates safe profile and least privilege claims", async () => {
  const token = await account("fn-new", "New@UT.EDU.VN");
  const r = await verify(token); assert.equal(r.status, 200); assert.equal(r.body.result.status, "verified");
  const user = await adminAuth.getUser("fn-new");
  assert.equal(user.customClaims.uthVerified, true); assert.equal(user.customClaims.role, "STUDENT");
  const profile = (await adminDb.doc("users/fn-new").get()).data();
  assert.equal(profile.studentId, null); assert.equal(profile.email, "new@ut.edu.vn");
  assert.equal(profile.role, undefined); assert.equal(profile.uthVerified, undefined);
});
test("unverified email never receives claims or a private profile", async () => {
  const token = await account("fn-unverified", "unverified@ut.edu.vn", false);
  const r = await verify(token); assert.equal(r.body.error.details.reason, "EmailNotVerified");
  assert.notEqual((await adminAuth.getUser("fn-unverified")).customClaims?.uthVerified, true);
  assert.equal((await adminDb.doc("users/fn-unverified").get()).exists, false);
});
test("callable reads current Admin record rather than stale client token", async () => {
  const token = await account("fn-stale", "stale@ut.edu.vn", false);
  await adminAuth.updateUser("fn-stale", { emailVerified: true });
  assert.equal((await verify(token)).status, 200);
});
test("outside and deceptive domains are rejected even with verified email", async () => {
  let index = 0;
  for (const email of ["outside@gmail.com", "x@ut.edu.vn.evil.com", "x@sub.ut.edu.vn"]) {
    const uid = `fn-outside-${index++}`; const token = await account(uid, email);
    assert.equal((await verify(token)).body.error.details.reason, "DomainNotAllowed");
    assert.notEqual((await adminAuth.getUser(uid)).customClaims?.uthVerified, true);
    assert.equal((await adminDb.doc(`users/${uid}`).get()).exists, false);
  }
});
test("client identity or ADMIN payload is rejected", async () => {
  const token = await account("fn-payload", "payload@ut.edu.vn");
  const r = await verify(token, { uid: "victim", role: "ADMIN", studentId: "fake", uthVerified: true });
  assert.equal(r.status, 400); assert.notEqual((await adminAuth.getUser("fn-payload")).customClaims?.uthVerified, true);
});
test("repeat and concurrent verification is idempotent", async () => {
  const token = await account("fn-repeat", "repeat@ut.edu.vn");
  await verify(token);
  const ref = adminDb.doc("users/fn-repeat"); const original = (await ref.get()).data();
  await ref.update({ phoneNumber: "0912345678", studentId: "server-reviewed" });
  const results = await Promise.all([verify(token), verify(token), verify(token)]);
  assert.ok(results.every(r => r.status === 200));
  const result = (await ref.get()).data();
  assert.equal(result.phoneNumber, "0912345678"); assert.equal(result.studentId, "server-reviewed");
  assert.ok(result.createdAt.isEqual(original.createdAt));
});
test("verification never downgrades privileged backend roles or unrelated claims", async () => {
  for (const role of ["ORGANIZER", "ADMIN"]) {
    const uid = `fn-${role.toLowerCase()}`;
    const token = await account(uid, `${uid}@ut.edu.vn`, true, { role, orgIds: ["club"], feature: true });
    assert.equal((await verify(token)).status, 200);
    assert.deepEqual((await adminAuth.getUser(uid)).customClaims, { role, orgIds: ["club"], feature: true, uthVerified: true });
  }
});
test("disabled and deleted users cannot provision with an earlier token", async () => {
  const token = await account("fn-disabled", "disabled@ut.edu.vn");
  await adminAuth.updateUser("fn-disabled", { disabled: true });
  assert.equal((await verify(token)).status, 401);
  const deleted = await account("fn-deleted", "deleted@ut.edu.vn");
  await adminAuth.deleteUser("fn-deleted");
  assert.equal((await verify(deleted)).status, 401);
});
test("private profile rejects anonymous, no claims and unverified email", async () => {
  await assertFails(getDoc(doc(env.unauthenticatedContext().firestore(), "users/rules-student")));
  const none = env.authenticatedContext("rules-student", { email: "student@ut.edu.vn", email_verified: true }).firestore();
  await assertFails(getDoc(doc(none, "users/rules-student")));
  await assertFails(getDoc(doc(context("rules-student", { email_verified: false }), "users/rules-student")));
});
test("own verified profile and valid contact update are allowed", async () => {
  const db = context();
  await assertSucceeds(getDoc(doc(db, "users/rules-student")));
  await assertSucceeds(updateDoc(doc(db, "users/rules-student"), { phoneNumber: "0912345678", personalEmail: "personal@example.com", currentAddress: "Address" }));
});
test("cross UID profile reads, writes, listing and client creation are denied", async () => {
  const db = context();
  await assertFails(getDoc(doc(db, "users/rules-other")));
  await assertFails(updateDoc(doc(db, "users/rules-other"), { phoneNumber: "0912345678" }));
  await assertFails(getDocs(collection(db, "users")));
  await assertFails(setDoc(doc(db, "users/new-client-profile"), { displayName: "Untrusted" }));
  await assertFails(deleteDoc(doc(db, "users/rules-student")));
});
test("document role cannot elevate any client privileges", async () => {
  const db = context();
  await assertFails(updateDoc(doc(db, "users/rules-student"), { role: "ADMIN" }));
  await assertFails(setDoc(doc(db, "events/student-escalation"), { title: "denied despite legacy role field" }));
  await assertFails(setDoc(doc(db, "notifications/student-escalation"), { title: "denied" }));
});
test("student cannot modify security metadata or academic identity", async () => {
  for (const [key, value] of Object.entries({ uthVerified: true, studentId: "forged", email: "other@ut.edu.vn", displayName: "forged", createdAt: "forged", profileVersion: 999 })) {
    await assertFails(updateDoc(doc(context(), "users/rules-student"), { [key]: value }));
  }
});
test("contact validation is enforced by rules as well as client", async () => {
  for (const update of [{ phoneNumber: { role: "ADMIN" } }, { phoneNumber: "123" }, { personalEmail: "invalid" }, { currentAddress: "x".repeat(201) }]) {
    await assertFails(updateDoc(doc(context(), "users/rules-student"), update));
  }
});
test("forged UTH claims on outside or deceptive domains grant no UTH permissions", async () => {
  for (const email of ["user@gmail.com", "x@ut.edu.vn.evil.com", "x@sub.ut.edu.vn"]) {
    const db = context("rules-student", { email, role: "ADMIN" });
    await assertFails(getDoc(doc(db, "users/rules-student")));
    await assertFails(setDoc(doc(db, "events/outside"), { title: "denied" }));
  }
});
test("verified Organizer and Admin retain valid module permissions", async () => {
  const org = context("rules-organizer", { role: "ORGANIZER" });
  await assertSucceeds(setDoc(doc(org, "events/organizer"), { title: "Organizer event" }));
  await assertFails(setDoc(doc(org, "notifications/organizer"), { title: "Admin only" }));
  const admin = context("rules-admin", { role: "ADMIN" });
  await assertSucceeds(setDoc(doc(admin, "events/admin"), { title: "Admin event" }));
  await assertSucceeds(setDoc(doc(admin, "notifications/admin"), { title: "Admin notice" }));
  await assertSucceeds(setDoc(doc(admin, "grades/admin"), { grade: 9 }));
});
test("student registration and own grades/schedule retain legitimate access", async () => {
  const db = context();
  await assertSucceeds(setDoc(doc(db, "events/existing/registrations/rules-student"), { status: "registered" }));
  await assertFails(setDoc(doc(db, "events/existing/registrations/rules-other"), { status: "registered" }));
  await assertSucceeds(getDoc(doc(db, "schedules/own")));
  await assertSucceeds(getDoc(doc(db, "grades/own")));
  await assertFails(getDoc(doc(context("rules-other"), "grades/own")));
});
test("unverified student cannot register or write verified collections", async () => {
  const db = context("rules-student", { email_verified: false });
  await assertFails(setDoc(doc(db, "events/existing/registrations/rules-student"), { status: "registered" }));
  await assertFails(updateDoc(doc(db, "users/rules-student"), { phoneNumber: "0912345678" }));
});
test("existing authenticated public event/notice reads stay available", async () => {
  const db = context();
  await assertSucceeds(getDoc(doc(db, "events/existing")));
  await assertSucceeds(getDoc(doc(db, "notifications/existing")));
});

test("explicit backend revocation cannot be undone by verify callable", async () => {
  const claims = { role: "ORGANIZER", uthVerified: false, customPermission: "preserved" };
  const token = await account("fn-revoked", "revoked@ut.edu.vn", true, claims);
  const result = await verify(token);
  assert.equal(result.status, 403);
  assert.equal(result.body.error.details.reason, "PermissionDenied");
  assert.deepEqual((await adminAuth.getUser("fn-revoked")).customClaims, claims);
  assert.equal((await adminDb.doc("users/fn-revoked").get()).exists, false);
});

test("email local part rejects line endings and format controls", () => {
  for (const email of ["student\n@ut.edu.vn", "student\r@ut.edu.vn", "student\r\n@ut.edu.vn",
      "student\u2028@ut.edu.vn", "student\u2029@ut.edu.vn", "student\u200b@ut.edu.vn"]) {
    assert.equal(normalizeUthEmail(email), null);
  }
});

test("registration privacy preserves owner and trusted staff access", async () => {
  const own = "events/existing/registrations/rules-student";
  await assertSucceeds(getDoc(doc(context(), own)));
  await assertFails(getDoc(doc(context("rules-other"), own)));
  await assertFails(getDoc(doc(context("rules-student", { email_verified: false }), own)));
  await assertFails(getDoc(doc(env.unauthenticatedContext().firestore(), own)));
  for (const role of ["ORGANIZER", "ADMIN"]) {
    const db = context("rules-organizer", { role });
    await assertSucceeds(getDoc(doc(db, own)));
    await assertSucceeds(getDocs(collection(db, "events/existing/registrations")));
  }
  await assertFails(getDocs(collection(context(), "events/existing/registrations")));
});

test("cooperative role writer and verifier preserve privilege and unrelated claims", async () => {
  const { withClaimsLock } = require("../src/claims-writer.cjs");
  const token = await account("fn-lock", "lock@ut.edu.vn", true, { customPermission: "keep" });
  await Promise.all([
    withClaimsLock(adminDb, "fn-lock", async () => {
      const current = await adminAuth.getUser("fn-lock");
      await new Promise((resolve) => setTimeout(resolve, 100));
      await adminAuth.setCustomUserClaims("fn-lock", { ...current.customClaims, role: "ORGANIZER" });
    }),
    verify(token).then((result) => assert.equal(result.status, 200)),
  ]);
  assert.deepEqual((await adminAuth.getUser("fn-lock")).customClaims,
    { customPermission: "keep", role: "ORGANIZER", uthVerified: true });
  assert.equal((await adminDb.doc("_authClaimLocks/fn-lock").get()).exists, false);
});

test("unknown write outcome keeps a fail-closed lock inaccessible to clients", async () => {
  const { withClaimsLock } = require("../src/claims-writer.cjs");
  await assert.rejects(withClaimsLock(adminDb, "uncertain", async () => { throw new Error("unknown outcome"); }));
  assert.equal((await adminDb.doc("_authClaimLocks/uncertain").get()).exists, true);
  let entered = false;
  await assert.rejects(withClaimsLock(adminDb, "uncertain", async () => { entered = true; }),
    (error) => error.code === "aborted");
  assert.equal(entered, false);
  const db = context("uncertain", { role: "ADMIN" });
  await assertFails(getDoc(doc(db, "_authClaimLocks/uncertain")));
  await assertFails(deleteDoc(doc(db, "_authClaimLocks/uncertain")));
});

test("cooperative revocation wins over a concurrent verification with an older token", async () => {
  const { withClaimsLock } = require("../src/claims-writer.cjs");
  const token = await account("fn-revoke-lock", "revokelock@ut.edu.vn", true,
    { role: "STUDENT", uthVerified: true, marker: "keep" });
  let locked;
  const ready = new Promise((resolve) => { locked = resolve; });
  const revoke = withClaimsLock(adminDb, "fn-revoke-lock", async () => {
    locked();
    const current = await adminAuth.getUser("fn-revoke-lock");
    await new Promise((resolve) => setTimeout(resolve, 100));
    await adminAuth.setCustomUserClaims("fn-revoke-lock", { ...current.customClaims, uthVerified: false });
  });
  await ready;
  const [result] = await Promise.all([verify(token), revoke]);
  assert.equal(result.status, 403);
  assert.deepEqual((await adminAuth.getUser("fn-revoke-lock")).customClaims,
    { role: "STUDENT", uthVerified: false, marker: "keep" });
});
