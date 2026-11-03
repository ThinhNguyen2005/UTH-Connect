"use strict";
const { test } = require("node:test");
const { initializeTestEnvironment, assertSucceeds } = require("@firebase/rules-unit-testing");
const { doc, setDoc, getDoc, updateDoc } = require("firebase/firestore");
const { execFileSync } = require("node:child_process");

// Fixed historical baseline, isolated from the current rules and from production.
test("historical rules permit private UID reads and document-role escalation", async () => {
  if (!process.env.FIRESTORE_EMULATOR_HOST?.match(/^(127\.0\.0\.1|localhost):/)) throw new Error("Local emulator required");
  const rules = execFileSync("git", ["show", "5efb1f37cc8ca75fed8b861fb0a782aabacd5d5b:firestore.rules"], { cwd: "..", encoding: "utf8" });
  const env = await initializeTestEnvironment({ projectId: "demo-uth-auth3-baseline", firestore: { host: "127.0.0.1", port: 8080, rules } });
  try {
    await env.withSecurityRulesDisabled(async (context) => {
      await setDoc(doc(context.firestore(), "users/attacker"), { role: "student" });
      await setDoc(doc(context.firestore(), "users/victim"), { phoneNumber: "private" });
    });
    const db = env.authenticatedContext("attacker", { email: "attacker@gmail.com", email_verified: false, role: "student" }).firestore();
    await assertSucceeds(getDoc(doc(db, "users/victim")));
    await assertSucceeds(updateDoc(doc(db, "users/attacker"), { role: "admin" }));
    await assertSucceeds(setDoc(doc(db, "events/escalated"), { title: "must be blocked by Auth3" }));
  } finally { await env.cleanup(); }
});
