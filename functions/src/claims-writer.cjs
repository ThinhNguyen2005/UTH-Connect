"use strict";
const { randomUUID } = require("node:crypto");
const { FieldValue } = require("firebase-admin/firestore");
const { HttpsError } = require("firebase-functions/v2/https");

// Auth has no CAS: every trusted writer must use this lock and read claims inside it.
async function withClaimsLock(db, uid, operation) {
  const lock = db.collection("_authClaimLocks").doc(uid);
  let acquired = false;
  for (let attempt = 0; attempt < 10; attempt++) {
    try {
      await lock.create({ owner: randomUUID(), createdAt: FieldValue.serverTimestamp() });
      acquired = true;
      break;
    } catch (error) {
      if (error.code !== 6 && error.code !== "already-exists") throw error;
      if (attempt < 9) await new Promise((resolve) => setTimeout(resolve, 200));
    }
  }
  if (!acquired) throw new HttpsError("aborted", "Claims update in progress; retry later", { reason: "TooManyRequests" });
  try {
    const result = await operation();
    await lock.delete();
    return result;
  } catch (error) {
    // Unknown outcome/crash keeps the lock until an operator reconciles the Auth record.
    if (error instanceof HttpsError) await lock.delete();
    throw error;
  }
}
module.exports = { withClaimsLock };
