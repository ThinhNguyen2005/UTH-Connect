"use strict";
// Opt-in only after Identity Platform is enabled and deployment has been approved.
// Not exported from index.cjs, so normal callable deployment does not create a blocking trigger.
const { beforeUserCreated } = require("firebase-functions/v2/identity");
const { HttpsError } = require("firebase-functions/v2/https");
const { normalizeUthEmail } = require("./policy.cjs");
exports.requireUthDomain = beforeUserCreated((event) => {
  if (!normalizeUthEmail(event.data.email)) throw new HttpsError("permission-denied", "University domain required");
});
