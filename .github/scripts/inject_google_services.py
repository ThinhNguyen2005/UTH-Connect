"""Validate the CI-provided Firebase configuration without printing its contents."""
import json
import os
import sys
from pathlib import Path


def inject(raw, expected_project, destination):
    if not raw or not expected_project:
        raise ValueError("GOOGLE_SERVICES_JSON and FIREBASE_PROJECT_ID are required")
    try:
        data = json.loads(raw)
        project = data["project_info"]["project_id"]
        packages = [client["client_info"]["android_client_info"]["package_name"] for client in data["client"]]
    except (ValueError, KeyError, TypeError):
        raise ValueError("Invalid Firebase configuration") from None
    if project != expected_project:
        raise ValueError("Firebase project mismatch")
    if "com.giathinh.uthconnect" not in packages:
        raise ValueError("Firebase package mismatch")

    target_path = Path(destination)
    try:
        target_path.parent.mkdir(parents=True, exist_ok=True)
        target_path.write_text(json.dumps(data), encoding="utf-8")
    except OSError as error:
        print(f"Error writing Firebase configuration to {destination}: {error}", file=sys.stderr)
        raise ValueError(f"Failed to write configuration: {error}") from error


if __name__ == "__main__":
    try:
        inject(os.getenv("GOOGLE_SERVICES_JSON"), os.getenv("EXPECTED_FIREBASE_PROJECT_ID"), "app/google-services.json")
    except ValueError as error:
        raise SystemExit(str(error)) from None
