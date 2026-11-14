"""Housing pre-moderation acceptance; requires only Python 3.10+.

Never point this script at production. It creates specially named
[MOD-TEST] housing fixtures through two student sessions and one admin
session, exercises the PENDING/OPEN/REJECTED contract, and deletes only
the fixtures it created. Pre-existing rows of the test accounts are never
modified or deleted. See supabase/README.md for the env-var convention.

Required environment (process-only, staging credentials):
  SUPABASE_URL, SUPABASE_PUBLISHABLE_KEY,
  SUPABASE_STAGING_ACCEPTANCE=I_UNDERSTAND_STAGING_ONLY,
  TEST_STUDENT_A_EMAIL/PASSWORD, TEST_STUDENT_B_EMAIL/PASSWORD,
  TEST_ADMIN_EMAIL/PASSWORD  (the admin profile must have role='admin').
"""

from __future__ import annotations

import json
import os
from urllib.error import HTTPError
from urllib.parse import urlencode
from urllib.request import Request, urlopen
from uuid import uuid4


class ApiError(Exception):
    def __init__(self, code: int, message: str):
        super().__init__(f"HTTP {code}: {message[:300]}")
        self.code = code
        self.message = message


class Client:
    def __init__(self, root: str, key: str, email: str, password: str):
        self.root, self.key = root, key
        self.email, self.password = email, password
        self.token = ""
        self.user_id = ""

    def request(self, path: str, method: str = "GET", body: dict | None = None,
                prefer: str | None = None):
        payload = None if body is None else json.dumps(body).encode("utf-8")
        headers = {"apikey": self.key, "Accept": "application/json"}
        if self.token:
            headers["Authorization"] = f"Bearer {self.token}"
        if payload is not None:
            headers["Content-Type"] = "application/json"
        if prefer:
            headers["Prefer"] = prefer
        req = Request(self.root + path, data=payload, headers=headers, method=method)
        try:
            with urlopen(req, timeout=30) as response:
                result = response.read().decode("utf-8")
                return json.loads(result) if result else None
        except HTTPError as error:
            detail = error.read().decode("utf-8", errors="replace")
            raise ApiError(error.code, detail) from error

    def login(self):
        payload = self.request(
            "/auth/v1/token?grant_type=password", "POST",
            {"email": self.email, "password": self.password},
        )
        self.token = payload["access_token"]
        self.user_id = payload["user"]["id"]

    def rpc(self, name: str, **kwargs):
        return self.request("/rest/v1/rpc/" + name, "POST", kwargs)

    def rows(self, table: str, **query):
        return self.request("/rest/v1/" + table + "?" + urlencode(query))

    def insert(self, table: str, body: dict):
        return self.request("/rest/v1/" + table, "POST", body,
                            prefer="return=representation")

    def patch(self, table: str, row_id: str, body: dict):
        return self.request(f"/rest/v1/{table}?id=eq.{row_id}", "PATCH", body,
                            prefer="return=representation")

    def delete(self, table: str, row_id: str):
        return self.request(f"/rest/v1/{table}?id=eq.{row_id}", "DELETE")


def require(condition: bool, message: str):
    if not condition:
        raise AssertionError(message)
    print("PASS:", message)


def denied(label: str, fn, marker: str | None = None):
    try:
        fn()
    except ApiError as error:
        require(400 <= error.code < 500, label + f" ({error.code})")
        if marker:
            require(marker in error.message, label + f" ({marker})")
    else:
        raise AssertionError(label + ": request unexpectedly succeeded")


def env(name: str) -> str:
    value = os.environ.get(name, "").strip()
    if not value:
        raise ValueError(f"Missing environment variable: {name}")
    return value


FIXTURE_PREFIX = "[MOD-TEST] "


