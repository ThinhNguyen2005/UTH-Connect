"""Isolated Supabase staging acceptance test; requires only Python 3.10+.

Never point this script at production: it creates three events, registrations,
attendance, and temporary private-data rows. It cancels test events (retaining
audit records) and deletes the private-data fixtures.
See supabase/README.md for the required environment variables and accounts.
"""

from __future__ import annotations

import json
import os
import sys
import time
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timedelta, timezone
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

    def request(self, path: str, method: str = "GET", body: dict | None = None):
        payload = None if body is None else json.dumps(body).encode("utf-8")
        headers = {"apikey": self.key, "Accept": "application/json"}
        if self.token:
            headers["Authorization"] = f"Bearer {self.token}"
        if payload is not None:
            headers["Content-Type"] = "application/json"
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


def event_count(client: Client, event_id: str) -> int:
    return client.rows(
        "events", select="current_registered", id=f"eq.{event_id}",
    )[0]["current_registered"]


def create_event(organizer: Client, label: str, capacity: int) -> str:
    start = datetime.now(timezone.utc) + timedelta(minutes=10)
    end = start + timedelta(hours=1)
    event_id = organizer.rpc(
        "create_event",
        p_title=label,
        p_description="Automated staging acceptance fixture",
        p_location="Staging only",
        p_campus="STAGING",
        p_start_at=start.isoformat(),
        p_end_at=end.isoformat(),
        p_max_participants=capacity,
    )
    organizer.rpc("set_event_status", p_event_id=event_id, p_status="PUBLISHED")
    return event_id


def main():
    root = env("SUPABASE_URL").rstrip("/")
    require(root.startswith("https://") and ".supabase.co" in root,
            "Supabase HTTPS staging URL format")
    require(env("SUPABASE_STAGING_ACCEPTANCE") == "I_UNDERSTAND_STAGING_ONLY",
            "Explicit staging-only opt-in")
    key = env("SUPABASE_PUBLISHABLE_KEY")
    require(key.startswith("sb_publishable_"), "Client publishable key")
    accounts = {}
    for name in ("STUDENT_A", "STUDENT_B", "ORGANIZER_A", "ORGANIZER_B", "ADMIN"):
        accounts[name] = Client(
            root, key, env(f"TEST_{name}_EMAIL"), env(f"TEST_{name}_PASSWORD"),
        )
        accounts[name].login()
        require(bool(accounts[name].user_id), f"{name} authenticated")
    a, b = accounts["STUDENT_A"], accounts["STUDENT_B"]
    org, other_org = accounts["ORGANIZER_A"], accounts["ORGANIZER_B"]
    admin = accounts["ADMIN"]
    require(len({c.user_id for c in accounts.values()}) == 5,
            "Five distinct test identities")
    profiles = admin.rows("profiles", select="id,role,student_id,student_id_verified",
                          id="in.(" + ",".join(c.user_id for c in accounts.values()) + ")")
    by_id = {profile["id"]: profile for profile in profiles}
    require(len(by_id) == 5 and by_id[admin.user_id]["role"] == "admin",
            "Admin can inspect all staging test profiles")
    require(all(by_id[c.user_id]["role"] == "student"
                and by_id[c.user_id]["student_id_verified"]
                and by_id[c.user_id]["student_id"]
                for c in (a, b)), "Both students have verified IDs")
    require(all(by_id[c.user_id]["role"] == "organizer"
                for c in (org, other_org)), "Two organizer accounts have correct roles")

    # A test-owned event prefix lets operators find/cancel test fixtures.
    prefix = "UTH ACCEPTANCE " + uuid4().hex[:8]
    created = []
    private_fixtures = []
    try:
        # Permissions and RLS: authenticated, verified students cannot edit
        # protected identity fields, inspect another profile, or write attendance.
        require(len(a.rows("profiles", select="id", id=f"eq.{a.user_id}")) == 1,
                "Student can see own profile")
        require(a.rows("profiles", select="id", id=f"eq.{b.user_id}") == [],
                "Student cannot view another student's profile")
        denied("Student cannot elevate own role", lambda: a.request(
            "/rest/v1/profiles?id=eq." + a.user_id,
            "PATCH", {"role": "admin"},
        ))
        denied("Student cannot change verified student ID", lambda: a.request(
            "/rest/v1/profiles?id=eq." + a.user_id,
            "PATCH", {"student_id": "FAKE999999"},
        ))
        denied("Student cannot set verified flag", lambda: a.request(
            "/rest/v1/profiles?id=eq." + a.user_id,
            "PATCH", {"student_id_verified": False},
        ))

        # Create isolated private rows as admin. A must not see B's schedule,
        # grade or targeted notification, even when querying by the row UUID.
        fixture_data = (
            ("schedules", {"user_id": b.user_id,
                           "student_id": by_id[b.user_id]["student_id"],
                           "course_code": "STAGING-RLS", "course_name": prefix}),
            ("grades", {"user_id": b.user_id,
                        "student_id": by_id[b.user_id]["student_id"],
                        "course_code": "STAGING-RLS", "course_name": prefix}),
            ("notifications", {"recipient_id": b.user_id, "title": prefix,
                               "summary": "Private staging RLS fixture"}),
        )
        for table, data in fixture_data:
            fixture_id = str(uuid4())
            private_fixtures.append((table, fixture_id))
            admin.request("/rest/v1/" + table, "POST", {"id": fixture_id, **data})
            query = {"select": "id", "id": f"eq.{fixture_id}"}
            require(len(b.rows(table, **query)) == 1,
                    f"Owner can read own {table} row")
            require(a.rows(table, **query) == [],
                    f"Another student cannot read {table} row")
        denied("Student cannot insert grades", lambda: a.request(
            "/rest/v1/grades", "POST",
            {"student_id": by_id[a.user_id]["student_id"],
             "course_code": "STAGING-UNAUTHORIZED"},
        ))

        event_one = create_event(org, prefix + " capacity-1", 1)
        created.append(event_one)
        denied("Student cannot update server-owned event count", lambda: a.request(
            "/rest/v1/events?id=eq." + event_one,
            "PATCH", {"current_registered": 999},
        ))
        denied("Organizer cannot bypass event status RPC", lambda: org.request(
            "/rest/v1/events?id=eq." + event_one,
            "PATCH", {"status": "COMPLETED"},
        ))
        denied("Student cannot create events", lambda: a.rpc(
            "create_event", p_title="Not allowed", p_description="",
            p_location="Campus", p_campus="STAGING",
            p_start_at=(datetime.now(timezone.utc) + timedelta(hours=1)).isoformat(),
            p_end_at=(datetime.now(timezone.utc) + timedelta(hours=2)).isoformat(),
            p_max_participants=1,
        ), "FORBIDDEN")
        denied("Other organizer cannot edit owner's event", lambda: other_org.rpc(
            "edit_event", p_event_id=event_one, p_title="Unauthorized edit",
            p_description="", p_location="Campus", p_campus="STAGING",
            p_start_at=(datetime.now(timezone.utc) + timedelta(hours=1)).isoformat(),
            p_end_at=(datetime.now(timezone.utc) + timedelta(hours=2)).isoformat(),
            p_max_participants=1,
        ), "FORBIDDEN")
        denied("Student cannot insert registration directly", lambda: a.request(
            "/rest/v1/registrations", "POST",
            {"event_id": event_one, "user_id": a.user_id},
        ))

        # The event row is locked by register_for_event: two requests for a
        # one-seat event must yield one winner and a stable count of one.
        def register(client: Client):
            try:
                client.rpc("register_for_event", p_event_id=event_one)
                return True
            except ApiError as error:
                if "EVENT_FULL" in error.message:
                    return False
                raise

        with ThreadPoolExecutor(max_workers=2) as pool:
            first = pool.submit(register, a)
            second = pool.submit(register, b)
            results = first.result(), second.result()
        require(sum(results) == 1, "Concurrent 1-seat registration accepts exactly one")
        winner = a if results[0] else b
        loser = b if results[0] else a
        require(event_count(org, event_one) == 1, "Capacity count remains one")
        require(loser.rows("registrations", select="user_id",
                           event_id=f"eq.{event_one}") == [],
                "Losing student cannot see winner's registration")
        winner.rpc("register_for_event", p_event_id=event_one)
        require(event_count(org, event_one) == 1, "Repeated registration is idempotent")
        winner.rpc("cancel_event_registration", p_event_id=event_one)
        winner.rpc("cancel_event_registration", p_event_id=event_one)
        require(event_count(org, event_one) == 0, "Repeated cancellation decrements once")
        winner.rpc("register_for_event", p_event_id=event_one)
        require(event_count(org, event_one) == 1, "Re-registration increments once")

        unlimited = create_event(org, prefix + " unlimited", 0)
        created.append(unlimited)
        a.rpc("register_for_event", p_event_id=unlimited)
        b.rpc("register_for_event", p_event_id=unlimited)
        require(event_count(org, unlimited) == 2,
                "Zero-capacity legacy convention permits unlimited registration")
        a.rpc("cancel_event_registration", p_event_id=unlimited)
        b.rpc("cancel_event_registration", p_event_id=unlimited)
        require(event_count(org, unlimited) == 0,
                "Unlimited event cancellation restores count")

        # A second event leaves both students with valid tickets for signed QR
        # and permits an actual replay/expiry/ownership test.
        event_qr = create_event(org, prefix + " QR", 2)
        created.append(event_qr)
        a.rpc("register_for_event", p_event_id=event_qr)
        require(b.rows("registrations", select="user_id",
                       event_id=f"eq.{event_qr}") == [],
                "Other student's registration remains private")
        b.rpc("register_for_event", p_event_id=event_qr)
        require(event_count(org, event_qr) == 2, "QR event count is two")
        denied("Student cannot insert attendance directly", lambda: a.request(
            "/rest/v1/attendance", "POST",
            {"event_id": event_qr, "user_id": a.user_id, "status": "PRESENT"},
        ))
        token_a = a.rpc("issue_checkin_token", p_event_id=event_qr)
        require(isinstance(token_a, str) and len(token_a.split(".")) == 5,
                "Server issues structured signed token")
        tampered = token_a[:-1] + ("0" if token_a[-1] != "0" else "1")
        denied("Tampered QR is rejected", lambda: org.rpc("check_in", p_token=tampered),
               "INVALID_QR")
        denied("Other organizer cannot scan this event", lambda: other_org.rpc(
            "check_in", p_token=token_a,
        ), "FORBIDDEN")
        require(a.rows("attendance", select="event_id",
                       event_id=f"eq.{event_qr}") == [],
                "Unauthorized scans create no attendance")
        org.rpc("check_in", p_token=token_a)
        require(len(a.rows("attendance", select="event_id",
                           event_id=f"eq.{event_qr}")) == 1,
                "Authorized check-in persists")
        denied("Replaying checked-in QR is rejected", lambda: org.rpc(
            "check_in", p_token=token_a,
        ), "ALREADY_CHECKED_IN")
        token_b = b.rpc("issue_checkin_token", p_event_id=event_qr)
        # Token B remains unused; this tests expiry without replay interference.
        print("Waiting 62 seconds for real server-side QR expiry...")
        time.sleep(62)
        denied("Expired unused QR is rejected", lambda: org.rpc(
            "check_in", p_token=token_b,
        ), "QR_EXPIRED")
        require(b.rows("attendance", select="event_id",
                       event_id=f"eq.{event_qr}") == [],
                "Expired QR creates no attendance")
    finally:
        for table, fixture_id in reversed(private_fixtures):
            try:
                admin.request("/rest/v1/" + table + "?" + urlencode({
                    "id": f"eq.{fixture_id}",
                }), "DELETE")
            except Exception as error:
                print(f"WARNING: delete staging {table} fixture {fixture_id} manually: {error}",
                      file=sys.stderr)
        # Cancellation preserves the test audit trail, and ensures no test
        # event remains open for students after this run.
        for event_id in created:
            try:
                org.rpc("set_event_status", p_event_id=event_id, p_status="CANCELLED")
            except Exception as error:
                print(f"WARNING: cancel staging event {event_id} manually: {error}",
                      file=sys.stderr)

    print("PASS: real-user staging workflow and security acceptance")


if __name__ == "__main__":
    try:
        main()
    except (AssertionError, ApiError, ValueError) as error:
        print(f"FAIL: {error}", file=sys.stderr)
        sys.exit(1)