def listing_payload(label: str, author_id: str, **extra) -> dict:
    payload = {
        "author_id": author_id,
        "type": "ROOM_FOR_RENT",
        "campus": "CS1_QUANG_TRUNG",
        "title": FIXTURE_PREFIX + label,
        "description": "Automated staging acceptance fixture",
        "host_name": "Moderation Test",
        "host_student_id": "00000000",
        "contact_phone": "0000000000",
        "monthly_rent_vnd": 1500000,
        "address_label": "Staging fixture address",
        "available_slots": 1,
    }
    payload.update(extra)
    return payload


def active_count(client: Client) -> int:
    return len(client.rows(
        "housing_posts", select="id", author_id=f"eq.{client.user_id}",
        status="in.(OPEN,PENDING)",
    ))


def read_post(client: Client, post_id: str) -> dict | None:
    rows = client.rows("housing_posts", select="*", id=f"eq.{post_id}")
    return rows[0] if rows else None


def main():
    root = env("SUPABASE_URL").rstrip("/")
    require(root.startswith("https://") and ".supabase.co" in root,
            "Supabase HTTPS staging URL format")
    require(env("SUPABASE_STAGING_ACCEPTANCE") == "I_UNDERSTAND_STAGING_ONLY",
            "Explicit staging-only opt-in")
    key = env("SUPABASE_PUBLISHABLE_KEY")
    require(key.startswith("sb_publishable_"), "Client publishable key")

    accounts = {}
    for name in ("STUDENT_A", "STUDENT_B", "ADMIN"):
        accounts[name] = Client(
            root, key, env(f"TEST_{name}_EMAIL"), env(f"TEST_{name}_PASSWORD"),
        )
        accounts[name].login()
        require(bool(accounts[name].user_id), f"{name} authenticated")
    admin = accounts["ADMIN"]
    require(len({c.user_id for c in accounts.values()}) == 3,
            "Three distinct test identities")

    # Pick the student with fewer active posts as fixture author; the other
    # is the stranger. Never touch pre-existing rows of either account.
    a, b = accounts["STUDENT_A"], accounts["STUDENT_B"]
    author, stranger = (a, b) if active_count(a) <= active_count(b) else (b, a)
    baseline = active_count(author)
    require(baseline < 2,
            f"fixture author has room for fixtures (active={baseline})")

    created: list[str] = []
    try:
        # A1: client cannot self-approve — insert with status OPEN lands PENDING.
        row = author.insert("housing_posts",
                            listing_payload("A1 self-approve", author.user_id,
                                            status="OPEN"))[0]
        created.append(row["id"])
        require(row["status"] == "PENDING", "A1 insert forced to PENDING")
        require(row.get("rejection_reason") is None, "A1 rejection_reason null")
        post1 = row["id"]

        # A2: pending post invisible to feed and stranger, visible to owner/admin.
        feed = author.rows("housing_posts", select="id", status="eq.OPEN",
                           available_slots="gt.0")
        require(post1 not in [r["id"] for r in feed], "A2 feed excludes PENDING")
        require(read_post(author, post1) is not None, "A2 owner reads own PENDING")
        require(read_post(stranger, post1) is None, "A2 stranger cannot read PENDING")
        require(read_post(admin, post1) is not None, "A2 admin reads PENDING")

        # A3/A4: non-admin cannot approve/reject.
        denied("A3 student approve", lambda: author.rpc(
            "approve_housing_listing", p_listing_id=post1), "HOUSING_ADMIN_ONLY")
        denied("A4 student reject", lambda: author.rpc(
            "reject_housing_listing", p_listing_id=post1,
            p_reason="khong hop le"), "HOUSING_ADMIN_ONLY")

        # A8: direct status PATCH is RPC-only.
        denied("A8 direct status patch", lambda: author.patch(
            "housing_posts", post1, {"status": "OPEN"}), "HOUSING_STATUS_RPC_ONLY")

        # A5: blank reason and non-PENDING target are rejected.
        denied("A5 blank reason", lambda: admin.rpc(
            "reject_housing_listing", p_listing_id=post1,
            p_reason="   "), "HOUSING_REASON_REQUIRED")

        # A6: approve must never raise the open limit (total OPEN+PENDING is
        # unchanged by approval). Approve post1, then a second pending post
        # while the author already holds one OPEN.
        admin.rpc("approve_housing_listing", p_listing_id=post1)
        require(read_post(admin, post1)["status"] == "OPEN", "A6 approve -> OPEN")
        if active_count(author) < 2:
            row = author.insert("housing_posts", listing_payload("A6 second", author.user_id))[0]
            created.append(row["id"])
            post2 = row["id"]
            require(row["status"] == "PENDING", "A6 second insert PENDING")
            admin.rpc("approve_housing_listing", p_listing_id=post2)
            require(read_post(admin, post2)["status"] == "OPEN",
                    "A6 approve with 1 OPEN + 1 PENDING succeeds")
        else:
            post2 = post1
            print("NOTE: A6 ran the at-limit variant (author already at 2 active)")

        # A5b: rejecting a non-PENDING post fails.
        denied("A5b reject non-PENDING", lambda: admin.rpc(
            "reject_housing_listing", p_listing_id=post2,
            p_reason="tin da duyet"), "HOUSING_NOT_PENDING")

        # A7: reject with reason, then owner edit re-submits to PENDING.
        author.patch("housing_posts", post2, {"title": FIXTURE_PREFIX + "A7 edit"})
        require(read_post(author, post2)["status"] == "PENDING",
                "A7 owner edit of OPEN returns to PENDING")
        admin.rpc("reject_housing_listing", p_listing_id=post2,
                  p_reason="Ảnh không rõ")
        rejected = read_post(author, post2)
        require(rejected["status"] == "REJECTED", "A7 reject -> REJECTED")
        require(rejected["rejection_reason"] == "Ảnh không rõ",
                "A7 rejection_reason stored")
        require(read_post(stranger, post2) is None,
                "A7 stranger cannot read REJECTED")
        author.patch("housing_posts", post2,
                     {"title": FIXTURE_PREFIX + "A7 resubmit"})
        resubmitted = read_post(author, post2)
        require(resubmitted["status"] == "PENDING", "A7 edit of REJECTED -> PENDING")
        require(resubmitted["rejection_reason"] is None,
                "A7 rejection_reason cleared on resubmit")

        # A9: third active post is denied with the mapped limit error. Track the
        # row if the insert unexpectedly succeeds so cleanup never orphans it.
        require(active_count(author) == 2, "A9 author at 2 active before limit probe")

        def a9_insert():
            row = author.insert("housing_posts",
                                listing_payload("A9 third", author.user_id))[0]
            created.append(row["id"])
            return row

        denied("A9 open limit", a9_insert, "HOUSING_OPEN_LIMIT")

        # A10: re-submitting a REJECTED post re-enters the active set, so it counts
        # against the limit. Reject post2, fill the freed slot, then the edit that
        # would return post2 to PENDING must be denied at two active posts.
        admin.rpc("reject_housing_listing", p_listing_id=post2, p_reason="A10 reject")
        require(read_post(author, post2)["status"] == "REJECTED", "A10 post2 rejected")
        row = author.insert("housing_posts",
                            listing_payload("A10 filler", author.user_id))[0]
        created.append(row["id"])
        require(active_count(author) == 2, "A10 author at 2 active before resubmit probe")
        denied("A10 resubmit at limit", lambda: author.patch(
            "housing_posts", post2, {"title": FIXTURE_PREFIX + "A10 resubmit"}),
            "HOUSING_OPEN_LIMIT")

        print("ALL HOUSING MODERATION ACCEPTANCE CASES PASSED")
    finally:
        for post_id in created:
            try:
                author.delete("housing_posts", post_id)
                print("CLEANED fixture:", post_id)
            except ApiError as error:
                print(f"MANUAL CLEANUP NEEDED: housing_posts {post_id}: {error}")


if __name__ == "__main__":
    main()
